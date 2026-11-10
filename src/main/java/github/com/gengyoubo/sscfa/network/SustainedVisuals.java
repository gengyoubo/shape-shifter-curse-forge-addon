package github.com.gengyoubo.sscfa.network;

import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.network.VisualRecipe.Kind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 服务端授权的持续视觉租约；实际粒子表现由客户端按同一配方重放。 */
public final class SustainedVisuals {
    public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "sustained_visuals_v2");

    public record Key(UUID source, Kind kind) {}

    public record View(Key key, UUID decorationOwner, int entityId, Vec3 pos, long epoch, int duration,
                       float width, float eyeHeight, double radius, double outerRadius) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(key.source());
            buf.writeEnum(key.kind());
            buf.writeUUID(decorationOwner);
            buf.writeVarInt(entityId);
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
            buf.writeLong(epoch);
            buf.writeVarInt(duration);
            buf.writeFloat(width);
            buf.writeFloat(eyeHeight);
            buf.writeDouble(radius);
            buf.writeDouble(outerRadius);
        }

        public static View read(FriendlyByteBuf buf) {
            Key key = new Key(buf.readUUID(), buf.readEnum(Kind.class));
            return new View(key, buf.readUUID(), buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readLong(), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                    buf.readDouble(), buf.readDouble());
        }
    }

    private static final class Entry {
        Entity source;
        ServerLevel world;
        View view;
        int touched;
    }

    private record Audience(ServerPlayer player, ServerLevel world, int sent, List<View> views) {}

    private static final Map<Key, Entry> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, Audience> AUDIENCES = new HashMap<>();

    private SustainedVisuals() {
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

    public static void touch(Entity source, Kind kind, int elapsed, int duration, double radius, double outerRadius) {
        if (!(source.level() instanceof ServerLevel world)) return;
        Key key = new Key(source.getUUID(), kind);
        Entry entry = ACTIVE.computeIfAbsent(key, ignored -> new Entry());
        entry.source = source;
        entry.world = world;
        entry.touched = world.getServer().getTickCount();
        UUID owner = source instanceof github.com.gengyoubo.sscfa.entity.FrostStormEntity storm
                && storm.getDecorationOwner() != null ? storm.getDecorationOwner() : source.getUUID();
        entry.view = new View(key, owner, source.getId(), source.position(), world.getGameTime() - elapsed,
                duration, source.getBbWidth(), source.getEyeHeight(source.getPose()), radius, outerRadius);
    }

    public static void stop(Entity source, Kind kind) {
        ACTIVE.remove(new Key(source.getUUID(), kind));
    }

    private static void flush(MinecraftServer server) {
        int tick = server.getTickCount();
        ACTIVE.values().removeIf(entry -> entry.touched < tick
                || entry.source.isRemoved() || entry.source.level() != entry.world);
        if (ACTIVE.isEmpty() && AUDIENCES.isEmpty()) return;

        for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
            if (!AddonNetwork.canSend(observer, STATE)) continue;
            List<View> visible = new ArrayList<>();
            for (Entry entry : ACTIVE.values()) {
                double range = entry.view.key().kind().range
                        + Math.max(entry.view.radius(), entry.view.outerRadius()) + 4;
                boolean trackedCore = entry.view.key().kind() == Kind.SEED_FIELD
                        && observer.distanceToSqr(entry.source) <= 128.0 * 128.0;
                if (entry.world == observer.serverLevel()
                        && (observer.distanceToSqr(entry.view.pos()) <= range * range || trackedCore)) {
                    visible.add(entry.view);
                }
            }
            Audience previous = AUDIENCES.get(observer.getUUID());
            boolean newWorld = previous == null || previous.player != observer
                    || previous.world != observer.serverLevel();
            if (visible.isEmpty() && (previous == null || previous.views().isEmpty())) continue;
            boolean changed = newWorld || !sameState(previous.views(), visible);
            boolean moved = false;
            if (!newWorld && !changed) {
                for (int i = 0; i < visible.size(); i++) {
                    View view = visible.get(i);
                    Entry entry = ACTIVE.get(view.key());
                    if (entry == null) continue;
                    if (entry.source != observer
                            && view.pos().distanceToSqr(previous.views().get(i).pos()) > 1.0e-12
                            && observer.distanceToSqr(entry.source) > 128.0 * 128.0) {
                        moved = true;
                        break;
                    }
                }
            }
            if (!changed && !moved && tick - previous.sent() < 20) continue;

            FriendlyByteBuf buf = AddonNetwork.buffer();
            buf.writeResourceLocation(observer.serverLevel().dimension().location());
            buf.writeLong(observer.serverLevel().getGameTime());
            buf.writeVarInt(visible.size());
            visible.forEach(view -> view.write(buf));
            AddonNetwork.sendToPlayer(observer, STATE, buf);
            if (visible.isEmpty()) AUDIENCES.remove(observer.getUUID());
            else AUDIENCES.put(observer.getUUID(),
                    new Audience(observer, observer.serverLevel(), tick, visible));
        }
    }

    static boolean sameState(List<View> a, List<View> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            View x = a.get(i);
            View y = b.get(i);
            if (!x.key().equals(y.key()) || !x.decorationOwner().equals(y.decorationOwner())
                    || x.entityId() != y.entityId() || x.epoch() != y.epoch()
                    || x.duration() != y.duration() || x.width() != y.width()
                    || x.eyeHeight() != y.eyeHeight() || x.radius() != y.radius()
                    || x.outerRadius() != y.outerRadius()) {
                return false;
            }
        }
        return true;
    }
}
