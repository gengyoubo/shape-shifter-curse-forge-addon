package github.com.gengyoubo.sscfa.item;

import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

import java.util.UUID;

/** 侵蚀沙棱镜（Forge 简化）：保留能力层使用的佩戴判定。 */
public class ErosionSandPrismItem extends AccessoryItem {
    public ErosionSandPrismItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
        return AddonAccessoryGuard.canEquip(entity, github.com.gengyoubo.sscfa.util.FormUtils::isGoldenSandstormSP);
    }

    public static boolean isEquippedBy(ServerPlayer player) {
        return TrinketUtils.isWearingAuto(player, github.com.gengyoubo.sscfa.init.AddonItems.EROSION_SAND_PRISM.get());
    }

    public static void clearPlayer(UUID uuid) {}

    public static void registerLootTable() {}
}
