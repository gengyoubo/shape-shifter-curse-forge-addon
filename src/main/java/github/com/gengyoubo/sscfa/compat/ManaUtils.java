package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/** 法力资源兼容层：映射到 SSC Forge 的 FormActivePowerService。 */
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

    /** SSC Forge 未声明 mana 类型的形态使用的通用池，与 Fabric 版 ManaTypeID 同名。 */
    public static final String GENERIC_MANA_TYPE = "shape-shifter-curse:generic_mana";

    /**
     * 当前形态的 mana 类型 ID：与 SSC Forge {@code FormActivePowerService} 相同的规则，取激活的
     * {@code shape-shifter-curse:mana_type_power} 中最后一个 {@code mana_type}，没有时为通用池。
     * Forge 与 Fabric 版的类型 ID 一致，旧档 {@code ManaTypeID} 可直接比较。
     */
    public static String getPlayerManaTypeID(Player player) {
        if (player == null) return GENERIC_MANA_TYPE;
        final String[] type = {GENERIC_MANA_TYPE};
        net.onixary.shapeShifterCurseForge.power.FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:mana_type_power".equals(
                    net.onixary.shapeShifterCurseForge.power.FormPowerRegistry.typeOf(power))
                    && power.has("mana_type")) {
                type[0] = power.get("mana_type").getAsString();
            }
        });
        return type[0];
    }

    public static void gainPlayerMana(Player player, double amount) {
        FormActivePowerService.gainMana(player, (float) amount);
    }
}
