package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.screen.EnergyBottlerScreenHandler;
import github.com.gengyoubo.sscfa.screen.PotionStorageBoxScreenHandler;
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
    private AddonMenus() {}
}
