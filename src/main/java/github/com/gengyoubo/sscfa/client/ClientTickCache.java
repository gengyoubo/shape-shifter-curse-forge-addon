package github.com.gengyoubo.sscfa.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

/**
 * 客户端每 tick 数据缓存：寒棘狐法阵进度与潮汐球拴人倒计时每 tick 扫描一次，
 * 避免 HUD 每帧做实体盒扫描。本地玩家为空或切换世界/维度时清空。
 */
public final class ClientTickCache {
    private ClientTickCache() {}

    private static long cachedTick = Long.MIN_VALUE;
    private static Object cachedLevel;
    private static float frostForgeProgress = -1f;
    private static double tidalTetherFraction = -1.0;
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.TickEvent.ClientTickEvent event) -> {
                    if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) refresh(Minecraft.getInstance());
                });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> invalidate());
    }

    private static void refresh(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            invalidate();
            return;
        }
        if (cachedLevel != mc.level) invalidate();
        long now = mc.level.getGameTime();
        if (now == cachedTick) return;
        cachedTick = now;
        cachedLevel = mc.level;
        var player = mc.player;

        frostForgeProgress = -1f;
        var arrays = mc.level.getEntitiesOfClass(github.com.gengyoubo.sscfa.entity.FrostArrayEntity.class,
                player.getBoundingBox().inflate(4.0), a -> a.getTrackedOwnerId() == player.getId());
        if (!arrays.isEmpty()) {
            frostForgeProgress = Math.max(0f, Math.min(1f, arrays.get(0).getProgress() / 100f));
        }

        tidalTetherFraction = -1.0;
        if (github.com.gengyoubo.sscfa.util.FormUtils.isAnyForm(player,
                github.com.gengyoubo.sscfa.util.FormIdentifiers.AXOLOTL_FLUORESCENT,
                github.com.gengyoubo.sscfa.util.FormIdentifiers.AXOLOTL_ALING)) {
            java.util.UUID owner = player.getUUID();
            var orbs = mc.level.getEntitiesOfClass(github.com.gengyoubo.sscfa.entity.TidalOrbEntity.class,
                    player.getBoundingBox().inflate(64.0),
                    orb -> orb.isTetherActive() && orb.getTetherRemain() > 0
                            && owner.equals(orb.getTrackedOwnerUuid().orElse(null)));
            if (!orbs.isEmpty()) {
                var orb = orbs.get(0);
                int total = Math.max(1, orb.getTetherTotal());
                tidalTetherFraction = Math.min(1.0, orb.getTetherRemain() / (double) total);
            }
        }
    }

    private static void invalidate() {
        cachedTick = Long.MIN_VALUE;
        cachedLevel = null;
        frostForgeProgress = -1f;
        tidalTetherFraction = -1.0;
    }

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

    /** 寒棘狐法阵进度 0..1；无法阵时为 -1。 */
    public static float frostForgeProgress() { return frostForgeProgress; }

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

    /** 潮汐球拴人剩余占比 0..1；无拴人球时为 -1。 */
    public static double tidalTetherFraction() { return tidalTetherFraction; }
}
