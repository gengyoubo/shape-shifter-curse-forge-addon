package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;


/**
 * 附属在能力 JSON 里用的自定义动作与条件，按 JSON 的 {@code "type"} 注册到 SSC Forge。
 * 双实体条件里 actor 是能力持有者，target 是另一方（与 Fabric 版 Apoli 一致）。
 */
public final class AddonActionsAndConditions {
	private AddonActionsAndConditions() {}


	public static void init() {
		// 每 tick 把倒计时资源减 1（到 min 为止），用于 HUD 冷却条等
		SscJavaRegistries.registerAction(id("my_addon", "tick_countdown"), (actor, target, json) -> {
			if (!(target instanceof ServerPlayer player) || !json.has("resource")) return;
			ResourceLocation resource = ResourceLocation.tryParse(json.get("resource").getAsString());
			if (resource == null) return;
			int current = PowerUtils.getResourceValue(player, resource);
			if (current > minOf(resource)) PowerUtils.countDownAndSync(player, resource, current - 1);
		});

	}

	private static int minOf(ResourceLocation resource) {
		JsonObject definition = PowerJson.definition(resource);
		return definition != null && definition.has("min") ? definition.get("min").getAsInt() : 0;
	}

	private static ResourceLocation id(String namespace, String path) {
		return new ResourceLocation(namespace, path);
	}
}
