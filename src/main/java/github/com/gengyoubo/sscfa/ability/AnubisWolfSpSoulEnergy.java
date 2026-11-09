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
 * SP阿努比斯之狼 - 灵魂能量系??
 * 通过击杀/战斗积累灵魂能量（最?00点），满能量时下一次死亡领域施放将被增强：
 * - 半??24??2
 * - 充能时间 2秒????
 * - 持续时间 15秒??0??
 * - 閸戝娴侷I 閺囧じ鍞?閸戝娴侷
 * - 自动召??只冥?
 * 能量满时消耗全部能量施放增强领域?
 */
public class AnubisWolfSpSoulEnergy {

	/**
	 * 最大灵魂能量（默认值；运行时从 balance 快照读取，scope：abilities.anubis_soul_energy??
	 */
	public static final int MAX_ENERGY = 100;

// ==================== 常??====================
	// 以下常量均为默认值；运行时??balance 快照读取（scope：abilities.anubis_soul_energy??
	/**
	 * 在死亡领域内击杀获得的能??
	 */
	private static final int KILL_IN_DOMAIN_ENERGY = 20;
	/**
	 * 冥狼击杀获得的能??
	 */
	private static final int MINION_KILL_ENERGY = 10;
	/**
	 * 普通击杀获得的能?
	 */
	private static final int REGULAR_KILL_ENERGY = 5;
	/**
	 * 自身处于凋零时击杀额外获得的能量（凋零收割循环?
	 */
	private static final int WITHER_KILL_BONUS_ENERGY = 10;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.anubis_soul_energy");

	private AnubisWolfSpSoulEnergy() {
	}

// ==================== 公开接口 ====================

	/**
	 * 获取玩家当前灵魂能??
	 */
	public static int getEnergy(ServerPlayer player) {
		return ResourceBars.get(player, BarKeys.ANUBIS_SOUL);
	}

	/**
	 * 增加灵魂能量（自动限制上限）?
	 * <p>修复：统一框??maxOf ??power 查询异常路径下可能返??0，导?gain ??clamp ??0（能量加不上?
	 * 表现为召唤??凋零击杀失效）。此处改为直接??VariableIntPower 的真??max??
	 * 查不到时回退常??{@link #MAX_ENERGY}（与旧版行为一致），保证任何时序下都能累积??
	 */
	public static void addEnergy(ServerPlayer player, int amount) {
		int max = PowerUtils.getResourceMax(player, FormIdentifiers.ANUBIS_WOLF_SP_SOUL_ENERGY);
		if (max <= 0) {
			max = BAL.i("max_energy", MAX_ENERGY); // power 未就?查询失败的回退上限（旧版行为??
		}
		int current = getEnergy(player);
		int newValue = Math.min(current + amount, max);
		if (newValue != current) {
			ResourceBars.set(player, BarKeys.ANUBIS_SOUL, newValue);
		}
	}

	/**
	 * 检查灵魂能量是否已?
	 */
	public static boolean isFullEnergy(ServerPlayer player) {
		return getEnergy(player) >= ResourceBars.maxOf(player, BarKeys.ANUBIS_SOUL);
	}

	/**
	 * 消耗全部灵魂能量（施放增强领域时调用??
	 */
	public static void consumeEnergy(ServerPlayer player) {
		ResourceBars.set(player, BarKeys.ANUBIS_SOUL, 0);
	}

	/**
	 * 清除玩家能量（断??变形时）
	 */
	public static void clearPlayer(ServerPlayer player) {
		if (player != null) {
			ResourceBars.set(player, BarKeys.ANUBIS_SOUL, 0);
		}
	}

	/**
	 * 设置灵魂能量（由set_mana命令调用??
	 */
	public static void setEnergy(ServerPlayer player, int amount) {
		ResourceBars.set(player, BarKeys.ANUBIS_SOUL, amount);
	}

	// ==================== 框架接入：满能触发音效（BarTrigger，替代原内联判断??====================

	/**
	 * 凋零击杀回能：受害者被阿努比斯玩家（或其冥狼）施加凋零后死于凋?DOT 时，给施加者回能?
	 * <p>原版凋零掉血?DamageSource ??attacker（查不到击杀者），故沿用金沙岚的「施加时注册来源」方案：
	 * 施加凋零时记?(受害???施加???过期时刻)，死亡时若查不到直接击杀者且身上有表内凋零，回溯给最近施加者??
	 */
	private static final java.util.Map<UUID, java.util.Map<UUID, Long>> WITHER_SOURCES = new java.util.concurrent.ConcurrentHashMap<>();
	/** 凋零来源过期宽限（tick）：效果到期后精简存活时间，避免残留误归因??*/
	private static final long WITHER_SOURCE_GRACE = 40L;

	/** 注册凋零来源（由 SscAddonLivingEntityMixin 在阿努比斯玩家施加凋零时调用）??*/
	public static void registerWitherSource(LivingEntity victim, ServerPlayer player, int witherDurationTicks) {
		if (victim == null || player == null) return;
		long expire = victim.level().getGameTime() + (long) witherDurationTicks + WITHER_SOURCE_GRACE;
		WITHER_SOURCES.computeIfAbsent(victim.getUUID(), k -> new java.util.concurrent.ConcurrentHashMap<>())
				.put(player.getUUID(), expire);
	}

	/** 死亡回溯：查直接击杀者无果时，若身上有未过期凋零来源，给最近的施加者回能并清理??*/
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
		// 只回溯身上仍常驻凋零效果的（防过宽??
		if (!victim.hasEffect(MobEffects.WITHER)) return;
		Player ownerEntity = victim.level().getPlayerByUUID(best);
		if (ownerEntity instanceof ServerPlayer ownerPlayer
				&& FormUtils.isForm(ownerPlayer, FormIdentifiers.ANUBIS_WOLF_SP)) {
			addEnergy(ownerPlayer, BAL.i("wither_kill_bonus_energy", WITHER_KILL_BONUS_ENERGY));
		}
		WITHER_SOURCES.remove(victim.getUUID());
	}

	/** 满能提示音：跨入满值时播（与原 addEnergy 内联逻辑等价）?*/
	private static final github.com.gengyoubo.sscfa.resource.BarTrigger ANUBIS_FULL_TRIGGER =
			(player, oldV, newV, max) -> {
				if (oldV < max && newV >= max) {
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.2f);
				}
			};

	static {
		// 挂到统一框架（可插拔：移除本行即去掉音效，不影响其它逻辑?
		BarKeys.ANUBIS_SOUL.addTrigger(ANUBIS_FULL_TRIGGER);
	}

// ==================== 事件注册 ====================

	/**
	 * 注册击杀事件监听器（在SscAddon.onInitialize中调用）
	 */
	public static void registerEvents() {
		github.com.gengyoubo.sscfa.util.SscEvents.onLivingDeath((victim, source) -> {
            try { onEntityDeath(victim, source); }
            finally { WITHER_SOURCES.remove(victim.getUUID()); }
        });
	}

	/**
	 * 实体死亡时的处理逻??
	 */
	private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
		if (entity.level().isClientSide()) return;

// 情??：凋?DOT 击杀（无 attacker 归因）——按施加来源回溯（含玩家/冥狼施加的凋零）
		if (damageSource.getEntity() == null
				&& damageSource.is(net.minecraft.world.damagesource.DamageTypes.WITHER)) {
			onWitherDeath(entity);
			return;
		}

// 情??：玩家直接击杀
		if (damageSource.getEntity() instanceof ServerPlayer killer) {
			if (!FormUtils.isForm(killer, FormIdentifiers.ANUBIS_WOLF_SP)) return;

// 检查击杀是否发生在死亡领域内
			if (AnubisWolfSpDeathDomain.hasActiveDomain(killer.getUUID())
					&& AnubisWolfSpDeathDomain.isInActiveDomain(killer.getUUID(), entity.blockPosition())) {
// 增强领域范围内击杀不获取能??
				if (!AnubisWolfSpDeathDomain.isEnhancedDomain(killer.getUUID())) {
					addEnergy(killer, BAL.i("kill_in_domain_energy", KILL_IN_DOMAIN_ENERGY));
				}
			} else {
				addEnergy(killer, BAL.i("regular_kill_energy", REGULAR_KILL_ENERGY));
			}
// 凋零击杀回能：自身处于凋零时击杀额??+10 灵魂能量
			if (killer.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
				addEnergy(killer, BAL.i("wither_kill_bonus_energy", WITHER_KILL_BONUS_ENERGY));
			}
			return;
		}

// 情??：冥狼击杀（AnubisWolfMinionEntity的owner获得能量??
		if (damageSource.getEntity() instanceof AnubisWolfMinionEntity wolf) {
			java.util.UUID ownerUuid = wolf.getOwnerUUID();
			if (ownerUuid == null) return;

			Player ownerEntity = entity.level().getPlayerByUUID(ownerUuid);
			if (ownerEntity instanceof ServerPlayer ownerPlayer) {
				if (!FormUtils.isForm(ownerPlayer, FormIdentifiers.ANUBIS_WOLF_SP)) return;
// 增强领域范围内冥狼击杀不获取能?
				if (AnubisWolfSpDeathDomain.isEnhancedDomain(ownerUuid)
						&& AnubisWolfSpDeathDomain.isInActiveDomain(ownerUuid, entity.blockPosition())) {
					return;
				}
				addEnergy(ownerPlayer, BAL.i("minion_kill_energy", MINION_KILL_ENERGY));
// 凋零击杀回能：冥狼击杀时，若主人处于凋零，额??+10 灵魂能量
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
