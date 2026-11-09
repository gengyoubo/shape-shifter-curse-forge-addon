package github.com.gengyoubo.sscfa.effect;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 霜凝效???- SP雪狐近战技能造成的debuff
 * 效果?
 * - 移动速???-35%
 * - 攻击速??-40%
 * - 受到物???魔法伤???+35% (通过Mixin实??
 * - 持续时间????
 */
public class FrostFreezeEffect extends MobEffect {

	private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("f1a2b3c4-d5e6-4789-abcd-ef0123456789");
	private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("f1a2b3c4-d5e6-4789-abcd-ef0123456790");

	private static final String SPEED_MODIFIER_NAME = "Frost Freeze Speed Debuff";
	private static final String ATTACK_SPEED_MODIFIER_NAME = "Frost Freeze Attack Speed Debuff";

	public FrostFreezeEffect() {
		super(MobEffectCategory.HARMFUL, 0x7DD3FC); // Light ice blue color
	}

	/**
	 * 检查伤害类型是否为物理或魔法伤??
	 * 用于判断是否应用+20%伤害增???
	 */
	public static boolean isPhysicalOrMagicDamage(net.minecraft.world.damagesource.DamageSource source) {
		// 物理伤害类??
		if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.ARROW) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.TRIDENT) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.THROWN)) {
			return true;
		}

		// 魔法伤害类??
		if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) ||
				source.is(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM)) {
			return true;
		}

		// 通用伤???通常用于mod自定义伤??
		return source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);

		// Apply movement speed reduction (-35%)
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(SPEED_MODIFIER_UUID);
			speedAttr.addTransientModifier(new AttributeModifier(
					SPEED_MODIFIER_UUID,
					SPEED_MODIFIER_NAME,
					-0.35,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}

		// Apply attack speed reduction (-40%)
		AttributeInstance attackSpeedAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
		if (attackSpeedAttr != null) {
			attackSpeedAttr.removeModifier(ATTACK_SPEED_MODIFIER_UUID);
			attackSpeedAttr.addTransientModifier(new AttributeModifier(
					ATTACK_SPEED_MODIFIER_UUID,
					ATTACK_SPEED_MODIFIER_NAME,
					-0.4,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);

		// Remove movement speed modifier
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(SPEED_MODIFIER_UUID);
		}

		// Remove attack speed modifier
		AttributeInstance attackSpeedAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
		if (attackSpeedAttr != null) {
			attackSpeedAttr.removeModifier(ATTACK_SPEED_MODIFIER_UUID);
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// Apply update effect every 4 ticks for particles
		return duration % 4 == 0;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// Spawn frost particles
		if (entity.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld,
					ParticleTypes.SNOWFLAKE,
					entity.getX(),
					entity.getY() + entity.getBbHeight() / 2.0,
					entity.getZ(),
					3,
					entity.getBbWidth() / 2.0,
					entity.getBbHeight() / 4.0,
					entity.getBbWidth() / 2.0,
					0.01
			);
		}
	}
}
