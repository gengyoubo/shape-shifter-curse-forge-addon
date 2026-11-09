package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.ParticleUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 閲戞矙宀歋P - 寮曠垎鏍囪锛堜富鍔ㄢ憽娆¤鎶€鑳斤級
 *
 * 寮曠垎鎵€鏈夐潪缁胯壊鐘舵€佺殑渚佃殌鐑欏嵃鏍囪锛?
 * - 閫犳垚鐩爣褰撳墠鐢熷懡鍊?0%鐨勪激瀹筹紙涓婇檺20锛屾灟娌欐寚鐜彁鍗囧埌26锛?
 * - 铓€娌欐１鏅朵細璁╀富鐩爣鎵垮彈60%浼ゅ锛?0%浼ゅ鎵╂暎鍒板懆鍥?鏍?
 * - 鑷韩鎸夊凡鎹熺敓鍛藉€肩殑20%鍥炲涓€娆?
 * - 3灞傜洰鏍囧厛瑙﹀彂琚姩鐖嗗彂鍐嶅紩鐖?
 * - 寮曠垎鍚庣洰鏍囪繘鍏ョ豢鑹茬姸鎬?0绉掞紝鍏朵腑鍓?绉掍笉鍙啀鍙犲眰
 *
 * CD: 10绉掞紙200tick锛?
 */
public class GoldenSandstormDetonate {

	// CD 涓庢妧鑳?ID 宸茬敱 power 灞傦紙form_golden_sandstorm_sp_erosion_brand锛? 缁熶竴鍐峰嵈鏈嶅姟绠＄悊锛屽師甯搁噺宸插垹

	private GoldenSandstormDetonate() {
	}

	/**
	 * 鐜╁鎸変笅娆¤鎶€鑳介敭瑙﹀彂
	 */
	public static boolean execute(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel serverWorld)) return false;

		// 璋冪敤 ErosionBrand 鐨勫紩鐖嗘柟娉曪紙CD 闂ㄧ宸茬敱 power 灞?begin 缁熶竴鎷︽埅锛屼慨 P1-1锛?
		int[] result = GoldenSandstormErosionBrand.detonateAll(player);
		int targets = result[0];
		int totalStacks = result[1];

		if (targets <= 0) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}

		// 閲婃斁闊虫晥
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);

		// 鑷韩鍛ㄥ洿绮掑瓙鎻愮ず
		ParticleUtils.spawnDecorationParticles(serverWorld, player, ParticleTypes.SOUL_FIRE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				20 * totalStacks, 1.0, 1.0, 1.0, 0.1);

		return true;
	}
}
