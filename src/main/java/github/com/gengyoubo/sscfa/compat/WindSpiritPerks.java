package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** perk 效果；能力由 AddonPerkTrees 镜像的豹猫树授予。远距扑袭按既有要求停用。 */
public final class WindSpiritPerks {
    private WindSpiritPerks() {}

    public static final net.minecraft.resources.ResourceLocation METABOLISM_1 =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("my_addon", "perk_wind_spirit_metabolism_1");
    public static final net.minecraft.resources.ResourceLocation METABOLISM_2 =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("my_addon", "perk_wind_spirit_metabolism_2");

    /** 代谢 perk 的饥饿消耗折扣（百分比）：II 级 25%，I 级 15%。 */
    public static int hungerDiscount(Player player) {
        if (player == null || !github.com.gengyoubo.sscfa.util.FormUtils.isForm(player,
                github.com.gengyoubo.sscfa.util.FormIdentifiers.OCELOT_SP)) return 0;
        if (github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(player, METABOLISM_2)) return 25;
        return github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(player, METABOLISM_1) ? 15 : 0;
    }

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
