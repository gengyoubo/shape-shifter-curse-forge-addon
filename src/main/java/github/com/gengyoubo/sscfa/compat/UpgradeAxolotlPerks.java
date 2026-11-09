package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 升级美西螈（Upgrade Axolotl）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class UpgradeAxolotlPerks {
    private UpgradeAxolotlPerks() {}

    public static int landDashAirCost(Player player, int base) {
        return Math.max(0, base);
    }

    public static int spearAirCost(Player player, int base) {
        return Math.max(0, base);
    }
}
