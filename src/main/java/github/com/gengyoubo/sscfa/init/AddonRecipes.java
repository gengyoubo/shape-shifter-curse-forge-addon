package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.recipe.InfiniteEnergyPotionRecipe;
import github.com.gengyoubo.sscfa.recipe.SpUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SscAddonForge.MOD_ID);
    public static final RegistryObject<RecipeSerializer<InfiniteEnergyPotionRecipe>> INFINITE_ENERGY_POTION =
            SERIALIZERS.register("infinite_energy_potion_crafting", () -> new SimpleCraftingRecipeSerializer<>(InfiniteEnergyPotionRecipe::new));
    public static final RegistryObject<RecipeSerializer<SpUpgradeRecipe>> SP_UPGRADE =
            SERIALIZERS.register("sp_upgrade_crafting", () -> new SimpleCraftingRecipeSerializer<>(SpUpgradeRecipe::new));
    private AddonRecipes() {}
}
