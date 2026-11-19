package github.com.gengyoubo.sscfa.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

/**
 * 客户端独占的冷却查询，供救命猫尾与幻影铃的提示使用。单独成类，避免通用物品类在专用服务端
 * 链接 {@link Minecraft}；调用方须先以 {@code FMLEnvironment.dist == CLIENT} 守卫。
 * 冷却由服务端物品冷却同步到本地玩家；无玩家（未进入世界或已退出）时视为未冷却。
 */
public final class LifesavingCatTailClient {
    private LifesavingCatTailClient() {}

    /** 本地玩家该物品的剩余冷却 tick；无玩家或未冷却时为 0。 */
    public static int getCooldownRemaining(Object item, int max) {
        if (!(item instanceof Item cooldownItem)) return 0;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return 0;
        // 1.0 = 刚开始冷却，0.0 = 冷却完成
        float progress = client.player.getCooldowns().getCooldownPercent(cooldownItem, 0.0f);
        return Math.max(0, Math.round(progress * max));
    }

    public static boolean isOnCooldown(Object item) {
        if (!(item instanceof Item cooldownItem)) return false;
        Minecraft client = Minecraft.getInstance();
        return client != null && client.player != null && client.player.getCooldowns().isOnCooldown(cooldownItem);
    }
}
