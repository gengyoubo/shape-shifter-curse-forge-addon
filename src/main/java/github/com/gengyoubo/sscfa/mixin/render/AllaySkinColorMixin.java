package github.com.gengyoubo.sscfa.mixin.render;

import github.com.gengyoubo.sscfa.client.renderer.AllaySkinColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AllaySkinColorMixin {

    @Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true)
    private void ssca$colorExposedSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(AllaySkinColor.texture((AbstractClientPlayer) (Object) this, cir.getReturnValue()));
    }
}
