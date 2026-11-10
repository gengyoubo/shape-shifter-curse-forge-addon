package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 版：无限/凋零/通用能量药水的酿造组合（依据上游源码重写为 Mojang 映射）。 */
@Mixin(PotionBrewing.class)
public abstract class BrewingRegistryInfiniteMixin {

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private static void ssc_addon$hasInfiniteRecipe(ItemStack input, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (ssc_addon$infiniteOutput(input, ingredient) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private static void ssc_addon$craftInfinite(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        Item out = ssc_addon$infiniteOutput(input, ingredient);
        if (out != null) {
            cir.setReturnValue(new ItemStack(out));
        }
    }

    @Unique
    private static Item ssc_addon$infiniteOutput(ItemStack input, ItemStack ingredient) {
        if (input == null || input.isEmpty() || ingredient == null || ingredient.isEmpty()) {
            return null;
        }
        if (input.is(SscAddon.INFINITE_ENERGY_POTION) && ingredient.is(Items.GUNPOWDER)) {
            return SscAddon.INFINITE_ENERGY_POTION_SPLASH;
        }
        if (input.is(SscAddon.INFINITE_ENERGY_POTION_SPLASH) && ingredient.is(Items.DRAGON_BREATH)) {
            return SscAddon.INFINITE_ENERGY_POTION_LINGERING;
        }
        if (input.is(SscAddon.WITHER_POTION) && ingredient.is(Items.GUNPOWDER)) {
            return SscAddon.WITHER_POTION_SPLASH;
        }
        if (input.is(SscAddon.WITHER_POTION_SPLASH) && ingredient.is(Items.DRAGON_BREATH)) {
            return SscAddon.WITHER_POTION_LINGERING;
        }
        if (input.is(SscAddon.UNIVERSAL_ENERGY_POTION) && ingredient.is(Items.GUNPOWDER)) {
            return SscAddon.UNIVERSAL_ENERGY_POTION_SPLASH;
        }
        if (input.is(SscAddon.UNIVERSAL_ENERGY_POTION_SPLASH) && ingredient.is(Items.DRAGON_BREATH)) {
            return SscAddon.UNIVERSAL_ENERGY_POTION_LINGERING;
        }
        return null;
    }
}
