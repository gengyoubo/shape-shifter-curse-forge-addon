package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.item.Item;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/** SSC 核心物品兼容入口。 */
public final class RegCustomItem {
    private RegCustomItem() {}

    public static final Item MORPHSCALE_CORE = ModItems.MORPHSCALE_CORE.get();
    public static final Item MOONDUST_CRYSTAL_SHARD = ModItems.MOONDUST_CRYSTAL_SHARD.get();
    public static final Item UNTREATED_MOONDUST = ModItems.UNTREATED_MOONDUST.get();
    public static final Item VENOM_SPINDLE = ModItems.VENOM_SPINDLE.get();
    /** SSC Forge 以测试版饰品批量注册，没有独立 RegistryObject；缺失时为 null。 */
    public static final Item VENOM_SPINDLE_PLUS = lookup("venom_spindle_plus");

    private static Item lookup(String path) {
        Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", path));
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }
}
