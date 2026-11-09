package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.level.ServerPlayer;

/** 冻结控制（Forge 占位）。 */
public final class FrozenControl {
    private FrozenControl() {}
    public static boolean blocked(ServerPlayer player) { return false; }
}
