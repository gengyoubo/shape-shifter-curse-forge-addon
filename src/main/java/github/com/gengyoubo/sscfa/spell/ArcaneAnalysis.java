package github.com.gengyoubo.sscfa.spell;

import net.minecraft.world.item.ItemStack;

public final class ArcaneAnalysis {
    public static final String UNANALYZED = "SscaUnanalyzed";

    private ArcaneAnalysis() {}

    public static boolean isUnanalyzed(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(UNANALYZED);
    }

    public static ItemStack markUnanalyzed(ItemStack stack) {
        if (!stack.isEmpty()) stack.getOrCreateTag().putBoolean(UNANALYZED, true);
        return stack;
    }

    public static void identify(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove(UNANALYZED);
    }
}