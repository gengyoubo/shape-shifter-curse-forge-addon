package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import github.com.gengyoubo.sscfa.item.PotionTestTubes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 一瓶可分装药水 + 三支空试管（无序）→ 三支装药试管；玻璃瓶留在原格子。 */
public final class TestTubeFillingRecipe extends CustomRecipe {
	public TestTubeFillingRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	private int bottleSlot(CraftingContainer inventory) {
		int bottle = -1, tubes = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.is(AddonItems.TEST_TUBE.get())) tubes++;
			else if (bottle == -1 && stack.getCount() == 1 && PotionTestTubes.isBottle(stack)) bottle = i;
			else return -1;
		}
		return tubes == PotionTestTubes.PORTIONS ? bottle : -1;
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		return bottleSlot(inventory) >= 0;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registries) {
		int slot = bottleSlot(inventory);
		return slot < 0 ? ItemStack.EMPTY : PotionTestTubes.divide(inventory.getItem(slot));
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
		var remainder = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
		int slot = bottleSlot(inventory);
		if (slot >= 0) remainder.set(slot, new ItemStack(Items.GLASS_BOTTLE));
		return remainder;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 4;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.TEST_TUBE_FILLING.get();
	}
}
