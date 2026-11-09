package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/** 法力资源兼容层：映射到 Forge SSC 的 FormActivePowerService。 */
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

    public static void gainPlayerMana(Player player, double amount) {
        FormActivePowerService.gainMana(player, (float) amount);
    }
}
