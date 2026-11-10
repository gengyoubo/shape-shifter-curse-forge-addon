package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import github.com.gengyoubo.sscfa.init.AddonMenus;
import github.com.gengyoubo.sscfa.screen.EnergyBottlerScreenHandler;
import github.com.gengyoubo.sscfa.screen.InfusionAltarScreenHandler;
import github.com.gengyoubo.sscfa.screen.PotionStorageBoxScreenHandler;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric 方块实体/菜单注册兼容 shim。 */
public final class RegAddonBlockEntities {
    private RegAddonBlockEntities() {}

    public static final MenuType<EnergyBottlerScreenHandler> ENERGY_BOTTLER_SH = AddonMenus.ENERGY_BOTTLER.get();
    public static final MenuType<PotionStorageBoxScreenHandler> POTION_STORAGE_BOX_SH = AddonMenus.POTION_STORAGE_BOX.get();
    public static final MenuType<InfusionAltarScreenHandler> INFUSION_ALTAR_SH = AddonMenus.INFUSION_ALTAR_SCREEN_HANDLER.get();
    public static final MenuType<SpellResearchTableScreenHandler> SPELL_RESEARCH_TABLE_SH = AddonMenus.SPELL_RESEARCH_TABLE_SCREEN_HANDLER.get();

    public static final BlockEntityType<InfusionAltarBlockEntity> INFUSION_ALTAR_BE = AddonBlockEntities.INFUSION_ALTAR.get();
    public static final BlockEntityType<SpellResearchTableBlockEntity> SPELL_RESEARCH_TABLE_BE = AddonBlockEntities.SPELL_RESEARCH_TABLE.get();
}
