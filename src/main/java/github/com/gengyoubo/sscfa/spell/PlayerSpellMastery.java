package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** 娉曟湳鐔熺粌搴︼紙Forge 鍗犱綅锛夛細缁忛獙/绛夌骇鏈哄埗寰?NeoMagic 杩愯鏃剁Щ妞嶅悗琛ュ洖銆?*/
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

    public static int clientExpTen() { return 0; }

    public static void grant(ServerPlayer player, ItemStack book, int expTen) {
    }
}
