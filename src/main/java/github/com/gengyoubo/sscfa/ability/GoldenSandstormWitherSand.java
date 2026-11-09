package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.GoldenSandstormPerks;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 闁叉垶鐭欏畝姝婸 - 閸戝娴傞柌鎴炵煓閿涘牅瀵岄崝銊㈡喖閿涘矁鎼崝娑樼€烽敍?
 *
 * 閹稿绗呴幎鈧懗浠嬫暛瀵偓婵鎼崝娑崇礄1缁夋帪绱氶敍宀冨殰闊偄鍣洪柅?0%閵?
 * 閽冨嫬濮忕€瑰本鍨氶崥搴ㄥ櫞閺€?5閺嶇硛oE闁叉垶鐭欐搴㈡瘹閿?
 * - 鐎电瀵栭崶鏉戝敶闂堢偟娅ч崥宥呭礋閻㈢喓澧块弬钘夊閼峰娲搁弫鍫熺亯閿?缁夋帪绱?
 * - 缂佹瑨瀵栭崶鏉戝敶閻╊喗鐖ｉ崥鍕綌閸?鐏炲倷闀滈摀鈧悜娆忓祪
 * - 婢堆囧櫤濞屾瑥鐨圭划鎺戠摍閺佸牊鐏?
 *
 * 閽冨嫬濮忛張鐔兼？閸欐鍩屾导銈咁唺閸掓瑦澧﹂弬顓ㄧ窗閸欐牗绉烽幎鈧懗鏂ょ礉鏉╂稑鍙?缁夋墫D閵?
 * 濮濓絽鐖堕柌濠冩杹CD: 26缁夋帪绱?20tick閿?
 */
public class GoldenSandstormWitherSand {

	// ==================== 鐢悂鍣?====================
	// 娴犮儰绗呯敮鎼佸櫤閸у洣璐熸妯款吇閸婄》绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閿涘澃cope閿涙瓫bilities.golden_sandstorm_wither_sand閿?
	/** AoE閸楀﹤绶?*/
	private static final double RADIUS = 15.0;
	/** 閼峰娲搁幐浣虹敾閺冨爼妫块敍鍧眎ck閿?*/
	private static final int BLIND_DURATION = 60; // 3缁?
	/** 閽冨嫬濮忛弮鍫曟？閿涘澅ick閿?*/
	private static final int CHARGE_TICKS = 20; // 1缁?
	// 濮濓絽鐖?閹垫挻鏌?CD 瀹歌尙鏁辩紒鐔剁閸愬嘲宓堥張宥呭 SkillCooldowns + power JSON 闁板秶鐤嗙粻锛勬倞閿涘苯甯敮鎼佸櫤瀹告彃鍨?
	/** 閽冨嫬濮忛崙蹇涒偓鐔舵叏濮濓絽娅扷UID */
	private static final UUID CHARGE_SLOW_UUID = UUID.fromString("b8c9d0e1-f2a3-4b5c-8d6e-7f8901234567");
	private static final String CHARGE_SLOW_NAME = "Wither Sand Charge Slow";

	// ==================== 閻樿埖鈧浇鎷烽煪?====================
	/** 濮濓絽婀拑鍕閻ㄥ嫮甯虹€硅绱癠UID -> 閽冨嫬濮忛悩鑸碘偓?*/
	private static final ConcurrentHashMap<UUID, ChargeState> CHARGING_PLAYERS = new ConcurrentHashMap<>();

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("abilities.golden_sandstorm_wither_sand");
	/** 缂佺喍绔撮崘宄板祱閺堝秴濮熼惃鍕旂€规碍濡ч懗?ID閿? power 濞夈劌鍞界捄顖氱窞閿涘鈧?*/
	private static final String SKILL_ID = "my_addon:form_golden_sandstorm_sp_wither_sand";

	private GoldenSandstormWitherSand() {
	}

	/**
	 * 閻溾晛顔嶉幐澶夌瑓閹垛偓閼充粙鏁?- 瀵偓婵鎼崝?
	 */
	public static boolean execute(ServerPlayer player) {
		if (!FormUtils.isGoldenSandstormSP(player)) return false;
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return false;
		// 瀹歌尙绮￠崷銊ㄦ惈閸旀稐鑵戦崚娆忔嫹閻ｃ儻绱機D 闂傘劎顩﹀鑼暠 power 鐏?begin 缂佺喍绔撮幏锔藉焻閿涘奔鎱?P1-1閿?
		if (CHARGING_PLAYERS.containsKey(player.getUUID())) return false;

		if (!(player.level() instanceof ServerLevel serverWorld)) return false;

        var spec = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.spec(player, SKILL_ID);
        if (spec == null || !github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
		// 瀵偓婵鎼崝?
		ChargeState state = new ChargeState();
		state.startTick = serverWorld.getGameTime();
		state.healthAtStart = player.getHealth();
		CHARGING_PLAYERS.put(player.getUUID(), state);
        state.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		state.brandStacks = GoldenSandstormPerks.sandstormStacks(player);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, SKILL_ID, state.castId,
				GoldenSandstormPerks.normalCooldown(spec.cooldown(), GoldenSandstormPerks.responseRank(player)));

		// 閺傝棄濮為崙蹇涒偓?0%
		applyChargeSlow(player);

		// 閽冨嫬濮忓鈧慨瀣叾閺?
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);

		return true;
	}

	/**
	 * 濮ｅ紨ick閺囧瓨鏌婇拑鍕閻樿埖鈧?
	 * 闂団偓鐟曚礁婀?SscAddon 閻?tick handler 娑擃叀鐨熼悽?
	 */
	public static void tick(ServerPlayer player) {
		ChargeState state = CHARGING_PLAYERS.get(player.getUUID());
		if (state == null) return;

		if (!(player.level() instanceof ServerLevel serverWorld)) return;

		// 瑜般垺鈧焦顥呴弻?
		if (!FormUtils.isGoldenSandstormSP(player)) {
			cancelCharge(player, false);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, state.castId);
			return;
		}

		long currentTick = serverWorld.getGameTime();
		long elapsed = currentTick - state.startTick;

		// 濡偓閺屻儲妲搁崥锕侇潶閹垫挻鏌囬敍鍫㈡晸閸涜棄鈧棿绗呴梽?= 閸欐鍩屾导銈咁唺閿?
		if (player.getHealth() < state.healthAtStart) {
			// 鐞氼偅澧﹂弬顓ㄧ窗閹?fail_cooldown 缂佹挾鐣婚敍鍫㈢埠娑撯偓閺堝秴濮熼敍? 閹垫挻鏌囬棅铏櫏
			cancelCharge(player, true);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(player, SKILL_ID, state.castId);

			serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SAND_FALL, SoundSource.PLAYERS, 1.0f, 0.3f);
			return;
		}

		// 閽冨嫬濮忛張鐔兼？缁帒鐡欓敍鍫ｅ殰闊偄鎳嗛崶瀛樻鏉烆剚鐭欑亸姗堢礆
		if (elapsed % 4 == 0) {
			double angle = (elapsed * 0.3) % (Math.PI * 2);
			double px = player.getX() + Math.cos(angle) * 1.5;
			double pz = player.getZ() + Math.sin(angle) * 1.5;
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, player,
					new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
					px, player.getY() + 1.0, pz, 3, 0.1, 0.3, 0.1, 0);
		}

		// 閽冨嫬濮忕€瑰本鍨?
		if (elapsed >= BAL.i("charge_ticks", CHARGE_TICKS)) {
			removeChargeSlow(player);
			CHARGING_PLAYERS.remove(player.getUUID());
			releaseSkill(player, serverWorld, state.castId, state.brandStacks);
		}
	}

	/**
	 * 闁插﹥鏂侀幎鈧懗?- 15閺嶇硛oE
	 */
	private static void releaseSkill(ServerPlayer player, ServerLevel serverWorld, long castId, int brandStacks) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, castId);
		double radius = BAL.d("radius", RADIUS);

		// 闁插﹥鏂侀棅铏櫏
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);

		// 婢堆囧櫤缁帒鐡欓弫鍫熺亯閿涙岸鍣鹃懝鍙夌煓鐏忔﹢顥撻弳?
		for (int i = 0; i < 100; i++) {
			double angle = Math.random() * Math.PI * 2;
			double dist = Math.random() * radius;
			double px = player.getX() + Math.cos(angle) * dist;
			double pz = player.getZ() + Math.sin(angle) * dist;
			double py = player.getY() + Math.random() * 2.5;
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, player,
					new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
					px, py, pz, 1, 0, 0, 0, 0);
		}
		ParticleUtils.spawnDecorationParticles(serverWorld, player, ParticleTypes.SOUL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, radius * 0.5, 1.0, radius * 0.5, 0.02);

		// 閼惧嘲褰囬懠鍐ㄦ纯閸愬懐鏁撻悧?
		AABB box = player.getBoundingBox().inflate(radius);
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius);

		for (LivingEntity target : targets) {
			// 閻ц棄鎮曢崡鏇燁梾閺?
			if (WhitelistUtils.isProtected(player, target)) continue;

			// 閺傝棄濮為懛瀵告锤閺佸牊鐏夐敍?缁夋帪绱氶敍娑樼敨閺傝姤纭堕懓?source 娓氭稑鍙嗗锔藉閹搭亜缍婇崶?
			target.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.SAND_BLIND.get(), BAL.i("blind_duration", BLIND_DURATION), 0, false, true), player);

			// 閸欑姴濮?鐏炲倷闀滈摀鈧悜娆忓祪
			GoldenSandstormErosionBrand.onSandstormHit(player, target, brandStacks);

			// 閸涙垝鑵戠划鎺戠摍
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					5, 0.3, 0.3, 0.3, 0.02);
		}
	}

	/**
	 * 閸欐牗绉烽拑鍕
	 * @param interrupted 閺勵垰鎯佺悮顐ｅⅵ閺傤叏绱欓崣妤€鍩屾导銈咁唺閿?
	 */
	private static void cancelCharge(ServerPlayer player, boolean interrupted) {
		removeChargeSlow(player);
		CHARGING_PLAYERS.remove(player.getUUID());
	}

	/**
	 * 閺傝棄濮為拑鍕閸戝繘鈧?
	 */
	private static void applyChargeSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(CHARGE_SLOW_UUID);
			speedAttr.addTransientModifier(new AttributeModifier(
					CHARGE_SLOW_UUID, CHARGE_SLOW_NAME, -0.50,
					AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	/**
	 * 缁夊娅庨拑鍕閸戝繘鈧?
	 */
	private static void removeChargeSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(CHARGE_SLOW_UUID);
		}
	}

	/**
	 * 濡偓閺屻儳甯虹€硅埖妲搁崥锔筋劀閸︺劏鎼崝娑崇礄閸欘垳鏁ゆ禍搴ㄦ▎濮濄垹鍙炬禒鏍ㄦ惙娴ｆ粣绱?
	 */
	public static boolean isCharging(UUID playerUuid) {
		return CHARGING_PLAYERS.containsKey(playerUuid);
	}

	/**
	 * 閻溾晛顔嶉弬顓犲殠/閸欐ê鑸伴弮鑸电閻?
	 */
	public static void clearPlayer(ServerPlayer player) {
        ChargeState state = CHARGING_PLAYERS.remove(player.getUUID());
        if (state != null) {
            removeChargeSlow(player);
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, state.castId);
        }
	}

	/**
	 * 閺堝秴濮熼崳銊╁櫢閸?閻戭參鍣告潪鑺ユ濞撳懐鎮婇幍鈧張澶屽Ц閹?
	 * 娴肩姴鍙?server 娴犮儰绌堕柅鎰嚋缁夊娅庨崷銊у殠閻溾晛顔嶉煬顐＄瑐閻ㄥ嫯鎼崝娑樺櫤闁喍鎱ㄦ鎵儊閿涘矂浼╅崗?reload 閸氬海甯虹€规儼顫﹀闀愮畽閸戝繘鈧喆鈧?
	 * 閸忎浇顔?server 娑?null閿涘牓娼ら幀浣稿灙鐞涖劋绮庡〒鍛敄閿涘鈧?
	 */
	public static void clearAll(net.minecraft.server.MinecraftServer server) {
		// SERVER_STARTING 闂冭埖顔?getPlayerManager() 閸欘垵鍏樼亸姘弓閸掓繂顫愰崠鏍电礉韫囧懘銆忛崑姘扁敄閸婂吋顥呴弻?
		if (server != null) {
			net.minecraft.server.players.PlayerList pm = server.getPlayerList();
			if (pm != null) {
				for (ServerPlayer player : pm.getPlayers()) {
					if (CHARGING_PLAYERS.containsKey(player.getUUID())) {
                        clearPlayer(player);
					}
				}
			}
		}
		CHARGING_PLAYERS.clear();
	}

	// ==================== 閸愬懘鍎撮弫鐗堝祦缁?====================
	private static class ChargeState {
        long castId = -1;
		int brandStacks = 1;
		long startTick;
		float healthAtStart;
	}
}
