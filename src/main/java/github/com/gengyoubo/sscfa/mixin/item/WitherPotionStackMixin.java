package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.compat.PotionStackLimit;
import github.com.gengyoubo.sscfa.item.PotionTestTubes;
import github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.WitherPotionItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 版：附属药水按形态/power 分档堆叠（依据上游源码重写为 Mojang 映射）。 */
@Mixin(value = Slot.class, priority = 900)
public abstract class WitherPotionStackMixin {

    @Shadow
    @Final
    public Container container;

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void ssc_addon$addonPotionStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!(this.container instanceof Inventory playerInventory)) {
            return;
        }
        if (PotionTestTubes.hasCustomStackLimit(stack)) {
            cir.setReturnValue(PotionTestTubes.stackLimit(stack, playerInventory.player));
            return;
        }
        if (stack.getItem() instanceof WitherPotionItem) {
            int limit = WitherPotionItem.getStackLimitFor(playerInventory.player);
            if (limit > 1) {
                cir.setReturnValue(Math.max(cir.getReturnValue(), limit));
            }
            return;
        }
        if (stack.getItem() instanceof UniversalEnergyPotionItem) {
            int limit = PotionStackLimit.of(playerInventory.player);
            if (limit > 1) {
                cir.setReturnValue(Math.max(cir.getReturnValue(), limit));
            }
        }
    }
}
