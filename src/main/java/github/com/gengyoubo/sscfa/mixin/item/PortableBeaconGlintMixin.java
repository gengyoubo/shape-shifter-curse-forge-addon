package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.ability.AllaySPPortableBeacon;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class PortableBeaconGlintMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void ssca$activeBeaconGlint(CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (AllaySPPortableBeacon.isActiveStack(stack)
                || github.com.gengyoubo.sscfa.ability.AllaySPTotem.isMarkedActiveTotem(stack)) cir.setReturnValue(true);
    }
}
