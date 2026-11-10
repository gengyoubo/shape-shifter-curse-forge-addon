package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 死亡终章：服务端掌握目标与危险标记，仅成功完成引导后结算。 */
public final class DeathFinaleManager {
    public static final ResourceLocation SNAPSHOT = new ResourceLocation("ssc_addon", "death_finale_state");

    private static final Map<UUID, Sequence> ACTIVE = new LinkedHashMap<>();
    private static final Set<UUID> VIEWERS = new HashSet<>();
    private static final Map<UUID, Set<UUID>> DANGER = new HashMap<>();

    private static final class Sequence {
        final UUID id = UUID.randomUUID();
        final UUID owner;
        final ServerLevel world;
        final DeathFinaleRules.ChargePosition position;
        final int duration;
        final double radius = DeathFinaleRules.RADIUS;
        int elapsed;
        long releasedAt = -1;
        SpellAnomalyContext anomaly;

        Sequence(ServerPlayer player, int duration) {
            owner = player.getUUID();
            world = player.serverLevel();
            position = new DeathFinaleRules.ChargePosition(player.position());
            this.duration = duration;
        }

        Vec3 center() {
            return position.center();
        }
    }

    private DeathFinaleManager() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(event.getServer());
        });
        PlayerSessionCleanup.register((handler, server) -> {
            ACTIVE.remove(handler.player.getUUID());
            VIEWERS.remove(handler.player.getUUID());
            DANGER.remove(handler.player.getUUID());
            sync(server, true);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            ACTIVE.clear();
            VIEWERS.clear();
            DANGER.clear();
        });
    }

    public static void begin(ServerPlayer caster, int duration) {
        ACTIVE.put(caster.getUUID(), new Sequence(caster, duration));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 1.1f, 0.55f);
        sync(caster.getServer(), true);
    }

    public static void advance(ServerPlayer caster, int ticks) {
        Sequence s = ACTIVE.get(caster.getUUID());
        if (s == null || s.releasedAt >= 0 || caster.level() != s.world
                || !s.position.follow(caster.position(), OcelotSpellLink.wind(caster))) {
            return;
        }
        s.elapsed = ticks;
        int remaining = s.duration - ticks;
        if (remaining > 0 && ticks % (remaining <= 60 ? 10 : 20) == 0) {
            s.world.playSound(null, s.center().x, s.center().y, s.center().z,
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.3f,
                    0.65f + 0.45f * ticks / s.duration);
            if (ticks % 20 == 0) {
                s.world.playSound(null, s.center().x, s.center().y, s.center().z,
                        SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.65f, 0.55f);
            }
        }
    }

    public static boolean canContinue(ServerPlayer caster) {
        Sequence s = ACTIVE.get(caster.getUUID());
        return s != null && s.releasedAt < 0 && caster.level() == s.world
                && (OcelotSpellLink.wind(caster) || s.position.canContinue(caster.position()));
    }

    public static void cancel(ServerPlayer caster) {
        if (ACTIVE.remove(caster.getUUID()) != null) sync(caster.getServer(), true);
    }

    private static boolean eligible(Sequence s, ServerPlayer owner, LivingEntity target) {
        if (owner == null || target.level() != s.world || !target.isAlive() || target.isRemoved()
                || target.isSpectator() || target instanceof ArmorStand
                || !DeathFinaleRules.contains(target.distanceToSqr(s.center()), s.radius)
                || WhitelistUtils.isProtected(owner, target)
                || DomainManager.blocksPath(s.world, s.center(), target.position(), 0)) {
            return false;
        }
        if (target instanceof ServerPlayer player && !owner.canHarmPlayer(player)) return false;
        return !target.isInvulnerableTo(SpellDamageSource.of(s.world.damageSources(), owner));
    }

    public static void release(ServerPlayer caster, float power, UUID refundId, int exp) {
        Sequence s = ACTIVE.get(caster.getUUID());
        if (s == null || s.releasedAt >= 0 || s.elapsed < s.duration || caster.level() != s.world
                || !s.position.follow(caster.position(), OcelotSpellLink.wind(caster))) {
            return;
        }
        s.releasedAt = s.world.getGameTime();
        s.anomaly = SpellAnomalyHits.current();
        for (LivingEntity target : s.world.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(s.center(), s.radius * 2, s.radius * 2, s.radius * 2),
                entity -> eligible(s, caster, entity))) {
            if (SpellHitHelper.projectileHit(caster, target, power, FormationElement.VOID, refundId, exp, s.anomaly)
                    == SpellHitHelper.HitResult.HIT) {
                exp = 0;
            }
        }
        s.world.playSound(null, s.center().x, s.center().y, s.center().z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2f, 0.65f);
        s.world.playSound(null, s.center().x, s.center().y, s.center().z,
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
        sync(caster.getServer(), true);
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty() && VIEWERS.isEmpty()) return;
        boolean removed = ACTIVE.values().removeIf(s -> {
            if (s.releasedAt >= 0) {
                return s.world.getGameTime() - s.releasedAt >= DeathFinaleRules.AFTERGLOW_TICKS;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(s.owner);
            return owner == null || !owner.isAlive() || owner.level() != s.world
                    || !SpellChannelManager.isCasting(owner);
        });
        sync(server, removed || server.getTickCount() % 5 == 0);
    }

    private static void sync(MinecraftServer server, boolean force) {
        if (server == null) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (!AddonNetwork.canSend(viewer, SNAPSHOT)) continue;
            var visible = ACTIVE.values().stream()
                    .filter(s -> s.world == viewer.serverLevel()
                            && viewer.distanceToSqr(s.center()) <= 64.0 * 64.0)
                    .toList();
            Set<UUID> danger = new HashSet<>();
            for (Sequence s : visible) {
                if (s.releasedAt < 0 && eligible(s, server.getPlayerList().getPlayer(s.owner), viewer)) {
                    danger.add(s.id);
                }
            }
            UUID viewerId = viewer.getUUID();
            boolean wasVisible = VIEWERS.contains(viewerId);
            if (visible.isEmpty() && !wasVisible) continue;
            if (!force && !visible.isEmpty() && wasVisible && danger.equals(DANGER.get(viewerId))) continue;

            FriendlyByteBuf out = AddonNetwork.buffer();
            out.writeResourceLocation(viewer.serverLevel().dimension().location());
            out.writeVarInt(visible.size());
            for (Sequence s : visible) {
                out.writeUUID(s.id);
                out.writeUUID(s.owner);
                out.writeDouble(s.center().x);
                out.writeDouble(s.center().y);
                out.writeDouble(s.center().z);
                out.writeDouble(s.radius);
                out.writeVarInt(s.duration);
                out.writeVarInt(s.releasedAt < 0 ? s.elapsed
                        : s.duration + (int) (s.world.getGameTime() - s.releasedAt));
                out.writeBoolean(s.releasedAt >= 0);
                out.writeBoolean(danger.contains(s.id));
            }
            AddonNetwork.sendToPlayer(viewer, SNAPSHOT, out);
            if (visible.isEmpty()) {
                VIEWERS.remove(viewerId);
                DANGER.remove(viewerId);
            } else {
                VIEWERS.add(viewerId);
                DANGER.put(viewerId, danger);
            }
        }
    }
}
