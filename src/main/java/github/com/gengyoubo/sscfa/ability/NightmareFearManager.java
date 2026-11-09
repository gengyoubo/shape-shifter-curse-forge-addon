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
 * 食梦魔（Nightmare）主要技能「恐惧」（Fear）—??服务端权威状态机??
 *
 * <p>按键（sp_primary）触发：??b>所有已入梦</b>目标施加恐惧（无论距离），要求技??CD 就绪?
 * 恐惧持续 15 秒??00t），期间??/p>
 * <ul>
 *   <li>目标视野缩??16 格（客户端粉雾渐进淡入，仅本人可见）+ 减??15%??/li>
 *   <li>守卫者式心跳声（服务端周期广播，本人可闻）??/li>
 *   <li>目标的入梦到期时??b>持续锁定??20s</b>（??tick 刷新?now+400）??/li>
 *   <li>??b>任何食梦??/b>伤????（??{@code SscAddonLivingEntityMixin} damage 钩子）；</li>
 *   <li>梦魔进入目??16 格视???目标客户端获??1 秒「看不见该梦魔」窗口（渲染屏蔽，仅本地）?/li>
 * </ul>
 * <p>恐惧结束：强制出??+ 20s??00t）入梦免疫（{@code NightmareDreamManager} ??IMMUNE 表）?/p>
 *
 * <p>客户端包 {@code PACKET_FEAR_STATE}（S2C，仅目标本人）：varint durationTicks??
 * 粉雾淡入/心跳渲染/1s 隐身判定全在客户?{@code NightmareFearClient}??/p>
 */
public final class NightmareFearManager {

	// ==== 以下常量为默认值；运行时从 balance 快照读取（scope: abilities.nightmare_fear，数据包可覆盖）====
	/** 恐惧持续 tick??5 秒）。默认；运行时??balance 快照读取??*/
	public static final int FEAR_DURATION_TICKS = 300;
	/** 梦魇戒指：恐惧持续时长增幅??35% ??405t ??20.25 秒），施加瞬间快照。未登??balance，保持常量??*/
	public static final float FEAR_DURATION_RING_BONUS = github.com.gengyoubo.sscfa.item.NightmareRingItem.FEAR_DURATION_BONUS;
	/** 技??CD（tick??0 秒）。默认；运行时??balance 快照读取??*/
	public static final int FEAR_COOLDOWN_TICKS = 400;
	/** 诅咒之月共鸣：诅咒之月当夜恐?CD 降??14 秒??80t）。默认；运行时从 balance 快照读取??*/
	public static final int FEAR_COOLDOWN_TICKS_CURSED_MOON = 280;
	/** 恐惧结束后入梦免疫时长（tick??0 秒）。默认；运行时??balance 快照读取??*/
	public static final int DREAM_IMMUNE_TICKS = 400;
	/** 心跳音效间隔（tick??.6 ??拍——守卫者心跳节奏）。未登记 balance，保持常量??*/
	public static final int HEARTBEAT_INTERVAL = 32;
	/** 减??20% 的属?modifier UUID（固??UUID，可幂等移除）?*/
	private static final UUID FEAR_SLOW_UUID = UUID.fromString("e3a1f7c2-9b4d-4e6a-8c15-d2f3a7b9e810");
	private static final String FEAR_SLOW_NAME = "Nightmare Fear Slow";
	/** 减速幅度（用户定稿：必备减?20%，玩??生物一致）。默认；运行时从 balance 快照读取??*/
	public static final float FEAR_SLOW_RATIO = 0.20f;
	/** 非玩家目标的仇恨压制：被梦魔攻击后的反击窗口（tick?? 秒）。默认；运行时??balance 快照读取??*/
	public static final int MOB_AGGRO_WINDOW_TICKS = 40;
	/** 恐惧视野半径（格）。默认；运行时从 balance 快照读取??*/
	public static final double FEAR_SIGHT_RADIUS = 16.0;
	/** 可见性脉冲：隐匿相位时长（tick?? 秒）。未登??balance，保持常量??*/
	public static final int PULSE_HIDDEN_TICKS = 40;
	/** 可见性脉冲：现形相位时长（tick??.6 秒）。未登??balance，保持常量??*/
	public static final int PULSE_VISIBLE_TICKS = 12;
	/** 攻击显形时长（tick??.5 秒）。默认；运行时??balance 快照读取??*/
	public static final int REVEAL_ON_ATTACK_TICKS = 30;

	/** 服务端权??balance 快照读取（快照未初始化时回退上方默认常量）??*/
	private static final BalanceReader BAL = new BalanceReader("abilities.nightmare_fear");
	/** 统一冷却服务的稳定技?ID?? power 注册路径）??*/
	private static final String SKILL_ID = "my_addon:form_wild_cat_nightmare_fear";
	/** 范围外完全隐匿的包刷新间隔（tick）??*/
	private static final int OUT_OF_RANGE_HIDE_REFRESH = 40;
	/** 心跳基础音量（范围??无梦魔时的极小固定音量）??*/
	private static final float HEARTBEAT_BASE_VOL = 0.15f;
	/** 心跳最大音量（梦魔贴脸时）?*/
	private static final float HEARTBEAT_MAX_VOL = 1.2f;

	/** 可见性状态机：目标UUID|梦魔UUID -> 脉冲相位状态??*/
	private static final Map<String, PairState> PAIRS = new ConcurrentHashMap<>();

	/** 单对（目标×梦魔）的可见性脉冲状态?*/
	private static final class PairState {
		/** 当前是否隐匿相位??*/
		boolean hidden;
		/** 当前相位切换时刻?? = 尚未初始??刚从范围外回来）??*/
		long phaseEnd;
		/** 上次发隐匿包的时刻（范围外周期刷新用）?*/
		long lastHideSent;
	}

	/** 梦魔攻击恐惧目标时调用（damage mixin）：显形 1.5s 并重置该对的脉冲相位?*/
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

	/** 清理某目标的全部可见性状态（恐惧结束/断线）?*/
	private static void clearPairs(UUID tid) {
		PAIRS.keySet().removeIf(k -> k.startsWith(tid + "|"));
	}

	/** 恐惧中目标：目标 UUID -> FearState{到期时间, 施恐惧的梦魔 UUID}??*/
	private static final Map<UUID, FearState> FEARING = new ConcurrentHashMap<>();
	/** 入梦免疫表：目标 UUID -> 免疫到期世界时间（恐惧结束惩罚窗口）??*/
	private static final Map<UUID, Long> DREAM_IMMUNE = new ConcurrentHashMap<>();

	private NightmareFearManager() {
	}

	/** 目标当前是否处于恐惧中（服务端判定）?*/
	public static boolean isFeared(UUID targetUuid, long now) {
		FearState v = FEARING.get(targetUuid);
		return v != null && v.endTick > now;
	}

	/**
	 * 一次性消耗「双倍伤害」机会（用户规格：整个恐惧期间只触发一次）??
	 * 处于恐惧中且本次恐惧尚未用????标记已用并返?true（该次伤????）；否则返回 false??
	 * 恐惧重新施加?FearState 重建，新一轮恐惧重新可触发一次??
	 */
	public static boolean tryConsumeDoubleDamage(UUID targetUuid, long now) {
		FearState v = FEARING.get(targetUuid);
		if (v == null || v.endTick <= now || v.doubleDamageUsed) return false;
		// 梦魇戒指（施加时快照）：本轮恐惧不提供首次伤害翻?
		if (!v.doubleDamageAllowed) return false;
		v.doubleDamageUsed = true;
		return true;
	}

	/** 诅咒之月共鸣：当夜恐?CD 改用 power JSON extra_cooldowns.cursed_moon。仅服务端调用?*/
	private static void applyCursedMoonCooldown(ServerPlayer player) {
		if (net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isInCursedMoon(player.level())) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, SKILL_ID,
					github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.extra(player, SKILL_ID, "cursed_moon"));
		}
	}

	/** 目标当前是否入梦免疫（恐惧结束后?20s 惩罚窗口）?*/
	public static boolean isDreamImmune(UUID targetUuid, long now) {
		Long until = DREAM_IMMUNE.get(targetUuid);
		return until != null && until > now;
	}

	/**
	 * 主要技能「恐惧」入口（power action 调用，仅服务端）??
	 * 对所有已入梦目标施加恐惧；无入梦目标时返??false（不?CD，白提示）??
	 */
	public static boolean execute(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) return false;
		// CD 门禁已由 power ??begin 统一拦截（??P1-1：去掉内部二?canBegin 防自锁）

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

	/** 对单目标施加恐惧：状态入??+ 减??+ ??+ 音效（过渡：先低沉震颤音预告，再起心跳）??
	 * 梦魇戒指（施加瞬间快照）：持续时?+35%；双倍伤害整轮禁用。进行中的恐惧不受中途??摘影响?*/
	private static void startFear(ServerLevel world, ServerPlayer caster, LivingEntity target, long now) {
		UUID tid = target.getUUID();
		// 戒指快照：施加瞬间判定佩戴状态，写入本??FearState（时长增??+ 双倍伤害禁用）
		boolean ringWorn = github.com.gengyoubo.sscfa.item.NightmareRingItem.isWearingBy(caster);
		int baseDuration = BAL.i("duration_ticks", FEAR_DURATION_TICKS);
		int duration = ringWorn ? Math.round(baseDuration * (1.0f + FEAR_DURATION_RING_BONUS)) : baseDuration;
		FEARING.put(tid, new FearState(now + duration, caster.getUUID(), !ringWorn));
		// 入梦时间重置?20s（规格②：获得恐惧即重置?
		NightmareDreamManager.resetDream(caster.getUUID(), tid, now);
		// 减??20%（幂等：先移除再加；用户定稿：必备减速，玩??生物一致）
		AttributeInstance attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr != null) {
			float slowRatio = (float) BAL.d("slow_ratio", FEAR_SLOW_RATIO);
			attr.removeModifier(FEAR_SLOW_UUID);
			attr.addPermanentModifier(new AttributeModifier(
					FEAR_SLOW_UUID, FEAR_SLOW_NAME, -slowRatio, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		// 客户端包（粉雾淡??+ 心跳启动 + 本地失明驱动），仅目标本人（时长随戒指快照变化??
		if (target instanceof ServerPlayer sp) {
			SscAddonNetworking.sendFearState(sp, duration);
		}
		// 施加过渡音效：低沉梦境震颤（目标位置，全员可闻但音量克制?? 目标本人听到尖啸耳??
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
				net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.45f);
		if (target instanceof ServerPlayer sp) {
            MancianimaMarkManager.playSoundToPlayer(sp,net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE,0.7f,1.6f);
		}
		// 爆发粒子：粉紫梦境尘环绕目标一圈（他人可见的视觉信号??
		world.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				24, 0.5, 0.6, 0.5, 0.03);
		world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				6, 0.3, 0.5, 0.3, 0.02);
	}

	/** 梦魔玩家??tick 推进（由 SscAddonServerEvents 调用）?
	 * 修复：只推??<b>本梦魔自己施?/b> 的恐惧条目（此前任意在线玩家 tick 都遍历全表??
	 * 多人在线时状态机重复执行导致隐匿/现形包互相打???16 格内偶发突然现形）??*/
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
			// 仅施恐惧的梦魔本人的 tick 推进该条目（去重??
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
			// 规格②：恐惧期间入梦时间持续锁定??20s
			NightmareDreamManager.lockDream(v.casterUuid, tid, now);
			// 心跳声（规格⑤）：按最近的食梦魔距离动态音量——范围外极小固定音量??
			// 16 格内越近越响（音??= 基??+ (1-距??16)??最??基????
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
			// 可见性状态机（目标本人??tick 推进，另一重去重；多人多梦魔各自独立??
			if (tid.equals(player.getUUID()) && target instanceof ServerPlayer sp) {
				tickVisibility(world, sp, now);
			}
		}
		// 免疫表清??
		DREAM_IMMUNE.values().removeIf(until -> until <= now);
	}

	/** 可见性状态机（规格①②③）：
	 * <ul>
	 *   <li>16 格范围外 ??完全隐身（周期性重发隐匿包保活）??/li>
	 *   <li>16 格????脉冲：隐 2s ????0.6s 循环（进入范围时从隐匿相位开始）??/li>
	 *   <li>攻击 ????1.5s 并重置相位（onNightmareAttackFeared）??/li>
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
				// 规格①：范围外完全隐身（??2s 重发一次隐匿包保活，防包丢失导致现形??
				if (now - ps.lastHideSent >= OUT_OF_RANGE_HIDE_REFRESH) {
					ps.lastHideSent = now;
					ps.hidden = true;
					ps.phaseEnd = now + OUT_OF_RANGE_HIDE_REFRESH + 20;
					SscAddonNetworking.sendFearHide(target, nightmare.getUUID(), OUT_OF_RANGE_HIDE_REFRESH + 20);
				}
				continue;
			}
			// 范围内：脉冲相位推进（phaseEnd 到期切换??
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

	/** 恐惧结束：移除减速、发结束包、强制出?+ 免疫。applyImmune=false 用于目标死亡场景??*/
	private static void endFear(ServerLevel world, UUID tid, UUID casterUuid, long now, boolean applyImmune) {
		if (world.getEntity(tid) instanceof LivingEntity target) {
			AttributeInstance attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (attr != null) attr.removeModifier(FEAR_SLOW_UUID);
			if (target instanceof ServerPlayer sp) {
				SscAddonNetworking.sendFearState(sp, 0);
			}
		}
		clearPairs(tid);
		// 强制出梦（由施恐惧的梦魔的入梦表移除该目标；其它梦魔若也入梦了它则一并清——恐惧结束强制全出）
		NightmareDreamManager.forceWakeAll(tid, now, world);
		if (applyImmune) {
			DREAM_IMMUNE.put(tid, now + BAL.i("dream_immune_ticks", DREAM_IMMUNE_TICKS));
		}
	}

	/** 梦魔断线/死??失形清理：其施加的恐惧一并结束（目标恢??+ 免疫照常）?*/
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

	/** 恐惧状态（到??+ 施恐惧的梦魔 + 双倍伤害开?已用标记）??*/
	private static final class FearState {
		final long endTick;
		final UUID casterUuid;
		/** 梦魇戒指快照：本轮恐惧是否保留双倍伤害（戴戒????false）??*/
		final boolean doubleDamageAllowed;
		/** 本轮恐惧的双倍伤害是否已消耗（整轮只触发一次）?*/
		boolean doubleDamageUsed;
		FearState(long endTick, UUID casterUuid, boolean doubleDamageAllowed) {
			this.endTick = endTick;
			this.casterUuid = casterUuid;
			this.doubleDamageAllowed = doubleDamageAllowed;
		}
	}

	// ===== 非玩家目标：仇恨压制（用户定稿??=====

	/**
	 * 恐惧中的非玩家生物是否允许将 target 设为攻击目标（用户定稿：只对释放玩家不可视）?
	 * <p>机制：恐惧期??mob <b>仅对释放该恐惧的梦魇本??/b>失去仇恨（??setTarget）??
	 * 被梦魔攻击后仅??{@link #MOB_AGGRO_WINDOW_TICKS}?? 秒）反击窗口?
	 * 窗口过后（lastAttackedTime 超时）仇恨消失（mobTick 清目标）??
	 * 对其它实体（其它玩家、其它梦魇、生物）一切正常，不受恐惧影响??/p>
	 */
	public static boolean isMobAggroAllowed(Mob mob, LivingEntity target) {
		long now = mob.level().getGameTime();
		FearState v = FEARING.get(mob.getUUID());
		if (v == null || v.endTick <= now) return true; // 非恐?mob 不受?
		// 仅当目标正是释放该恐惧的梦魇本人时才压制仇恨
		if (!(target instanceof ServerPlayer tp) || !tp.getUUID().equals(v.casterUuid)) return true;
		long lastHit = mob.getLastHurtByMobTimestamp(); // vanilla：受击成功时更新
		return now - lastHit <= BAL.i("mob_aggro_window_ticks", MOB_AGGRO_WINDOW_TICKS); // 仅被?2 秒内可反??
	}

	/** 恐惧中的 mob 每刻清除对「施法梦魇」的过期仇恨（由 MobEntityMixin.mobTick 调用，仅服务端）??*/
	public static void tickFearedMobAggro(Mob mob) {
		long now = mob.level().getGameTime();
		FearState v = FEARING.get(mob.getUUID());
		if (v == null || v.endTick <= now) return;
		LivingEntity cur = mob.getTarget();
		if (cur == null) return;
		if (!isMobAggroAllowed(mob, cur)) {
			mob.setTarget(null); // 仇恨消失（放行：setTarget(null) 不受拦截??
		}
	}

	/**
	 * 双倍伤害受益者扩展（用户定稿）：恐惧目标受「任何食梦??b>及其白名单成?/b>」的首次伤??????
	 * 白名单成??= 在线恐惧梦魔的白名单友军（WhitelistUtils.isBuffTarget 强化类目标判定）?
	 */
	public static boolean isDoubleDamageBeneficiary(LivingEntity attacker, ServerLevel world) {
		if (!(attacker instanceof ServerPlayer sp)) return false;
		if (NightmareDreamManager.isNightmare(sp)) return true;
		// 非梦魔玩家：是任一在线食梦魔的白名单友???受??
		for (ServerPlayer p : world.players()) {
			if (!NightmareDreamManager.isNightmare(p)) continue;
			if (github.com.gengyoubo.sscfa.util.WhitelistUtils.isBuffTarget(p, sp)) {
				return true;
			}
		}
		return false;
	}

	/** 目标断线清理?*/
	public static void onTargetDisconnect(UUID targetUuid) {
		FEARING.remove(targetUuid);
		DREAM_IMMUNE.remove(targetUuid);
		clearPairs(targetUuid);
	}

	/** 当前恐惧目标数（调试用）??*/
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
