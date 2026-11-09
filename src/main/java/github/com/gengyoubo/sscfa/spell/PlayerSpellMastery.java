package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** 法术熟练度（Forge 占位）：经验/等级机制待 NeoMagic 运行时移植后补回。 */
public final class PlayerSpellMastery {
    public static final int EXPERIENCE_PER_LEVEL_TEN = 6000;

    private PlayerSpellMastery() {}

    public static int tier(int experience) {
        experience = Math.max(0, experience);
        return experience / EXPERIENCE_PER_LEVEL_TEN;
    }

    public static int thresholdTen(int level) {
        return EXPERIENCE_PER_LEVEL_TEN;
    }

    public static int progressTen(int experience) {
        experience = Math.max(0, experience);
        return experience % EXPERIENCE_PER_LEVEL_TEN;
    }

    public static int bonus(int experience) {
        return tier(experience) * 50;
    }

    public static void grant(ServerPlayer player, ItemStack book, int expTen) {
    }
}
