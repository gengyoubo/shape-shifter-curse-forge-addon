package github.com.gengyoubo.sscfa;

import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.balance.SscBalanceSchema;
import github.com.gengyoubo.sscfa.config.AddonServerConfig;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldownSync;
import github.com.gengyoubo.sscfa.power.AddonPowerTypes;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import github.com.gengyoubo.sscfa.forms.AddonForms;
import github.com.gengyoubo.sscfa.init.AddonRegistries;

@Mod(SscAddonForge.MOD_ID)
public final class SscAddonForge {
    public static final String MOD_ID = "ssc_addon";

    public SscAddonForge() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        AddonRegistries.init(bus);
        AddonForms.init();
        AddonNetwork.init();
        BalanceIntegration.initServer(SscBalanceSchema.create());
        SkillCooldownSync.init();
        AddonPowerTypes.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AddonServerConfig.SPEC);
    }
}
