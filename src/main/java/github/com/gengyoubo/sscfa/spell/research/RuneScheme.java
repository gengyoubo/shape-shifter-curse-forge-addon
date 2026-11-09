package github.com.gengyoubo.sscfa.spell.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import github.com.gengyoubo.sscfa.spell.Spell;

/** 符文方案（Forge 占位）：校验通过、无加成。完整符文研究系统待研究层移植后补回。 */
public final class RuneScheme {
    public static final String KEY = "RuneScheme";
    private RuneScheme() {}
    public static boolean isModified(ItemStack stack) { return false; }
    public static boolean validScroll(ServerPlayer player, ItemStack scroll, Spell spell) { return true; }
    public static RuneModifiers modifiers(Player player, ItemStack scroll, int selectedLevel) {
        return RuneModifiers.NONE;
    }
}
