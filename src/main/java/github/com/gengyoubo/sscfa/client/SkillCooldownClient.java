package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.cooldown.SkillCooldownPacket;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldownSync;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;

public final class SkillCooldownClient {
    public record State(int phase, int remaining, int total, long anchor) {
        public int remainingNow(long now) {
            return (int) Math.max(0, remaining - Math.max(0, now - anchor));
        }
    }

    private static Map<String, State> states = Map.of();

    public static State get(String skill) {
        return states.get(github.com.gengyoubo.sscfa.cooldown.SkillCastManager.domain(skill));
    }

    public static long now() {
        return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> states = Map.of());
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> states = Map.of());
        AddonNetwork.registerClientReceiver(SkillCooldownSync.ID, buf -> {
            Map<String, State> received = new HashMap<>();
            for (var view : SkillCooldownPacket.read(buf)) {
                received.put(view.skill(), new State(view.phase(), view.remaining(), view.total(), 0));
            }
            long time = now();
            Map<String, State> anchored = new HashMap<>();
            received.forEach((id, s) -> anchored.put(id, new State(s.phase(), s.remaining(), s.total(), time)));
            states = Map.copyOf(anchored);
        });
    }
}
