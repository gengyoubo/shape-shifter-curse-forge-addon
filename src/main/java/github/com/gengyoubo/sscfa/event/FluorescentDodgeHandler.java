package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import java.util.Random;

/**
 * 荧光幼灵被动③——黏液保护膜闪避（由 FluorescentDodgeMixin 迁移到官?{@link ServerLivingEntityEvents#ALLOW_DAMAGE}）?
 * <p>荧光幼灵形态受到【物理攻击】伤害时 20% 概率完全闪避（取消伤?+ 水花粒子 + 音效）?
 * 仅物理类伤害（近?弹射?荆棘/仙人掌等）生效；魔法/火焰/岩浆/窒息/饥饿/虚空等不闪避?
 * <p>{@code ALLOW_DAMAGE} 仅服务端触发、返?false 即取消伤害，语义等价于原 mixin ?damage 返回 false?
 * 且天然满足原 mixin 的「仅服务端」判定?
 */
public final class FluorescentDodgeHandler {

	private static final float DODGE_CHANCE = 0.20f;
	private static final Random DODGE_RNG = new Random();

	private FluorescentDodgeHandler() {
	}

	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((entity, source, amount) -> {
			// 仅玩家、荧光幼灵形态、物理类伤害
			if (!(entity instanceof Player self)) {
				return true;
			}
			if (!FormUtils.isAxolotlFluorescent(self)) {
				return true;
			}
			if (!isPhysicalDamage(source)) {
				return true;
			}
			if (DODGE_RNG.nextFloat() < DODGE_CHANCE) {
				// 闪避成功：取消伤?+ 水花反馈（owner 打标：仅本人第一人称避让?
				if (self.level() instanceof ServerLevel sw) {
					github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, self, ParticleTypes.SPLASH, self.getX(), self.getY() + 1.0, self.getZ(), 16, 0.4, 0.6, 0.4, 0.3);
					github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, self, ParticleTypes.BUBBLE, self.getX(), self.getY() + 1.0, self.getZ(), 8, 0.4, 0.6, 0.4, 0.1);
					sw.playSound(null, self.getX(), self.getY() + 1.0, self.getZ(),
							SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.6f, 1.5f);
				}
				return false;
			}
			return true;
		});
	}

	/** 判定是否为物理类伤害（可被闪避）?*/
	private static boolean isPhysicalDamage(DamageSource source) {
		String id = source.type().msgId();
		// 物理攻击/弹射?荆棘/仙人?飞镖等；排除魔法/火焰/岩浆/药水/窒息/饥饿/虚空/溺水/干渴
		return "mob".equals(id) || "player".equals(id) || "sting".equals(id)
				|| "arrow".equals(id) || "trident".equals(id) || "fireball".equals(id)
				|| "thrown".equals(id) || "thorns".equals(id) || "cactus".equals(id)
				|| "sweetBerryBush".equals(id) || "stalagmite".equals(id) || "fallingStalactite".equals(id)
				|| (source.getDirectEntity() != null && source.getDirectEntity().getType().is(EntityTypeTags.ARROWS));
	}
}
