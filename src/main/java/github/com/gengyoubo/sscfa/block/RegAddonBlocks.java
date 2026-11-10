package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.init.AddonBlocks;
import net.minecraft.world.level.block.Block;

/** Fabric 方块注册兼容 shim。 */
public final class RegAddonBlocks {
    private RegAddonBlocks() {}

    public static final Block WEB_MEMBRANE = AddonBlocks.WEB_MEMBRANE.get();
    public static final Block TEMP_WEB_BRIDGE = AddonBlocks.TEMP_WEB_BRIDGE.get();
    public static final Block INFUSION_ALTAR = AddonBlocks.INFUSION_ALTAR.get();
    public static final Block SPELL_RESEARCH_TABLE = AddonBlocks.SPELL_RESEARCH_TABLE.get();
}
