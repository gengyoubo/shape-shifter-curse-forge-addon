package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import github.com.gengyoubo.sscfa.item.PortableFridgeItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 便携冰箱（未满）+ 雪块（每块 8 充能）→ 充能，上限 64。 */
public class BlizzardTankRechargeRecipe extends CustomRecipe {
	public BlizzardTankRechargeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		ItemStack tank = ItemStack.EMPTY;
		boolean hasSnowBlock = false;
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.is(AddonItems.PORTABLE_FRIDGE.get())) {
				if (!tank.isEmpty()) return false;
				tank = stack;
			} else if (stack.is(Items.SNOW_BLOCK)) {
				hasSnowBlock = true;
			} else {
				return false;
			}
		}
		return !tank.isEmpty() && hasSnowBlock && PortableFridgeItem.getCharge(tank) < PortableFridgeItem.MAX_CHARGE;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryManager) {
		ItemStack tank = ItemStack.EMPTY;
		int chargeToAdd = 0;
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(AddonItems.PORTABLE_FRIDGE.get())) {
				tank = stack.copy();
			} else if (stack.is(Items.SNOW_BLOCK)) {
				chargeToAdd += 8;
			}
		}
		if (tank.isEmpty() || chargeToAdd <= 0) return ItemStack.EMPTY;
		PortableFridgeItem.setCharge(tank, PortableFridgeItem.getCharge(tank) + chargeToAdd);
		return tank;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.BLIZZARD_TANK_RECHARGE.get();
	}
}
