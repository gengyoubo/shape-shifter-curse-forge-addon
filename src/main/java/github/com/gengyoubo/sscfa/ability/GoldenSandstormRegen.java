package github.com.gengyoubo.sscfa.ability;

import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 金沙岚SP - 自定义回血系??
 *
 * 金沙岚禁止通过饱食度自然回血（??GoldenSandstormHungerMixin 拦截）?
 * 仅以下方式可恢复生命值：
 *
 * 战斗内（5秒内有攻击或受击?
 *   - 自身附加的凋零每次对敌造成伤害 ??+1 HP（半颗心?
 *   - 自身造成击??                 ??+4 HP??颗心?
 *   - 冥狼（AnubisWolfMinionEntity）造成击杀 ??+3 HP??.5颗心?
 *
 * 被动回血:
 *   - 战斗? ??00 tick(10?? +1 HP（半颗心?
 *   - 战斗? ??20 tick(6??  +1 HP（半颗心?
 *
 * 备??
 *   - 半颗?= 1.0 HP, 1颗??= 2.0 HP
 *   - 战斗状?= 5??100 tick)内有主动攻击或受到伤??
 */
public final class GoldenSandstormRegen {

	// 以下带「默认」注释的常量已登??balance（scope：abilities.golden_sandstorm_regen），运行时从快照读取?
// COMBAT_DURATION / WITHER_SOURCE_GRACE 未登记，保持编译期常量?
	/** 凋零每次伤害回血量（HP??= 半颗心。默认；运行时??balance 快照读取 */
	private static final float WITHER_TICK_HEAL = 1.0f;
	/** 玩家击杀回血量（HP??= 2颗心。默认；运行时??balance 快照读取 */
	private static final float KILL_HEAL = 4.0f;
	/** 冥狼击杀回血量（HP??= 1.5颗心。默认；运行时??balance 快照读取 */
	private static final float MINION_KILL_HEAL = 3.0f;
	/** 被动回血量（HP??= 半颗心。默认；运行时??balance 快照读取 */
	private static final float PASSIVE_HEAL = 1.0f;
	/** 战斗外被动回血间隔（tick??= 10秒。默认；运行时从 balance 快照读取 */
	private static final int PASSIVE_INTERVAL_OOC = 200;
	/** 战斗内被动回血间隔（tick??= 6秒。默认；运行时从 balance 快照读取 */
	private static final int PASSIVE_INTERVAL_IC = 120;
	/** 战斗状态持续时间（tick??= 8??*/
	private static final long COMBAT_DURATION = 160L;
	/** 凋零源记录的额外保留时间（tick），覆盖凋??tick 边??*/
	private static final long WITHER_SOURCE_GRACE = 40L;

	/** 玩家UUID -> 上次进入战斗的世界时?tick) */
	private static final Map<UUID, Long> LAST_COMBAT_TICK = new ConcurrentHashMap<>();
	/** 玩家UUID -> 上次被动回血的世界时?tick) */
	private static final Map<UUID, Long> LAST_PASSIVE_TICK = new ConcurrentHashMap<>();
	/** 受害者UUID -> { 金沙岚玩家UUID -> 凋零源记录到期世界时?tick) } */
	private static final Map<UUID, Map<UUID, Long>> WITHER_SOURCES = new ConcurrentHashMap<>();

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.golden_sandstorm_regen");

	private GoldenSandstormRegen() {
	}

	// ==================== 战斗状?====================

	/** 标记玩家进入战斗状态（仅对金沙岚生效，其他形态会?tick 自然忽略??*/
	public static void markCombat(ServerPlayer player) {
		if (player == null) return;
		LAST_COMBAT_TICK.put(player.getUUID(), player.level().getGameTime());
	}

	/** 检查玩家是否处于战斗状?*/
	public static boolean isInCombat(ServerPlayer player) {
		// 同步果蝠交战判定（造??受到伤害 10s 内、或来源仍??10 格内）；
		// 同时保留金沙岚自身??markCombat 标记（覆盖冥狼为主人标记、凋零源等果蝠事件不涵盖的特殊来源）?
		if (github.com.gengyoubo.sscfa.ability.ParasiticCombatTracker.isInCombat(player)) {
			return true;
		}
		Long last = LAST_COMBAT_TICK.get(player.getUUID());
		if (last == null) return false;
		return player.level().getGameTime() - last <= COMBAT_DURATION;
	}

	// ==================== 凋零来源追踪 ====================

	/** 注册凋零来源（金沙岚玩家给受害者施加凋零时调用??*/
	public static void registerWitherSource(LivingEntity victim, ServerPlayer player, int witherDurationTicks) {
		if (victim == null || player == null) return;
		long expire = victim.level().getGameTime() + (long) witherDurationTicks + WITHER_SOURCE_GRACE;
		WITHER_SOURCES.computeIfAbsent(victim.getUUID(), k -> new ConcurrentHashMap<>())
				.put(player.getUUID(), expire);
	}

	/** 凋??tick 对受害者造成伤害时调用：为所有注册的金沙岚玩家回??*/
	public static void onWitherTickDamage(LivingEntity victim) {
		if (victim == null) return;
		Map<UUID, Long> sources = WITHER_SOURCES.get(victim.getUUID());
		if (sources == null || sources.isEmpty()) return;
		if (!(victim.level() instanceof ServerLevel serverWorld)) return;
		long now = serverWorld.getGameTime();
		// 清理过期记??
		sources.entrySet().removeIf(e -> e.getValue() < now);
		if (sources.isEmpty()) {
			WITHER_SOURCES.remove(victim.getUUID());
			return;
		}
		for (UUID playerUuid : sources.keySet()) {
			Player p = serverWorld.getPlayerByUUID(playerUuid);
			if (!(p instanceof ServerPlayer sp)) continue;
			if (!FormUtils.isForm(sp, FormIdentifiers.GOLDEN_SANDSTORM_SP)) continue;
			sp.heal((float) BAL.d("wither_tick_heal", WITHER_TICK_HEAL));
			markCombat(sp);
		}
        if (!victim.isAlive()) WITHER_SOURCES.remove(victim.getUUID());
	}

	// ==================== 被动回血 tick ====================

    /** Use the configured tick interval exactly; only players without a timer use the cheap form probe. */
    public static void tick(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;
        UUID owner = player.getUUID();
        if (player.tickCount % 20 != 0 && !LAST_PASSIVE_TICK.containsKey(owner)) return;
        if (!FormUtils.isForm(player, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
            LAST_PASSIVE_TICK.remove(owner);
            return;
        }
		// 满血时仅刷新计时器，避免一进战斗就立即结算累积时间
		long now = player.level().getGameTime();
		if (player.getHealth() >= player.getMaxHealth()) {
			LAST_PASSIVE_TICK.put(player.getUUID(), now);
			return;
		}
		Long last = LAST_PASSIVE_TICK.get(player.getUUID());
		if (last == null) {
			LAST_PASSIVE_TICK.put(player.getUUID(), now);
			return;
		}
		int interval = isInCombat(player) ? BAL.i("passive_interval_ic", PASSIVE_INTERVAL_IC) : BAL.i("passive_interval_ooc", PASSIVE_INTERVAL_OOC);
		if (now - last >= interval) {
			player.heal((float) BAL.d("passive_heal", PASSIVE_HEAL));
			LAST_PASSIVE_TICK.put(player.getUUID(), now);
		}
	}

	// ==================== 击杀监听 ====================

	/** 注册击杀事件监听器（?SscAddon.onInitialize 中调用） */
	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.onLivingDeath(GoldenSandstormRegen::onEntityDeath);
	}

	private static void onEntityDeath(LivingEntity entity, DamageSource source) {
		if (entity == null || entity.level().isClientSide()) return;
		// 1. 玩家直接击??
		if (source.getEntity() instanceof ServerPlayer killer
				&& FormUtils.isForm(killer, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
			killer.heal((float) BAL.d("kill_heal", KILL_HEAL));
			markCombat(killer);
		}
		// 2. 閸愩儳瀚鹃崙缁樻絻
		else if (source.getEntity() instanceof AnubisWolfMinionEntity wolf
				&& entity.level() instanceof ServerLevel serverWorld) {
			UUID ownerUuid = wolf.getOwnerUUID();
			if (ownerUuid != null) {
				Player owner = serverWorld.getPlayerByUUID(ownerUuid);
				if (owner instanceof ServerPlayer ownerPlayer
						&& FormUtils.isForm(ownerPlayer, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
					ownerPlayer.heal((float) BAL.d("minion_kill_heal", MINION_KILL_HEAL));
					markCombat(ownerPlayer);
				}
			}
		}
        // Successful lethal wither is credited by damage RETURN, after AFTER_DEATH has run.
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)) WITHER_SOURCES.remove(entity.getUUID());
	}

	// ==================== 状态清?====================

	/** 清空所有状态（服务器启?/ 数据包重载??*/
	public static void clearAll() {
		LAST_COMBAT_TICK.clear();
		LAST_PASSIVE_TICK.clear();
		WITHER_SOURCES.clear();
	}

	/** 清理单个玩家状态（断??/ 切换形态） */
	public static void clearPlayer(UUID playerUuid) {
		if (playerUuid == null) return;
		LAST_COMBAT_TICK.remove(playerUuid);
		LAST_PASSIVE_TICK.remove(playerUuid);
		WITHER_SOURCES.values().forEach(m -> m.remove(playerUuid));
	}
    public static void pruneSources(long now) {
        WITHER_SOURCES.values().removeIf(sources -> {
            sources.values().removeIf(expiry -> expiry < now);
            return sources.isEmpty();
        });
    }
}
