package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 毒液腺体：八个蜘蛛眼围住中心的剧毒药水（饮用/喷溅/滞留，普通/延长/II 均可）。
 * 原版有序配方无法按药水 NBT 匹配，因此用特殊配方。
 */
public class VenomGlandRecipe extends CustomRecipe {
	public VenomGlandRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		if (inventory.getWidth() < 3 || inventory.getHeight() < 3) return false;
		if (!isPoisonPotion(inventory.getItem(4))) return false;
		for (int i = 0; i < 9; i++) {
			if (i != 4 && !inventory.getItem(i).is(Items.SPIDER_EYE)) return false;
		}
		return true;
	}

	private static boolean isPoisonPotion(ItemStack stack) {
		if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) return false;
		Potion potion = PotionUtils.getPotion(stack);
		return potion == Potions.POISON || potion == Potions.LONG_POISON || potion == Potions.STRONG_POISON;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryManager) {
		return new ItemStack(AddonItems.VENOM_GLAND.get());
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 3 && height >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.VENOM_GLAND.get();
	}
}
