package github.com.gengyoubo.sscfa;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import github.com.gengyoubo.sscfa.init.AddonRegistries;

@Mod(SscAddonForge.MOD_ID)
public final class SscAddonForge {
    public static final String MOD_ID = "ssc_addon";

    public SscAddonForge() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        AddonRegistries.init(bus);
    }
}
