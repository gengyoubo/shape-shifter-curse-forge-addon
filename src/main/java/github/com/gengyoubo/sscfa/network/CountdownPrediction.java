package github.com.gengyoubo.sscfa.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** 公共层的客户端预测桥：客户端初始化时注入 Provider，服务端保持为空。 */
public final class CountdownPrediction {
    @FunctionalInterface
    public interface Provider {
        Integer predicted(Player player, ResourceLocation resourceId);
    }

    private static volatile Provider provider;

    private CountdownPrediction() {
    }

    public static void setProvider(Provider value) {
        provider = value;
    }

    public static Integer predicted(Player player, ResourceLocation resourceId) {
        Provider current = provider;
        return current == null ? null : current.predicted(player, resourceId);
    }
}
