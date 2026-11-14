package github.com.gengyoubo.sscfa.neomagic;

import github.com.gengyoubo.sscfa.anomaly.AnomalyEffects;
import github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits;
import github.com.gengyoubo.sscfa.spell.FormCastingStyle;
import github.com.gengyoubo.sscfa.spell.FormSpellStyle;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Blueprint;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Carrier;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Core;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Plan;

/** 服务端持有的轨迹、延迟结构与召唤物。不会调用旧的魔法处理器。 */
public final class NeoEffects {
    public record Anchor(Vec3 position, UUID target) {}

    private record Stored(NeoRuntime.Active active, int expires) {}

    private static final Map<UUID, Stored> STORED = new HashMap<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final Map<Wolf, Integer> SUMMONS = new HashMap<>();
    private static final String SUMMON_TAG = "ssca_neomagic_summon";
    private static boolean registered;

    private static final class Bolt {
        final ServerPlayer caster;
        final ServerLevel world;
        final NeoRuntime.Active active;
        Vec3 pos;
        Vec3 velocity;
        final double budget;
        double remaining;

        Bolt(ServerPlayer player, NeoRuntime.Active active, Vec3 direction, double budget) {
            this.caster = player;
            this.world = player.serverLevel();
            this.active = active;
            this.pos = player.getEyePosition();
            this.velocity = direction.normalize();
            this.budget = budget;
            this.remaining = active.session.blueprint.range() * active.rangeMultiplier;
        }
    }

    private NeoEffects() {}

    public static Vec3 aim(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getLocation();
    }

    public static Anchor capture(ServerPlayer player, Blueprint blueprint) {
        return capture(player, blueprint, 0, 0, NeoLegacyCasting.blueprintRangeMultiplier(player, blueprint));
    }

    private static Anchor capture(ServerPlayer player, Blueprint blueprint, double yaw, double pitch, double multiplier) {
        double range = (blueprint.carrier() == Carrier.CONTACT ? Math.min(4, blueprint.range()) : blueprint.range()) * multiplier;
        Vec3 direction = Vec3.directionFromRotation((float) (player.getXRot() + pitch), (float) (player.getYRot() + yaw));
        Vec3 eye = player.getEyePosition();
        Vec3 end = player.level().clip(new ClipContext(eye, eye.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity entity = along(player, eye, end, blueprint.carrier() == Carrier.LOCK ? .8 : .4);
        return new Anchor(entity == null ? end : entity.position(), entity == null ? null : entity.getUUID());
    }

    public static boolean preflight(ServerPlayer player, Blueprint blueprint, Anchor anchor) {
        if (blueprint.output() == Core.SPACE && AnomalyEffects.teleportBlocked(player)) return false;
        if (blueprint.carrier() == Carrier.LOCK && anchor.target() == null) return false;
        if (blueprint.carrier() == Carrier.CONTACT && anchor.target() == null) return false;
        if (blueprint.core() == Core.SPACE || blueprint.core() == Core.SUMMON) {
            BlockPos pos = BlockPos.containing(anchor.position());
            return player.serverLevel().getWorldBorder().isWithinBounds(pos)
                    && player.serverLevel().mayInteract(player, pos);
        }
        return true;
    }

    private static LivingEntity along(ServerPlayer player, Vec3 start, Vec3 end, double width) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, end).inflate(width + 1),
                        e -> e != player && e.isAlive() && !e.isSpectator())
                .stream()
                .filter(e -> e.getBoundingBox().inflate(width).clip(start, end).isPresent())
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().inflate(width).clip(start, end)
                        .orElse(end).distanceToSqr(start)))
                .orElse(null);
    }

    private static boolean allowed(ServerPlayer caster, LivingEntity target, boolean hostile) {
        if (target == null || !target.isAlive() || target.isSpectator()) return false;
        if (!hostile) return true;
        if (target == caster || caster.isAlliedTo(target)) return false;
        if (target instanceof ServerPlayer other && !caster.canHarmPlayer(other)) return false;
        return target.level() == caster.level();
    }

    public static boolean hasStored(UUID id) { return STORED.containsKey(id); }

    public static void clearStored(UUID id) {
        Stored lost = STORED.remove(id);
        if (lost != null) lost.active.record.result = "storage_lost";
    }

    public static void clear() {
        STORED.values().forEach(stored -> stored.active.record.result = "storage_lost");
        STORED.clear();
        BOLTS.forEach(bolt -> {
            bolt.active.record.result = "effect_lost";
            bolt.active.record.pending = 0;
        });
        BOLTS.clear();
        SUMMONS.keySet().forEach(Entity::discard);
        SUMMONS.clear();
    }

    public static boolean release(ServerPlayer player, NeoRuntime.Active active) {
        if (active.session.quote.plan().stored()) {
            STORED.put(player.getUUID(), new Stored(active, player.getServer().getTickCount() + 200));
            return true;
        }
        return apply(player, active);
    }

    public static void trigger(ServerPlayer player) {
        Stored stored = STORED.remove(player.getUUID());
        if (stored == null) return;
        if (!NeoRuntime.blocked(player)
                && stored.active.record.dimension.equals(player.level().dimension().location().toString())
                && apply(player, stored.active)) {
            stored.active.record.result = "released";
            NeoRuntime.data(player).successful(stored.active.session.blueprint);
            NeoRuntime.grantExperience(player, stored.active);
        } else {
            stored.active.record.result = "storage_lost";
        }
        NeoRuntime.dirty(player);
    }

    private static boolean apply(ServerPlayer player, NeoRuntime.Active active) {
        if (active.anomaly == null) active.anomaly = anomaly(player, active);
        NeoLegacyCasting.beginBlueprintEffect(player, active);
        boolean released = applyNow(player, active);
        if (released) NeoLegacyCasting.blueprintReleased(player, active);
        return released;
    }

    static SpellAnomalyContext anomaly(ServerPlayer player, NeoRuntime.Active active) {
        Blueprint blueprint = active.session.blueprint;
        String controls = blueprint.controls().isEmpty() ? "direct"
                : String.join("_", blueprint.controls().stream().map(c -> c.name().toLowerCase(Locale.ROOT)).toList());
        String identity = "blueprint/" + blueprint.core().name().toLowerCase(Locale.ROOT)
                + "/" + blueprint.carrier().name().toLowerCase(Locale.ROOT)
                + "/" + controls + "/" + blueprint.output().name().toLowerCase(Locale.ROOT);
        return new SpellAnomalyContext(new ResourceLocation("ssca_neomagic", identity),
                NeoLegacyCasting.element(blueprint.output()).id, blueprint.band() + 1, 0,
                player.getUUID(), UUID.fromString(active.record.id));
    }

    private static boolean applyNow(ServerPlayer player, NeoRuntime.Active active) {
        NeoSession session = active.session;
        Blueprint blueprint = session.blueprint;
        Plan plan = session.quote.plan();
        double budget = 9 * blueprint.scale() * plan.retention() * session.attenuation * active.powerMultiplier;
        if (blueprint.carrier() == Carrier.PROJECTILE) {
            active.record.pending = plan.branches();
            for (int i = 0; i < plan.branches(); i++) {
                double fan = (i - (plan.branches() - 1) / 2.0) * 8;
                Vec3 direction = Vec3.directionFromRotation((float) (player.getXRot() + session.pitch),
                        (float) (player.getYRot() + session.yaw + fan));
                BOLTS.add(new Bolt(player, active, direction, budget / plan.branches()));
            }
            return true;
        }
        double range = blueprint.range() * active.rangeMultiplier;
        Vec3 endpoint = aim(player, range);
        LivingEntity target = null;
        if (blueprint.carrier() == Carrier.LOCK) {
            Entity found = active.anchor.target() == null ? null : player.serverLevel().getEntity(active.anchor.target());
            if (!(found instanceof LivingEntity living) || player.distanceToSqr(living) > range * range
                    || !player.hasLineOfSight(living)) return false;
            target = living;
            endpoint = target.position();
            if (session.wrongTarget) {
                var candidates = player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5),
                        e -> e != player && e != found && e.isAlive() && player.hasLineOfSight(e));
                if (candidates.isEmpty()) return false;
                target = candidates.get(player.getRandom().nextInt(candidates.size()));
                endpoint = target.position();
            }
            endpoint = endpoint.add(Math.tan(Math.toRadians(session.yaw)) * player.distanceTo(target), 0,
                    Math.tan(Math.toRadians(session.pitch)) * player.distanceTo(target));
        } else if (blueprint.carrier() == Carrier.SELF) {
            target = player;
            endpoint = player.position();
        } else if (blueprint.carrier() == Carrier.CONTACT) {
            Anchor hit = capture(player, blueprint, session.yaw, session.pitch, active.rangeMultiplier);
            endpoint = hit.position();
            Entity found = hit.target() == null ? null : player.serverLevel().getEntity(hit.target());
            if (found instanceof LivingEntity living) target = living;
        } else {
            Vec3 direction = Vec3.directionFromRotation((float) (player.getXRot() + session.pitch),
                    (float) (player.getYRot() + session.yaw));
            endpoint = player.getEyePosition().add(direction.scale(Math.min(range, player.getEyePosition().distanceTo(endpoint))));
        }

        if (blueprint.output() == Core.SPACE) {
            if (AnomalyTeleportGuard.reject(player)) return false;
            LivingEntity moved = blueprint.carrier() == Carrier.SELF ? player : target;
            if (moved == null) return false;
            var movedTargets = new ArrayList<LivingEntity>();
            movedTargets.add(moved);
            if (plan.branches() > 1) {
                var candidates = targets(player, moved.position(), 3, false);
                candidates.remove(moved);
                candidates.stream().limit(plan.branches() - 1).forEach(movedTargets::add);
            }
            int movedCount = 0;
            for (LivingEntity traveler : movedTargets) {
                // 不安全的落点是有意的：不吸附到实体地面或安全空气口袋。
                Vec3 direction = Vec3.directionFromRotation((float) (player.getXRot() + session.pitch),
                        (float) (player.getYRot() + session.yaw));
                Vec3 to = traveler.position().add(direction.scale(Math.min(blueprint.range(), 4 * blueprint.scale())
                        * active.rangeMultiplier * session.attenuation * plan.retention() / plan.branches()));
                BlockPos pos = BlockPos.containing(to);
                ServerLevel level = player.serverLevel();
                if (!level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
                        || !level.mayInteract(player, pos)) continue;
                if (traveler != player && !allowed(player, traveler, true)) continue;
                double traveled = traveler.position().distanceTo(to);
                teleport(traveler, to);
                active.record.arrived += traveled;
                active.record.hits++;
                movedCount++;
            }
            return movedCount > 0;
        }
        if (blueprint.output() == Core.SUMMON) return summon(player, active, endpoint, budget);
        if (blueprint.carrier() == Carrier.AREA) {
            Vec3 side = Vec3.directionFromRotation(0, player.getYRot() + 90).scale(3);
            for (int i = 0; i < plan.branches(); i++) {
                Vec3 center = endpoint.add(side.scale(i - (plan.branches() - 1) / 2.0));
                var list = effectTargets(player, active, center, plan.focused() ? 1 : 3);
                if (list.isEmpty()) measureBlock(player, active, center, budget / plan.branches());
                else distribute(player, active, list, budget / plan.branches());
            }
            return true;
        }
        if (target != null) {
            if (blueprint.carrier() == Carrier.LOCK && target.position().distanceTo(endpoint) > 1.25) {
                measureBlock(player, active, endpoint, budget);
                return true;
            }
            affect(player, active, target, budget / plan.branches());
            if (plan.branches() > 1) {
                var extra = effectTargets(player, active, target.position(),
                        blueprint.carrier() == Carrier.CONTACT ? 2 : 5);
                extra.remove(target);
                extra.stream().limit(plan.branches() - 1)
                        .forEach(other -> affect(player, active, other, budget / plan.branches()));
            }
            return true;
        }
        measureBlock(player, active, endpoint, budget);
        return blueprint.carrier() != Carrier.CONTACT;
    }

    private static void teleport(LivingEntity traveler, Vec3 to) {
        if (traveler instanceof ServerPlayer player) {
            player.connection.teleport(to.x, to.y, to.z, player.getYRot(), player.getXRot());
        } else {
            traveler.teleportTo(to.x, to.y, to.z);
        }
    }

    private static boolean summon(ServerPlayer player, NeoRuntime.Active active, Vec3 pos, double budget) {
        int count = Math.max(0, (int) Math.floor(active.session.quote.plan().branches() * active.session.attenuation));
        if (count == 0) return false;
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Vec3 at = pos.add(i * .6, 0, 0);
            BlockPos block = BlockPos.containing(at);
            if (!level.hasChunkAt(block) || !level.getWorldBorder().isWithinBounds(block)
                    || !level.mayInteract(player, block)) continue;
            Wolf wolf = EntityType.WOLF.create(level);
            if (wolf == null) continue;
            wolf.moveTo(at.x, at.y, at.z, 0, 0);
            wolf.setTame(true);
            wolf.setOwnerUUID(player.getUUID());
            var identity = active.anomaly == null ? anomaly(player, active) : active.anomaly;
            if (wolf instanceof SpellAnomalyHits.SummonedData summoned) {
                summoned.ssca$summonAnomaly(new SpellAnomalyContext(identity.spellId(), "summon",
                        identity.level(), 0, identity.casterUUID(), identity.castUUID()));
            }
            double share = budget / count;
            var health = wolf.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) health.setBaseValue(Math.max(1, share));
            wolf.setHealth((float) Math.max(1, share));
            var attack = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(Math.max(.01, share / 3));
            wolf.addTag(SUMMON_TAG);
            wolf.setCustomName(NeoMagic.text("summon_name"));
            if (!level.noCollision(wolf)) {
                wolf.discard();
                continue;
            }
            SUMMONS.put(wolf, player.getServer().getTickCount() + 200);
            if (level.addFreshEntity(wolf)) {
                active.record.summons++;
                active.record.arrived += share;
            } else {
                SUMMONS.remove(wolf);
                wolf.discard();
            }
        }
        return active.record.summons > 0;
    }

    private static List<LivingEntity> targets(ServerPlayer player, Vec3 center, double radius, boolean healing) {
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> allowed(player, e, !healing) && e.distanceToSqr(center) <= radius * radius
                        && player.hasLineOfSight(e));
    }

    private static List<LivingEntity> effectTargets(ServerPlayer player, NeoRuntime.Active active,
                                                    Vec3 center, double radius) {
        boolean lunar = active.session.blueprint.output() == Core.LUNAR;
        var list = targets(player, center, radius, lunar && !active.session.reversed);
        if (lunar && active.session.reversed && player.distanceToSqr(center) <= radius * radius
                && !list.contains(player)) list.add(player);
        return list;
    }

    private static void distribute(ServerPlayer player, NeoRuntime.Active active,
                                   List<LivingEntity> targets, double budget) {
        if (targets.isEmpty()) return;
        double per = budget / targets.size();
        targets.forEach(target -> affect(player, active, target, per));
    }

    private static void affect(ServerPlayer player, NeoRuntime.Active active, LivingEntity target, double budget) {
        affectNow(player, active, target, budget);
    }

    private static boolean affectNow(ServerPlayer player, NeoRuntime.Active active, LivingEntity target, double budget) {
        Core core = active.session.blueprint.output();
        boolean healing = core == Core.LUNAR && !active.session.reversed;
        boolean invertedSelf = target == player && core == Core.LUNAR && active.session.reversed;
        if (!invertedSelf && !allowed(player, target, !healing)) return false;
        active.record.arrived += budget;
        active.record.hits++;
        float before = target.getHealth();
        float absorption = target.getAbsorptionAmount();
        boolean conditionBeforeHit = core == Core.THERMAL ? target.getRemainingFireTicks() > 0
                : FormCastingStyle.hasHarmfulEffect(target);
        boolean effective = false;
        switch (core) {
            case LUNAR -> {
                if (healing) target.heal((float) budget);
                else effective = target.hurt(invertedSelf ? player.damageSources().magic()
                        : player.damageSources().indirectMagic(player, player), (float) budget);
            }
            case THERMAL -> effective = target.hurt(player.damageSources().indirectMagic(player, player), (float) budget);
            case FROST -> {
                if (effective = target.hurt(player.damageSources().indirectMagic(player, player), (float) (budget * .5))) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (budget * 12),
                            Math.min(3, (int) (budget / 6))), player);
                }
            }
            case CURSE -> {
                if (effective = target.hurt(player.damageSources().indirectMagic(player, player), (float) (budget * .4))) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                            Math.round((int) (budget * 20) * active.curseDurationMultiplier), 0), player);
                }
            }
            case VOID -> {
                if (effective = target.hurt(player.damageSources().indirectMagic(player, player), (float) (budget * .8))) {
                    target.setAbsorptionAmount(Math.max(0, target.getAbsorptionAmount() - (float) (budget * .2)));
                }
            }
            default -> { }
        }
        if (effective && !healing && !active.session.reversed && target != player) {
            NeoLegacyCasting.blueprintHit(player, active, target, conditionBeforeHit);
        }
        active.record.damage += Math.max(0, before - target.getHealth());
        active.record.absorbed += Math.max(0, absorption - target.getAbsorptionAmount());
        active.record.healing += Math.max(0, target.getHealth() - before);
        if (active.record.measurements.size() < 32) {
            active.record.measurements.add(new NeoState.Measurement(target.getStringUUID(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(), target.position().toString(),
                    budget, before, target.getHealth(), absorption, target.getAbsorptionAmount()));
        }
        if (effective && !healing && !active.session.reversed && target != player) {
            SpellAnomalyHits.hit(active.anomaly, target);
        }
        NeoRuntime.dirty(player);
        return effective;
    }

    private static boolean measureBlock(ServerPlayer player, NeoRuntime.Active active, Vec3 pos, double budget) {
        // 独立观测靶已移除；正式法术的命中结算沿用各自实现。
        return false;
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (var iterator = STORED.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            if (now >= entry.getValue().expires) {
                entry.getValue().active.record.result = "storage_expired";
                var player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) NeoRuntime.dirty(player);
                iterator.remove();
            }
        }
        for (var iterator = SUMMONS.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            if (!entry.getKey().isAlive() || now >= entry.getValue()) {
                entry.getKey().discard();
                iterator.remove();
            }
        }
        for (var iterator = BOLTS.iterator(); iterator.hasNext(); ) {
            Bolt bolt = iterator.next();
            if (!bolt.caster.isAlive() || bolt.caster.hasDisconnected()
                    || bolt.caster.level() != bolt.world || bolt.remaining <= 0) {
                finishBolt(bolt);
                iterator.remove();
                continue;
            }
            var style = bolt.active.style;
            if (style != null && style.target != null && FormSpellStyle.legalTarget(bolt.caster, style.target)) {
                Vec3 delta = style.target.getBoundingBox().getCenter().subtract(bolt.pos);
                if (delta.lengthSqr() > 1e-8) bolt.velocity = delta.normalize().scale(bolt.velocity.length());
            }
            double step = Math.min(bolt.velocity.length(), bolt.remaining);
            Vec3 end = bolt.pos.add(bolt.velocity.normalize().scale(step));
            BlockHitResult wall = bolt.world.clip(new ClipContext(bolt.pos, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bolt.caster));
            Vec3 clipped = wall.getType() == HitResult.Type.BLOCK ? wall.getLocation() : end;
            LivingEntity hit = along(bolt.caster, bolt.pos, clipped,
                    bolt.active.session.quote.plan().focused() ? .08 : .3);
            if (hit != null || wall.getType() == HitResult.Type.BLOCK) {
                Plan plan = bolt.active.session.quote.plan();
                boolean plain = plan.branches() == 1 && !plan.focused();
                if (hit != null) affect(bolt.caster, bolt.active, hit, plain ? bolt.budget * 2 / 3 : bolt.budget);
                if (plain) {
                    var nearby = effectTargets(bolt.caster, bolt.active, hit == null ? clipped : hit.position(), 2);
                    nearby.remove(hit);
                    distribute(bolt.caster, bolt.active, nearby, bolt.budget / 3);
                }
                if (hit == null) measureBlock(bolt.caster, bolt.active, clipped,
                        plain ? bolt.budget * 2 / 3 : bolt.budget);
                finishBolt(bolt);
                iterator.remove();
            } else {
                bolt.pos = end;
                bolt.remaining -= step;
                bolt.world.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void finishBolt(Bolt bolt) {
        var record = bolt.active.record;
        record.pending = Math.max(0, record.pending - 1);
        if (record.pending == 0) record.result = record.arrived > 0 ? "resolved" : "missed";
        NeoRuntime.dirty(bolt.caster);
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getEntity().getTags().contains(SUMMON_TAG)
                    && !SUMMONS.containsKey(event.getEntity())) {
                event.getEntity().discard();
            }
        });
    }
}
