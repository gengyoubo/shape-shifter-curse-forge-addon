package github.com.gengyoubo.sscfa.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import github.com.gengyoubo.sscfa.util.TrinketUtils;

/** 毒腺（Forge 简化）：保留能力层使用的静态参数与佩戴判定。 */
public class VenomGlandItem extends Item {
    public static final float DURATION_SCALE = 0.7f;

    public VenomGlandItem(Properties properties) {
        super(properties);
    }

    public static boolean isWearingBy(LivingEntity entity) {
        return TrinketUtils.isWearingAuto(entity, github.com.gengyoubo.sscfa.init.AddonItems.VENOM_GLAND.get());
    }
}
