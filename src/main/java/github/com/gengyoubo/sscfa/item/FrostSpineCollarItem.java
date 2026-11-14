package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

/** 霜脊项圈（Forge 简化）：保留技能使用的静态参数与佩戴判定。 */
public class FrostSpineCollarItem extends AccessoryItem {
    public static final float CHARGE_INTERVAL_MULTIPLIER = 1.75f;
    public static final float DAMAGE_MULTIPLIER = 0.8f;

    public FrostSpineCollarItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
        return AddonAccessoryGuard.canEquip(entity, e -> github.com.gengyoubo.sscfa.util.FormUtils.isForm(e, github.com.gengyoubo.sscfa.util.FormIdentifiers.SNOW_FOX_FROSTSPINE));
    }

    public static boolean isWearingBy(LivingEntity entity) {
        return TrinketUtils.isWearingAuto(entity, github.com.gengyoubo.sscfa.init.AddonItems.FROST_SPINE_COLLAR.get());
    }
}
