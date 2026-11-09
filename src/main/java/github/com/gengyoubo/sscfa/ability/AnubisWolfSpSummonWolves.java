package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.compat.MinionTracker;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import net.onixary.shapeShifterCurseForge.power.AnubisMinionService;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.AnubisWolfSpPerks;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP闂傚倸鍟悘婵囨叏椤愨懣鎺楀籍閳ь剟寮绘径瀣枙閻庯綆鍓欓缁樼箾閸犅板€ゅú锝夋煙閸ㄦ稑浜鹃梺?- 闂佸憡鍔戦崕宕団偓姘⒔閹叉鎷呴崨濠傚幑
 * 闂佸憡鐟崜娆撳极濞ｅ窉ubisWolfMinionEntity闂佹寧绋戦悧鍛箔瀹€鍕煑婵°倐鍋撻柛?闂佽　鍋撻悹鍥ㄥ絻濮ｅ﹪鏌￠崘顓熺【鐟滈偊鍋婂畷顒勫Χ閸ャ劎鏆犻梺鑲╊劜缁辨洜鈧岸绠栭幆鍕矙閹稿骸鈧亶鎮楅崷顓炰粧缂傚秴顑夐弫宥嗗緞婵犲倻顦ч梺鍛婃煛閺呮粓宕归鍕婵＄偛鐨烽崑?
 * 闂佹悶鍊曟姝屻亹閵娾晜瀚呴柛鏇ㄥ亜椤?.5缂備礁顦扮敮鎺楀箖濡ゅ懏鏅悘鐐插悑閸炲姊洪幐搴ｆ噧闁搞値鍙冮幃?闂佸憡鐟禍婊堝疮閿濆鍋戠痪顓㈩棑缁€鍑瀍ath Domain濠电姷顣介崑鎾寸箾閼碱剛甯涙俊鐐插€圭粙?闂佸憡鐟禍椋庢濠靛洨鈻旈柡鍌涱儥閺夎崵鈧鍠栭幖顐︽儓濮椻偓楠炩偓鐟滄繄妲愬璺何?
 * 闂佸憡鍔戦崕宕団偓姘剁畺楠炴劖鎷呴搹瑙勬毎30缂備礁顦扮敮鎺楀箖濡も偓閳藉宕奸悢鍛婄枃闂?
 * 闂佸憡鍔戦崕宕団偓姘剁畺閹虫盯顢旈崟顓熸殧闂佸憡鍨甸澶娒归崒鐐茬哗閻犲洦褰冨В濠囨煕濠婂啯婀板☉鎾崇Ч楠炲洭宕煎鍐В婵炲瓨绮嶅浠嬪焵椤掍焦顫楃紒棰濆亰閺佸秹宕稿鍧磂l 3闂佹寧绋掔喊宥夊极妤ｅ啫绀勯悹鍥ㄥ絻椤?闂佹寧绋戦惉濂稿极閹捐宸?0闂佹寧绋戦張顒佺▔瀹ュ绠?HP/hit闂佹寧绋戦ˇ顓㈠焵?
 * 濠殿喗绻嗛濠冾殽閸涘绱ｉ柛鈩冾殘閸樼敻鏌ゆ潏銊︾叆濠殿喒鏅犲顕€骞嗛幍顔绢啋婵炴垶鎸撮崑鎾存叏濠靛嫬鍔氭い銈呭暙椤曪綁宕崟顓熷墤闂佽鍎搁崘顓炵彲缂傚倷鐒﹂悷銈囪姳閿濆鐒婚柣鏂垮槻椤斿﹪鏌?
 * CD30缂備礁顦扮敮顏嗘濠靛鍤愰柕澶堝劚閸ゆ帡寮堕崼銏犱壕婵炴捁鍩栧鍕槻鐟滈偊鍋婂畷顒勫Χ閸℃ɑ顔嶉梺?闂佸憡鐟禍椋庢濮樿泛绀嗘繛鍡樺姇閸旓妇绱撻崘鈺冾暡闁逞屽墻濠€涓?缂備礁顦扮敮鍥焵?
 */
public class AnubisWolfSpSummonWolves {

	/**
	 * 闂佹悶鍊曟姝屻亹閵娾晜瀚呴柛鏇ㄥ亜椤棃鏌￠崘銊у煟婵☆偄娼￠弫宥夊锤閻穯k闂?
	 */
	private static final int HOWL_TICKS = 30; // 1.5缂?

	// ==================== 闁汇埄鍨遍幃鍌炲闯?====================
	// 婵炲濮伴崕鎵箔閸涱垱鏆滈柟闂寸濞呫倝鏌涜濞诧絿鎷归悢鍓叉付婵☆垱顑欓崥鍥煕婵犲嫨鈧妲愭潏銊︿氦闁归偊鍨奸弨浠嬫煛閸愨晜鏋勭紒?balance 闂婎偄娴傞崑鍡涘矗鎼达絾瀚氶悹鍥ㄥ絻缁插潡鏌ㄥ☉妯荤窛cope闂佹寧绋掗悺鐜漣lities.anubis_summon_wolves闂?
	/**
	 * 闂佹悶鍊曟姝屻亹閵娾晛绀勯煫鍥ㄧ▓閸嬫捇鎮㈤崫銉πユ繛鎾磋壘椤戞垹妲愬▎鎰攳婵犻潧鐗婂▓?0%闂備緡鍋嗛崰搴ｂ偓瑙勫▕閺?
	 */
	private static final double HOWL_SLOW_FACTOR = 0.5;
	/**
	 * 闂佸憡鐟崜娆撳极濠婂牊鈷掗柟缁㈠枟椤撻箖鏌ㄥ☉妯荤窡ick闂佹寧绋戦¨鈧紒杈ㄧ箖缁傚秹濡舵径瀣珦闂佸憡鍨煎▍锝夌嵁?
	 */
	private static final int SUMMON_INTERVAL = 5;
	/**
	 * 闂佸憡鍔戦崕宕団偓姘⒔閳ь剚绋掕摫婵＄偛娼″顕€宕奸弴鐕傜吹闂佹寧绋戝鍗沜k闂?
	 */
	private static final int WOLF_DURATION = 600; // 30缂?
	// CD闂佹寧绋戦悧濠囶敆濠婂懏鏆?闂佽鍨弲婵堢礊閹烘鏅璺猴工閸ゆ帡鏌ｉ姀锝勫惈缂侇喚鍎ょ粙澶愬焵椤掑嫬绀冪€瑰嫭婢樼粊閬嶆煛閸繄孝濠?SkillCooldowns + power JSON 闂備焦婢樼粔鍫曟偪閸℃瑧涓嶉柨娑樺閸婄偤鏌ㄥ☉妯垮閻㈩垼鍋嗛弫顕€骞栨担鍛婎仭閻庣懓鎲¤ぐ鍐垂?
	/**
	 * 闂佸搫鐗冮崑鎾愁熆閸棗鍟崐閬嶆煛閸愵厽纭鹃柣掳鍔戝畷鐑藉Ω閵夈儲娈犻梺缁樼懃閸氬寮?
	 */
	private static final int MAX_WOLVES = 6;
	/**
	 * 闂佺硶鏅炲▍锝夈€侀崨鏉戠煑妞ゆ巻鍋撻柡浣圭矒瀵偅瀵奸弶鎴烆仭
	 */
	private static final int BASE_SUMMON_COUNT = 2;
	/**
	 * 婵☆偅婢樺Λ妤呮偂濞嗘劖娅犻柣鎰絻缁讳線鏌涘▎妯哄闁轰焦绮撳顐ｅ閺夋垶顏?
	 */
	private static final int DOMAIN_SUMMON_COUNT = 4;
	/**
	 * 闂佸憡鍔戦崕宕団偓姘⒔缁灚寰勫畝鍕礈闂佹寧绋戦悧鍛箔鐎涚澑C闂佸憡顭囬崰鎾存櫠濮婃紴vel 3婵炴垶鎸撮崑鎾绘煠閻ゎ垱褰х紒?
	 */
	private static final int MINION_LEVEL = 3;
	/**
	 * 婵☆偅婢樺Λ妤呮偂濞嗘劖娅犻柣鎰絻缁讳礁螞閻楀牏绠撴い锕€顭烽幃浠嬫偄闁垮娈ラ梺?
	 */
	private static final double DOMAIN_BONUS_HEALTH = 10.0;
	/**
	 * 婵☆偅婢樺Λ妤呮偂濞嗘劖娅犻柣鎰絻缁讳礁螞閻楀牏绠撴い锕€顭峰銊ф嫚閹绘帗鐦栭梺?
	 */
	private static final double DOMAIN_BONUS_ATTACK = 2.0;
	/**
	 * 闂佹悶鍊曟姝屻亹閵娾晛绀勯煫鍥ㄧ▓閸嬫捇鎮㈤懜闈涘伎婵°倖顨嗘竟鍡涱敄娑斾箒ID
	 */
	private static final UUID HOWL_SLOW_UUID = UUID.fromString("c9d5e6f7-a8b9-4c0d-1e2f-3a4b5c6d7e8f");
	/**
	 * 婵☆偅婢樺Λ妤呮偂濞嗘劖娅犻柣鎰絻缁讳線鏌￠埀顒傛嫚閹绘帗鐦栭梺鍛婃⒐缁嬫劙骞堥妸锔绘闁规鍣崕濂ID
	 */
	private static final UUID DOMAIN_ATTACK_UUID = UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a");
	/**
	 * 婵☆偅婢樺Λ妤呮偂濞嗘劖娅犻柣鎰絻缁讳線鏌ｉ姀銏犳瀻闁圭绻濆畷鎰版⒒鐎涙ê寮挎俊銈嗩殕婢瑰棝顢栨稊涔両D
	 */
	private static final UUID DOMAIN_HEALTH_UUID = UUID.fromString("e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b");
	/**
	 * 婵°倖顨嗗姗€骞栬瀵劎鎷犻幓鎺撶槚闂佸憡姊圭粙鎴炴櫠濞戙垹绀勯煫鍥у閸欏繐螖濡や礁顥嬫い鎴滅畳UID
	 */
	private static final UUID TRINKET_DAMAGE_UUID = UUID.fromString("f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c");
	/**
	 * 婵°倖顨嗗姗€骞栬瀵劎鎷犻幓鎺撶槚闂佸憡姊圭粙鎴炴櫠濞戙垹绀勯煫鍥ㄦ⒒濡插嘲銆掑顒夊劀缂?25%闂?
	 */
	private static final double TRINKET_DAMAGE_REDUCTION = -0.25;
	/**
	 * 婵°倖顨嗗姗€骞栬閹即鍩€椤掑嫭鐓傞煫鍥ㄦ尭椤も偓闂佸憡鍨电换瀣箞閵婏富娈伴柟娈垮櫘閸庡イUID
	 */
	private static final UUID TRINKET_HEALTH_UUID = UUID.fromString("a3b4c5d6-e7f8-4a9b-0c1d-2e3f4a5b6c7d");
	/**
	 * 婵°倖顨嗗姗€骞栬閹即鍩€椤掑嫭鐓傞煫鍥ㄦ尭椤も偓闂佸憡鍨电换鎴︽儊椤旂晫鐟归悗锝呭缁€?35%闂?
	 */
	private static final double TRINKET_HEALTH_REDUCTION = -0.35;
	// ==================== 闂佺粯顭堥崺鏍焵椤戞寧绁伴柟椋庡厴閻?====================
	private static final ConcurrentHashMap<UUID, SummonData> ACTIVE_SUMMONS = new ConcurrentHashMap<>();
	/** 缂傚倷鑳堕崰宥囩博閹绢喖绀冪€瑰嫭婢樼粊閬嶆煛閸繄孝濠殿喚鍠栭幆鍐礋椤撯偓閺冨倵鍋撶憴鍕暡濠碘姍鍥ㄥ殑?ID闂? power 濠电偛顦崝宀勫船閻ｅ本宕夋い鏍ㄦ皑缁愮偤鏌ㄥ☉姗嗘Ф闁?*/
	public static final String SKILL_ID = "my_addon:form_anubis_wolf_sp_summon_wolves";

	// 闂傚倸鍟抽崺鏍敊?5闂佹寧绋掔喊宥咃耿閸ヮ剙绀夐柨娑樺娴煎倿鏌℃径濠傛殭闁宠顦崇粻娑㈩敃閵堝懎瀛ｉ柣鐘叉穿椤曆嗐亹閸ヮ剚鏅柛顐ｇ箖瑜般儵鏌ｉ幘妤€瀚鎾绘煕閹烘挾绠撴い顐ｅ姍瀹曠娀寮借缁€鈧梻渚囧亐閸嬫挸顫楀☉娆樼劸妞ゆ捁宕甸弫顕€骞栨担鍛婎仭闂?
	private static final BalanceReader BAL = new BalanceReader("abilities.anubis_summon_wolves");

	private AnubisWolfSpSummonWolves() {
	}

	/**
	 * 闂佺粯澹曢弲娑㈩敊瀹ュ绠板璺侯槺閻熸挻绻涢崰掳鍊ゅú锝夋煙閸ㄦ稑浜鹃梺鐓庡帠缁瑩寮鐘冲枂闁挎洍鍋撶憸?
	 */
	public static boolean execute(ServerPlayer player) {
		// 闂備焦褰冪粔鎾囬弻銉︾厒濠电姴鍟弶瑙勪繆椤愮喎浜鹃梺鍝勮閸庤崵妲愬鐑?闂傚倸鍊堕崝搴ㄣ€呴敃鈧蹇涙嚑椤掍焦娈?power 闁?begin 缂傚倷鑳堕崰宥囩博閹绢喖绠柨鏃囨閻掑鏌ㄥ☉妯侯殭闁?P1-1闂?
		if (ACTIVE_SUMMONS.containsKey(player.getUUID())) {
			return false;
		}

		// 濠碘槅鍋€閸嬫捇鏌＄仦璇插姦闁靛牅鍗冲畷顐ｆ媴缁嬫娼遍梺?
		boolean hasCrystal = hasTrinketEquipped(player);
		int maxWolves = maximumWolves(player);

		// 闂備緡鍋呮穱铏规崲閸嶅摉layerEntityMinion缂備緡鍨靛畷鐢靛垝閸濆嫀娑㈠焵椤掑嫬钃熼柕澶堝劤缁夊ジ鏌涢幘宕囆㈤柛鐕傜秮閹亪宕楅崗鍏碱啀闂?
		int aliveCount = getMinionCount(player);
		if (aliveCount >= maxWolves) {
			// 閻庤鐡曠亸顏堝箵椤忓懐鈻斿┑鐘叉处椤庢瑩鏌ㄥ☉娆戭暡閻?fail_cooldown 缂傚倷鐒﹂幐楣冩偩?+ 婵犮垺鍎肩划鍓ф喆閿曞倹顥婇柧蹇ｅ亝濞?
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.penalizeAttempt(player, SKILL_ID);
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8f, 0.5f);
			return false;
		}

		// 濠碘槅鍋€閸嬫捇鏌＄仦璇插姕婵″弶鎮傚畷銉╂晝閳ь剙锕㈤鍕垫桨閻犱焦濞婇妤€螞閺夋寧顥滈柣鎾寸懄缁?
		boolean domainActive = AnubisWolfSpDeathDomain.hasActiveDomain(player.getUUID());

		// 闁荤姳绶ょ槐鏇㈡偩婵犳艾鐭楁い鏍ㄨ壘鐏忓棝鏌涢悙顏勨偓鏍汲閻斿吋鐓傞煫鍥ュ劤缁€鍕斿Δ浣圭凡闁逛匠鍕珷闁绘劖褰冮～?闂佸憡鐟禍婊堟偉閸撲焦鍠嗛柛鏇ㄥ亜鐏忓棝鏌涢悙顏勨偓褏妲?
		int baseSummon = hasCrystal ? BAL.i("base_summon_count", BASE_SUMMON_COUNT) + 1 : BAL.i("base_summon_count", BASE_SUMMON_COUNT);
		int domainSummon = hasCrystal ? BAL.i("domain_summon_count", DOMAIN_SUMMON_COUNT) + 1 : BAL.i("domain_summon_count", DOMAIN_SUMMON_COUNT);
		int targetCount = domainActive ? domainSummon : baseSummon;
		int canSummon = Math.min(targetCount, maxWolves - aliveCount);

		// 闂佸憡甯楃粙鎴犵磽閹捐鐭楁い鎺嗗亾闁轰焦绮撳顐︽偋閸繄銈﹀Δ鐘靛仦閸庤偐鎹㈠鈧畷妤呭Ψ閵夛妇鏋冮梺鍛婄懐閸嬪﹤螣娴ｇ硶鏋?
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
		SummonData data = new SummonData(canSummon, domainActive);
        data.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		ACTIVE_SUMMONS.put(player.getUUID(), data);

		// 闂佸湱铏庨崢浠嬪棘娓氣偓閹亪鎮℃惔銏㈡瀮闂佸憡鐟崑鍕晬?
		ServerLevel world = player.serverLevel();
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5f, 0.6f);

		// 闁圭厧鐡ㄥ濠氬极閵堝绀勯煫鍥ㄧ▓閸?
		applyHowlSlow(player);

		return true;
	}

	// ==================== 闂佺娴氶崜娆戞閹达箑绠抽柕澶堝劚缂?====================

	/**
	 * 濠殿噯绲界槐鈺k闂佸搫娲ら悺銊╁蓟?
	 */
	public static void tick(ServerPlayer player) {
		// 闂佸憡绋忛崝宥咃耿閿熺姵鈷掗柕濠忕畱娴犳﹢鏌涢幘宕囆ら柣銈呮閺佸秹宕煎┑鍡楀紬闂佸憡鐟﹂悧鏃傜礊閹达箑绀冪€广儱妫涢崣鈧悷婊嗗焽閸ㄦ椽鍩€椤戣法绛忕紒杈ㄥ哺閺佸秴顫濋懜闈涱潯闂傚倸鍟悘婵囨叏椤愨懣鎺楀籍閳ь剟寮绘径鎰亼闁兼祴鏅涚拹鐔兼倵绾懎鐓愰柣?tick 闂佸憡鐟禍锝嗘叏閾忣偆鈻旈柍褜鍓欓埢搴ㄥ灳閸愯尙鎮兼繛杈剧稻閻熻京妲?
		// 闂備緡鍓欓悘婵嬪储?%20 闂佸憡甯囬崐鏍蓟閸ャ劎鈻曢悗锝庝簻椤ゅ懘鏌涜箛鎾跺濞?CCA 閻熸粏鍩囬崹娲焵椤戣法鍔嶉柣鎿勭磿閹?
		if (player.serverLevel().getGameTime() % 20 == 0 && FormUtils.isAnubisWolfSP(player)) {
			tickTrinketModifiers(player);
		}

		UUID uuid = player.getUUID();
		SummonData data = ACTIVE_SUMMONS.get(uuid);
		if (data == null) {
			if (player.tickCount % 20 == 0) removeHowlSlow(player);
			return;
		}
		if (!player.isAlive() || !FormUtils.isAnubisWolfSP(player)) {
			ACTIVE_SUMMONS.remove(uuid);
			removeHowlSlow(player);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
			return;
		}
		if (data.phase != Phase.ACTIVE && !data.enhancedDomain
				&& (github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != data.castId)) {
			interruptControl(player);
			return;
		}

		data.ticksElapsed++;

		switch (data.phase) {
			case HOWLING -> tickHowling(player, data);
			case SUMMONING -> tickSummoning(player, data);
			case ACTIVE -> tickActive(player, data);
		}
	}

	/**
	 * 闂佺粯澹曢弲娑㈩敊瀹ュ妫樻い鎾跺Т濞?闂佸憡鐟﹂敋闁奸晲鍗冲顕€鎳滈悽娈夸紩闂?
	 */
	public static void clearPlayer(UUID uuid) {
		ACTIVE_SUMMONS.remove(uuid);
	}

	/**
	 * 濠电偞鎸搁幊鎰板箖婵犲洤绠ラ柍褜鍓熷鍨緞婵犲倸鐨㈤梺鍝ュ亼閸婃鎱ㄩ幖浣哥畱濞撴艾锕︾粈鍕煛閸繄孝濠殿喚鍠栧畷鎶藉Ω閳轰焦顏熼梺鍛婂嚬閸ㄥ崬顪冮崒婊勫闁告劦浜濋弳蹇涙煥?
	 */
	public static void clearAll() {
		ACTIVE_SUMMONS.clear();
	}

	/** Stop the pending action while keeping wolves which have already become autonomous. */
	public static void interruptControl(ServerPlayer player) {
		SummonData data = ACTIVE_SUMMONS.get(player.getUUID());
		removeHowlSlow(player);
		if (data == null || data.phase == Phase.ACTIVE || data.enhancedDomain) return;
		if (data.summonedWolfUuids.isEmpty()) ACTIVE_SUMMONS.remove(player.getUUID());
		data.wolvesToSummon = data.wolvesSummoned;
		data.phase = Phase.ACTIVE;
		data.ticksElapsed = 0;
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
	}

	public static void clearAll(net.minecraft.server.MinecraftServer server) {
		if (server.getPlayerList() != null)
			for (ServerPlayer player : server.getPlayerList().getPlayers()) removeHowlSlow(player);
		clearAll();
	}

	/** Stop pending summons before killing owned wolves, so recall cannot spawn replacements. */
	public static void recallAll(ServerPlayer player) {
		SummonData data = ACTIVE_SUMMONS.remove(player.getUUID());
		removeHowlSlow(player);
		if (data != null && data.phase != Phase.ACTIVE && data.castId >= 0) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
		}
		player.removeEffect(MobEffects.WITHER);
		var minions = MinionTracker.get(player, MinionTracker.ANUBIS_WOLF);
		if (minions == null) return;
		for (UUID uuid : List.copyOf(minions)) {
			for (ServerLevel world : player.getServer().getAllLevels()) {
				if (world.getEntity(uuid) instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()
						&& player.getUUID().equals(wolf.getOwnerUUID())) {
					// Keep SSC's onDeath/death-heal path; discard would lose the recall heal.
					wolf.kill();
				}
			}
		}
		MinionTracker.clear(player, MinionTracker.ANUBIS_WOLF);
	}

	public static int maximumWolves(ServerPlayer player) {
		return AnubisWolfSpPerks.capacity(player,
				BAL.i("max_wolves", MAX_WOLVES) + (hasTrinketEquipped(player) ? 2 : 0));
	}

	/**
	 * 婵犫拃鍛粶鐎圭宕～銏㈡媼閹剁瓔鈧螞閺夋寧顥滈柣鎾寸懇閹虫盯顢旈崟顐嶆鏌涘▎妯哄闁轰焦绮撳畷姗€濡疯椤忓鏌ㄥ☉妯煎ⅲ闁绘埊闄勫濠氬炊瑜庨悿鍥煕濞嗘ê浜版俊顖欑閳绘挾鍠婃径宀€顦梺鐑╂櫅閻°劎鏁幘顔藉仺闁绘梻顭堥悘鍥煥?
	 * 闁诲骸婀遍崑銈咁瀶椤栫偛鐭楁い鎺嗗亾闁轰焦绮撳顐ｅ閺夋垶顏?= min(requestCount, MAX_WOLVES - 閻庡湱顭堝璺猴耿娓氣偓瀵偅瀵奸弶鎴烆仭)
	 */
	public static void autoSummonForEnhancedDomain(ServerPlayer player, int requestCount) {
		removeHowlSlow(player);
		// 婵犫拃鍛粶鐎规瓕椴搁敍鎰板幢濡ゅ啫骞嶉梺鎸庣⊕閼归箖宕㈠☉妯锋闁割偆鍠愬▓鐘绘煟濠婂懎顣兼繝鈧笟鈧畷姗€濡疯椤忓鏌涘鍐鐟滈偊鍋婂畷顒勫Χ閸℃瑣鍋婄紓浣割儏椤戞垹妲愬┑鍫熷厹妞ゆ棁宕电粻浠嬫煠鐎圭姵顥夌憸閭﹀亜椤?闂?
		SummonData oldData = ACTIVE_SUMMONS.remove(player.getUUID());
		if (oldData != null) {
			dissipateWolves(player, oldData, player.serverLevel());
		}
		// 濠电偞鍨甸悧濠囧汲瀹ョ嬁layerEntityMinion缂備緡鍨靛畷鐢靛垝閻戞鈻旀い鎾跺枔閺嗘瑩鏌ｉ敐鍡欐噮婵炲牊鍨垮畷姗€濡疯椤?
		dissipateAllMinions(player);

		// 闂佸憡甯楃粙鎴犵磽閹惧鈻旈柍褜鍓氱粙澶愵敂閸偅娈梺绯曟櫇閸犲酣路閸愵煈鍤曢柧蹇氼潐閻ｇ洦ummonData闂佹寧绋戦惉鐓幟洪崸妤€绠抽柕澶堝妿缁犲鏌涜箛锝嗙彃UMMONING闂傚倸鍟抽崺鏍敊?
		SummonData data = new SummonData(Math.min(requestCount, maximumWolves(player)), true);
		data.enhancedDomain = true;
		data.phase = Phase.SUMMONING;
		data.ticksElapsed = 0;

		ACTIVE_SUMMONS.put(player.getUUID(), data);
	}

	private static void tickHowling(ServerPlayer player, SummonData data) {
		ServerLevel world = player.serverLevel();

		// 闂佹悶鍊曟姝屻亹閵娾晛瀚夐柣鏂垮悑閿涚喓绱掗钘夌瑨闁烩剝鐟╁顐﹀醇閻旇桨鍑?- 闂佽绻掗弫濠氭偤婵犲嫬鍨濋柟鐑樺灩閹藉秹鏌涢妷銉殶缂?
		if (data.ticksElapsed % 3 == 0) {
			double angle = (data.ticksElapsed * 12.0) * Math.PI / 180.0;
			double radius = 1.5;
			double px = player.getX() + Math.cos(angle) * radius;
			double pz = player.getZ() + Math.sin(angle) * radius;
			ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.SOUL,
					px, player.getY() + 0.5, pz, 2, 0.1, 0.3, 0.1, 0.02);
		}

		if (data.ticksElapsed >= BAL.i("howl_ticks", HOWL_TICKS)) {
			// 闂佽棄鍟€氼剚鎱ㄨ箛鏇楀亾閻熺増婀伴柛銊﹀哺閺佸秴鐣濋埀顑跨昂闂傚倸瀚ㄩ崐鏇㈠闯濞差亝鐒?
			removeHowlSlow(player);

			// 闁哄鏅滅粙鎴﹀矗閸℃稑鐭楁い鎺嗗亾闁轰焦绮撳濂告嚋濞堝灝鏂€
			data.phase = Phase.SUMMONING;
			data.ticksElapsed = 0;

			// 闂佸湱铏庨崢浠嬪棘娓氣偓瀹曪綁顢楅埀顒勫极濠婂牊顥婇柧蹇ｅ亝濞?
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.5f);
		}
	}

	private static void tickSummoning(ServerPlayer player, SummonData data) {
		// 濠殿噯绲界划绫橫MON_INTERVAL tick闂佸憡鐟崜娆撳极濠婂嫮鈻旈柍褜鍓熷畷?
		if (data.ticksElapsed % BAL.i("summon_interval", SUMMON_INTERVAL) == 0 && data.wolvesSummoned < data.wolvesToSummon) {
			spawnMinionWolf(player, data);
			data.wolvesSummoned++;
		}

		// 闂佺绻堥崝鎴﹀磿閹绢喖鐭楁い鎺嗗亾闁轰焦绮庨埀顒傛嚀閺堫剟鎯侀鐐茬婵炲棙甯楃粊顕€鏌涜箛锝嗩€怌TIVE闂傚倸鍟抽崺鏍敊?
		if (data.wolvesSummoned >= data.wolvesToSummon) {
			data.phase = Phase.ACTIVE;
			data.ticksElapsed = 0;

			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, data.castId);
		}
	}

	// ==================== 闂傚倸鍟抽崺鏍敊鐏炴儳绶為柛鏇ㄥ幗閸?====================

	private static void tickActive(ServerPlayer player, SummonData data) {
		ServerLevel world = player.serverLevel();
		int wolfDuration = BAL.i("wolf_duration", WOLF_DURATION);

		// 濠?0tick濠碘槅鍋€閸嬫捇鏌＄仦璇插姍缂佹梹娼欓埢搴ㄥ灳閸愬弶娈犻梺缁樼懆閸嬫劕鈻撻幋锔藉€堕柣銏犳憸閹艰櫕绻涚仦鍓ф噧闁告ǜ鍊濋幃?
		if (data.ticksElapsed % 20 == 0) {
			refreshWolfBuffs(world, data);
		}

		// 闂佸憡鍔戦崕宕団偓姘剁畺楠炴劖鎷呴搹瑙勬毎闂佸搫鍟悥鐓幬涢崸妤€绀嗛柣妤€鐗婇崺?
		if (data.ticksElapsed >= wolfDuration) {
			// 濠电偞鍨甸悧濠囧汲鎼淬劌绠ラ柍褜鍓熷鍨緞鐎ｎ偅瀚抽梺閫涚祷椤顢曟總鍛婂剭闁告洦鍘奸?
			dissipateWolves(player, data, world);
			ACTIVE_SUMMONS.remove(player.getUUID());
		}
	}

	private static void spawnMinionWolf(ServerPlayer player, SummonData data) {
		ServerLevel world = player.serverLevel();
		// Passive summons and accessory changes can fill the slots during the howl.
		if (getMinionCount(player) >= maximumWolves(player)) return;

		BlockPos spawnPos = summonPosition(world, player, player);
		AnubisWolfMinionEntity wolf = spawnSpellWolf(player, spawnPos, data.domainActive, data.enhancedDomain);
		if (wolf != null) data.summonedWolfUuids.add(wolf.getUUID());
	}

	/** Shared construction keeps native minion registration, level, domain and accessories identical. */
	public static AnubisWolfMinionEntity spawnSpellWolf(ServerPlayer player, BlockPos spawnPos, boolean domainActive, boolean enhancedDomain) {
		ServerLevel world = player.serverLevel();
		if (getMinionCount(player) >= maximumWolves(player)) return null;

		// 婵炶揪缍€濞夋洟寮ˉ鐮硁ionRegister.SpawnMinion闂佹眹鍨婚崰鎰板垂濮樿埖鏅柛顐到濞堜即鏌涢弬璇插闁汇劎鍠栭幃浠嬪焼閺夋tMinion濠电偛顦崝宀勫船娴犲绀嗛柣褎鈧挻ayerEntityMinion缂備緡鍨靛畷鐢靛垝濞差亝鏅?
		AnubisWolfMinionEntity wolf = MinionTracker.spawn(world, spawnPos, player);

		if (wolf == null) return null;

		int wolfDuration = BAL.i("wolf_duration", WOLF_DURATION);

		// 闁荤姳绀佹晶浠嬫偪閸℃瑧椹冲璺虹焸閻涙捇鏌ㄥ☉妯婚クevel 3: HP=20, Attack=4, 濠电偛鐬肩划顖炲礈閵堝棛鈻旈悹浣芥珪閻?HP/hit闂?
		wolf.setMinionLevel(BAL.i("minion_level", MINION_LEVEL));
		AnubisMinionService.summon(player, wolf, new com.google.gson.JsonObject());

		// 濠殿喗绻嗛濠冾殽閸涘绱ｉ柛鈩冾殘閸樼敻鏌ゆ潏銊︾叆濠殿喒鏅滈弲鍫曟倷閹绘帞绠?
		if (domainActive) {
			// 婵☆偆澧楃换鍌炈囨繝姘仺闁绘棃鏀遍崵鎺楁煕?
			AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
			if (healthAttr != null) {
				healthAttr.addPermanentModifier(new AttributeModifier(
						DOMAIN_HEALTH_UUID, "domain_bonus_health",
						BAL.d("domain_bonus_health", DOMAIN_BONUS_HEALTH), AttributeModifier.Operation.ADDITION));
				wolf.setHealth((float) healthAttr.getValue());
			}
			// 婵☆偆澧楃换鍌炈囨繝姘哗閻犲洦褰冨В濠囨煕?
			AttributeInstance attackAttr = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attackAttr != null) {
				attackAttr.addPermanentModifier(new AttributeModifier(
						DOMAIN_ATTACK_UUID, "domain_bonus_attack",
						BAL.d("domain_bonus_attack", DOMAIN_BONUS_ATTACK), AttributeModifier.Operation.ADDITION));
			}
			// 闂備緡鍋嗛崰搴ｂ偓鍦吋闂佽桨绀侀悧濠囨倶?
			wolf.addEffect(new MobEffectInstance(
					MobEffects.MOVEMENT_SPEED, wolfDuration, 0, false, false, true));
		}

		// 婵°倖顨嗗姗€骞栬瀵偊宕奸悢杞板嚱闂佹寧绋掑畝绋款瀶闁垮濯撮幖绮光偓铏疇闂佺粯鐟ラ崥瀣极妤ｅ啫绀勯悹鍥ㄥ絻椤?5%闂侀潧妫斿ù鍥€侀崨瀛樼厒?5%
		if (hasTrinketEquipped(player)) {
			AttributeInstance atkAttr = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
			if (atkAttr != null && atkAttr.getModifier(TRINKET_DAMAGE_UUID) == null) {
				atkAttr.addPermanentModifier(new AttributeModifier(
						TRINKET_DAMAGE_UUID, "trinket_damage_reduction",
						TRINKET_DAMAGE_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
			}
			AttributeInstance hpAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
			if (hpAttr != null && hpAttr.getModifier(TRINKET_HEALTH_UUID) == null) {
				hpAttr.addPermanentModifier(new AttributeModifier(
						TRINKET_HEALTH_UUID, "trinket_health_reduction",
						TRINKET_HEALTH_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
				// 闂佸憡鑹鹃張顒勵敆閻愯翰浜归柟鎯у暱椤ゅ懘鎮跺☉鎺戜壕闂備焦褰冪换鎰板春瀹€鍕濡鑳堕悷鎰版⒒?
				wolf.setHealth(Math.min(wolf.getHealth(), (float) hpAttr.getValue()));
			}
		}

		// 闂佹眹鍨婚崰鎰板垂濮樺崬鍨濋柟鐑樺灩閹藉秹鏌℃担鍝ュ闁诲繐顦甸弫宥夊醇濠垫劖婢旈悗娈垮枟濡炰粙銆傞锕€鏄ラ柣鏂垮槻鐏忓棝鏌涢悙顏勨偓妤€鈻撻幋锔藉亼闂傚倸顕悷婵嬫煛閸曨偄鈷旈柕鍥ㄥ哺閹﹢鎮介崫銉︽儞闂佸憡鍔栫敮鎺楀吹椤撶姴鍨濋柟鐑樺灩閹藉秹鏌?
		if (!enhancedDomain) {
			ParticleUtils.spawnParticles(world, ParticleTypes.SOUL_FIRE_FLAME,
					wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
			ParticleUtils.spawnParticles(world, ParticleTypes.SOUL,
					wolf.getX(), wolf.getY() + 0.3, wolf.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		}

		// 闂佸湱铏庨崢浠嬪棘娓氣偓瀹曟瑩鎼归悷鎵杸闂傚倸锕ㄥ▍鏇㈠疾?
		world.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
				SoundEvents.WOLF_GROWL, SoundSource.NEUTRAL, 1.0f, 0.7f);
		return wolf;
	}
	public static int spellWolfDuration() { return BAL.i("wolf_duration", WOLF_DURATION); }

	private static void refreshWolfBuffs(ServerLevel world, SummonData data) {
		for (UUID wolfUuid : data.summonedWolfUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (entity instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()) {
				// 闂佽绻掗弫濠氭偤婵犲倵鏌︽繛鍡楃箰鐢娊鏌ｉ妶搴′壕缂佸彉鍗冲畷鐑藉Ω瑜岀槐銉ヮ潡閸曨偄鈧綊鎯屽▎鎰枖濠电姴鍟ˇ褏绱撴担鍦噥缂侇煉缍侀弻鍛存偄瀹勯偊鍞篒II闂佺娉涢悘姘辩矓閻戣棄绀勯煫鍥ㄧ▓閸?
				BlockPos below = wolf.blockPosition().below();
				if (world.getBlockState(below).is(net.minecraft.world.level.block.Blocks.SOUL_SAND)
						|| world.getBlockState(wolf.blockPosition()).is(net.minecraft.world.level.block.Blocks.SOUL_SAND)) {
					wolf.addEffect(new MobEffectInstance(
							MobEffects.MOVEMENT_SPEED, 25, 2, false, false, false));
				}
			}
		}
	}

	// ==================== 闂佺粯鐟ч崢褑銇愰锕€鑸归柕鍫濆閻熴垻绱掗悪鍛？闁?====================

	private static void dissipateWolves(ServerPlayer player, SummonData data, ServerLevel world) {
		for (UUID wolfUuid : data.summonedWolfUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (entity instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()) {
				// 濠电偞鍨甸悧濠囧汲鎼达絽鍨濋柟鐑樺灩閹?
				ParticleUtils.spawnParticles(world, ParticleTypes.SOUL,
						wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 12, 0.4, 0.6, 0.4, 0.03);
				ParticleUtils.spawnParticles(world, ParticleTypes.SMOKE,
						wolf.getX(), wolf.getY() + 0.3, wolf.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
				// 闂佸湱铏庨崢浠嬪棘娴ｇ硶妲堥柛顐ゅ枑濞堢娀姊绘繝鍛彧闁?
				world.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
						SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 0.8f, 0.8f);
				// 婵炲濮存稉顢秎ayerEntityMinion缂備緡鍨靛畷鐢靛垝閾忓湱鐭撳┑鐘崇閻濈喐顨ラ悙鑼煟闁衡偓閵忕姌?
				MinionTracker.remove(player, MinionTracker.ANUBIS_WOLF, wolfUuid);
				wolf.discard();
			}
		}
	}

	/**
	 * 濠电偞鍨甸悧濠囧汲鎼淬劍鍋濋柍杞扮贰閸熲偓闂佸湱顣介崑鎾绘煛閸繍妲搁柣掳鍔岀叅闁煎鍊栭悾閬嶆煕閹扳晛鍔ら悗姘剁畺閺佸秹宕奸妷銏犱壕婵ê纾粻鏈揚layerEntityMinion缂備緡鍨靛畷鐢靛垝濞差亝鏅?
	 */
	private static void dissipateAllMinions(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		// 闂佸吋鍎抽崲鑼躲亹閸ヮ剙绠ラ柍褜鍓熷鍨緞婵犲倹娈犻梺缁樼懅鐏忔紛ID闂佸憡鎼╅崹鍗烇耿娴兼潙瑙﹂幖娣妷閸嬫捇骞囬鍫濇疂濠电偞鍨甸悧濠囧汲?
		List<UUID> minionUuids = new ArrayList<>(MinionTracker.get(player, MinionTracker.ANUBIS_WOLF));
		for (UUID wolfUuid : minionUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (entity instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()) {
				ParticleUtils.spawnParticles(world, ParticleTypes.SOUL,
						wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 12, 0.4, 0.6, 0.4, 0.03);
				wolf.discard();
			}
			MinionTracker.remove(player, MinionTracker.ANUBIS_WOLF, wolfUuid);
		}
	}

	private static int getMinionCount(ServerPlayer player) {
		return MinionTracker.count(player, MinionTracker.ANUBIS_WOLF);
	}

	/**
	 * 濠碘槅鍋€閸嬫捇鏌＄仦璇插姢閻㈩垵娅ｉ埀顒傤攰閸╂牕危閹间礁瑙﹂柨鏇氱贰濡懎顭跨捄鐑樼煑缂侇煉绻濆濂稿垂椤斿す娆愭叏閿濆棙鈷愰柡灞筋樀瀵爼宕橀埡鍌滅☉婵炴垶鎸搁敃锕€鈻撻幋婵愭僵閻庢稒蓱閻濈兘鏌ㄥ☉妯煎妞ゃ垹顑夊鎼佹嚋閸偓楠忛梺绋跨箳椤掕尙妲愭导瀛樺殜妞ゅ繐瀚闂備緡鍋勯崐鍧楀储?Trinkets/Curios闂?
	 */
	public static boolean hasTrinketEquipped(ServerPlayer player) {
		return TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.ANUBIS_CRYSTAL.get());
	}

	/** Soul sand lowers the anchor into its block; do not choose air below a thin floor. */
	public static BlockPos summonPosition(ServerLevel world, ServerPlayer owner, Entity anchor) {
		BlockPos candidate = MinionTracker.nearbyEmptySpace(world, owner.getRandom(), anchor.blockPosition(), 3, 1, 1, 4);
		if (candidate != null && (world.getFluidState(candidate).is(net.minecraft.tags.FluidTags.WATER)
				|| !world.getBlockState(candidate.below()).getCollisionShape(world, candidate.below()).isEmpty())) {
			return candidate;
		}
		return BlockPos.containing(anchor.getX(), Math.ceil(anchor.getY()), anchor.getZ());
	}

	// ==================== 闁哄鐗嗛幊搴㈡叏椤忓牆妫橀悷娆忓閵?====================

	private static void applyHowlSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null && speedAttr.getModifier(HOWL_SLOW_UUID) == null) {
			speedAttr.addTransientModifier(new AttributeModifier(
					HOWL_SLOW_UUID, "howl_slow",
					BAL.d("howl_slow_factor", HOWL_SLOW_FACTOR) - 1.0, // -0.5 = 婵烇絽娲︾换鍕汲閳?0%
					AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	private static void removeHowlSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(HOWL_SLOW_UUID);
		}
	}

	/**
	 * 闂佸憡绋忛崝宥咃耿閿熺姴绠戠憸搴∶洪崸妤€妫橀柣妤€鐗嗛。鏌ユ煛閸繍妲搁柛鐕傜秮閹亪鎳栭埡鍌滄殸婵°倖顨嗗姗€骞栬娴狅箓鎮欑划鐟颁壕鐟滃矂骞堥妸锔绘闁规鍣崕?
	 * 闂備緡鍓欑粔鏉戭啅婵犳碍鍋濋柍杞扮贰閸熲偓闂佸湱顣介崑鎾绘煛閸繍妲搁柛鐕傜秮閹亞娑垫搴ｎ槷闂佸搫绉烽～澶婄暤娴ｈ娈伴柡澶嬪閹倿鎮锋担鍛婂櫣妞わ富鍓熼幃鈺呮嚋绾版ê浜惧ù锝囨嚀濞兼垿鏌?缂備礁顦…宄扳枍?25%闂佽　鍋撻悹鍥ㄥ絻濮ｅ﹪鏌涢弮鈧粙鎴﹀箯?35%闁荤偞绋堥崑鎾绘⒑閹绘帞绠ｉ柟渚垮妽椤ㄦ粓骞嶉鍛墾
	 * 婵炴垶鎸诲Σ鎺椼€呴敂鐐暫闁糕剝鐟︾壕浼存煕濞嗘劦娈旈柛?闂佽　鍋撻悹鍥ㄥ絻濮ｅ﹪鏌￠崘銊ヮ暢闁轰胶娲昐ON power闂佸憡鐟崜娆撳极濠婂牊鍎嶉柛鏇ㄥ亜閺嗗酣鏌ｅ▎鎴欌偓瀣濞嗘垟鍋撻悷鏉挎殨婵犲﹥鍨剁粙澶婎吋閸モ晝姊鹃柡澶嗘櫅濡剧倍awnMinionWolf闂?
	 */
	private static void tickTrinketModifiers(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		boolean hasCrystal = hasTrinketEquipped(player);

		List<UUID> wolfUuids = new ArrayList<>(MinionTracker.get(player, MinionTracker.ANUBIS_WOLF));
		for (UUID wolfUuid : wolfUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (!(entity instanceof AnubisWolfMinionEntity wolf) || !wolf.isAlive()) continue;

			// 闂佽　鍋撻悹鍥ㄥ絻濮ｅ﹪鏌涢弮鈧粙鎰板箞閵婏富娈伴柟娈垮櫘閸?
			AttributeInstance attackAttr = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attackAttr != null) {
				AttributeModifier dmgMod = attackAttr.getModifier(TRINKET_DAMAGE_UUID);
				if (hasCrystal && dmgMod == null) {
					attackAttr.addPermanentModifier(new AttributeModifier(
							TRINKET_DAMAGE_UUID, "trinket_damage_reduction",
							TRINKET_DAMAGE_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
				} else if (!hasCrystal && dmgMod != null) {
					attackAttr.removeModifier(TRINKET_DAMAGE_UUID);
				}
			}

			// 闁荤偞绋堥崑鎾绘⒑閹绘帞绠ｉ柟渚垮妽椤ㄦ粓骞嶉鍛墾
			AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
			if (healthAttr != null) {
				AttributeModifier hpMod = healthAttr.getModifier(TRINKET_HEALTH_UUID);
				if (hasCrystal && hpMod == null) {
					healthAttr.addPermanentModifier(new AttributeModifier(
							TRINKET_HEALTH_UUID, "trinket_health_reduction",
							TRINKET_HEALTH_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
					wolf.setHealth(Math.min(wolf.getHealth(), (float) healthAttr.getValue()));
				} else if (!hasCrystal && hpMod != null) {
					healthAttr.removeModifier(TRINKET_HEALTH_UUID);
				}
			}
		}
	}

	// ==================== 闂傚倸鍟抽崺鏍敊瀹€鍕嚑婵﹩鍋勯々?====================
	private enum Phase {
		HOWLING,     // 闂佹悶鍊曟姝屻亹閵娾晜瀚呴柛鏇ㄥ亜椤柨鈽夐幙鍐ㄥ伎缂?.5缂備礁顦扮敮顏嗘?
		SUMMONING,   // 婵炲瓨鍤庨崐婵嬪极婵犲洤鐭楁い鎺嗗亾闁轰焦绮嶇粙?
		ACTIVE,      // 闂佸憡鍔戦崕宕団偓姘劤鐓ら悹鍥ㄥ絻琚熸繛鎴炴惄閸欏繒妲?0缂備礁顦扮敮顏嗘?
	}

	// ==================== 闂佽桨鑳舵晶妤€鐣垫担铏瑰暗?====================
	private static class SummonData {
        long castId = -1;
		Phase phase;
		int ticksElapsed;
		int wolvesToSummon;     // 闂佸搫鐗滈崜姘额敃婵傚憡顥嗛柍褜鍓涢幉鐗堟媴缁嬪灝鐨㈤梺鍝ュ亼閸婃鈻撻幋锕€绠戠紓浣股戝▓?
		int wolvesSummoned;     // 閻庣懓鎲¤ぐ鍐亹椤愶箑鑸归柕鍫濇处閻ｉ亶鏌℃担宄板祮闁?
		boolean domainActive;   // 闂佸憡鐟崜娆撳极濠婂牆绫嶉柤绋跨仛绗戦梺鍛婄啲闂勫嫬锕㈡担绯曟斀闁逞屽墮鐓ら柤濮愬€栭悾杈ㄦ叏濠靛鍞哄Δ鐘叉啞閿涙劙宕卞Δ鍐ㄥ箥
		boolean enhancedDomain; // 闂佸搫瀚烽崹浼村箚娴ｅ湱鈻旈柛婵嗗閺夎崵鈧鍠氶崕銈夘敆鐎涙顩查柨鐔哄閺嗘瑩鏌涢埡鍐ㄦ瀻鐟滈偊鍋婂畷顒勫Χ韫囨洜顦╂繛鎴炴尭缁夐潧螣婢跺瞼鐭嗛柧蹇涒偓娑氳Е婵帒瀚崐鎼佸疮鐎ｎ喖绀勯柧蹇撴贡閻撳牓鎮楀☉娆忓Ё缂?
		List<UUID> summonedWolfUuids = new ArrayList<>(); // 閻熸粎澧楅幐鍛婃櫠閻樿绠ラ悷娆忓閸嬔囨煕濞嗘ê澧柡浣圭矒閹啴宕熼鐑嗕紳UUID

		SummonData(int wolvesToSummon, boolean domainActive) {
			this.phase = Phase.HOWLING;
			this.ticksElapsed = 0;
			this.wolvesToSummon = wolvesToSummon;
			this.wolvesSummoned = 0;
			this.domainActive = domainActive;
			this.enhancedDomain = false;
		}
	}
}
