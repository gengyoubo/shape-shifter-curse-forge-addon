package github.com.gengyoubo.sscfa.command;

import github.com.gengyoubo.sscfa.compat.ManaUtils;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import github.com.gengyoubo.sscfa.config.ConfigChangeManager;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpFrostStorm;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpMeleeAbility;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpTeleportAttack;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpSummonWolves;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpDeathDomain;
import github.com.gengyoubo.sscfa.ability.AllaySPJukebox;
import github.com.gengyoubo.sscfa.ability.AllaySPGroupHeal;
import github.com.gengyoubo.sscfa.entity.FrostBallEntity;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.evolution.EvolutionManager;
import github.com.gengyoubo.sscfa.palette.PaletteCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SscAddonCommands {
	private static final Logger LOGGER = LoggerFactory.getLogger("SscAddon-Debug");
	private static final String SKILL_BLOCKED_PREFIX = "ssc_skill_blocked:";

	private static int giveUnanalyzedScroll(CommandContext<CommandSourceStack> context, int level) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String spellPath = StringArgumentType.getString(context, "spell");
		var stack = github.com.gengyoubo.sscfa.loot.MagicScrollLoot.createNaturalScroll(
				github.com.gengyoubo.sscfa.spell.research.WorldRuneState.get(player.getServer()),
				spellPath, level, player.level().random.nextLong());
		if (stack.isEmpty()) {
			context.getSource().sendFailure(Component.translatable("command.ssc_addon.unanalyzed.invalid", spellPath, level));
			return 0;
		}
		if (!player.getInventory().add(stack)) player.drop(stack, false);
		context.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.unanalyzed.given", spellPath, level), false);
		return 1;
	}

	private SscAddonCommands() {
		// This utility class should not be instantiated
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ssc_addon")
				.then(Commands.literal("moon_dust_spellbook")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("exp")
								.then(Commands.literal("addon")
										.then(Commands.argument("amount", IntegerArgumentType.integer(1))
												.executes(SscAddonCommands::addSpellbookExp)
												.then(Commands.literal("equipped")
														.executes(context -> addSpellbookExp(context, true))))))
						.then(Commands.literal("cd")
								.then(Commands.literal("reset")
										.executes(SscAddonCommands::resetSpellCdOnSelf)
										.then(Commands.argument("targets", EntityArgument.players())
												.executes(context -> resetSpellCd(context, EntityArgument.getPlayers(context, "targets")))))))
				.then(Commands.literal("give_unanalyzed_scroll")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("spell", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										github.com.gengyoubo.sscfa.spell.SpellRegistry.all().stream()
												.map(spell -> spell.getId().getPath()), builder))
								.executes(context -> giveUnanalyzedScroll(context, 1))
								.then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
										.executes(context -> giveUnanalyzedScroll(context, IntegerArgumentType.getInteger(context, "level"))))))
				.then(Commands.literal("set_mana")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("targets", EntityArgument.players())
								.then(Commands.argument("amount", IntegerArgumentType.integer(0))
										.executes(context -> setMana(context, EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount")))
								)
						)
				)
				.then(Commands.literal("mark_owner")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("targets", EntityArgument.entities())
								.executes(SscAddonCommands::markOwner)
						)
				)
				.then(Commands.literal("debug")
						.then(Commands.literal("form")
								.executes(SscAddonCommands::debugFormInfo)
						)
						.then(Commands.literal("mana")
								.executes(SscAddonCommands::debugMana)
						)
						.then(Commands.literal("anim")
								.executes(SscAddonCommands::debugAnim)
						)
				)
				.then(Commands.literal("get_book")
						.requires(source -> source.hasPermission(2))
						// 使用字符串ID参数，支持任意书籍ID（不仅仅是数字）
						.then(Commands.argument("book_id", StringArgumentType.string())
								.suggests((context, builder) -> {
									// 自动补全：显示所有可用的书籍ID
									return SharedSuggestionProvider.suggest(
											github.com.gengyoubo.sscfa.loot.StoryBookLoot.getBookIds(),
											builder
									);
								})
								.executes(SscAddonCommands::giveStoryBookById)
								.then(Commands.argument("language", StringArgumentType.string())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"zh_cn", "en_us"}, builder))
										.executes(SscAddonCommands::giveStoryBookByIdWithLang)
								)
						)
				)
				.then(Commands.literal("list_books")
						.requires(source -> source.hasPermission(2))
						.executes(SscAddonCommands::listBooks)
						.then(Commands.argument("language", StringArgumentType.string())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"zh_cn", "en_us"}, builder))
								.executes(SscAddonCommands::listBooksWithLang)
						)
				)
				.then(Commands.literal("reload_books")
						.requires(source -> source.hasPermission(2))
						.executes(SscAddonCommands::reloadBooks)
				)
				.then(Commands.literal("reload")
						.requires(source -> source.hasPermission(2))
						.executes(SscAddonCommands::reloadConfig)
				)
				// 玩家自助白名单 GUI（无 OP 限制，仅作用于调用者自己）
				.then(Commands.literal("my_whitelist")
						.executes(SscAddonCommands::openWhitelistGui)
				)
				// 重置法术 CD（OP）：清目标玩家全部法术冷却（共享表 + 书内卷轴 NBT + 施法 GCD）
				.then(Commands.literal("reset_spell_cd")
						.requires(source -> source.hasPermission(2))
						.executes(SscAddonCommands::resetSpellCdOnSelf)
						.then(Commands.argument("targets", EntityArgument.players())
								.executes(context -> resetSpellCd(context, EntityArgument.getPlayers(context, "targets")))
						)
				)
				.then(Commands.literal("skill")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("form", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Arrays.asList("snow_fox", "anubis_wolf", "allay", "axolotl", "wild_cat", "familiar_fox", "familiar_fox_red"), builder))
								.then(Commands.argument("skill", StringArgumentType.word())
										.suggests((context, builder) -> {
											String form = StringArgumentType.getString(context, "form");
											return SharedSuggestionProvider.suggest(getSkillsForForm(form), builder);
										})
										.then(Commands.argument("player", EntityArgument.player())
												.executes(SscAddonCommands::invokeSkillOnPlayer)
										)
										.executes(SscAddonCommands::invokeSkillOnSelf)
								)
						)
				)
				.then(Commands.literal("block")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("form", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(
												Arrays.asList("snow_fox", "anubis_wolf", "allay", "axolotl", "wild_cat", "familiar_fox", "familiar_fox_red"), builder))
										.then(Commands.argument("skill", StringArgumentType.word())
												.suggests((context, builder) -> {
													String form = StringArgumentType.getString(context, "form");
													return SharedSuggestionProvider.suggest(getSkillsForForm(form), builder);
												})
												.executes(SscAddonCommands::blockSkill)
										)
								)
						)
				)
				.then(Commands.literal("unblock")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("form", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(
												Arrays.asList("snow_fox", "anubis_wolf", "allay", "axolotl", "wild_cat", "familiar_fox", "familiar_fox_red"), builder))
										.then(Commands.argument("skill", StringArgumentType.word())
												.suggests((context, builder) -> {
													String form = StringArgumentType.getString(context, "form");
													return SharedSuggestionProvider.suggest(getSkillsForForm(form), builder);
												})
												.executes(SscAddonCommands::unblockSkill)
										)
								)
						)
				)
				.then(Commands.literal("list_blocks")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(SscAddonCommands::listBlockedSkills)
						)
				)
				.then(Commands.literal("resistance")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("get")
								.executes(ctx -> resistanceGet(ctx, ctx.getSource().getPlayer()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> resistanceGet(ctx, EntityArgument.getPlayer(ctx, "player")))
								)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("value", IntegerArgumentType.integer(0))
										.executes(ctx -> resistanceSet(ctx, ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value")))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(ctx -> resistanceSet(ctx, EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value")))
										)
								)
						)
						.then(Commands.literal("add")
								.then(Commands.argument("delta", IntegerArgumentType.integer())
										.executes(ctx -> resistanceAdd(ctx, ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "delta")))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(ctx -> resistanceAdd(ctx, EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "delta")))
										)
								)
						)
				)
				// ============== /ssc_addon mancianima_assault ==============
				// 控制玩家"今日是否可触发契灵敲钟袭击"的每日冷却（持久化）
				.then(Commands.literal("mancianima_assault")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("reset")
								.executes(ctx -> mancianimaAssaultReset(ctx, ctx.getSource().getPlayer()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> mancianimaAssaultReset(ctx, EntityArgument.getPlayer(ctx, "player")))
								)
						)
						.then(Commands.literal("lock")
								.executes(ctx -> mancianimaAssaultLock(ctx, ctx.getSource().getPlayer()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> mancianimaAssaultLock(ctx, EntityArgument.getPlayer(ctx, "player")))
								)
						)
						.then(Commands.literal("status")
								.executes(ctx -> mancianimaAssaultStatus(ctx, ctx.getSource().getPlayer()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> mancianimaAssaultStatus(ctx, EntityArgument.getPlayer(ctx, "player")))
								)
						)
				)
				// ============== /ssc_addon evolution ==============
				// SSCA 进化加点系统管理指令（框架）：unlock_all 全解锁 / reset 重置
				.then(Commands.literal("evolution")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("unlock_all")
								.executes(ctx -> evolutionUnlockAll(ctx, ctx.getSource().getPlayer()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> evolutionUnlockAll(ctx, EntityArgument.getPlayer(ctx, "player")))
								)
						)
						.then(Commands.literal("reset")
								.executes(ctx -> evolutionReset(ctx, ctx.getSource().getPlayer()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> evolutionReset(ctx, EntityArgument.getPlayer(ctx, "player")))
								)
						)
				)
				// ============== /ssc_addon palette ==============
				// 形态配色「分享码」：导出当前配色为分享文本；apply 一键应用
				// 不加 .requires(permissionLevel) — 关闭作弊的存档/服务器内普通玩家也能使用；只对执行者本人生效（规则 #49）
				.then(Commands.literal("palette")
						.then(Commands.literal("export")
								.executes(ctx -> paletteExport(ctx, ctx.getSource().getPlayer()))
						)
						.then(Commands.literal("apply")
								.then(Commands.argument("code", StringArgumentType.greedyString())
										.executes(ctx -> paletteApply(ctx, ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "code")))
								)
						)
				)
		);
	}

	// ============== /ssc_addon palette ==============
	// 仅对执行者本人生效，禁止 target 参数；反馈消息全部走 lang key
	private static int paletteExport(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("ssc_addon.palette.only_self")); return 0; }
		var skin = net.onixary.shapeShifterCurseForge.api.SscApi.currentSkin(player).orElse(null);
		if (skin == null) { ctx.getSource().sendFailure(Component.translatable("ssc_addon.palette.only_self")); return 0; }
		FormTextureUtils.ColorSetting cs = skin.getFormColor();
		// SSC stores ABGR; the code carries RGBA.
		String code = PaletteCodec.encode(FormTextureUtils.abgr2Rgba(cs.primaryColor()), FormTextureUtils.abgr2Rgba(cs.accentColor1()),
				FormTextureUtils.abgr2Rgba(cs.accentColor2()), FormTextureUtils.abgr2Rgba(cs.eyeColorA()), FormTextureUtils.abgr2Rgba(cs.eyeColorB()),
				cs.primaryGreyReverse(), cs.accent1GreyReverse(), cs.accent2GreyReverse());

		MutableComponent codeText = Component.literal(code).setStyle(Style.EMPTY
				.withColor(ChatFormatting.AQUA)
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("ssc_addon.palette.export.copy_hover"))));
		ctx.getSource().sendSuccess(() -> Component.translatable("ssc_addon.palette.export.header").append(codeText), false);
		ctx.getSource().sendSuccess(() -> Component.translatable("ssc_addon.palette.export.hint"), false);
		return 1;
	}

	private static int paletteApply(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String rawCode) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("ssc_addon.palette.only_self")); return 0; }
		PaletteCodec.PaletteData data;
		try {
			data = PaletteCodec.decode(rawCode);
		} catch (PaletteCodec.DecodeException e) {
			ctx.getSource().sendFailure(Component.translatable("ssc_addon.palette.apply.failed",
					Component.translatable(e.langKey, e.args)));
			return 0;
		}
		var skin = net.onixary.shapeShifterCurseForge.api.SscApi.currentSkin(player).orElse(null);
		if (skin == null) { ctx.getSource().sendFailure(Component.translatable("ssc_addon.palette.only_self")); return 0; }
		skin.setFormColor(new FormTextureUtils.ColorSetting(FormTextureUtils.rgba2Abgr(data.primaryRGBA()),
				FormTextureUtils.rgba2Abgr(data.accent1RGBA()), FormTextureUtils.rgba2Abgr(data.accent2RGBA()),
				FormTextureUtils.rgba2Abgr(data.eyeARGBA()), FormTextureUtils.rgba2Abgr(data.eyeBRGBA()),
				data.primaryGreyReverse(), data.accent1GreyReverse(), data.accent2GreyReverse()));
		skin.setEnableFormColor(true);
		// Broadcast so the player and everyone tracking them see the new colours now.
		net.onixary.shapeShifterCurseForge.network.ModNetwork.CHANNEL.send(
				net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				net.onixary.shapeShifterCurseForge.network.SyncSkinPacket.forPlayer(player, skin));
		ctx.getSource().sendSuccess(() -> Component.translatable("ssc_addon.palette.apply.success"), false);
		return 1;
	}

	// ============== /ssc_addon evolution ==============
	private static int evolutionUnlockAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.literal("目标玩家无效")); return 0; }
		EvolutionManager.unlockAll(player);
		ctx.getSource().sendSuccess(() -> Component.literal("已将 " + player.getName().getString() + " 的 SSCA 进化路线设为全解锁"), true);
		return 1;
	}

	private static int evolutionReset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.literal("目标玩家无效")); return 0; }
		EvolutionManager.reset(player);
		ctx.getSource().sendSuccess(() -> Component.literal("已重置 " + player.getName().getString() + " 的 SSCA 进化数据"), true);
		return 1;
	}

	// ============== /ssc_addon mancianima_assault ==============
	private static final long MANCIANIMA_ASSAULT_COOLDOWN_TICKS = 24000L;

	private static int mancianimaAssaultReset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.common.no_target_player")); return 0; }
		net.minecraft.server.MinecraftServer srv = ctx.getSource().getServer();
		github.com.gengyoubo.sscfa.ability.MancianimaAssaultState state =
				github.com.gengyoubo.sscfa.ability.MancianimaAssaultState.get(srv);
		if (state.lastRoll.remove(player.getUUID()) != null) {
			state.setDirty();
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.assault.reset", player.getName().getString()), true);
		return 1;
	}

	private static int mancianimaAssaultLock(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.common.no_target_player")); return 0; }
		net.minecraft.server.MinecraftServer srv = ctx.getSource().getServer();
		github.com.gengyoubo.sscfa.ability.MancianimaAssaultState state =
				github.com.gengyoubo.sscfa.ability.MancianimaAssaultState.get(srv);
		state.lastRoll.put(player.getUUID(), srv.overworld().getGameTime());
		state.setDirty();
		ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.assault.lock", player.getName().getString()), true);
		return 1;
	}

	private static int mancianimaAssaultStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.common.no_target_player")); return 0; }
		net.minecraft.server.MinecraftServer srv = ctx.getSource().getServer();
		github.com.gengyoubo.sscfa.ability.MancianimaAssaultState state =
				github.com.gengyoubo.sscfa.ability.MancianimaAssaultState.get(srv);
		Long last = state.lastRoll.get(player.getUUID());
		long now = srv.overworld().getGameTime();
		if (last == null || now - last >= MANCIANIMA_ASSAULT_COOLDOWN_TICKS) {
			ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.assault.status.available", player.getName().getString()), false);
		} else {
			long remain = MANCIANIMA_ASSAULT_COOLDOWN_TICKS - (now - last);
			ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.assault.status.cooldown", player.getName().getString(), remain, remain / 20), false);
		}
		return 1;
	}

	// ============== /ssc_addon resistance ==============
	private static int resistanceGet(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.common.no_target_player")); return 0; }
		int cur = PowerUtils.getResourceValue(player, FormIdentifiers.MANCIANIMA_RESISTANCE);
		int max = PowerUtils.getResourceMax(player, FormIdentifiers.MANCIANIMA_RESISTANCE);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.resistance.get", player.getName().getString(), cur, max), false);
		return 1;
	}

	private static int resistanceSet(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int value) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.common.no_target_player")); return 0; }
		int max = PowerUtils.getResourceMax(player, FormIdentifiers.MANCIANIMA_RESISTANCE);
		if (max <= 0) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.resistance.no_power")); return 0; }
		int clamped = Math.max(0, Math.min(value, max));
		PowerUtils.setResourceValueAndSync(player, FormIdentifiers.MANCIANIMA_RESISTANCE, clamped);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.resistance.set", player.getName().getString(), clamped, max), true);
		return 1;
	}

	private static int resistanceAdd(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int delta) {
		if (player == null) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.common.no_target_player")); return 0; }
		int max = PowerUtils.getResourceMax(player, FormIdentifiers.MANCIANIMA_RESISTANCE);
		if (max <= 0) { ctx.getSource().sendFailure(Component.translatable("command.ssc_addon.resistance.no_power")); return 0; }
		int cur = PowerUtils.getResourceValue(player, FormIdentifiers.MANCIANIMA_RESISTANCE);
		int next = (int) Math.max(0L, Math.min((long) cur + delta, max));
		PowerUtils.setResourceValueAndSync(player, FormIdentifiers.MANCIANIMA_RESISTANCE, next);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.resistance.add", player.getName().getString(), cur, next, max), true);
		return 1;
	}

	private static int setMana(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, int amount) {
		int count = 0;
		for (ServerPlayer player : targets) {
			boolean updated = false;

			int snowFoxMax = PowerUtils.getResourceMax(player, FormIdentifiers.SNOW_FOX_RESOURCE);
			if (snowFoxMax > 0) {
				int clamped = Math.min(amount, snowFoxMax);
				PowerUtils.setResourceValueAndSync(player, FormIdentifiers.SNOW_FOX_RESOURCE, clamped);
				updated = true;
			}

			int allayMax = PowerUtils.getResourceMax(player, FormIdentifiers.ALLAY_MANA_RESOURCE);
			if (allayMax > 0) {
				int clamped = Math.min(amount, allayMax);
				PowerUtils.setResourceValueAndSync(player, FormIdentifiers.ALLAY_MANA_RESOURCE, clamped);
				updated = true;
			}

			// 吸血蝙蝠雾血条：同样按各自上限 clamp 后写入
			int batBloodMax = PowerUtils.getResourceMax(player, FormIdentifiers.BAT_BLOOD_RESOURCE);
			if (batBloodMax > 0) {
				int clamped = Math.min(amount, batBloodMax);
				PowerUtils.setResourceValueAndSync(player, FormIdentifiers.BAT_BLOOD_RESOURCE, clamped);
				updated = true;
			}

			int soulMax = PowerUtils.getResourceMax(player, FormIdentifiers.ANUBIS_WOLF_SP_SOUL_ENERGY);
			if (soulMax > 0) {
				int clamped = Math.min(amount, soulMax);
				github.com.gengyoubo.sscfa.ability.AnubisWolfSpSoulEnergy.setEnergy(player, clamped);
				updated = true;
			}

			double maxMana = ManaUtils.getPlayerMaxMana(player);
			if (maxMana > 0) {
				ManaUtils.setPlayerMana(player, Math.min(amount, maxMana));
				updated = true;
			}

			if (updated) {
				count++;
			}
		}
		final int finalCount = count;
		context.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.set_mana.result", finalCount, amount), true);
		return count;
	}

	private static int markOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
		CommandSourceStack source = context.getSource();
		Entity attacker = source.getEntity();

		if (attacker instanceof ServerPlayer player) {
			UUID playerUUID = player.getUUID();
			for (Entity target : targets) {
				if (target instanceof LivingEntity livingTarget) {
					// Update ownership tag: remove old owner, set new owner
					livingTarget.getTags().removeIf(tag -> tag.startsWith("ssc_owner:"));
					livingTarget.addTag("ssc_owner:" + playerUUID.toString());
				}
			}
			return targets.size();
		}
		return 0;
	}

	private static int debugFormInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();

		// 获取玩家形态组件（getPlayerOrThrow 保证非空）
		var form = net.onixary.shapeShifterCurseForge.form.FormManager.current(player);
		StringBuilder debugInfo = new StringBuilder();
		debugInfo.append("===== SSC_ADDON FORM DEBUG =====\n");
		player.displayClientMessage(Component.translatable("command.ssc_addon.debug_form.header").withStyle(ChatFormatting.AQUA), false);
		if (form == null) {
			debugInfo.append("Current Form: NULL (no form active)\n");
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_form.no_form").withStyle(ChatFormatting.AQUA), false);
		} else {
			debugInfo.append("Form ID: ").append(form.id()).append("\n");
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_form.form_id", String.valueOf(form.id())).withStyle(ChatFormatting.AQUA), false);
			debugInfo.append("Form Class: ").append(form.getClass().getName()).append("\n");
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_form.form_class", form.getClass().getName()).withStyle(ChatFormatting.AQUA), false);
		}
		debugInfo.append("================================");
		player.displayClientMessage(Component.translatable("command.ssc_addon.debug_form.footer").withStyle(ChatFormatting.AQUA), false);

		// 记录到服务器日志
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(debugInfo.toString());
		}

		return 1;
	}

	private static int debugMana(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean foundMana = false;

		int snowFoxVal = PowerUtils.getResourceValue(player, FormIdentifiers.SNOW_FOX_RESOURCE);
		int snowFoxMax = PowerUtils.getResourceMax(player, FormIdentifiers.SNOW_FOX_RESOURCE);
		if (snowFoxMax > 0) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_mana.snow_fox", snowFoxVal, snowFoxMax).withStyle(ChatFormatting.AQUA), false);
			foundMana = true;
		}

		int allayVal = PowerUtils.getResourceValue(player, FormIdentifiers.ALLAY_MANA_RESOURCE);
		int allayMax = PowerUtils.getResourceMax(player, FormIdentifiers.ALLAY_MANA_RESOURCE);
		if (allayMax > 0) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_mana.allay", allayVal, allayMax).withStyle(ChatFormatting.AQUA), false);
			foundMana = true;
		}

		int soulVal = PowerUtils.getResourceValue(player, FormIdentifiers.ANUBIS_WOLF_SP_SOUL_ENERGY);
		int soulMax = PowerUtils.getResourceMax(player, FormIdentifiers.ANUBIS_WOLF_SP_SOUL_ENERGY);
		if (soulMax > 0) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_mana.anubis_wolf", soulVal, soulMax).withStyle(ChatFormatting.AQUA), false);
			foundMana = true;
		}

		double maxMana = ManaUtils.getPlayerMaxMana(player);
		if (maxMana > 0) {
			String type = ManaUtils.getPlayerManaTypeID(player);
			if (type != null) player.displayClientMessage(Component.translatable("command.ssc_addon.debug_mana.mana_type", type).withStyle(ChatFormatting.AQUA), false);
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_mana.mana", ManaUtils.getPlayerMana(player), maxMana).withStyle(ChatFormatting.AQUA), false);
			foundMana = true;
		}

		if (!foundMana) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.debug_mana.no_mana").withStyle(ChatFormatting.YELLOW), false);
		}
		return 1;
	}

	/**
	 * /ssc_addon debug anim：切换月织蛛动画调试日志记录（客户端功能）。
	 * 服务端发 S2C 包给执行者，客户端切换本地日志记录开关；再次输入关闭。
	 * 记录期间会把动画切换事件写入游戏日志（搜 SSCA_AnimDebug），便于分析二段跳等动画时序问题。
	 */
	private static int debugAnim(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		// 发 S2C 包给执行者客户端切换记录开关；反馈由客户端接收方给出（含切换后状态），此处不重复提示
		github.com.gengyoubo.sscfa.network.SscAddonNetworking.sendAnimDebugToggle(player);
		return 1;
	}

	// ===== 新增：书籍命令方法 =====

	/**
	 * 通过书籍ID获取书籍（使用配置的默认语言）
	 */
	private static int giveStoryBookById(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return giveStoryBookByIdInternal(context, null);
	}

	/**
	 * 通过书籍ID和指定语言获取书籍
	 */
	private static int giveStoryBookByIdWithLang(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String lang = StringArgumentType.getString(context, "language");
		return giveStoryBookByIdInternal(context, lang);
	}

	/**
	 * 内部方法：通过书籍ID获取书籍
	 */
	private static int giveStoryBookByIdInternal(CommandContext<CommandSourceStack> context, String language) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String bookId = StringArgumentType.getString(context, "book_id");

		net.minecraft.world.item.ItemStack book = github.com.gengyoubo.sscfa.loot.StoryBookLoot.getStoryBookById(bookId, language);

		if (book.isEmpty()) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.book.not_found", bookId).withStyle(ChatFormatting.RED), false);
			return 0;
		}

		// 获取书籍信息用于显示
		github.com.gengyoubo.sscfa.loot.StoryBookLoot.BookData bookData =
				github.com.gengyoubo.sscfa.loot.StoryBookLoot.getBookDataById(bookId, language);

		if (!player.getInventory().add(book)) {
			player.drop(book, false);
		}

		String bookTitle = bookData != null ? bookData.title : bookId;
		player.displayClientMessage(Component.translatable("command.ssc_addon.book.obtained", bookTitle).withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	/**
	 * 列出所有可用书籍（使用配置的默认语言）
	 */
	private static int listBooks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return listBooksInternal(context, null);
	}

	/**
	 * 列出所有可用书籍（指定语言）
	 */
	private static int listBooksWithLang(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String lang = StringArgumentType.getString(context, "language");
		return listBooksInternal(context, lang);
	}

	/**
	 * 内部方法：列出所有可用书籍
	 */
	private static int listBooksInternal(CommandContext<CommandSourceStack> context, String language) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();

		java.util.List<String> bookIds;
		if (language != null && !language.isEmpty()) {
			bookIds = github.com.gengyoubo.sscfa.loot.StoryBookLoot.getBookIds(language);
		} else {
			bookIds = github.com.gengyoubo.sscfa.loot.StoryBookLoot.getBookIds();
		}

		if (bookIds.isEmpty()) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.empty").withStyle(ChatFormatting.YELLOW), false);
			return 0;
		}

		player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.header").withStyle(ChatFormatting.GOLD), false);
		player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.count", bookIds.size()).withStyle(ChatFormatting.AQUA), false);

		for (String bookId : bookIds) {
			github.com.gengyoubo.sscfa.loot.StoryBookLoot.BookData bookData =
					github.com.gengyoubo.sscfa.loot.StoryBookLoot.getBookDataById(bookId, language);

			if (bookData != null) {
				// 截断过长的标题
				String displayTitle = bookData.title;
				if (displayTitle.length() > 30) {
					displayTitle = displayTitle.substring(0, 27) + "...";
				}
				player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.entry", bookId, displayTitle, bookData.author).withStyle(ChatFormatting.WHITE), false);
			} else {
				player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.entry_failed", bookId).withStyle(ChatFormatting.RED), false);
			}
		}

		player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.footer").withStyle(ChatFormatting.GOLD), false);
		player.displayClientMessage(Component.translatable("command.ssc_addon.book.list.hint").withStyle(ChatFormatting.GRAY), false);

		return bookIds.size();
	}

	/**
	 * 重新加载书籍配置
	 */
	private static int reloadBooks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();

		try {
			github.com.gengyoubo.sscfa.loot.StoryBookLoot.reloadBooks();
			int bookCount = github.com.gengyoubo.sscfa.loot.StoryBookLoot.getBookCount();
			player.displayClientMessage(Component.translatable("command.ssc_addon.reload.books.success", bookCount).withStyle(ChatFormatting.GREEN), false);
			return 1;
		} catch (Exception e) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.reload.books.fail", e.getMessage()).withStyle(ChatFormatting.RED), false);
			LOGGER.error("Failed to reload books", e);
			return 0;
		}
	}

	/**
	 * 重新加载模组配置
	 */
	private static int reloadConfig(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();

		try {
			ConfigChangeManager.notifyChange();
			player.displayClientMessage(Component.translatable("command.ssc_addon.reload.config.success").withStyle(ChatFormatting.GREEN), true);
			return 1;
		} catch (Exception e) {
			player.displayClientMessage(Component.translatable("command.ssc_addon.reload.config.fail", e.getMessage()).withStyle(ChatFormatting.RED), true);
			LOGGER.error("Failed to reload config", e);
			return 0;
		}
	}

	// ===== SP悦灵治疗白名单命令 =====
	// 旧的 /ssc_addon whitelist add/remove/list/clear 系列指令已被 my_whitelist GUI 取代，
	// 相关私有方法已删除以减少代码维护面。

	/**
	 * 打开玩家自助白名单 GUI（无 OP 限制，仅作用于调用者本人）。
	 * 服务端通过 S2C 包推送当前白名单数据给客户端，由客户端打开 WhitelistManageScreen。
	 * 控制台 / 命令方块调用会返回错误。
	 */
	private static int openWhitelistGui(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.translatable("command.ssc_addon.whitelist.gui.console_only"));
			return 0;
		}
		github.com.gengyoubo.sscfa.network.SscAddonNetworking.sendWhitelistSync(player);
		return 1;
	}

	private static int addSpellbookExp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return addSpellbookExp(context, false);
	}

	private static int addSpellbookExp(CommandContext<CommandSourceStack> context, boolean equipped) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack book = SpellbookExperience.select(equipped, player.getMainHandItem(), player.getOffhandItem(),
				() -> github.com.gengyoubo.sscfa.spell.SpellCastManager.getEquippedBook(player));
		if (book == null || book.isEmpty()) {
			context.getSource().sendFailure(Component.translatable("command.ssc_addon.moon_dust_spellbook.exp."
					+ (equipped ? "not_equipped" : "not_held")));
			return 0;
		}
		int amount = IntegerArgumentType.getInteger(context, "amount");
		var result = SpellbookExperience.add(player, book, amount);
		if (!result.success()) {
			context.getSource().sendFailure(Component.translatable("command.ssc_addon.moon_dust_spellbook.exp.too_large",
					result.maximumAdditional()));
			return 0;
		}
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
		if (player.containerMenu != player.inventoryMenu) {
			player.inventoryMenu.broadcastChanges();
		}
		String total = String.format(Locale.ROOT, "%.1f", result.totalTen() / 10.0);
		if(SpellbookData.getLevel(book)>=SpellbookData.MAX_LEVEL) {
			context.getSource().sendSuccess(()->Component.translatable("text.ssc_addon.player_mastery.command",amount,total),true);
			return 1;
		}
		context.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.moon_dust_spellbook.exp."
				+ (equipped ? "added_equipped" : "added"), amount, total), true);
		return 1;
	}

	private static int resetSpellCdOnSelf(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		return resetSpellCd(context, player == null ? Collections.emptyList() : Collections.singletonList(player));
	}

	/**
	 * 重置目标玩家的全部法术 CD（/ssc_addon moon_dust_spellbook cd reset，OP；保留 reset_spell_cd）。
	 * 清理三处：① 共享 CD 表（施法判定权威）；② 装备书内所有卷轴的 NBT Cd；
	 * ③ 施法 GCD 门（释放后 0.8s 间隔）。正在读条的会话不强制中断（CD 重置只影响下一次起手）。
	 */
	private static int resetSpellCd(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		if (targets.isEmpty()) {
			context.getSource().sendFailure(Component.translatable("command.ssc_addon.reset_spell_cd.console_need_target"));
			return 0;
		}
		int count = 0;
		for (ServerPlayer player : targets) {
			// ① 共享 CD 表（权威）
			github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.clearAllFor(player);
			// ② 装备书内卷轴 NBT Cd（HUD 显示用；共享表已清，这里是保持显示一致）
			net.minecraft.world.item.ItemStack book = github.com.gengyoubo.sscfa.spell.SpellCastManager.getEquippedBook(player);
			if (book != null && !book.isEmpty()) {
				for (int slot = 0; slot < github.com.gengyoubo.sscfa.spell.SpellbookData.getSlotCount(book); slot++) {
					net.minecraft.world.item.ItemStack scroll = github.com.gengyoubo.sscfa.spell.SpellbookData.getScroll(book, slot);
					if (!scroll.isEmpty()) {
						github.com.gengyoubo.sscfa.spell.ScrollData.setCooldownEnd(scroll, 0L);
						github.com.gengyoubo.sscfa.spell.SpellbookData.setScroll(book, slot, scroll);
					}
				}
			}
			// ③ 施法 GCD 门
			github.com.gengyoubo.sscfa.spell.SpellChannelManager.clearCastGate(player);
			count++;
		}
		final int cleared = count;
		context.getSource().sendSuccess(() -> Component.translatable("command.ssc_addon.reset_spell_cd.success", cleared), true);
		return count;
	}

	private static List<String> getSkillsForForm(String form) {
		return switch (form) {
			case "snow_fox" -> Arrays.asList("melee_primary", "melee_secondary", "ranged_primary", "ranged_secondary", "frost_regen");
			case "anubis_wolf" -> Arrays.asList("summon_wolves", "death_domain", "soul_sand_heal", "wither_hunt", "soul_scrutiny");
			case "allay" -> Arrays.asList("jukebox_charge", "group_heal", "mana_regen");
			case "axolotl" -> Arrays.asList("natural_regen_boost", "rain_wetness");
			case "wild_cat" -> Arrays.asList("night_speed", "day_slow");
			case "familiar_fox" -> Arrays.asList("mana_regen");
			case "familiar_fox_red" -> Arrays.asList("red_mana_regen");
			default -> Collections.emptyList();
		};
	}

	private static boolean isSkillBlocked(ServerPlayer player, String form, String skill) {
		String tag = SKILL_BLOCKED_PREFIX + form + ":" + skill;
		return player.getTags().contains(tag);
	}

	private static void blockSkill(ServerPlayer player, String form, String skill) {
		String tag = SKILL_BLOCKED_PREFIX + form + ":" + skill;
		player.addTag(tag);
	}

	private static void unblockSkill(ServerPlayer player, String form, String skill) {
		String tag = SKILL_BLOCKED_PREFIX + form + ":" + skill;
		player.getTags().remove(tag);
	}

	private static List<String> getBlockedSkills(ServerPlayer player) {
		return player.getTags().stream()
				.filter(tag -> tag.startsWith(SKILL_BLOCKED_PREFIX))
				.map(tag -> tag.substring(SKILL_BLOCKED_PREFIX.length()))
				.toList();
	}

	private static int invokeSkillOnSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String form = StringArgumentType.getString(context, "form");
		String skill = StringArgumentType.getString(context, "skill");
		return invokeSkill(context, player, form, skill);
	}

	private static int invokeSkillOnPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		String form = StringArgumentType.getString(context, "form");
		String skill = StringArgumentType.getString(context, "skill");
		return invokeSkill(context, target, form, skill);
	}

	private static int blockSkill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		String form = StringArgumentType.getString(context, "form");
		String skill = StringArgumentType.getString(context, "skill");

		if (isSkillBlocked(target, form, skill)) {
			context.getSource().sendSuccess(() -> Component.translatable(
					"command.ssc_addon.block_skill.already", form, skill, target.getName().getString()
			).withStyle(ChatFormatting.YELLOW), false);
			return 0;
		}

		blockSkill(target, form, skill);
		LOGGER.info("[SSC] Blocked " + form + "/" + skill + " for " + target.getName().getString());
		context.getSource().sendSuccess(() -> Component.translatable(
				"command.ssc_addon.block_skill.success", target.getName().getString(), form, skill
		).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int unblockSkill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		String form = StringArgumentType.getString(context, "form");
		String skill = StringArgumentType.getString(context, "skill");

		if (!isSkillBlocked(target, form, skill)) {
			context.getSource().sendSuccess(() -> Component.translatable(
					"command.ssc_addon.unblock_skill.not_blocked", form, skill, target.getName().getString()
			).withStyle(ChatFormatting.YELLOW), false);
			return 0;
		}

		unblockSkill(target, form, skill);
		LOGGER.info("[SSC] Unblocked " + form + "/" + skill + " for " + target.getName().getString());
		context.getSource().sendSuccess(() -> Component.translatable(
				"command.ssc_addon.unblock_skill.success", target.getName().getString(), form, skill
		).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int listBlockedSkills(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		List<String> blockedSkills = getBlockedSkills(target);

		if (blockedSkills.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.translatable(
					"command.ssc_addon.list_blocked.empty", target.getName().getString()
			).withStyle(ChatFormatting.YELLOW), false);
			return 0;
		}

		context.getSource().sendSuccess(() -> Component.translatable(
				"command.ssc_addon.list_blocked.header", target.getName().getString()
		).withStyle(ChatFormatting.GOLD), false);

		for (String blocked : blockedSkills) {
			context.getSource().sendSuccess(() -> Component.translatable(
					"command.ssc_addon.list_blocked.entry", blocked
			).withStyle(ChatFormatting.WHITE), false);
		}

		return blockedSkills.size();
	}

	private static int invokeSkill(CommandContext<CommandSourceStack> context, ServerPlayer target, String form, String skill) {
		CommandSourceStack source = context.getSource();
		String executorName = source.getTextName();

		if (isSkillBlocked(target, form, skill)) {
			LOGGER.warn("[SSC] Blocked skill invocation: " + form + "/" + skill + " on " + target.getName());
			source.sendFailure(Component.translatable("command.ssc_addon.ssc_test.skill_blocked", form, skill));
			return 0;
		}

	if ("snow_fox".equals(form)) {
            return invokeSnowFoxSkill(source, target, skill, executorName);
        } else if ("anubis_wolf".equals(form)) {
            return invokeAnubisWolfSkill(source, target, skill, executorName);
        } else if ("allay".equals(form)) {
            return invokeAllaySkill(source, target, skill, executorName);
        }

        LOGGER.warn("[SSC] Unknown form: " + form);
		source.sendFailure(Component.translatable("command.ssc_addon.ssc_test.unknown_form", form));
		return 0;
	}

	private static int invokeSnowFoxSkill(CommandSourceStack source, ServerPlayer target, String skill, String executorName) {
		return switch (skill) {
			case "melee_primary" -> {
				LOGGER.info("[SSC] Invoking snow_fox/melee_primary on " + target.getName().getString() + " by " + executorName);
				boolean success = SnowFoxSpMeleeAbility.execute(target);
				if (!success) {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.melee_primary.fail").withStyle(ChatFormatting.YELLOW), false);
				} else {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.melee_primary.success").withStyle(ChatFormatting.GREEN), false);
				}
				yield 1;
			}
			case "melee_secondary" -> {
				LOGGER.info("[SSC] Invoking snow_fox/melee_secondary on " + target.getName().getString() + " by " + executorName);
				boolean success = SnowFoxSpTeleportAttack.execute(target);
				if (!success) {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.melee_secondary.fail").withStyle(ChatFormatting.YELLOW), false);
				} else {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.melee_secondary.success").withStyle(ChatFormatting.GREEN), false);
				}
				yield 1;
			}
			case "ranged_primary" -> {
				LOGGER.info("[SSC] Invoking snow_fox/ranged_primary (frost_ball) on " + target.getName().getString() + " by " + executorName);
				boolean success = invokeSnowFoxFrostBall(target);
				if (!success) {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.ranged_primary.fail").withStyle(ChatFormatting.YELLOW), false);
				} else {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.ranged_primary.success").withStyle(ChatFormatting.GREEN), false);
				}
				yield success ? 1 : 0;
			}
			case "ranged_secondary" -> {
				LOGGER.info("[SSC] Invoking snow_fox/ranged_secondary (frost_storm) on " + target.getName().getString() + " by " + executorName);
				boolean success = SnowFoxSpFrostStorm.startCharging(target);
				if (!success) {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.ranged_secondary.fail").withStyle(ChatFormatting.YELLOW), false);
				} else {
					source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.snow_fox.ranged_secondary.success").withStyle(ChatFormatting.GREEN), false);
				}
				yield 1;
			}
default -> {
                LOGGER.warn("[SSC] Unknown snow_fox skill: " + skill);
                source.sendFailure(Component.translatable("command.ssc_addon.ssc_test.unknown_skill", skill, "snow_fox"));
                yield 0;
            }
        };
    }

    private static int invokeAnubisWolfSkill(CommandSourceStack source, ServerPlayer target, String skill, String executorName) {
        return switch (skill) {
            case "summon_wolves" -> {
                LOGGER.info("[SSC] Invoking anubis_wolf/summon_wolves on " + target.getName().getString() + " by " + executorName);
                boolean success = AnubisWolfSpSummonWolves.execute(target);
                if (!success) {
                    source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.anubis_wolf.summon_wolves.fail").withStyle(ChatFormatting.YELLOW), false);
                } else {
                    source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.anubis_wolf.summon_wolves.success").withStyle(ChatFormatting.GREEN), false);
                }
                yield 1;
            }
            case "death_domain" -> {
                LOGGER.info("[SSC] Invoking anubis_wolf/death_domain on " + target.getName().getString() + " by " + executorName);
                boolean success = AnubisWolfSpDeathDomain.execute(target);
                if (!success) {
                    source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.anubis_wolf.death_domain.fail").withStyle(ChatFormatting.YELLOW), false);
                } else {
                    source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.anubis_wolf.death_domain.success").withStyle(ChatFormatting.GREEN), false);
                }
                yield 1;
            }
            default -> {
                LOGGER.warn("[SSC] Unknown anubis_wolf skill: " + skill);
                source.sendFailure(Component.translatable("command.ssc_addon.ssc_test.unknown_skill", skill, "anubis_wolf"));
                yield 0;
            }
        };
    }

	private static int invokeAllaySkill(CommandSourceStack source, ServerPlayer target, String skill, String executorName) {
		return switch (skill) {
			case "jukebox_charge" -> {
				LOGGER.info("[SSC] Invoking allay/jukebox_charge on " + target.getName().getString() + " by " + executorName);
				AllaySPJukebox.tick(target);
				source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.allay.jukebox_charge.triggered").withStyle(ChatFormatting.AQUA), false);
				yield 1;
			}
			case "group_heal" -> {
				LOGGER.info("[SSC] Invoking allay/group_heal on " + target.getName().getString() + " by " + executorName);
				AllaySPGroupHeal.tick(target);
				source.sendSuccess(() -> Component.translatable("command.ssc_addon.ssc_test.allay.group_heal.triggered").withStyle(ChatFormatting.AQUA), false);
				yield 1;
			}
			default -> {
				LOGGER.warn("[SSC] Unknown allay skill: " + skill);
				source.sendFailure(Component.translatable("command.ssc_addon.ssc_test.unknown_skill", skill, "allay"));
				yield 0;
			}
		};
	}

	private static boolean invokeSnowFoxFrostBall(ServerPlayer player) {
		int manaCost = 10;
		int currentMana = PowerUtils.getResourceValue(player, FormIdentifiers.SNOW_FOX_RESOURCE);
		if (currentMana >= manaCost) {
			PowerUtils.changeResourceValueAndSync(player, FormIdentifiers.SNOW_FOX_RESOURCE, -manaCost);
		} else {
			return false;
		}

		FrostBallEntity frostBall = new FrostBallEntity(player.level(), player);
		Vec3 lookDir = player.getLookAngle().normalize();
		Vec3 startPos = player.position().add(lookDir.scale(0.5));
		frostBall.setPos(startPos.x, startPos.y, startPos.z);
		frostBall.setDeltaMovement(lookDir.scale(3.0));
		player.level().addFreshEntity(frostBall);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 1.2f);
		return true;
	}
}
