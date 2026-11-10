package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 进化美西螈「投掷水矛」蓄力期的客户端渲染状态：正在蓄力的玩家 UUID 集合。 */
public final class UpgradeAxolotlSpearRenderState {
    private static final Set<UUID> CHARGING = ConcurrentHashMap.newKeySet();

    private UpgradeAxolotlSpearRenderState() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_SPEAR_CHARGE_STATE, buf -> {
            UUID id = buf.readUUID();
            set(id, buf.readBoolean());
        });
        MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> clear());
    }

    public static void set(UUID id, boolean charging) {
        if (id == null) return;
        if (charging) {
            CHARGING.add(id);
        } else {
            CHARGING.remove(id);
        }
    }

    public static boolean isCharging(UUID id) {
        return id != null && CHARGING.contains(id);
    }

    public static void clear() {
        CHARGING.clear();
    }
}
