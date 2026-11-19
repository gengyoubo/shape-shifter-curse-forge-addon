package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** 美西螈 SP 的 perk 效果；能力由 {@link AddonPerkTrees} 的 axolotl_sp 树授予。 */
public final class AxolotlSpPerks {
    public static final ResourceLocation WATER_MAGIC = addon("perk_axolotl_sp_water_magic_efficiency");
    public static final ResourceLocation TIDAL_PULL = addon("perk_axolotl_sp_tidal_pull");

    private AxolotlSpPerks() {}

    public static boolean hasWaterMagicEfficiency(Player player) {
        return player != null && FormUtils.isForm(player, FormIdentifiers.AXOLOTL_SP)
                && ResourceBars.hasPowerId(player, WATER_MAGIC);
    }

    /** 蓄力消耗的空气为整数：累计 60% 的账单只向上取整一次，而不是每次脉冲取整。 */
    public static int vortexAirCost(int baseTotal, boolean discounted) {
        if (baseTotal <= 0) return 0;
        return discounted ? (int) (((long) baseTotal * 3 + 4) / 5) : baseTotal;
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
