package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

// 失聪：客??SoundManagerDeafenMixin 据此静音受影响玩家自身的所有声?
public class DeafenEffect extends MobEffect {
	public DeafenEffect() {
		super(MobEffectCategory.HARMFUL, 0x6B5B95);
	}
}
