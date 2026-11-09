package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 阿努比斯冥狼（Anubis Wolf SP）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class AnubisWolfSpPerks {
    private AnubisWolfSpPerks() {}

    public static int capacity(Player player, int base) {
        return base;
    }
}
