package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蓝火环视觉 {@code ssc_addon:blue_fire_ring_visual}：纯装饰，伤害与资源结算不在这里。
 *
 * <p>沿用 Apoli action_over_time 的节奏：每 4 tick 采样一次 condition，满足期间每 tick 给场景续租
 * （{@link ParametricVisuals#touch}）。同一玩家可以有多层（{@code layer}），各层独立续租。</p>
 */
public final class BlueFireRingVisualPower implements SscPowerType {
    public static final ResourceLocation TYPE = new ResourceLocation("ssc_addon", "blue_fire_ring_visual");

    /** 一个玩家一层火环的状态。 */
    private static final class State {
        UUID eventId;
        int phase = -1;
        long epoch;
        boolean displaying;
    }

    /** 玩家 -> (power ID -> 状态)。 */
    private final Map<UUID, Map<ResourceLocation, State>> states = new ConcurrentHashMap<>();

    @Override
    public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
        State state = states.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(powerId, ignored -> new State());
        double radius = FormPowerRuntime.doubleValue(data, "radius", 6.0);
        if (state.eventId == null) {
            // 每层一个独立场景 ID，赋能层与护符层可以同时存在
            UUID actor = player.getUUID();
            state.eventId = new UUID(actor.getMostSignificantBits() ^ 0x42524c554552494eL,
                    actor.getLeastSignificantBits() ^ (0x4756495355414c00L + FormPowerRuntime.intValue(data, "layer", 0)));
        }
        if (state.phase < 0) {
            // 第一 tick 只记录相位，之后的 tick 才执行
            state.phase = Math.floorMod(player.tickCount, 4);
            state.epoch = player.level().getGameTime();
            return;
        }
        if (Math.floorMod(player.tickCount, 4) == state.phase) {
            state.displaying = FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"));
        }
        if (!state.displaying) return;
        ParametricVisuals.touch(player, state.eventId, ParametricVisuals.Kind.BLUE_FIRE_RING,
                player.getUUID(), player.position(), true, state.epoch, 0, Math.abs(radius),
                radius, FormPowerRuntime.booleanValue(data, "flame_layer", false) ? 1 : 0);
    }

    @Override
    public void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
        Map<ResourceLocation, State> layers = states.get(player.getUUID());
        if (layers == null) return;
        State state = layers.remove(powerId);
        if (layers.isEmpty()) states.remove(player.getUUID());
        if (state != null && state.eventId != null) {
            ParametricVisuals.stop(state.eventId, ParametricVisuals.Kind.BLUE_FIRE_RING);
        }
    }
}
