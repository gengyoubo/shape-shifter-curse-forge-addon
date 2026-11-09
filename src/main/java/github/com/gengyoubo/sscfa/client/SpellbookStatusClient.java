package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SpellbookStatusSync;
import github.com.gengyoubo.sscfa.spell.SpellbookStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

/** 魔法书状态客户端镜像（Forge 版）。 */
public final class SpellbookStatusClient {
    private static ClientLevel world;
    private static int entityId;
    private static long receivedAt, serverTime;
    private static SpellbookStatus status;

    private SpellbookStatusClient() {}

    public static void init() {
        AddonNetwork.registerClientReceiver(SpellbookStatusSync.STATE, buf -> {
            var incoming = SpellbookStatusSync.Snapshot.read(buf);
            Minecraft client = Minecraft.getInstance();
            if (client.getConnection() == null || client.level == null || client.player == null
                    || client.player.getId() != incoming.entityId()
                    || !client.level.dimension().location().equals(incoming.dimension())) return;
            world = client.level;
            entityId = incoming.entityId();
            receivedAt = world.getGameTime();
            serverTime = incoming.time();
            status = incoming.status();
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> clear());
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
    }

    public static SpellbookStatus get() {
        Minecraft client = Minecraft.getInstance();
        if (world == null || client.level != world || client.player == null || client.player.getId() != entityId
                || world.getGameTime() < receivedAt || world.getGameTime() - receivedAt > 150) {
            clear();
        }
        return status;
    }

    public static long now() {
        return world == null ? 0 : Math.max(serverTime, world.getGameTime());
    }

    private static void clear() { world = null; status = null; receivedAt = serverTime = 0; entityId = 0; }
}
