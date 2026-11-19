package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** perk 效果；能力由 AddonPerkTrees 镜像的豹猫树授予。远距扑袭按既有要求停用。 */
public final class NovaPerks {
    private NovaPerks() {}

    public static final net.minecraft.resources.ResourceLocation METABOLISM_1 =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("my_addon", "perk_ocelot_nova_metabolism_1");
    public static final net.minecraft.resources.ResourceLocation METABOLISM_2 =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("my_addon", "perk_ocelot_nova_metabolism_2");

    /** 代谢 perk 的饥饿消耗折扣（百分比）：II 级 25%，I 级 15%。 */
    public static int hungerDiscount(Player player) {
        if (player == null || !github.com.gengyoubo.sscfa.util.FormUtils.isForm(player,
                github.com.gengyoubo.sscfa.util.FormIdentifiers.OCELOT_NOVA)) return 0;
        if (github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(player, METABOLISM_2)) return 25;
        return github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(player, METABOLISM_1) ? 15 : 0;
    }

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
