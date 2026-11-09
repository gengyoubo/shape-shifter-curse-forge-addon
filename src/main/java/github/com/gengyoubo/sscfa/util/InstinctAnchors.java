package github.com.gengyoubo.sscfa.util;

import net.minecraft.server.level.ServerPlayer;

/** 本能锚定占位实现：待本能/形态锁定子系统移植后替换。 */
public final class InstinctAnchors {
    private InstinctAnchors() {}

    public enum Result {
        FIXED, RELEASED, CURSED_MOON, NATIVE_LOCKED, TRANSFORMING, ALREADY_FIXED, NOT_FIXED
    }

    public static void init() {
    }

    public static boolean locked(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    public static boolean blocksChange(net.minecraft.world.entity.player.Player player, net.onixary.shapeShifterCurseForge.form.FormDefinition target) {
        return false;
    }

    public static Result apply(ServerPlayer player, boolean fix) {
        return fix ? Result.ALREADY_FIXED : Result.NOT_FIXED;
    }
}
