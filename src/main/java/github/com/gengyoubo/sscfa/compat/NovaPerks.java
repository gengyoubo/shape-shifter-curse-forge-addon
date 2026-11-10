package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 鏂版槦锛圢ova锛夊舰鎬佸姞鎴愬崰浣嶅疄鐜帮細寰?Apoli perk 灞傜Щ妞嶅悗鏇挎崲銆?*/
public final class NovaPerks {
    private NovaPerks() {}

    public static int hungerDiscount(Player player) { return 0; }

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
