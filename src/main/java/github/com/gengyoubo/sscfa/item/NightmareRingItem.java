package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

/** 梦魇戒指（Forge 简化）：保留能力层使用的静态参数与佩戴判定。 */
public class NightmareRingItem extends AccessoryItem {
    public static final float FEAR_DURATION_BONUS = 0.35f;

    public NightmareRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
        return AddonAccessoryGuard.canEquip(entity, e -> github.com.gengyoubo.sscfa.util.FormUtils.isForm(e, github.com.gengyoubo.sscfa.util.FormIdentifiers.WILD_CAT_NIGHTMARE));
    }

    public static boolean isWearingBy(LivingEntity entity) {
        return TrinketUtils.isWearingAuto(entity, github.com.gengyoubo.sscfa.init.AddonItems.NIGHTMARE_RING.get());
    }
}
