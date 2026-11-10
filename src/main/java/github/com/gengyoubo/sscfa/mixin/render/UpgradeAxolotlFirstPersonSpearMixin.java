package github.com.gengyoubo.sscfa.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.client.UpgradeAxolotlSpearRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 进化美西螈「投掷水矛」蓄力期：第一人称把手持物品渲染成 3D 水矛并略后仰。 */
@Mixin(ItemInHandRenderer.class)
public class UpgradeAxolotlFirstPersonSpearMixin {

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack sscAddon$swapFpSpear(ItemStack item, AbstractClientPlayer player,
                                           float tickDelta, float pitch, InteractionHand hand) {
        if (player != null && UpgradeAxolotlSpearRenderState.isCharging(player.getUUID())) {
            ItemStack spear = new ItemStack(SscAddon.WATER_SPEAR);
            spear.getOrCreateTag().putInt("CustomModelData", 1);
            return spear;
        }
        return item;
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void sscAddon$raiseFpPush(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
                                      float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
                                      MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND && player != null
                && UpgradeAxolotlSpearRenderState.isCharging(player.getUUID())) {
            matrices.pushPose();
            matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void sscAddon$raiseFpPop(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
                                     float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
                                     MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND && player != null
                && UpgradeAxolotlSpearRenderState.isCharging(player.getUUID())) {
            matrices.popPose();
        }
    }
}
