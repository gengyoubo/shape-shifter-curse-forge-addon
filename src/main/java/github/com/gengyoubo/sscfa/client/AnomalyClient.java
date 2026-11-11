package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.anomaly.AnomalyEffects;
import github.com.gengyoubo.sscfa.anomaly.AnomalyNetwork;
import github.com.gengyoubo.sscfa.anomaly.AnomalySnapshot;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 世界徽章/目标 HUD 的只读异常镜像。 */
public final class AnomalyClient {
    private static final class View {
        final int entityId;
        final AnomalySnapshot snapshot;
        final long received;
        final long expiresAt;
        final Map<String, Double> accumulatingValues;
        final Map<String, Double> fullValues;
        long projectedTick = Long.MIN_VALUE;
        AnomalySnapshot projected;

        View(int entityId, AnomalySnapshot snapshot, long received, long expiresAt) {
            this.entityId = entityId;
            this.snapshot = snapshot;
            this.received = received;
            this.expiresAt = expiresAt;
            if (snapshot.fullElements().isEmpty()) {
                accumulatingValues = snapshot.values();
                fullValues = Map.of();
            } else {
                var accumulating = new HashMap<String, Double>();
                var full = new HashMap<String, Double>();
                snapshot.values().forEach((element, amount) -> {
                    (snapshot.fullElements().contains(element) ? full : accumulating).put(element, amount);
                });
                accumulatingValues = Map.copyOf(accumulating);
                fullValues = Map.copyOf(full);
            }
        }

        int entityId() { return entityId; }

        AnomalySnapshot snapshot() { return snapshot; }

        long received() { return received; }

        long expiresAt() { return expiresAt; }

        Map<String, Double> values(long elapsed) {
            boolean accumulating = elapsed < snapshot.remainingTicks();
            boolean full = elapsed < snapshot.fullRemainingTicks();
            return accumulating && full ? snapshot.values()
                    : accumulating ? accumulatingValues
                    : full ? fullValues : Map.of();
        }
    }

    private static final Map<UUID, View> STATES = new HashMap<>();
    private static final List<LivingEntity> TARGETS = new ArrayList<>();
    private static final List<LivingEntity> READ_ONLY_TARGETS = Collections.unmodifiableList(TARGETS);
    private static long targetsTick = Long.MIN_VALUE;
    private static ClientLevel world;
    private static long ticks;
    private static boolean initialized;

    private AnomalyClient() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        AddonNetwork.registerClientReceiver(AnomalyNetwork.STATE, buf -> {
            AnomalyNetwork.Message message = AnomalyNetwork.read(buf);
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || !client.level.dimension().location().equals(message.dimension())) return;
            ensureWorld(client.level);
            if (message.snapshot().empty()) {
                STATES.remove(message.entityUuid());
            } else {
                int duration = Math.max(message.snapshot().remainingTicks(),
                        Math.max(message.snapshot().linkRemaining(), message.snapshot().fullRemainingTicks()));
                for (AnomalyEffects.EffectView effect : message.snapshot().effects()) {
                    duration = Math.max(duration, effect.ticks());
                }
                STATES.put(message.entityUuid(), new View(message.entityId(), message.snapshot(),
                        ticks, ticks + duration));
            }
            invalidateTargets();
        });
        AddonNetwork.registerClientReceiver(AnomalyNetwork.CONFIG, buf -> {
            String json = buf.readUtf(AnomalyNetwork.MAX_CONFIG_CHARS);
            github.com.gengyoubo.sscfa.anomaly.AnomalyRules.acceptClientSnapshot(json);
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            clear();
            github.com.gengyoubo.sscfa.anomaly.AnomalyRules.clearClientSnapshot();
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ClientLevel level && level == world) clear();
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
    }

    private static void tick(Minecraft client) {
        ensureWorld(client.level);
        if (client.isPaused()) return;
        ticks++;
        if (STATES.isEmpty() || client.level == null || ticks % 20 != 0) return;
        var iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            View view = entry.getValue();
            var entity = client.level.getEntity(view.entityId());
            if (entity != null && !entity.getUUID().equals(entry.getKey())) {
                iterator.remove();
                continue;
            }
            if (ticks >= view.expiresAt() || entity != null && !entity.isAlive()) iterator.remove();
        }
    }

    public static AnomalySnapshot snapshot(LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        if (!entity.isAlive() || entity.isRemoved() || client.level != world || entity.level() != world) {
            return null;
        }
        View view = STATES.get(entity.getUUID());
        if (view == null || view.entityId() != entity.getId()) return null;
        if (ticks >= view.expiresAt()) return null;
        if (view.projectedTick == ticks) return view.projected;
        int elapsed = (int) Math.min(Integer.MAX_VALUE, ticks - view.received());
        var state = view.snapshot();
        int remaining = Math.max(0, state.remainingTicks() - elapsed);
        int link = Math.max(0, state.linkRemaining() - elapsed);
        int full = Math.max(0, state.fullRemainingTicks() - elapsed);
        var effects = state.effects().stream().filter(effect -> effect.ticks() > elapsed)
                .map(effect -> new AnomalyEffects.EffectView(effect.id(), effect.ticks() - elapsed,
                        effect.strength())).toList();
        view.projected = new AnomalySnapshot(state.maximum(), view.values(elapsed), remaining,
                link == 0 ? "" : state.lastElement(), link, effects, full,
                full == 0 ? java.util.Set.of() : state.fullElements());
        view.projectedTick = ticks;
        return view.projected;
    }

    public static int trackedCount() {
        return STATES.size();
    }

    /** Resolve only synchronized accumulating targets, once per tick; includes NPCs and creatures. */
    public static List<LivingEntity> accumulationTargets() {
        if (world == null || Minecraft.getInstance().level != world) return List.of();
        if (targetsTick != ticks) {
            TARGETS.clear();
            for (var entry : STATES.entrySet()) {
                View view = entry.getValue();
                if (view.values(ticks - view.received()).isEmpty()) continue;
                var entity = world.getEntity(view.entityId());
                if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                        && living.getUUID().equals(entry.getKey())) {
                    TARGETS.add(living);
                }
            }
            targetsTick = ticks;
        }
        return READ_ONLY_TARGETS;
    }

    private static void invalidateTargets() {
        TARGETS.clear();
        targetsTick = Long.MIN_VALUE;
    }

    /** Allocation-free view for world badges; time/effects projection stays in snapshot(). */
    public static Map<String, Double> accumulation(LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        if (!entity.isAlive() || entity.isRemoved() || client.level != world || entity.level() != world) {
            return Map.of();
        }
        View view = STATES.get(entity.getUUID());
        if (view == null || view.entityId() != entity.getId()) return Map.of();
        return view.values(ticks - view.received());
    }

    private static void ensureWorld(ClientLevel current) {
        if (world != current) {
            clear();
            world = current;
        }
    }

    private static void clear() {
        STATES.clear();
        invalidateTargets();
        world = null;
        ticks = 0;
    }
}
