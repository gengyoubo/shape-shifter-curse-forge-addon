package github.com.gengyoubo.sscfa.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 铚樿洓璺宠洓锛圫pider Salticidae锛夊舰鎬佸姞鎴愬崰浣嶅疄鐜帮細寰?Apoli perk 灞傜Щ妞嶅悗鏇挎崲銆?*/
public final class SpiderSalticidaePerks {
    private SpiderSalticidaePerks() {}

    public static boolean beginSkill(ServerPlayer player, String skill, boolean venom) {
        return true;
    }

    public static int chargeTicks(Player player, int base) {
        return base;
    }

    public static float killHealing(Player player) {
        return 0f;
    }

    public static void rewardDirectHit(ServerPlayer player, LivingEntity target, float healthLost, float healing, int strengthTicks) {
    }

    public static void setSensing(ServerPlayer player, boolean charging) {
    }

    public static int strengthTicks(Player player) {
        return 0;
    }

    public static double venomMultiplier(Player player) {
        return 1.0;
    }
}
