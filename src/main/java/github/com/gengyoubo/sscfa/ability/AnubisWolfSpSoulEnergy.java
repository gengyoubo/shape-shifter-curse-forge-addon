package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/**
 * SP闂冨灝濮В鏃€鏌夋稊瀣 - 閻忕敻鐡婇懗浠嬪櫤缁崵绮?
 * 闁俺绻冮崙缁樻絻/閹存ɑ鏋熺粔顖滅柈閻忕敻鐡婇懗浠嬪櫤閿涘牊娓舵径?00閻愮櫢绱氶敍灞惧姬閼充粙鍣洪弮鏈电瑓娑撯偓濞嗏剝顒存禍锟狀暙閸╃喐鏌﹂弨鎯х殺鐞氼偄顤冨鐚寸窗
 * - 閸楀﹤绶?24閳?2
 * - 閸忓懓鍏橀弮鍫曟？ 2缁夋巻鍟?缁?
 * - 閹镐胶鐢婚弮鍫曟？ 15缁夋巻鍟?0缁?
 * - 閸戝娴侷I 閺囧じ鍞?閸戝娴侷
 * - 閼奉亜濮╅崣顒€鏁?閸欘亜鍟ｉ悪?
 * 閼充粙鍣哄鈩冩濞戝牐鈧鍙忛柈銊ㄥ厴闁插繑鏌﹂弨鎯ь杻瀵椽顣崺鐔粹偓?
 */
public class AnubisWolfSpSoulEnergy {

	/**
	 * 閺堚偓婢堆呬紥姒勫倽鍏橀柌蹇ョ礄姒涙顓婚崐纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘cope閿涙瓫bilities.anubis_soul_energy閿?
	 */
	public static final int MAX_ENERGY = 100;

// ==================== 鐢悂鍣?====================
	// 娴犮儰绗呯敮鎼佸櫤閸у洣璐熸妯款吇閸婄》绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閿涘澃cope閿涙瓫bilities.anubis_soul_energy閿?
	/**
	 * 閸︺劍顒存禍锟狀暙閸╃喎鍞撮崙缁樻絻閼惧嘲绶遍惃鍕厴闁?
	 */
	private static final int KILL_IN_DOMAIN_ENERGY = 20;
	/**
	 * 閸愩儳瀚鹃崙缁樻絻閼惧嘲绶遍惃鍕厴闁?
	 */
	private static final int MINION_KILL_ENERGY = 10;
	/**
	 * 閺咁噣鈧艾鍤弶鈧懢宄扮繁閻ㄥ嫯鍏橀柌?
	 */
	private static final int REGULAR_KILL_ENERGY = 5;
	/**
	 * 閼奉亣闊╂径鍕艾閸戝娴傞弮璺哄毊閺夆偓妫版繂顦婚懢宄扮繁閻ㄥ嫯鍏橀柌蹇ョ礄閸戝娴傞弨璺哄瀵邦亞骞嗛敍?
	 */
	private static final int WITHER_KILL_BONUS_ENERGY = 10;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("abilities.anubis_soul_energy");

	private AnubisWolfSpSoulEnergy() {
	}

// ==================== 閸忣剙绱戦幒銉ュ經 ====================

	/**
	 * 閼惧嘲褰囬悳鈺侇啀瑜版挸澧犻悘鐢哥搳閼充粙鍣?
	 */
	public static int getEnergy(ServerPlayer player) {
		return ResourceBars.get(player, BarKeys.ANUBIS_SOUL);
	}

	/**
	 * 婢х偛濮為悘鐢哥搳閼充粙鍣洪敍鍫ｅ殰閸斻劑妾洪崚鏈电瑐闂勬劧绱氶妴?
	 * <p>娣囶喖顦查敍姘辩埠娑撯偓濡楀棙鐏?maxOf 閸?power 閺屻儴顕楀鍌氱埗鐠侯垰绶炴稉瀣讲閼冲€熺箲閸?0閿涘苯顕遍懛?gain 鐞?clamp 閹?0閿涘牐鍏橀柌蹇撳娑撳秳绗傞敍?
	 * 鐞涖劎骞囨稉鍝勫将閸炪倗澧?閸戝娴傞崙缁樻絻婢惰鲸鏅ラ敍澶堚偓鍌涱劃婢跺嫭鏁兼稉铏规纯閹恒儴顕?VariableIntPower 閻ㄥ嫮婀＄€?max閿?
	 * 閺屻儰绗夐崚鐗堟閸ョ偤鈧偓鐢悂鍣?{@link #MAX_ENERGY}閿涘牅绗岄弮褏澧楃悰灞艰礋娑撯偓閼疯揪绱氶敍灞肩箽鐠囦椒鎹㈡担鏇熸鎼村繋绗呴柈鍊熷厴缁鳖垳袧閵?
	 */
	public static void addEnergy(ServerPlayer player, int amount) {
		int max = PowerUtils.getResourceMax(player, FormIdentifiers.ANUBIS_WOLF_SP_SOUL_ENERGY);
		if (max <= 0) {
			max = BAL.i("max_energy", MAX_ENERGY); // power 閺堫亜姘ㄧ紒?閺屻儴顕楁径杈Е閻ㄥ嫬娲栭柅鈧稉濠囨閿涘牊妫悧鍫ｎ攽娑撶尨绱?
		}
		int current = getEnergy(player);
		int newValue = Math.min(current + amount, max);
		if (newValue != current) {
			ResourceBars.set(player, BarKeys.ANUBIS_SOUL, newValue);
		}
	}

	/**
	 * 濡偓閺屻儳浼掓鍌濆厴闁插繑妲搁崥锕€鍑″?
	 */
	public static boolean isFullEnergy(ServerPlayer player) {
		return getEnergy(player) >= ResourceBars.maxOf(player, BarKeys.ANUBIS_SOUL);
	}

	/**
	 * 濞戝牐鈧鍙忛柈銊т紥姒勫倽鍏橀柌蹇ョ礄閺傝姤鏂佹晶鐐插繁妫板棗鐓欓弮鎯扮殶閻㈩煉绱?
	 */
	public static void consumeEnergy(ServerPlayer player) {
		ResourceBars.set(player, BarKeys.ANUBIS_SOUL, 0);
	}

	/**
	 * 濞撳懘娅庨悳鈺侇啀閼充粙鍣洪敍鍫熸焽缁?閸欐ê鑸伴弮璁圭礆
	 */
	public static void clearPlayer(ServerPlayer player) {
		if (player != null) {
			ResourceBars.set(player, BarKeys.ANUBIS_SOUL, 0);
		}
	}

	/**
	 * 鐠佸墽鐤嗛悘鐢哥搳閼充粙鍣洪敍鍫㈡暠set_mana閸涙垝鎶ょ拫鍐暏閿?
	 */
	public static void setEnergy(ServerPlayer player, int amount) {
		ResourceBars.set(player, BarKeys.ANUBIS_SOUL, amount);
	}

	// ==================== 濡楀棙鐏﹂幒銉ュ弳閿涙碍寮ч懗鍊熜曢崣鎴︾叾閺佸牞绱橞arTrigger閿涘本娴涙禒锝呭斧閸愬懓浠堥崚銈嗘焽閿?====================

	/**
	 * 閸戝娴傞崙缁樻絻閸ョ偠鍏橀敍姘綀鐎瑰疇鈧懓顫﹂梼鍨Й濮ｆ梹鏌夐悳鈺侇啀閿涘牊鍨ㄩ崗璺哄暎閻欑》绱氶弬钘夊閸戝娴傞崥搴㈩劥娴滃骸鍣甸梿?DOT 閺冭绱濈紒娆愭煢閸旂姾鈧懎娲栭懗濮愨偓?
	 * <p>閸樼喓澧楅崙瀣祩閹哄顢呴惃?DamageSource 閺?attacker閿涘牊鐓℃稉宥呭煂閸戠粯娼冮懓鍜冪礆閿涘本鏅犲▽璺ㄦ暏闁叉垶鐭欏畝姘辨畱閵嗗本鏌﹂崝鐘虫濞夈劌鍞介弶銉︾爱閵嗗秵鏌熷鍫窗
	 * 閺傝棄濮為崙瀣祩閺冩儼顔囪ぐ?(閸欐顔婇懓?閳?閺傝棄濮為懓?閳?鏉╁洦婀￠弮璺哄煝)閿涘本顒存禍鈩冩閼汇儲鐓℃稉宥呭煂閻╁瓨甯撮崙缁樻絻閼板懍绗栭煬顐＄瑐閺堝銆冮崘鍛櫟闂嗚绱濋崶鐐村嚱缂佹瑦娓舵潻鎴炴煢閸旂姾鈧懌鈧?
	 */
	private static final java.util.Map<UUID, java.util.Map<UUID, Long>> WITHER_SOURCES = new java.util.concurrent.ConcurrentHashMap<>();
	/** 閸戝娴傞弶銉︾爱鏉╁洦婀＄€逛粙妾洪敍鍧眎ck閿涘绱伴弫鍫熺亯閸掔増婀￠崥搴ｇ翱缁犫偓鐎涙ɑ妞块弮鍫曟？閿涘矂浼╅崗宥嗙暙閻ｆ瑨顕よぐ鎺戞礈閵?*/
	private static final long WITHER_SOURCE_GRACE = 40L;

	/** 濞夈劌鍞介崙瀣祩閺夈儲绨敍鍫㈡暠 SscAddonLivingEntityMixin 閸︺劑妯嬮崝顏呯槷閺傤垳甯虹€硅埖鏌﹂崝鐘插櫟闂嗚埖妞傜拫鍐暏閿涘鈧?*/
	public static void registerWitherSource(LivingEntity victim, ServerPlayer player, int witherDurationTicks) {
		if (victim == null || player == null) return;
		long expire = victim.level().getGameTime() + (long) witherDurationTicks + WITHER_SOURCE_GRACE;
		WITHER_SOURCES.computeIfAbsent(victim.getUUID(), k -> new java.util.concurrent.ConcurrentHashMap<>())
				.put(player.getUUID(), expire);
	}

	/** 濮濊楠搁崶鐐村嚱閿涙碍鐓￠惄瀛樺复閸戠粯娼冮懓鍛￥閺嬫粍妞傞敍宀冨闊偂绗傞張澶嬫弓鏉╁洦婀￠崙瀣祩閺夈儲绨敍宀€绮伴張鈧潻鎴犳畱閺傝棄濮為懓鍛礀閼宠棄鑻熷〒鍛倞閵?*/
	private static void onWitherDeath(LivingEntity victim) {
		java.util.Map<UUID, Long> sources = WITHER_SOURCES.get(victim.getUUID());
		if (sources == null || sources.isEmpty()) return;
		long now = victim.level().getGameTime();
		UUID best = null;
		long bestExpire = Long.MIN_VALUE;
		for (java.util.Map.Entry<UUID, Long> e : sources.entrySet()) {
			if (e.getValue() < now) {
				sources.remove(e.getKey());
				continue;
			}
			if (e.getValue() > bestExpire) {
				bestExpire = e.getValue();
				best = e.getKey();
			}
		}
		if (sources.isEmpty()) {
			WITHER_SOURCES.remove(victim.getUUID());
			return;
		}
		// 閸欘亜娲栧┃顖濋煩娑撳﹣绮涚敮鎼佲敆閸戝娴傞弫鍫熺亯閻ㄥ嫸绱欓梼鑼剁箖鐎规枻绱?
		if (!victim.hasEffect(MobEffects.WITHER)) return;
		Player ownerEntity = victim.level().getPlayerByUUID(best);
		if (ownerEntity instanceof ServerPlayer ownerPlayer
				&& FormUtils.isForm(ownerPlayer, FormIdentifiers.ANUBIS_WOLF_SP)) {
			addEnergy(ownerPlayer, BAL.i("wither_kill_bonus_energy", WITHER_KILL_BONUS_ENERGY));
		}
		WITHER_SOURCES.remove(victim.getUUID());
	}

	/** 濠娐ゅ厴閹绘劗銇氶棅绛圭窗鐠恒劌鍙嗗鈥斥偓鍏兼閹绢叏绱欐稉搴″斧 addEnergy 閸愬懓浠堥柅鏄忕帆缁涘鐜敍澶堚偓?*/
	private static final github.com.gengyoubo.sscfa.resource.BarTrigger ANUBIS_FULL_TRIGGER =
			(player, oldV, newV, max) -> {
				if (oldV < max && newV >= max) {
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.2f);
				}
			};

	static {
		// 閹稿倸鍩岀紒鐔剁濡楀棙鐏﹂敍鍫濆讲閹绘帗瀚堥敍姘毙╅梽銈嗘拱鐞涘苯宓嗛崢缁樺竴闂婅櫕鏅ラ敍灞肩瑝瑜板崬鎼烽崗璺虹暊闁槒绶敍?
		BarKeys.ANUBIS_SOUL.addTrigger(ANUBIS_FULL_TRIGGER);
	}

// ==================== 娴滃娆㈠▔銊ュ斀 ====================

	/**
	 * 濞夈劌鍞介崙缁樻絻娴滃娆㈤惄鎴濇儔閸ｎ煉绱欓崷鈯縮cAddon.onInitialize娑擃叀鐨熼悽顭掔礆
	 */
	public static void registerEvents() {
		github.com.gengyoubo.sscfa.util.SscEvents.onLivingDeath((victim, source) -> {
            try { onEntityDeath(victim, source); }
            finally { WITHER_SOURCES.remove(victim.getUUID()); }
        });
	}

	/**
	 * 鐎圭偘缍嬪璁抽閺冨墎娈戞径鍕倞闁槒绶?
	 */
	private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
		if (entity.level().isClientSide()) return;

// 閹懎鍠?閿涙艾鍣甸梿?DOT 閸戠粯娼冮敍鍫熸￥ attacker 瑜版帒娲滈敍澶嗏偓鏂衡偓鏃€瀵滈弬钘夊閺夈儲绨崶鐐村嚱閿涘牆鎯堥悳鈺侇啀/閸愩儳瀚鹃弬钘夊閻ㄥ嫬鍣甸梿璁圭礆
		if (damageSource.getEntity() == null
				&& damageSource.is(net.minecraft.world.damagesource.DamageTypes.WITHER)) {
			onWitherDeath(entity);
			return;
		}

// 閹懎鍠?閿涙氨甯虹€瑰墎娲块幒銉ュ毊閺夆偓
		if (damageSource.getEntity() instanceof ServerPlayer killer) {
			if (!FormUtils.isForm(killer, FormIdentifiers.ANUBIS_WOLF_SP)) return;

// 濡偓閺屻儱鍤弶鈧弰顖氭儊閸欐垹鏁撻崷銊︻劥娴滐繝顣崺鐔峰敶
			if (AnubisWolfSpDeathDomain.hasActiveDomain(killer.getUUID())
					&& AnubisWolfSpDeathDomain.isInActiveDomain(killer.getUUID(), entity.blockPosition())) {
// 婢х偛宸辨０鍡楃厵閼煎啫娲块崘鍛毊閺夆偓娑撳秷骞忛崣鏍厴闁?
				if (!AnubisWolfSpDeathDomain.isEnhancedDomain(killer.getUUID())) {
					addEnergy(killer, BAL.i("kill_in_domain_energy", KILL_IN_DOMAIN_ENERGY));
				}
			} else {
				addEnergy(killer, BAL.i("regular_kill_energy", REGULAR_KILL_ENERGY));
			}
// 閸戝娴傞崙缁樻絻閸ョ偠鍏橀敍姘冲殰闊偄顦╂禍搴″櫟闂嗚埖妞傞崙缁樻絻妫版繂顦?+10 閻忕敻鐡婇懗浠嬪櫤
			if (killer.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
				addEnergy(killer, BAL.i("wither_kill_bonus_energy", WITHER_KILL_BONUS_ENERGY));
			}
			return;
		}

// 閹懎鍠?閿涙艾鍟ｉ悪鐓庡毊閺夆偓閿涘湏nubisWolfMinionEntity閻ㄥ埣wner閼惧嘲绶遍懗浠嬪櫤閿?
		if (damageSource.getEntity() instanceof AnubisWolfMinionEntity wolf) {
			java.util.UUID ownerUuid = wolf.getOwnerUUID();
			if (ownerUuid == null) return;

			Player ownerEntity = entity.level().getPlayerByUUID(ownerUuid);
			if (ownerEntity instanceof ServerPlayer ownerPlayer) {
				if (!FormUtils.isForm(ownerPlayer, FormIdentifiers.ANUBIS_WOLF_SP)) return;
// 婢х偛宸辨０鍡楃厵閼煎啫娲块崘鍛暎閻欑厧鍤弶鈧稉宥堝箯閸欐牞鍏橀柌?
				if (AnubisWolfSpDeathDomain.isEnhancedDomain(ownerUuid)
						&& AnubisWolfSpDeathDomain.isInActiveDomain(ownerUuid, entity.blockPosition())) {
					return;
				}
				addEnergy(ownerPlayer, BAL.i("minion_kill_energy", MINION_KILL_ENERGY));
// 閸戝娴傞崙缁樻絻閸ョ偠鍏橀敍姘暎閻欑厧鍤弶鈧弮璁圭礉閼汇儰瀵屾禍鍝勵槱娴滃骸鍣甸梿璁圭礉妫版繂顦?+10 閻忕敻鐡婇懗浠嬪櫤
				if (ownerPlayer.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
					addEnergy(ownerPlayer, BAL.i("wither_kill_bonus_energy", WITHER_KILL_BONUS_ENERGY));
				}
			}
		}
	}
    public static void pruneSources(long now) {
        WITHER_SOURCES.values().removeIf(sources -> {
            sources.values().removeIf(expiry -> expiry < now);
            return sources.isEmpty();
        });
    }
    public static void clearSources() { WITHER_SOURCES.clear(); }

}
