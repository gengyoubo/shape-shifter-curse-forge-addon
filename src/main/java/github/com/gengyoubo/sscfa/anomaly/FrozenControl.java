package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.world.entity.LivingEntity;

/** 冻结控制（Forge 简化）：保留异常效果层所需入口；完整冻结/视角锁待能力层移植。 */
public final class FrozenControl {
    public static final int DEFAULT_DURATION_TICKS = 100;
    public static final int SHATTER_SLOW_TICKS = 60;
    private FrozenControl() {}
    public static boolean freeze(LivingEntity target, int durationTicks) { return false; }
    public static void clear(LivingEntity target) {}
    public static boolean blocked(LivingEntity target) { return false; }
    public static boolean blocked(net.minecraft.server.level.ServerPlayer player) { return false; }
    public static void init() {}
}
