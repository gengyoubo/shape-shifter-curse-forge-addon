package github.com.gengyoubo.sscfa.recipe;

import net.onixary.shapeShifterCurseForge.registry.ModPotions;
import github.com.gengyoubo.sscfa.init.AddonRecipes;
import github.com.gengyoubo.sscfa.init.AddonItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 无限压缩能量药水合成配方（特殊配方，需匹配带 NBT 的 feed_potion 药水，原版 shaped 无法匹配）。
 * 布局（3x3）：
 * <pre>
 *   0 M 0      M = 月髓环 (ssc_addon:sp_upgrade_thing)
 *   A I A      A = 附魔金苹果 (minecraft:enchanted_golden_apple)
 *   0 0 0      I = 压缩能量药水 (原版药水 + feed_potion)
 * </pre>
 */
public class InfiniteEnergyPotionRecipe extends CustomRecipe {

	public InfiniteEnergyPotionRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level world) {
		if (inventory.getWidth() < 3 || inventory.getHeight() < 3) {
			return false;
		}
		// 在更大的工作台中也检查所有格子，避免额外物品被忽略并消耗。
		for (int y = 0; y <= inventory.getHeight() - 2; y++) {
			for (int x = 0; x <= inventory.getWidth() - 3; x++) {
				if (matchesLayout(inventory, x, y)) return true;
			}
		}
		return false;
	}

	/** 检查 3×2 图案及图案外的所有空格。 */
	private boolean matchesLayout(CraftingContainer inv, int left, int top) {
		for (int y = 0; y < inv.getHeight(); y++) {
			for (int x = 0; x < inv.getWidth(); x++) {
				ItemStack stack = inv.getItem(y * inv.getWidth() + x);
				int dx = x - left, dy = y - top;
				boolean matches = dy == 0 && dx == 1 ? isMoonRing(stack)
						: dy == 1 && (dx == 0 || dx == 2) ? isEnchantedGoldenApple(stack)
						: dy == 1 && dx == 1 ? isCompressedEnergyPotion(stack)
						: stack.isEmpty();
				if (!matches) return false;
			}
		}
		return true;
	}

	private boolean isMoonRing(ItemStack stack) {
		return stack.is(AddonItems.SP_UPGRADE_THING.get());
	}

	private boolean isEnchantedGoldenApple(ItemStack stack) {
		return stack.is(Items.ENCHANTED_GOLDEN_APPLE);
	}

	/** 压缩能量药水 = 饮用/喷溅/滞留三种瓶型之一，且药水类型为 feed_potion。 */
	private boolean isCompressedEnergyPotion(ItemStack stack) {
		boolean isPotionBottle = stack.is(Items.POTION)
				|| stack.is(Items.SPLASH_POTION)
				|| stack.is(Items.LINGERING_POTION);
		return isPotionBottle && PotionUtils.getPotion(stack) == ModPotions.FEED.get();
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryManager) {
		return new ItemStack(AddonItems.INFINITE_ENERGY_POTION.get());
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 3 && height >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AddonRecipes.INFINITE_ENERGY_POTION.get();
	}
}
