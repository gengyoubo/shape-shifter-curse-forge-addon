package github.com.gengyoubo.sscfa.cooldown;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import github.com.gengyoubo.sscfa.power.PowerJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能冷却统一入口（服务端）。数值一律来自玩家当前形态上该技能 power 的 JSON 字段（{@link SkillCooldownSpec}），
 * JSON 由 SSC Forge 加载；技能 ID 见 {@link PowerJson}（顶层能力为 power ID，apoli:multiple 子能力为 <父 ID>_<子键>）。
 *
 * <p>生命周期：begin（真正开始时登记）→ released（已放出、效果持续）→ ended（持续效果结束）；
 * 瞬发成功用 completed；未成功放出用 failed；死亡/变形用 cancelled 按阶段结算。
 * Instant entry points use instant(); callbacks without an active cast are no-ops.</p>
 */
public final class SkillCooldowns {

	private static final Logger LOGGER = LoggerFactory.getLogger("SSCA/SkillCooldown");
	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	private SkillCooldowns() {
	}

	/** 解析结果按 power JSON 对象缓存；数据包重载换成新对象后自然重新解析。 */
	private static final Map<JsonObject, Optional<SkillCooldownSpec>> SPECS = new WeakHashMap<>();

	/** 玩家身上该技能的冷却配置；缺失时该技能不可用（只记一次日志）。 */
	public static SkillCooldownSpec spec(ServerPlayer player, String skillId) {
		SkillCooldownSpec spec = specOf(power(player, skillId), skillId);
		if (spec != null) return spec;
		if (WARNED.add(skillId)) {
			LOGGER.warn("Skill {} has no cooldown power on player {} (expected data/<ns>/powers/<path>.json attached to the form); skill disabled",
					skillId, player.getName().getString());
		}
		return null;
	}

	public static boolean ready(ServerPlayer player, String skillId) {
		return spec(player, skillId) != null && manager(player).canBegin(player, skillId);
	}

	public static boolean isCasting(ServerPlayer player, String skillId) {
		return cast(player, skillId) != null;
	}

	/** 真正开始施放时登记；冷却中/已在施放中/缺配置返回 false。 */
	public static boolean begin(ServerPlayer player, String skillId) {
		SkillCooldownSpec spec = spec(player, skillId);
		return spec != null && manager(player).begin(player, skillId, spec.resolve()) >= 0;
	}

    /** Capture once when accepting a cast; delayed callbacks must retain this value. */
    public static long currentCastId(ServerPlayer player, String skillId) {
        var cast = cast(player, skillId);
        return cast == null ? -1 : cast.castId;
    }

    private static boolean matches(ServerPlayer player, String skillId, long castId) {
        return castId >= 0 && currentCastId(player, skillId) == castId;
    }

    public static void released(ServerPlayer player, String skillId, long castId) {
        if (matches(player, skillId, castId)) manager(player).released(player, castId);
    }
    public static void completed(ServerPlayer player, String skillId, long castId) {
        if (matches(player, skillId, castId)) manager(player).complete(castId, SkillCastManager.now(player));
    }
    public static void ended(ServerPlayer player, String skillId, long castId) {
        if (matches(player, skillId, castId)) manager(player).finish(player, castId);
    }
    public static void failed(ServerPlayer player, String skillId, long castId) {
        if (matches(player, skillId, castId)) manager(player).fail(player, castId);
    }
    public static void interruptedFull(ServerPlayer player, String skillId, long castId) {
        if (matches(player, skillId, castId)) manager(player).interruptFull(castId, SkillCastManager.now(player));
    }
    public static void cancelled(ServerPlayer player, String skillId, long castId) {
        if (matches(player, skillId, castId)) manager(player).interrupt(castId, SkillCastManager.now(player));
    }
    public static void retune(ServerPlayer player, String skillId, long castId, int ticks) {
        if (matches(player, skillId, castId)) manager(player).retune(castId, ticks);
    }

    /** Only for a newly accepted instant action, never a delayed completion callback. */
    public static void instant(ServerPlayer player, String skillId) {
        if (begin(player, skillId)) completed(player, skillId, currentCastId(player, skillId));
    }

    /** Explicit rejected-attempt penalty; never use for delayed failure notifications. */
    public static void penalizeAttempt(ServerPlayer player, String skillId) {
        if (!ready(player, skillId)) return;
        var spec = spec(player, skillId);
        if (spec != null && spec.failCooldown() > 0) force(player, skillId, spec.failCooldown());
    }

    // ID-only overloads are for synchronous actions controlling the current cast.

	public static void released(ServerPlayer player, String skillId) {
		SkillCastManager.Cast cast = cast(player, skillId);
		if (cast != null) manager(player).released(player, cast.castId);
	}

	public static void completed(ServerPlayer player, String skillId) {
		SkillCastManager.Cast cast = cast(player, skillId);
		if (cast != null) {
			manager(player).complete(cast.castId, SkillCastManager.now(player));
		}
	}

	public static void ended(ServerPlayer player, String skillId) {
		SkillCastManager.Cast cast = cast(player, skillId);
		if (cast != null) manager(player).finish(player, cast.castId);
	}

	public static void failed(ServerPlayer player, String skillId) {
		SkillCastManager.Cast cast = cast(player, skillId);
		if (cast != null) {
			manager(player).fail(player, cast.castId);
		}
	}

	public static void cancelled(ServerPlayer player, String skillId) {
		SkillCastManager.Cast cast = cast(player, skillId);
		if (cast != null) manager(player).interrupt(cast.castId, SkillCastManager.now(player));
	}

	/** 改写本次施放的正常 CD（档位/诅咒之月/单段超时等）；已起算的冷却同步改长短。 */
	public static void retune(ServerPlayer player, String skillId, int ticks) {
		SkillCastManager.Cast cast = cast(player, skillId);
		if (cast != null) manager(player).retune(cast.castId, ticks);
	}

	/** 不经施放流程直接给技能上 CD（外部联动：增强领域、提前结束等）。 */
	public static void force(ServerPlayer player, String skillId, int ticks) {
		manager(player).force(player.getUUID(), skillId, ticks, SkillCastManager.now(player));
	}

	public static void reset(ServerPlayer player, String skillId) {
		manager(player).reset(player.getUUID(), skillId);
	}

	public static int cooldown(ServerPlayer player, String skillId) {
		SkillCooldownSpec spec = spec(player, skillId);
		return spec == null ? 0 : spec.cooldown();
	}

	public static int failCooldown(ServerPlayer player, String skillId) {
		SkillCooldownSpec spec = spec(player, skillId);
		return spec == null ? 0 : spec.failCooldown();
	}

	public static int extra(ServerPlayer player, String skillId, String key) {
		SkillCooldownSpec spec = spec(player, skillId);
		return spec == null ? 0 : spec.extra(key);
	}

	public static int remaining(ServerPlayer player, String skillId) {
		SkillCastManager.CooldownState state = manager(player).cooldown(player.getUUID(), skillId);
		return state == null ? 0 : state.remaining(SkillCastManager.now(player));
	}

	/** 丢失 power 时使用施放快照结算：已释放走正常结束，未释放走失败。 */
	public static void sweep(MinecraftServer server) {
		if (server.overworld() == null) return;
		SkillCastManager manager = SkillCastManager.get(server.overworld());
		for (SkillCastManager.Cast cast : manager.activeCasts()) {
			ServerPlayer player = server.getPlayerList().getPlayer(cast.playerId);
			if (player != null && power(player, cast.skillId) == null) manager.interrupt(cast.castId, SkillCastManager.now(player));
		}
	}

	/** 玩家当前形态上的该技能 power 的 JSON（含 apoli:multiple 的子能力）；没有返回 null。 */
	private static JsonObject power(ServerPlayer player, String skillId) {
		return PowerJson.find(player, skillId);
	}

	private static synchronized SkillCooldownSpec specOf(JsonObject power, String skillId) {
		if (power == null) return null;
		return SPECS.computeIfAbsent(power, json -> {
			try {
				return Optional.ofNullable(SkillCooldownSpec.parse(json));
			} catch (JsonSyntaxException invalid) {
				LOGGER.error("Invalid cooldown fields in power {}; skill disabled", skillId, invalid);
				return Optional.empty();
			}
		}).orElse(null);
	}

	private static SkillCastManager.Cast cast(ServerPlayer player, String skillId) {
		return manager(player).control(player.getUUID(), skillId);
	}

	private static SkillCastManager manager(ServerPlayer player) {
		return SkillCastManager.get(player.serverLevel());
	}
}
