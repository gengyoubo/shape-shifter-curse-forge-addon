package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import github.com.gengyoubo.sscfa.item.SnowballLauncherItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 雪球发射器 + 雪球/雪层（各 1 发）/雪块（8 发）→ 装弹，上限 20。 */
public class ReloadSnowballLauncherRecipe extends CustomRecipe {
	public ReloadSnowballLauncherRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	private static int ammoValue(ItemStack stack) {
		if (stack.is(Items.SNOWBALL) || stack.is(Items.SNOW)) return 1;
		if (stack.is(Items.SNOW_BLOCK)) return 8;
		return 0;
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		boolean hasLauncher = false;
		boolean hasAmmo = false;
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.is(AddonItems.SNOWBALL_LAUNCHER.get())) {
				if (hasLauncher) return false;
				hasLauncher = true;
			} else if (ammoValue(stack) > 0) {
				hasAmmo = true;
			} else {
				return false;
			}
		}
		return hasLauncher && hasAmmo;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryManager) {
		ItemStack launcher = ItemStack.EMPTY;
		int ammoToAdd = 0;
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(AddonItems.SNOWBALL_LAUNCHER.get())) {
				launcher = stack.copy();
			} else {
				ammoToAdd += ammoValue(stack);
			}
		}
		if (launcher.isEmpty()) return ItemStack.EMPTY;
		SnowballLauncherItem.setAmmo(launcher, SnowballLauncherItem.getAmmo(launcher) + ammoToAdd);
		return launcher;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.RELOAD_SNOWBALL_LAUNCHER.get();
	}
}
