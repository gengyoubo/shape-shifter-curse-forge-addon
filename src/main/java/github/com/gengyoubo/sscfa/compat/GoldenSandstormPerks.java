package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 金砂暴（Golden Sandstorm）形态加成占位实现：待 Apoli perk 层移植后替换。 */
public final class GoldenSandstormPerks {
    private GoldenSandstormPerks() {}

    public static int responseRank(Player player) {
        return 0;
    }

    public static int normalCooldown(int baseTicks, int rank) {
        return baseTicks;
    }

    public static int sandstormStacks(Player player) {
        return 1;
    }

    public static float damagePercent(Player player, float base) {
        return base;
    }

    public static float damageCap(Player player, float base) {
        return base;
    }

    public static float recallHealing(float maximumHealth, int layers) {
        return 0f;
    }
}
