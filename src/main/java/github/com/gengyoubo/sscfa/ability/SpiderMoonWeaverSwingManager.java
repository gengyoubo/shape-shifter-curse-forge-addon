package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.ShellProjectile;
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
 * 閺堝牏绮愰摂娑栤偓宀冩磽娑撴繆宕卞淇扁偓宥嗩偧閹垛偓閼虫枻绱檚p_secondary閿涘鈧柡鈧?<b>閺堝秴濮熺粩顖涙綀婵炰礁鐪?/b>閵?
 *
 * <p>閻椻晝鎮婇弨鎯ь吂閹撮顏敍鍧紷code SwingPhysicsMixin}閿涘绱濋張顒傝閸欘亜浠涢敍姘絺鐏忓嫯娲撴稉婵嬵棧瀵箍鈧沟ana 閺夊啫鈻夐妴浣规焽娑撴繃顥呭ù瀣ㄢ偓?
 * TETHER 閹锋牗瀚块惄顔界垼閻㈢喓澧块妴浣哄Ц閹礁绠嶉幘顓溾偓鍌氬絺鐏忓嫭鏁兼稉鐑樺鐏忓嫮澧?{@link ShellProjectile}閿涘牊濮忛悧鈺冨殠妞嬬偠顢戦敍灞芥嚒娑擃厽鏌熼崸妞诲晪
 * 閹藉棜宕?/ 閸涙垝鑵戦悽鐔哄⒖閳帗瀚嬮幏?tether / miss閳帟鎯ら崷鐗堢Х婢舵唻绱氶妴?
 */
public final class SpiderMoonWeaverSwingManager {

	// ==== 閸欐垵鐨?/ 妞嬬偠顢戦敍鍫濆嚒閻ф槒顔囬敍姝渦llet_speed / mana_per_block / tether_hit_mana_cost閿涙稖绻嶇悰灞炬娴?balance 韫囶偆鍙庣拠璇插絿 abilities.moon_weaver_swing閿?====
	private static final float BULLET_SPEED = 1.7f; // 姒涙顓婚敍娑滅箥鐞涘本妞傛禒?balance 韫囶偆鍙庣拠璇插絿
	public static final double MANA_PER_BLOCK = 2.0; // 姒涙顓婚敍娑滅箥鐞涘本妞傛禒?balance 韫囶偆鍙庣拠璇插絿
	public static final double TETHER_HIT_MANA_COST = 8.0; // 姒涙顓婚敍娑滅箥鐞涘本妞傛禒?balance 韫囶偆鍙庣拠璇插絿閿涘牆瀣€娑擃厾鏁撻悧鈺冪仜闂傛挳顤傛径鏍ㄥ⒏閻?mana閿涘奔绗夌搾铏⒏閸?0閿?

	// ==== 缂佹娊鏆?/ 閺傤厺绗ｉ敍鍫滀簰娑?6 閸欏倽顫?client 閸?SwingPhysicsMixin / ShellProjectile 瀵洜鏁ら崑姘额暕濞村鏆呴崓蹇ョ礉鐢悂鍣烘穱婵堟殌娑撳秷绺肩粔浼欑礆 ====
	public static final double MAX_ROPE_REACH = 32.0;
	public static final double TETHER_MAX_LEN = 16.0; // 姒涙顓婚敍娑滅箥鐞涘本妞傛禒?balance 韫囶偆鍙庣拠璇插絿閵嗗€€ether 閹峰鏁撻悧鈺冣€栨稉濠囨閿涘牏绮锋稉宥堢Т閿涘苯鎯涢弨鎯扮箼缁?+ 婢堆冨閻楅潧绱╅敍?
	public static final double TETHER_SOFT_BUFFER = 4.0; // 鏉烆垱濯虹紓鎾冲暱閿涙俺绐涚涵顑跨瑐闂?4 閺嶇》绱?12 閺嶇》绱氱挧椋庡殠閹呭瀵洩绱濋崚?16 婢堆冨绾剟妾?
	public static final double TETHER_PULL_GAIN = 0.2; // 12~16 缁炬寧鈧呭瀵洜閮撮弫甯礄濮ｅ繑鐗?over 婢х偛濮為惃鍕瀵洟鈧喎瀹抽敍?
	public static final double TETHER_HARD_GAIN = 0.8; // 鐡?16 濮ｅ繑鐗告０婵嗩樆婢堆冨閻楅潧绱╃化缁樻殶閿涘牆宸遍幏澶婃礀閵嗕線妲婚崘鎻掑毉閿?
	public static final double MIN_ROPE_LEN = 1.5;
	public static final double REEL_SPEED = 0.16;
	private static final double OBSCURE_BREAK_BLOCKS = 1.0;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_swing");
	/** 缂佺喍绔撮崘宄板祱閺堝秴濮熼惃鍕旂€规碍濡ч懗?ID閿涘牏缍夌紒婊冨瘶鐟欙箑褰傞崹瀣剁礆閵?*/
	private static final String SKILL_ID = "my_addon:form_spider_moon_weaver_swing";

	/** 閺堝秴濮熺粩顖涙焽娑撴繈妲囬崐纭风窗max_rope_reach 韫囶偆鍙庨崐?+ 3 閺嶉棿缍戦柌蹇ョ礄娑撳骸鐖堕柌蹇曞 BREAK_OVERSTRETCH = MAX_ROPE_REACH + 3.0 缁涘鐜敍澶堚偓?*/
	private static double breakOverstretch() {
		return BAL.d("max_rope_reach", MAX_ROPE_REACH) + 3.0;
	}

	// ==== 閻樿埖鈧?====
	public static final int STATE_IDLE = 0;
	public static final int STATE_SWINGING = 2;
	public static final int STATE_TETHER = 3; // 鏉╃偞甯撮悽鐔哄⒖閹锋牗瀚?

	private static final class SwingState {
        long castId = -1;
		int state = STATE_IDLE;
		Vec3 anchor = Vec3.ZERO;    // SWINGING 闁库偓閻?
		int tetherEntityId = -1;      // TETHER 閻╊喗鐖ｇ€圭偘缍?id
		double ropeLen = 0.0;
		boolean canExtend = true;
		int broadcastTick = 0;
		double lastPX = Double.NaN, lastPY = 0, lastPZ = 0; // 閸椻剝顒村Λ鈧ù瀣剁窗娑?tick 閻溾晛顔嶆担宥囩枂
		int stuckTicks = 0;          // 鐞氼偅濯烘担鍡氱箾缂侇厺绗夐崝銊ф畱 tick 閺?
	}

	private static final Map<UUID, SwingState> STATES = new ConcurrentHashMap<>();
	/** 閾旀稐绗ｆ鐐茶剨閸︺劑顥ｉ惃鍕负鐎?閳?瀵懓鐤勬担鎿勭礄闂冭尪绻涢崣?+ 閺€顖涘瘮妞嬬偠顢戞稉顓炲晙閹稿鏁崣鏍ㄧХ閿涙稑鎳℃稉?miss 閺冨墎些闂勩倧绱氶妴?*/
	private static final Map<UUID, ShellProjectile> BULLET_IN_FLIGHT = new ConcurrentHashMap<>();

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
	 * 鐠囥儳甯虹€硅埖妲搁崥锔筋劀閹风繝缍囬敍鍦盓THER閿涘瀵氱€规艾鐤勬担鎾扁偓鍌滄暏娴滃氦鍐绘稉婵囧閻㈢喓澧块惃鍕縺鐎瑰啿顤冮崙蹇撳灲鐎规熬绱欓崣灞芥倻闁氨鏁ら敍澶堚偓?
	 * 閸欏倹鏆熼悽?{@link Entity} 娴犮儱鍘ゆ０婵嗩樆 import閿涙埠layer/target 娴犺绔存稉?null 閹存牠娼?TETHER 閸у洩绻戦崶?false閵?
	 */
	public static boolean isTethering(Entity player, Entity target) {
		if (player == null || target == null) return false;
		SwingState s = STATES.get(player.getUUID());
		return s != null && s.state == STATE_TETHER && s.tetherEntityId == target.getId();
	}

	/**
	 * 閸欏秵鐓″锝嗗娴ｅ骏绱橳ETHER閿涘瀵氱€规艾鐤勬担鎾舵畱閹风繝瀵岄悳鈺侇啀閿涘本妫ら崚娆掔箲閸?null閵嗗倻鏁ゆ禍搴ゎ潶閹峰娲伴弽鍥у綀娴笺倖妞傞幍鐐娑撲紮绱欓弫灞惧灉閸掋倕鐣?娴笺倕顔婇崚鍡樺閿涘鈧?
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

	// ==== 濞嗭繝鏁敍娆糄LE閳帒褰傚?/ 閸氾箑鍨埆鎺撴焽娑?====
	public static void onSecondaryPress(ServerPlayer player) {
		if (!isSpiderMoonWeaver(player)) return;
		SwingState s = state(player);
		if (s.state == STATE_IDLE) {
			// 妞嬬偠顢戞稉顓炲晙閹稿鏁?閳?閸欐牗绉锋鐐茶剨閿涘牃鍟?remove 閳?onBulletMiss 閳?5 缁?CD閿?
			ShellProjectile flying = BULLET_IN_FLIGHT.get(player.getUUID());
			if (flying != null && flying.isAlive()) {
				flying.discard();
				return;
			}
			shootBullet(player);
		} else {
			breakWeb(player, s, true);
		}
	}

	/** 閸欐垵鐨犻摂娑楃妞嬬偛鑴婇敍鍫熷閻椻晝鍤庨幎鏇炵殸閻椻晪绱濈喊鐗堟寬閸氬骸娲栫拫鍐箻閸忋儲鎲滈懡?/ tether閿涘鈧?*/
	private static void shootBullet(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return;
		if (BULLET_IN_FLIGHT.containsKey(player.getUUID())) return;
		if (mana(player).getMana() < 1.0) {
			player.displayClientMessage(Component.translatable("message.my_addon.spider_moon_weaver.swing.no_mana"), true);
			return;
		}
		// 閸欐垵鐨犻崡宕囨鐠佸府绱盋D 閸︺劍鏌囨稉?miss 閺冭埖澧犵挧椋庣暬閿涘潏ooldown_start = on_end閿?
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return;
		state(player).castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
        ShellProjectile bullet = new ShellProjectile(github.com.gengyoubo.sscfa.init.AddonEntities.SPIDER_SWING_BULLET.get(), player.level()); bullet.setOwner(player); bullet.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
				(float) BAL.d("bullet_speed", BULLET_SPEED), 0.0f);
		player.level().addFreshEntity(bullet);
		BULLET_IN_FLIGHT.put(player.getUUID(), bullet);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.3f);
	}

	// ==== 妞嬬偛鑴婄喊鐗堟寬閸ョ偠鐨熼敍鍫㈡暠 ShellProjectile 鐠嬪啰鏁ら敍?====

	/** 妞嬬偛鑴婇崨鎴掕厬閺傜懓娼?閳?闁解晙缍囨潻娑樺弳閹藉棜宕遍妴?*/
	public static void onBulletHitBlock(ServerPlayer player, Vec3 anchor, ShellProjectile bullet) {
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

	/** 閹风繝缍囬惄顔界垼閻ㄥ嫰鐝崗澶嬪伎鏉堢澹婇敍姘辨閸氬秴宕熼崣瀣晽閳帞璞㈤敍灞炬櫕娴滆　鍟嬮拑婵撶礄娴犲懏鏌﹀▔鏇♀偓鍛讲鐟欎緤绱氶妴?*/
	private static int tetherHighlightColor(ServerPlayer player, LivingEntity target) {
		return WhitelistUtils.isProtected(player, target) ? 0x3AF03A : 0x3AA0FF;
	}

	/** 妞嬬偛鑴婇崨鎴掕厬閻㈢喓澧?閳?鏉╃偞甯存潻娑樺弳 tether 閹锋牗瀚块妴?*/
	public static void onBulletHitEntity(ServerPlayer player, LivingEntity target, ShellProjectile bullet) {
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
		s.ropeLen = BAL.d("tether_max_len", TETHER_MAX_LEN); // 閸ュ搫鐣?16 閺嶉棿绗傞梽鎰剁礄娑撳秶鏁ら幏缈犵秶閻剟妫跨捄婵堫瀲閿涘绱伴崗浣筋啅 0~16 閼奉亞鏁遍敍宀冪Т 16 绾剛瀹抽弶鐔稿閸ョ偑鈧椒绗夐崣顖氭闂€?
		s.canExtend = true;
		mana(player).consumeMana(BAL.d("tether_hit_mana_cost", TETHER_HIT_MANA_COST)); // 閸曞彞鑵戦悽鐔哄⒖閻剟妫挎０婵嗩樆閹?8 閻?mana閿涘牐鍤滈崝?clamp 閸?0 娑撳秳绱扮拹鐔粹偓浣藉殰閸斻劌鎮撳銉ヮ吂閹撮顏?mana 閺夆槄绱?
		SscAddonNetworking.sendWebHighlight(player, target.getId(), 40, tetherHighlightColor(player, target)); // 娴犲懏鏌﹀▔鏇♀偓鍛讲鐟欎線鐝崗澶涚礄閸欏鍟楃紒?閺佸奔姹夐拑婵撶礆
		broadcastState(player, s);
	}

	/** 妞嬬偛鑴?miss 閽€钘夋勾濞戝牆銇?/ 鐞氼偆些闂?閳?5 缁?CD閿涘牆绠撶粵澶涚窗娴犲懍绮涢崷銊╊棧閺堫亜鎳℃稉顓熸閻㈢喐鏅ラ敍娌歛lance break_cooldown_ticks 閸欘垵顩惄鏍电礆閵?*/
	public static void onBulletMiss(ServerPlayer player, ShellProjectile bullet) {
        if (BULLET_IN_FLIGHT.remove(player.getUUID(), bullet)) {
			settleBreak(player);
		}
	}

	// ==== 閺傤厺绗?====
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

	/** 閺傤厺绗?miss閿涙碍婀板▎陇宕辨稉婵堢波閺夌噦绱濋幐?power JSON cooldown 鐠ч鐣婚妴?*/
	private static void settleBreak(ServerPlayer player) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, state(player).castId);
	}

	// ==== 濮ｅ繑婀囬崝锛勵伂 tick ====
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

	/** SWINGING閿涙碍鏌囨稉婵囶梾濞?+ 鐎规碍婀￠獮鎸庢尡 + 闁库偓閻愬湱鐭戠€涙劧绱欓悧鈺冩倞閸︺劌顓归幋椋庮伂閿涘鈧?*/
	private static void tickSwinging(ServerPlayer player, SwingState s) {
		player.fallDistance = 0.0f; // 閺堝秴濮熺粩顖涚閹芥棁鎯ょ捄婵堫瀲闂冨弶鎲滈懡陇鎯ら崷鐗堟啹娴笺倧绱欐稉宥呭閸濆秴顓归幋椋庮伂 fallDistance 妞瑰崬濮╅惃?FALL 閸斻劎鏁鹃敍?
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
			// 闁挎氨鍋ｆ稉婵呯隘閿涘潵wner 閹垫挻鐖ｉ敍姘矌閺堫兛姹夐柆鑳唨閿?
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn((ServerLevel) player.level(), player, net.minecraft.core.particles.ParticleTypes.CLOUD,
					s.anchor.x, s.anchor.y, s.anchor.z, 2, 0.12, 0.12, 0.12, 0.0);
		}
	}

	/** TETHER閿涙碍濯洪惄顔界垼閻㈢喓澧块崥鎴犲负鐎硅绱欓幐澶嬪閹呯級閺€鎾呯礉閺堝妯嗛崝娑崇礆+ 閺傤厺绗ｅΛ鈧ù?+ 妤傛ê鍘滈崚閿嬫煀 + 楠炴寧鎸遍妴?*/
	private static void tickTether(ServerPlayer player, SwingState s) {
		player.fallDistance = 0.0f; // 閺堝秴濮熺粩顖涚閹芥棁鎯ょ捄婵堫瀲闂?tether 閹锋牗瀚块拃钘夋勾閹芥柧婵€
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
		double ropeLen = Math.max(s.ropeLen, MIN_ROPE_LEN); // 绾兛绗傞梽鎰剁礄閸掓繂顫?16閿?
		double softLen = Math.max(MIN_ROPE_LEN, ropeLen - TETHER_SOFT_BUFFER); // 鏉烆垱濯洪梼鍫濃偓纭风礄閸掓繂顫?12閿?
		// 閻楅潧绱╅敍?2~16 缁炬寧鈧冨婢堆嶇礄over鑴砅ULL_GAIN閿涘绱濈搾?16 閸氬憡鏁规潻婊咁瀲 + 妫版繂顦绘径褍濮忛悧闈涚穿閿涘牆宸遍幏澶婃礀閵嗕線妲婚崘鎻掑毉 16閿涘绱辩痪顖炩偓鐔峰娑撳秵鏁兼担宥囩枂閵?
		if (dist > softLen) {
			double resist = knockbackResist(living);
			Vec3 dir = pPos.subtract(tPos).normalize(); // 閻╊喗鐖?閳?閻溾晛顔?
			Vec3 tv = living.getDeltaMovement();
			double over = dist - softLen; // 12 閺?0閿?6 閺?4
			double pull = over * TETHER_PULL_GAIN; // 12 鐠ч鍤庨幀褍濮炴径褏澹嶅?
			double cap = 0.55;
			if (dist > ropeLen) {
				pull += (dist - ropeLen) * TETHER_HARD_GAIN; // 鐡?16 妫版繂顦绘径褍濮忛敍鍫㈠殠閹勬纯闂勨槄绱濋棃鐘靛瀵洖濮忛梼鑼剁箖鏉╂嚎鈧椒绗夌涵顒€鎯涢弨鍫曗偓鐔峰閵嗕椒绗夐崡鈽呯礆
				cap = 0.9; // 閺€鎯ь啍闁喎瀹虫稉濠囨缂佹瑥銇囬崝?
			}
			pull *= (1.0 - resist); // 閹稿濮夐幀褝绱版潪鑽ゆ窗閺嶅洣瀵岀憰浣瑰閻╊喗鐖ｉ敍宀勫櫢閻╊喗鐖ｉ棃鐘差吂閹撮顏幎濠勫负鐎硅埖濯烘潻鍥у箵
			if (pull > 0.001) {
				double toward = tv.dot(dir);
				if (toward < pull) tv = tv.add(dir.scale(pull - toward)); // 閸ョ偞濯洪崥鎴犲负鐎?
			}
			double sp = tv.length();
			if (sp > cap) tv = tv.scale(cap / sp); // 闁喎瀹虫稉濠囨闂冭尙鍨庨崘璇х礄鐡?16 閺€鎯ь啍缂佹瑥銇囬崝娑崇礆
			living.setDeltaMovement(tv);
			living.hurtMarked = true;
			living.hasImpulse = true;
		}
		// 妤傛ê鍘滈崚閿嬫煀閿涘牅绮庨弬鑺ョ《閼板懎褰茬憴渚婄幢閸欏鍟楃紒?閺佸奔姹夐拑婵撶礆閳ユ柡鈧棃妾锋０鎴炵槨 10t 閸掕渹绔村▎鈽呯窗閺冨爼鏆?20t閿涘苯宕愮粙瀣敾閺堢喍绗夐梻顏呮焽閿?
		// 閻焦甯€ tether 閺堢喐鐦?tick 娑撯偓娑擃亪鐝崗澶婂瘶閻?~20鑴?閸愭ぞ缍戦敍鍫濐嚠姒?NightmareDreamManager 閹诲繗绔熼崚閿嬫煀閹垯绶ラ敍?
		if (world.getGameTime() % 10L == 0L) {
			SscAddonNetworking.sendWebHighlight(player, living.getId(), 20, tetherHighlightColor(player, living));
		}
		// 閸椻剝顒存穱婵嗙俺閿涙俺顫﹂幏澶涚礄dist>缂佹娊鏆遍敍澶夌稻鏉╃偟鐢绘稉宥呭З 閳?2 缁夋帒鎮楅懛顏勫З閺傤厺绗ｇ憴锝堝姎閿涘牓妲诲闀愮畽閸椻剝顒撮敍?
		if (dist > ropeLen + 0.5) {
			if (!Double.isNaN(s.lastPX)) {
				double dx = pPos.x - s.lastPX, dy = pPos.y - s.lastPY, dz = pPos.z - s.lastPZ;
				if (dx * dx + dy * dy + dz * dz < 0.0016) { // 缁夎濮?< 0.04 閺?
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
	 * 鐎广垺鍩涚粩顖涚槨 2 tick 娑撳﹥濮ゆ稉鈧▎锛勬€€闂€?+ 閺€鑸垫杹閹板繐娴橀敍?1 閺€?/ -1 閺€?/ 0閿涘鈧倹婀囬崝锛勵伂娣団€叉崲鐎广垺鍩涚粩?ropeLen閿涘牐娴嗛崣?+ 閺傤厺绗ｉ崣鍌濃偓鍐跨礆閿?
	 * 閺€鍓ф€€閺夊啫鈻夐幍?mana閿涘牊鐦￠崠鍛⒏ 2 tick 閻ㄥ嫰鍣洪敍澶涚礉mana 娑撳秷鍐婚崚?canExtend=false 楠炴寧鎸遍梼缁橆剾缂佈呯敾閺€鍓ф€€閵嗕總WINGING / TETHER 閸у洭鈧倻鏁ら妴?
	 */
	public static void onReelSync(ServerPlayer player, double clientRopeLen, int reel) {
		SwingState s = STATES.get(player.getUUID());
		if (s == null || (s.state != STATE_SWINGING && s.state != STATE_TETHER)) return;
		// tether 閹峰鏁撻悧鈺嬬窗閺堚偓婢堆囨？鐠?16 娑撴梻顩﹀銏℃杹缂佸啿娆㈤梹鍖＄幢swinging 閼解剝瀛ｆ禒宥呭讲閸?32 楠炶埖鏂佺紒?
		double maxLen = (s.state == STATE_TETHER) ? BAL.d("tether_max_len", TETHER_MAX_LEN) : MAX_ROPE_REACH;
		s.ropeLen = Mth.clamp(clientRopeLen, MIN_ROPE_LEN, maxLen);
		if (s.state == STATE_TETHER) reel = 0; // tether 娑撳秷鍏樺鍫曟毐閾旀稐绗ｉ敍灞芥嫹閻ｃ儲鏂佺紒铏壈閸?
		boolean prev = s.canExtend;
		if (reel < 0) {
			// 鐎广垺鍩涚粩顖涚槨 2 tick 娑撳﹥濮ゆ稉鈧▎鈽呯礉濮ｅ繐瀵橀幍?2 tick 閻ㄥ嫭鏂佺紒鎶藉櫤閿涘湩EEL_SPEED 娑撶儤鐦?tick 閺€鍓ф€€闂€鍨閿涘绱?
			// 娣囨繃瀵旀稉搴″斧濮?tick 娑撳﹥濮ら弮鍓佹祲閸氬瞼娈?mana 濞戝牐鈧鈧喓宸?
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
