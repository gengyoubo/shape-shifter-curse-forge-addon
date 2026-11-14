package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

/** 毒腺（Forge 简化）：保留能力层使用的静态参数与佩戴判定。 */
public class VenomGlandItem extends AccessoryItem {
    public static final float DURATION_SCALE = 0.7f;

    public VenomGlandItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
        return AddonAccessoryGuard.canEquip(entity, e -> github.com.gengyoubo.sscfa.util.FormUtils.isForm(e, github.com.gengyoubo.sscfa.util.FormIdentifiers.SPIDER_SALTICIDAE));
    }

    public static boolean isWearingBy(LivingEntity entity) {
        return TrinketUtils.isWearingAuto(entity, github.com.gengyoubo.sscfa.init.AddonItems.VENOM_GLAND.get());
    }
}
