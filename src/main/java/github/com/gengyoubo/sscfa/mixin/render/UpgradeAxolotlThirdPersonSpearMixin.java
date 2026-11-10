package github.com.gengyoubo.sscfa.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.client.UpgradeAxolotlSpearRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 进化美西螈「投掷水矛」蓄力期：第三人称渲染手持水矛并略后仰；血雾/真隐身时隐藏手持物。 */
@Mixin(ItemInHandLayer.class)
public class UpgradeAxolotlThirdPersonSpearMixin {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"),
            require = 0
    )
    private ItemStack sscAddon$swapTpSpear(LivingEntity entity, Operation<ItemStack> original) {
        if (entity instanceof AbstractClientPlayer
                && UpgradeAxolotlSpearRenderState.isCharging(entity.getUUID())) {
            ItemStack spear = new ItemStack(SscAddon.WATER_SPEAR);
            spear.getOrCreateTag().putInt("CustomModelData", 1);
            return spear;
        }
        return original.call(entity);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void sscAddon$raiseTpPush(LivingEntity entity, ItemStack stack, ItemDisplayContext mode, HumanoidArm arm,
                                      PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (arm == entity.getMainArm() && UpgradeAxolotlSpearRenderState.isCharging(entity.getUUID())) {
            matrices.pushPose();
            matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void sscAddon$raiseTpPop(LivingEntity entity, ItemStack stack, ItemDisplayContext mode, HumanoidArm arm,
                                     PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (arm == entity.getMainArm() && UpgradeAxolotlSpearRenderState.isCharging(entity.getUUID())) {
            matrices.popPose();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void sscAddon$hideHeldItemDuringMist(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                                                 LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                 float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                                 CallbackInfo ci) {
        if (entity.hasEffect(SscAddon.MIST_FORM) || entity.hasEffect(SscAddon.TRUE_INVISIBILITY)) {
            ci.cancel();
        }
    }
}
