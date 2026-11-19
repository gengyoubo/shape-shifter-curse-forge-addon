package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Upgraded axolotl's versions of the axolotl perks. Each perk is an addon power and counts while
 * the player is in this form and holds it.
 *
 * <p>The powers come from the upgrade_axolotl perk tree that {@link AddonPerkTrees} mirrors from
 * SSC's axolotl tree (the Fabric version remapped the original purchases in {@code loadMapped}).</p>
 */
public final class UpgradeAxolotlPerks {
    public static final ResourceLocation VEGETATION = addon("perk_upgrade_axolotl_vegetation");
    public static final ResourceLocation DRY_TOLERANCE = addon("perk_upgrade_axolotl_dry_tolerance");
    public static final ResourceLocation PROPULSION = addon("perk_upgrade_axolotl_propulsion_efficiency");
    public static final ResourceLocation WAVE = addon("perk_upgrade_axolotl_wave_efficiency");
    public static final ResourceLocation WATER_MAGIC = addon("perk_upgrade_axolotl_water_magic_efficiency");
    public static final ResourceLocation TIDAL_PULL = addon("perk_upgrade_axolotl_tidal_pull");
    private static final int SPEAR_AIR_REDUCTION = 8;
    private static final double LAND_AIR_MULTIPLIER = 0.5;
    private static final BalanceReader BAL = new BalanceReader("abilities.upgrade_axolotl_perks");

    private UpgradeAxolotlPerks() {}

    public static boolean enabled(Player player, ResourceLocation id) {
        return player != null && FormUtils.isForm(player, FormIdentifiers.UPGRADE_AXOLOTL) && ResourceBars.hasPowerId(player, id);
    }

    /** Shared by the server charge and the client availability check; the configured floor is zero. */
    public static int spearAirCost(Player player, int base) {
        return Math.max(0, base - (enabled(player, WATER_MAGIC) ? BAL.i("spear_air_reduction", SPEAR_AIR_REDUCTION) : 0));
    }

    public static int landDashAirCost(Player player, int base) {
        return enabled(player, WAVE)
                ? Math.max(0, (int) Math.ceil(base * BAL.d("land_air_multiplier", LAND_AIR_MULTIPLIER)))
                : Math.max(0, base);
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
