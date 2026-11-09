package github.com.gengyoubo.sscfa.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** 客户端资源缓存占位实现：按 Forge 客户端资源值判断是否存在。 */
public final class ClientResourceCache {
    private ClientResourceCache() {}

    public static boolean has(Player player, ResourceLocation resourceId) {
        if (player == null) return false;
        return PowerUtils.getClientResourceValueAndMax(player, resourceId)[1] > 1;
    }
}
