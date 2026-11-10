package github.com.gengyoubo.sscfa.item;

// [forge-port] removed yarn wildcard import net.minecraft.item.*
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/** Item-aware recipes: only drinking bottles may produce full-strength moon-dust potions. */
public final class MoonDustBrewing {
    private MoonDustBrewing() {}
    public static Item output(ItemStack input, ItemStack ingredient) {
        if (input.is(Items.POTION) && PotionUtils.getPotion(input) == Potions.AWKWARD
                && ingredient.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST)) return MoonDustReliefItem.BOTTLE;
        if (input.is(MoonDustReliefItem.BOTTLE) && ingredient.is(Items.GUNPOWDER)) return MoonDustReliefItem.SPLASH;
        return null;
    }
    public static ItemStack craft(ItemStack input, Item output) {
        ItemStack result = new ItemStack(output, input.getCount());
        if (input.hasTag()) result.setTag(input.getTag().copy());
        // Custom relief items own their dose; don't carry an awkward/base effect into the thrown entity.
        result.getOrCreateTag().remove("Potion");
        result.getTag().remove("CustomPotionEffects");
        return result;
    }
    public static boolean isBottle(ItemStack stack) {
        return stack.is(MoonDustReliefItem.BOTTLE) || stack.is(MoonDustReliefItem.SPLASH);
    }
}
