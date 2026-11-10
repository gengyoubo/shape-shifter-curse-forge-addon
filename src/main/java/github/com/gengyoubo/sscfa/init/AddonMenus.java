package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.screen.EnergyBottlerScreenHandler;
import github.com.gengyoubo.sscfa.screen.PotionStorageBoxScreenHandler;
import github.com.gengyoubo.sscfa.screen.SpellbookScreenHandler;
import github.com.gengyoubo.sscfa.screen.PotionBagScreenHandler;
import github.com.gengyoubo.sscfa.screen.InfusionAltarScreenHandler;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AddonMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SscAddonForge.MOD_ID);
    public static final RegistryObject<MenuType<EnergyBottlerScreenHandler>> ENERGY_BOTTLER =
            MENUS.register("energy_bottler", () -> new MenuType<>(EnergyBottlerScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<PotionStorageBoxScreenHandler>> POTION_STORAGE_BOX =
            MENUS.register("potion_storage_box", () -> new MenuType<>(PotionStorageBoxScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<SpellbookScreenHandler>> SPELLBOOK_SCREEN_HANDLER =
            MENUS.register("spellbook", () -> net.minecraftforge.common.extensions.IForgeMenuType.create((id, inv, buf) -> new SpellbookScreenHandler(id, inv, buf)));



    public static final RegistryObject<MenuType<PotionBagScreenHandler>> POTION_BAG_SCREEN_HANDLER =
            MENUS.register("potion_bag", () -> new MenuType<>(PotionBagScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<InfusionAltarScreenHandler>> INFUSION_ALTAR_SCREEN_HANDLER =
            MENUS.register("infusion_altar", () -> new MenuType<>(InfusionAltarScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<SpellResearchTableScreenHandler>> SPELL_RESEARCH_TABLE_SCREEN_HANDLER =
            MENUS.register("spell_research_table", () -> new MenuType<>(SpellResearchTableScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));

    private AddonMenus() {}
}
