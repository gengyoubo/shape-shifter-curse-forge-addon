package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;

import java.util.function.BiPredicate;

/**
 * 附属在能力 JSON 里用的自定义动作与条件，按 JSON 的 {@code "type"} 注册到 SSC Forge。
 * 双实体条件里 actor 是能力持有者，target 是另一方（与 Fabric 版 Apoli 一致）。
 */
public final class AddonActionsAndConditions {
	/** 食梦魔「入梦」：actor 被 target 入梦时返回 true（效果不施加）。由食梦魔能力注册；未注册时谁都不会被阻挡。 */
	private static volatile BiPredicate<LivingEntity, LivingEntity> dreamBlocked = (actor, target) -> false;

	private AddonActionsAndConditions() {}

	public static void setDreamBlocked(BiPredicate<LivingEntity, LivingEntity> predicate) {
		dreamBlocked = predicate;
	}

	public static void init() {
		// 每 tick 把倒计时资源减 1（到 min 为止），用于 HUD 冷却条等
		SscJavaRegistries.registerAction(id("my_addon", "tick_countdown"), (actor, target, json) -> {
			if (!(target instanceof ServerPlayer player) || !json.has("resource")) return;
			ResourceLocation resource = ResourceLocation.tryParse(json.get("resource").getAsString());
			if (resource == null) return;
			int current = PowerUtils.getResourceValue(player, resource);
			if (current > minOf(resource)) PowerUtils.countDownAndSync(player, resource, current - 1);
		});

		// apply_effect 等无法带来源的 JSON 动作用：actor 被 target 入梦时不施加
		SscJavaRegistries.registerCondition(id("my_addon", "not_dream_blocked"), (actor, target, json) -> {
			if (!(target instanceof LivingEntity living) || actor.level().isClientSide()) return true;
			return !dreamBlocked.test(actor, living);
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
