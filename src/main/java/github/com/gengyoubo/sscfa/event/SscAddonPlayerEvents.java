package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.AllaySPJukebox;
import github.com.gengyoubo.sscfa.ability.AllaySPTotem;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpDeathDomain;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpSummonWolves;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormErosionBrand;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormRegen;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormWitherSand;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpFrostStorm;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpMeleeAbility;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpTeleportAttack;
import github.com.gengyoubo.sscfa.mixin.FormActivePowerServiceAccessor;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.UndeadNeutralState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Player lifecycle hooks, ported from the Fabric {@code SscAddonPlayerEvents}: respawn resets, join
 * resync and the one-time welcome message, the water spear charge lock, and disconnect cleanup.
 *
 * <p>Left out on purpose: the skill-cooldown and balance join/leave handling (already done by
 * {@code SkillCooldownSync} and {@code BalanceIntegration}), Fabric SSC's orphaned mana-type cleanup
 * (SSC Forge has no per-source mana component), and the start-tracking form resync (SSC Forge's own
 * capability events send it).</p>
 */
public final class SscAddonPlayerEvents {
	private static final String WELCOME_TAG = "ssc_addon_welcomed";
	private static boolean registered;

	private SscAddonPlayerEvents() {}

	public static synchronized void register() {
		if (registered) return;
		registered = true;

		github.com.gengyoubo.sscfa.util.SscEvents.afterRespawn((oldPlayer, newPlayer, alive) -> {
			if (alive) return;
			newPlayer.getCooldowns().removeCooldown(SscAddon.LIFESAVING_CAT_TAIL);
			newPlayer.getCooldowns().removeCooldown(SscAddon.PHANTOM_BELL);
			// Netherjudge: dying resets the undying (virtual totem) passive's cooldown.
			if (FormUtils.isAnubisWolfSP(newPlayer)) resetVirtualTotemCooldowns(newPlayer);
		});

		github.com.gengyoubo.sscfa.util.SscEvents.onPlayerJoin(player -> {
			// Reconnect / dimension return: resend mancianima marks and erosion brands so the client HUD
			// and glow caches are not empty until the next change.
			try { github.com.gengyoubo.sscfa.ability.MancianimaMarkManager.resyncToPlayer(player); } catch (Throwable ignored) {}
			try { GoldenSandstormErosionBrand.resyncToPlayer(player); } catch (Throwable ignored) {}
			if (!player.getTags().contains(WELCOME_TAG)) {
				player.addTag(WELCOME_TAG);
				scheduleWelcome(player.getServer(), player.getUUID());
			}
		});

		// Upgraded axolotl water spear charge: no block placement or item use while charging.
		github.com.gengyoubo.sscfa.util.SscEvents.useBlock((player, world, hand, hit) ->
				!world.isClientSide && github.com.gengyoubo.sscfa.ability.WaterSpearLeapManager.isCharging(player.getUUID())
						? InteractionResult.FAIL : InteractionResult.PASS);
		github.com.gengyoubo.sscfa.util.SscEvents.useItem((player, world, hand) ->
				!world.isClientSide && github.com.gengyoubo.sscfa.ability.WaterSpearLeapManager.isCharging(player.getUUID())
						? InteractionResultHolder.fail(player.getItemInHand(hand))
						: InteractionResultHolder.pass(player.getItemInHand(hand)));

		PlayerSessionCleanup.register((handler, server) -> server.execute(() -> cleanupDisconnectedPlayer(handler, server)));
	}

	/** Clears the cooldown of every active {@code shape-shifter-curse:virtual_totem} power. */
	private static void resetVirtualTotemCooldowns(ServerPlayer player) {
		var cooldowns = FormActivePowerServiceAccessor.ssca$cooldowns().get(player.getUUID());
		if (cooldowns == null) return;
		FormPowerRegistry.visitActive(player, (id, power) -> {
			if ("shape-shifter-curse:virtual_totem".equals(FormPowerRegistry.typeOf(power))) cooldowns.remove(id);
		});
	}

	/** Sent once per player, 3 s after the first join so the client's language has reached the server. */
	private static void scheduleWelcome(MinecraftServer server, UUID playerUuid) {
		CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
			if (!server.isRunning()) return;
			server.execute(() -> {
				ServerPlayer p = server.getPlayerList().getPlayer(playerUuid);
				if (p == null) return;
				String url = "https://github.com/MangZai-120/shape-shifter-curse-addon/issues";
				String wikiUrl = "https://www.mcmod.cn/class/24327.html";
				String lang = SscAddon.PLAYER_LANGUAGES.getOrDefault(playerUuid, "en_us");
				boolean chinese = lang.toLowerCase(Locale.ROOT).startsWith("zh");
				MutableComponent githubLink = link(url);
				MutableComponent wikiLink = link(wikiUrl);
				if (chinese) {
					p.sendSystemMessage(Component.empty()
							.append(Component.literal("欢迎游玩幻形者诅咒扩展，游玩教程在MC百科上：").withStyle(ChatFormatting.GOLD))
							.append(wikiLink)
							.append(Component.literal("。由于作者水平有限，模组难免会有bug，如有bug请将日志发到GitHub上：").withStyle(ChatFormatting.GOLD))
							.append(githubLink)
							.append(Component.literal("；若后续更新版本导致崩溃请将崩溃日志以及必要信息文件发到模组的GitHub上，谢谢！").withStyle(ChatFormatting.GOLD)));
					p.sendSystemMessage(Component.literal("请 不 要 只 发 送 照 片 过 来 谢 谢！").withStyle(ChatFormatting.RED));
					p.sendSystemMessage(Component.literal("ps：此对话只显示这一次").withStyle(ChatFormatting.GRAY));
				} else {
					p.sendSystemMessage(Component.empty()
							.append(Component.literal("Welcome to Shape Shifter's Curse Addon! Tutorial is available on MCMOD Wiki: ").withStyle(ChatFormatting.GOLD))
							.append(wikiLink)
							.append(Component.literal(". Due to the author's limited expertise, the mod may have bugs. If you encounter any, please submit your logs on GitHub: ").withStyle(ChatFormatting.GOLD))
							.append(githubLink)
							.append(Component.literal("; If a future update causes a crash, please submit the crash log and necessary info files to the mod's GitHub, thank you!").withStyle(ChatFormatting.GOLD)));
					p.sendSystemMessage(Component.literal("Please do NOT only send screenshots, thank you!").withStyle(ChatFormatting.RED));
					p.sendSystemMessage(Component.literal("PS: This message will only be shown once.").withStyle(ChatFormatting.GRAY));
				}
			});
		});
	}

	private static MutableComponent link(String url) {
		return Component.literal(url).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
	}

	/** Drops every per-player static table, so nothing leaks and a reconnect starts clean. */
	private static void cleanupDisconnectedPlayer(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		UUID uuid = handler.player.getUUID();
		var online = server.getPlayerList().getPlayer(uuid);
		// A respawn may replace the entity while keeping the handler; a reconnect replaces the handler.
		if (online != null && online.connection != handler) return;
		ServerPlayer player = handler.player;
		github.com.gengyoubo.sscfa.ability.NightmareFearManager.onCasterRemoved(player);
		github.com.gengyoubo.sscfa.ability.NightmareFearManager.onTargetDisconnect(uuid);
		SnowFoxSpMeleeAbility.clearPlayer(uuid);
		SnowFoxSpTeleportAttack.clearPlayer(uuid);
		SnowFoxSpFrostStorm.clearPlayer(uuid);
		github.com.gengyoubo.sscfa.ability.FrostArmorManager.clearPlayer(uuid);
		github.com.gengyoubo.sscfa.ability.JumpKillManager.cancel(player);
		github.com.gengyoubo.sscfa.ability.VenomSkillManager.clearPlayer(uuid);
		github.com.gengyoubo.sscfa.ability.WindSpiritClawManager.onPlayerDisconnect(player);
		github.com.gengyoubo.sscfa.ability.WindDashManager.onPlayerDisconnect(player);
		github.com.gengyoubo.sscfa.ability.WindSpiritLandingSurgeManager.onPlayerDisconnect(player);
		github.com.gengyoubo.sscfa.ability.WaterSpearLeapManager.onPlayerDisconnect(player);
		github.com.gengyoubo.sscfa.ability.VortexGuideManager.onPlayerDisconnect(uuid);
		github.com.gengyoubo.sscfa.ability.AxolotlWaterSpurtHandler.onPlayerDisconnect(uuid);
		github.com.gengyoubo.sscfa.ability.NightmareDreamManager.onPlayerDisconnect(player);
		AnubisWolfSpDeathDomain.clearPlayer(player);
		AnubisWolfSpSummonWolves.clearPlayer(uuid);
		github.com.gengyoubo.sscfa.compat.AnubisWolfSpPerks.clearPlayer(uuid);
		AllaySPTotem.clearPlayer(player);
		GoldenSandstormErosionBrand.clearPlayer(player);
		GoldenSandstormWitherSand.clearPlayer(player);
		GoldenSandstormRegen.clearPlayer(uuid);
		github.com.gengyoubo.sscfa.item.ErosionSandPrismItem.clearPlayer(uuid);
		github.com.gengyoubo.sscfa.item.WitheredSandRingItem.clearPlayer(uuid);
		AllaySPJukebox.onPlayerDisconnect(player);
		UndeadNeutralState.clearPlayer(uuid);
		SscAddon.PLAYER_LANGUAGES.remove(uuid);
		github.com.gengyoubo.sscfa.ability.MancianimaPassive.onPlayerDisconnect(uuid);
		github.com.gengyoubo.sscfa.network.SscAddonNetworking.onPlayerDisconnect(uuid);
		github.com.gengyoubo.sscfa.ability.WildCatPaceManager.clearPlayer(player);
		github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.onPlayerDisconnect(player);
		github.com.gengyoubo.sscfa.ability.FluorescentTidalManager.onPlayerDisconnect(uuid);
		github.com.gengyoubo.sscfa.ability.VortexChargeManager.onPlayerDisconnect(uuid);
	}
}
