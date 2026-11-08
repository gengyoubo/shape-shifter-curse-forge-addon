package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 月髓环的灵活合成：四角金锭、中心变形鳞核心，四个边位任意顺序放置
 * 绿宝石 / 红石 / 月尘水晶碎片 / 下界合金锭各一。
 */
public class SpUpgradeRecipe extends CustomRecipe {
	private static final int[] CORNERS = {0, 2, 6, 8};
	private static final int[] EDGES = {1, 3, 5, 7};

	public SpUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level level) {
		if (inventory.getWidth() != 3 || inventory.getHeight() != 3) return false;
		for (int slot : CORNERS) {
			if (!inventory.getItem(slot).is(Items.GOLD_INGOT)) return false;
		}
		if (!inventory.getItem(4).is(ModItems.MORPHSCALE_CORE.get())) return false;

		List<Item> remaining = new ArrayList<>(List.of(
				Items.EMERALD, Items.REDSTONE, ModItems.MOONDUST_CRYSTAL_SHARD.get(), Items.NETHERITE_INGOT));
		for (int slot : EDGES) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !remaining.remove(stack.getItem())) return false;
		}
		return remaining.isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
		return new ItemStack(AddonItems.SP_UPGRADE_THING.get());
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 3 && height >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.SP_UPGRADE.get();
	}
}
