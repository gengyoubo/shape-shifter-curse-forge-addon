package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 荧光幼灵天赋（Forge 简化）：保留技能所需的查询入口，暂不加成。 */
public final class FluorescentAxolotlPerks {
    private FluorescentAxolotlPerks() {}
    public static boolean hasWaterMagicEfficiency(Player player) { return false; }
    public static float enhancedLaserDamage(float damage, boolean efficient) { return damage; }
    public static void onLaserHit(Player player) {}
    public static int laserChargeTicks(int base, boolean efficient) { return base; }
    public static double tidalFlightSpeed(double speed, boolean efficient) { return speed; }
    public static void onTidalHit(Player player) {}
}
