package github.com.gengyoubo.sscfa.network;

import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 把「每 tick 递减」的资源倒计时批量同步给附近客户端，客户端按服务端时间锚本地预测。 */
public final class CountdownSync {
    public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "countdowns_v1");

    public record Key(UUID owner, ResourceLocation resource) {}

    public record State(Key key, int entityId, CountdownValue clock) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(key.owner);
            buf.writeResourceLocation(key.resource);
            buf.writeVarInt(entityId);
            buf.writeVarInt(clock.value());
            buf.writeVarInt(clock.minimum());
            buf.writeBoolean(clock.running());
        }

        public static State read(FriendlyByteBuf buf, long time) {
            Key key = new Key(buf.readUUID(), buf.readResourceLocation());
            int entityId = buf.readVarInt();
            int value = buf.readVarInt();
            int minimum = buf.readVarInt();
            return new State(key, entityId, new CountdownValue(value, time, minimum, buf.readBoolean()));
        }
    }

    private static final class Entry {
        final ServerPlayer owner;
        final ServerLevel world;
        final ResourceLocation resource;
        int touched;

        Entry(ServerPlayer owner, ResourceLocation resource) {
            this.owner = owner;
            this.world = owner.serverLevel();
            this.resource = resource;
        }
    }

    private record Audience(ServerPlayer player, ServerLevel world, int sent, List<State> states) {}

    private static final Map<Key, Entry> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, Audience> AUDIENCES = new HashMap<>();

    private CountdownSync() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) flush(event.getServer());
        });
        PlayerSessionCleanup.register((handler, server) -> AUDIENCES.remove(handler.player.getUUID()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            ACTIVE.clear();
            AUDIENCES.clear();
        });
    }

    /** 登记一个每 tick 递减中的资源；非单调资源不要调用。 */
    public static void mark(ServerPlayer owner, ResourceLocation resourceId) {
        if (owner == null || resourceId == null) return;
        Key key = new Key(owner.getUUID(), resourceId);
        Entry entry = ACTIVE.get(key);
        if (entry == null || entry.owner != owner || entry.world != owner.serverLevel()) {
            entry = new Entry(owner, resourceId);
            ACTIVE.put(key, entry);
        }
        entry.touched = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
    }

    private static void flush(MinecraftServer server) {
        if (ACTIVE.isEmpty() && AUDIENCES.isEmpty()) return;
        int tick = server.getTickCount();
        ACTIVE.values().removeIf(entry -> entry.owner.isRemoved()
                || entry.owner.serverLevel() != entry.world);
        if (ACTIVE.isEmpty() && AUDIENCES.isEmpty()) return;

        Map<Key, State> states = new LinkedHashMap<>();
        ACTIVE.forEach((key, entry) -> {
            int value = PowerUtils.getResourceValue(entry.owner, entry.resource);
            int minimum = PowerUtils.getResourceMin(entry.resource);
            boolean running = entry.touched == tick && value > minimum;
            states.put(key, new State(key, entry.owner.getId(),
                    new CountdownValue(value, entry.world.getGameTime(), minimum, running)));
        });

        for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
            if (!AddonNetwork.canSend(observer, STATE)) continue;
            List<State> visible = new ArrayList<>();
            for (Map.Entry<Key, State> mapEntry : states.entrySet()) {
                Entry entry = ACTIVE.get(mapEntry.getKey());
                if (entry == null || entry.world != observer.serverLevel()) continue;
                if (entry.owner == observer || observer.distanceToSqr(entry.owner) <= 160.0 * 160.0) {
                    visible.add(mapEntry.getValue());
                }
            }
            Audience previous = AUDIENCES.get(observer.getUUID());
            if (visible.isEmpty() && previous == null) continue;
            boolean changed = previous == null || previous.player != observer
                    || previous.world != observer.serverLevel()
                    || !sameState(previous.states, visible);
            if (!changed && tick - previous.sent < 20) continue;

            FriendlyByteBuf buf = AddonNetwork.buffer();
            buf.writeResourceLocation(observer.serverLevel().dimension().location());
            buf.writeLong(observer.serverLevel().getGameTime());
            buf.writeVarInt(visible.size());
            visible.forEach(state -> state.write(buf));
            AddonNetwork.sendToPlayer(observer, STATE, buf);
            if (visible.isEmpty()) AUDIENCES.remove(observer.getUUID());
            else AUDIENCES.put(observer.getUUID(), new Audience(observer, observer.serverLevel(), tick, visible));
        }

        ACTIVE.entrySet().removeIf(entry -> !states.get(entry.getKey()).clock().running());
    }

    static boolean sameState(List<State> previous, List<State> current) {
        if (previous.size() != current.size()) return false;
        for (int i = 0; i < previous.size(); i++) {
            State a = previous.get(i);
            State b = current.get(i);
            if (!a.key().equals(b.key()) || a.entityId() != b.entityId()
                    || !a.clock().agreesWith(b.clock())) {
                return false;
            }
        }
        return true;
    }
}
