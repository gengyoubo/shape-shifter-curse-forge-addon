package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.InfiniteEnergyPotionItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class BrewingPotionSlotMixin {

    @Inject(method = "mayPlaceItem", at = @At("HEAD"), cancellable = true)
    private static void ssc_addon$allowInfinitePotion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof InfiniteEnergyPotionItem
                || stack.getItem() instanceof github.com.gengyoubo.sscfa.item.WitherPotionItem
                || stack.getItem() instanceof github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem) {
            cir.setReturnValue(true);
        }
    }
}

