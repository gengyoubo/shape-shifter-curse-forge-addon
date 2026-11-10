package github.com.gengyoubo.sscfa.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.client.CastingVisualState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 施法第一人称视觉（jackcooper）：施法读条时双手（含手持物品）向前抬起，
 * 与第三人称举手动画（{@code SpellCastPoseMixin}）配套，对齐铁魔法举手施法的观感。
 * <p>
 * 在逐手渲染入口独立压入矩阵，抬手并错相摆动，返回时恢复矩阵。
 * 第一人称只有本人可见，第三人称由两套模型姿态 mixin 对应处理。
 */
@Mixin(ItemInHandRenderer.class)
public abstract class SpellCastFirstPersonMixin {

	@Inject(method = "renderArmWithItem",
			at = @At("HEAD"), require = 0)
	private void ssca$raiseFirstPersonHands(net.minecraft.client.player.AbstractClientPlayer player,
			float tickDelta, float pitch, net.minecraft.world.InteractionHand hand, float swingProgress,
			net.minecraft.world.item.ItemStack item, float equipProgress, PoseStack matrices,
			net.minecraft.client.renderer.MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
		matrices.pushPose();
		if (player == null || !CastingVisualState.localPlayerCasting()) return;
		// FERAL 兽形不播放举手动画（与第三人称 SpellCastPoseMixin 的排除一致）
		var form = github.com.gengyoubo.sscfa.util.FormUtils.getCurrentForm(player);
		if (form == null || form.bodyType() != net.onixary.shapeShifterCurseForge.form.FormBodyType.NORMAL) return;
		float progress = CastingVisualState.raiseProgress(player.getUUID());
		if (progress <= 0.0F) return;
		// 双手整体向前推 + 轻微上抬 + 内收（铁魔法施法观感）；随抬手进度渐进
		float push = Mth.lerp(progress, 0.0F, -1.1F);
		float lift = Mth.lerp(progress, 0.0F, 0.45F);
		matrices.translate(0.0F, lift, push);
		matrices.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, 0.0F, 25.0F)));
		boolean right = (hand == net.minecraft.world.InteractionHand.MAIN_HAND)
				== (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
		float sway = CastingVisualState.armSway(player.tickCount + tickDelta, right);
		matrices.translate(0, 0.018F * sway * progress, 0.10F * sway * progress);
		matrices.mulPose(Axis.XP.rotationDegrees(sway * progress * 4.0F));
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
	private void ssca$restoreFirstPersonHands(net.minecraft.client.player.AbstractClientPlayer player,
			float tickDelta, float pitch, net.minecraft.world.InteractionHand hand, float swingProgress,
			net.minecraft.world.item.ItemStack item, float equipProgress, PoseStack matrices,
			net.minecraft.client.renderer.MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
		matrices.popPose();
	}
}
