package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.effect.UniversalEnergyEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SscAddonForge.MOD_ID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, SscAddonForge.MOD_ID);
    public static final RegistryObject<MobEffect> UNIVERSAL_ENERGY = EFFECTS.register("universal_energy", UniversalEnergyEffect::new);
    public static final RegistryObject<Potion> UNIVERSAL_ENERGY_POTION = POTIONS.register("universal_energy", () ->
            new Potion(new MobEffectInstance(UNIVERSAL_ENERGY.get(), 1)));

    public static void registerBrewing() {
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.UNIVERSAL_ENERGY_POTION.get()),
                Ingredient.of(Items.GUNPOWDER), new ItemStack(AddonItems.UNIVERSAL_ENERGY_POTION_SPLASH.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.UNIVERSAL_ENERGY_POTION_SPLASH.get()),
                Ingredient.of(Items.DRAGON_BREATH), new ItemStack(AddonItems.UNIVERSAL_ENERGY_POTION_LINGERING.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.INFINITE_ENERGY_POTION.get()),
                Ingredient.of(Items.GUNPOWDER), new ItemStack(AddonItems.INFINITE_ENERGY_POTION_SPLASH.get())));
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(Ingredient.of(AddonItems.INFINITE_ENERGY_POTION_SPLASH.get()),
                Ingredient.of(Items.DRAGON_BREATH), new ItemStack(AddonItems.INFINITE_ENERGY_POTION_LINGERING.get())));
    }
    private AddonEffects() {}
}
