package github.com.gengyoubo.sscfa.util;

import github.com.gengyoubo.sscfa.resource.ResourceBars;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** 查询客户端已同步的形态能力。 */
public final class ClientResourceCache {
    private ClientResourceCache() {}

    public static boolean has(Player player, ResourceLocation resourceId) {
        return ResourceBars.hasPowerId(player, resourceId);
    }
}
