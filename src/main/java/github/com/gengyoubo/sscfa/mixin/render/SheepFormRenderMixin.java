package github.com.gengyoubo.sscfa.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.client.SheepFormClientHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 变羊渲染替换：带 SHEEP_FORM 的活体改用原版羊模型绘制。 */
@Mixin(LivingEntityRenderer.class)
public abstract class SheepFormRenderMixin<T extends LivingEntity> {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void ssca$renderAsSheep(T entity, float yaw, float tickDelta, PoseStack matrices,
                                    MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (!entity.hasEffect(SscAddon.SHEEP_FORM) || entity.isInvisible() || entity.isSpectator()
                || entity.deathTime > 0) {
            return;
        }
        if (SheepFormClientHooks.renderAsSheep(entity, yaw, tickDelta, matrices, vertexConsumers, light)) {
            ci.cancel();
        }
    }
}
