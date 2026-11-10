package github.com.gengyoubo.sscfa.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

/** 客户端每 tick 数据缓存（Forge 简化）：当前只保留佩戴魔法书查询，其余缓存待对应系统移植。 */
public final class ClientTickCache {
    private ClientTickCache() {}

    public static void register() {}

    public static ItemStack equippedBook() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ItemStack book = TrinketUtils.findFirstEquipped(mc.player,
                stack -> stack.getItem() == github.com.gengyoubo.sscfa.init.AddonItems.MOON_DUST_SPELLBOOK.get());
        return book == null || book.isEmpty() ? null : book;
    }

    public static boolean isWearing(Item item) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && TrinketUtils.isWearing(mc.player, item);
    }

    public static float frostForgeProgress() { return -1f; }

    public static boolean hasHoverThorn() { return hoverThornCount() > 0; }

    public static int hoverThornCount() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;
        java.util.UUID owner = mc.player.getUUID();
        var scan = mc.player.getBoundingBox().inflate(8.0);
        int count = 0;
        for (var thorn : mc.level.getEntitiesOfClass(
                github.com.gengyoubo.sscfa.entity.FrostThornEntity.class, scan,
                e -> e.isHover() && owner.equals(e.getOwnerUuid().orElse(null)))) {
            count++;
        }
        return count;
    }

    public static double tidalTetherFraction() { return -1.0; }
}
