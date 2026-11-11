package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.power.TrueInvisibilityAbilityPower;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;

public class PreInvisibilityEffect extends MobEffect {
	public PreInvisibilityEffect() {
		super(MobEffectCategory.BENEFICIAL, 0x101010); // Dark color
		this.addAttributeModifier(
				Attributes.MOVEMENT_SPEED,
				"12db6328-9844-4e20-9118-202758169972",
				-0.5,
				AttributeModifier.Operation.MULTIPLY_TOTAL
		);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration == 1;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!entity.level().isClientSide()) {
			// 未放出就被净????死亡属于施放失败，不能先转???ACTIVE??
			if (entity.isDeadOrDying() || entity.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) return;
			ServerLevel serverWorld = (ServerLevel) entity.level();

			// 1. Spawn Black Particles
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SQUID_INK,
					entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
					20, 0.5, 0.5, 0.5, 0.1);

			// 2. Play Extinguish Sound
			serverWorld.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);

			// 3. Find Power to get duration
			int duration = TrueInvisibilityAbilityPower.getEffectDuration(entity, 100); // 没有该能力时默认 5 秒

			// 4. Apply True Invisibility
			entity.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.TRUE_INVISIBILITY.get(), duration, 0, false, false, true));
			// 同步叠加原版隐身，确保服务器把“不可见”状态同步给其他客户端???
			entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
			if (entity.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.TRUE_INVISIBILITY.get())) {
				TrueInvisibilityAbilityPower.onInvisibilityReleased(entity);
			}

			// 5. Notify Player
			// 通知逻辑已移除，保留注释占????
		}
	}
}
