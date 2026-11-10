package github.com.gengyoubo.sscfa.mixin.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.sscfa.client.NightmareFearClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 食梦魔「恐惧」粉雾：恐惧目标本人 16 格粉色雾墙，按强度渐进淡入。 */
@Mixin(FogRenderer.class)
public class FearFogMixin {

    @Inject(method = "setupFog", at = @At("RETURN"), require = 0)
    private static void ssca$fearFog(Camera camera, FogRenderer.FogMode fogMode, float viewDistance,
                                     boolean thickFog, float tickDelta, CallbackInfo ci) {
        float strength = NightmareFearClient.getFogStrength(tickDelta);
        if (strength <= 0.0F) return;
        if (camera.getFluidInCamera() != FogType.NONE) return;
        float targetStart = fogMode == FogRenderer.FogMode.FOG_SKY ? 0.0F : 12.0F;
        float targetEnd = 16.0F;
        float curStart = RenderSystem.getShaderFogStart();
        float curEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(lerp(curStart, targetStart, strength));
        RenderSystem.setShaderFogEnd(lerp(curEnd, targetEnd, strength));
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
        RenderSystem.setShaderFogColor(1.0F, 0.4314F, 0.7804F, strength);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
