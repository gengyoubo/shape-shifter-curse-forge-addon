package github.com.gengyoubo.sscfa.client.evolution;

import github.com.gengyoubo.sscfa.evolution.EvolutionRegistry;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;

import java.util.LinkedHashMap;
import java.util.Map;

/** 接收服务端同步的进化路线 JSON，供进化树/转职界面在多人服使用。 */
public final class EvolutionClientSync {
    private EvolutionClientSync() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_EVO_ROUTES_SYNC, buf -> {
            int count = Math.max(0, Math.min(1024, buf.readInt()));
            Map<String, String> raw = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                raw.put(buf.readUtf(256), buf.readUtf(2000000));
            }
            EvolutionRegistry.INSTANCE.applyClientSync(raw);
        });
    }
}
