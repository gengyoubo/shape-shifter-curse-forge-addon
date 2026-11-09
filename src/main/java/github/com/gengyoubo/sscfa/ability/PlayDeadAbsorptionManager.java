package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 装死回血给予的「黄心」存?+ 衰减管理?
 * 规则（用?2026-06-23 确认）：
 * - 仅作用于「装死给的黄心」这部分预算，其它来源（如金苹果）的黄心不动?
 * - 计时从「装死结束」那一刻起算，存留 30 秒?
 * - 30 秒后开始衰减：每秒?1 颗心? 2 点吸?HP），直接?setAbsorptionAmount 扣，无受击特效?
 */
public class PlayDeadAbsorptionManager {

	/** 对外暴露?scope 读取器（SscAddonActions 装死 CD 共用，避免双份维护）?*/
	public static github.com.gengyoubo.sscfa.balance.BalanceReader balanceReader() {
		return EARLY_END_BAL;
	}

	private static final github.com.gengyoubo.sscfa.balance.BalanceReader EARLY_END_BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.playing_dead");
	// 「装死黄心」预算（只有这部分会?30s 后衰减）
	private static final Map<UUID, Float> BUDGET = new ConcurrentHashMap<>();
	// 衰减开始的服务?tick? 装死结束那刻 + 30s?
	private static final Map<UUID, Long> DECAY_START = new ConcurrentHashMap<>();

	private static final int RETAIN_TICKS = 600;      // 30 秒存?
	private static final int DECAY_INTERVAL = 20;     // 每秒衰减一?
	private static final float DECAY_PER_SEC = 2.0f;  // 每次?1 颗心 = 2 HP

	// 阶段 5：运行时快照读取（abilities.playing_dead；快照未初始化回退默认常量?
	private static final BalanceReader BAL = new BalanceReader("abilities.playing_dead");

	private PlayDeadAbsorptionManager() {
	}

    private static final String SAVE_KEY = "ssca_play_dead_absorption";

    /** Preserve the owned budget across reconnects without keeping disconnected UUIDs in memory. */
    public static void writePlayerData(ServerPlayer player, net.minecraft.nbt.CompoundTag nbt) {
        float budget = Math.min(BUDGET.getOrDefault(player.getUUID(), 0f), player.getAbsorptionAmount());
        if (!Float.isFinite(budget) || budget <= 0) { nbt.remove(SAVE_KEY); return; }
        var data = new net.minecraft.nbt.CompoundTag();
        data.putFloat("Budget", budget);
        Long start = DECAY_START.get(player.getUUID());
        if (start != null) {
            long now = player.getServer().getTickCount();
            int interval = Math.max(1, BAL.i("decay_interval", DECAY_INTERVAL));
            long remaining = start > now ? start - now : Math.floorMod(start - now, interval);
            data.putLong("Remaining", remaining);
        }
        nbt.put(SAVE_KEY, data);
    }

    public static void readPlayerData(ServerPlayer player, net.minecraft.nbt.CompoundTag nbt) {
        forgetPlayer(player.getUUID());
        if (!nbt.contains(SAVE_KEY, 10)) return;
        var data = nbt.getCompound(SAVE_KEY);
        float budget = Math.min(data.getFloat("Budget"), player.getAbsorptionAmount());
        if (!Float.isFinite(budget) || budget <= 0) return;
        BUDGET.put(player.getUUID(), budget);
        if (data.contains("Remaining")) {
            long remaining = Math.max(0, Math.min(BAL.i("retain_ticks", RETAIN_TICKS), data.getLong("Remaining")));
            DECAY_START.put(player.getUUID(), player.getServer().getTickCount() + remaining);
        }
    }

    public static void forgetPlayer(UUID uuid) { BUDGET.remove(uuid); DECAY_START.remove(uuid); }
    public static void clearAll() { BUDGET.clear(); DECAY_START.clear(); }

	/** 装死期间每次给黄心时调用，累加「装死黄心」预算；并把衰减计时清零（结束后再重新计 30s）?*/
	public static void addAbsorption(Player player, float delta) {
		if (delta <= 0f) {
			return;
		}
		UUID id = player.getUUID();
		BUDGET.merge(id, delta, Float::sum);
		DECAY_START.remove(id);
	}

	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		Float budgetObj = BUDGET.get(id);
		if (budgetObj == null) {
			return;
		}

		// 预算不能超过实际黄心（被打掉/死亡会减少实际黄心）
		float actual = player.getAbsorptionAmount();
		float budget = Math.min(budgetObj, actual);
		if (budget <= 0f) {
			BUDGET.remove(id);
			DECAY_START.remove(id);
			return;
		}

		// 仍在装死 ?还在累积，不衰减；计时起点留到结束后再设
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PLAYING_DEAD.get())) {
			BUDGET.put(id, budget);
			DECAY_START.remove(id);
			return;
		}

		// 装死已结??确保衰减起点已设（结束那?+ 30s?
		long now = player.getServer().getTickCount();
		long decayStart = DECAY_START.computeIfAbsent(id, k -> now + BAL.i("retain_ticks", RETAIN_TICKS));

		// 到点后每秒减 2 HP（代码直接扣，无受击特效?
		if (now >= decayStart && (now - decayStart) % BAL.i("decay_interval", DECAY_INTERVAL) == 0) {
			float reduce = Math.min(budget, (float) BAL.d("decay_per_sec", DECAY_PER_SEC));
			github.com.gengyoubo.sscfa.spell.FrostArmorState.removeOther(player, reduce);
			budget -= reduce;
		}

		if (budget <= 0f) {
			BUDGET.remove(id);
			DECAY_START.remove(id);
		} else {
			BUDGET.put(id, budget);
		}
	}
}
