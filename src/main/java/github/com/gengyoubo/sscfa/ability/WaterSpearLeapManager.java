package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.UpgradeAxolotlPerks;
import github.com.gengyoubo.sscfa.entity.ShellProjectile;
import github.com.gengyoubo.sscfa.evolution.AxolotlTree;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 杩涘寲缇庤タ铻堛€屾姇鎺锋按鐭涖€嶄富鎶€鑳斤紙sp_primary锛夆€斺€?鏈嶅姟绔姸鎬佹満銆?
 *
 * <p>鎸変富鎶€鑳介敭锛?b>涓嶅線鑳屽寘鏀句换浣曠墿鍝?/b>锛屼粎鍚屾銆岃搫鍔涗腑銆嶇姸鎬佺粰瀹㈡埛绔?鈫?
 * 鐢辨覆鏌?mixin 绾覆鏌撳湪鎵嬩笂鐢诲嚭 3D 姘寸煕妯″瀷 + 涓剧煕杩囪偐濮垮娍锛坽@code getArmPose}/绗竴浜虹О鐭╅樀 mixin锛?
 * 闈?{@code CustomModelData=1} 鎭掑畾 3D锛屼笉渚濊禆 vanilla銆屾鍦ㄤ娇鐢ㄧ墿鍝併€嶇姸鎬併€佷笉鍔ㄧ帺瀹惰儗鍖咃級銆?
 * 鍚屾椂鍐查噺鏂滃悗涓婅穬骞?b>鏃犻噸鍔涙偓娴?/b>锛涜搫鍔?1.35 绉掑悗鏈濆噯鏄熸姇鍑虹洿绾挎按鐭涖€?
 * 鐩村嚮 12 鐗╃悊 + 2 鏍煎崐寰?5 鐐?AOE锛岃€?6% 婀挎鼎搴︼紙18 air锛夛紝CD 8 绉掋€?/p>
 *
 * <p>闆惰儗鍖呮薄鏌擄細鍏ㄧ▼涓嶇敓鎴?鏇挎崲浠讳綍鐗╁搧锛屾姇鍑?/ 鍙栨秷 / 姝讳骸 / 褰㈡€佷涪澶?/ 鏂嚎 鍙渶鎭㈠閲嶅姏 + 缁撴潫娓叉煋銆?/p>
 */
public final class WaterSpearLeapManager {

	// 浠ヤ笅鍧囦负榛樿鍊硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.water_spear_leap锛?
	private static final int CHARGE_TICKS = 27;    // 钃勫姏 ~1.35 绉掑悗鎶曠煕
	// CD锛? 绉掞級鐢辩粺涓€鍐峰嵈鏈嶅姟 + power JSON 閰嶇疆绠＄悊锛屽師 CD_TICKS 甯搁噺宸插垹
	private static final int AIR_COST = 18;        // 6% 婀挎鼎搴?
	private static final double LEAP_BACK = 0.80;  // 璧疯穬鍚戝悗鍐查噺锛堟洿鏂滃悗锛?
	private static final double LEAP_UP = 0.62;    // 璧疯穬鍚戜笂鍐查噺锛堣烦鏇撮珮锛?
	private static final double DECAY = 0.80;       // 姣?tick 閫熷害琛板噺锛堢紦鍏ョ紦鍑?+ 鏀舵潫鍒版偓娴級

	/** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.water_spear_leap");
	/** 缁熶竴鍐峰嵈鏈嶅姟鐨勭ǔ瀹氭妧鑳?ID锛堢綉缁滃寘瑙﹀彂鍨嬶級銆?*/
	private static final String SKILL_ID = "my_addon:form_upgrade_axolotl_water_spear";

	/** HUD 闂ㄦ鍚屾簮锛氭按鐭涚┖姘旀秷鑰楋紙balance 鍙皟锛汬UD 灞曠ず鐢級銆?*/
	public static int airCostForHud() { return BAL.i("air_cost", AIR_COST); }
	public static int airCostForHud(Player player) { return UpgradeAxolotlPerks.spearAirCost(player, airCostForHud()); }

	private static final Map<UUID, LeapState> STATES = new ConcurrentHashMap<>();

	private static final class LeapState {
        long castId = -1;
		int tick = 0;
	}

	private WaterSpearLeapManager() {
	}

	/** 瀹㈡埛绔寜涓绘妧鑳介敭锛堟棤 payload锛夈€傛湇鍔＄鏍￠獙銆佹崲姘寸煕骞惰捣璺冩偓娴€?*/
	public static void onKeyPress(ServerPlayer player) {
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return;
		if (STATES.containsKey(player.getUUID())) return; // 鏂芥硶涓笉鍙噸鍏?
		if (!FormUtils.isUpgradeAxolotl(player)) return;
		if (!RegEvolutionComponent.EVOLUTION.get(player).isUnlocked(AxolotlTree.NODE_WATER_SPEAR)) return;
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return; // CD 涓?
		int airCost = airCostForHud(player);
		if (player.getAirSupply() < airCost) return; // 婀挎鼎搴︿笉瓒?
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return;

		player.setAirSupply(player.getAirSupply() - airCost);

		LeapState s = new LeapState();
        s.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		STATES.put(player.getUUID(), s);

		// 鍚屾銆岃搫鍔涗腑銆嶁啋 瀹㈡埛绔函娓叉煋锛氭墜涓婃覆鏌?3D 姘寸煕妯″瀷锛堜笉寰€鑳屽寘鏀句换浣曠墿鍝侊級+ 涓剧煕杩囪偐濮垮娍
		SscAddonNetworking.syncSpearChargeState(player, true);

		// 鏃犻噸鍔?+ 涓€娆℃€у啿閲忔枩鍚庝笂璺冿紙瑙嗙嚎鍙嶆柟鍚?+ 涓婏級锛岄殢鍚庤“鍑忓埌绌轰腑鎮诞
		PlayerGravityLease.acquire(player, SKILL_ID);
		Vec3 look = player.getLookAngle();
		double leapBack = BAL.d("leap_back", LEAP_BACK);
		double leapUp = BAL.d("leap_up", LEAP_UP);
		Vec3 back = new Vec3(-look.x, 0, -look.z);
		if (back.lengthSqr() < 1.0e-4) back = new Vec3(0, 0, -1);
		back = back.normalize();
		player.setDeltaMovement(back.x * leapBack, leapUp, back.z * leapBack);
		player.hurtMarked = true;
		player.fallDistance = 0.0f;

		ServerLevel sw = (ServerLevel) player.level();
		// 璧锋墜钃勫姏闊虫晥锛氭捣鏅舵牳婵€娲伙紙榄旀硶姘存秾锛岄煶閲忔媺澶у仛鏄庢樉锛? 楂橀€熸按鑺?+ 缇庤タ铻堝叆姘达紙鍏ㄥ憳鍙椈 null锛?
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2.0f, 1.2f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0f, 1.1f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1.2f, 0.8f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 0.2, player.getZ(),
				30, 0.4, 0.2, 0.4, 0.3);
	}

	/** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢ㄣ€?*/
	public static void tick(ServerPlayer player) {
		LeapState s = STATES.get(player.getUUID());
		if (s == null) return;
		if (player.isDeadOrDying() || !FormUtils.isUpgradeAxolotl(player)) {
			cancel(player); // 姝讳骸 / 褰㈡€佷涪澶?鈫?鍙栨秷骞跺綊杩樼墿鍝併€佹仮澶嶉噸鍔?
			return;
		}
		s.tick++;
		ServerLevel sw = (ServerLevel) player.level();
		int chargeTicks = BAL.i("charge_ticks", CHARGE_TICKS);
		double decay = BAL.d("decay", DECAY);

		if (s.tick < chargeTicks) {
			// 鏃犻噸鍔涗笅閫熷害琛板噺锛氳捣璺冨啿閲忓钩婊戞敹鏉熷埌 0 鈫?璺冭捣鍚庢偓娴湪绌轰腑锛堜笉钀斤級
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * decay, v.y * decay, v.z * decay);
			player.hurtMarked = true;
			player.fallDistance = 0.0f;
			if (s.tick % 4 == 0) {
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.FALLING_WATER, player.getX(), player.getY() + 1.0, player.getZ(),
						6, 0.4, 0.4, 0.4, 0.0);
			}
			// 钃勫姏闊虫晥锛氭瘡 5 tick 涓€澹颁笂鍗囨皵娉★紙闊宠皟闅忚搫鍔涜繘搴?0.8鈫?.7锛岃惀閫犺兘閲忚仛闆嗘劅锛?
			if (s.tick % 5 == 0) {
				float progress = (float) s.tick / (float) chargeTicks;
				float pitch = 0.8f + progress * 0.9f;
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.7f, pitch);
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.35f, pitch);
			}
			// 钃勬弧鍓嶇灛闂达紙鏈€鍚?3 tick锛夛細娴锋櫠鏍哥煭楦ｆ彁绀恒€屽嵆灏嗘姇鍑恒€?
			if (s.tick == chargeTicks - 3) {
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, 0.9f, 1.4f);
			}
		} else {
			// 鎶曠煕锛氭仮澶嶉噸鍔涖€佹湞鍑嗘槦鍙戝皠鐩寸嚎姘寸煕
			throwSpear(player, s);
			finish(player);
		}
	}

	/** 鏈濆噯鏄熸柟鍚戞姇鍑虹洿绾挎按鐭涖€?*/
	private static void throwSpear(ServerPlayer player, LeapState s) {
		ServerLevel sw = (ServerLevel) player.level();
		Vec3 dir = player.getLookAngle().normalize();
		ShellProjectile spear = new ShellProjectile(github.com.gengyoubo.sscfa.init.AddonEntities.THROWN_WATER_SPEAR.get(), sw); spear.setOwner(player);
		spear.setPos(player.getX() + dir.x * 0.6, player.getEyeY() - 0.1 + dir.y * 0.6, player.getZ() + dir.z * 0.6);
		spear.shoot(dir.x, dir.y, dir.z, 1.5f, 0.0f);
		sw.addFreshEntity(spear);
		// 涓?swingHand锛氫笌 vanilla 姘寸煕锛堜笁鍙夋垷锛夋姇鎺蜂竴鑷粹€斺€旂煕浠庝妇鐭涜搫鍔涘Э鍔跨洿鎺ラ鍑恒€佹墜鑷傝惤鍥炴甯革紝
		// 鑰岄潪鏅€氭敾鍑绘尌鎵嬶紙TridentItem.onStoppedUsing 鍚屾牱涓嶆尌鎵嬶級銆?
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.2f, 1.1f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 0.9f);
	}

	/** 鎶曠煕瀹屾垚锛氭仮澶嶉噸鍔涖€佺粨鏉熻搫鍔涙覆鏌撱€佽繘鍏?CD锛堢粺涓€鏈嶅姟 on_release 缁撶畻锛夊苟娓呯悊鐘舵€併€?*/
	private static void finish(ServerPlayer player) {
        LeapState s = STATES.remove(player.getUUID());
        if (s == null) return;
		PlayerGravityLease.release(player, SKILL_ID);
		SscAddonNetworking.syncSpearChargeState(player, false);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, s.castId);
	}

	/** 鍙栨秷锛堜笉杩?CD銆佷笉鎶曠煕锛夛細鎭㈠閲嶅姏銆佺粨鏉熻搫鍔涙覆鏌撱€?*/
	public static void cancel(ServerPlayer player) {
		LeapState s = STATES.remove(player.getUUID());
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s == null ? -1 : s.castId);
		if (s != null) {
			PlayerGravityLease.release(player, SKILL_ID);
			SscAddonNetworking.syncSpearChargeState(player, false);
		}
	}

	/** 璇ョ帺瀹讹紙鎸?UUID锛夋槸鍚﹀浜庢姇鎺锋按鐭涜搫鍔涗腑锛堟湇鍔＄鏉冨▉锛岀敤浜庤搫鍔涙湡绂佺敤鍙抽敭浜や簰锛夈€?*/
	public static boolean isCharging(java.util.UUID id) {
		return STATES.containsKey(id);
	}

	/** 鏂嚎锛氭仮澶嶉噸鍔涘苟娓呯悊銆?*/
	public static void onPlayerDisconnect(ServerPlayer player) {
        cancel(player);
	}
}
