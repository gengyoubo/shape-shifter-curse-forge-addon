package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 美西螈 SP（Axolotl SP）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class AxolotlSpPerks {
    private AxolotlSpPerks() {}

    public static boolean hasWaterMagicEfficiency(Player player) {
        return false;
    }

    public static int vortexAirCost(int baseTotal, boolean discounted) {
        return baseTotal;
    }
}
