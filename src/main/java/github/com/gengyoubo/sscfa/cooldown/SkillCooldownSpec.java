package github.com.gengyoubo.sscfa.cooldown;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 所有技能 power 共用的冷却字段（数据包作者只需认这一套）：
 * cooldown（成功 CD）/ fail_cooldown（失败 CD，0=失败不进 CD）/ cooldown_start（on_cast|on_release|on_end）/
 * extra_cooldowns（该技能特有的附加 CD，如 {"cursed_moon": 280}）。单位均为 tick。
 */
public record SkillCooldownSpec(int cooldown, int failCooldown, String cooldownStart, Map<String, Integer> extras) {
	private static final Set<String> STARTS = Set.of(
			SkillCastManager.START_ON_CAST, SkillCastManager.START_ON_RELEASE, SkillCastManager.START_ON_END);

	private record Defaults(int cooldown, String start) {}

	/** 带统一冷却字段的 power 类型，以及字段缺省时的默认值。 */
	private static final Map<String, Defaults> HOLDER_TYPES = Map.of(
			"my_addon:fail_aware_active_self", new Defaults(1, SkillCastManager.START_ON_CAST),
			"ssc_addon:familiar_skill", new Defaults(1, SkillCastManager.START_ON_CAST),
			"my_addon:sonic_wave", new Defaults(160, SkillCastManager.START_ON_CAST),
			"my_addon:mist_form", new Defaults(200, SkillCastManager.START_ON_END),
			"my_addon:parasitic_fruit_seed", new Defaults(200, SkillCastManager.START_ON_CAST),
			"my_addon:parasitic_spore_bomb", new Defaults(400, SkillCastManager.START_ON_CAST),
			"my_addon:true_invisibility", new Defaults(240, SkillCastManager.START_ON_END));

	/** 是否带统一冷却字段的 power 类型。 */
	public static boolean isHolderType(String type) {
		return HOLDER_TYPES.containsKey(type);
	}

	/**
	 * 从 power JSON 读取冷却配置；不是带冷却字段的类型返回 null。
	 *
	 * @throws JsonSyntaxException 字段非法（负数、非整数、未知起算模式）
	 */
	@Nullable
	public static SkillCooldownSpec parse(JsonObject power) {
		JsonElement type = power.get("type");
		Defaults defaults = type == null ? null : HOLDER_TYPES.get(type.getAsString());
		if (defaults == null) return null;
		int cooldown = power.has("cooldown") ? strictTicks(power.get("cooldown")) : defaults.cooldown();
		int fail = power.has("fail_cooldown") ? strictTicks(power.get("fail_cooldown")) : 0;
		String start = defaults.start();
		if (power.has("cooldown_start")) {
			JsonElement value = power.get("cooldown_start");
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || !STARTS.contains(value.getAsString()))
				throw new JsonSyntaxException("Invalid cooldown_start: " + value);
			start = value.getAsString();
		}
		Map<String, Integer> extras = power.has("extra_cooldowns") ? parseExtras(power.get("extra_cooldowns")) : Map.of();
		return new SkillCooldownSpec(cooldown, fail, start, Map.copyOf(extras));
	}

	static int strictTicks(JsonElement value) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
			throw new JsonSyntaxException("Cooldown must be an integer number");
		try {
			int ticks = value.getAsBigDecimal().intValueExact();
			if (ticks < 0) throw new ArithmeticException("negative");
			return ticks;
		} catch (ArithmeticException ex) {
			throw new JsonSyntaxException("Cooldown must be an integer in 0..2147483647", ex);
		}
	}

	private static Map<String, Integer> parseExtras(JsonElement json) {
		if (!json.isJsonObject()) throw new JsonSyntaxException("extra_cooldowns must be an object");
		Map<String, Integer> map = new HashMap<>();
		for (var entry : json.getAsJsonObject().entrySet()) map.put(entry.getKey(), strictTicks(entry.getValue()));
		return map;
	}

	/** 附加 CD；数据包删掉该键时回退到正常 cooldown。 */
	public int extra(String key) {
		return extras.getOrDefault(key, cooldown);
	}

	public SkillCastManager.ResolvedConfig resolve() {
		return new SkillCastManager.ResolvedConfig(cooldown, failCooldown, cooldownStart);
	}
}
