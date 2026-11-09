package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.cooldown.SkillCastManager;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldownPacket;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldownSync;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;

/** 客户端的技能冷却镜像（HUD 用）；收到服务端快照时按本地世界时间锚定，之后本地推算剩余时间。 */
public final class SkillCooldownClient {
    public record State(int phase, int remaining, int total, long anchor) {
        public int remainingNow(long now) {
            return (int) Math.max(0, remaining - Math.max(0, now - anchor));
        }
    }

    private static volatile Map<String, State> states = Map.of();

    private SkillCooldownClient() {}

    public static State get(String skill) {
        return states.get(SkillCastManager.domain(skill));
    }

    public static long now() {
        return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
    }

    /** 客户端初始化时调用一次。 */
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> states = Map.of());
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> states = Map.of());
        AddonNetwork.registerClientReceiver(SkillCooldownSync.ID, buf -> {
            long time = now();
            Map<String, State> anchored = new HashMap<>();
            for (var view : SkillCooldownPacket.read(buf)) {
                anchored.put(view.skill(), new State(view.phase(), view.remaining(), view.total(), time));
            }
            states = Map.copyOf(anchored);
        });
    }
}
