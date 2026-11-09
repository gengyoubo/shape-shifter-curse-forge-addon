package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 妞嬬喐鈪垫鏃撶礄Nightmare閿涘瀵岀憰浣瑰Η閼冲鈧本浜归幆褋鈧稄绱橣ear閿涘鈧柡鈧?閺堝秴濮熺粩顖涙綀婵炰胶濮搁幀浣规簚閵?
 *
 * <p>閹稿鏁敍鍧皃_primary閿涘袝閸欐埊绱扮€?b>閹碘偓閺堝鍑￠崗銉︹叺</b>閻╊喗鐖ｉ弬钘夊閹劖鍎抽敍鍫熸￥鐠侀缚绐涚粋浼欑礆閿涘矁顩﹀Ч鍌涘Η閼?CD 鐏忚京鍗庨妴?
 * 閹劖鍎抽幐浣虹敾 15 缁夋帪绱?00t閿涘绱濋張鐔兼？閿?/p>
 * <ul>
 *   <li>閻╊喗鐖ｇ憴鍡涘櫣缂傗晞鍤?16 閺嶇》绱欑€广垺鍩涚粩顖滅焽闂嗙偓绗庢潻娑欒窗閸忋儻绱濇禒鍛拱娴滃搫褰茬憴渚婄礆+ 閸戝繘鈧?15%閿?/li>
 *   <li>鐎瑰牆宕奸懓鍛础韫囧啳鐑︽竟甯礄閺堝秴濮熺粩顖氭噯閺堢喎绠嶉幘顓ㄧ礉閺堫兛姹夐崣顖炴閿涘绱?/li>
 *   <li>閻╊喗鐖ｉ惃鍕弳濮婏箑鍩岄張鐔告闂?b>閹镐胶鐢婚柨浣哥暰閸?20s</b>閿涘牊鐦?tick 閸掗攱鏌婇崚?now+400閿涘绱?/li>
 *   <li>閸?b>娴犺缍嶆鐔糕叺姒?/b>娴笺倕顔?鑴?閿涘牐顫?{@code SscAddonLivingEntityMixin} damage 闁解晛鐡欓敍澶涚幢</li>
 *   <li>濮婏箓鐡熸潻娑樺弳閻╊喗鐖?16 閺嶈壈顫嬮柌?閳?閻╊喗鐖ｇ€广垺鍩涚粩顖濆箯瀵?1 缁夋帇鈧瞼婀呮稉宥堫潌鐠囥儲鈪垫鏂烩偓宥囩崶閸欙綇绱欏〒鍙夌厠鐏炲繗鏂€閿涘奔绮庨張顒€婀撮敍澶堚偓?/li>
 * </ul>
 * <p>閹劖鍎崇紒鎾存将閿涙艾宸遍崚璺哄毉濮?+ 20s閿?00t閿涘鍙嗗锕€鍘ら悿顐礄{@code NightmareDreamManager} 閻?IMMUNE 鐞涱煉绱氶妴?/p>
 *
 * <p>鐎广垺鍩涚粩顖氬瘶 {@code PACKET_FEAR_STATE}閿涘湯2C閿涘奔绮庨惄顔界垼閺堫兛姹夐敍澶涚窗varint durationTicks閵?
 * 缁娴樺ǎ鈥冲弳/韫囧啳鐑﹀〒鍙夌厠/1s 闂呮劘闊╅崚銈呯暰閸忋劌婀€广垺鍩涚粩?{@code NightmareFearClient}閵?/p>
 */
public final class NightmareFearManager {

	// ==== 娴犮儰绗呯敮鎼佸櫤娑撴椽绮拋銈呪偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘澃cope: abilities.nightmare_fear閿涘本鏆熼幑顔煎瘶閸欘垵顩惄鏍电礆====
	/** 閹劖鍎抽幐浣虹敾 tick閿?5 缁夋帪绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int FEAR_DURATION_TICKS = 300;
	/** 濮婏箓鐡忛幋鎺撳瘹閿涙碍浜归幆褎瀵旂紒顓熸闂€鍨杻楠炲拑绱?35% 閳?405t 閳?20.25 缁夋帪绱氶敍灞炬煢閸旂姷鐏涢梻鏉戞彥閻撗佲偓鍌涙弓閻ф槒顔?balance閿涘奔绻氶幐浣哥埗闁插繈鈧?*/
	public static final float FEAR_DURATION_RING_BONUS = github.com.gengyoubo.sscfa.item.NightmareRingItem.FEAR_DURATION_BONUS;
	/** 閹垛偓閼?CD閿涘澅ick閿?0 缁夋帪绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int FEAR_COOLDOWN_TICKS = 400;
	/** 鐠囧懎鎷绘稊瀣箑閸忛亶绂忛敍姘崇槒閸滄帊绠ｉ張鍫濈秼婢舵粍浜归幆?CD 闂勫秳璐?14 缁夋帪绱?80t閿涘鈧倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int FEAR_COOLDOWN_TICKS_CURSED_MOON = 280;
	/** 閹劖鍎崇紒鎾存将閸氬骸鍙嗗锕€鍘ら悿顐ｆ闂€鍖＄礄tick閿?0 缁夋帪绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int DREAM_IMMUNE_TICKS = 400;
	/** 韫囧啳鐑﹂棅铏櫏闂傛挳娈ч敍鍧眎ck閿?.6 缁?閹峰秮鈧柡鈧柨鐣ч崡顐モ偓鍛妇鐠哄疇濡總蹇ョ礆閵嗗倹婀惂鏄忣唶 balance閿涘奔绻氶幐浣哥埗闁插繈鈧?*/
	public static final int HEARTBEAT_INTERVAL = 32;
	/** 閸戝繘鈧?20% 閻ㄥ嫬鐫橀幀?modifier UUID閿涘牆娴愮€?UUID閿涘苯褰查獮鍌滅搼缁夊娅庨敍澶堚偓?*/
	private static final UUID FEAR_SLOW_UUID = UUID.fromString("e3a1f7c2-9b4d-4e6a-8c15-d2f3a7b9e810");
	private static final String FEAR_SLOW_NAME = "Nightmare Fear Slow";
	/** 閸戝繘鈧喎绠欐惔锔肩礄閻劍鍩涚€规氨顭堥敍姘箑婢跺洤鍣洪柅?20%閿涘瞼甯虹€?閻㈢喓澧挎稉鈧懛杈剧礆閵嗗倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final float FEAR_SLOW_RATIO = 0.20f;
	/** 闂堢偟甯虹€瑰墎娲伴弽鍥╂畱娴犲洦浠洪崢瀣煑閿涙俺顫﹀锕傜摕閺€璇插毊閸氬海娈戦崣宥呭毊缁愭褰涢敍鍧眎ck閿? 缁夋帪绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int MOB_AGGRO_WINDOW_TICKS = 40;
	/** 閹劖鍎崇憴鍡涘櫣閸楀﹤绶為敍鍫熺壐閿涘鈧倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final double FEAR_SIGHT_RADIUS = 16.0;
	/** 閸欘垵顫嗛幀褑鍓﹂崘璇х窗闂呮劕灏堕惄闀愮秴閺冨爼鏆遍敍鍧眎ck閿? 缁夋帪绱氶妴鍌涙弓閻ф槒顔?balance閿涘奔绻氶幐浣哥埗闁插繈鈧?*/
	public static final int PULSE_HIDDEN_TICKS = 40;
	/** 閸欘垵顫嗛幀褑鍓﹂崘璇х窗閻滄澘鑸伴惄闀愮秴閺冨爼鏆遍敍鍧眎ck閿?.6 缁夋帪绱氶妴鍌涙弓閻ф槒顔?balance閿涘奔绻氶幐浣哥埗闁插繈鈧?*/
	public static final int PULSE_VISIBLE_TICKS = 12;
	/** 閺€璇插毊閺勬儳鑸伴弮鍫曟毐閿涘澅ick閿?.5 缁夋帪绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int REVEAL_ON_ATTACK_TICKS = 30;

	/** 閺堝秴濮熺粩顖涙綀婵?balance 韫囶偆鍙庣拠璇插絿閿涘牆鎻╅悡褎婀崚婵嗩潗閸栨牗妞傞崶鐐衡偓鈧稉濠冩煙姒涙顓荤敮鎼佸櫤閿涘鈧?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.nightmare_fear");
	/** 缂佺喍绔撮崘宄板祱閺堝秴濮熼惃鍕旂€规碍濡ч懗?ID閿? power 濞夈劌鍞界捄顖氱窞閿涘鈧?*/
	private static final String SKILL_ID = "my_addon:form_wild_cat_nightmare_fear";
	/** 閼煎啫娲挎径鏍х暚閸忋劑娈ｉ崠璺ㄦ畱閸栧懎鍩涢弬浼存？闂呮棑绱檛ick閿涘鈧?*/
	private static final int OUT_OF_RANGE_HIDE_REFRESH = 40;
	/** 韫囧啳鐑﹂崺铏诡攨闂婃娊鍣洪敍鍫ｅ瘱閸ユ潙顦?閺冪姵鈪垫鏃€妞傞惃鍕€亸蹇撴祼鐎规岸鐓堕柌蹇ョ礆閵?*/
	private static final float HEARTBEAT_BASE_VOL = 0.15f;
	/** 韫囧啳鐑﹂張鈧径褔鐓堕柌蹇ョ礄濮婏箓鐡熺拹纾嬪姱閺冭绱氶妴?*/
	private static final float HEARTBEAT_MAX_VOL = 1.2f;

	/** 閸欘垵顫嗛幀褏濮搁幀浣规簚閿涙氨娲伴弽鍢ID|濮婏箓鐡烾UID -> 閼村鍟块惄闀愮秴閻樿埖鈧降鈧?*/
	private static final Map<String, PairState> PAIRS = new ConcurrentHashMap<>();

	/** 閸楁洖顕敍鍫㈡窗閺嶅毟妤佲叺姒勬棑绱氶惃鍕讲鐟欎焦鈧嗗墻閸愯尙濮搁幀浣碘偓?*/
	private static final class PairState {
		/** 瑜版挸澧犻弰顖氭儊闂呮劕灏堕惄闀愮秴閵?*/
		boolean hidden;
		/** 瑜版挸澧犻惄闀愮秴閸掑洦宕查弮璺哄煝閿? = 鐏忔碍婀崚婵嗩潗閸?閸掓矮绮犻懠鍐ㄦ纯婢舵牕娲栭弶銉礆閵?*/
		long phaseEnd;
		/** 娑撳﹥顐奸崣鎴︽閸栧灝瀵橀惃鍕閸掍紮绱欓懠鍐ㄦ纯婢舵牕鎳嗛張鐔峰煕閺傛壆鏁ら敍澶堚偓?*/
		long lastHideSent;
	}

	/** 濮婏箓鐡熼弨璇插毊閹劖鍎抽惄顔界垼閺冩儼鐨熼悽顭掔礄damage mixin閿涘绱伴弰鎯ц埌 1.5s 楠炲爼鍣哥純顔款嚉鐎靛湱娈戦懘澶婂暱閻╅晲缍呴妴?*/
	public static void onNightmareAttackFeared(ServerPlayer target, ServerPlayer attacker) {
		long now = target.level().getGameTime();
		int reveal = BAL.i("reveal_on_attack_ticks", REVEAL_ON_ATTACK_TICKS);
		PairState ps = PAIRS.get(target.getUUID() + "|" + attacker.getUUID());
		if (ps != null) {
			ps.hidden = false;
			ps.phaseEnd = now + reveal;
		}
		SscAddonNetworking.sendFearReveal(target, attacker.getUUID(), reveal);
	}

	/** 濞撳懐鎮婇弻鎰窗閺嶅洨娈戦崗銊╁劥閸欘垵顫嗛幀褏濮搁幀渚婄礄閹劖鍎崇紒鎾存将/閺傤厾鍤庨敍澶堚偓?*/
	private static void clearPairs(UUID tid) {
		PAIRS.keySet().removeIf(k -> k.startsWith(tid + "|"));
	}

	/** 閹劖鍎虫稉顓犳窗閺嶅浄绱伴惄顔界垼 UUID -> FearState{閸掔増婀￠弮鍫曟？, 閺傝姤浜归幆褏娈戝锕傜摕 UUID}閵?*/
	private static final Map<UUID, FearState> FEARING = new ConcurrentHashMap<>();
	/** 閸忋儲鈪甸崗宥囨焻鐞涱煉绱伴惄顔界垼 UUID -> 閸忓秶鏌呴崚鐗堟埂娑撴牜鏅弮鍫曟？閿涘牊浜归幆褏绮ㄩ弶鐔稿劦缂冩氨鐛ラ崣锝忕礆閵?*/
	private static final Map<UUID, Long> DREAM_IMMUNE = new ConcurrentHashMap<>();

	private NightmareFearManager() {
	}

	/** 閻╊喗鐖ｈぐ鎾冲閺勵垰鎯佹径鍕艾閹劖鍎虫稉顓ㄧ礄閺堝秴濮熺粩顖氬灲鐎规熬绱氶妴?*/
	public static boolean isFeared(UUID targetUuid, long now) {
		FearState v = FEARING.get(targetUuid);
		return v != null && v.endTick > now;
	}

	/**
	 * 娑撯偓濞嗏剝鈧勭Х閼版ぜ鈧苯寮婚崐宥勬縺鐎圭偨鈧秵婧€娴兼熬绱欓悽銊﹀煕鐟欏嫭鐗搁敍姘殻娑擃亝浜归幆褎婀￠梻鏉戝涧鐟欙箑褰傛稉鈧▎鈽呯礆閵?
	 * 婢跺嫪绨幁鎰劤娑擃厺绗栭張顒侇偧閹劖鍎崇亸姘弓閻劏绻?閳?閺嶅洩顔囧鑼暏楠炴儼绻戦崶?true閿涘牐顕氬▎鈥叉縺鐎?鑴?閿涘绱遍崥锕€鍨潻鏂挎礀 false閵?
	 * 閹劖鍎抽柌宥嗘煀閺傝棄濮為弮?FearState 闁插秴缂撻敍灞炬煀娑撯偓鏉烆喗浜归幆褔鍣搁弬鏉垮讲鐟欙箑褰傛稉鈧▎掳鈧?
	 */
	public static boolean tryConsumeDoubleDamage(UUID targetUuid, long now) {
		FearState v = FEARING.get(targetUuid);
		if (v == null || v.endTick <= now || v.doubleDamageUsed) return false;
		// 濮婏箓鐡忛幋鎺撳瘹閿涘牊鏌﹂崝鐘虫韫囶偆鍙庨敍澶涚窗閺堫剝鐤嗛幁鎰劤娑撳秵褰佹笟娑㈩浕濞嗏€叉縺鐎瑰磭鐐曢崐?
		if (!v.doubleDamageAllowed) return false;
		v.doubleDamageUsed = true;
		return true;
	}

	/** 鐠囧懎鎷绘稊瀣箑閸忛亶绂忛敍姘秼婢舵粍浜归幆?CD 閺€鍦暏 power JSON extra_cooldowns.cursed_moon閵嗗倷绮庨張宥呭缁旑垵鐨熼悽銊ｂ偓?*/
	private static void applyCursedMoonCooldown(ServerPlayer player) {
		if (net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isInCursedMoon(player.level())) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, SKILL_ID,
					github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.extra(player, SKILL_ID, "cursed_moon"));
		}
	}

	/** 閻╊喗鐖ｈぐ鎾冲閺勵垰鎯侀崗銉︹叺閸忓秶鏌呴敍鍫熶汗閹呯波閺夌喎鎮楅惃?20s 閹晝缍掔粣妤€褰涢敍澶堚偓?*/
	public static boolean isDreamImmune(UUID targetUuid, long now) {
		Long until = DREAM_IMMUNE.get(targetUuid);
		return until != null && until > now;
	}

	/**
	 * 娑撴槒顩﹂幎鈧懗濮愨偓灞句汗閹佲偓宥呭弳閸欙綇绱檖ower action 鐠嬪啰鏁ら敍灞肩矌閺堝秴濮熺粩顖ょ礆閵?
	 * 鐎佃澧嶉張澶婂嚒閸忋儲鈪甸惄顔界垼閺傝棄濮為幁鎰劤閿涙稒妫ら崗銉︹叺閻╊喗鐖ｉ弮鎯扮箲閸?false閿涘牅绗夋潻?CD閿涘瞼娅ч幓鎰仛閿涘鈧?
	 */
	public static boolean execute(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) return false;
		// CD 闂傘劎顩﹀鑼暠 power 鐏?begin 缂佺喍绔撮幏锔藉焻閿涘牅鎱?P1-1閿涙艾骞撻幒澶婂敶闁劋绨╁▎?canBegin 闂冭尪鍤滈柨渚婄礆

		long now = world.getGameTime();
		List<LivingEntity> targets = NightmareDreamManager.collectDreamTargets(player, now);
		if (targets.isEmpty()) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}

		for (LivingEntity target : targets) {
			startFear(world, player, target, now);
		}
		applyCursedMoonCooldown(player);
		return true;
	}

	/** 鐎电懓宕熼惄顔界垼閺傝棄濮為幁鎰劤閿涙氨濮搁幀浣稿弳鐞?+ 閸戝繘鈧?+ 閸?+ 闂婅櫕鏅ラ敍鍫ｇ箖濞撯槄绱伴崗鍫滅秵濞屽娓挎０銈夌叾妫板嫬鎲￠敍灞藉晙鐠у嘲绺剧捄绛圭礆閵?
	 * 濮婏箓鐡忛幋鎺撳瘹閿涘牊鏌﹂崝鐘电仜闂傛潙鎻╅悡褝绱氶敍姘瘮缂侇厽妞傞梹?+35%閿涙稑寮婚崐宥勬縺鐎硅櫕鏆ｆ潪顔绢洣閻劊鈧倽绻樼悰灞艰厬閻ㄥ嫭浜归幆褌绗夐崣妞捐厬闁梹鍩?閹芥ê濂栭崫宥冣偓?*/
	private static void startFear(ServerLevel world, ServerPlayer caster, LivingEntity target, long now) {
		UUID tid = target.getUUID();
		// 閹存帗瀵氳箛顐ゅ弾閿涙碍鏌﹂崝鐘电仜闂傛潙鍨界€规矮鍍甸幋瀵稿Ц閹緤绱濋崘娆忓弳閺堫剝鐤?FearState閿涘牊妞傞梹鍨杻楠?+ 閸欏苯鈧秳婵€鐎瑰磭顩﹂悽顭掔礆
		boolean ringWorn = github.com.gengyoubo.sscfa.item.NightmareRingItem.isWearingBy(caster);
		int baseDuration = BAL.i("duration_ticks", FEAR_DURATION_TICKS);
		int duration = ringWorn ? Math.round(baseDuration * (1.0f + FEAR_DURATION_RING_BONUS)) : baseDuration;
		FEARING.put(tid, new FearState(now + duration, caster.getUUID(), !ringWorn));
		// 閸忋儲鈪甸弮鍫曟？闁插秶鐤嗛崶?20s閿涘牐顫夐弽灏栨喗閿涙俺骞忓妤佷汗閹冨祮闁插秶鐤嗛敍?
		NightmareDreamManager.resetDream(caster.getUUID(), tid, now);
		// 閸戝繘鈧?20%閿涘牆绠撶粵澶涚窗閸忓牏些闂勩倕鍟€閸旂媴绱遍悽銊﹀煕鐎规氨顭堥敍姘箑婢跺洤鍣洪柅鐕傜礉閻溾晛顔?閻㈢喓澧挎稉鈧懛杈剧礆
		AttributeInstance attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr != null) {
			float slowRatio = (float) BAL.d("slow_ratio", FEAR_SLOW_RATIO);
			attr.removeModifier(FEAR_SLOW_UUID);
			attr.addPermanentModifier(new AttributeModifier(
					FEAR_SLOW_UUID, FEAR_SLOW_NAME, -slowRatio, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		// 鐎广垺鍩涚粩顖氬瘶閿涘牏鐭囬梿鐐窗閸?+ 韫囧啳鐑﹂崥顖氬З + 閺堫剙婀存径杈ㄦ妞瑰崬濮╅敍澶涚礉娴犲懐娲伴弽鍥ㄦ拱娴滅尨绱欓弮鍫曟毐闂呭繑鍨濋幐鍥ф彥閻撗冨綁閸栨牭绱?
		if (target instanceof ServerPlayer sp) {
			SscAddonNetworking.sendFearState(sp, duration);
		}
		// 閺傝棄濮炴潻鍥ㄦ诞闂婅櫕鏅ラ敍姘秵濞屽鈪垫晶鍐缚妫般倧绱欓惄顔界垼娴ｅ秶鐤嗛敍灞藉弿閸涙ê褰查梻璁崇稻闂婃娊鍣洪崗瀣煑閿? 閻╊喗鐖ｉ張顑挎眽閸氼剙鍩岀亸鏍ф毄閼版娊绂?
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
				net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.45f);
		if (target instanceof ServerPlayer sp) {
            MancianimaMarkManager.playSoundToPlayer(sp,net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE,0.7f,1.6f);
		}
		// 閻栧棗褰傜划鎺戠摍閿涙氨鐭囩槐顐ｂ叺婢у啫鐨归悳顖滅搏閻╊喗鐖ｆ稉鈧崷鍫礄娴犳牔姹夐崣顖濐潌閻ㄥ嫯顫嬬憴澶変繆閸欏嚖绱?
		world.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				24, 0.5, 0.6, 0.5, 0.03);
		world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				6, 0.3, 0.5, 0.3, 0.02);
	}

	/** 濮婏箓鐡熼悳鈺侇啀濮?tick 閹恒劏绻橀敍鍫㈡暠 SscAddonServerEvents 鐠嬪啰鏁ら敍澶堚偓?
	 * 娣囶喖顦查敍姘涧閹恒劏绻?<b>閺堫剚鈪垫鏃囧殰瀹歌鲸鏌﹂崝?/b> 閻ㄥ嫭浜归幆褎娼惄顕嗙礄濮濄倕澧犳禒缁樺壈閸︺劎鍤庨悳鈺侇啀 tick 闁粙浜堕崢鍡楀弿鐞涱煉绱?
	 * 婢舵矮姹夐崷銊у殠閺冨墎濮搁幀浣规簚闁插秴顦查幍褑顢戠€佃壈鍤ч梾鎰岸/閻滄澘鑸伴崠鍛鞍閻╁憡澧﹂弸?閳?16 閺嶇厧鍞撮崑璺哄絺缁愪胶鍔ч悳鏉胯埌閿涘鈧?*/
	public static void tick(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) return;
		long now = world.getGameTime();
		if (FEARING.isEmpty() && DREAM_IMMUNE.isEmpty()) return;
		boolean isCasterTick = NightmareDreamManager.isNightmare(player);

		Iterator<Map.Entry<UUID, FearState>> it = FEARING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, FearState> e = it.next();
			UUID tid = e.getKey();
			FearState v = e.getValue();
			// 娴犲懏鏌﹂幁鎰劤閻ㄥ嫭鈪垫鏃€婀版禍铏规畱 tick 閹恒劏绻樼拠銉︽蒋閻╊噯绱欓崢濠氬櫢閿?
			if (isCasterTick && !v.casterUuid.equals(player.getUUID())) continue;
			if (!isCasterTick && !tid.equals(player.getUUID())) continue;
			if (v.endTick <= now) {
				it.remove();
				endFear(world, tid, v.casterUuid, now, true);
				continue;
			}
			if (!(world.getEntity(tid) instanceof LivingEntity target) || !target.isAlive()) {
				it.remove();
				endFear(world, tid, v.casterUuid, now, false);
				continue;
			}
			// 鐟欏嫭鐗搁埗鈽呯窗閹劖鍎抽張鐔兼？閸忋儲鈪甸弮鍫曟？閹镐胶鐢婚柨浣哥暰閸?20s
			NightmareDreamManager.lockDream(v.casterUuid, tid, now);
			// 韫囧啳鐑︽竟甯礄鐟欏嫭鐗搁埗銈忕礆閿涙碍瀵滈張鈧潻鎴犳畱妞嬬喐鈪垫鏃囩獩缁傝濮╅幀渚€鐓堕柌蹇娾偓鏂衡偓鏃囧瘱閸ユ潙顦婚弸浣哥毈閸ュ搫鐣鹃棅鎶藉櫤閿?
			// 16 閺嶇厧鍞寸搾濠呯箮鐡掑﹤鎼烽敍鍫ョ叾闁?= 閸╄櫣顢?+ (1-鐠烘繄顬?16)鑴?閺堚偓婢?閸╄櫣顢?閿?
			if (tid.equals(player.getUUID()) && now % HEARTBEAT_INTERVAL == 0 && target instanceof ServerPlayer sp) {
				double sightRadius = BAL.d("sight_radius", FEAR_SIGHT_RADIUS);
				double nearest = Double.MAX_VALUE;
				for (ServerPlayer p : world.players()) {
					if (p.getUUID().equals(tid) || !NightmareDreamManager.isNightmare(p)) continue;
					nearest = Math.min(nearest, p.distanceToSqr(sp));
				}
				float vol = HEARTBEAT_BASE_VOL;
				if (nearest != Double.MAX_VALUE && nearest <= sightRadius * sightRadius) {
					double dist = Math.sqrt(nearest);
					vol = HEARTBEAT_BASE_VOL + (float) ((1.0 - dist / sightRadius) * (HEARTBEAT_MAX_VOL - HEARTBEAT_BASE_VOL));
				}
                MancianimaMarkManager.playSoundToPlayer(sp,net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,vol,0.9f);
			}
			// 閸欘垵顫嗛幀褏濮搁幀浣规簚閿涘牏娲伴弽鍥ㄦ拱娴滆櫣娈?tick 閹恒劏绻橀敍灞藉綗娑撯偓闁插秴骞撻柌宥忕幢婢舵矮姹夋径姘叺姒勬柨鎮囬懛顏嗗缁斿绱?
			if (tid.equals(player.getUUID()) && target instanceof ServerPlayer sp) {
				tickVisibility(world, sp, now);
			}
		}
		// 閸忓秶鏌呯悰銊︾閻?
		DREAM_IMMUNE.values().removeIf(until -> until <= now);
	}

	/** 閸欘垵顫嗛幀褏濮搁幀浣规簚閿涘牐顫夐弽灏栨喖閳垛檧鎲块敍澶涚窗
	 * <ul>
	 *   <li>16 閺嶈壈瀵栭崶鏉戭樆 閳?鐎瑰苯鍙忛梾鎰煩閿涘牆鎳嗛張鐔糕偓褔鍣搁崣鎴︽閸栧灝瀵樻穱婵囨た閿涘绱?/li>
	 *   <li>16 閺嶇厧鍞?閳?閼村鍟块敍姘舵 2s 閳?閻?0.6s 瀵邦亞骞嗛敍鍫ｇ箻閸忋儴瀵栭崶瀛樻娴犲酣娈ｉ崠璺ㄦ祲娴ｅ秴绱戞慨瀣剁礆閿?/li>
	 *   <li>閺€璇插毊 閳?閻?1.5s 楠炲爼鍣哥純顔炬祲娴ｅ稄绱檕nNightmareAttackFeared閿涘鈧?/li>
	 * </ul> */
	private static void tickVisibility(ServerLevel world, ServerPlayer target, long now) {
		for (ServerPlayer nightmare : world.players()) {
			if (nightmare.getUUID().equals(target.getUUID())) continue;
			if (!NightmareDreamManager.isNightmare(nightmare)) continue;
			String key = target.getUUID() + "|" + nightmare.getUUID();
			double sightRadius = BAL.d("sight_radius", FEAR_SIGHT_RADIUS);
			boolean inRange = nightmare.distanceToSqr(target) <= sightRadius * sightRadius;
			PairState ps = PAIRS.computeIfAbsent(key, k -> new PairState());
			if (!inRange) {
				// 鐟欏嫭鐗搁埗鐙呯窗閼煎啫娲挎径鏍х暚閸忋劑娈ｉ煬顐礄濮?2s 闁插秴褰傛稉鈧▎锟犳閸栧灝瀵樻穱婵囨た閿涘矂妲婚崠鍛丢婢跺崬顕遍懛瀵稿箛瑜邦澁绱?
				if (now - ps.lastHideSent >= OUT_OF_RANGE_HIDE_REFRESH) {
					ps.lastHideSent = now;
					ps.hidden = true;
					ps.phaseEnd = now + OUT_OF_RANGE_HIDE_REFRESH + 20;
					SscAddonNetworking.sendFearHide(target, nightmare.getUUID(), OUT_OF_RANGE_HIDE_REFRESH + 20);
				}
				continue;
			}
			// 閼煎啫娲块崘鍜冪窗閼村鍟块惄闀愮秴閹恒劏绻橀敍鍧aseEnd 閸掔増婀￠崚鍥ㄥ床閿?
			if (now >= ps.phaseEnd) {
				ps.hidden = !ps.hidden;
				ps.phaseEnd = now + (ps.hidden ? PULSE_HIDDEN_TICKS : PULSE_VISIBLE_TICKS);
				if (ps.hidden) {
					SscAddonNetworking.sendFearHide(target, nightmare.getUUID(), PULSE_HIDDEN_TICKS);
				} else {
					SscAddonNetworking.sendFearReveal(target, nightmare.getUUID(), PULSE_VISIBLE_TICKS);
				}
			}
		}
	}

	/** 閹劖鍎崇紒鎾存将閿涙氨些闂勩倕鍣洪柅鐔粹偓浣稿絺缂佹挻娼崠鍛偓浣稿繁閸掕泛鍤?+ 閸忓秶鏌呴妴淇沺plyImmune=false 閻劋绨惄顔界垼濮濊楠搁崷鐑樻珯閵?*/
	private static void endFear(ServerLevel world, UUID tid, UUID casterUuid, long now, boolean applyImmune) {
		if (world.getEntity(tid) instanceof LivingEntity target) {
			AttributeInstance attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (attr != null) attr.removeModifier(FEAR_SLOW_UUID);
			if (target instanceof ServerPlayer sp) {
				SscAddonNetworking.sendFearState(sp, 0);
			}
		}
		clearPairs(tid);
		// 瀵搫鍩楅崙鐑樷叺閿涘牏鏁遍弬鑺ヤ汗閹呮畱濮婏箓鐡熼惃鍕弳濮婏箒銆冪粔濠氭珟鐠囥儳娲伴弽鍥风幢閸忚泛鐣犲锕傜摕閼汇儰绡冮崗銉︹叺娴滃棗鐣犻崚娆庣楠炶埖绔婚垾鏂衡偓鏃€浜归幆褏绮ㄩ弶鐔峰繁閸掕泛鍙忛崙鐚寸礆
		NightmareDreamManager.forceWakeAll(tid, now, world);
		if (applyImmune) {
			DREAM_IMMUNE.put(tid, now + BAL.i("dream_immune_ticks", DREAM_IMMUNE_TICKS));
		}
	}

	/** 濮婏箓鐡熼弬顓犲殠/濮濊楠?婢跺崬鑸板〒鍛倞閿涙艾鍙鹃弬钘夊閻ㄥ嫭浜归幆褌绔撮獮鍓佺波閺夌噦绱欓惄顔界垼閹垹顦?+ 閸忓秶鏌呴悡褍鐖堕敍澶堚偓?*/
	public static void onCasterRemoved(ServerPlayer caster) {
		if (!(caster.level() instanceof ServerLevel world)) return;
		long now = world.getGameTime();
		Iterator<Map.Entry<UUID, FearState>> it = FEARING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, FearState> e = it.next();
			if (e.getValue().casterUuid.equals(caster.getUUID())) {
				it.remove();
				endFear(world, e.getKey(), caster.getUUID(), now, true);
			}
		}
	}

	/** 閹劖鍎抽悩鑸碘偓渚婄礄閸掔増婀?+ 閺傝姤浜归幆褏娈戝锕傜摕 + 閸欏苯鈧秳婵€鐎瑰啿绱戦崗?瀹歌尙鏁ら弽鍥唶閿涘鈧?*/
	private static final class FearState {
		final long endTick;
		final UUID casterUuid;
		/** 濮婏箓鐡忛幋鎺撳瘹韫囶偆鍙庨敍姘拱鏉烆喗浜归幆褎妲搁崥锔跨箽閻ｆ瑥寮婚崐宥勬縺鐎圭绱欓幋瀛樺灊閹?閳?false閿涘鈧?*/
		final boolean doubleDamageAllowed;
		/** 閺堫剝鐤嗛幁鎰劤閻ㄥ嫬寮婚崐宥勬縺鐎硅櫕妲搁崥锕€鍑″☉鍫ｂ偓妤嬬礄閺佺鐤嗛崣顏囆曢崣鎴滅濞嗏槄绱氶妴?*/
		boolean doubleDamageUsed;
		FearState(long endTick, UUID casterUuid, boolean doubleDamageAllowed) {
			this.endTick = endTick;
			this.casterUuid = casterUuid;
			this.doubleDamageAllowed = doubleDamageAllowed;
		}
	}

	// ===== 闂堢偟甯虹€瑰墎娲伴弽鍥风窗娴犲洦浠洪崢瀣煑閿涘牏鏁ら幋宄扮暰缁嬪尅绱?=====

	/**
	 * 閹劖鍎虫稉顓犳畱闂堢偟甯虹€瑰墎鏁撻悧鈺傛Ц閸氾箑鍘戠拋绋跨殺 target 鐠佸彞璐熼弨璇插毊閻╊喗鐖ｉ敍鍫㈡暏閹村嘲鐣剧粙鍖＄窗閸欘亜顕柌濠冩杹閻溾晛顔嶆稉宥呭讲鐟欏棴绱氶妴?
	 * <p>閺堝搫鍩楅敍姘汗閹勬埂闂?mob <b>娴犲懎顕柌濠冩杹鐠囥儲浜归幆褏娈戝锕傜搹閺堫兛姹?/b>婢跺崬骞撴禒鍥ㄤ缓閿涘牊瀚?setTarget閿涘绱?
	 * 鐞氼偅鈪垫鏃€鏁鹃崙璇叉倵娴犲懏婀?{@link #MOB_AGGRO_WINDOW_TICKS}閿? 缁夋帪绱氶崣宥呭毊缁愭褰涢敍?
	 * 缁愭褰涙潻鍥ф倵閿涘潤astAttackedTime 鐡掑懏妞傞敍澶夌矏閹劍绉锋径鎲嬬礄mobTick 濞撳懐娲伴弽鍥风礆閵?
	 * 鐎电懓鍙剧€瑰啫鐤勬担鎿勭礄閸忚泛鐣犻悳鈺侇啀閵嗕礁鍙剧€瑰啯鈪垫鍥モ偓浣烘晸閻椻晪绱氭稉鈧崚鍥劀鐢潻绱濇稉宥呭綀閹劖鍎宠ぐ鍗炴惙閵?/p>
	 */
	public static boolean isMobAggroAllowed(Mob mob, LivingEntity target) {
		long now = mob.level().getGameTime();
		FearState v = FEARING.get(mob.getUUID());
		if (v == null || v.endTick <= now) return true; // 闂堢偞浜归幆?mob 娑撳秴褰堥幒?
		// 娴犲懎缍嬮惄顔界垼濮濓絾妲搁柌濠冩杹鐠囥儲浜归幆褏娈戝锕傜搹閺堫兛姹夐弮鑸靛閸樺鍩楁禒鍥ㄤ缓
		if (!(target instanceof ServerPlayer tp) || !tp.getUUID().equals(v.casterUuid)) return true;
		long lastHit = mob.getLastHurtByMobTimestamp(); // vanilla閿涙艾褰堥崙缁樺灇閸旂喐妞傞弴瀛樻煀
		return now - lastHit <= BAL.i("mob_aggro_window_ticks", MOB_AGGRO_WINDOW_TICKS); // 娴犲懓顫﹂崙?2 缁夋帒鍞撮崣顖氬冀閸?
	}

	/** 閹劖鍎虫稉顓犳畱 mob 濮ｅ繐鍩㈠〒鍛存珟鐎靛箍鈧本鏌﹀▔鏇熲叺姒勫洢鈧秶娈戞潻鍥ㄦ埂娴犲洦浠洪敍鍫㈡暠 MobEntityMixin.mobTick 鐠嬪啰鏁ら敍灞肩矌閺堝秴濮熺粩顖ょ礆閵?*/
	public static void tickFearedMobAggro(Mob mob) {
		long now = mob.level().getGameTime();
		FearState v = FEARING.get(mob.getUUID());
		if (v == null || v.endTick <= now) return;
		LivingEntity cur = mob.getTarget();
		if (cur == null) return;
		if (!isMobAggroAllowed(mob, cur)) {
			mob.setTarget(null); // 娴犲洦浠哄☉鍫濄亼閿涘牊鏂佺悰宀嬬窗setTarget(null) 娑撳秴褰堥幏锔藉焻閿?
		}
	}

	/**
	 * 閸欏苯鈧秳婵€鐎瑰啿褰堥惄濠呪偓鍛⒖鐏炴洩绱欓悽銊﹀煕鐎规氨顭堥敍澶涚窗閹劖鍎抽惄顔界垼閸欐ぜ鈧奔鎹㈡担鏇㈩棨濮婏箓鐡?b>閸欏﹤鍙鹃惂钘夋倳閸楁洘鍨氶崨?/b>閵嗗秶娈戞＃鏍偧娴笺倕顔?鑴?閵?
	 * 閻ц棄鎮曢崡鏇熷灇閸?= 閸︺劎鍤庨幁鎰劤濮婏箓鐡熼惃鍕閸氬秴宕熼崣瀣晽閿涘湹hitelistUtils.isBuffTarget 瀵搫瀵茬猾鑽ゆ窗閺嶅洤鍨界€规熬绱氶妴?
	 */
	public static boolean isDoubleDamageBeneficiary(LivingEntity attacker, ServerLevel world) {
		if (!(attacker instanceof ServerPlayer sp)) return false;
		if (NightmareDreamManager.isNightmare(sp)) return true;
		// 闂堢偞鈪垫鏃傚负鐎硅绱伴弰顖欐崲娑撯偓閸︺劎鍤庢鐔糕叺姒勬梻娈戦惂钘夋倳閸楁洖寮搁崘?閳?閸欐娉?
		for (ServerPlayer p : world.players()) {
			if (!NightmareDreamManager.isNightmare(p)) continue;
			if (github.com.gengyoubo.sscfa.util.WhitelistUtils.isBuffTarget(p, sp)) {
				return true;
			}
		}
		return false;
	}

	/** 閻╊喗鐖ｉ弬顓犲殠濞撳懐鎮婇妴?*/
	public static void onTargetDisconnect(UUID targetUuid) {
		FEARING.remove(targetUuid);
		DREAM_IMMUNE.remove(targetUuid);
		clearPairs(targetUuid);
	}

	/** 瑜版挸澧犻幁鎰劤閻╊喗鐖ｉ弫甯礄鐠嬪啳鐦悽顭掔礆閵?*/
	public static int size() {
		return FEARING.size();
	}
    public static void pruneImmunity(long now) {
        DREAM_IMMUNE.values().removeIf(expiry -> expiry <= now);
    }
    public static void clearAll() {
        FEARING.clear(); DREAM_IMMUNE.clear(); PAIRS.clear();
    }

}
