package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/** 荧光幼灵（含阿澪）的 perk 效果；能力由 {@link AddonPerkTrees} 的 axolotl_fluorescent 树授予。 */
public final class FluorescentAxolotlPerks {
    public static final ResourceLocation WATER_MAGIC = addon("perk_axolotl_fluorescent_water_magic_efficiency");
    public static final ResourceLocation MOISTURE_RETURN = addon("perk_axolotl_fluorescent_moisture_return");
    /** 湿润回流的两个子冷却（apoli:multiple 子能力 ID = 父 ID + "_" + 子键）。 */
    public static final ResourceLocation LASER_REFUND = addon("perk_axolotl_fluorescent_moisture_return_laser");
    public static final ResourceLocation TIDAL_REFUND = addon("perk_axolotl_fluorescent_moisture_return_tidal");
    private static final int REFUND_COOLDOWN = 20;

    private FluorescentAxolotlPerks() {}

    public static boolean isFluorescent(Player player) {
        return player != null && FormUtils.isAnyForm(player, FormIdentifiers.AXOLOTL_FLUORESCENT, FormIdentifiers.AXOLOTL_ALING);
    }

    public static boolean hasWaterMagicEfficiency(Player player) {
        return isFluorescent(player) && ResourceBars.hasPowerId(player, WATER_MAGIC);
    }

    public static float enhancedLaserDamage(float damage, boolean efficient) { return efficient ? damage * 1.25f : damage; }

    public static int laserChargeTicks(int base, boolean efficient) { return Math.max(1, base - (efficient ? 40 : 0)); }

    public static double tidalFlightSpeed(double speed, boolean efficient) { return efficient ? speed * 1.25 : speed; }

    /** 激光命中回复 10 点空气（湿润度），每名玩家每秒最多一次，跨目标与两级 perk 共用计时。 */
    public static void onLaserHit(Player player) { restoreMoisture(player, LASER_REFUND, 10); }

    /** 潮汐牵引命中回复 3 点空气，计时规则同上。 */
    public static void onTidalHit(Player player) { restoreMoisture(player, TIDAL_REFUND, 3); }

    private static boolean restoreMoisture(Player player, ResourceLocation timer, int amount) {
        if (player == null || player.level().isClientSide || !player.isAlive() || !isFluorescent(player)
                || !ResourceBars.hasPowerId(player, MOISTURE_RETURN)) return false;
        if (!FormActivePowerService.usePowerCooldown(player, timer, REFUND_COOLDOWN)) return false;
        player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + amount));
        return true;
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
