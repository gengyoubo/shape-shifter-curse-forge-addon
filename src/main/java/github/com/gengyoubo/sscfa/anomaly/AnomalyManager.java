package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/** 异常状态访问与客户端快照组装。 */
public final class AnomalyManager {
    private static long packets;
    private static long localQueries;
    private static boolean initialized;

    private AnomalyManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        AnomalyEffects.init();
        AnomalyNetwork.init();
    }

    public static long now(MinecraftServer server) {
        return Integer.toUnsignedLong(server.getTickCount());
    }

    public static void effectsChanged(LivingEntity target) {
        if (target != null && target.level() instanceof ServerLevel && target.isAlive() && !target.isRemoved()) {
            AnomalyNetwork.sync(target);
        }
    }

    public static void recordLocalQuery() {
        localQueries++;
    }

    public static void recordPacket() {
        packets++;
    }

    public static AnomalyTargetState state(LivingEntity target) {
        return target instanceof AnomalyTargetAccess access ? access.ssca$anomalyState() : null;
    }

    public static void damageObserved(LivingEntity target) {
    }

    public static AnomalySnapshot snapshot(LivingEntity target) {
        AnomalyTargetState current = state(target);
        long tick = target.level() instanceof ServerLevel world ? now(world.getServer()) : 0;
        if (current == null || current.target != target || current.world != target.level()) {
            return new AnomalySnapshot(AnomalyLedger.MAXIMUM, Map.of(), 0, "", 0,
                    AnomalyEffects.effectSnapshot(target));
        }
        AnomalyLedger.Snapshot values = current.ledger.snapshot(tick);
        int link = current.linkRemaining(tick);
        return new AnomalySnapshot(values.maximum(), values.values(), values.remainingTicks(),
                current.lastElement, link, AnomalyEffects.effectSnapshot(target),
                values.fullRemainingTicks(), values.fullElements());
    }
}
