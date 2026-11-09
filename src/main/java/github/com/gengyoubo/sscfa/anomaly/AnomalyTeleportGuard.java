package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.level.ServerPlayer;

/** 异常传送守卫（Forge 占位）。 */
public final class AnomalyTeleportGuard {
    private AnomalyTeleportGuard() {}
    public static boolean reject(ServerPlayer player) { return false; }
    public static boolean isTeleportSpell(github.com.gengyoubo.sscfa.spell.Spell spell) { return false; }
}
