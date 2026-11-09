package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 新星（Nova）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class NovaPerks {
    private NovaPerks() {}

    public static boolean hasLongPounce(Player player) {
        return false;
    }

    public static double horizontalSpeed(double base, boolean enhanced) {
        return enhanced ? base * 1.25 : base;
    }

    public static int fullCooldown(int base, boolean enhanced) {
        return enhanced ? (int) Math.round(base * 0.8) : base;
    }

    public static int singleCooldown(int base, boolean enhanced) {
        return enhanced ? (int) Math.round(base * 5.0 / 6.0) : base;
    }
}
