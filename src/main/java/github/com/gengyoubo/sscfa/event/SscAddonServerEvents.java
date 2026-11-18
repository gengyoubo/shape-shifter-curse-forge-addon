package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.AllaySPGroupHeal;
import github.com.gengyoubo.sscfa.ability.AllaySPJukebox;
import github.com.gengyoubo.sscfa.ability.AllaySPTotem;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpDeathDomain;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpSummonWolves;
import github.com.gengyoubo.sscfa.ability.AxolotlWaterSpurtHandler;
import github.com.gengyoubo.sscfa.ability.BatDesmodusBloodThirst;
import github.com.gengyoubo.sscfa.ability.FluorescentLaserManager;
import github.com.gengyoubo.sscfa.ability.FluorescentTidalManager;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormErosionBrand;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormRegen;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormWitherSand;
import github.com.gengyoubo.sscfa.ability.MancianimaPassive;
import github.com.gengyoubo.sscfa.ability.PlayDeadAbsorptionManager;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpFrostStorm;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpMeleeAbility;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpTeleportAttack;
import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.ability.VortexGuideManager;
import github.com.gengyoubo.sscfa.ability.WaterSpearLeapManager;
import github.com.gengyoubo.sscfa.ability.WildCatPaceManager;
import github.com.gengyoubo.sscfa.ability.WindDashManager;
import github.com.gengyoubo.sscfa.ability.WindSpiritClawManager;
import github.com.gengyoubo.sscfa.ability.WindSpiritLandingSurgeManager;
import github.com.gengyoubo.sscfa.ability.WitherFrenzyManager;
import github.com.gengyoubo.sscfa.effect.StunEffect;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.UndeadNeutralState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Server tick and lifecycle wiring for the form abilities, ported from the Fabric
 * {@code SscAddonServerEvents}:
 * <ul>
 *   <li>{@link #registerTickHandlers}: advances every form's passives and running skills each tick;</li>
 *   <li>{@link #registerStunOrphanCleanup}: water spear cap and orphaned STUN modifier cleanup;</li>
 *   <li>{@link #registerServerLifecycleHandlers}: clears static skill state on start and reload, and
 *       restores death-domain blocks before the world saves.</li>
 * </ul>
 * Fabric's START_WORLD_TICK / START_SERVER_TICK map to the START phase of Forge's level and server
 * tick events; END_DATA_PACK_RELOAD maps to {@link OnDatapackSyncEvent} with no player (a /reload).
 */
public final class SscAddonServerEvents {
	private static final Logger LOGGER = LoggerFactory.getLogger("ssc_addon");
	private static boolean registered;

	private SscAddonServerEvents() {}

	public static synchronized void registerAll() {
		if (registered) return;
		registered = true;
		registerTickHandlers();
		registerStunOrphanCleanup();
		registerServerLifecycleHandlers();
	}

	private static void registerTickHandlers() {
		AllaySPJukebox.init();
		github.com.gengyoubo.sscfa.ability.DeathDomainRecovery.init();
		github.com.gengyoubo.sscfa.ability.RedFormTickManager.init();
		SkillStateMaintenance.init();

		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase != TickEvent.Phase.START) return;
			for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
				github.com.gengyoubo.sscfa.compat.MoistureWaterRecovery.capture(player);
				if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)) {
					github.com.gengyoubo.sscfa.ability.ControlledAbilityInterruption.interrupt(player);
				}
			}
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
			if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel world)) return;
			worldTick(world);
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase != TickEvent.Phase.END) return;
			Collection<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
			// Fluorescent tidal skill: settle cooldowns whose orb/shield vanished without a player handle.
			FluorescentTidalManager.tickPendingCd(players);
			for (ServerPlayer player : players) github.com.gengyoubo.sscfa.compat.MoistureWaterRecovery.restore(player);
		});

		// Moon weaver web / swing, nightmare spook, frost spikes: drop per-player state on disconnect.
		PlayerSessionCleanup.register((handler, server) -> {
			github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager.onDisconnect(handler.player);
			github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.onDisconnect(handler.player);
			github.com.gengyoubo.sscfa.ability.NightmareSpookManager.onDisconnect(handler.player);
			// Orbiting frost spikes leave with the player; their ages are saved and rebuilt on join.
			github.com.gengyoubo.sscfa.ability.FrostSpikeManager.onDisconnect(handler.player);
		});
		github.com.gengyoubo.sscfa.util.SscEvents.onPlayerJoin(player -> {
			github.com.gengyoubo.sscfa.ability.FrostSpikeManager.onJoin(player);
			// VORTEX_STATE persists in NBT but the CHARGING table does not survive a disconnect; a
			// leftover value would leave the client "charging" forever, and nobody can be charging on join.
			if (PowerUtils.getResourceValue(player, VortexChargeManager.VORTEX_STATE) > 0) {
				PowerUtils.setResourceValueAndSync(player, VortexChargeManager.VORTEX_STATE, 0);
			}
		});
		// Nightmare spook: the target attacking the ghost creeper is judged on the real entity.
		github.com.gengyoubo.sscfa.ability.NightmareSpookManager.registerEvents();

		MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
			github.com.gengyoubo.sscfa.block.WebMembraneOwners.clear();
			github.com.gengyoubo.sscfa.compat.MoistureWaterRecovery.clear();
		});
	}

	private static void worldTick(ServerLevel world) {
		MinecraftServer server = world.getServer();
		if (world.dimension() == Level.OVERWORLD) {
			// Disconnected players' domain blocks are restored on the server thread.
			AnubisWolfSpDeathDomain.tickCleanup();
			if (server.getTickCount() % 20 == 0) {
				MancianimaPassive.serverGlobalFleeCleanup(server);
				// Raider group lifecycle (LINGER -> MARCH -> cleanup).
				MancianimaPassive.tickRaiderGroups(server);
			}
		}
		// Frost-armour thorn layers expire per world.
		github.com.gengyoubo.sscfa.ability.FrostArmorManager.tick(world);
		for (ServerPlayer player : world.players()) {
			SnowFoxSpMeleeAbility.tick(player);
			SnowFoxSpTeleportAttack.tick(player);
			SnowFoxSpFrostStorm.tick(player);
			AllaySPGroupHeal.tick(player);
			AllaySPJukebox.tick(player);
			AnubisWolfSpDeathDomain.tick(player);
			AnubisWolfSpSummonWolves.tick(player);
			GoldenSandstormErosionBrand.tick(player);
			GoldenSandstormWitherSand.tick(player);
			GoldenSandstormRegen.tick(player);
			// BatDesmodusBloodThirst.tick runs from BatDesmodusEvents.
			MancianimaPassive.tick(player);
			VortexChargeManager.tick(player);
			// Wild cat day/night pace (persistent modifier instead of re-applied potions).
			WildCatPaceManager.tick(player);
			github.com.gengyoubo.sscfa.ability.FrostSpikeManager.tick(player);
			github.com.gengyoubo.sscfa.ability.JumpKillManager.tick(player);
			github.com.gengyoubo.sscfa.ability.VenomSkillManager.tick(player);
			// Jumping spider safety silk: the 6 s anchor countdown after a jump kill ends.
			github.com.gengyoubo.sscfa.ability.JumpKillManager.tickAnchors(player);
			github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager.tick(player);
			github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.tick(player);
			github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverMoonPoisonManager.tick(player);
			WindSpiritClawManager.tick(player);
			WindDashManager.tick(player);
			WindSpiritLandingSurgeManager.tick(player);
			WaterSpearLeapManager.tick(player);
			VortexGuideManager.tick(player);
			AxolotlWaterSpurtHandler.tick(player);
			PlayDeadAbsorptionManager.tick(player);
			FluorescentTidalManager.tick(player);
			FluorescentLaserManager.tick(player);
			// Netherjudge wither ladder / wither resistance tracking.
			WitherFrenzyManager.tick(player);
			github.com.gengyoubo.sscfa.ability.NightmareDreamManager.tick(player);
			github.com.gengyoubo.sscfa.ability.NightmareFearManager.tick(player);
			github.com.gengyoubo.sscfa.ability.NightmareSpookManager.tick(player);
			github.com.gengyoubo.sscfa.evolution.EvolutionManager.tickPlayer(player);
			// AddonAccessoryGuard.tick runs from SscStateEvents.
			// Red familiar / SP periodic logic (formerly RedFormTickMixin; gated to once a second inside).
			github.com.gengyoubo.sscfa.ability.RedFormTickManager.tick(player);
		}
	}

	/**
	 * Clears orphaned STUN attack/speed modifiers. STUN applies fixed-UUID -100% modifiers; if the
	 * effect is removed outside the normal path (e.g. a form change clearing effects) the modifier
	 * stays behind and the player deals no damage. Also caps the axolotl's water spear at one.
	 */
	private static void registerStunOrphanCleanup() {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase != TickEvent.Phase.END) return;
			MinecraftServer server = event.getServer();
			if (server.getTickCount() % 10 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (FormUtils.isAxolotlSP(player)) capWaterSpear(server, player);
				// Independent of the sheep restore snapshot: clear a finished sheep modifier even under STUN.
				github.com.gengyoubo.sscfa.effect.SheepFormRestoreHandler.clearOrphanedAttackModifier(player);
				if (player.hasEffect(AddonEffects.STUN.get())) continue;
				AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
				if (atk != null && atk.getModifier(StunEffect.ATTACK_MODIFIER_UUID) != null) {
					atk.removeModifier(StunEffect.ATTACK_MODIFIER_UUID);
				}
				AttributeInstance spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
				if (spd != null && spd.getModifier(StunEffect.SPEED_MODIFIER_UUID) != null) {
					spd.removeModifier(StunEffect.SPEED_MODIFIER_UUID);
				}
			}
		});
	}

	/** At most one water spear; when the last one leaves the inventory the craft cooldown restarts. */
	private static void capWaterSpear(MinecraftServer server, ServerPlayer player) {
		Inventory inv = player.getInventory();
		int count = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).is(SscAddon.WATER_SPEAR)) {
				count++;
				if (count > 1) {
					inv.setItem(i, ItemStack.EMPTY);
					count--;
				}
			}
		}
		// A spear held on the cursor is still in hand; count it before comparing with the last scan.
		var menu = player.containerMenu;
		if (menu != null && menu.getCarried().is(SscAddon.WATER_SPEAR)) count += menu.getCarried().getCount();
		Integer previous = SscAddon.WS_LAST_SPEAR_COUNT.put(player.getUUID(), count);
		// Spear gone (thrown or used): the craft cooldown starts now, not when the spear was made.
		if (previous != null && previous > 0 && count == 0) {
			SscAddon.WATER_SPEAR_CRAFT_CD.put(player.getUUID(), server.getTickCount() + (long) SscAddon.WATER_SPEAR_CRAFT_CD_TICKS);
			player.getCooldowns().addCooldown(Items.ARROW, SscAddon.WATER_SPEAR_CRAFT_CD_TICKS);
			PowerUtils.resetCooldown(player, ResourceLocation.fromNamespaceAndPath("my_addon", "form_axolotl_sp_water_spear_craft_spear"));
		}
	}

	private static void registerServerLifecycleHandlers() {
		// Before worlds load: clear all static skill state.
		MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> {
			MinecraftServer server = event.getServer();
			SnowFoxSpMeleeAbility.clearAll();
			SnowFoxSpTeleportAttack.clearAll();
			SnowFoxSpFrostStorm.resetForServerStart();
			github.com.gengyoubo.sscfa.ability.FrostArmorManager.clearAll();
			AnubisWolfSpDeathDomain.clearAll();
			AnubisWolfSpSummonWolves.clearAll(server);
			AllaySPTotem.clearAll();
			GoldenSandstormErosionBrand.clearAll();
			GoldenSandstormWitherSand.clearAll(server);
			GoldenSandstormRegen.clearAll();
			BatDesmodusBloodThirst.clearAll();
			UndeadNeutralState.clearAll();
			MancianimaPassive.clearAll();
			FluorescentLaserManager.clearAll();
			FluorescentTidalManager.clearAll();
			github.com.gengyoubo.sscfa.ability.JumpKillManager.clearAll();
			github.com.gengyoubo.sscfa.ability.VenomSkillManager.clearAll();
			github.com.gengyoubo.sscfa.ability.NightmareSpookManager.clearAll(server);
			LOGGER.debug("[SSC_ADDON] server starting: ability state cleared");
		});
		// Before the world saves: put back every death-domain block.
		MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> AnubisWolfSpDeathDomain.forceRestoreAll());
		// After a successful /reload (Forge fires this with no player once the new data is live).
		MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() != null) return;
			MinecraftServer server = event.getPlayerList().getServer();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				github.com.gengyoubo.sscfa.ability.ControlledAbilityInterruption.cancelTransient(player);
			}
			SnowFoxSpMeleeAbility.clearAll();
			SnowFoxSpTeleportAttack.clearAll();
			SnowFoxSpFrostStorm.clearAll();
			// A domain may be up during the reload: restore its blocks before clearing state.
			AnubisWolfSpDeathDomain.forceRestoreAll(true);
			AnubisWolfSpSummonWolves.clearAll(server);
			AllaySPTotem.clearAll();
			GoldenSandstormErosionBrand.clearAll();
			GoldenSandstormWitherSand.clearAll(server);
			GoldenSandstormRegen.pruneSources(server.overworld().getGameTime());
			BatDesmodusBloodThirst.clearAll();
			UndeadNeutralState.clearAll();
			MancianimaPassive.clearAll();
			FluorescentLaserManager.clearAll();
			FluorescentTidalManager.clearAll();
			github.com.gengyoubo.sscfa.ability.JumpKillManager.clearAll();
			github.com.gengyoubo.sscfa.ability.NightmareSpookManager.clearAll(server);
			LOGGER.debug("[SSC_ADDON] datapack reload: ability state cleared");
		});
	}
}
