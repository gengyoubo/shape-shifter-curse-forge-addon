package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月织蛛减速网「踩网蓝色高亮」等客户端专属描边表：服务端只发给施法者本人，
 * 由 {@code EntityWebGlowMixin} 依据 entityId 在本机描边，实现「仅施法者可见」。
 */
public final class WebHighlightClient {
    private static final Map<Integer, long[]> HIGHLIGHT = new ConcurrentHashMap<>();
    private static final int DEFAULT_COLOR = 0x3AA0FF;

    private WebHighlightClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_WEB_HIGHLIGHT, buf -> {
            int entityId = buf.readVarInt();
            int duration = buf.readVarInt();
            int color = buf.readInt();
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            HIGHLIGHT.put(entityId, new long[]{client.level.getGameTime() + duration, color});
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) {
                HIGHLIGHT.clear();
                return;
            }
            if (HIGHLIGHT.isEmpty()) return;
            long now = client.level.getGameTime();
            HIGHLIGHT.entrySet().removeIf(entry -> entry.getValue()[0] <= now);
        });
    }

    public static boolean isHighlighted(int entityId) {
        return HIGHLIGHT.containsKey(entityId);
    }

    public static int getHighlightColor(int entityId) {
        long[] value = HIGHLIGHT.get(entityId);
        return value != null ? (int) value[1] : DEFAULT_COLOR;
    }

    /** 客户端每 tick 续约的短租约描边（不经过网络）。 */
    public static void highlightLocal(int entityId, int color) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        HIGHLIGHT.put(entityId, new long[]{client.level.getGameTime() + 5, color});
    }
}
