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
 * 月织蛛「蛛丝荡漾」次技能（sp_secondary）—??<b>服务端权威??/b>??
 *
 * <p>物理放客户端（{@code SwingPhysicsMixin}），本类只做：发射蛛丝飞弹、mana 权威、断丝检测?
 * TETHER 拖拽目标生物、状态广播。发射改为投射??{@link ShellProjectile}（抛物线飞行，命中方块→
 * 摆??/ 命中生物→拖?tether / miss→落地消失）?
 */
public final class SpiderMoonWeaverSwingManager {

	// ==== 发??/ 飞行（已登记：bullet_speed / mana_per_block / tether_hit_mana_cost；运行时??balance 快照读取 abilities.moon_weaver_swing??====
	private static final float BULLET_SPEED = 1.7f; // 默认；运行时?balance 快照读取
	public static final double MANA_PER_BLOCK = 2.0; // 默认；运行时?balance 快照读取
	public static final double TETHER_HIT_MANA_COST = 8.0; // 默认；运行时?balance 快照读取（勾中生物瞬间额外扣??mana，不足扣??0??

	// ==== 绳??/ 断丝（以??6 参??client ??SwingPhysicsMixin / ShellProjectile 引用做预测镜像，常量保留不迁移） ====
	public static final double MAX_ROPE_REACH = 32.0;
	public static final double TETHER_MAX_LEN = 16.0; // 默认；运行时?balance 快照读取。tether 拴生物硬上限（绝不超，吸收远??+ 大力牵引?
	public static final double TETHER_SOFT_BUFFER = 4.0; // 软拉缓冲：距硬上??4 格??12 格）起线性牵引，?16 大力硬??
	public static final double TETHER_PULL_GAIN = 0.2; // 12~16 线性牵引系数（每??over 增加的牵引速度?
	public static final double TETHER_HARD_GAIN = 0.8; // ??16 每格额外大力牵引系数（强拉回、防冲出??
	public static final double MIN_ROPE_LEN = 1.5;
	public static final double REEL_SPEED = 0.16;
	private static final double OBSCURE_BREAK_BLOCKS = 1.0;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_swing");
	/** 统一冷却服务的稳定技?ID（网络包触发型）??*/
	private static final String SKILL_ID = "my_addon:form_spider_moon_weaver_swing";

	/** 服务端断丝阈值：max_rope_reach 快照?+ 3 格余量（与常量版 BREAK_OVERSTRETCH = MAX_ROPE_REACH + 3.0 等价）?*/
	private static double breakOverstretch() {
		return BAL.d("max_rope_reach", MAX_ROPE_REACH) + 3.0;
	}

	// ==== 状??====
	public static final int STATE_IDLE = 0;
	public static final int STATE_SWINGING = 2;
	public static final int STATE_TETHER = 3; // 连接生物拖??

	private static final class SwingState {
        long castId = -1;
		int state = STATE_IDLE;
		Vec3 anchor = Vec3.ZERO;    // SWINGING 闁库偓閻?
		int tetherEntityId = -1;      // TETHER 目标实??id
		double ropeLen = 0.0;
		boolean canExtend = true;
		int broadcastTick = 0;
		double lastPX = Double.NaN, lastPY = 0, lastPZ = 0; // 卡死检测：??tick 玩家位置
		int stuckTicks = 0;          // 被拉但连续不动的 tick ??
	}

	private static final Map<UUID, SwingState> STATES = new ConcurrentHashMap<>();
	/** 蛛丝飞弹在飞的玩????弹实体（防连?+ 支持飞行中再按键取消；命?miss 时移除）?*/
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
	 * 该玩家是否正拴住（TETHER）指定实体。用于足丝拴生物的伤害增减判定（双向通用）?
	 * 参数?{@link Entity} 以免额外 import；player/target 任一?null 或??TETHER 均返?false??
	 */
	public static boolean isTethering(Entity player, Entity target) {
		if (player == null || target == null) return false;
		SwingState s = STATES.get(player.getUUID());
		return s != null && s.state == STATE_TETHER && s.tetherEntityId == target.getId();
	}

	/**
	 * 反查正拴住（TETHER）指定实体的拴主玩家，无则返??null。用于被拴目标受伤时找拴主（敌我判??伤害分担）??
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

	// ==== 次键：IDLE→发?/ 否则→断??====
	public static void onSecondaryPress(ServerPlayer player) {
		if (!isSpiderMoonWeaver(player)) return;
		SwingState s = state(player);
		if (s.state == STATE_IDLE) {
			// 飞行中再按????取消飞弹（??remove ??onBulletMiss ??5 ??CD??
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

	/** 发射蛛丝飞弹（抛物线投射物，碰撞后回调进入摆?/ tether）??*/
	private static void shootBullet(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return;
		if (BULLET_IN_FLIGHT.containsKey(player.getUUID())) return;
		if (mana(player).getMana() < 1.0) {
			player.displayClientMessage(Component.translatable("message.my_addon.spider_moon_weaver.swing.no_mana"), true);
			return;
		}
		// 发射即登记；CD 在断?miss 时才起算（cooldown_start = on_end??
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

	// ==== 飞弹碰撞回调（由 ShellProjectile 调用?====

	/** 飞弹命中方????钩住进入摆荡?*/
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

	/** 拴住目标的高光描边色：白名单友军→绿，敌人→蓝（仅施法者可见）?*/
	private static int tetherHighlightColor(ServerPlayer player, LivingEntity target) {
		return WhitelistUtils.isProtected(player, target) ? 0x3AF03A : 0x3AA0FF;
	}

	/** 飞弹命中生????连接进入 tether 拖拽?*/
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
		s.ropeLen = BAL.d("tether_max_len", TETHER_MAX_LEN); // 固??16 格上限（不用拴住瞬间距离）：允许 0~16 自由，超 16 硬约束拉回、不可延?
		s.canExtend = true;
		mana(player).consumeMana(BAL.d("tether_hit_mana_cost", TETHER_HIT_MANA_COST)); // 勾中生物瞬间额外??8 ??mana（自?clamp ??0 不会负、自动同步客户??mana 条??
		SscAddonNetworking.sendWebHighlight(player, target.getId(), 40, tetherHighlightColor(player, target)); // 仅施法者可见高光（友军?敌人蓝）
		broadcastState(player, s);
	}

	/** 飞??miss 落地消??/ 被移????5 ??CD（幂等：仅仍在飞未命中时生效；balance break_cooldown_ticks 可覆盖）??*/
	public static void onBulletMiss(ServerPlayer player, ShellProjectile bullet) {
        if (BULLET_IN_FLIGHT.remove(player.getUUID(), bullet)) {
			settleBreak(player);
		}
	}

	// ==== 断??====
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

	/** 断??miss：本次荡丝结束，?power JSON cooldown 起算?*/
	private static void settleBreak(ServerPlayer player) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, state(player).castId);
	}

	// ==== 每服务端 tick ====
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

	/** SWINGING：断丝检??+ 定期广播 + 销点粒子（物理在客户端）??*/
	private static void tickSwinging(ServerPlayer player, SwingState s) {
		player.fallDistance = 0.0f; // 服务端清摔落距离防摆荡落地摔伤（不影响客户端 fallDistance 驱动?FALL 动画?
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
			// 锚点丝云（owner 打标：仅本人避让??
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn((ServerLevel) player.level(), player, net.minecraft.core.particles.ParticleTypes.CLOUD,
					s.anchor.x, s.anchor.y, s.anchor.z, 2, 0.12, 0.12, 0.12, 0.0);
		}
	}

	/** TETHER：拉目标生物向玩家（按抗性缩放，有阻力）+ 断丝检?+ 高光刷新 + 广播?*/
	private static void tickTether(ServerPlayer player, SwingState s) {
		player.fallDistance = 0.0f; // 服务端清摔落距离??tether 拖拽落地摔伤
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
		double ropeLen = Math.max(s.ropeLen, MIN_ROPE_LEN); // 硬上限（初??16??
		double softLen = Math.max(MIN_ROPE_LEN, ropeLen - TETHER_SOFT_BUFFER); // 软拉阈值（初??12??
		// 牵引?2~16 线性加大（over×PULL_GAIN），?16 吸收远离 + 额外大力牵引（强拉回、防冲出 16）；纯速度不改位置??
		if (dist > softLen) {
			double resist = knockbackResist(living);
			Vec3 dir = pPos.subtract(tPos).normalize(); // 目????玩??
			Vec3 tv = living.getDeltaMovement();
			double over = dist - softLen; // 12 閺?0閿?6 閺?4
			double pull = over * TETHER_PULL_GAIN; // 12 起线性加大牵?
			double cap = 0.55;
			if (dist > ropeLen) {
				pull += (dist - ropeLen) * TETHER_HARD_GAIN; // ??16 额外大力（线性更陡，靠牵引力防过远、不硬吸收速度、不卡）
				cap = 0.9; // 放宽速度上限给大?
			}
			pull *= (1.0 - resist); // 按抗性：轻目标主要拉目标，重目标靠客户端把玩家拉过去
			if (pull > 0.001) {
				double toward = tv.dot(dir);
				if (toward < pull) tv = tv.add(dir.scale(pull - toward)); // 回拉向玩??
			}
			double sp = tv.length();
			if (sp > cap) tv = tv.scale(cap / sp); // 速度上限防爆冲（??16 放宽给大力）
			living.setDeltaMovement(tv);
			living.hurtMarked = true;
			living.hasImpulse = true;
		}
		// 高光刷新（仅施法者可见；友军?敌人蓝）——降频每 10t 刷一次：时??20t，半程续期不闪断??
		// 省掉 tether 期??tick 一个高光包??~20??冗余（对??NightmareDreamManager 描边刷新惯例?
		if (world.getGameTime() % 10L == 0L) {
			SscAddonNetworking.sendWebHighlight(player, living.getId(), 20, tetherHighlightColor(player, living));
		}
		// 卡死保底：被拉（dist>绳长）但连续不动 ??2 秒后自动断丝解脱（防永久卡死?
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
	 * 客户端每 2 tick 上报一次绳?+ 收放意图?1 ?/ -1 ?/ 0）。服务端信任客户?ropeLen（转?+ 断丝参考）??
	 * 放绳权威?mana（每包扣 2 tick 的量），mana 不足?canExtend=false 广播阻止继续放绳。SWINGING / TETHER 均适用?
	 */
	public static void onReelSync(ServerPlayer player, double clientRopeLen, int reel) {
		SwingState s = STATES.get(player.getUUID());
		if (s == null || (s.state != STATE_SWINGING && s.state != STATE_TETHER)) return;
		// tether 拴生物：最大间??16 且禁止放绳延长；swinging 荡漾仍可??32 并放?
		double maxLen = (s.state == STATE_TETHER) ? BAL.d("tether_max_len", TETHER_MAX_LEN) : MAX_ROPE_REACH;
		s.ropeLen = Mth.clamp(clientRopeLen, MIN_ROPE_LEN, maxLen);
		if (s.state == STATE_TETHER) reel = 0; // tether 不能延长蛛丝，忽略放绳意??
		boolean prev = s.canExtend;
		if (reel < 0) {
			// 客户端每 2 tick 上报一次，每包?2 tick 的放绳量（REEL_SPEED 为??tick 放绳长度）??
			// 保持与原??tick 上报时相同??mana 消耗速??
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
