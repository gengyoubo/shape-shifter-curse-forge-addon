package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 椋庣伒锛圵ind Spirit锛夊舰鎬佸姞鎴愬崰浣嶅疄鐜帮細寰?Apoli perk 灞傜Щ妞嶅悗鏇挎崲銆?*/
public final class WindSpiritPerks {
    private WindSpiritPerks() {}

    public static int hungerDiscount(Player player) { return 0; }

    public static boolean hasLongPounce(Player player) {
        return false;
    }

    public static double dashRange(double base, boolean enhanced) {
        return enhanced ? base * 1.5 : base;
    }

    public static double landingRadius(double base, boolean enhanced) {
        return enhanced ? base * 4.0 / 3.0 : base;
    }

    public static float landingDamage(float base, boolean enhanced) {
        return enhanced ? base * 4.0f / 3.0f : base;
    }
}
