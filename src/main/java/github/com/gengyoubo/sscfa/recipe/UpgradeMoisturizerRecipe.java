package github.com.gengyoubo.sscfa.recipe;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import github.com.gengyoubo.sscfa.item.PortableMoisturizerItem;
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

/**
 * 便携加湿器升级配方（固定 3x3 图案，保留原充能，不超新等级上限）：
 * 1→2：四角湿海绵 + 四边金锭 + 中心一级加湿器。
 * 2→3：四角海洋之心 + 四边钻石 + 中心二级加湿器。
 */
public class UpgradeMoisturizerRecipe extends CustomRecipe {
	private static final int[] CORNERS = {0, 2, 6, 8};
	private static final int[] EDGES = {1, 3, 5, 7};

	public UpgradeMoisturizerRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	private static Item cornerMaterial(int level) {
		return level == 1 ? Items.WET_SPONGE : Items.HEART_OF_THE_SEA;
	}

	private static Item edgeMaterial(int level) {
		return level == 1 ? Items.GOLD_INGOT : Items.DIAMOND;
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		if (inventory.getWidth() != 3 || inventory.getHeight() != 3) return false;
		ItemStack center = inventory.getItem(4);
		if (!center.is(AddonItems.PORTABLE_MOISTURIZER.get())) return false;
		int level = PortableMoisturizerItem.getLevel(center);
		if (level >= PortableMoisturizerItem.MAX_LEVEL) return false;
		Item corner = cornerMaterial(level);
		Item edge = edgeMaterial(level);
		for (int i : CORNERS) if (!inventory.getItem(i).is(corner)) return false;
		for (int i : EDGES) if (!inventory.getItem(i).is(edge)) return false;
		return true;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryManager) {
		ItemStack center = inventory.getItem(4);
		if (!center.is(AddonItems.PORTABLE_MOISTURIZER.get())) return ItemStack.EMPTY;
		int level = PortableMoisturizerItem.getLevel(center);
		if (level >= PortableMoisturizerItem.MAX_LEVEL) return ItemStack.EMPTY;
		ItemStack result = center.copy();
		result.setCount(1);
		PortableMoisturizerItem.setLevel(result, level + 1);
		PortableMoisturizerItem.setCharge(result, PortableMoisturizerItem.getCharge(center));
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 3 && height >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.UPGRADE_MOISTURIZER.get();
	}
}
