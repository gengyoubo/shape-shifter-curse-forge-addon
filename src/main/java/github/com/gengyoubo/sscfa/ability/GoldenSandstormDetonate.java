package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.ParticleUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 金沙岚SP - 引爆标记（主动②次要技能）
 *
 * 引爆所有非绿色状态的侵蚀烙印标记?
 * - 造成目标当前生命?0%的伤害（上限20，枯沙指环提升到26?
 * - 蚀沙棱晶会让主目标承受60%伤害?0%伤害扩散到周??
 * - 自身按已损生命值的20%回复一?
 * - 3层目标先触发被动爆发再引?
 * - 引爆后目标进入绿色状?0秒，其中?秒不可再叠层
 *
 * CD: 10绉掞紙200tick锛?
 */
public class GoldenSandstormDetonate {

	// CD 与技?ID 已由 power 层（form_golden_sandstorm_sp_erosion_brand? 统一冷却服务管理，原常量已删

	private GoldenSandstormDetonate() {
	}

	/**
	 * 玩家按下次要技能键触发
	 */
	public static boolean execute(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel serverWorld)) return false;

		// 调用 ErosionBrand 的引爆方法（CD 门禁已由 power ?begin 统一拦截，修 P1-1?
		int[] result = GoldenSandstormErosionBrand.detonateAll(player);
		int targets = result[0];
		int totalStacks = result[1];

		if (targets <= 0) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}

		// 释放音效
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);

		// 自身周围粒子提示
		ParticleUtils.spawnDecorationParticles(serverWorld, player, ParticleTypes.SOUL_FIRE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				20 * totalStacks, 1.0, 1.0, 1.0, 0.1);

		return true;
	}
}
