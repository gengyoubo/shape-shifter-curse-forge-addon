package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/** 娉曞姏璧勬簮鍏煎灞傦細鏄犲皠鍒?Forge SSC 鐨?FormActivePowerService銆?*/
public final class ManaUtils {
    private ManaUtils() {}

    public static ManaComponent getManaComponent(Player player) {
        return ManaComponent.of(player);
    }

    public static double getPlayerMana(Player player) {
        return FormActivePowerService.mana(player);
    }

    public static double getPlayerMaxMana(Player player) {
        float pct = FormActivePowerService.manaPercent(player);
        float cur = FormActivePowerService.mana(player);
        if (pct > 1.0e-4f) return cur / pct;
        return 20.0;
    }

    public static void setPlayerMana(Player player, double value) {
        double cur = getPlayerMana(player);
        double max = getPlayerMaxMana(player);
        double clamped = Math.max(0.0, Math.min(max, value));
        if (clamped >= cur) {
            FormActivePowerService.gainMana(player, (float) (clamped - cur));
        } else {
            FormActivePowerService.consumeMana(player, (float) (cur - clamped));
        }
    }

    public static String getPlayerManaTypeID(Player player) {
        return "";
    }

    public static void gainPlayerMana(Player player, double amount) {
        FormActivePowerService.gainMana(player, (float) amount);
    }
}
