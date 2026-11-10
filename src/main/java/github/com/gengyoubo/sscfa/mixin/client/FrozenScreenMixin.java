package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FrozenScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void ssca$blockInventory(Screen screen, CallbackInfo ci) {
        if (screen instanceof AbstractContainerScreen<?> && FrozenControl.blocked(((Minecraft)(Object)this).player)) ci.cancel();
    }
}
