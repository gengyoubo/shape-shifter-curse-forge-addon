package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.MoonDustBrewing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import github.com.gengyoubo.sscfa.compat.RegCustomItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Registry and both inventory paths must agree. Tubes are deliberately not potion-slot containers. */
@Mixin(PotionBrewing.class)
public abstract class MoonDustBrewingMixin {
    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void ssca$moonDustIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(RegCustomItem.UNTREATED_MOONDUST)) cir.setReturnValue(true);
    }
    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private static void ssca$moonDustRecipe(ItemStack input, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (MoonDustBrewing.output(input, ingredient) != null) cir.setReturnValue(true);
    }
    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private static void ssca$moonDustCraft(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        var output = MoonDustBrewing.output(input, ingredient);
        if (output != null) cir.setReturnValue(MoonDustBrewing.craft(input, output));
    }
}

@Mixin(BrewingStandBlockEntity.class)
abstract class MoonDustBrewingInventoryMixin {
    @Shadow public abstract ItemStack getItem(int slot);
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void ssca$moonDustBottle(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot >= 0 && slot < 3 && MoonDustBrewing.isBottle(stack)) cir.setReturnValue(getItem(slot).isEmpty());
    }
}

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
abstract class MoonDustBrewingSlotMixin {
    @Inject(method = "mayPlaceItem", at = @At("HEAD"), cancellable = true)
    private static void ssca$moonDustBottle(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (MoonDustBrewing.isBottle(stack)) cir.setReturnValue(true);
    }
}
