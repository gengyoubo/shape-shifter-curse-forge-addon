package github.com.gengyoubo.sscfa.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

import java.util.UUID;

/** 凋零沙环（Forge 简化）：保留能力层使用的佩戴判定。 */
public class WitheredSandRingItem extends Item {
    public WitheredSandRingItem(Properties properties) {
        super(properties);
    }

    public static boolean isEquippedBy(ServerPlayer player) {
        return TrinketUtils.isWearingAuto(player, github.com.gengyoubo.sscfa.init.AddonItems.WITHERED_SAND_RING.get());
    }

    public static void clearPlayer(UUID uuid) {}

    public static void registerLootTable() {}
}
