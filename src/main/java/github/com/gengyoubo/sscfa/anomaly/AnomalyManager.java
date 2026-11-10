package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

/** 寮傚父绠＄悊鍣紙Forge 绠€鍖栵級锛氫繚鐣欐硶鏈?鏁堟灉灞傛墍闇€鐨勬椂閽熶笌閫氱煡鍏ュ彛锛涘畬鏁磋Е鍙戦槦鍒楀緟绉绘銆?*/
public final class AnomalyManager {
    private AnomalyManager() {}
    public static long now(MinecraftServer server) { return Integer.toUnsignedLong(server.getTickCount()); }
    public static void effectsChanged(LivingEntity target) {}
    public static void recordLocalQuery() {}
    public static AnomalyTargetState state(LivingEntity target) {
        return target instanceof AnomalyTargetAccess access ? access.ssca$anomalyState() : null;
    }
    public static void damageObserved(net.minecraft.world.entity.LivingEntity target) {}
    public static void init() {}
}
