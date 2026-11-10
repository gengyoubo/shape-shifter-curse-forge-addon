package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Forge 版：食梦魔惊吓客户端幽灵显形表。 */
public final class NightmareSpookClient {

    private static final Map<UUID, Long> GHOSTS = new ConcurrentHashMap<>();

    private NightmareSpookClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_SPOOK_GHOST, buf -> {
            UUID ghostUuid = buf.readUUID();
            int life = buf.readVarInt();
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            GHOSTS.put(ghostUuid, client.level.getGameTime() + life);
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> GHOSTS.clear());
    }

    public static boolean isGhostVisible(UUID entityUuid) {
        Long until = GHOSTS.get(entityUuid);
        if (until == null) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        if (client.level.getGameTime() >= until) {
            GHOSTS.remove(entityUuid);
            return false;
        }
        return true;
    }
}
