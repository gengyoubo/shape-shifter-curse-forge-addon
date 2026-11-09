package github.com.gengyoubo.sscfa.effect;

import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/**
 * 致盲效果 - 金沙岚SP的凋零金沙技能施加的debuff
 * 效果：
 * - 视野限制（通过同时施加原版失明效果实现）
 * - 移动速度 -20%
 * - 非玩家生物的警戒和攻击距离减少至5格（通过修改FOLLOW_RANGE属性实现）
 * - 头部周围生成沙尘和黑色粒子
 * 持续时间：3秒
 */
public class SandBlindEffect extends MobEffect {

	private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a7b8c9d0-e1f2-4a3b-8c5d-6e7f89012345");
	private static final String SPEED_MODIFIER_NAME = "Sand Blind Speed Debuff";

	private static final UUID FOLLOW_RANGE_MODIFIER_UUID = UUID.fromString("a7b8c9d0-e1f2-4a3b-8c5d-6e7f89012346");
	private static final String FOLLOW_RANGE_MODIFIER_NAME = "Sand Blind Follow Range Reduction";

	public SandBlindEffect() {
		super(MobEffectCategory.HARMFUL, 0xD4A017); // 金沙色
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);

		// 施加原版失明效果（视野限制）
		MobEffectInstance currentSandBlind = entity.getEffect(this);
		int blindDuration = currentSandBlind != null ? currentSandBlind.getDuration() : 60;
		entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindDuration, 0, false, false, false));

		// 移动速度 -20%
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(SPEED_MODIFIER_UUID);
			speedAttr.addTransientModifier(new AttributeModifier(
					SPEED_MODIFIER_UUID,
					SPEED_MODIFIER_NAME,
					-0.20,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}

		// 非玩家生物：将警戒/攻击距离减少至5格
		if (entity instanceof Mob mob) {
			AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
			if (followRange != null) {
				followRange.removeModifier(FOLLOW_RANGE_MODIFIER_UUID);
				// 计算需要减少的量，使最终值为5
				double currentBase = followRange.getValue();
				double reduction = -(currentBase - 5.0);
				if (reduction < 0) {
					followRange.addTransientModifier(new AttributeModifier(
							FOLLOW_RANGE_MODIFIER_UUID,
							FOLLOW_RANGE_MODIFIER_NAME,
							reduction,
							AttributeModifier.Operation.ADDITION
					));
				}
			}
		}
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);

		// 移除原版失明效果
		entity.removeEffect(MobEffects.BLINDNESS);

		// 移除速度修正
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(SPEED_MODIFIER_UUID);
		}

		// 移除跟踪距离修正
		if (entity instanceof Mob mob) {
			AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
			if (followRange != null) {
				followRange.removeModifier(FOLLOW_RANGE_MODIFIER_UUID);
			}
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// 每4tick生成一次头部粒子
		return duration % 4 == 0;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel serverWorld)) return;

		double headX = entity.getX();
		double headY = entity.getEyeY() + 0.3;
		double headZ = entity.getZ();

		// 沙尘落粒子 - 广播给所有附近玩家（含受影响实体本身）
		serverWorld.sendParticles(
				new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
				headX, headY, headZ,
				5, 0.45, 0.25, 0.45, 0);

		// 黑色烟雾粒子 - 广播给所有附近玩家
		serverWorld.sendParticles(
				ParticleTypes.SMOKE,
				headX, headY, headZ,
				3, 0.4, 0.2, 0.4, 0.008);
	}
}
