package github.com.gengyoubo.sscfa.ability;

import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 闁叉垶鐭欏畝姝婸 - 閼奉亜鐣炬稊澶婃礀鐞涒偓缁崵绮?
 *
 * 闁叉垶鐭欏畝姘鳖洣濮濄垽鈧俺绻冩閬嶎棨鎼达箒鍤滈悞璺烘礀鐞涒偓閿涘牏鏁?GoldenSandstormHungerMixin 閹凤附鍩呴敍澶堚偓?
 * 娴犲懍浜掓稉瀣煙瀵繐褰查幁銏狀槻閻㈢喎鎳￠崐纭风窗
 *
 * 閹存ɑ鏋熼崘鍜冪礄5缁夋帒鍞撮張澶嬫暰閸戠粯鍨ㄩ崣妤€鍤敍?
 *   - 閼奉亣闊╅梽鍕閻ㄥ嫬鍣甸梿鑸电槨濞嗏€愁嚠閺佸矂鈧姵鍨氭导銈咁唺 閳?+1 HP閿涘牆宕愭０妤€绺鹃敍?
 *   - 閼奉亣闊╅柅鐘冲灇閸戠粯娼?                 閳?+4 HP閿?妫版绺鹃敍?
 *   - 閸愩儳瀚鹃敍鍦搉ubisWolfMinionEntity閿涘鈧姵鍨氶崙缁樻絻 閳?+3 HP閿?.5妫版绺鹃敍?
 *
 * 鐞氼偄濮╅崶鐐额攨:
 *   - 閹存ɑ鏋熸径? 濮?00 tick(10缁? +1 HP閿涘牆宕愭０妤€绺鹃敍?
 *   - 閹存ɑ鏋熼崘? 濮?20 tick(6缁?  +1 HP閿涘牆宕愭０妤€绺鹃敍?
 *
 * 婢跺洦鏁?
 *   - 閸楀﹪顣艰箛?= 1.0 HP, 1妫版绺?= 2.0 HP
 *   - 閹存ɑ鏋熼悩鑸碘偓?= 5缁?100 tick)閸愬懏婀佹稉璇插З閺€璇插毊閹存牕褰堥崚棰佹縺鐎?
 */
public final class GoldenSandstormRegen {

	// 娴犮儰绗呯敮锔衡偓宀勭帛鐠併們鈧秵鏁為柌濠勬畱鐢悂鍣哄鑼鐠?balance閿涘澃cope閿涙瓫bilities.golden_sandstorm_regen閿涘绱濇潻鎰攽閺冩湹绮犺箛顐ゅ弾鐠囪褰囬敍?
// COMBAT_DURATION / WITHER_SOURCE_GRACE 閺堫亞娅ョ拋甯礉娣囨繃瀵旂紓鏍槯閺堢喎鐖堕柌蹇嬧偓?
	/** 閸戝娴傚В蹇旑偧娴笺倕顔婇崶鐐额攨闁插骏绱橦P閿?= 閸楀﹪顣艰箛鍐︹偓鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿 */
	private static final float WITHER_TICK_HEAL = 1.0f;
	/** 閻溾晛顔嶉崙缁樻絻閸ョ偠顢呴柌蹇ョ礄HP閿?= 2妫版绺鹃妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿 */
	private static final float KILL_HEAL = 4.0f;
	/** 閸愩儳瀚鹃崙缁樻絻閸ョ偠顢呴柌蹇ョ礄HP閿?= 1.5妫版绺鹃妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿 */
	private static final float MINION_KILL_HEAL = 3.0f;
	/** 鐞氼偄濮╅崶鐐额攨闁插骏绱橦P閿?= 閸楀﹪顣艰箛鍐︹偓鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿 */
	private static final float PASSIVE_HEAL = 1.0f;
	/** 閹存ɑ鏋熸径鏍潶閸斻劌娲栫悰鈧梻鎾閿涘澅ick閿?= 10缁夋帇鈧倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿 */
	private static final int PASSIVE_INTERVAL_OOC = 200;
	/** 閹存ɑ鏋熼崘鍛邦潶閸斻劌娲栫悰鈧梻鎾閿涘澅ick閿?= 6缁夋帇鈧倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿 */
	private static final int PASSIVE_INTERVAL_IC = 120;
	/** 閹存ɑ鏋熼悩鑸碘偓浣瑰瘮缂侇厽妞傞梻杈剧礄tick閿?= 8缁?*/
	private static final long COMBAT_DURATION = 160L;
	/** 閸戝娴傚┃鎰唶瑜版洜娈戞０婵嗩樆娣囨繄鏆€閺冨爼妫块敍鍧眎ck閿涘绱濈憰鍡欐磰閸戝娴?tick 鏉堝湱鏅?*/
	private static final long WITHER_SOURCE_GRACE = 40L;

	/** 閻溾晛顔峌UID -> 娑撳﹥顐兼潻娑樺弳閹存ɑ鏋熼惃鍕瑯閻ｅ本妞傞梻?tick) */
	private static final Map<UUID, Long> LAST_COMBAT_TICK = new ConcurrentHashMap<>();
	/** 閻溾晛顔峌UID -> 娑撳﹥顐肩悮顐㈠З閸ョ偠顢呴惃鍕瑯閻ｅ本妞傞梻?tick) */
	private static final Map<UUID, Long> LAST_PASSIVE_TICK = new ConcurrentHashMap<>();
	/** 閸欐顔婇懓鍖睻ID -> { 闁叉垶鐭欏畝姘卞负鐎圭ΚUID -> 閸戝娴傚┃鎰唶瑜版洖鍩岄張鐔剁瑯閻ｅ本妞傞梻?tick) } */
	private static final Map<UUID, Map<UUID, Long>> WITHER_SOURCES = new ConcurrentHashMap<>();

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("abilities.golden_sandstorm_regen");

	private GoldenSandstormRegen() {
	}

	// ==================== 閹存ɑ鏋熼悩鑸碘偓?====================

	/** 閺嶅洩顔囬悳鈺侇啀鏉╂稑鍙嗛幋妯绘灍閻樿埖鈧緤绱欐禒鍛嚠闁叉垶鐭欏畝姘辨晸閺佸牞绱濋崗鏈电铂瑜般垺鈧椒绱扮悮?tick 閼奉亞鍔ц箛鐣屾殣閿?*/
	public static void markCombat(ServerPlayer player) {
		if (player == null) return;
		LAST_COMBAT_TICK.put(player.getUUID(), player.level().getGameTime());
	}

	/** 濡偓閺屻儳甯虹€硅埖妲搁崥锕€顦╂禍搴㈠灛閺傛濮搁幀?*/
	public static boolean isInCombat(ServerPlayer player) {
		// 閸氬本顒為弸婊嗘浆娴溿倖鍨崚銈呯暰閿涘牓鈧姵鍨?閸欐鍩屾导銈咁唺 10s 閸愬懌鈧焦鍨ㄩ弶銉︾爱娴犲秴婀?10 閺嶇厧鍞撮敍澶涚幢
		// 閸氬本妞傛穱婵堟殌闁叉垶鐭欏畝姘冲殰闊偆娈?markCombat 閺嶅洩顔囬敍鍫ｎ洬閻╂牕鍟ｉ悪闂磋礋娑撹姹夐弽鍥唶閵嗕礁鍣甸梿鑸电爱缁涘鐏夐摝鐘辩皑娴犳湹绗夊☉鐢垫磰閻ㄥ嫮澹掑▓濠冩降濠ф劧绱氶妴?
		if (github.com.gengyoubo.sscfa.ability.ParasiticCombatTracker.isInCombat(player)) {
			return true;
		}
		Long last = LAST_COMBAT_TICK.get(player.getUUID());
		if (last == null) return false;
		return player.level().getGameTime() - last <= COMBAT_DURATION;
	}

	// ==================== 閸戝娴傞弶銉︾爱鏉╁€熼嚋 ====================

	/** 濞夈劌鍞介崙瀣祩閺夈儲绨敍鍫ュ櫨濞屾瑥鐭氶悳鈺侇啀缂佹瑥褰堢€瑰疇鈧懏鏌﹂崝鐘插櫟闂嗚埖妞傜拫鍐暏閿?*/
	public static void registerWitherSource(LivingEntity victim, ServerPlayer player, int witherDurationTicks) {
		if (victim == null || player == null) return;
		long expire = victim.level().getGameTime() + (long) witherDurationTicks + WITHER_SOURCE_GRACE;
		WITHER_SOURCES.computeIfAbsent(victim.getUUID(), k -> new ConcurrentHashMap<>())
				.put(player.getUUID(), expire);
	}

	/** 閸戝娴?tick 鐎电懓褰堢€瑰疇鈧懘鈧姵鍨氭导銈咁唺閺冩儼鐨熼悽顭掔窗娑撶儤澧嶉張澶嬫暈閸愬瞼娈戦柌鎴炵煓瀹€姘卞负鐎硅泛娲栫悰鈧?*/
	public static void onWitherTickDamage(LivingEntity victim) {
		if (victim == null) return;
		Map<UUID, Long> sources = WITHER_SOURCES.get(victim.getUUID());
		if (sources == null || sources.isEmpty()) return;
		if (!(victim.level() instanceof ServerLevel serverWorld)) return;
		long now = serverWorld.getGameTime();
		// 濞撳懐鎮婃潻鍥ㄦ埂鐠佹澘缍?
		sources.entrySet().removeIf(e -> e.getValue() < now);
		if (sources.isEmpty()) {
			WITHER_SOURCES.remove(victim.getUUID());
			return;
		}
		for (UUID playerUuid : sources.keySet()) {
			Player p = serverWorld.getPlayerByUUID(playerUuid);
			if (!(p instanceof ServerPlayer sp)) continue;
			if (!FormUtils.isForm(sp, FormIdentifiers.GOLDEN_SANDSTORM_SP)) continue;
			sp.heal((float) BAL.d("wither_tick_heal", WITHER_TICK_HEAL));
			markCombat(sp);
		}
        if (!victim.isAlive()) WITHER_SOURCES.remove(victim.getUUID());
	}

	// ==================== 鐞氼偄濮╅崶鐐额攨 tick ====================

    /** Use the configured tick interval exactly; only players without a timer use the cheap form probe. */
    public static void tick(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;
        UUID owner = player.getUUID();
        if (player.tickCount % 20 != 0 && !LAST_PASSIVE_TICK.containsKey(owner)) return;
        if (!FormUtils.isForm(player, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
            LAST_PASSIVE_TICK.remove(owner);
            return;
        }
		// 濠娐ゎ攨閺冩湹绮庨崚閿嬫煀鐠佲剝妞傞崳顭掔礉闁灝鍘ゆ稉鈧潻娑欏灛閺傛姘ㄧ粩瀣祮缂佹挾鐣荤槐顖溞濋弮鍫曟？
		long now = player.level().getGameTime();
		if (player.getHealth() >= player.getMaxHealth()) {
			LAST_PASSIVE_TICK.put(player.getUUID(), now);
			return;
		}
		Long last = LAST_PASSIVE_TICK.get(player.getUUID());
		if (last == null) {
			LAST_PASSIVE_TICK.put(player.getUUID(), now);
			return;
		}
		int interval = isInCombat(player) ? BAL.i("passive_interval_ic", PASSIVE_INTERVAL_IC) : BAL.i("passive_interval_ooc", PASSIVE_INTERVAL_OOC);
		if (now - last >= interval) {
			player.heal((float) BAL.d("passive_heal", PASSIVE_HEAL));
			LAST_PASSIVE_TICK.put(player.getUUID(), now);
		}
	}

	// ==================== 閸戠粯娼冮惄鎴濇儔 ====================

	/** 濞夈劌鍞介崙缁樻絻娴滃娆㈤惄鎴濇儔閸ｎ煉绱欓崷?SscAddon.onInitialize 娑擃叀鐨熼悽顭掔礆 */
	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.onLivingDeath(GoldenSandstormRegen::onEntityDeath);
	}

	private static void onEntityDeath(LivingEntity entity, DamageSource source) {
		if (entity == null || entity.level().isClientSide()) return;
		// 1. 閻溾晛顔嶉惄瀛樺复閸戠粯娼?
		if (source.getEntity() instanceof ServerPlayer killer
				&& FormUtils.isForm(killer, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
			killer.heal((float) BAL.d("kill_heal", KILL_HEAL));
			markCombat(killer);
		}
		// 2. 閸愩儳瀚鹃崙缁樻絻
		else if (source.getEntity() instanceof AnubisWolfMinionEntity wolf
				&& entity.level() instanceof ServerLevel serverWorld) {
			UUID ownerUuid = wolf.getOwnerUUID();
			if (ownerUuid != null) {
				Player owner = serverWorld.getPlayerByUUID(ownerUuid);
				if (owner instanceof ServerPlayer ownerPlayer
						&& FormUtils.isForm(ownerPlayer, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
					ownerPlayer.heal((float) BAL.d("minion_kill_heal", MINION_KILL_HEAL));
					markCombat(ownerPlayer);
				}
			}
		}
        // Successful lethal wither is credited by damage RETURN, after AFTER_DEATH has run.
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)) WITHER_SOURCES.remove(entity.getUUID());
	}

	// ==================== 閻樿埖鈧焦绔婚悶?====================

	/** 濞撳懐鈹栭幍鈧張澶屽Ц閹緤绱欓張宥呭閸ｃ劌鎯庨崑?/ 閺佺増宓侀崠鍛村櫢鏉炴枻绱?*/
	public static void clearAll() {
		LAST_COMBAT_TICK.clear();
		LAST_PASSIVE_TICK.clear();
		WITHER_SOURCES.clear();
	}

	/** 濞撳懐鎮婇崡鏇氶嚋閻溾晛顔嶉悩鑸碘偓渚婄礄閺傤厾鍤?/ 閸掑洦宕茶ぐ銏♀偓渚婄礆 */
	public static void clearPlayer(UUID playerUuid) {
		if (playerUuid == null) return;
		LAST_COMBAT_TICK.remove(playerUuid);
		LAST_PASSIVE_TICK.remove(playerUuid);
		WITHER_SOURCES.values().forEach(m -> m.remove(playerUuid));
	}
    public static void pruneSources(long now) {
        WITHER_SOURCES.values().removeIf(sources -> {
            sources.values().removeIf(expiry -> expiry < now);
            return sources.isEmpty();
        });
    }
}
