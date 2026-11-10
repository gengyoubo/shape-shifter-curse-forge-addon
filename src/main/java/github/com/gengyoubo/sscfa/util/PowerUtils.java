package github.com.gengyoubo.sscfa.util;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.power.PowerJson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/**
 * Apoli 资源型能力（{@code apoli:resource}，含 {@code apoli:multiple} 的子资源）的读写，落到 SSC Forge 的资源存储：
 * 写入按 JSON 的 min/max 夹取，服务端改动自动同步给本人客户端，并随玩家数据保存；客户端读同步来的值。
 */
public final class PowerUtils {
	private PowerUtils() {}

	/** 当前值（双端可读；客户端为服务端同步的值）。 */
	public static int getResourceValue(Player player, ResourceLocation resourceId) {
		return (int) Math.round(FormActivePowerService.resource(player, resourceId));
	}

	/** 客户端读取（HUD 等）；有倒计时预测时优先使用预测值。 */
	public static int getClientResourceValue(Player player, ResourceLocation resourceId) {
		Integer predicted = github.com.gengyoubo.sscfa.network.CountdownPrediction.predicted(player, resourceId);
		return predicted != null ? predicted : getResourceValue(player, resourceId);
	}

	/** 客户端读取当前值与上限：{@code {current, max}}，上限缺失时为 1。 */
	public static int[] getClientResourceValueAndMax(Player player, ResourceLocation resourceId) {
		int max = getResourceMax(resourceId);
		return new int[]{getClientResourceValue(player, resourceId), max > 0 ? max : 1};
	}

	/** 设值（按 JSON 上下限夹取）并同步给本人。 */
	public static void setResourceValue(ServerPlayer player, ResourceLocation resourceId, int value) {
		JsonObject modifier = new JsonObject();
		modifier.addProperty("operation", "set_total");
		modifier.addProperty("value", value);
		FormActivePowerService.modifyResource(player, resourceId, modifier);
	}

	public static void setResourceValueAndSync(ServerPlayer player, ResourceLocation resourceId, int value) {
		setResourceValue(player, resourceId, value);
	}

	/** 每 tick 递减的倒计时资源；登记到倒计时批量同步以减少每 tick 小包。 */
	public static void countDownAndSync(ServerPlayer player, ResourceLocation resourceId, int value) {
		setResourceValue(player, resourceId, value);
		if (player != null && !player.level().isClientSide()) {
			github.com.gengyoubo.sscfa.network.CountdownSync.mark(player, resourceId);
		}
	}

	/** 增减（按 JSON 上下限夹取）并同步。 */
	public static void changeResourceValue(ServerPlayer player, ResourceLocation resourceId, int change) {
		if (change == 0) return;
		setResourceValue(player, resourceId, getResourceValue(player, resourceId) + change);
	}

	public static void changeResourceValueAndSync(ServerPlayer player, ResourceLocation resourceId, int change) {
		changeResourceValue(player, resourceId, change);
	}

	/** 资源上限（JSON 的 max）；没有该资源或未写 max 返回 0。 */
	public static int getResourceMax(ResourceLocation resourceId) {
		JsonObject definition = PowerJson.definition(resourceId);
		return definition != null && definition.has("max") ? definition.get("max").getAsInt() : 0;
	}

	/** 资源下限（JSON 的 min）；未写时按 0。 */
	public static int getResourceMin(ResourceLocation resourceId) {
		JsonObject definition = PowerJson.definition(resourceId);
		return definition != null && definition.has("min") ? definition.get("min").getAsInt() : 0;
	}

	public static int getResourceMax(ServerPlayer player, ResourceLocation resourceId) {
		return getResourceMax(resourceId);
	}

	public static boolean hasResource(ServerPlayer player, ResourceLocation resourceId, int required) {
		return getResourceValue(player, resourceId) >= required;
	}

	/** 是否处于 SP 悦灵形态。 */
	public static boolean isSpAllay(ServerPlayer player) {
		return FormUtils.isAllaySP(player);
	}

	/** 让某个冷却型能力（active_self 等）重新从满冷却开始。 */
	public static void resetCooldown(ServerPlayer player, ResourceLocation cooldownPowerId) {
		FormActivePowerService.triggerCooldown(player, cooldownPowerId);
	}
}
