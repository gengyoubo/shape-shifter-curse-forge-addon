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
 * 霜降效???- SP雪狐远程技能造成的debuff
 * 效果?
 * - 移动速???-30%
 * - 持续时间????
 */
public class FrostFallEffect extends MobEffect {

	private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("f2a3b4c5-d6e7-4890-bcde-f01234567891");
	private static final String SPEED_MODIFIER_NAME = "Frost Fall Speed Debuff";

	public FrostFallEffect() {
		super(MobEffectCategory.HARMFUL, 0xADD8E6); // Light blue color
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);

		// Apply movement speed reduction (-30%)
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(SPEED_MODIFIER_UUID);
			speedAttr.addTransientModifier(new AttributeModifier(
					SPEED_MODIFIER_UUID,
					SPEED_MODIFIER_NAME,
					-0.3,
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
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// Apply update effect every 5 ticks for particles
		return duration % 5 == 0;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// Spawn snowflake particles
		if (entity.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld,
					ParticleTypes.SNOWFLAKE,
					entity.getX(),
					entity.getY() + entity.getBbHeight() / 2.0,
					entity.getZ(),
					2,
					entity.getBbWidth() / 2.0,
					entity.getBbHeight() / 4.0,
					entity.getBbWidth() / 2.0,
					0.02
			);
		}
	}
}
