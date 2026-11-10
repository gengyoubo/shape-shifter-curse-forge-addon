package github.com.gengyoubo.sscfa.mixin.render;

import github.com.gengyoubo.sscfa.util.IrisShadowPassDetector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 淇 Apoli LivingEntityRendererMixin.preventFeatureRendering 鍦?Iris 闃村奖
 * 娓叉煋閲嶅叆璺緞涓嬭Е鍙戠殑 NullPointerException 宕╂簝锛坰ourceStage==null锛夈€?
 *
 * 鍘熺悊锛氬綋 Iris 姝ｅ湪娓叉煋闃村奖 pass 鏃讹紝杩涘叆 LivingEntityRenderer.render 鏈熼棿
 * 璁剧疆 ThreadLocal 鏍囧織锛岃 PowerHolderComponent.getPowers 鏆傛椂杩斿洖绌哄垪琛紝
 * 浣?Apoli 鐨?anyMatch 鍒ゆ柇鐭矾涓?false锛屼粠鑰岃烦杩囧穿婧冭矾寰勮€屼笉褰卞搷瑙嗚
 * 锛堥槾褰?pass 涓帺瀹?feature 缁嗚妭鏈氨涓嶅彲瑙侊級銆?
 *
 * 姝ｅ父涓绘覆鏌?pass 涓嶅彈褰卞搷锛孉poli 鍘熷姛鑳藉畬鏁翠繚鐣欍€?
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererShadowFixMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void sscAddon$markShadowSuppressionStart(LivingEntity entity, float f, float g,
                                                     com.mojang.blaze3d.vertex.PoseStack matrixStack,
                                                     net.minecraft.client.renderer.MultiBufferSource vertexConsumerProvider,
                                                     int i, CallbackInfo ci) {
        if (IrisShadowPassDetector.isRenderingShadowPass()) {
            IrisShadowPassDetector.enterSuppressScope();
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void sscAddon$markShadowSuppressionEnd(LivingEntity entity, float f, float g,
                                                   com.mojang.blaze3d.vertex.PoseStack matrixStack,
                                                   net.minecraft.client.renderer.MultiBufferSource vertexConsumerProvider,
                                                   int i, CallbackInfo ci) {
        if (IrisShadowPassDetector.shouldSuppressPowerQuery()) {
            IrisShadowPassDetector.exitSuppressScope();
        }
    }
}
