package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.level.ServerPlayer;

/** 异常反应效果（Forge 占位）：传送阻断等实际规则待异常系统移植后补回。 */
public final class AnomalyEffects {
    private AnomalyEffects() {}
    public static boolean teleportBlocked(ServerPlayer player) { return false; }
}
