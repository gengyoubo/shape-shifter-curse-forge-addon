package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;

/** 通用法术阵管理器占位实现：待法术阵子系统移植后替换。 */
public final class UniversalFormationManager {
    private UniversalFormationManager() {}

    public static boolean isCharging(ServerPlayer player) {
        return false;
    }
}
