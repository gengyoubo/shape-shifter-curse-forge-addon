package github.com.gengyoubo.sscfa.neomagic;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** 崩解度三档效果（ssca_neomagic:disorder / disintegration / breakdown），移植自 Fabric 版 NeoMagic。 */
public class CollapseEffect extends MobEffect {
	public CollapseEffect(int color, double slow) {
		super(MobEffectCategory.HARMFUL, color);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, "2a36537d-c648-48b2-9bd1-6d420f2fdf7b", -slow,
				AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	/** 崩溃：每秒 2+等级 点魔法伤害。 */
	public static class Breakdown extends CollapseEffect {
		public Breakdown(int color, double slow) {
			super(color, slow);
		}

		@Override
		public boolean isDurationEffectTick(int duration, int amplifier) {
			return duration % 20 == 0;
		}

		@Override
		public void applyEffectTick(LivingEntity entity, int amplifier) {
			entity.hurt(entity.damageSources().magic(), 2 + amplifier);
		}
	}
}
