package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.SpiderSwingBullet;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鏈堢粐铔涖€岃洓涓濊崱婕俱€嶆鎶€鑳斤紙sp_secondary锛夆€??<b>鏈嶅姟绔潈濞??/b>??
 *
 * <p>鐗╃悊鏀惧鎴风锛坽@code SwingPhysicsMixin}锛夛紝鏈被鍙仛锛氬彂灏勮洓涓濋寮广€乵ana 鏉冨▉銆佹柇涓濇娴?
 * TETHER 鎷栨嫿鐩爣鐢熺墿銆佺姸鎬佸箍鎾€傚彂灏勬敼涓烘姇灏??{@link ShellProjectile}锛堟姏鐗╃嚎椋炶锛屽懡涓柟鍧椻啋
 * 鎽??/ 鍛戒腑鐢熺墿鈫掓嫋?tether / miss鈫掕惤鍦版秷澶憋級?
 */
public final class SpiderMoonWeaverSwingManager {

	// ==== 鍙??/ 椋炶锛堝凡鐧昏锛歜ullet_speed / mana_per_block / tether_hit_mana_cost锛涜繍琛屾椂??balance 蹇収璇诲彇 abilities.moon_weaver_swing??====
	private static final float BULLET_SPEED = 1.7f; // 榛樿锛涜繍琛屾椂?balance 蹇収璇诲彇
	public static final double MANA_PER_BLOCK = 2.0; // 榛樿锛涜繍琛屾椂?balance 蹇収璇诲彇
	public static final double TETHER_HIT_MANA_COST = 8.0; // 榛樿锛涜繍琛屾椂?balance 蹇収璇诲彇锛堝嬀涓敓鐗╃灛闂撮澶栨墸??mana锛屼笉瓒虫墸??0??

	// ==== 缁??/ 鏂笣锛堜互??6 鍙??client ??SwingPhysicsMixin / ShellProjectile 寮曠敤鍋氶娴嬮暅鍍忥紝甯搁噺淇濈暀涓嶈縼绉伙級 ====
	public static final double MAX_ROPE_REACH = 32.0;
	public static final double TETHER_MAX_LEN = 16.0; // 榛樿锛涜繍琛屾椂?balance 蹇収璇诲彇銆倀ether 鎷寸敓鐗╃‖涓婇檺锛堢粷涓嶈秴锛屽惛鏀惰繙??+ 澶у姏鐗靛紩?
	public static final double TETHER_SOFT_BUFFER = 4.0; // 杞媺缂撳啿锛氳窛纭笂??4 鏍??12 鏍硷級璧风嚎鎬х壍寮曪紝?16 澶у姏纭??
	public static final double TETHER_PULL_GAIN = 0.2; // 12~16 绾挎€х壍寮曠郴鏁帮紙姣??over 澧炲姞鐨勭壍寮曢€熷害?
	public static final double TETHER_HARD_GAIN = 0.8; // ??16 姣忔牸棰濆澶у姏鐗靛紩绯绘暟锛堝己鎷夊洖銆侀槻鍐插嚭??
	public static final double MIN_ROPE_LEN = 1.5;
	public static final double REEL_SPEED = 0.16;
	private static final double OBSCURE_BREAK_BLOCKS = 1.0;

	// 闃??5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺??
	private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_swing");
	/** 缁熶竴鍐峰嵈鏈嶅姟鐨勭ǔ瀹氭妧?ID锛堢綉缁滃寘瑙﹀彂鍨嬶級??*/
	private static final String SKILL_ID = "my_addon:form_spider_moon_weaver_swing";

	/** 鏈嶅姟绔柇涓濋槇鍊硷細max_rope_reach 蹇収?+ 3 鏍间綑閲忥紙涓庡父閲忕増 BREAK_OVERSTRETCH = MAX_ROPE_REACH + 3.0 绛変环锛?*/
	private static double breakOverstretch() {
		return BAL.d("max_rope_reach", MAX_ROPE_REACH) + 3.0;
	}

	// ==== 鐘??====
	public static final int STATE_IDLE = 0;
	public static final int STATE_SWINGING = 2;
	public static final int STATE_TETHER = 3; // 杩炴帴鐢熺墿鎷??

	private static final class SwingState {
        long castId = -1;
		int state = STATE_IDLE;
		Vec3 anchor = Vec3.ZERO;    // SWINGING 闂佸簱鍋撻柣?
		int tetherEntityId = -1;      // TETHER 鐩爣瀹??id
		double ropeLen = 0.0;
		boolean canExtend = true;
		int broadcastTick = 0;
		double lastPX = Double.NaN, lastPY = 0, lastPZ = 0; // 鍗℃妫€娴嬶細??tick 鐜╁浣嶇疆
		int stuckTicks = 0;          // 琚媺浣嗚繛缁笉鍔ㄧ殑 tick ??
	}

	private static final Map<UUID, SwingState> STATES = new ConcurrentHashMap<>();
	/** 铔涗笣椋炲脊鍦ㄩ鐨勭帺????寮瑰疄浣擄紙闃茶繛?+ 鏀寔椋炶涓啀鎸夐敭鍙栨秷锛涘懡?miss 鏃剁Щ闄わ級?*/
	private static final Map<UUID, SpiderSwingBullet> BULLET_IN_FLIGHT = new ConcurrentHashMap<>();

	private SpiderMoonWeaverSwingManager() {}

	private static boolean isSpiderMoonWeaver(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER);
	}

	private static github.com.gengyoubo.sscfa.compat.ManaComponent mana(ServerPlayer player) {
		return github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
	}

	private static SwingState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), k -> new SwingState());
	}

	private static void broadcastState(ServerPlayer player, SwingState s) {
		SscAddonNetworking.syncSwingState(player, s.state != STATE_IDLE,
				s.anchor.x, s.anchor.y, s.anchor.z, s.ropeLen, s.state, s.canExtend, s.tetherEntityId);
	}

	/**
	 * 璇ョ帺瀹舵槸鍚︽鎷翠綇锛圱ETHER锛夋寚瀹氬疄浣撱€傜敤浜庤冻涓濇嫶鐢熺墿鐨勪激瀹冲鍑忓垽瀹氾紙鍙屽悜閫氱敤锛?
	 * 鍙傛暟?{@link Entity} 浠ュ厤棰濆 import锛沺layer/target 浠讳竴?null 鎴??TETHER 鍧囪繑?false??
	 */
	public static boolean isTethering(Entity player, Entity target) {
		if (player == null || target == null) return false;
		SwingState s = STATES.get(player.getUUID());
		return s != null && s.state == STATE_TETHER && s.tetherEntityId == target.getId();
	}

	/**
	 * 鍙嶆煡姝ｆ嫶浣忥紙TETHER锛夋寚瀹氬疄浣撶殑鎷翠富鐜╁锛屾棤鍒欒繑??null銆傜敤浜庤鎷寸洰鏍囧彈浼ゆ椂鎵炬嫶涓伙紙鏁屾垜鍒??浼ゅ鍒嗘媴锛??
	 */
	public static ServerPlayer getTetheringPlayer(Entity target) {
		if (target == null || target.getServer() == null) return null;
		for (Map.Entry<UUID, SwingState> e : STATES.entrySet()) {
			SwingState s = e.getValue();
			if (s.state == STATE_TETHER && s.tetherEntityId == target.getId()) {
				ServerPlayer sp = target.getServer().getPlayerList().getPlayer(e.getKey());
				if (sp != null) return sp;
			}
		}
		return null;
	}

	private static Vec3 torso(ServerPlayer player) {
		return player.position().add(0, 1.0, 0);
	}

	private static Vec3 entityCenter(LivingEntity e) {
		return e.position().add(0, e.getBbHeight() * 0.5, 0);
	}

	// ==== 娆￠敭锛欼DLE鈫掑彂?/ 鍚﹀垯鈫掓柇??====
	public static void onSecondaryPress(ServerPlayer player) {
		if (!isSpiderMoonWeaver(player)) return;
		SwingState s = state(player);
		if (s.state == STATE_IDLE) {
			// 椋炶涓啀鎸????鍙栨秷椋炲脊锛??remove ??onBulletMiss ??5 ??CD??
			SpiderSwingBullet flying = BULLET_IN_FLIGHT.get(player.getUUID());
			if (flying != null && flying.isAlive()) {
				flying.discard();
				return;
			}
			shootBullet(player);
		} else {
			breakWeb(player, s, true);
		}
	}

	/** 鍙戝皠铔涗笣椋炲脊锛堟姏鐗╃嚎鎶曞皠鐗╋紝纰版挒鍚庡洖璋冭繘鍏ユ憜?/ tether锛??*/
	private static void shootBullet(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return;
		if (BULLET_IN_FLIGHT.containsKey(player.getUUID())) return;
		if (mana(player).getMana() < 1.0) {
			player.displayClientMessage(Component.translatable("message.my_addon.spider_moon_weaver.swing.no_mana"), true);
			return;
		}
		// 鍙戝皠鍗崇櫥璁帮紱CD 鍦ㄦ柇?miss 鏃舵墠璧风畻锛坈ooldown_start = on_end??
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return;
		state(player).castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
        SpiderSwingBullet bullet = new SpiderSwingBullet(player); bullet.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
				(float) BAL.d("bullet_speed", BULLET_SPEED), 0.0f);
		player.level().addFreshEntity(bullet);
		BULLET_IN_FLIGHT.put(player.getUUID(), bullet);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.3f);
	}

	// ==== 椋炲脊纰版挒鍥炶皟锛堢敱 ShellProjectile 璋冪敤?====

	/** 椋炲脊鍛戒腑鏂????閽╀綇杩涘叆鎽嗚崱?*/
	public static void onBulletHitBlock(ServerPlayer player, Vec3 anchor, Object bullet) {
        if (!BULLET_IN_FLIGHT.remove(player.getUUID(), bullet)) return;
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)) {
			settleBreak(player);
			return;
		}
		if (!isSpiderMoonWeaver(player)) return;
		SwingState s = state(player);
		s.state = STATE_SWINGING;
		s.anchor = anchor;
		s.tetherEntityId = -1;
		s.ropeLen = Mth.clamp(torso(player).distanceTo(anchor), MIN_ROPE_LEN, MAX_ROPE_REACH);
		s.canExtend = true;
		broadcastState(player, s);
	}

	/** 鎷翠綇鐩爣鐨勯珮鍏夋弿杈硅壊锛氱櫧鍚嶅崟鍙嬪啗鈫掔豢锛屾晫浜衡啋钃濓紙浠呮柦娉曡€呭彲瑙侊級?*/
	private static int tetherHighlightColor(ServerPlayer player, LivingEntity target) {
		return WhitelistUtils.isProtected(player, target) ? 0x3AF03A : 0x3AA0FF;
	}

	/** 椋炲脊鍛戒腑鐢????杩炴帴杩涘叆 tether 鎷栨嫿?*/
	public static void onBulletHitEntity(ServerPlayer player, LivingEntity target, Object bullet) {
        if (!BULLET_IN_FLIGHT.remove(player.getUUID(), bullet)) return;
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)) {
			settleBreak(player);
			return;
		}
		if (!isSpiderMoonWeaver(player)) return;
		SwingState s = state(player);
		s.state = STATE_TETHER;
		s.tetherEntityId = target.getId();
		s.anchor = Vec3.ZERO;
		s.ropeLen = BAL.d("tether_max_len", TETHER_MAX_LEN); // 鍥??16 鏍间笂闄愶紙涓嶇敤鎷翠綇鐬棿璺濈锛夛細鍏佽 0~16 鑷敱锛岃秴 16 纭害鏉熸媺鍥炪€佷笉鍙欢?
		s.canExtend = true;
		mana(player).consumeMana(BAL.d("tether_hit_mana_cost", TETHER_HIT_MANA_COST)); // 鍕句腑鐢熺墿鐬棿棰濆??8 ??mana锛堣嚜?clamp ??0 涓嶄細璐熴€佽嚜鍔ㄥ悓姝ュ鎴??mana 鏉??
		SscAddonNetworking.sendWebHighlight(player, target.getId(), 40, tetherHighlightColor(player, target)); // 浠呮柦娉曡€呭彲瑙侀珮鍏夛紙鍙嬪啗?鏁屼汉钃濓級
		broadcastState(player, s);
	}

	/** 椋??miss 钀藉湴娑??/ 琚Щ????5 ??CD锛堝箓绛夛細浠呬粛鍦ㄩ鏈懡涓椂鐢熸晥锛沚alance break_cooldown_ticks 鍙鐩栵級??*/
	public static void onBulletMiss(ServerPlayer player, Object bullet) {
        if (BULLET_IN_FLIGHT.remove(player.getUUID(), bullet)) {
			settleBreak(player);
		}
	}

	// ==== 鏂??====
	private static void breakWeb(ServerPlayer player, SwingState s, boolean giveCd) {
		boolean wasActive = s.state != STATE_IDLE;
		s.state = STATE_IDLE;
		s.anchor = Vec3.ZERO;
		s.tetherEntityId = -1;
		s.ropeLen = 0.0;
		s.canExtend = true;
		s.stuckTicks = 0;
		s.lastPX = Double.NaN;
		if (wasActive) {
			ServerLevel sw = (ServerLevel) player.level();
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.7f, 1.1f);
			if (giveCd) {
				settleBreak(player);
			} else {
				github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
			}
		}
		broadcastState(player, s);
	}

	/** 鏂??miss锛氭湰娆¤崱涓濈粨鏉燂紝?power JSON cooldown 璧风畻?*/
	private static void settleBreak(ServerPlayer player) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, state(player).castId);
	}

	// ==== 姣忔湇鍔＄ tick ====
	public static void tick(ServerPlayer player) {
		SwingState s = STATES.get(player.getUUID());
		if (s == null || s.state == STATE_IDLE) return;
		if (player.isDeadOrDying() || !isSpiderMoonWeaver(player)) {
			breakWeb(player, s, false);
			return;
		}
		if (s.state == STATE_SWINGING) {
			tickSwinging(player, s);
		} else if (s.state == STATE_TETHER) {
			tickTether(player, s);
		}
	}

	/** SWINGING锛氭柇涓濇??+ 瀹氭湡骞挎挱 + 閿€鐐圭矑瀛愶紙鐗╃悊鍦ㄥ鎴风锛??*/
	private static void tickSwinging(ServerPlayer player, SwingState s) {
		player.fallDistance = 0.0f; // 鏈嶅姟绔竻鎽旇惤璺濈闃叉憜鑽¤惤鍦版憯浼わ紙涓嶅奖鍝嶅鎴风 fallDistance 椹卞姩?FALL 鍔ㄧ敾?
		Vec3 torso = torso(player);
		double dist = torso.distanceTo(s.anchor);
		if (dist > breakOverstretch()) {
			breakWeb(player, s, true);
			return;
		}
		if (computeObscuration(player, torso, s.anchor) > OBSCURE_BREAK_BLOCKS) {
			breakWeb(player, s, true);
			return;
		}
		if (++s.broadcastTick >= 3) {
			s.broadcastTick = 0;
			broadcastState(player, s);
			// 閿氱偣涓濅簯锛坥wner 鎵撴爣锛氫粎鏈汉閬胯??
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn((ServerLevel) player.level(), player, net.minecraft.core.particles.ParticleTypes.CLOUD,
					s.anchor.x, s.anchor.y, s.anchor.z, 2, 0.12, 0.12, 0.12, 0.0);
		}
	}

	/** TETHER锛氭媺鐩爣鐢熺墿鍚戠帺瀹讹紙鎸夋姉鎬х缉鏀撅紝鏈夐樆鍔涳級+ 鏂笣妫€?+ 楂樺厜鍒锋柊 + 骞挎挱?*/
	private static void tickTether(ServerPlayer player, SwingState s) {
		player.fallDistance = 0.0f; // 鏈嶅姟绔竻鎽旇惤璺濈??tether 鎷栨嫿钀藉湴鎽斾激
		ServerLevel world = (ServerLevel) player.level();
		Entity target = world.getEntity(s.tetherEntityId);
		if (!(target instanceof LivingEntity living) || !living.isAlive()) {
			breakWeb(player, s, true);
			return;
		}
		Vec3 pPos = torso(player);
		Vec3 tPos = entityCenter(living);
		double dist = pPos.distanceTo(tPos);
		if (dist > breakOverstretch()) {
			breakWeb(player, s, true);
			return;
		}
		if (computeObscuration(player, pPos, tPos) > OBSCURE_BREAK_BLOCKS) {
			breakWeb(player, s, true);
			return;
		}
		double ropeLen = Math.max(s.ropeLen, MIN_ROPE_LEN); // 纭笂闄愶紙鍒??16??
		double softLen = Math.max(MIN_ROPE_LEN, ropeLen - TETHER_SOFT_BUFFER); // 杞媺闃堝€硷紙鍒??12??
		// 鐗靛紩?2~16 绾挎€у姞澶э紙over脳PULL_GAIN锛夛紝?16 鍚告敹杩滅 + 棰濆澶у姏鐗靛紩锛堝己鎷夊洖銆侀槻鍐插嚭 16锛夛紱绾€熷害涓嶆敼浣嶇疆??
		if (dist > softLen) {
			double resist = knockbackResist(living);
			Vec3 dir = pPos.subtract(tPos).normalize(); // 鐩????鐜??
			Vec3 tv = living.getDeltaMovement();
			double over = dist - softLen; // 12 闁?0闁?6 闁?4
			double pull = over * TETHER_PULL_GAIN; // 12 璧风嚎鎬у姞澶х壍?
			double cap = 0.55;
			if (dist > ropeLen) {
				pull += (dist - ropeLen) * TETHER_HARD_GAIN; // ??16 棰濆澶у姏锛堢嚎鎬ф洿闄★紝闈犵壍寮曞姏闃茶繃杩溿€佷笉纭惛鏀堕€熷害銆佷笉鍗★級
				cap = 0.9; // 鏀惧閫熷害涓婇檺缁欏ぇ?
			}
			pull *= (1.0 - resist); // 鎸夋姉鎬э細杞荤洰鏍囦富瑕佹媺鐩爣锛岄噸鐩爣闈犲鎴风鎶婄帺瀹舵媺杩囧幓
			if (pull > 0.001) {
				double toward = tv.dot(dir);
				if (toward < pull) tv = tv.add(dir.scale(pull - toward)); // 鍥炴媺鍚戠帺??
			}
			double sp = tv.length();
			if (sp > cap) tv = tv.scale(cap / sp); // 閫熷害涓婇檺闃茬垎鍐诧紙??16 鏀惧缁欏ぇ鍔涳級
			living.setDeltaMovement(tv);
			living.hurtMarked = true;
			living.hasImpulse = true;
		}
		// 楂樺厜鍒锋柊锛堜粎鏂芥硶鑰呭彲瑙侊紱鍙嬪啗?鏁屼汉钃濓級鈥斺€旈檷棰戞瘡 10t 鍒蜂竴娆★細鏃??20t锛屽崐绋嬬画鏈熶笉闂柇??
		// 鐪佹帀 tether 鏈??tick 涓€涓珮鍏夊寘??~20??鍐椾綑锛堝??NightmareDreamManager 鎻忚竟鍒锋柊鎯緥?
		if (world.getGameTime() % 10L == 0L) {
			SscAddonNetworking.sendWebHighlight(player, living.getId(), 20, tetherHighlightColor(player, living));
		}
		// 鍗℃淇濆簳锛氳鎷夛紙dist>缁抽暱锛変絾杩炵画涓嶅姩 ??2 绉掑悗鑷姩鏂笣瑙ｈ劚锛堥槻姘镐箙鍗℃?
		if (dist > ropeLen + 0.5) {
			if (!Double.isNaN(s.lastPX)) {
				double dx = pPos.x - s.lastPX, dy = pPos.y - s.lastPY, dz = pPos.z - s.lastPZ;
				if (dx * dx + dy * dy + dz * dz < 0.0016) { // 缂佸顕ф慨?< 0.04 闁?
					if (++s.stuckTicks > 20) {
						breakWeb(player, s, true);
						return;
					}
				} else {
					s.stuckTicks = 0;
				}
			}
			s.lastPX = pPos.x;
			s.lastPY = pPos.y;
			s.lastPZ = pPos.z;
		} else {
			s.stuckTicks = 0;
			s.lastPX = Double.NaN;
		}
		if (++s.broadcastTick >= 2) {
			s.broadcastTick = 0;
			broadcastState(player, s);
		}
	}

	private static double knockbackResist(LivingEntity living) {
		AttributeInstance inst = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		return inst != null ? Mth.clamp(inst.getValue(), 0.0, 1.0) : 0.0;
	}

	/**
	 * 瀹㈡埛绔瘡 2 tick 涓婃姤涓€娆＄怀?+ 鏀舵斁鎰忓浘?1 ?/ -1 ?/ 0锛夈€傛湇鍔＄淇′换瀹㈡埛?ropeLen锛堣浆?+ 鏂笣鍙傝€冿級??
	 * 鏀剧怀鏉冨▉?mana锛堟瘡鍖呮墸 2 tick 鐨勯噺锛夛紝mana 涓嶈冻?canExtend=false 骞挎挱闃绘缁х画鏀剧怀銆係WINGING / TETHER 鍧囬€傜敤?
	 */
	public static void onReelSync(ServerPlayer player, double clientRopeLen, int reel) {
		SwingState s = STATES.get(player.getUUID());
		if (s == null || (s.state != STATE_SWINGING && s.state != STATE_TETHER)) return;
		// tether 鎷寸敓鐗╋細鏈€澶ч棿??16 涓旂姝㈡斁缁冲欢闀匡紱swinging 鑽℃季浠嶅彲??32 骞舵斁?
		double maxLen = (s.state == STATE_TETHER) ? BAL.d("tether_max_len", TETHER_MAX_LEN) : MAX_ROPE_REACH;
		s.ropeLen = Mth.clamp(clientRopeLen, MIN_ROPE_LEN, maxLen);
		if (s.state == STATE_TETHER) reel = 0; // tether 涓嶈兘寤堕暱铔涗笣锛屽拷鐣ユ斁缁虫剰??
		boolean prev = s.canExtend;
		if (reel < 0) {
			// 瀹㈡埛绔瘡 2 tick 涓婃姤涓€娆★紝姣忓寘?2 tick 鐨勬斁缁抽噺锛圧EEL_SPEED 涓??tick 鏀剧怀闀垮害锛??
			// 淇濇寔涓庡師??tick 涓婃姤鏃剁浉鍚??mana 娑堣€楅€??
			double cost = REEL_SPEED * 2.0 / BAL.d("mana_per_block", MANA_PER_BLOCK);
			github.com.gengyoubo.sscfa.compat.ManaComponent m = mana(player);
			if (m.getMana() >= cost) {
				m.consumeMana(cost);
				s.canExtend = true;
			} else {
				s.canExtend = false;
			}
		} else {
			s.canExtend = true;
		}
		if (s.canExtend != prev) {
			broadcastState(player, s);
		}
	}

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

	public static void onDisconnect(ServerPlayer player) {
        interruptControl(player);
        STATES.remove(player.getUUID());
        BULLET_IN_FLIGHT.remove(player.getUUID());
	}

	public static void interruptControl(ServerPlayer player) {
		SwingState s = STATES.get(player.getUUID());
		if (s != null && s.state != STATE_IDLE) breakWeb(player, s, false);
	}
}
