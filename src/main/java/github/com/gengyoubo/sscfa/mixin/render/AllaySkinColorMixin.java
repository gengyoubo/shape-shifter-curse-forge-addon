package github.com.gengyoubo.sscfa.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.com.gengyoubo.sscfa.client.renderer.AllaySkinColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayer.class)
public abstract class AllaySkinColorMixin {

    @WrapMethod(method = "getSkinTextureLocation")
    private ResourceLocation ssca$colorExposedSkin(Operation<ResourceLocation> original) {
        return AllaySkinColor.texture((AbstractClientPlayer) (Object) this, original.call());
    }
}
