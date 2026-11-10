package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.item.Item;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/** SSC 核心物品兼容入口。 */
public final class RegCustomItem {
    private RegCustomItem() {}

    public static final Item MORPHSCALE_CORE = ModItems.MORPHSCALE_CORE.get();
    public static final Item MOONDUST_CRYSTAL_SHARD = ModItems.MOONDUST_CRYSTAL_SHARD.get();
    public static final Item UNTREATED_MOONDUST = ModItems.UNTREATED_MOONDUST.get();
}
