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
 * 跳蛛「跳杀（Jump Kill）」主技能（sp_primary）—?服务端状态机?
 *
 * <p><b>蓄力</b>（长?sp_primary）：蓄力越久索敌距离越大（基础 {@value #BASE_DIST} ?/
 * 满蓄 {@value #CHARGE_MAX}t=3 ??{@value #MAX_DIST} 格）；蓄力期移速减半、转视角瞄准?
 * ?tick 扫描前方视锥内可扑目标：全部<b>红色描边</b>、准星最接近?b>绿色描边</b>（仅本人可见）?/p>
 *
 * <p><b>跳杀</b>（松开）：以绿色锁定目标（无则按准星落点）向前跳扑——{@code setNoGravity} + ?tick
 * 施指向目标的「拉力」被拽过去（第六条），距离越远起跳越高（第五条）。命中造成 {@value #DAMAGE}
 * 物理 + 中毒 II {@value #POISON_DURATION}t(8s) + 定身 {@value #STUN_DURATION}t(0.35s)?/p>
 *
 * <p><b>空中追踪（第七条?/b>：追踪时扑向目标<b>提前量拦截点</b>（当前位?+ 速度×{@value #LEAD_TICKS}），
 * 加大更正范围；脱锁容差以「起跳时锁定位置 lockPos0」为圆心、半?= {@value #LEASH_K}×跳跃距离
 * （跳越远容差越大、越近越小）；目标移出容差即脱锁、按最后方向直飞扑空?/p>
 *
 * <p><b>CD</b>：扑中按 power JSON cooldown、扑空按 fail_cooldown?
 * 白名单：默认白名单（护玩?+ 宠物/召唤物）。全判定服务端，跳跃?tick 补发速度包给客机?/p>
 */
public final class JumpKillManager {

	// 以下已登记常量均为默认值；运行时从 balance 快照读取（abilities.jump_kill?
	private static final int CHARGE_MAX = 60;        // 满蓄 3 ?
	private static final double BASE_DIST = 3.0;      // 基础索敌 3 ?
	private static final double MAX_DIST = 16.0;      // 满蓄索敌 16 ?
	private static final float DAMAGE = 8.0f;
	private static final int POISON_DURATION = 160;  // 中毒 II 8 ?
	private static final int POISON_AMPLIFIER = 1;   // 中毒 II
	private static final int STUN_DURATION = 7;      // 定身 0.35 ?
	private static final String SKILL_ID = "my_addon:form_spider_salticidae_jump_kill";
	private static final double CHARGE_SLOW = -0.5;  // 蓄力移?×0.5
	private static final double LEASH_K = 0.4;       // 脱锁容差系数（半?= LEASH_K × 跳跃距离?
	private static final int LEAD_TICKS = 6;         // 提前量：扑向 目标位置 + 速度 × LEAD_TICKS
	private static final double LEAP_SPEED = 0.85;   // 跳跃水平拉力速度
	private static final int MAX_LEAP_TICKS = 40;    // 跳跃硬上?2 ?
	private static final double HIT_RADIUS = 1.3;    // 命中判定距离
	private static final double SCAN_COS = 0.978;    // 锁定视锥半角 ?12°（只锁准星附近目标）
	private static final double GRAVITY_STEP = 0.08; // 跳跃期手动抛物：?tick 竖直速度衰减? 重力加速度?
	private static final double TURN_LERP = 0.3;     // 水平转向平滑系数（防接近目标时左右摆动）
	private static final int HIGHLIGHT_TICKS = 15;   // 描边时长（留刷新余量防闪烁）
	private static final int CHARGE_CRY_COUNT = 5;   // 蓄力期蜘蛛嘶鸣总次数（仅自己听?
	private static final int COLOR_RED = 0xFF3030;   // 候选目标红?
	private static final int COLOR_GREEN = 0x30FF30; // 锁定目标绿边
	// ===== 安全丝（保命拉回?=====
	private static final double SILK_MAX = 48.0;     // 丝线最?48 格，超过即断
	private static final int RECALL_WINDOW = 160;    // 跳杀结束?8 秒内可拉?
	private static final double RECALL_SPEED = 1.15; // 拉回速度（比跳跃快，快速拽回）
	private static final double RECALL_ARRIVE = 1.6; // 拉回到达阈值（距锚点小于即停）
	private static final int RECALL_STALL_TICKS = 5; // 拉回连续 5t 距离不拉??外力过强，丝?
	private static final double OBSCURE_BREAK_BLOCKS = 1.5; // 丝线被实心方块遮挡累计阈值（采样 0.5 ?个：允许?1.5 格厚墙，? 格断?

	private static final UUID SLOW_UUID = UUID.fromString("b7e2c9a4-3f81-4d6e-9a25-7c1e0f4d82ab");

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
	private static final BalanceReader BAL = new BalanceReader("abilities.jump_kill");

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	/** 安全丝拉回窗口：跳杀结束后锚点保留的时段（超时即失效）?*/
	private static final Map<UUID, SilkAnchor> ANCHORS = new ConcurrentHashMap<>();

	private static final class SilkAnchor {
		final Vec3 pos;        // 丝线锚点（起跳点?
		final long validUntil;  // 世界时间：超过即失效
		double lastDist = -1;   // 拉回期：?tick 到锚点距离（断丝判定用）
		int stall = 0;          // 拉回期：距离未拉近连?tick ?
		SilkAnchor(Vec3 pos, long validUntil) { this.pos = pos; this.validUntil = validUntil; }
	};

	private static final class State {
        long castId = -1;
		int chargeMax = CHARGE_MAX;
		float killHealing;
		int strengthTicks;
		int phase = 0;        // 0=蓄力 1=跳跃 2=拉回
		int chargeTick = 0;
		// 璺宠穬鏈?
		int leapTick = 0;
		UUID lockTarget;      // 跳跃锁定目标（null=纯位移）
		Vec3 lockPos0;       // 起跳时锁定目标位置（脱锁容差圆心?
		double jumpDist;      // 起跳时到目标的距离（决定跳高 + 容差?
		double flightTime;    // 预计飞行 tick（协调抛物线落点?
		Vec3 lastDir = new Vec3(0, 0, 1); // 水平拉力方向（单位向量，y=0；平滑插值追踪）
		double vy0;           // 起跳竖直初速（协调抛物：flightTime 后竖直到目标高度?
	}

	private JumpKillManager() {}

	private static boolean isSalticidae(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE);
	}

	/** 客户端按?sp_primary：优先判「安全丝拉回」（6s 窗口内，绕过 CD——跳杀 CD 8~12s 必然覆盖窗口）；否则开始蓄力?*/
	public static void startCharge(ServerPlayer player) {
		if (!isSalticidae(player)) return;
		// 安全丝拉回：跳杀结束?6s 内再按主??快速拽回锚点（不进蓄力、不?CD 阻挡?
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor != null) {
			if (player.serverLevel().getGameTime() > anchor.validUntil
					|| player.position().distanceTo(anchor.pos) > SILK_MAX) {
				// 窗口超时 / 距锦点过远：丝已断（带断丝特效）
				if (player.level() instanceof ServerLevel sw0) snapSilk(player, sw0, anchor);
				ANCHORS.remove(player.getUUID());
				SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
			} else {
				startRecall(player, anchor);
				return;
			}
		}
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return; // CD 涓?
		if (STATES.containsKey(player.getUUID())) return; // 施法中不可重?
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
			// 蓄力起手：低沉蜘蛛嘶鸣（蓄势?
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);
		}
	}

	/** 客户端松开 sp_primary ?跳杀?*/
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

		// 松开瞬间按当前准星重新锁定（准星最接近的候选）
		LivingEntity lock = pickLockTarget(player, sw, scanDist);

		s.phase = 1;
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID, s.castId);
		s.leapTick = 0;
		Vec3 look = player.getLookAngle().normalize();
		s.lastDir = look;

		// 安全丝：锚点必须落在实地——向下检?2 格内的首个实心方块顶面（脚离?2 格内起跳 ?丝转移到地面；超?2 格空??不生成）
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
		// 协调抛物线起跳：计算使抛物线 flightTime 后水?竖直同时抵达落点（轨迹流畅，距离越远抛物越高?
		Vec3 aim = (lock != null) ? bodyCenter(lock) : player.getEyePosition().add(look.scale(s.jumpDist));
		Vec3 horiz = new Vec3(aim.x - player.getX(), 0, aim.z - player.getZ());
		double horizDist = horiz.length();
		if (horizDist < 1.0e-4) { horiz = new Vec3(look.x, 0, look.z); horizDist = Math.max(1.0e-4, horiz.length()); }
		horiz = horiz.normalize();
		double dy = aim.y - player.getY();
		double T = Math.max(6.0, horizDist / leapSpeed);
		s.flightTime = T;
		s.vy0 = dy / T + 0.5 * GRAVITY_STEP * T; // T tick 后竖直到?aim.y；T 越大（距离越远）起跳越高
		s.lastDir = horiz;
		player.setDeltaMovement(horiz.x * leapSpeed, s.vy0, horiz.z * leapSpeed);
		player.hurtMarked = true;
		pushVelocity(player);

		// 起跳音效：蜘蛛扑击嘶?+ 破空
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.8f);
	}

	/** 每服务端 tick 对每个在线玩家调用?*/
	public static void tick(ServerPlayer player) {
		State s = STATES.get(player.getUUID());
		SpiderSalticidaePerks.setSensing(player, s != null && s.phase == 0 && s.strengthTicks > 0);
		if (s == null) return;
		if (player.isDeadOrDying() || !isSalticidae(player)) { cancel(player); return; }
		if (!(player.level() instanceof ServerLevel sw)) return;

		if (s.phase == 0) {
			tickCharge(player, sw, s);
		} else if (s.phase == 1) {
			checkSilk(player); // 跳跃期丝线检查：?32 ?/ 被遮??丝断（拉回窗口作废）
			if (STATES.containsKey(player.getUUID())) tickLeap(player, sw, s); // 丝断不清跳跃态，仅作废锦?
		} else {
			tickRecall(player, sw, s);
		}
	}

	/** 锦点存在 tick：纯锦点（无活动状态）也要推进 8 秒倒计时——到时丝线断裂消失（带特效）?*/
	public static void tickAnchors(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor == null) return;
		State s = STATES.get(player.getUUID());
		if (s != null && (s.phase == 1 || s.phase == 2)) return; // 跳跃/拉回中：窗口?finishRecall 重置计时
		if (player.serverLevel().getGameTime() > anchor.validUntil) {
			if (player.level() instanceof ServerLevel sw) snapSilk(player, sw, anchor);
			ANCHORS.remove(player.getUUID());
			SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0); // 丝线到时断裂（客户端同步移除?
		}
	}

	/** 开始安全丝拉回：锚点从窗口表转入活动状态，phase=2?*/
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
			// 拉回起手：蛛?+ 三叉戟拖拽声
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.9f, 1.3f);
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.9f, 1.5f);
		}
	}

	/** 拉回推进：每 tick 向锦点直线拽（三维直?+ 距离比例减速），被外力压过拉不近（连续 5t 距离不缩短）或撞??丝断放弃?*/
	private static void tickRecall(ServerPlayer player, ServerLevel sw, State s) {
		s.leapTick++;
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (anchor == null) { finishRecall(player, false); return; }
		if (s.leapTick > 60) { finishRecall(player, true); return; } // 硬上?3s

		double dist = player.position().distanceTo(anchor.pos);
		// 断丝判定：撞墙卡?/ 连续?tick 距离不缩短（外力过强拽不回来? 丝线被遮?
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

		// 到达锦点附近 ?拉回成功（清水平速度防冲过头乱晃?
		if (dist < RECALL_ARRIVE) {
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * 0.1, v.y, v.z * 0.1);
			player.hurtMarked = true;
			finishRecall(player, true);
			return;
		}

		// 拽向锦点：三维直?+ 距离比例减速（远处快、临近自动刹车，不冲过锦点不乱晃?
		Vec3 to = anchor.pos.subtract(player.position());
		Vec3 dir3 = to.normalize();
		double speed = Math.min(RECALL_SPEED, dist / 3.0 + 0.25); // 临近自动降速（?1 ??0.58/t?
		speed = Math.max(0.3, speed);
		player.setDeltaMovement(dir3.x * speed, Math.max(dir3.y * speed, -0.5), dir3.z * speed);
		player.hurtMarked = true;
		player.fallDistance = 0.0f;
		pushVelocity(player);

		// 丝线视觉：沿线每 2 格一个蛛网粒子（拉回过程短暂，开销可控? 尾迹
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

	/** 丝断反馈：绳结断裂音 + 白色丝线粒子?*/
	private static void snapSilk(ServerPlayer player, ServerLevel sw, SilkAnchor anchor) {
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.9f, 1.4f);
		// owner 打标：仅本人第一人称避让，他人视角原?
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY(0.5), player.getZ(),
				10, 0.2, 0.2, 0.2, 0.02);
	}

	/**
	 * 落地锚点：从玩家脚部向下?2 格，返回首个「可站立方块」（有外形轮廓即可—?
	 * 栏杆/栅栏/半砖/台阶/门等非实心但可碰撞均可）的坐标；
	 * 空中（脚?2 格无地）返回 null = 不生成蛛丝（脚离?2 格内 ?丝转移到地面；超?2 ??消失）?
	 */
	private static Vec3 groundAnchor(ServerPlayer player) {
		BlockPos feet = player.blockPosition();
		for (int dy = 0; dy <= 2; dy++) { // 0（脚部）?1?2 三档：覆?2 格检?
			BlockPos check = feet.below(dy);
			if (isStandable(player, check)) {
				return new Vec3(check.getX() + 0.5, check.getY() + 1.0, check.getZ() + 0.5); // 方块顶面中心
			}
		}
		// 脚部、下一格、下两格都空：再精确检测半格（玩家可能站在方块边缘，脚?y 偏上?
		BlockPos half = BlockPos.containing(player.getX(), player.getY() - 2.5, player.getZ());
		if (isStandable(player, half)) {
			return new Vec3(half.getX() + 0.5, half.getY() + 1.0, half.getZ() + 0.5);
		}
		return null; // 空中：无地可锚，不生成丝
	}

	/** 可站立判定：方块外形轮廓非空（栏?栅栏/半砖/台阶/门等有碰撞面即算，空?草丛不算）?*/
	private static boolean isStandable(ServerPlayer player, BlockPos pos) {
		net.minecraft.world.level.block.state.BlockState state = player.level().getBlockState(pos);
		return !state.getShape(player.level(), pos).isEmpty();
	}

	/** 拉回结束：恢复重力、清锦点（无论成败，拉回机会只用一次；丝线耗尽也播断丝特效）?*/
	private static void finishRecall(ServerPlayer player, boolean arrived) {
		PlayerGravityLease.release(player, SKILL_ID);
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		if (player.level() instanceof ServerLevel sw) {
			if (anchor != null) snapSilk(player, sw, anchor); // 丝线拉完耗尽：同样断裂反?
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
		boolean justReachedMax = s.chargeTick == chargeMax - 1; // 下一行递增后恰好满蓄（一次性边沿，防满蓄后?tick 重响?
		if (s.chargeTick < chargeMax) s.chargeTick++;
		double ratio = Math.min(1.0, (double) s.chargeTick / chargeMax);
		double scanDist = BAL.d("base_dist", BASE_DIST) + ratio * (BAL.d("max_dist", MAX_DIST) - BAL.d("base_dist", BASE_DIST));

		// 扫描候选：索敌距离内全部可扑目标（无遮挡才显示/可锁）——红边；准星 12° 锥内最近者绿边锁?
		List<LivingEntity> cands = scanCandidates(player, sw, scanDist);
		LivingEntity best = pickInCrosshair(player, cands);

		for (LivingEntity e : cands) {
			int color = (e == best) ? COLOR_GREEN : COLOR_RED;
			SscAddonNetworking.sendWebHighlight(player, e.getId(), HIGHLIGHT_TICKS, color);
		}

		// 蓄力音效：全程共 5 声上升嘶鸣（仅自己听），间隔均分；满蓄瞬间额外一声经验叮声（边沿触发，防满蓄后循环响?
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
		// 超时和撞墙立即结束；落地前先核对命中，避免同 tick 的有效接触被判成扑空?
		if (s.leapTick > s.flightTime + 15 || s.leapTick > BAL.i("max_leap_ticks", MAX_LEAP_TICKS)
				|| (s.leapTick > 3 && player.horizontalCollision)) {
			finish(player, false);
			return;
		}

		Vec3 playerPos = bodyCenter(player);
		Vec3 targetPoint;

		LivingEntity target = (s.lockTarget != null) ? resolve(sw, s.lockTarget) : null;
		if (target != null && target.isAlive()) {
			// 脱锁判定：目标偏离起跳锁定位置超过容差（半径随跳跃距离线性）
			double leash = LEASH_K * s.jumpDist;
			if (bodyCenter(target).distanceTo(s.lockPos0) > leash) {
				s.lockTarget = null; // 脱锁 ?沿最后方向直飞扑?
				targetPoint = playerPos.add(s.lastDir.scale(10));
			} else {
				// 提前量拦截点（第七条）：扑向 目标当前位置 + 速度 × LEAD_TICKS
				targetPoint = bodyCenter(target).add(target.getDeltaMovement().scale(LEAD_TICKS));
				// 命中判定
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

		// 只有未命中目标的落地才结算扑空；不延长落地后的追击时间?
		if (s.leapTick > 5 && player.onGround()) {
			finish(player, false);
			return;
		}

		// 施力：水平方向平滑插值转向（防接近目标时左右摆动? 协调抛物竖直（流畅曲线）
		Vec3 desired = new Vec3(targetPoint.x - playerPos.x, 0, targetPoint.z - playerPos.z);
		if (desired.lengthSqr() > 1.0e-4) {
			desired = desired.normalize();
			s.lastDir = s.lastDir.lerp(desired, TURN_LERP);
			if (s.lastDir.lengthSqr() > 1.0e-4) s.lastDir = s.lastDir.normalize();
		}
		double leapSpeed = BAL.d("leap_speed", LEAP_SPEED);
		double vy = s.vy0 - GRAVITY_STEP * s.leapTick; // 抛物竖直（不重算，保持平滑）
		player.setDeltaMovement(s.lastDir.x * leapSpeed, vy, s.lastDir.z * leapSpeed);
		player.hurtMarked = true;
		player.fallDistance = 0.0f;
		pushVelocity(player);

		// 跳跃拖尾：蛛丝微粒（owner 打标：仅本人第一人称避让?
		if (s.leapTick % 2 == 0) {
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CRIT, player.getX(), player.getY(0.5), player.getZ(),
					2, 0.1, 0.1, 0.1, 0.0);
		}
	}

	/** 命中结算? 物理 + 中毒 II 8s（毒液腺体：等级+1 / 时长×70%? 定身 0.35s + 音效粒子?*/
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

	/** 跳跃结束：恢复重力、进 CD、清状态?*/
	private static void finish(ServerPlayer player, boolean hit) {
        State finishing = STATES.get(player.getUUID());
        if (finishing == null) return;
		PlayerGravityLease.release(player, SKILL_ID);
		// 扑中 = 成功 CD（cooldown），扑空 = 失败 CD（fail_cooldown?
		if (hit) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, finishing.castId);
		else github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(player, SKILL_ID, finishing.castId);
		STATES.remove(player.getUUID());
	}

	/** 跳跃期丝线检查：离锚点超 32 ?/ 丝线被实心方块遮??丝断（锚点失效，无拉回）?*/
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

	/** 丝线遮挡量：沿丝线采样累计实心方块数（同月织蛛算法，两端各跳?0.6 格防贴墙误断）?*/
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

	/** 取消（死?丢形?断线）：移除减速、恢复重力、清状态与锦点（带断丝特效），不进 CD?*/
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

	/** 断线/停服清理：锦点一并清?*/
	public static void clearPlayer(UUID uuid) {
		STATES.remove(uuid);
		ANCHORS.remove(uuid);
	}

	public static void clearAll() {
		STATES.clear();
		ANCHORS.clear();
	}

	/** 跳跃期免疫：玩家正在跳杀且伤害源攻击者是其锁定目??免疫（mixin 查询）?*/
	public static boolean isLeapingAgainst(ServerPlayer player, Entity attacker) {
		if (attacker == null) return false;
		State s = STATES.get(player.getUUID());
		if (s == null || s.phase != 1) return false;
		return attacker.getUUID().equals(s.lockTarget);
	}

	/** 毒液技能用：安全丝锚点是否存在且在有效窗口内（=「有丝线连着自己」）?*/
	public static boolean hasActiveSilk(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.get(player.getUUID());
		return anchor != null && player.serverLevel().getGameTime() <= anchor.validUntil
				&& player.position().distanceTo(anchor.pos) <= SILK_MAX;
	}

	/** 毒液技能用：消耗丝线（荡丝冲刺用掉锚点，带断丝特效与客户端同步）?*/
	public static void consumeSilk(ServerPlayer player) {
		SilkAnchor anchor = ANCHORS.remove(player.getUUID());
		if (anchor != null) {
			if (player.level() instanceof ServerLevel sw) snapSilk(player, sw, anchor);
			SscAddonNetworking.syncJumpKillSilk(player, false, 0, 0, 0);
		}
	}

	/** 松开时按当前准星锁定：从索敌候选（含遮挡过滤）里取准星 12° 锥内最近者?*/
	private static LivingEntity pickLockTarget(ServerPlayer player, ServerLevel sw, double scanDist) {
		List<LivingEntity> cands = scanCandidates(player, sw, scanDist);
		return pickInCrosshair(player, cands);
	}

	/** 索敌候选：scanDist 内全部活体（非自?非旁?非白名单），且视线无方块遮挡（遮?= 跳不过去 ?不显示不可锁）?*/
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

	/** 准星锁定：候选中与准星方向夹角在 12° 锥内、且与准星最接近者?*/
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

	/** 遮挡检测：玩家眼睛→目标身体中心射线（碰撞箱形状，忽略流体）不命中任何方块 = 无遮挡?*/
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
		// 玩家移动客户端权威：显式补发速度包，强制主机/客机应用跳跃位移（冲刺技踩过坑）
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
