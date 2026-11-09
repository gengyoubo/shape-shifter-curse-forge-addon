package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SpellbookStatusSync;
import github.com.gengyoubo.sscfa.spell.research.RuneCastContext;
import github.com.gengyoubo.sscfa.spell.research.RuneModifiers;
import github.com.gengyoubo.sscfa.spell.research.RuneScheme;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * 鏂芥硶璇绘潯绠＄悊鍣紙Forge 绠€鍖栫増锛夛細璧锋墜鏍￠獙銆丟CD銆佽鏉℃帹杩涖€侀噴鏀?鎵撴柇銆佸喎鍗寸粨绠椼€佺Щ鍔ㄥ噺閫熶笌鐘舵€佸悓姝ャ€? * 褰㈡€佸崗鍚?宕╄В锛圢eoMagic锛夌瓑楂樼骇缁撶畻寰呭搴旂郴缁熺Щ妞嶅悗琛ュ洖銆? */
public final class SpellChannelManager {
	public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "spell_channel_state");
	public static final ResourceLocation RELEASE = new ResourceLocation("ssc_addon", "spell_channel_release");
	public static final ResourceLocation CANCEL_HOLD = new ResourceLocation("ssc_addon", "spell_channel_cancel_hold");
	public static final ResourceLocation SUPPRESSION = new ResourceLocation("ssc_addon", "spell_channel_suppression");
	public static final ResourceLocation CAST_VISUAL = new ResourceLocation("ssc_addon", "spell_cast_visual");
	private static final Set<SpellCastingRules.Tier> NO_ARM_POSE_TIERS =
			Set.of(SpellCastingRules.Tier.INSTANT, SpellCastingRules.Tier.BASIC_1, SpellCastingRules.Tier.BASIC_2);
	private static final UUID SLOW_ID = UUID.fromString("e178a586-4244-4fa7-a8b2-38a6a436d1d5");
	private static final Map<UUID, Channel> ACTIVE = new HashMap<>();
	private static UUID clientImmobile;
	public static final int CAST_INTERVAL_TICKS = 16;
	private static final BalanceReader BAL = new BalanceReader("systems.casting");
	private static final Map<UUID, Long> NEXT_CAST_OK = new HashMap<>();
	private static final int CALIBRATE_INTERVAL = 20;

	private SpellChannelManager() {}

	public static void init() {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
			for (Channel channel : java.util.List.copyOf(ACTIVE.values())) tick(channel);
		});
		AddonNetwork.registerServerReceiver(RELEASE, (player, buf) -> release(player, buf.readVarInt(), false));
		AddonNetwork.registerServerReceiver(CANCEL_HOLD, (player, buf) -> {
			int token = buf.readVarInt();
			boolean held = buf.readBoolean();
			Channel channel = ACTIVE.get(player.getUUID());
			if (channel != null && channel.spell.isLockedIn(player)) return;
			if (channel != null && !channel.solo && channel.token == token
					&& (channel.mode == SpellCastingRules.Mode.AUTOMATIC
					|| channel.mode == SpellCastingRules.Mode.RELEASE)) {
				channel.cancelHeld = held;
				if (!held) channel.cancelTicks = 0;
			}
		});
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((connection, server) -> {
			stop(connection.player, true);
			NEXT_CAST_OK.remove(connection.player.getUUID());
		});
		MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) clearSlow(player);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
			if (event.getEntity() instanceof ServerPlayer player) cancelSelf(player);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
			if (event.getEntity() instanceof ServerPlayer player) cancelSelf(player);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (event.getEntity() instanceof ServerPlayer player) cancelSelf(player);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
			if (event.getEntity() instanceof ServerPlayer player) cancelSelf(player);
		});
		MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
			for (Channel channel : java.util.List.copyOf(ACTIVE.values())) stop(channel.player, true);
		});
	}

	public static boolean isCasting(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
	public static boolean releaseLocked(ServerPlayer player) {
		Channel channel = ACTIVE.get(player.getUUID());
		return channel != null && channel.spell.isLockedIn(player);
	}
	public static boolean onSkillAction(ServerPlayer player) { cancelSelf(player); return true; }
	public static long getNextCastAt(ServerPlayer player) { return NEXT_CAST_OK.getOrDefault(player.getUUID(), 0L); }
	public static void clearCastGate(ServerPlayer player) {
		NEXT_CAST_OK.remove(player.getUUID());
		SpellbookStatusSync.sync(player);
	}
	public static void setClientImmobile(UUID player) { clientImmobile = player; }
	public static boolean isImmobile(LivingEntity entity) {
		if (entity.level().isClientSide) return entity.getUUID().equals(clientImmobile);
		Channel channel = ACTIVE.get(entity.getUUID());
		return channel != null && channel.profile.immobilized();
	}

	public static boolean start(ServerPlayer player, Spell spell, ItemStack scroll, int level,
			boolean solo, int token, int mana, int cooldown, BooleanSupplier sourceValid,
			Consumer<Vec3> effect, IntConsumer settleCooldown) {
		return start(player, spell, scroll, level, solo, token, mana, cooldown, sourceValid, effect, settleCooldown, () -> {});
	}
	public static boolean start(ServerPlayer player, Spell spell, ItemStack scroll, int level,
			boolean solo, int token, int mana, int cooldown, BooleanSupplier sourceValid,
			Consumer<Vec3> effect, IntConsumer settleCooldown, Runnable consumeUse) {
		return start(player, spell, scroll, level, solo, token, mana, cooldown, sourceValid, effect, settleCooldown, consumeUse,
				github.com.gengyoubo.sscfa.spell.research.RuneScheme.modifiers(player, scroll, solo ? level : ScrollData.getCastLevel(scroll)));
	}
	public static boolean start(ServerPlayer player, Spell spell, ItemStack scroll, int level,
			boolean solo, int token, int mana, int cooldown, BooleanSupplier sourceValid,
			Consumer<Vec3> effect, IntConsumer settleCooldown, Runnable consumeUse, RuneModifiers modifiers) {
		return startInternal(player, spell, scroll, level, solo, token, mana, cooldown, sourceValid, effect, settleCooldown, consumeUse, modifiers);
	}

	private static boolean startInternal(ServerPlayer player, Spell spell, ItemStack scroll, int level,
			boolean solo, int token, int mana, int cooldown, BooleanSupplier sourceValid, Consumer<Vec3> effect,
			IntConsumer settleCooldown, Runnable consumeUse, RuneModifiers modifiers) {
		if (!github.com.gengyoubo.sscfa.spell.research.RuneScheme.validScroll(player, scroll, spell)) return false;
		if (ArcaneAnalysis.isUnanalyzed(scroll)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.analysis.required"), true);
			return false;
		}
		if (!github.com.gengyoubo.sscfa.balance.BalanceIntegration.isPlayerReady(player)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.spellbook.data_syncing"), true);
			return false;
		}
		if (isCasting(player) || !player.isAlive() || player.isSpectator()) return false;
		if (player.hasEffect(AddonEffects.PURIFIED.get())) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.spell.purified_blocked"), true);
			return false;
		}
		if (spell.isForbiddenInsideDomain() && DomainManager.enclosedTrapping(player.serverLevel(), player.position())) {
			playFailureSound(player);
			player.displayClientMessage(spell.getDomainForbiddenMessage(), true);
			return false;
		}
		if (!spell.getConfig().manaCostConfigured) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.spellbook.cost_unavailable"), true);
			return false;
		}
		long gate = NEXT_CAST_OK.getOrDefault(player.getUUID(), 0L);
		if (player.level().getGameTime() < gate) {
			playNoManaSound(player);
			player.displayClientMessage(Component.translatable("message.ssc_addon.spellbook.cast_interval",
					SpellCastFeedback.seconds(gate - player.level().getGameTime())), true);
			return false;
		}
		ItemStack equipped = solo ? null : SpellCastManager.getEquippedBook(player);
		double formationRange = equipped == null || equipped.isEmpty() ? 1 : FormationData.sumSpaceRangeMultiplier(equipped, spell.getElement());
		Vec3 initialTarget = null;
		if (spell.requiresTargetBeforeChannel()) {
			initialTarget = RuneCastContext.with(modifiers, formationRange, () -> spell.captureCastTarget(player, level));
			if (initialTarget == null || spell.getCastingMode() != SpellCastingRules.Mode.RELEASE) {
				player.displayClientMessage(spell.getCastFailureMessage(player), true);
				return false;
			}
		}
		var baseProfile = spell.getCastingProfile(player, level, solo);
		var style = RuneCastContext.with(modifiers, formationRange,
				() -> FormSpellStyle.plan(player, spell, level, baseProfile.ticks(), modifiers.time(baseProfile.ticks())));
		double lockRange = RuneCastContext.with(modifiers, formationRange, () -> spell.getCastingLockRange(player, level));
		if (!style.pay()) return false;
		Channel channel = new Channel(player, spell, scroll, level, solo, token, mana, cooldown,
				sourceValid, effect, settleCooldown, consumeUse, modifiers, style, lockRange);
		if (spell.requiresTargetBeforeChannel()) channel.progress.release(token, initialTarget);
		ACTIVE.put(player.getUUID(), channel);
		RuneCastContext.run(channel.modifiers, channel.formationRange, () -> spell.onChannelStarted(player, channel.progress.target()));
		channel.bindLock(initialTarget);
		applyMovement(player, channel.profile.speedMultiplier());
		syncFull(channel);
		broadcastVisual(channel, true);
		playChargeSound(channel);
		advance(channel);
		if (solo) SpellbookStatusSync.sync(player);
		return true;
	}

	public static void release(ServerPlayer player, int token, boolean solo) {
		Channel channel = ACTIVE.get(player.getUUID());
		if (channel == null || channel.solo != solo || channel.token != token || channel.progress.released()) return;
		if (!ensureValid(channel)) return;
		if (channel.mode == SpellCastingRules.Mode.AUTOMATIC) return;
		if (channel.mode == SpellCastingRules.Mode.CONTINUOUS) {
			stop(player, !channel.progress.started());
			return;
		}
		Vec3 target = RuneCastContext.with(channel.modifiers, channel.formationRange,
				() -> channel.spell.captureCastTarget(player, channel.level));
		if (channel.spell.getAimMaxRange() > 0 && target == null) {
			stop(player, true, channel.spell.getCastFailureMessage(player));
			return;
		}
		if (channel.progress.release(token, target)) { channel.bindLock(target); advance(channel); }
	}

	public static void cancelSelf(ServerPlayer player) {
		Channel channel = ACTIVE.get(player.getUUID());
		if (channel != null && !channel.spell.isLockedIn(player)
				&& SpellCastingRules.allowsSelf(channel.interruptMode)) stop(player, true, "message.ssc_addon.spell.cast_interrupted_action");
	}
	public static void onDamaged(ServerPlayer player) {
		Channel channel = ACTIVE.get(player.getUUID());
		if (channel != null && (!player.isAlive()
				|| !channel.spell.isLockedIn(player) && SpellCastingRules.allowsExternal(channel.interruptMode))) stop(player, true, "message.ssc_addon.spell.cast_interrupted_damage");
	}
	public static void onPurified(ServerPlayer player) {
		stop(player, true, Component.translatable("message.ssc_addon.spell.cast_interrupted_purified"), true);
	}
	public static void onFullyControlled(ServerPlayer player) {
		stop(player, true, "message.ssc_addon.spell.cast_interrupted_action");
	}
	public static void onFrozen(ServerPlayer player) {
		stop(player, true, Component.translatable("message.ssc_addon.spell.cast_interrupted_frozen"), true);
	}

	private static boolean ensureValid(Channel channel) {
		ServerPlayer player = channel.player;
		if (!player.isAlive() || player.isRemoved() || player.isSpectator()
				|| !player.level().dimension().equals(channel.dimension)
				|| !channel.sourceValid.getAsBoolean()
				|| !RuneCastContext.with(channel.modifiers, channel.formationRange,
						() -> channel.spell.canContinueCasting(player, channel.scroll))) {
			stop(player, true);
			return false;
		}
		if (player.hasEffect(AddonEffects.PURIFIED.get())) {
			onPurified(player);
			return false;
		}
		return true;
	}

	private static void tick(Channel channel) {
		ServerPlayer player = channel.player;
		if (ACTIVE.get(player.getUUID()) != channel) return;
		if (player.getServer().getTickCount() <= channel.beganTick) return;
		if (!ensureValid(channel)) return;
		if (channel.cancelHeld && ++channel.cancelTicks >= BAL.i("cancel_hold_ticks", 20)
				&& !channel.spell.isLockedIn(player)
				&& SpellCastingRules.allowsSelf(channel.interruptMode)) {
			stop(player, true, "message.ssc_addon.spell.cast_cancelled");
			return;
		}
		if (channel.progress.started()) {
			if (!channel.spell.tickContinuousCast(player, channel.level, channel.scroll, ++channel.continuousTicks)) {
				stop(player, false);
				return;
			}
		} else {
			channel.progress.tick();
			RuneCastContext.run(channel.modifiers, channel.formationRange,
					() -> channel.spell.tickChannel(player, channel.level, channel.scroll, channel.style.timing.logical(channel.progress.elapsed())));
			advance(channel);
		}
		boolean lockedNow = channel.spell.isLockedIn(player);
		boolean lockBegun = lockedNow && !channel.lockedAnnounced;
		if (lockedNow) channel.lockedAnnounced = true;
		if (ACTIVE.get(player.getUUID()) == channel) {
			if (lockBegun && channel.progress.elapsed() % CALIBRATE_INTERVAL != 0) sendCalibration(channel);
			syncCalibration(channel);
		}
		if (ACTIVE.get(player.getUUID()) == channel && ++channel.visualTicks % 20 == 0) broadcastVisual(channel, true);
	}

	private static void advance(Channel channel) {
		if (ACTIVE.get(channel.player.getUUID()) != channel) return;
		if (!ensureValid(channel)) return;
		if (!RuneCastContext.with(channel.modifiers, channel.formationRange,
				() -> channel.spell.readyToRelease(channel.player, channel.scroll)) || !channel.progress.beginEffect()) return;
		if (channel.mode == SpellCastingRules.Mode.CONTINUOUS) {
			NEXT_CAST_OK.put(channel.player.getUUID(), channel.player.level().getGameTime() + BAL.i("gcd_ticks", CAST_INTERVAL_TICKS));
			channel.effect.accept(channel.progress.target());
		} else {
			NEXT_CAST_OK.put(channel.player.getUUID(), channel.player.level().getGameTime() + BAL.i("gcd_ticks", CAST_INTERVAL_TICKS));
			stop(channel.player, false);
			channel.effect.accept(channel.progress.target());
		}
	}

	private static void stop(ServerPlayer player, boolean interrupted) {
		stop(player, interrupted, "message.ssc_addon.spell.cast_interrupted");
	}
	private static void stop(ServerPlayer player, boolean interrupted, String reason) {
		stop(player, interrupted, Component.translatable(reason));
	}
	private static void stop(ServerPlayer player, boolean interrupted, Component reason) {
		stop(player, interrupted, reason, false);
	}
	private static void stop(ServerPlayer player, boolean interrupted, Component reason, boolean fullCooldown) {
		Channel channel = ACTIVE.remove(player.getUUID());
		if (channel == null) return;
		clearSlow(player);
		broadcastVisual(channel, false);
		channel.spell.onChannelEnded(player, interrupted);
		channel.settleCooldown.accept(SpellCastingRules.cooldownAfterStop(channel.spell, channel.cooldown, interrupted, fullCooldown));
		if (!interrupted || channel.progress.started()) channel.consumeUse.run();
		if (channel.progress.started() && channel.mode == SpellCastingRules.Mode.CONTINUOUS) {
			channel.spell.endContinuousCast(player, channel.level, interrupted);
		}
		if (AddonNetwork.canSend(player, STATE)) {
			FriendlyByteBuf buf = AddonNetwork.buffer();
			buf.writeBoolean(false);
			AddonNetwork.sendToPlayer(player, STATE, buf);
		}
		if (interrupted) {
			playFailureSound(player);
			player.displayClientMessage(reason.copy(), true);
		}
		if (interrupted || channel.solo || channel.mode == SpellCastingRules.Mode.CONTINUOUS) SpellbookStatusSync.sync(player);
	}

	public static void playFailureSound(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 0.65f);
	}
	public static void playNoManaSound(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.9f, 0.9f);
	}
	private static void playChargeSound(Channel channel) {
		if (channel.progress.elapsed() != 0 || !SpellCastingRules.playsChargeStartSound(channel.profile.ticks())) return;
		ServerPlayer player = channel.player;
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.75f, 1.2f);
		if (!SpellCastingRules.playsChargeSound(channel.profile.ticks())) return;
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 0.65f);
	}

	public static void applyMovement(ServerPlayer player, double multiplier) {
		var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW_ID);
			speed.addTransientModifier(new AttributeModifier(SLOW_ID, "Spell casting", multiplier - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}
	public static void clearSlow(ServerPlayer player) {
		var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.removeModifier(SLOW_ID);
	}

	private static Set<ServerPlayer> tracking(ServerPlayer player) {
		Set<ServerPlayer> audience = new HashSet<>();
		audience.add(player);
		for (net.minecraft.world.entity.player.Player other : player.level().players()) {
			if (other instanceof ServerPlayer server && server.distanceToSqr(player) <= 64 * 64) audience.add(server);
		}
		return audience;
	}

	private static void broadcastVisual(Channel channel, boolean active) {
		for (ServerPlayer viewer : tracking(channel.player)) {
			if (!AddonNetwork.canSend(viewer, CAST_VISUAL)) continue;
			FriendlyByteBuf buf = AddonNetwork.buffer();
			buf.writeUUID(channel.player.getUUID());
			buf.writeBoolean(active);
			if (active) {
				buf.writeBoolean(!NO_ARM_POSE_TIERS.contains(channel.spell.getConfig().spellTier));
				buf.writeInt(channel.spell.getElement().color);
				buf.writeInt(channel.spell.getRarity(channel.level).color.getColor());
				buf.writeResourceLocation(channel.spell.getId());
			}
			AddonNetwork.sendToPlayer(viewer, CAST_VISUAL, buf);
		}
	}
	private static void syncCalibration(Channel channel) {
		if (channel.progress.elapsed() % CALIBRATE_INTERVAL != 0) return;
		sendCalibration(channel);
	}
	private static void sendCalibration(Channel channel) {
		if (!AddonNetwork.canSend(channel.player, STATE)) return;
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeBoolean(true);
		buf.writeBoolean(false);
		buf.writeVarInt(channel.token);
		buf.writeVarInt(channel.progress.elapsed());
		buf.writeBoolean(channel.progress.released());
		buf.writeVarInt(channel.cancelTicks);
		buf.writeBoolean(channel.spell.isLockedIn(channel.player));
		AddonNetwork.sendToPlayer(channel.player, STATE, buf);
	}
	private static void syncFull(Channel channel) {
		if (!AddonNetwork.canSend(channel.player, STATE)) return;
		AddonNetwork.sendToPlayer(channel.player, STATE, fullStatePacket(channel));
	}
	private static FriendlyByteBuf fullStatePacket(Channel channel) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeBoolean(true);
		buf.writeBoolean(true);
		buf.writeResourceLocation(channel.spell.getId());
		buf.writeUUID(channel.player.getUUID());
		buf.writeVarInt(channel.token);
		buf.writeVarInt(channel.progress.elapsed());
		buf.writeVarInt(channel.profile.ticks());
		buf.writeEnum(channel.mode);
		buf.writeBoolean(channel.progress.released());
		buf.writeBoolean(channel.profile.immobilized());
		buf.writeBoolean(channel.solo);
		buf.writeBoolean(channel.progress.started());
		buf.writeVarInt(channel.cancelTicks);
		buf.writeBoolean(channel.spell.isLockedIn(channel.player));
		buf.writeVarInt(channel.level);
		buf.writeVarInt(channel.style.timing.lockTick());
		buf.writeFloat((float) channel.style.projectileRange);
		return buf;
	}

	public static FormSpellStyle.Timing animationTiming(ServerPlayer player) {
		var channel = ACTIVE.get(player.getUUID());
		return channel == null ? null : channel.style.timing;
	}
	public static double projectileRangeSnapshot(ServerPlayer player) {
		var channel = ACTIVE.get(player.getUUID());
		return channel == null ? Double.NaN : channel.style.projectileRange;
	}

	private static final class Channel {
		final ServerPlayer player;
		final Spell spell;
		final FormSpellStyle style;
		final RuneModifiers modifiers;
		final double formationRange;
		final double lockRange;
		CastRangeGuard.Lock targetLock;
		final ItemStack scroll;
		final int level, token, cooldown, interruptMode;
		final boolean solo;
		final SpellCastingRules.Profile profile;
		final SpellCastingRules.Mode mode;
		final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
		final BooleanSupplier sourceValid;
		final Consumer<Vec3> effect;
		final IntConsumer settleCooldown;
		final Runnable consumeUse;
		final SpellCastingRules.Progress<Vec3> progress;
		final int beganTick;
		int cancelTicks, continuousTicks, visualTicks;
		boolean cancelHeld;
		boolean lockedAnnounced;

		Channel(ServerPlayer player, Spell spell, ItemStack scroll, int level, boolean solo, int token,
				int mana, int cooldown, BooleanSupplier sourceValid, Consumer<Vec3> effect,
				IntConsumer settleCooldown, Runnable consumeUse, RuneModifiers modifiers,
				FormSpellStyle style, double lockRange) {
			this.style = style;
			this.lockRange = lockRange;
			this.modifiers = modifiers;
			ItemStack equipped = solo ? null : SpellCastManager.getEquippedBook(player);
			this.formationRange = equipped == null || equipped.isEmpty() ? 1 : FormationData.sumSpaceRangeMultiplier(equipped, spell.getElement());
			this.player = player;
			this.spell = spell;
			this.scroll = scroll;
			this.level = level;
			this.solo = solo;
			this.token = token;
			this.cooldown = cooldown;
			this.sourceValid = sourceValid;
			this.effect = target -> style.effect(() -> RuneCastContext.run(modifiers, this.formationRange, () -> effect.accept(target)));
			this.settleCooldown = settleCooldown;
			this.consumeUse = consumeUse;
			var baseProfile = spell.getCastingProfile(player, level, solo);
			this.profile = new SpellCastingRules.Profile(style.timing.duration(), baseProfile.speedMultiplier(), baseProfile.immobilized());
			this.mode = spell.getCastingMode();
			this.progress = new SpellCastingRules.Progress<>(this.mode, this.profile.ticks(), token);
			this.beganTick = player.getServer().getTickCount();
			this.interruptMode = spell.getConfig().interruptMode;
			this.dimension = player.level().dimension();
		}
		void bindLock(Vec3 target) {
			var entity = spell.getCastingLockEntity(player);
			targetLock = entity == null ? CastRangeGuard.point(target, lockRange) : CastRangeGuard.entity(entity, lockRange);
		}
	}
}
