package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.spell.DomainManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 妞嬬喐鈪垫鏃撶礄Nightmare閿涘鈧苯鍙嗗锔衡偓宥嗙壋韫囧啰濮搁幀浣规簚 閳ユ柡鈧?閺堝秴濮熺粩顖涙綀婵炰降鈧?
 *
 * <p>閺堝搫鍩楅敍姘额棨濮婏箓鐡熼悳鈺侇啀鐎靛湱娲伴弽鍥偓鐘冲灇<b>缁鳖垵顓?10 閻?/b>娴笺倕顔婇崥搴礄鐠囧懎鎷绘稊瀣箑閸忛亶绂忛敍姘秼婢舵粓妾锋稉?6 閻愮櫢绱氶敍宀€娲伴弽鍥箻閸忋儯鈧苯鍙嗗锔衡偓宥囧Ц閹降鈧?
 * 閸忋儲鈪甸張鐔兼？閸愬秵顐肩悮顐ヮ嚉妞嬬喐鈪垫鏃€鏁鹃崙?閳?鐠佲剝鏆熷〒鍛存祩闁插秶鐣?+ 閸ｎ剚鈪甸崥姝岊攨閿?5% 鏉烆剙瀵查崶鐐额攨閿涙稓娲伴弽鍥劥娴滐繝顤傛径鏍ф礀 4 閻愮櫢绱氶妴?
 * 閸忋儲鈪垫稉顓犳畱閺佸本鏌熺€靛綊顥ゅ锕傜摕閺堫兛姹夐弬钘夊閻ㄥ嫪绔撮崚?debuff閿涘牐绀嬮棃銏㈠Ц閹焦鏅ラ弸婊愮礆閸忋劑鍎撮弮鐘虫櫏閳ユ柡鈧柨鎯堢€规俺闊╅敍鍧皊c_addon:stun閿涘鈧?
 * 娑撹濮?鐞氼偄濮╂妯哄帨閹诲繗绔熼敍鍦檒owMarker / 閾旀稓缍夋妯瑰瘨 / 娓氫絻娈岄悜娆忓祪 / 婵傛垹浼掗弽鍥唶缁涘鐢┃鎰煢閸旂姷鍋ｉ柈鎴掔窗鐞?
 * {@code NightmareDreamGuard.isBlocked} 閹凤附鍩呴敍澶堚偓?
 * 妞嬬喐鈪垫鏃€婀版禍铏规箙閸忋儲鈪甸惄顔界垼閺?b>缁瀛╅懝鍙夊伎鏉?/b>閿涘牆顦查悽?WebHighlight 鐎广垺鍩涚粩顖涘伎鏉堝綊鈧岸浜鹃敍灞肩矌閺堫兛姹夐崣顖濐潌閿涘鈧?/p>
 *
 * <p>閺佺増宓佺紒鎾寸€敍姘樆鐏?key = 妞嬬喐鈪垫鏃傚负鐎?UUID閿涙稑鍞寸仦?key = 閸忋儲鈪甸惄顔界垼 UUID閵?
 * 閺堝秴濮熺粩顖涚槨 tick閿涘澖@link #tick}閿涘甯规潻娑樺煂閺堢喐绔婚悶鍡楄嫙楠炴寧鎸遍敍娑橆吂閹撮顏崣顏呮暪閸氬本顒為崠鍛樊閹躲倖寮挎潏骞库偓?
 * 濮濊楠?/ 瑜般垺鈧椒娑径?/ 閺傤厾鍤?閳?濞撳懐鎮婄拠銉у负鐎瑰墎娈戦崗銊╁劥閸忋儲鈪甸悩鑸碘偓浣碘偓?/p>
 */
public final class NightmareDreamManager {

	// ==== 娴犮儰绗呯敮鎼佸櫤娑撴椽绮拋銈呪偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘澃cope: abilities.nightmare_dream閿涘本鏆熼幑顔煎瘶閸欘垵顩惄鏍电礆====
	/** 鐟欙箑褰傞崗銉︹叺閹碘偓闂団偓缁鳖垵顓告导銈咁唺閿涘牊鐦″▎鈥冲弳濮婏箑鎮楀〒鍛存祩闁插秶鐣婚敍澶堚偓鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final float DREAM_THRESHOLD = 10.0f;
	/** 鐠囧懎鎷绘稊瀣箑閸忛亶绂忛敍姘崇槒閸滄帊绠ｉ張鍫濈秼婢舵粌鍙嗗锕傛閸婂ジ妾锋稉?6 閻愮櫢绱欏锕傜搹娑斿濮忔妯诲畾閿涘鈧倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final float DREAM_THRESHOLD_CURSED_MOON = 6.0f;
	/** 閸ｎ剚鈪电悮顐㈠З閿涙艾顕鎻掑弳濮婏妇娲伴弽鍥偓鐘冲灇娴笺倕顔婇惃鍕儧鐞涒偓濮ｆ柧绶ラ敍鍫熷瘻闂堛垺婢樻导銈咁唺鐠佲槄绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final float DREAM_LIFESTEAL_RATIO = 0.15f;
	/** 閸ｎ剚鈪电悮顐㈠З閿涙艾鍙嗗锔炬窗閺嶅洦顒存禍鈩冩妫版繂顦婚崶鐐差槻閻ㄥ嫮鏁撻崨钘夆偓纭风礄2 妫版绺鹃敍澶堚偓鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final float DREAM_KILL_HEAL = 4.0f;

	/** 閺堝秴濮熺粩顖涙綀婵?balance 韫囶偆鍙庣拠璇插絿閿涘牆鎻╅悡褎婀崚婵嗩潗閸栨牗妞傞崶鐐衡偓鈧稉濠冩煙姒涙顓荤敮鎼佸櫤閿涘鈧?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.nightmare_dream");

	/** 瑜版挸澧犻悽鐔告櫏閻ㄥ嫬鍙嗗锕傛閸婄》绱扮拠鍛嫽娑斿婀€瑜版挸顧侀敍鍫濄亯姒?+ 鐠囧懎鎷绘稊瀣箑閺冦儻绱氶梽宥勮礋 6閿涘苯鎯侀崚?10閵嗗倷绮庨張宥呭缁旑垵鐨熼悽銊ｂ偓?*/
	public static float currentDreamThreshold(ServerPlayer player) {
		if (net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isInCursedMoon(player.level())) {
			return (float) BAL.d("threshold_cursed_moon", DREAM_THRESHOLD_CURSED_MOON);
		}
		return (float) BAL.d("threshold", DREAM_THRESHOLD);
	}
	/** 閸忋儲鈪甸幐浣虹敾 tick閿?0 缁夋帪绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int DREAM_DURATION_TICKS = 400;
	/** 閸欐鍤憴锕€褰傞崗銉︹叺閻ㄥ嫬娴愮€规碍妞傞梹鍖＄礄tick閿?0 缁夋帪绱濇稉宥夋閸欐鍤崚閿嬫煀閿涙稒浜归幆褔鍣哥純顔芥閸ョ偛鍩?20 缁夋帡鏀ｇ€规熬绱氶妴鍌炵帛鐠併倧绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閵?*/
	public static final int DREAM_FIXED_TICKS = 200;

	/** 濮婏箑顣ㄧ划鎺戠摍閸欐垵鐨犻梻鎾閿涘澅ick閿?.75 缁夋帊绔村▎鈽呯礉"閸嬭泛鐨甸崘?閻ㄥ嫯濡總蹇ョ礆閵嗗倹婀惂鏄忣唶 balance閿涘奔绻氶幐浣哥埗闁插繈鈧?*/
	public static final int DREAM_PARTICLE_INTERVAL = 15;

	/** 閸忋儲鈪甸幓蹇氱珶閸氬本顒為梻鎾閿涘澅ick閿? 缁夋帊绔村▎鈽呯幢閹跺﹦婀＄€圭偛澧挎担娆愭闂€鍨煕缂佹瑩顥ゅ锕傜摕鐎广垺鍩涚粩顖ょ礉娣囶喖顦查幓蹇氱珶娑撳骸鐤勯梽鍛弳濮婏附妞傞梹鑳姎閼哄偊绱氶妴?*/
	public static final int DREAM_OUTLINE_SYNC_INTERVAL = 20;

	/** 缁瀛╅幓蹇氱珶妫版粏澹婇敍鍦B閿涘奔绮庢鐔糕叺姒勬梹婀版禍鍝勫讲鐟欎緤绱氶妴?*/
	public static final int DREAM_OUTLINE_COLOR = 0xFF6EC7;

	/** 閻溾晛顔?UUID -> (閻╊喗鐖?UUID -> 閸忋儲鈪甸崚鐗堟埂娑撴牜鏅弮鍫曟？)閵?*/
	private static final Map<UUID, Map<UUID, Long>> DREAMING = new ConcurrentHashMap<>();
	/** 閻溾晛顔?UUID -> (閻╊喗鐖?UUID -> 缁鳖垵顓告导銈咁唺)閵?*/
	private static final Map<UUID, Map<UUID, Float>> ACCUM = new ConcurrentHashMap<>();

	private NightmareDreamManager() {
	}

	/** 鐠囥儳甯虹€硅埖妲搁崥锔胯礋妞嬬喐鈪垫鏂胯埌閹緤绱欓張宥呭缁旑垽绱氶妴?*/
	public static boolean isNightmare(ServerPlayer player) {
		return FormUtils.isForm(player, github.com.gengyoubo.sscfa.util.FormIdentifiers.WILD_CAT_NIGHTMARE);
	}

	/**
	 * 妞嬬喐鈪垫鏂款嚠閻╊喗鐖ｉ柅鐘冲灇娴笺倕顔婇崥搴ｆ畱缁鳖垵顓搁崗銉ュ經閿涘牏鏁?LivingEntityMixin damage RETURN 鐠嬪啰鏁ら敍灞肩矌閺堝秴濮熺粩顖ょ礆閵?
	 * 缁鳖垵顓告潏楣冩閸?閳?閻╊喗鐖ｉ崗銉︹叺閿涘牐顓搁弮?20s閿涘绱卞鎻掑弳濮婏箑鍟€鐞氼偅澧?閳?閸掗攱鏌婇弮鍫曟毐 + 鐠佲剝鏆熷〒鍛存祩闁插秶鐣婚妴?
	 */
	public static void onNightmareDealtDamage(ServerPlayer player, LivingEntity target, float amount) {
		if (amount <= 0) return;
		UUID pid = player.getUUID();
		UUID tid = target.getUUID();

		Map<UUID, Long> dreams = DREAMING.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
		Map<UUID, Float> acc0 = ACCUM.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
		long now = player.level().getGameTime();

		// 瀹告彃鍙嗗锔肩窗閸ｎ剚鈪甸崥姝岊攨閿?5% 鏉烆剙瀵查崶鐐额攨閿涘本瀵滈棃銏℃緲娴笺倕顔婄拋鈽呯礆+ 閻╊喗鐖ｅ璁抽妫版繂顦婚崶?2 韫囧喛绱辨稉宥呭晙閸掗攱鏌婇弮鍫曟毐
		// 閿涘牏鏁ら幋宄扮暰缁嬪尅绱伴崣妤€鍤崗銉︹叺閸ュ搫鐣?10 缁夋帒鍩岄張鐕傜幢閹劖鍎虫禒宥呭讲闁插秶鐤嗛獮鍫曟敚鐎?20 缁夋帪绱?
		Long until = dreams.get(tid);
		if (until != null && until > now) {
			acc0.remove(tid); // 鐠佲剝鏆熷〒鍛存祩闁插秶鐣?
			player.heal(amount * (float) BAL.d("lifesteal_ratio", DREAM_LIFESTEAL_RATIO)); // 閸ｎ剚鈪甸敍姘緢閸欐牗鈪垫晶鍐х閸旀稑娲栫悰鈧?
			if (!target.isAlive()) {
				player.heal((float) BAL.d("kill_heal", DREAM_KILL_HEAL)); // 濮婏箑鏁栨禍杞伴閿涙岸顤傛径鏍ф偠閸ｎ剚鐣?
			}
			SscAddonNetworking.sendWebHighlight(player, target.getId(),
					(int) Math.max(20, until - now), DREAM_OUTLINE_COLOR); // 閹诲繗绔熼崜鈺€缍戦弮鍫曟毐閸氬本顒為敍鍫滅瑝瀵ゅ爼鏆遍敍?
			return;
		}

		// 閺堫亜鍙嗗锔肩窗缁鳖垵顓告导銈咁唺閿涘矁鎻梼鍫濃偓鑹靶曢崣鎴濆弳濮婏讣绱欓幁鎰劤缂佹挻娼崥搴ｆ畱 20s 閸忓秶鏌呯粣妤€褰涢崘鍛瑝閸愬秷袝閸欐埊绱?
		// 鐠囧懎鎷绘稊瀣箑閸忛亶绂忛敍姘秼婢舵粓妲囬崐濂告娑?6 閻愮櫢绱?
		Map<UUID, Float> acc = acc0;
		// 閸忓秶鏌呴惇鐔衡敄閺堢喎鍞存导銈咁唺娑撳秶鐤拋鈽呯礄娣囶喖顦查敍姘帳閻ゎ偅婀℃稉鈧潻鍥︾瑓娑撯偓閸戣崵鐝涢崡鎶藉櫢閺傛澘鍙嗗锔肩礉閻喓鈹栭張鐔疯埌閸氬矁娅勭拋鎾呯礆
		if (NightmareFearManager.isDreamImmune(tid, now)) {
			acc.remove(tid); // 濞撳懏甯€閸忓秶鏌呴張鐔峰缁鳖垳袧閻ㄥ嫯顓搁弫甯礉閻喓鈹栭張鐔虹波閺夌喎鎮楁禒搴ㄦ祩闁插秵鏌婄槐顖濐吀
			return;
		}
		float total = acc.merge(tid, amount, Float::sum);
		if (total >= currentDreamThreshold(player)
				&& !NightmareFearManager.isDreamImmune(tid, now)) {
			acc.remove(tid);
			// 閸欐鍤憴锕€褰傞崗銉︹叺閿涙艾娴愮€?10 缁夋帪绱欓悽銊﹀煕鐎规氨顭堥敍娑欎汗閹囧櫢缂冾喗妞傞幍宥呮礀閸?20 缁夋帡鏀ｇ€规熬绱?
			int fixedTicks = BAL.i("fixed_ticks", DREAM_FIXED_TICKS);
			dreams.put(tid, now + fixedTicks);
			SscAddonNetworking.sendWebHighlight(player, target.getId(), fixedTicks, DREAM_OUTLINE_COLOR);
			sendVeilToTarget(player, target, fixedTicks);
			// 閸忋儲鈪甸棅铏櫏閿涙矮缍嗗▽澶嬧叺婢у啴鎸撴竟甯礄閸忋劌鎲抽崣顖炴 null閿? 濮婏箓鐡忔担搴ゎ嚔
			net.minecraft.server.level.ServerLevel sw = (net.minecraft.server.level.ServerLevel) player.level();
			sw.playSound(null, target.getX(), target.getY(), target.getZ(),
					net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.6f);
			sw.playSound(null, target.getX(), target.getY(), target.getZ(),
					net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
			// 缁帒鐡欓敍姘变紶閼瑰弶鈪垫晶鍐碃闂嗗墽骞嗙紒鏇犳窗閺?
			sw.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
					target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ(), 16, 0.4, 0.4, 0.4, 0.02);
		}
	}

	/**
	 * 閹凤附鍩呴崚銈呯暰閿涘牅绮庨張宥呭缁旑垽绱氶敍姝磑urce 鐎圭偘缍嬮敍鍧塭buff 閺傝棄濮為懓鍜冪礆閺勵垰鎯佹径鍕艾閵嗗矁顫?target 閻溾晛顔嶉崗銉︹叺閵嗗秶濮搁幀浣碘偓?
	 * 閸楃绱伴弬钘夊閼板懎鍑￠崗銉︹叺 娑?閸欐ぞ缍嬮弰顖涘Ω鐎瑰啯澧﹂崗銉︹叺閻ㄥ嫰顥ゅ锕傜摕閺堫兛姹?閳?鐠?debuff 閺冪姵鏅ラ妴?
	 */
	public static boolean isBlocked(LivingEntity source, LivingEntity target) {
		if (!(target instanceof ServerPlayer player)) return false;
		Map<UUID, Long> dreams = DREAMING.get(player.getUUID());
		if (dreams == null) return false;
		Long until = dreams.get(source.getUUID());
		return until != null && until > player.level().getGameTime();
	}

	/** 妞嬬喐鈪垫鏃傚负鐎硅埖鐦?tick 閹恒劏绻橀敍鍫㈡暠 SscAddonServerEvents 鐠嬪啰鏁ら敍澶涚窗閸掔増婀￠崙鐑樷叺濞撳懐鎮?+ 濮婏箑顣ㄧ划鎺戠摍閵?*/
	public static void tick(ServerPlayer player) {
		Map<UUID, Long> dreams = DREAMING.get(player.getUUID());
		if (dreams == null || dreams.isEmpty()) return;
		if (!isNightmare(player)) {
			// 瑜般垺鈧椒娑径鎲嬬窗濞撳懎鍙忛柈?
			clearAllFor(player);
			return;
		}
		net.minecraft.server.level.ServerLevel sw = (net.minecraft.server.level.ServerLevel) player.level();
		long now = sw.getGameTime();
		dreams.values().removeIf(until -> until <= now);
		// 濮婏箑顣ㄧ划鎺戠摍閿涙艾鍙嗗锔炬窗閺嶅洩闊╂稉濠備紦鐏忔柨鍟嬬划澶庡鐏忔鐭戦敍鍫滅瑢閹诲繗绔熼崥宀冨 0xFF6EC7閿涘绱遍惄顔界垼閺堫兛姹夋稉宥嗘暪閸?閳?缁楊兛绔存禍铏剐炴稉宥呭讲鐟?
		if (now % DREAM_PARTICLE_INTERVAL == 0) {
			for (Map.Entry<UUID, Long> e : dreams.entrySet()) {
				if (e.getValue() <= now) continue;
				if (!(sw.getEntity(e.getKey()) instanceof LivingEntity target) || !target.isAlive()) continue;
				spawnDreamParticles(sw, target);
			}
		}
		// 閹诲繗绔熼崨銊︽埂閸氬本顒為敍鍫熺槨 1s閿涘绱伴幎濠冪槨娑擃亜鍙嗗锔炬窗閺嶅洨娈戦妴宀€婀＄€圭偛澧挎担娆愭闂€瑁も偓宥呭煕缂佹瑩顥ゅ锕傜摕鐎广垺鍩涚粩顖樷偓?
		// 娣囶喖顦查敍姘额浕濞嗏€冲弳濮婏附寮挎潏鐟板涧閸欐垳绔村▎?200t 閸栧拑绱濋懟銉︿汗閹勫Ω閸忋儲鈪甸柨浣哥暰 20s/閹存帗瀵氬鍫曟毐 20.25s閿?
		// 鐎广垺鍩涚粩顖涘伎鏉堥€涚窗閸忓牅绨€圭偤妾崗銉︹叺缂佹挻娼懓宀€鍞查悘顓涒偓鏂衡偓鏂挎噯閺堢喎鍩涢弬鎷岊唨閹诲繗绔熸稉搴ｆ埂鐎圭偟濮搁幀浣割潗缂佸牅绔撮懛娣偓?
		if (now % DREAM_OUTLINE_SYNC_INTERVAL == 0) {
			for (Map.Entry<UUID, Long> e : dreams.entrySet()) {
				long remain = e.getValue() - now;
				if (remain <= 0) continue;
				if (!(sw.getEntity(e.getKey()) instanceof LivingEntity target) || !target.isAlive()) continue;
				SscAddonNetworking.sendWebHighlight(player, target.getId(), (int) remain, DREAM_OUTLINE_COLOR);
			}
		}
	}

	/** 閸忋儲鈪甸惄顔界垼閸涖劌娲块惃鍕焽閼瑰弶鈪垫晶鍐煈鐎涙劧绱伴柅鎰潎鐎电喕鈧懎褰傞崠鍜冪礄閹烘帡娅庨惄顔界垼閺堫兛姹?+ 32 閺嶈壈绐涚粋璇插ⅶ闂勩倧绱氶妴?*/
	private static void spawnDreamParticles(net.minecraft.server.level.ServerLevel world, LivingEntity target) {
		net.minecraft.world.phys.Vec3 pos = target.position();
		// 缁?0xFF6EC7 閳?(1.0, 0.4314, 0.7804)閿涙稑浼撻崣?1~2 缁帇鈧胶绱﹂幈顫瑐妞?
		net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket pkt = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
				new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0f, 0.4314f, 0.7804f), 1.0f),
				false,
				pos.x, pos.y + target.getBbHeight() * 0.6, pos.z,
				0.35f, 0.4f, 0.35f, 0.01f, 1);
		UUID targetUuid = target.getUUID();
		for (net.minecraft.server.level.ServerPlayer viewer : world.players()) {
			if (viewer.getUUID().equals(targetUuid)) continue; // 閻╊喗鐖ｉ張顑挎眽娑撳秴褰茬憴渚婄礄缁楊兛绔存禍铏剐炴稉宥呭晪缁帒鐡欓幐陇顫嬬痪鍖＄礆
			if (viewer.distanceToSqr(target) > 32 * 32) continue;
			viewer.connection.send(pkt);
		}
	}

	/** 閺傤厾鍤?/ 濮濊楠?/ 瑜般垺鈧椒娑径鎲嬬窗濞撳懐鎮婄拠銉у负鐎硅泛鍙忛柈銊ュ弳濮婏妇濮搁幀渚婄礄娑撳秴濮╅崗鏈电铂妞嬬喐鈪垫鏃傛畱閻樿埖鈧緤绱氶妴?*/
	public static void clearAllFor(ServerPlayer player) {
		Map<UUID, Long> dreams = DREAMING.remove(player.getUUID());
		ACCUM.remove(player.getUUID());
		// 閹绘劕澧犻崙鐑樷叺閿涙艾鎮滈幍鈧張澶夌矝閸︺劌鍙嗗锔炬畱閻╊喗鐖ｉ崣鎴濆彠缁崵绮ㄩ弶鐔峰瘶閿涘牏鐭囬懝鍙夋瑜拌鲸绉锋径?+ 鐎广垺鍩涚粩顖溞╅梽銈呭弳濮婏箑鍙х化浼欑礆閿涙稖鍤滈悞璺哄煂閺堢喓鏁辩€广垺鍩涚粩顖濆殰鐞涘本璐伴崙?
		if (dreams != null && player.getServer() != null) {
			for (UUID tid : dreams.keySet()) {
				if (player.getUUID().equals(tid)) continue;
				net.minecraft.server.level.ServerPlayer target =
						player.getServer().getPlayerList().getPlayer(tid);
				if (target != null) {
					SscAddonNetworking.sendDreamVeil(target, 0, player.getUUID());
				}
			}
		}
	}

	// ===== 鐎广垺鍩涚粩顖炴殔閸嶅骏绱欐禒鍛吂閹撮顏担璺ㄦ暏閿涙稒婀囬崝锛勵伂濮樻瓕绻欐稉铏光敄閿?=====
	/** 鐎广垺鍩涚粩顖ょ窗閵嗗本濡搁幋鎴礄閺堫剙婀撮悳鈺侇啀閿涘鍙嗗锔炬畱妞嬬喐鈪垫?UUID -> 閸掔増婀＄€广垺鍩涚粩顖涙闂傛番鈧秲鈧倷绶甸柅蹇氼潒閹凤附鍩呴弻銉嚄閵?*/
	private static final Map<UUID, Long> CLIENT_DREAMED_BY = new ConcurrentHashMap<>();

	/** 鐎广垺鍩涚粩顖涙暪閸掔増妾胯ぐ鍗炲瘶閺冨墎娣幎銈夋殔閸嶅骏绱檇reamVeilRenderer 鐠嬪啰鏁ら敍澶堚偓?*/
	public static void clientUpdateDreamedBy(UUID nightmareUuid, long endClientTime) {
		if (nightmareUuid == null) return;
		if (endClientTime <= 0) {
			CLIENT_DREAMED_BY.remove(nightmareUuid);
		} else {
			CLIENT_DREAMED_BY.put(nightmareUuid, endClientTime);
		}
	}

	/** 鐎广垺鍩涚粩顖涚叀鐠囶澁绱扮拠銉ョ杽娴ｆ搫绱欓柅蹇氼潒 power 閹镐焦婀侀懓鍜冪礆閺勵垰鎯佸锝咁槱娴滃簺鈧本濡搁幋鎴濆弳濮婏负鈧秶濮搁幀渚婄礄娓?EntityGlowPower 閹凤附鍩呴敍澶堚偓?
	 *  閺冪姳绗橀悾灞炬闂傜繝绗傛稉瀣瀮閺冨爼鈧偓閸栨牔璐熺€涙ê婀幀褍鍨介弬顓溾偓?*/
	public static boolean clientIsDreamingMe(UUID holderUuid, long nowClientTime) {
		if (holderUuid == null) return false;
		Long end = CLIENT_DREAMED_BY.get(holderUuid);
		if (end == null) return false;
		return nowClientTime <= 0 || end > nowClientTime;
	}

	/** 鐎广垺鍩涚粩顖涚叀鐠囶澁绱伴張顒€婀撮悳鈺侇啀瑜版挸澧犻弰顖氭儊鐞氼偂鎹㈡稉鈧鐔糕叺姒勬柨鍙嗗锔肩礄nowClientTime<=0 閺冩湹绗夐弽锟犵崣閸掔増婀￠敍澶堚偓?*/
	public static boolean clientHasAnyDream(long nowClientTime) {
		if (CLIENT_DREAMED_BY.isEmpty()) return false;
		if (nowClientTime <= 0) return true;
		return CLIENT_DREAMED_BY.values().stream().anyMatch(end -> end > nowClientTime);
	}

	/** 鐎广垺鍩涚粩顖涙焽缁?閹广垺婀囧〒鍛倞闂€婊冨剼閵?*/
	public static void clientClear() {
		CLIENT_DREAMED_BY.clear();
	}

	/** 閸氭垵鍙嗗锔炬窗閺嶅洦婀版禍鍝勫絺缁澹婇弲鏇炲閸栧拑绱欐禒鍛负鐎瑰墎娲伴弽鍥ㄦ箒鐏炲繐绠烽敍娑氭晸閻椻晝娲伴弽鍥儲鏉╁浄绱氶妴鍌氬瘶閸愬懏鎯＄敮锕傤棨濮婏箓鐡?UUID閿?
	 * 娓氭稑顓归幋椋庮伂缂佸瓨濮㈤妴宀冾潶鐠嬩礁鍙嗗锔衡偓宥夋殔閸?閳?閹凤附鍩呴崗銉︹叺閼板懓鍤滃杈╂畱 entity_glow 闁繗顫嬬€靛綊顥ゅ锕傜摕閻ㄥ嫭寮挎潏骞库偓?*/
	private static void sendVeilToTarget(ServerPlayer nightmare, LivingEntity target, int durationTicks) {
		if (target instanceof ServerPlayer targetPlayer) {
			SscAddonNetworking.sendDreamVeil(targetPlayer, durationTicks, nightmare.getUUID());
		}
	}

	/** 閺傤厾鍤庨柦鈺佺摍閵?*/
	public static void onPlayerDisconnect(ServerPlayer player) {
		clearAllFor(player);
	}

	// ===== 閵嗗本浜归幆褋鈧秵濡ч懗鑺ユ暜閹炬埊绱欓悽?NightmareFearManager 鐠嬪啰鏁ら敍灞肩矌閺堝秴濮熺粩顖ょ礆 =====
	/** O(1) lookup for effects landing now; another nightmare's ledger does not qualify. */
	public static boolean isDreaming(ServerPlayer nightmare, LivingEntity target) {
		Map<UUID, Long> dreams = DREAMING.get(nightmare.getUUID());
		Long until = dreams == null ? null : dreams.get(target.getUUID());
		return until != null && until > target.level().getGameTime();
	}

	/** 閺€鍫曟肠閺屾劙顥ゅ锕傜摕瑜版挸澧犲鎻掑弳濮婏妇娈戦崗銊╁劥鐎涙ɑ妞块惄顔界垼鐎圭偘缍嬮敍鍫ｆ硶鐎圭偘缍嬮弻銉﹀閿涘本澹樻稉宥呭煂閻ㄥ嫯鐑︽潻鍥风礆閵?*/
	public static List<LivingEntity> collectDreamTargets(ServerPlayer nightmare, long now) {
		List<LivingEntity> out = new ArrayList<>();
		Map<UUID, Long> dreams = DREAMING.get(nightmare.getUUID());
		if (dreams == null) return out;
		for (Map.Entry<UUID, Long> e : dreams.entrySet()) {
			if (e.getValue() <= now) continue;
			if (nightmare.getServer() == null) continue;
			net.minecraft.world.entity.Entity ent = null;
			for (net.minecraft.server.level.ServerLevel w : nightmare.getServer().getAllLevels()) {
				net.minecraft.world.entity.Entity found = w.getEntity(e.getKey());
				if (found != null) { ent = found; break; }
			}
			if (ent instanceof LivingEntity le && le.isAlive() && !DomainManager.blocksTargeting(nightmare, le)) out.add(le);
		}
		return out;
	}

	/** 閹跺﹥鐓囬惄顔界垼閸︺劏顕氭鐔糕叺姒勬柨鎮曟稉瀣畱閸忋儲鈪甸崚鐗堟埂閺冨爼妫块柌宥囩枂娑?now+400閿?0s閿涘鈧?*/
	public static void resetDream(UUID nightmareUuid, UUID targetUuid, long now) {
		Map<UUID, Long> dreams = DREAMING.get(nightmareUuid);
		if (dreams != null) dreams.put(targetUuid, now + BAL.i("duration_ticks", DREAM_DURATION_TICKS));
	}

	/** 閹劖鍎抽張鐔兼？閹镐胶鐢婚柨浣哥暰閿涙碍濡哥拠銉ф窗閺嶅洨娈戦崗銉︹叺閸掔増婀￠弮鍫曟？闁藉娲?now+400閿涘牊鐦?tick 鐠嬪啰鏁ら敍澶堚偓?*/
	public static void lockDream(UUID nightmareUuid, UUID targetUuid, long now) {
		resetDream(nightmareUuid, targetUuid, now);
	}

	/** 瀵搫鍩楅崬銈夊晪閻╊喗鐖ｉ敍姘矤<b>閹碘偓閺?/b>妞嬬喐鈪垫鏃傛畱閸忋儲鈪电悰銊よ厬缁夊娅庣拠銉ф窗閺嶅浄绱濋獮鍓佸敳閻忣厼鍙剧€广垺鍩涚粩顖涙瑜?閸忋儲鈪甸梹婊冨剼閿涘牊浜归幆褏绮ㄩ弶鐔烘暏閿涘鈧?*/
	public static void forceWakeAll(UUID targetUuid, long now, net.minecraft.server.level.ServerLevel contextWorld) {
		List<UUID> wokeBy = new ArrayList<>();
		for (Map.Entry<UUID, Map<UUID, Long>> e : DREAMING.entrySet()) {
			if (e.getValue().remove(targetUuid) != null) wokeBy.add(e.getKey());
		}
		ACCUM.values().forEach(acc -> acc.remove(targetUuid));
		// 閸氭垹娲伴弽鍥ㄦ拱娴滄椽鈧劖鈪垫鏂垮絺閸忓磭閮寸紒鎾存将閸栧拑绱欑€广垺鍩涚粩顖炴殔閸嶅繘鈧劖娼粔濠氭珟閵嗕焦妾胯ぐ杈ㄦ￥閸忓磭閮撮弮鍓佸敳閻忣叏绱?
		if (!wokeBy.isEmpty() && contextWorld != null) {
			if (contextWorld.getEntity(targetUuid) instanceof ServerPlayer targetPlayer) {
				for (UUID nightmareUuid : wokeBy) {
					SscAddonNetworking.sendDreamVeil(targetPlayer, 0, nightmareUuid);
				}
			}
			// 閸氭垶鐦℃稉顏呮禈閸忋儲鈪电€瑰啰娈戞鐔糕叺姒勬柨褰傞幓蹇氱珶閻斿嫮浼冮崠鍜冪礄閻喎鐤?entityId + duration=0閿涘绱伴張宥呭缁旑垰鍑″鍝勫煑閸戠儤鈪甸敍?
			// 娴ｅ棙寮挎潏鐟邦吂閹撮顏悰銊ュ涧鐠併倕鍩岄張鐔告闂傜补鈧柡鈧柧绗夐崣鎴濆瘶妞嬬喐鈪垫鏃囶潒鐟欐帞鐭囨潏閫涚窗濞堝鏆€閺堚偓婢?20s閿?
			// 閻鎹ｉ弶銉ュ剼閵嗗苯鍙嗗锔界梾鐞氼偄宸遍崚鍫曗偓鈧崙鎭掆偓宥忕礄婢舵碍鈪垫鏂挎倗閼奉亞鍞查悘顓″殰瀹搁亶鍋呴弶鈽呯礆閵?
			net.minecraft.world.entity.Entity targetEntity = contextWorld.getEntity(targetUuid);
			if (targetEntity != null) {
				int targetEntityId = targetEntity.getId();
				for (UUID nightmareUuid : wokeBy) {
					if (contextWorld.getEntity(nightmareUuid) instanceof ServerPlayer nightmarePlayer) {
						SscAddonNetworking.sendWebHighlight(nightmarePlayer, targetEntityId, 0, DREAM_OUTLINE_COLOR);
					}
				}
			}
		}
	}
}
