package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.cooldown.SkillCastManager;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

/**
 * 统一技能 power {@code my_addon:fail_aware_active_self}（所有形态主/副键技能共用这一种写法）。
 *
 * <pre>
 * {
 *   "type": "my_addon:fail_aware_active_self",
 *   "cooldown": 120,               // 成功 CD（tick）
 *   "fail_cooldown": 72,           // 失败 CD（0 = 失败不进 CD）
 *   "cooldown_start": "on_cast",   // on_cast / on_release / on_end
 *   "extra_cooldowns": { },        // 该技能特有的附加 CD
 *   "entity_action": { ... },      // 不写 = 按键由技能自己发包处理，本 power 只提供冷却配置
 *   "key": { "key": "key.ssc_addon.sp_primary" }
 * }
 * </pre>
 *
 * <p>defer_release=false（瞬发）：按键即登记施放，action 内调用 {@link #markFail()} 算失败，否则算成功。
 * defer_release=true：action 在真正开始时自己 begin，并负责后续结算。
 * repress_while_active=true：施放进行中再按键仍执行 action（取消/关环/二段），repress_delay 为开始后至少经过的 tick。</p>
 *
 * <p>技能 ID 就是 SSC Forge 给的 power ID：顶层能力为文件 ID，嵌在 apoli:multiple 里时为 Apoli 子能力 ID
 * {@code <父 ID>_<子键>}（SSC Forge 1.10.0.6 起）；冷却由 {@link SkillCooldowns} 按同一份 JSON 结算。</p>
 */
public class FailAwareActiveSelf implements SscPowerType {
	public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "fail_aware_active_self");

	private static final ThreadLocal<Boolean> PENDING_FAIL = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/** action 失败分支调用：本次施放按 fail_cooldown 进入冷却。 */
	public static void markFail() {
		PENDING_FAIL.set(Boolean.TRUE);
	}

	@Override
	public void onKeyPressed(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		use(player, powerId, data);
	}

	@Override
	public void whileKeyHeld(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		JsonObject key = data.getAsJsonObject("key");
		if (key != null && key.has("continuous") && key.get("continuous").getAsBoolean()) use(player, powerId, data);
	}

	/** 是否满足 power 的 condition（无 condition 视为满足）。 */
	protected static boolean conditionMet(ServerPlayer player, JsonObject data) {
		return FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"));
	}

	/** 按键触发：对应 Fabric 版 onUse。 */
	protected void use(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		if (AbilityUseGuard.blocked(player)) return;
		JsonObject action = data.getAsJsonObject("entity_action");
		if (action == null || !conditionMet(player, data)) return;
		if (!SkillCastManager.passesGates(player)) return;
		String id = powerId.toString();
		boolean deferRelease = booleanField(data, "defer_release");
		SkillCastManager.Cast active = SkillCastManager.get(player.serverLevel()).control(player.getUUID(), id);
		if (active != null) {
			if (booleanField(data, "repress_while_active")
					&& SkillCastManager.now(player) - active.beginTick >= intField(data, "repress_delay")) {
				runAction(player, id, action);
			}
			return;
		}
		if (!SkillCooldowns.ready(player, id)) return;
		if (!deferRelease && !SkillCooldowns.begin(player, id)) return;
		long castId = SkillCooldowns.currentCastId(player, id);
		if (runAction(player, id, action) || deferRelease) return;
		SkillCooldowns.completed(player, id, castId);
	}

	/** 执行 action；action 内 markFail 时按失败结算并返回 true。 */
	private static boolean runAction(ServerPlayer player, String id, JsonObject action) {
		long castId = SkillCooldowns.currentCastId(player, id);
		PENDING_FAIL.set(Boolean.FALSE);
		try {
			FormPowerRuntime.execute(player, player, action);
			if (!Boolean.TRUE.equals(PENDING_FAIL.get())) return false;
			if (castId >= 0) SkillCooldowns.failed(player, id, castId);
			else if (SkillCooldowns.isCasting(player, id)) SkillCooldowns.failed(player, id, SkillCooldowns.currentCastId(player, id));
			else SkillCooldowns.penalizeAttempt(player, id);
			return true;
		} catch (RuntimeException | Error failure) {
			// action 在 begin 之后抛异常：释放这次已接受的施放，异常照常留在服务端日志里
			long failedCast = castId >= 0 ? castId : SkillCooldowns.currentCastId(player, id);
			if (failedCast >= 0) SkillCooldowns.failed(player, id, failedCast);
			throw failure;
		} finally {
			PENDING_FAIL.remove();
		}
	}

	private static boolean booleanField(JsonObject data, String key) {
		return data.has(key) && data.get(key).getAsBoolean();
	}

	private static int intField(JsonObject data, String key) {
		return data.has(key) ? data.get(key).getAsInt() : 0;
	}
}
