package github.com.gengyoubo.sscfa.anomaly;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.ControlledAbilityInterruption;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 独立冻结控制：不改变原有霜冻/茧/眩晕语义。 */
public final class FrozenControl {
    public static final int DEFAULT_DURATION_TICKS = 100;
    public static final int SHATTER_SLOW_TICKS = 60;

    public record Pose(float yaw, float pitch) {}

    private record Entry(LivingEntity target, ServerLevel world) {}

    private static final Map<UUID, Pose> POSES = new HashMap<>();
    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();
    private static final java.util.Set<UUID> LOCKED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean initialized;

    private FrozenControl() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            for (Entry entry : List.copyOf(ACTIVE.values())) {
                LivingEntity target = entry.target();
                if (target.isRemoved() || target.level() != entry.world() || !blocked(target)) clear(target);
                else hold(target);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> clear(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getEntity() instanceof LivingEntity living && !event.getLevel().isClientSide()) {
                Entry entry = ACTIVE.get(living.getUUID());
                if (entry == null || entry.target() != living) clear(living);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getEntity() instanceof LivingEntity living) clear(living);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            clear(event.getEntity());
        });
        PlayerSessionCleanup.register((handler, server) -> clear(handler.player));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            clear(event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            clear(event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
            for (Entry entry : List.copyOf(ACTIVE.values())) clear(entry.target());
            ACTIVE.clear();
            POSES.clear();
            LOCKED.clear();
        });
    }

    public static boolean freeze(LivingEntity target, int durationTicks) {
        if (target == null || !(target.level() instanceof ServerLevel) || !target.isAlive()
                || target.isSpectator() || durationTicks <= 0) {
            return false;
        }
        target.addEffect(new MobEffectInstance(SscAddon.FROZEN, durationTicks, 0, false, false, true));
        if (!blocked(target)) return false;
        applied(target);
        return true;
    }

    public static void applied(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel world) || !blocked(target)) return;
        Entry previous = ACTIVE.get(target.getUUID());
        if (previous != null && previous.target() == target && previous.world() == world) {
            AnomalyManager.effectsChanged(target);
            return;
        }
        if (previous != null) clear(previous.target());
        ACTIVE.put(target.getUUID(), new Entry(target, world));
        capture(target);
        target.stopRiding();
        target.stopUsingItem();
        if (target instanceof Mob mob) mob.getNavigation().stop();
        if (target instanceof ServerPlayer player) {
            player.closeContainer();
            SpellChannelManager.onFrozen(player);
            ControlledAbilityInterruption.interrupt(player);
        }
        hold(target);
        AnomalyManager.effectsChanged(target);
    }

    public static void clear(LivingEntity target) {
        target.removeEffect(SscAddon.FROZEN);
        release(target);
    }

    public static void breakFromHit(LivingEntity target) {
        clear(target);
        if (target.isAlive() && !target.isRemoved()) {
            target.addEffect(new MobEffectInstance(SscAddon.FROZEN_RECOVERY, SHATTER_SLOW_TICKS, 0));
        }
    }

    public static boolean blocked(LivingEntity entity) {
        return entity != null && entity.isAlive() && entity.hasEffect(SscAddon.FROZEN);
    }

    public static void capture(LivingEntity entity) {
        POSES.put(entity.getUUID(), new Pose(entity.getYRot(), entity.getXRot()));
        LOCKED.add(entity.getUUID());
    }

    public static Pose pose(LivingEntity entity) {
        return POSES.get(entity.getUUID());
    }

    public static ServerboundMovePlayerPacket lockLook(LivingEntity entity, ServerboundMovePlayerPacket packet) {
        Pose pose = pose(entity);
        return new ServerboundMovePlayerPacket.PosRot(
                packet.getX(entity.getX()), packet.getY(entity.getY()), packet.getZ(entity.getZ()),
                pose == null ? entity.getYRot() : pose.yaw(),
                pose == null ? entity.getXRot() : pose.pitch(),
                packet.isOnGround());
    }

    public static boolean serverLocked(UUID uuid) {
        return LOCKED.contains(uuid);
    }

    public static void release(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel)) return;
        Entry entry = ACTIVE.get(entity.getUUID());
        if (entry != null && entry.target() != entity) return;
        ACTIVE.remove(entity.getUUID());
        boolean hadPose = POSES.remove(entity.getUUID()) != null;
        boolean hadLock = LOCKED.remove(entity.getUUID());
        if (entry != null || hadPose || hadLock) AnomalyManager.effectsChanged(entity);
    }

    public static void hold(LivingEntity entity) {
        Pose pose = POSES.get(entity.getUUID());
        if (pose == null) return;
        entity.setYRot(pose.yaw());
        entity.setXRot(pose.pitch());
        entity.setYHeadRot(pose.yaw());
        entity.setYBodyRot(pose.yaw());
        entity.zza = 0;
        entity.xxa = 0;
        entity.stopUsingItem();
    }
}
