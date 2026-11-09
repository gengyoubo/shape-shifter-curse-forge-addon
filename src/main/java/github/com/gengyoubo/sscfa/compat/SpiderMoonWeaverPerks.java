package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 月织蛛（Spider Moon Weaver）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class SpiderMoonWeaverPerks {
    private SpiderMoonWeaverPerks() {}

    public static boolean acceleratesCharge(Player player, int mode) {
        return false;
    }
}
