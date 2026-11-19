package github.com.gengyoubo.sscfa.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.SscManaApi;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/**
 * 法力资源兼容层。写入走 SSC Forge 的 {@link SscManaApi}（服务端，自动钳制并同步）：
 * 回复经 {@code SscManaRecoveryEvent}，直接设值（恢复旧档、补满、管理命令）绕过回复事件。
 * 读取在双端都可用：客户端读 SSC 同步过来的值。
 */
public final class ManaUtils {
    /** SSC Forge 未声明 mana 类型的形态使用的通用池，与 Fabric 版 ManaTypeID 同名。 */
    public static final String GENERIC_MANA_TYPE = "shape-shifter-curse:generic_mana";

    private ManaUtils() {}

    public static ManaComponent getManaComponent(Player player) {
        return ManaComponent.of(player);
    }

    public static double getPlayerMana(Player player) {
        return FormActivePowerService.mana(player);
    }

    /** 包含能力与饰品加成的上限。 */
    public static double getPlayerMaxMana(Player player) {
        return FormActivePowerService.maximumMana(player);
    }

    /** 直接设值（钳制到 0..上限），不经回复事件。 */
    public static void setPlayerMana(Player player, double value) {
        if (!(player instanceof ServerPlayer server) || !Double.isFinite(value)) return;
        SscManaApi.setMana(server, (float) value);
    }

    /**
     * 当前形态的 mana 类型 ID：取激活的 {@code shape-shifter-curse:mana_type_power} 中最后一个
     * {@code mana_type}，没有时为通用池。Forge 与 Fabric 版类型 ID 一致，旧档 {@code ManaTypeID} 可直接比较。
     */
    public static String getPlayerManaTypeID(Player player) {
        return player == null ? GENERIC_MANA_TYPE : FormActivePowerService.manaType(player);
    }

    /** 正数为回复（经回复事件，可被通用法阵充能等取消）；负数为扣除，不足时扣到 0。 */
    public static void gainPlayerMana(Player player, double amount) {
        if (!(player instanceof ServerPlayer server) || !Double.isFinite(amount) || amount == 0) return;
        if (amount > 0) SscManaApi.gainMana(server, (float) amount);
        else SscManaApi.setMana(server, SscManaApi.mana(server) + (float) amount);
    }
}
