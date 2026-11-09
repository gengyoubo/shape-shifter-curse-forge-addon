package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import github.com.gengyoubo.sscfa.item.PortableMoisturizerItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 便携加湿器 + 美西螈桶（无序）→ 按当前等级充满使用时间，保留其余 NBT。 */
public class RefillMoisturizerRecipe extends CustomRecipe {
	public RefillMoisturizerRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		boolean hasMoisturizer = false;
		boolean hasBucket = false;
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.is(AddonItems.PORTABLE_MOISTURIZER.get()) && !hasMoisturizer) {
				hasMoisturizer = true;
			} else if (stack.is(Items.AXOLOTL_BUCKET) && !hasBucket) {
				hasBucket = true;
			} else {
				return false;
			}
		}
		return hasMoisturizer && hasBucket;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryManager) {
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(AddonItems.PORTABLE_MOISTURIZER.get())) {
				ItemStack moisturizer = stack.copy();
				moisturizer.setCount(1);
				PortableMoisturizerItem.setFullCharge(moisturizer);
				return moisturizer;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.REFILL_MOISTURIZER.get();
	}
}
