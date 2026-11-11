package github.com.gengyoubo.sscfa.anomaly;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 异常快照的变更驱动同步；倒计时由客户端按服务端时间锚插值。 */
public final class AnomalyNetwork {
    public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "anomaly_snapshot");
    public static final ResourceLocation CONFIG = new ResourceLocation("ssc_addon", "anomaly_rules");
    public static final int MAX_CONFIG_CHARS = 262144;

    private static final AnomalySnapshot EMPTY =
            new AnomalySnapshot(AnomalyLedger.MAXIMUM, java.util.Map.of(), 0, "", 0, List.of());
    private static boolean initialized;

    public record Message(ResourceLocation dimension, int entityId, UUID entityUuid, AnomalySnapshot snapshot) {}

    private AnomalyNetwork() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) configuration(player);
        });
        AnomalyRules.onReload(() -> {
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) configuration(player);
            });
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getTarget() instanceof LivingEntity living
                    && (AnomalyManager.state(living) != null || AnomalyEffects.hasState(living))
                    && event.getEntity() instanceof ServerPlayer player) {
                send(living, player, AnomalyManager.snapshot(living));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StopTracking event) -> {
            if (event.getTarget() instanceof LivingEntity living
                    && (AnomalyManager.state(living) != null || AnomalyEffects.hasState(living))
                    && event.getEntity() instanceof ServerPlayer player) {
                send(living, player, EMPTY);
            }
        });
    }

    public static void sync(LivingEntity target) {
        broadcast(target, null);
    }

    public static void clear(LivingEntity target) {
        broadcast(target, EMPTY);
    }

    private static void broadcast(LivingEntity target, AnomalySnapshot supplied) {
        if (!(target.level() instanceof ServerLevel world)) return;
        AnomalySnapshot snapshot = supplied == null ? AnomalyManager.snapshot(target) : supplied;
        for (ServerPlayer viewer : world.players()) {
            if (viewer != target && viewer.distanceToSqr(target) > 128.0 * 128.0) continue;
            send(target, viewer, snapshot);
        }
    }

    private static void configuration(ServerPlayer player) {
        if (!AddonNetwork.canSend(player, CONFIG)) return;
        FriendlyByteBuf buffer = AddonNetwork.buffer();
        buffer.writeUtf(AnomalyRules.snapshotJson(), MAX_CONFIG_CHARS);
        AddonNetwork.sendToPlayer(player, CONFIG, buffer);
        AnomalyManager.recordPacket();
    }

    private static void send(LivingEntity target, ServerPlayer player, AnomalySnapshot snapshot) {
        if (!AddonNetwork.canSend(player, STATE)) return;
        FriendlyByteBuf buffer = AddonNetwork.buffer();
        write(buffer, new Message(target.level().dimension().location(), target.getId(),
                target.getUUID(), snapshot));
        AddonNetwork.sendToPlayer(player, STATE, buffer);
        AnomalyManager.recordPacket();
    }

    /** The codec is also exercised without a live network connection by integration probes. */
    public static void write(FriendlyByteBuf buffer, Message message) {
        AnomalySnapshot snapshot = message.snapshot();
        buffer.writeResourceLocation(message.dimension());
        buffer.writeVarInt(message.entityId());
        buffer.writeUUID(message.entityUuid());
        buffer.writeDouble(snapshot.maximum());
        buffer.writeVarInt(snapshot.remainingTicks());
        buffer.writeVarInt(snapshot.values().size());
        snapshot.values().forEach((element, amount) -> {
            buffer.writeUtf(element, 128);
            buffer.writeDouble(amount);
        });
        buffer.writeUtf(snapshot.lastElement(), 128);
        buffer.writeVarInt(snapshot.linkRemaining());
        buffer.writeVarInt(snapshot.effects().size());
        for (AnomalyEffects.EffectView effect : snapshot.effects()) {
            buffer.writeUtf(effect.id(), 128);
            buffer.writeVarInt(effect.ticks());
            buffer.writeDouble(effect.strength());
        }
        buffer.writeVarInt(snapshot.fullRemainingTicks());
        buffer.writeVarInt(snapshot.fullElements().size());
        for (String element : snapshot.fullElements()) buffer.writeUtf(element, 128);
    }

    public static Message read(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int entityId = buffer.readVarInt();
        UUID uuid = buffer.readUUID();
        double maximum = buffer.readDouble();
        if (!Double.isFinite(maximum) || maximum <= 0 || maximum > 1_000_000) {
            throw new IllegalArgumentException("Invalid anomaly maximum");
        }
        int remaining = bounded(buffer.readVarInt(), 0, AnomalyLedger.RETENTION_TICKS);
        int count = bounded(buffer.readVarInt(), 0, 64);
        var values = new LinkedHashMap<String, Double>();
        for (int i = 0; i < count; i++) {
            String element = buffer.readUtf(128);
            double amount = buffer.readDouble();
            if (element.isBlank() || !Double.isFinite(amount) || amount < 0 || amount > maximum
                    || values.put(element, amount) != null) {
                throw new IllegalArgumentException("Invalid anomaly meter");
            }
        }
        String last = buffer.readUtf(128);
        int link = bounded(buffer.readVarInt(), 0, AnomalyLedger.LINK_TICKS);
        int effectCount = bounded(buffer.readVarInt(), 0, 64);
        var effects = new java.util.ArrayList<AnomalyEffects.EffectView>(effectCount);
        for (int i = 0; i < effectCount; i++) {
            String id = buffer.readUtf(128);
            int ticks = bounded(buffer.readVarInt(), 0, 72000);
            double strength = buffer.readDouble();
            if (id.isBlank() || !Double.isFinite(strength) || strength < 0) {
                throw new IllegalArgumentException("Invalid anomaly effect");
            }
            effects.add(new AnomalyEffects.EffectView(id, ticks, strength));
        }
        int fullRemaining = 0;
        var fullElements = new java.util.LinkedHashSet<String>();
        if (buffer.isReadable()) {
            fullRemaining = bounded(buffer.readVarInt(), 0, AnomalyLedger.FULL_TICKS);
            int fullCount = bounded(buffer.readVarInt(), 0, count);
            for (int i = 0; i < fullCount; i++) {
                String element = buffer.readUtf(128);
                if (!values.containsKey(element) || values.get(element) != maximum
                        || !fullElements.add(element)) {
                    throw new IllegalArgumentException("Invalid full anomaly meter");
                }
            }
            if ((fullRemaining == 0) != fullElements.isEmpty() || buffer.isReadable()) {
                throw new IllegalArgumentException("Invalid full anomaly duration");
            }
        }
        return new Message(dimension, entityId, uuid,
                new AnomalySnapshot(maximum, values, remaining, last, link, effects,
                        fullRemaining, fullElements));
    }

    private static int bounded(int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Invalid anomaly count or duration");
        }
        return value;
    }
}
