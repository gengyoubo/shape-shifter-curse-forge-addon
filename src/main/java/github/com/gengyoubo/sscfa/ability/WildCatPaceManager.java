package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

/**
 * 野猫系「夜行者」昼夜移速被动（代码实现，替代原 wild_cat_sp_speed_passive power JSON）。
 *
 * <p>夜间（13000~23000）移速 +20%（速度 I 等效）；白昼不再减速（原 -15% 昼间惩罚已移除）。
 * 用<b>持久属性修饰符</b>而非周期施加的药水效果实现：原 apoli action_over_time 每 20t
 * 重新施加缓慢/速度药水，效果到期→属性移除→再施加的循环会让 FOV（视角随移速缩放）
 * 周期性来回弹动；持久修饰符只在昼夜/形态/禁用状态<b>变化时</b>切换一次，FOV 全程稳定。</p>
 *
 * <p>技能禁用联动：沿用 SkillBlocker 的 "wild_cat:night_speed" / "wild_cat:day_slow"
 * 标签与服务器配置，禁用对应技能即摘除修饰符（与原 JSON 的 ssc_addon:skill_disabled 条件等价）。
 * 作用于野猫 SP（{@code wild_cat_sp}）与食梦魔（{@code wild_cat_nightmare}）两形态。</p>
 */
public final class WildCatPaceManager {
	/** 固定 UUID：状态切换时先移除再按新值添加，防重复堆积。 */
	private static final UUID PACE_MODIFIER_UUID = UUID.fromString("5f6a2e8c-1b3d-4c7e-9a0f-8e2d1c4b6a3c");
	private static final String MODIFIER_NAME = "Wild Cat Night Walker";
	// ==== 以下常量为默认值；运行时从 balance 快照读取（scope: abilities.wild_cat_pace，数据包可覆盖）====
	/** 夜间速度 I 等效（vanilla speed amplifier 0 = +20% 移速）；白昼不再减速，昼间惩罚已移除。 */
	/** 夜间速度 I 等效（vanilla speed amplifier 0 = +20% 移速）。默认；运行时从 balance 快照读取。 */
	private static final double NIGHT_SPEED = 0.20;
	/** 检查粒度（tick）：与原 JSON interval 20 一致，昼夜切换响应延迟最多 1 秒。默认；运行时从 balance 快照读取。 */
	private static final int CHECK_INTERVAL = 20;

	/** 服务端权威 balance 快照读取（快照未初始化时回退上方默认常量）。 */
	private static final BalanceReader BAL = new BalanceReader("abilities.wild_cat_pace");

	private WildCatPaceManager() {
	}

	/** 每服务端 tick 对每个在线玩家调用（内部按 check_interval 降频）。 */
	public static void tick(ServerPlayer player) {
		if (player.level().getGameTime() % BAL.i("check_interval", CHECK_INTERVAL) != 0) return;

		Double target = targetValue(player);
		AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr == null) return;

		AttributeModifier current = attr.getModifier(PACE_MODIFIER_UUID);
		if (target == null) {
			// 非野猫形态 / 技能禁用：摘除后返回
			if (current != null) attr.removeModifier(PACE_MODIFIER_UUID);
			return;
		}
		// 状态未变不动修饰符 —— 每 20t 无脑 remove+add 会复现 FOV 周期弹动，必须跳过
		if (current != null && Math.abs(current.getAmount() - target) < 1.0e-9) return;
		if (current != null) attr.removeModifier(PACE_MODIFIER_UUID);
		attr.addTransientModifier(new AttributeModifier(
				PACE_MODIFIER_UUID, MODIFIER_NAME, target, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/**
	 * 计算当前应挂的修饰符值；null = 不挂。
	 * <p>昼夜判定与原 JSON 的 apoli:time_of_day 完全对齐：夜间 [13000, 23000]（含边界），
	 * 其余为白天。技能禁用各自独立判定。</p>
	 */
	private static Double targetValue(ServerPlayer player) {
		boolean isWildCat = FormUtils.isForm(player, FormIdentifiers.WILD_CAT_SP)
				|| FormUtils.isForm(player, FormIdentifiers.WILD_CAT_NIGHTMARE);
		if (!isWildCat) return null;

		long timeOfDay = player.level().getDayTime() % 24000L;
		boolean night = timeOfDay >= 13000L && timeOfDay <= 23000L;
		if (night) {
			return SkillBlocker.isSkillBlocked(player, "wild_cat", "night_speed") ? null : BAL.d("night_speed", NIGHT_SPEED);
		}
		// 白昼：不再减速，返回 null（修饰符保持摘除）。day_slow 标签保留兼容旧存档，仅不再生效。
		return null;
	}

	/** 断线兜底清理（temporary modifier 不进 NBT，重连由 tick 重新挂上）。 */
	public static void clearPlayer(ServerPlayer player) {
		AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr != null) attr.removeModifier(PACE_MODIFIER_UUID);
	}
}
