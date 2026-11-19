package github.com.gengyoubo.sscfa.compat;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

/** 饰品槽条件：通过 SSC Forge 的 {@link AccessoryUtils} 查询（未安装饰品模组时槽位为空，按未佩戴处理）。 */
public final class TrinketsConditionAction {
    private TrinketsConditionAction() {}

    /**
     * @param mode  SSC 饰品模组标识，{@code "auto"} 为当前启用的饰品模组
     * @param index 槽位序号；小于 0 时检查该槽类型下的全部槽位
     */
    public static boolean CheckEquipped(Player player, String mode, String group, String slot, int index,
                                        Predicate<ItemStack> predicate, boolean invert) {
        return invert != equipped(player, mode, group, slot, index, predicate);
    }

    private static boolean equipped(Player player, String mode, String group, String slot, int index,
                                    Predicate<ItemStack> predicate) {
        if (player == null || predicate == null) return false;
        List<ItemStack> stacks;
        try {
            stacks = AccessoryUtils.getEntitySlot(player, mode, group, slot);
        } catch (RuntimeException | LinkageError unavailable) {
            return false;
        }
        if (stacks == null || stacks.isEmpty()) return false;
        if (index >= 0) {
            if (index >= stacks.size()) return false;
            ItemStack stack = stacks.get(index);
            return stack != null && !stack.isEmpty() && predicate.test(stack);
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty() && predicate.test(stack)) return true;
        }
        return false;
    }
}
