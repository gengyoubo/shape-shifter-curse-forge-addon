package github.com.gengyoubo.sscfa.effect;

import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StunEffect extends MobEffect {
	// 固??UUID：暴露为常量，供 SscAddon 的「孤儿修正兜底清理」??UUID 精确移除残留??
	public static final UUID SPEED_MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
	public static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("22653B89-116E-49DC-9B6B-9971489B5C0A");

	public StunEffect() {
		super(MobEffectCategory.HARMFUL, 0x888888);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
				SPEED_MODIFIER_UUID.toString(), -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
				ATTACK_MODIFIER_UUID.toString(), -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// Removed explicit velocity reset to allow gravity/knockback to work
		// entity.setVelocity(0, entity.getVelocity().y, 0);
		// entity.velocityModified = true;
	}
}
