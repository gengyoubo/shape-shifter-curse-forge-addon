package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Trinkets 条件动作占位。 */
public final class TrinketsConditionAction {
    private TrinketsConditionAction() {}

    public static boolean CheckEquipped(Player player, String mode, String group, String slot, int index,
                                        Predicate<ItemStack> predicate, boolean invert) {
        return false;
    }
}
