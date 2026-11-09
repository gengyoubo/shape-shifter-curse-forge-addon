package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 风灵（Wind Spirit）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class WindSpiritPerks {
    private WindSpiritPerks() {}

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
