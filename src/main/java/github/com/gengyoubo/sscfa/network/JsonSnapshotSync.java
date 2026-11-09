package github.com.gengyoubo.sscfa.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;

/** Main-thread content snapshots. Reliable play packets establish the baseline per connection. */
public final class JsonSnapshotSync {
    private record Key(UUID player, ResourceLocation channel) {}
    private record Payload(Map<String, String> content, byte[] bytes) {}
    private record Sent(Object player, Object connection, Payload payload) {}
    private static final Map<ResourceLocation, Payload> ENCODED = new HashMap<>();
    private static final Map<Key, Sent> SENT = new HashMap<>();
    private JsonSnapshotSync() {}

    public static void register() {
        PlayerSessionCleanup.register((connection, server) -> {
            SENT.entrySet().removeIf(entry -> entry.getValue().connection == connection);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> { SENT.clear(); ENCODED.clear(); });
    }

    public static void send(ServerPlayer player, ResourceLocation channel, Map<String, String> content, boolean force) {
        if (!AddonNetwork.canSend(player, channel)) return;
        Payload payload = ENCODED.get(channel);
        if (payload == null || !payload.content.equals(content)) {
            FriendlyByteBuf buf = AddonNetwork.buffer();
            buf.writeInt(content.size());
            content.forEach((id, json) -> { buf.writeUtf(id, 256); buf.writeUtf(json, 2000000); });
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            payload = new Payload(Map.copyOf(content), bytes);
            ENCODED.put(channel, payload);
            buf.release();
        }
        Key key = new Key(player.getUUID(), channel);
        Sent previous = SENT.get(key);
        if (!force && previous != null && previous.player == player && previous.connection == player.connection
                && previous.payload == payload) return;
        FriendlyByteBuf out = AddonNetwork.buffer();
        out.writeBytes(payload.bytes);
        AddonNetwork.sendToPlayer(player, channel, out);
        SENT.put(key, new Sent(player, player.connection, payload));
    }
}
