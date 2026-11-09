package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.SpiderSalticidaePerks;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 璺宠洓銆岃烦鏉€锛圝ump Kill锛夈€嶄富鎶€鑳斤紙sp_primary锛夆€斺€?鏈嶅姟绔姸鎬佹満銆?
 *
 * <p><b>钃勫姏</b>锛堥暱鎸?sp_primary锛夛細钃勫姏瓒婁箙绱㈡晫璺濈瓒婂ぇ锛堝熀纭€ {@value #BASE_DIST} 鏍?/
 * 婊¤搫 {@value #CHARGE_MAX}t=3 绉?鈫?{@value #MAX_DIST} 鏍硷級锛涜搫鍔涙湡绉婚€熷噺鍗娿€佽浆瑙嗚鐬勫噯锛?
 * 姣?tick 鎵弿鍓嶆柟瑙嗛敟鍐呭彲鎵戠洰鏍囷細鍏ㄩ儴<b>绾㈣壊鎻忚竟</b>銆佸噯鏄熸渶鎺ヨ繎鑰?b>缁胯壊鎻忚竟</b>锛堜粎鏈汉鍙锛夈€?/p>
 *
 * <p><b>璺虫潃</b>锛堟澗寮€锛夛細浠ョ豢鑹查攣瀹氱洰鏍囷紙鏃犲垯鎸夊噯鏄熻惤鐐癸級鍚戝墠璺虫墤鈥斺€攞@code setNoGravity} + 姣?tick
 * 鏂芥寚鍚戠洰鏍囩殑銆屾媺鍔涖€嶈鎷借繃鍘伙紙绗叚鏉★級锛岃窛绂昏秺杩滆捣璺宠秺楂橈紙绗簲鏉★級銆傚懡涓€犳垚 {@value #DAMAGE}
 * 鐗╃悊 + 涓瘨 II {@value #POISON_DURATION}t(8s) + 瀹氳韩 {@value #STUN_DURATION}t(0.35s)銆?/p>
 *
 * <p><b>绌轰腑杩借釜锛堢涓冩潯锛?/b>锛氳拷韪椂鎵戝悜鐩爣<b>鎻愬墠閲忔嫤鎴偣</b>锛堝綋鍓嶄綅缃?+ 閫熷害脳{@value #LEAD_TICKS}锛夛紝
 * 鍔犲ぇ鏇存鑼冨洿锛涜劚閿佸宸互銆岃捣璺虫椂閿佸畾浣嶇疆 lockPos0銆嶄负鍦嗗績銆佸崐寰?= {@value #LEASH_K}脳璺宠穬璺濈
 * 锛堣烦瓒婅繙瀹瑰樊瓒婂ぇ銆佽秺杩戣秺灏忥級锛涚洰鏍囩Щ鍑哄宸嵆鑴遍攣銆佹寜鏈€鍚庢柟鍚戠洿椋炴墤绌恒€?/p>
 *
 * <p><b>CD</b>锛氭墤涓寜 power JSON cooldown銆佹墤绌烘寜 fail_cooldown銆?
 * 鐧藉悕鍗曪細榛樿鐧藉悕鍗曪紙鎶ょ帺瀹?+ 瀹犵墿/鍙敜鐗╋級銆傚叏鍒ゅ畾鏈嶅姟绔紝璺宠穬姣?tick 琛ュ彂閫熷害鍖呯粰瀹㈡満銆?/p>
 */
public final class JumpKillManager {

	// 浠ヤ笅宸茬櫥璁板父閲忓潎涓洪粯璁ゅ€硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.jump_kill锛?
	private static final int CHARGE_MAX = 60;        // 婊¤搫 3 绉?
	private static final double BASE_DIST = 3.0;      // 鍩虹绱㈡晫 3 鏍?
	private static final double MAX_DIST = 16.0;      // 婊¤搫绱㈡晫 16 鏍?
	private static final float DAMAGE = 8.0f;
	private static final int POISON_DURATION = 160;  // 涓瘨 II 8 绉?
	private static final int POISON_AMPLIFIER = 1;   // 涓瘨 II
	private static final int STUN_DURATION = 7;      // 瀹氳韩 0.35 绉?
	private static final String SKILL_ID = "my_addon:form_spider_salticidae_jump_kill";
	private static final double CHARGE_SLOW = -0.5;  // 钃勫姏绉婚€?脳0.5
	private static final double LEASH_K = 0.4;       // 鑴遍攣瀹瑰樊绯绘暟锛堝崐寰?= LEASH_K 脳 璺宠穬璺濈锛?
	private static final int LEAD_TICKS = 6;         // 鎻愬墠閲忥細鎵戝悜 鐩爣浣嶇疆 + 閫熷害 脳 LEAD_TICKS
	private static final double LEAP_SPEED = 0.85;   // 璺宠穬姘村钩鎷夊姏閫熷害
	private static final int MAX_LEAP_TICKS = 40;    // 璺宠穬纭笂闄?2 绉?
	private static final double HIT_RADIUS = 1.3;    // 鍛戒腑鍒ゅ畾璺濈
	private static final double SCAN_COS = 0.978;    // 閿佸畾瑙嗛敟鍗婅 鈮?12掳锛堝彧閿佸噯鏄熼檮杩戠洰鏍囷級
	private static final double GRAVITY_STEP = 0.08; // 璺宠穬鏈熸墜鍔ㄦ姏鐗╋細姣?tick 绔栫洿閫熷害琛板噺锛? 閲嶅姏鍔犻€熷害锛?
	private static final double TURN_LERP = 0.3;     // 姘村钩杞悜骞虫粦绯绘暟锛堥槻鎺ヨ繎鐩爣鏃跺乏鍙虫憜鍔級
	private static final int HIGHLIGHT_TICKS = 15;   // 鎻忚竟鏃堕暱锛堢暀鍒锋柊浣欓噺闃查棯鐑侊級
	private static final int CHARGE_CRY_COUNT = 5;   // 钃勫姏鏈熻湗铔涘樁楦ｆ€绘鏁帮紙浠呰嚜宸卞惉锛?
	private static final int COLOR_RED = 0xFF3030;   // 鍊欓€夌洰鏍囩孩杈?
	private static final int COLOR_GREEN = 0x30FF30; // 閿佸畾鐩爣缁胯竟
	// ===== 瀹夊叏涓濓紙淇濆懡鎷夊洖锛?=====
	private static final double SILK_MAX = 48.0;     // 涓濈嚎鏈€闀?48 鏍硷紝瓒呰繃鍗虫柇
	private static final int RECALL_WINDOW = 160;    // 璺虫潃缁撴潫鍚?8 绉掑唴鍙媺鍥?
	private static final double RECALL_SPEED = 1.15; // 鎷夊洖閫熷害锛堟瘮璺宠穬蹇紝蹇€熸嫿鍥烇級
	private static final double RECALL_ARRIVE = 1.6; // 鎷夊洖鍒拌揪闃堝€硷紙璺濋敋鐐瑰皬浜庡嵆鍋滐級
	private static final int RECALL_STALL_TICKS = 5; // 鎷夊洖杩炵画 5t 璺濈涓嶆媺杩?鈫?澶栧姏杩囧己锛屼笣鏂?
	private static final double OBSCURE_BREAK_BLOCKS = 1.5; // 涓濈嚎琚疄蹇冩柟鍧楅伄鎸＄疮璁￠槇鍊硷紙閲囨牱 0.5 鏍?涓細鍏佽绌?1.5 鏍煎帤澧欙紝鈮? 鏍兼柇锛?

	private static final UUID SLOW_UUID = UUID.fromString("b7e2c9a4-3f81-4d6e-9a25-7c1e0f4d82ab");

	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final BalanceReader BAL = new BalanceReader("abilities.jump_kill");

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	/** 瀹夊叏涓濇媺鍥炵獥鍙ｏ細璺虫潃缁撴潫鍚庨敋鐐逛繚鐣欑殑鏃舵锛堣秴鏃跺嵆澶辨晥锛夈€?*/
	private static final Map<UUID, SilkAnchor> ANCHORS = new ConcurrentHashMap<>();

	private static final class SilkAnchor {
		final Vec3 pos;        // 涓濈嚎閿氱偣锛堣捣璺崇偣锛?
		final long validUntil;  // 涓栫晫鏃堕棿锛氳秴杩囧嵆澶辨晥
		double lastDist = -1;   // 鎷夊洖鏈燂細涓?tick 鍒伴敋鐐硅窛绂伙紙鏂笣鍒ゅ畾鐢級
		int stall = 0;          // 鎷夊洖鏈燂細璺濈鏈媺杩戣繛缁?tick 鏁?
		SilkAnchor(Vec3 pos, long validUntil) { this.pos = pos; this.validUntil = validUntil; }
	};

	private static final class State {
        long castId = -1;
		int chargeMax = CHARGE_MAX;
		float killHealing;
		int strengthTicks;
		int phase = 0;        // 0=钃勫姏 1=璺宠穬 2=鎷夊洖
		int chargeTick = 0;
		// 璺宠穬鏈?
		int leapTick = 0;
		UUID lockTarget;      // 璺宠穬閿佸畾鐩爣锛坣ull=绾綅绉伙級
		Vec3 lockPos0;       // 璧疯烦鏃堕攣瀹氱洰鏍囦綅缃紙鑴遍攣瀹瑰樊鍦嗗績锛?
		double jumpDist;      // 璧疯烦鏃跺埌鐩爣鐨勮窛绂伙紙鍐冲畾璺抽珮 + 瀹瑰樊锛?
		double flightTime;    // 棰勮椋炶 tick锛堝崗璋冩姏鐗╃嚎钀界偣锛?
		Vec3 lastDir = new Vec3(0, 0, 1); // 姘村钩鎷夊姏鏂瑰悜锛堝崟浣嶅悜閲忥紝y=0锛涘钩婊戞彃鍊艰拷韪級
		double vy0;           // 璧疯烦绔栫洿鍒濋€燂紙鍗忚皟鎶涚墿锛歠lightTime 鍚庣珫鐩村埌鐩爣楂樺害锛?
	}

	private JumpKillManager() {}

	private static boolean isSalticidae(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE);
	}

	/** 瀹㈡埛绔寜涓?sp_primary锛氫紭鍏堝垽銆屽畨鍏ㄤ笣鎷夊洖銆嶏紙6s 绐楀彛鍐咃紝缁曡繃 CD鈥斺€旇烦鏉€ CD 8~12s 蹇呯劧瑕嗙洊绐楀彛锛夛紱鍚﹀垯寮€濮嬭搫鍔涖€?*/
	public static void startCharge(ServerPlayer player) {
		if (!isSalticidae(player)) return;
		// 瀹夊叏涓濇媺鍥烇細璺虫潃缁撴潫鍚?6s 鍐呭啀鎸変富閿?鈫?蹇€熸嫿鍥為敋鐐癸紙涓嶈繘钃勫姏銆佷笉鍙?CD 闃绘尅锛?
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor != null) {
			if (player.serverLevel().getGameTime() > anchor.validUntil
					|| player.position().distanceTo(anchor.pos) > SILK_MAX) {
				// 绐楀彛瓒呮椂 / 璺濋敠鐐硅繃杩滐細涓濆凡鏂紙甯︽柇涓濈壒鏁堬級
				if (player.level() instanceof ServerLevel sw0) snapSilk(player, sw0, anchor);
				ANCHORS.remove(player.getUUID());
				SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
			} else {
				startRecall(player, anchor);
				return;
			}
		}
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return; // CD 涓?
		if (STATES.containsKey(player.getUUID())) return; // 鏂芥硶涓笉鍙噸鍏?
		if (!SpiderSalticidaePerks.beginSkill(player, SKILL_ID, false)) return;
        State s = new State();
        s.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		s.chargeMax = SpiderSalticidaePerks.chargeTicks(player, BAL.i("charge_max", CHARGE_MAX));
		s.killHealing = SpiderSalticidaePerks.killHealing(player);
		s.strengthTicks = SpiderSalticidaePerks.strengthTicks(player);
		STATES.put(player.getUUID(), s);
		SpiderSalticidaePerks.setSensing(player, s.strengthTicks > 0);
		applyChargeSlow(player);
		if (player.level() instanceof ServerLevel sw) {
			// 钃勫姏璧锋墜锛氫綆娌夎湗铔涘樁楦ｏ紙钃勫娍锛?
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);
		}
	}

	/** 瀹㈡埛绔澗寮€ sp_primary 鈫?璺虫潃銆?*/
	public static void release(ServerPlayer player) {
		State s = STATES.get(player.getUUID());
		if (s == null || s.phase != 0) return;
		SpiderSalticidaePerks.setSensing(player, false);
		removeChargeSlow(player);
		if (!(player.level() instanceof ServerLevel sw)) { STATES.remove(player.getUUID()); return; }

		int chargeMax = s.chargeMax;
		double baseDist = BAL.d("base_dist", BASE_DIST);
		double maxDist = BAL.d("max_dist", MAX_DIST);
		double leapSpeed = BAL.d("leap_speed", LEAP_SPEED);
		double ratio = Math.min(1.0, (double) s.chargeTick / chargeMax);
		double scanDist = baseDist + ratio * (maxDist - baseDist);

		// 鏉惧紑鐬棿鎸夊綋鍓嶅噯鏄熼噸鏂伴攣瀹氾紙鍑嗘槦鏈€鎺ヨ繎鐨勫€欓€夛級
		LivingEntity lock = pickLockTarget(player, sw, scanDist);

		s.phase = 1;
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID, s.castId);
		s.leapTick = 0;
		Vec3 look = player.getLookAngle().normalize();
		s.lastDir = look;

		// 瀹夊叏涓濓細閿氱偣蹇呴』钀藉湪瀹炲湴鈥斺€斿悜涓嬫娴?2 鏍煎唴鐨勯涓疄蹇冩柟鍧楅《闈紙鑴氱鍦?2 鏍煎唴璧疯烦 鈫?涓濊浆绉诲埌鍦伴潰锛涜秴杩?2 鏍肩┖涓?鈫?涓嶇敓鎴愶級
		Vec3 anchorPos = groundAnchor(player);
		if (anchorPos != null) {
			SilkAnchor anchor = new SilkAnchor(anchorPos, player.serverLevel().getGameTime() + RECALL_WINDOW);
			ANCHORS.put(player.getUUID(), anchor);
			SscAddonNetworking.syncJumpKillSilk(player, true, anchorPos.x, anchorPos.y, anchorPos.z, RECALL_WINDOW);
		}

		if (lock != null) {
			s.lockTarget = lock.getUUID();
			s.lockPos0 = bodyCenter(lock);
			s.jumpDist = player.getEyePosition().distanceTo(bodyCenter(lock));
		} else {
			s.lockTarget = null;
			s.lockPos0 = null;
			s.jumpDist = scanDist;
		}

		PlayerGravityLease.acquire(player, SKILL_ID);
		player.fallDistance = 0.0f;
		// 鍗忚皟鎶涚墿绾胯捣璺筹細璁＄畻浣挎姏鐗╃嚎 flightTime 鍚庢按骞?绔栫洿鍚屾椂鎶佃揪钀界偣锛堣建杩规祦鐣咃紝璺濈瓒婅繙鎶涚墿瓒婇珮锛?
		Vec3 aim = (lock != null) ? bodyCenter(lock) : player.getEyePosition().add(look.scale(s.jumpDist));
		Vec3 horiz = new Vec3(aim.x - player.getX(), 0, aim.z - player.getZ());
		double horizDist = horiz.length();
		if (horizDist < 1.0e-4) { horiz = new Vec3(look.x, 0, look.z); horizDist = Math.max(1.0e-4, horiz.length()); }
		horiz = horiz.normalize();
		double dy = aim.y - player.getY();
		double T = Math.max(6.0, horizDist / leapSpeed);
		s.flightTime = T;
		s.vy0 = dy / T + 0.5 * GRAVITY_STEP * T; // T tick 鍚庣珫鐩村埌杈?aim.y锛汿 瓒婂ぇ锛堣窛绂昏秺杩滐級璧疯烦瓒婇珮
		s.lastDir = horiz;
		player.setDeltaMovement(horiz.x * leapSpeed, s.vy0, horiz.z * leapSpeed);
		player.hurtMarked = true;
		pushVelocity(player);

		// 璧疯烦闊虫晥锛氳湗铔涙墤鍑诲樁楦?+ 鐮寸┖
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.8f);
	}

	/** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢ㄣ€?*/
	public static void tick(ServerPlayer player) {
		State s = STATES.get(player.getUUID());
		SpiderSalticidaePerks.setSensing(player, s != null && s.phase == 0 && s.strengthTicks > 0);
		if (s == null) return;
		if (player.isDeadOrDying() || !isSalticidae(player)) { cancel(player); return; }
		if (!(player.level() instanceof ServerLevel sw)) return;

		if (s.phase == 0) {
			tickCharge(player, sw, s);
		} else if (s.phase == 1) {
			checkSilk(player); // 璺宠穬鏈熶笣绾挎鏌ワ細瓒?32 鏍?/ 琚伄鎸?鈫?涓濇柇锛堟媺鍥炵獥鍙ｄ綔搴燂級
			if (STATES.containsKey(player.getUUID())) tickLeap(player, sw, s); // 涓濇柇涓嶆竻璺宠穬鎬侊紝浠呬綔搴熼敠鐐?
		} else {
			tickRecall(player, sw, s);
		}
	}

	/** 閿︾偣瀛樺湪 tick锛氱函閿︾偣锛堟棤娲诲姩鐘舵€侊級涔熻鎺ㄨ繘 8 绉掑€掕鏃垛€斺€斿埌鏃朵笣绾挎柇瑁傛秷澶憋紙甯︾壒鏁堬級銆?*/
	public static void tickAnchors(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor == null) return;
		State s = STATES.get(player.getUUID());
		if (s != null && (s.phase == 1 || s.phase == 2)) return; // 璺宠穬/鎷夊洖涓細绐楀彛鐢?finishRecall 閲嶇疆璁℃椂
		if (player.serverLevel().getGameTime() > anchor.validUntil) {
			if (player.level() instanceof ServerLevel sw) snapSilk(player, sw, anchor);
			ANCHORS.remove(player.getUUID());
			SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0); // 涓濈嚎鍒版椂鏂锛堝鎴风鍚屾绉婚櫎锛?
		}
	}

	/** 寮€濮嬪畨鍏ㄤ笣鎷夊洖锛氶敋鐐逛粠绐楀彛琛ㄨ浆鍏ユ椿鍔ㄧ姸鎬侊紝phase=2銆?*/
	private static void startRecall(ServerPlayer player, SilkAnchor anchor) {
		SpiderSalticidaePerks.setSensing(player, false);
        State s = new State();
        State previous = STATES.get(player.getUUID());
        if (previous != null) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, previous.castId);
		s.phase = 2;
		STATES.put(player.getUUID(), s);
		PlayerGravityLease.acquire(player, SKILL_ID);
		player.fallDistance = 0.0f;
		if (player.level() instanceof ServerLevel sw) {
			// 鎷夊洖璧锋墜锛氳洓楦?+ 涓夊弶鎴熸嫋鎷藉０
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.9f, 1.3f);
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.9f, 1.5f);
		}
	}

	/** 鎷夊洖鎺ㄨ繘锛氭瘡 tick 鍚戦敠鐐圭洿绾挎嫿锛堜笁缁寸洿绾?+ 璺濈姣斾緥鍑忛€燂級锛岃澶栧姏鍘嬭繃鎷変笉杩戯紙杩炵画 5t 璺濈涓嶇缉鐭級鎴栨挒澧?鈫?涓濇柇鏀惧純銆?*/
	private static void tickRecall(ServerPlayer player, ServerLevel sw, State s) {
		s.leapTick++;
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor == null) { finishRecall(player, false); return; }
		if (s.leapTick > 60) { finishRecall(player, true); return; } // 纭笂闄?3s

		double dist = player.position().distanceTo(anchor.pos);
		// 鏂笣鍒ゅ畾锛氭挒澧欏崱浣?/ 杩炵画澶?tick 璺濈涓嶇缉鐭紙澶栧姏杩囧己鎷戒笉鍥炴潵锛? 涓濈嚎琚伄鎸?
		if (s.leapTick > 3 && player.horizontalCollision) { snapSilk(player, sw, anchor); finishRecall(player, false); return; }
		if (computeObscuration(player, bodyCenter(player), anchor.pos) > OBSCURE_BREAK_BLOCKS) {
			snapSilk(player, sw, anchor); finishRecall(player, false); return;
		}
		if (anchor.lastDist >= 0) {
			if (dist >= anchor.lastDist - 0.01) {
				anchor.stall++;
				if (anchor.stall >= RECALL_STALL_TICKS) { snapSilk(player, sw, anchor); finishRecall(player, false); return; }
			} else {
				anchor.stall = 0;
			}
		}
		anchor.lastDist = dist;

		// 鍒拌揪閿︾偣闄勮繎 鈫?鎷夊洖鎴愬姛锛堟竻姘村钩閫熷害闃插啿杩囧ご涔辨檭锛?
		if (dist < RECALL_ARRIVE) {
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * 0.1, v.y, v.z * 0.1);
			player.hurtMarked = true;
			finishRecall(player, true);
			return;
		}

		// 鎷藉悜閿︾偣锛氫笁缁寸洿绾?+ 璺濈姣斾緥鍑忛€燂紙杩滃蹇€佷复杩戣嚜鍔ㄥ埞杞︼紝涓嶅啿杩囬敠鐐逛笉涔辨檭锛?
		Vec3 to = anchor.pos.subtract(player.position());
		Vec3 dir3 = to.normalize();
		double speed = Math.min(RECALL_SPEED, dist / 3.0 + 0.25); // 涓磋繎鑷姩闄嶉€燂紙鍓?1 鏍?鈮?0.58/t锛?
		speed = Math.max(0.3, speed);
		player.setDeltaMovement(dir3.x * speed, Math.max(dir3.y * speed, -0.5), dir3.z * speed);
		player.hurtMarked = true;
		player.fallDistance = 0.0f;
		pushVelocity(player);

		// 涓濈嚎瑙嗚锛氭部绾挎瘡 2 鏍间竴涓洓缃戠矑瀛愶紙鎷夊洖杩囩▼鐭殏锛屽紑閿€鍙帶锛? 灏捐抗
		Vec3 dir = anchor.pos.subtract(player.position());
		double len = dir.length();
		if (len > 1.0) {
			dir = dir.normalize();
			int segs = (int) (len / 2.0);
			for (int i = 1; i <= segs; i++) {
				Vec3 p = player.position().add(dir.scale(i * 2.0));
				sw.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, p.x, p.y + 0.4, p.z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** 涓濇柇鍙嶉锛氱怀缁撴柇瑁傞煶 + 鐧借壊涓濈嚎绮掑瓙銆?*/
	private static void snapSilk(ServerPlayer player, ServerLevel sw, SilkAnchor anchor) {
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.9f, 1.4f);
		// owner 鎵撴爣锛氫粎鏈汉绗竴浜虹О閬胯锛屼粬浜鸿瑙掑師鏍?
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY(0.5), player.getZ(),
				10, 0.2, 0.2, 0.2, 0.02);
	}

	/**
	 * 钀藉湴閿氱偣锛氫粠鐜╁鑴氶儴鍚戜笅鎵?2 鏍硷紝杩斿洖棣栦釜銆屽彲绔欑珛鏂瑰潡銆嶏紙鏈夊褰㈣疆寤撳嵆鍙€斺€?
	 * 鏍忔潌/鏍呮爮/鍗婄爾/鍙伴樁/闂ㄧ瓑闈炲疄蹇冧絾鍙鎾炲潎鍙級鐨勫潗鏍囷紱
	 * 绌轰腑锛堣剼涓?2 鏍兼棤鍦帮級杩斿洖 null = 涓嶇敓鎴愯洓涓濓紙鑴氱鍦?2 鏍煎唴 鈫?涓濊浆绉诲埌鍦伴潰锛涜秴杩?2 鏍?鈫?娑堝け锛夈€?
	 */
	private static Vec3 groundAnchor(ServerPlayer player) {
		BlockPos feet = player.blockPosition();
		for (int dy = 0; dy <= 2; dy++) { // 0锛堣剼閮級銆?1銆?2 涓夋。锛氳鐩?2 鏍兼娴?
			BlockPos check = feet.below(dy);
			if (isStandable(player, check)) {
				return new Vec3(check.getX() + 0.5, check.getY() + 1.0, check.getZ() + 0.5); // 鏂瑰潡椤堕潰涓績
			}
		}
		// 鑴氶儴銆佷笅涓€鏍笺€佷笅涓ゆ牸閮界┖锛氬啀绮剧‘妫€娴嬪崐鏍硷紙鐜╁鍙兘绔欏湪鏂瑰潡杈圭紭锛岃剼閮?y 鍋忎笂锛?
		BlockPos half = BlockPos.containing(player.getX(), player.getY() - 2.5, player.getZ());
		if (isStandable(player, half)) {
			return new Vec3(half.getX() + 0.5, half.getY() + 1.0, half.getZ() + 0.5);
		}
		return null; // 绌轰腑锛氭棤鍦板彲閿氾紝涓嶇敓鎴愪笣
	}

	/** 鍙珯绔嬪垽瀹氾細鏂瑰潡澶栧舰杞粨闈炵┖锛堟爮鏉?鏍呮爮/鍗婄爾/鍙伴樁/闂ㄧ瓑鏈夌鎾為潰鍗崇畻锛岀┖姘?鑽変笡涓嶇畻锛夈€?*/
	private static boolean isStandable(ServerPlayer player, BlockPos pos) {
		net.minecraft.world.level.block.state.BlockState state = player.level().getBlockState(pos);
		return !state.getShape(player.level(), pos).isEmpty();
	}

	/** 鎷夊洖缁撴潫锛氭仮澶嶉噸鍔涖€佹竻閿︾偣锛堟棤璁烘垚璐ワ紝鎷夊洖鏈轰細鍙敤涓€娆★紱涓濈嚎鑰楀敖涔熸挱鏂笣鐗规晥锛夈€?*/
	private static void finishRecall(ServerPlayer player, boolean arrived) {
		PlayerGravityLease.release(player, SKILL_ID);
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (player.level() instanceof ServerLevel sw) {
			if (anchor != null) snapSilk(player, sw, anchor); // 涓濈嚎鎷夊畬鑰楀敖锛氬悓鏍锋柇瑁傚弽棣?
			if (arrived) {
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.GOAT_LONG_JUMP, SoundSource.PLAYERS, 0.7f, 1.2f);
			}
		}
		ANCHORS.remove(player.getUUID());
		SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
		STATES.remove(player.getUUID());
	}

	private static void tickCharge(ServerPlayer player, ServerLevel sw, State s) {
		int chargeMax = s.chargeMax;
		boolean justReachedMax = s.chargeTick == chargeMax - 1; // 涓嬩竴琛岄€掑鍚庢伆濂芥弧钃勶紙涓€娆℃€ц竟娌匡紝闃叉弧钃勫悗姣?tick 閲嶅搷锛?
		if (s.chargeTick < chargeMax) s.chargeTick++;
		double ratio = Math.min(1.0, (double) s.chargeTick / chargeMax);
		double scanDist = BAL.d("base_dist", BASE_DIST) + ratio * (BAL.d("max_dist", MAX_DIST) - BAL.d("base_dist", BASE_DIST));

		// 鎵弿鍊欓€夛細绱㈡晫璺濈鍐呭叏閮ㄥ彲鎵戠洰鏍囷紙鏃犻伄鎸℃墠鏄剧ず/鍙攣锛夆€斺€旂孩杈癸紱鍑嗘槦 12掳 閿ュ唴鏈€杩戣€呯豢杈归攣瀹?
		List<LivingEntity> cands = scanCandidates(player, sw, scanDist);
		LivingEntity best = pickInCrosshair(player, cands);

		for (LivingEntity e : cands) {
			int color = (e == best) ? COLOR_GREEN : COLOR_RED;
			SscAddonNetworking.sendWebHighlight(player, e.getId(), HIGHLIGHT_TICKS, color);
		}

		// 钃勫姏闊虫晥锛氬叏绋嬪叡 5 澹颁笂鍗囧樁楦ｏ紙浠呰嚜宸卞惉锛夛紝闂撮殧鍧囧垎锛涙弧钃勭灛闂撮澶栦竴澹扮粡楠屽彯澹帮紙杈规部瑙﹀彂锛岄槻婊¤搫鍚庡惊鐜搷锛?
		int interval = Math.max(1, chargeMax / CHARGE_CRY_COUNT);
		boolean cryNow = s.chargeTick < chargeMax && s.chargeTick % interval == 0 && s.chargeTick > 0;
		if (cryNow) {
			float pitch = 0.6f + (float) ratio * 0.8f;
			github.com.gengyoubo.sscfa.ability.MancianimaMarkManager
					.playSoundToPlayer(player, SoundEvents.SPIDER_AMBIENT, 0.5f, pitch);
		}
		if (justReachedMax) {
			github.com.gengyoubo.sscfa.ability.MancianimaMarkManager
					.playSoundToPlayer(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f);
		}
	}

	private static void tickLeap(ServerPlayer player, ServerLevel sw, State s) {
		s.leapTick++;
		// 瓒呮椂鍜屾挒澧欑珛鍗崇粨鏉燂紱钀藉湴鍓嶅厛鏍稿鍛戒腑锛岄伩鍏嶅悓 tick 鐨勬湁鏁堟帴瑙﹁鍒ゆ垚鎵戠┖銆?
		if (s.leapTick > s.flightTime + 15 || s.leapTick > BAL.i("max_leap_ticks", MAX_LEAP_TICKS)
				|| (s.leapTick > 3 && player.horizontalCollision)) {
			finish(player, false);
			return;
		}

		Vec3 playerPos = bodyCenter(player);
		Vec3 targetPoint;

		LivingEntity target = (s.lockTarget != null) ? resolve(sw, s.lockTarget) : null;
		if (target != null && target.isAlive()) {
			// 鑴遍攣鍒ゅ畾锛氱洰鏍囧亸绂昏捣璺抽攣瀹氫綅缃秴杩囧宸紙鍗婂緞闅忚烦璺冭窛绂荤嚎鎬э級
			double leash = LEASH_K * s.jumpDist;
			if (bodyCenter(target).distanceTo(s.lockPos0) > leash) {
				s.lockTarget = null; // 鑴遍攣 鈫?娌挎渶鍚庢柟鍚戠洿椋炴墤绌?
				targetPoint = playerPos.add(s.lastDir.scale(10));
			} else {
				// 鎻愬墠閲忔嫤鎴偣锛堢涓冩潯锛夛細鎵戝悜 鐩爣褰撳墠浣嶇疆 + 閫熷害 脳 LEAD_TICKS
				targetPoint = bodyCenter(target).add(target.getDeltaMovement().scale(LEAD_TICKS));
				// 鍛戒腑鍒ゅ畾
				if (bodyCenter(target).distanceTo(playerPos) < BAL.d("hit_radius", HIT_RADIUS)) {
					onHit(player, sw, target);
					finish(player, true);
					return;
				}
			}
		} else {
			s.lockTarget = null;
			targetPoint = playerPos.add(s.lastDir.scale(10));
		}

		// 鍙湁鏈懡涓洰鏍囩殑钀藉湴鎵嶇粨绠楁墤绌猴紱涓嶅欢闀胯惤鍦板悗鐨勮拷鍑绘椂闂淬€?
		if (s.leapTick > 5 && player.onGround()) {
			finish(player, false);
			return;
		}

		// 鏂藉姏锛氭按骞虫柟鍚戝钩婊戞彃鍊艰浆鍚戯紙闃叉帴杩戠洰鏍囨椂宸﹀彸鎽嗗姩锛? 鍗忚皟鎶涚墿绔栫洿锛堟祦鐣呮洸绾匡級
		Vec3 desired = new Vec3(targetPoint.x - playerPos.x, 0, targetPoint.z - playerPos.z);
		if (desired.lengthSqr() > 1.0e-4) {
			desired = desired.normalize();
			s.lastDir = s.lastDir.lerp(desired, TURN_LERP);
			if (s.lastDir.lengthSqr() > 1.0e-4) s.lastDir = s.lastDir.normalize();
		}
		double leapSpeed = BAL.d("leap_speed", LEAP_SPEED);
		double vy = s.vy0 - GRAVITY_STEP * s.leapTick; // 鎶涚墿绔栫洿锛堜笉閲嶇畻锛屼繚鎸佸钩婊戯級
		player.setDeltaMovement(s.lastDir.x * leapSpeed, vy, s.lastDir.z * leapSpeed);
		player.hurtMarked = true;
		player.fallDistance = 0.0f;
		pushVelocity(player);

		// 璺宠穬鎷栧熬锛氳洓涓濆井绮掞紙owner 鎵撴爣锛氫粎鏈汉绗竴浜虹О閬胯锛?
		if (s.leapTick % 2 == 0) {
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CRIT, player.getX(), player.getY(0.5), player.getZ(),
					2, 0.1, 0.1, 0.1, 0.0);
		}
	}

	/** 鍛戒腑缁撶畻锛? 鐗╃悊 + 涓瘨 II 8s锛堟瘨娑茶吅浣擄細绛夌骇+1 / 鏃堕暱脳70%锛? 瀹氳韩 0.35s + 闊虫晥绮掑瓙銆?*/
	private static void onHit(ServerPlayer player, ServerLevel sw, LivingEntity target) {
		if (WhitelistUtils.isProtected(player, target)) return;
		DamageSource src = player.damageSources().playerAttack(player);
		float before = target.getHealth();
		boolean damaged = target.hurt(src, NpcCombatEnhancements.scale(player, target, (float) BAL.d("damage", DAMAGE), "salticidae_primary_damage", 2.0));
		State cast = STATES.get(player.getUUID());
		if (damaged && cast != null) SpiderSalticidaePerks.rewardDirectHit(player, target, before - target.getHealth(), cast.killHealing, cast.strengthTicks);
		boolean gland = github.com.gengyoubo.sscfa.item.VenomGlandItem.isWearingBy(player);
		int amp = BAL.i("poison_amplifier", POISON_AMPLIFIER) + (gland ? 1 : 0);
		int baseDur = BAL.i("poison_duration", POISON_DURATION);
		int dur = gland ? Math.round(baseDur * github.com.gengyoubo.sscfa.item.VenomGlandItem.DURATION_SCALE) : baseDur;
		dur = NpcCombatEnhancements.duration(player, target, dur, "salticidae_primary_duration", 2.0);
		target.addEffect(new MobEffectInstance(MobEffects.POISON, dur, amp, false, true, true), player);
		int stunTicks = NpcCombatEnhancements.duration(player, target, BAL.i("stun_duration", STUN_DURATION), "salticidae_primary_duration", 2.0);
		target.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.STUN.get(), stunTicks, 0, false, false, false), player);
        if(damaged)github.com.gengyoubo.sscfa.spell.SpiderSpellLink.onJumpHit(player,target);
		sw.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
		sw.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.7f);
		sw.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(),
				18, 0.3, 0.3, 0.3, 0.4);
	}

	/** 璺宠穬缁撴潫锛氭仮澶嶉噸鍔涖€佽繘 CD銆佹竻鐘舵€併€?*/
	private static void finish(ServerPlayer player, boolean hit) {
        State finishing = STATES.get(player.getUUID());
        if (finishing == null) return;
		PlayerGravityLease.release(player, SKILL_ID);
		// 鎵戜腑 = 鎴愬姛 CD锛坈ooldown锛夛紝鎵戠┖ = 澶辫触 CD锛坒ail_cooldown锛?
		if (hit) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, finishing.castId);
		else github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(player, SKILL_ID, finishing.castId);
		STATES.remove(player.getUUID());
	}

	/** 璺宠穬鏈熶笣绾挎鏌ワ細绂婚敋鐐硅秴 32 鏍?/ 涓濈嚎琚疄蹇冩柟鍧楅伄鎸?鈫?涓濇柇锛堥敋鐐瑰け鏁堬紝鏃犳媺鍥烇級銆?*/
	private static void checkSilk(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor == null) return;
		boolean overstretch = player.position().distanceTo(anchor.pos) > SILK_MAX;
		boolean obscured = computeObscuration(player, bodyCenter(player), anchor.pos) > OBSCURE_BREAK_BLOCKS;
		if (overstretch || obscured) {
			if (player.level() instanceof ServerLevel sw) snapSilk(player, sw, anchor);
			ANCHORS.remove(player.getUUID());
			SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
		}
	}

	/** 涓濈嚎閬尅閲忥細娌夸笣绾块噰鏍风疮璁″疄蹇冩柟鍧楁暟锛堝悓鏈堢粐铔涚畻娉曪紝涓ょ鍚勮烦杩?0.6 鏍奸槻璐村璇柇锛夈€?*/
	private static double computeObscuration(ServerPlayer player, Vec3 from, Vec3 to) {
		double totalDist = from.distanceTo(to);
		if (totalDist < 2.0) return 0.0;
		int samples = Mth.ceil(totalDist * 2.0);
		double obscured = 0.0;
		Vec3 dir = to.subtract(from).normalize();
		double skip = 0.6 / totalDist;
		for (int i = 0; i < samples; i++) {
			double t = (i + 0.5) / samples;
			if (t < skip || t > 1.0 - skip) continue;
			Vec3 p = from.add(dir.scale(totalDist * t));
			BlockPos bp = BlockPos.containing(p);
			if (player.level().getBlockState(bp).isRedstoneConductor(player.level(), bp)) {
				obscured += 0.5;
			}
		}
		return obscured;
	}

	/** 鍙栨秷锛堟浜?涓㈠舰鎬?鏂嚎锛夛細绉婚櫎鍑忛€熴€佹仮澶嶉噸鍔涖€佹竻鐘舵€佷笌閿︾偣锛堝甫鏂笣鐗规晥锛夛紝涓嶈繘 CD銆?*/
	public static void cancel(ServerPlayer player) {
		SpiderSalticidaePerks.setSensing(player, false);
		State s = STATES.remove(player.getUUID());
        if (s != null) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
		if (s != null) {
			removeChargeSlow(player);
			PlayerGravityLease.release(player, SKILL_ID);
		}
		SilkAnchor anchor = ANCHORS.remove(player.getUUID());
		if (anchor != null) {
			if (player.level() instanceof ServerLevel sw) snapSilk(player, sw, anchor);
			SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
		}
	}

	public static void interruptControl(ServerPlayer player) {
		if (STATES.containsKey(player.getUUID())) cancel(player);
	}

	/** 鏂嚎/鍋滄湇娓呯悊锛氶敠鐐逛竴骞舵竻銆?*/
	public static void clearPlayer(UUID uuid) {
		STATES.remove(uuid);
		ANCHORS.remove(uuid);
	}

	public static void clearAll() {
		STATES.clear();
		ANCHORS.clear();
	}

	/** 璺宠穬鏈熷厤鐤細鐜╁姝ｅ湪璺虫潃涓斾激瀹虫簮鏀诲嚮鑰呮槸鍏堕攣瀹氱洰鏍?鈫?鍏嶇柅锛坢ixin 鏌ヨ锛夈€?*/
	public static boolean isLeapingAgainst(ServerPlayer player, Entity attacker) {
		if (attacker == null) return false;
		State s = STATES.get(player.getUUID());
		if (s == null || s.phase != 1) return false;
		return attacker.getUUID().equals(s.lockTarget);
	}

	/** 姣掓恫鎶€鑳界敤锛氬畨鍏ㄤ笣閿氱偣鏄惁瀛樺湪涓斿湪鏈夋晥绐楀彛鍐咃紙=銆屾湁涓濈嚎杩炵潃鑷繁銆嶏級銆?*/
	public static boolean hasActiveSilk(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		return anchor != null && player.serverLevel().getGameTime() <= anchor.validUntil
				&& player.position().distanceTo(anchor.pos) <= SILK_MAX;
	}

	/** 姣掓恫鎶€鑳界敤锛氭秷鑰椾笣绾匡紙鑽′笣鍐插埡鐢ㄦ帀閿氱偣锛屽甫鏂笣鐗规晥涓庡鎴风鍚屾锛夈€?*/
	public static void consumeSilk(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.remove(player.getUUID());
		if (anchor != null) {
			if (player.level() instanceof ServerLevel sw) snapSilk(player, sw, anchor);
			SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
		}
	}

	/** 鏉惧紑鏃舵寜褰撳墠鍑嗘槦閿佸畾锛氫粠绱㈡晫鍊欓€夛紙鍚伄鎸¤繃婊わ級閲屽彇鍑嗘槦 12掳 閿ュ唴鏈€杩戣€呫€?*/
	private static LivingEntity pickLockTarget(ServerPlayer player, ServerLevel sw, double scanDist) {
		List<LivingEntity> cands = scanCandidates(player, sw, scanDist);
		return pickInCrosshair(player, cands);
	}

	/** 绱㈡晫鍊欓€夛細scanDist 鍐呭叏閮ㄦ椿浣擄紙闈炶嚜宸?闈炴梺瑙?闈炵櫧鍚嶅崟锛夛紝涓旇绾挎棤鏂瑰潡閬尅锛堥伄鎸?= 璺充笉杩囧幓 鈫?涓嶆樉绀轰笉鍙攣锛夈€?*/
	private static List<LivingEntity> scanCandidates(ServerPlayer player, ServerLevel sw, double scanDist) {
		Vec3 eye = player.getEyePosition();
		AABB box = player.getBoundingBox().inflate(scanDist);
		List<LivingEntity> all = sw.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, e)
						&& eye.distanceTo(bodyCenter(e)) <= scanDist
						&& !WhitelistUtils.isProtected(player, e));
		List<LivingEntity> visible = new java.util.ArrayList<>(all.size());
		for (LivingEntity e : all) {
			if (hasLineOfSight(player, e)) visible.add(e);
		}
		return visible;
	}

	/** 鍑嗘槦閿佸畾锛氬€欓€変腑涓庡噯鏄熸柟鍚戝す瑙掑湪 12掳 閿ュ唴銆佷笖涓庡噯鏄熸渶鎺ヨ繎鑰呫€?*/
	private static LivingEntity pickInCrosshair(ServerPlayer player, List<LivingEntity> cands) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		LivingEntity best = null;
		double bestDot = SCAN_COS;
		for (LivingEntity e : cands) {
			double dot = look.dot(bodyCenter(e).subtract(eye).normalize());
			if (dot > bestDot) { bestDot = dot; best = e; }
		}
		return best;
	}

	/** 閬尅妫€娴嬶細鐜╁鐪肩潧鈫掔洰鏍囪韩浣撲腑蹇冨皠绾匡紙纰版挒绠卞舰鐘讹紝蹇界暐娴佷綋锛変笉鍛戒腑浠讳綍鏂瑰潡 = 鏃犻伄鎸°€?*/
	private static boolean hasLineOfSight(ServerPlayer player, LivingEntity target) {
		Vec3 from = player.getEyePosition();
		Vec3 to = bodyCenter(target);
		net.minecraft.world.phys.BlockHitResult hit = player.level().clip(new net.minecraft.world.level.ClipContext(
				from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE, player));
		return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	private static LivingEntity resolve(ServerLevel sw, UUID uuid) {
		Entity e = sw.getEntity(uuid);
		return (e instanceof LivingEntity le) ? le : null;
	}

	private static Vec3 bodyCenter(LivingEntity e) {
		return new Vec3(e.getX(), e.getY(0.5), e.getZ());
	}

	private static void pushVelocity(ServerPlayer player) {
		// 鐜╁绉诲姩瀹㈡埛绔潈濞侊細鏄惧紡琛ュ彂閫熷害鍖咃紝寮哄埗涓绘満/瀹㈡満搴旂敤璺宠穬浣嶇Щ锛堝啿鍒烘妧韪╄繃鍧戯級
		if (player.connection != null) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static void applyChargeSlow(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW_UUID);
			speed.addTransientModifier(new AttributeModifier(
					SLOW_UUID, "Jump Kill Charge Slow", BAL.d("charge_slow", CHARGE_SLOW), AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	private static void removeChargeSlow(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.removeModifier(SLOW_UUID);
	}
}
