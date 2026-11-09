package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

/** 异常管理器（Forge 简化）：保留法术/效果层所需的时钟与通知入口；完整触发队列待移植。 */
public final class AnomalyManager {
    private AnomalyManager() {}
    public static long now(MinecraftServer server) { return Integer.toUnsignedLong(server.getTickCount()); }
    public static void effectsChanged(LivingEntity target) {}
    public static void recordLocalQuery() {}
    public static AnomalyTargetState state(LivingEntity target) {
        return target instanceof AnomalyTargetAccess access ? access.ssca$anomalyState() : null;
    }
    public static void init() {}
}
