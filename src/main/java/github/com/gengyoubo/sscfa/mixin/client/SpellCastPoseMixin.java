package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.CastingVisualState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 施法举手动画（jackcooper）：施法时双手前举（铁魔法式）。
 * <p>
 * 注入 {@code BipedEntityModel.setAngles} TAIL——原版 positionArms/animateArms/sneaking 分支
 * 全部执行完毕后再覆盖双臂 pitch，原版姿势与本 mixin 的关系是「后者最终生效」。
 * <p>
 * <b>两条渲染链路（按 bodyType + 是否有 GeckoLib 兽形渲染分流）：</b>
 * <ul>
 * <li><b>纯人形态（无兽形模型，如原版皮肤人形态）</b>：直接覆盖 biped 手臂角度（本 mixin 生效）；</li>
 * <li><b>带 GeckoLib 兽形模型的人形形态（如美西螈，bodyType=NORMAL 但有兽形渲染）</b>：biped 手臂
 * 被隐藏且角度被 GeckoLib 动画覆盖，本 mixin 无效——由配套的
 * {@code SpellCastGeoPoseMixin}（DefaultModelAnimationSystem.processAnimation TAIL）直接覆盖
 * GeoBone 旋转（与 {@code UpgradeAxolotlSpearChargeArmMixin} 同链路同先例）；</li>
 * <li><b>FERAL 兽形</b>：不举手（用户要求）。</li>
 * </ul>
 * <p>
 * 多人一致：数据源为服务端广播的 {@code spell_cast_visual}，所有观察者客户端同表同动画。
 */
@Mixin(HumanoidModel.class)
public abstract class SpellCastPoseMixin {

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
	private void sscAddon$raiseArmsWhileCasting(LivingEntity entity, float limbAngle, float limbDistance,
			float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
		if (entity == null || entity.level() == null || !entity.level().isClientSide) return;
		if (!(entity instanceof net.minecraft.world.entity.player.Player)) return; // 人形怪物无形态组件
		if (!CastingVisualState.isCasting(entity.getUUID())) return;
		// 仅人形态（FERAL 兽形有自己的动画系统，biped 手臂角度会被复制进兽形骨骼，不能覆盖）
		FormDefinition form = github.com.gengyoubo.sscfa.util.FormUtils.getCurrentForm(entity);
		if (form == null || form.bodyType() != FormBodyType.NORMAL) return;
		HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;
		float progress = CastingVisualState.raiseProgress(entity.getUUID());
		if (progress <= 0.0F) return;
		// 铁魔法式：双臂向前上方举起（pitch 负值 = 向前抬）。从当前角度渐进插值到目标角。
		float targetPitch = -2.6F; // ≈149°，双臂高举过头偏前
		self.rightArm.xRot = net.minecraft.util.Mth.lerp(progress, self.rightArm.xRot,
				targetPitch + CastingVisualState.armSway(animationProgress, true) * 0.12F);
		self.rightArm.yRot = net.minecraft.util.Mth.lerp(progress, self.rightArm.yRot, 0.0F);
		self.leftArm.xRot = net.minecraft.util.Mth.lerp(progress, self.leftArm.xRot,
				targetPitch + CastingVisualState.armSway(animationProgress, false) * 0.12F);
		self.leftArm.yRot = net.minecraft.util.Mth.lerp(progress, self.leftArm.yRot, 0.0F);
	}
}
