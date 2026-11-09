package github.com.gengyoubo.sscfa.spell;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 领域壳的权威几何与生命周期。Forge 版把 Fabric 的 tick/连接/交互回调映射到 Forge 事件，
 * 网络下发走 {@link AddonNetwork}。异常脉冲（anomaly pressure）依赖异常系统，待其移植后补回。
 */
public final class DomainManager {
	public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "domain_state");
	public static final ResourceLocation SOUND = new ResourceLocation("ssc_addon", "domain_sound");
	private static final Map<UUID, Field> FIELDS = new LinkedHashMap<>();
    private static final github.com.gengyoubo.sscfa.network.SnapshotAudience AUDIENCE =
            new github.com.gengyoubo.sscfa.network.SnapshotAudience();
	private static final ThreadLocal<Entity> EFFECT_SOURCE = new ThreadLocal<>();
	private static final ThreadLocal<AttachedEffect> ATTACHED_EFFECT = new ThreadLocal<>();
	private record AttachedEffect(Entity source, LivingEntity target) {}
	private DomainManager() {}

	private record Field(ServerPlayer owner, ServerLevel world, Vec3 center,
	                     double headHeight, int startTick, boolean active,
	                     double innerRadius, double outerRadius, int chargeTicks, int durationTicks,
	                     int expandStartTick, int expandDurationTicks, double maxCastDisplacement) {
		int elapsed() { return world.getServer().getTickCount() - startTick; }
		double chargingRadius() {
			return active ? 0 : DomainRules.expansionRadius(elapsed(), expandStartTick, chargeTicks,
					expandDurationTicks, innerRadius);
		}
	}

	private static final Map<UUID, Vec3> CHARGE_ANCHORS = new LinkedHashMap<>();

	public static void init() {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) tick(event.getServer());
		});
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((connection, server) -> {
			try { remove(connection.player); }
			finally { AUDIENCE.disconnect(connection.player.getUUID(), connection); }
		});
		MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
			FIELDS.clear();
			CHARGE_ANCHORS.clear();
			AUDIENCE.clear();
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (event.getEntity() instanceof ServerPlayer serverPlayer
					&& blocksCrossBoundary(serverPlayer.serverLevel(), serverPlayer.getEyePosition(),
							Vec3.atCenterOf(event.getPos()))) {
				event.setCanceled(true);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
			if (event.getEntity() instanceof ServerPlayer serverPlayer
					&& blocksCrossBoundary(serverPlayer.serverLevel(), serverPlayer.getEyePosition(),
							Vec3.atCenterOf(event.getPos()))) {
				event.setCanceled(true);
			}
		});
	}

	public static boolean blocksCrossBoundary(ServerLevel world, Vec3 from, Vec3 to) {
		for (Field field : FIELDS.values()) {
			if (field.world != world) continue;
			double inner = field.active ? field.innerRadius : field.chargingRadius();
			if (inner <= 0.1) continue;
			if (DomainRules.separates(from.subtract(field.center), to.subtract(field.center), inner + 1)) return true;
		}
		return false;
	}

	public static volatile java.util.function.BiPredicate<Vec3, Vec3> clientBoundaryCheck;

	public static boolean blocksAimBoundary(Level world, Vec3 from, Vec3 to) {
		if (world instanceof ServerLevel serverWorld) {
			return blocksCrossBoundary(serverWorld, from, to);
		}
		java.util.function.BiPredicate<Vec3, Vec3> check = clientBoundaryCheck;
		return check != null && check.test(from, to);
	}
	public static boolean blocksTargeting(Entity source, Entity target) {
		if (source == null || target == null || source == target || !(source.level() instanceof ServerLevel world)) return false;
		if (target.level() == world) return blocksCrossBoundary(world, source.position(), target.position());
		return enclosedForTargeting(world, source.position()) || enclosedForTargeting(target.level(), target.position());
	}
	private static boolean enclosedForTargeting(Level world, Vec3 position) {
		for (Field field : FIELDS.values()) {
			if (field.world != world) continue;
			double inner = field.active ? field.innerRadius : field.chargingRadius();
			if (inner > 0.1 && field.center.distanceToSqr(position) <= (inner + 1) * (inner + 1)) return true;
		}
		return false;
	}
	public static boolean canStart(ServerPlayer player) {
		if (FIELDS.containsKey(player.getUUID())) return false;
		return !enclosedTrapping(player.serverLevel(), player.position());
	}
	public static void begin(ServerPlayer player) {
		CHARGE_ANCHORS.put(player.getUUID(), player.position());
		Field field = new Field(player, player.serverLevel(), player.position(),
				player.getBbHeight() + 0.8, player.getServer().getTickCount(), false,
				DomainRules.innerRadius(), DomainRules.outerRadius(),
				DomainRules.chargeTicks(), DomainRules.durationTicks(),
				DomainRules.expandStartTick(), DomainRules.expandDurationTicks(),
				DomainRules.maxCastDisplacement());
		FIELDS.put(player.getUUID(), field);
		if (SpellCastingRules.playsChargeSound(field.chargeTicks)) broadcast(field, SoundEvents.BEACON_ACTIVATE, 0.65f, 1.0f);
		sync(player.getServer());
	}
	public static boolean canContinue(ServerPlayer player) {
		Field field = FIELDS.get(player.getUUID());
		Vec3 anchor = CHARGE_ANCHORS.get(player.getUUID());
		if (isExpanding(player)) return true;
		return field != null && anchor != null && !field.active && player.level() == field.world
				&& player.position().distanceToSqr(anchor)
				<= field.maxCastDisplacement * field.maxCastDisplacement;
	}

	public static boolean isExpanding(ServerPlayer player) {
		Field field = FIELDS.get(player.getUUID());
		return field != null && field.elapsed() >= field.expandStartTick;
	}
	public static void activate(ServerPlayer player) {
		Field field = FIELDS.get(player.getUUID());
		if (field == null || field.active) return;
		CHARGE_ANCHORS.remove(player.getUUID());
		FIELDS.put(player.getUUID(), new Field(player, field.world, field.owner.position(), field.headHeight,
				player.getServer().getTickCount(), true,
				field.innerRadius, field.outerRadius, field.chargeTicks, field.durationTicks,
				field.expandStartTick, field.expandDurationTicks, field.maxCastDisplacement));
		broadcast(field, SoundEvents.WITHER_SPAWN, 0.55f, 0.5f);
		sync(player.getServer());
	}
	public static void remove(ServerPlayer player) {
		CHARGE_ANCHORS.remove(player.getUUID());
		Field field = FIELDS.remove(player.getUUID());
		if (field == null) return;
		if (field.active) broadcast(field, SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.0f);
		sync(player.getServer());
	}

	private static void broadcast(Field field, net.minecraft.sounds.SoundEvent sound, float pitch, float volumeScale) {
		Vec3 position = field.owner.level() == field.world ? field.owner.position() : field.center;
		long seed = field.world.getRandom().nextLong();
		for (ServerPlayer listener : field.world.players()) {
			if (listener.position().distanceToSqr(position) >= DomainRules.SOUND_RANGE * DomainRules.SOUND_RANGE
					|| !AddonNetwork.canSend(listener, SOUND)) continue;
			var buf = AddonNetwork.buffer();
			buf.writeResourceLocation(field.world.dimension().location());
			buf.writeUUID(field.owner.getUUID());
			buf.writeResourceLocation(sound.getLocation());
			buf.writeDouble(position.x);
			buf.writeDouble(position.y);
			buf.writeDouble(position.z);
			buf.writeFloat(pitch);
			buf.writeFloat(volumeScale);
			buf.writeLong(seed);
			buf.writeDouble(field.innerRadius);
			buf.writeDouble(field.outerRadius);
			AddonNetwork.sendToPlayer(listener, SOUND, buf);
		}
	}
	private static void tick(MinecraftServer server) {
		if (FIELDS.isEmpty()) return;
		for (Field field : java.util.List.copyOf(FIELDS.values())) {
			if (!field.owner.isAlive() || field.owner.isRemoved() || field.owner.isSpectator()
					|| field.owner.level() != field.world
					|| field.active && field.elapsed() >= field.durationTicks) {
				CHARGE_ANCHORS.remove(field.owner.getUUID());
				remove(field.owner);
			}
		}
		for (var entry : java.util.List.copyOf(FIELDS.entrySet())) {
			Field field = entry.getValue();
			if (field.active) continue;
			Vec3 current = field.owner.position();
			if (field.owner.level() == field.world && !field.center.equals(current)) {
				FIELDS.put(entry.getKey(), new Field(field.owner, field.world, current,
						field.headHeight, field.startTick, false,
						field.innerRadius, field.outerRadius, field.chargeTicks, field.durationTicks,
						field.expandStartTick, field.expandDurationTicks, field.maxCastDisplacement));
			}
		}
		for (Field field : FIELDS.values()) {
			if (field.active || !SpellCastingRules.playsChargeSound(field.chargeTicks) || field.elapsed() <= 0 || field.elapsed() % 40 != 0) continue;
			float pitch = 0.65f + Math.min(1f, (float) field.elapsed() / field.chargeTicks) * 0.25f;
			broadcast(field, SoundEvents.CONDUIT_AMBIENT_SHORT, pitch, 1.0f);
		}
		if (!FIELDS.isEmpty() && server.getTickCount() % 10 == 0) sync(server);
	}
	private static void sync(MinecraftServer server) {
		if (server == null) return;
		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			if (!AddonNetwork.canSend(viewer, STATE)) continue;
			var visible = FIELDS.values().stream().filter(field -> field.world == viewer.level()
					&& viewer.position().distanceToSqr(field.center) < 256 * 256).toList();
            if (!AUDIENCE.shouldSend(viewer.getUUID(), viewer.connection, !visible.isEmpty())) continue;
			var buf = AddonNetwork.buffer();
			buf.writeResourceLocation(viewer.level().dimension().location());
			buf.writeVarInt(visible.size());
			for (Field field : visible) {
				buf.writeUUID(field.owner.getUUID());
				buf.writeDouble(field.center.x);
				buf.writeDouble(field.center.y);
				buf.writeDouble(field.center.z);
				buf.writeDouble(field.headHeight);
				buf.writeBoolean(field.active);
				buf.writeVarInt(field.elapsed());
				buf.writeDouble(field.innerRadius);
				buf.writeDouble(field.outerRadius);
			}
			AddonNetwork.sendToPlayer(viewer, STATE, buf);
            AUDIENCE.sent(viewer.getUUID(), viewer.connection, !visible.isEmpty());
		}
	}
	public static boolean blocksPath(Level world, Vec3 from, Vec3 to, double padding) {
		if (FIELDS.isEmpty() || !(world instanceof ServerLevel) || from.equals(to)) return false;
		for (Field field : FIELDS.values()) {
			if (field.world != world) continue;
			Vec3 start = from.subtract(field.center), end = to.subtract(field.center);
			if (field.active) {
				if (DomainRules.crosses(start.x, start.y, start.z, end.x, end.y, end.z, padding,
						field.innerRadius, field.outerRadius)) return true;
			} else {
				if (DomainRules.crossesOutward(start.x, start.y, start.z, end.x, end.y, end.z,
						field.chargingRadius())) return true;
			}
		}
		return false;
	}
	public static Vec3 limitMovement(Entity entity, Vec3 movement) {
		Vec3 feet = entity.position();
		double padding = entity.getBbWidth() * 0.5;
		if (!blocksPath(entity.level(), feet, feet.add(movement), padding)) return movement;
		if (entity instanceof Projectile) { entity.discard(); return Vec3.ZERO; }
		return DomainRules.limitMovement(feet, movement, padding, movementShells(entity.level()));
	}

	public static Vec3 finishMovement(Entity entity, Vec3 movement) {
		if (!blocksPath(entity.level(), entity.position(), entity.position().add(movement), entity.getBbWidth() * 0.5)) return movement;
		if (entity instanceof Projectile) { entity.discard(); return Vec3.ZERO; }
		return DomainCollision.finishMovement(entity, movement, movementShells(entity.level()));
	}

	private static java.util.List<DomainRules.Shell> movementShells(Level world) {
		java.util.List<DomainRules.Shell> shells = new java.util.ArrayList<>(FIELDS.size());
		for (Field field : FIELDS.values()) {
			if (field.world != world) continue;
			double radius = field.active ? field.innerRadius : field.chargingRadius();
			if (radius > 0) shells.add(new DomainRules.Shell(field.center, radius, field.outerRadius, field.active));
		}
		return shells;
	}

	public static Vec3 clampToBoundary(Entity entity, Vec3 target) {
		Vec3 from = entity.position();
		return from.add(limitMovement(entity, target.subtract(from)));
	}

	public static Vec3 liftOutOfBlocks(Entity entity, Vec3 target) {
		Vec3 from = entity.position();
		return DomainRules.safeCorrection(from, target, candidate ->
				!blocksTeleport(entity, entity.level(), candidate)
				&& entity.level().noCollision(entity, entity.getBoundingBox().move(candidate.subtract(from))));
	}
	public static boolean blocksTeleport(Entity entity, Level destination, Vec3 position) {
		if (!(entity.level() instanceof ServerLevel)) return false;
		if (destination == entity.level()) return blocksPath(destination, entity.position(), position,
				entity.getBbWidth() * 0.5);
		if (enclosedTrapping(entity.level(), entity.position())) return true;
		return enclosedComplete(destination, position);
	}

	public static boolean enclosedTrapping(Level world, Vec3 position) {
		if (FIELDS.isEmpty() || !(world instanceof ServerLevel)) return false;
		for (Field field : FIELDS.values()) {
			if (field.world != world) continue;
			if (field.active) {
				if (field.center.distanceToSqr(position) <= field.outerRadius * field.outerRadius) return true;
			} else {
				double radius = field.chargingRadius();
				if (radius > 0 && field.center.distanceToSqr(position) <= radius * radius) return true;
			}
		}
		return false;
	}

	public static boolean enclosedComplete(Level world, Vec3 position) {
		if (!(world instanceof ServerLevel)) return false;
		for (Field field : FIELDS.values()) {
			if (field.active && field.world == world && field.center.distanceToSqr(position)
					<= field.outerRadius * field.outerRadius) return true;
		}
		return false;
	}
	public static boolean hasActive(Level world) {
		return world instanceof ServerLevel && FIELDS.values().stream().anyMatch(field -> field.active && field.world == world);
	}
	public static Entity setEffectSource(Entity source) {
		Entity previous = EFFECT_SOURCE.get();
		if (source == null) EFFECT_SOURCE.remove(); else EFFECT_SOURCE.set(source);
		return previous;
	}
	public static boolean blocksEffect(LivingEntity target, Entity source) {
		Entity actual = source == null ? EFFECT_SOURCE.get() : source;
		if (isAttachedEffect(actual, target)) return false;
		return actual != null && blocksPath(target.level(), actual.position(), target.position(), 0);
	}
	public static void runAttachedEffect(Entity source, LivingEntity target, Runnable action) {
		AttachedEffect previous = ATTACHED_EFFECT.get();
		ATTACHED_EFFECT.set(new AttachedEffect(source, target));
		try {
			action.run();
		} finally {
			if (previous == null) ATTACHED_EFFECT.remove(); else ATTACHED_EFFECT.set(previous);
		}
	}
	private static boolean isAttachedEffect(Entity source, LivingEntity target) {
		AttachedEffect effect = ATTACHED_EFFECT.get();
		return effect != null && effect.source == source && effect.target == target;
	}
	public static boolean blocksDamage(LivingEntity target, DamageSource source) {
		if (isAttachedEffect(source.getEntity(), target)) return false;
		if (blocksEffect(target, source.getEntity())) return true;
		Vec3 position = source.getSourcePosition();
		return position != null && blocksPath(target.level(), position, target.position(), 0);
	}
	public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
		if (!(target.level() instanceof ServerLevel)) return amount;
		Field received = fieldAt(target);
		Field dealt = source.getEntity() == null ? null : fieldAt(source.getEntity());
		if (received != null) amount *= DomainRules.receivedMultiplier(friendly(received, target));
		if (dealt != null) amount *= DomainRules.dealtMultiplier(friendly(dealt, source.getEntity()));
		return amount;
	}
	private static Field fieldAt(Entity entity) {
		for (Field field : FIELDS.values()) {
			if (field.active && field.world == entity.level()
					&& field.center.distanceToSqr(entity.position()) <= field.innerRadius * field.innerRadius) return field;
		}
		return null;
	}
	private static boolean friendly(Field field, Entity target) {
		return field.owner == target || target instanceof LivingEntity living && WhitelistUtils.isProtected(field.owner, living);
	}
}
