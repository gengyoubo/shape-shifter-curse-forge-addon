package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.recipe.*;
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
    public static final RegistryObject<RecipeSerializer<RefillMoisturizerRecipe>> REFILL_MOISTURIZER =
            SERIALIZERS.register("refill_moisturizer", () -> new SimpleCraftingRecipeSerializer<>(RefillMoisturizerRecipe::new));
    public static final RegistryObject<RecipeSerializer<UpgradeMoisturizerRecipe>> UPGRADE_MOISTURIZER =
            SERIALIZERS.register("upgrade_moisturizer", () -> new SimpleCraftingRecipeSerializer<>(UpgradeMoisturizerRecipe::new));
    public static final RegistryObject<RecipeSerializer<ReloadSnowballLauncherRecipe>> RELOAD_SNOWBALL_LAUNCHER =
            SERIALIZERS.register("reload_snowball_launcher", () -> new SimpleCraftingRecipeSerializer<>(ReloadSnowballLauncherRecipe::new));
    public static final RegistryObject<RecipeSerializer<BlizzardTankRechargeRecipe>> BLIZZARD_TANK_RECHARGE =
            SERIALIZERS.register("blizzard_tank_recharge", () -> new SimpleCraftingRecipeSerializer<>(BlizzardTankRechargeRecipe::new));
    public static final RegistryObject<RecipeSerializer<VenomGlandRecipe>> VENOM_GLAND =
            SERIALIZERS.register("venom_gland_crafting", () -> new SimpleCraftingRecipeSerializer<>(VenomGlandRecipe::new));
    public static final RegistryObject<RecipeSerializer<TestTubeFillingRecipe>> TEST_TUBE_FILLING =
            SERIALIZERS.register("test_tube_filling", () -> new SimpleCraftingRecipeSerializer<>(TestTubeFillingRecipe::new));
    private AddonRecipes() {}
}
