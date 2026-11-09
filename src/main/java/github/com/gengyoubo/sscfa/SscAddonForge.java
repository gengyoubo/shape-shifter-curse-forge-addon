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
import github.com.gengyoubo.sscfa.network.JsonSnapshotSync;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.spell.FormSpellStyle;
import github.com.gengyoubo.sscfa.spell.SpellDamageSource;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

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

        // 法术系统
        SpellRegistry.init();
        FormSpellStyle.register();
        DomainManager.init();
        JsonSnapshotSync.register();
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_SPELL_CONFIG_SYNC);
        AddonNetwork.clientbound(SpellChannelManager.STATE);
        AddonNetwork.clientbound(SpellChannelManager.CAST_VISUAL);
        AddonNetwork.clientbound(SpellChannelManager.SUPPRESSION);
        AddonNetwork.clientbound(SharedSpellCooldowns.SYNC);
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.network.SpellbookStatusSync.STATE);
        SpellChannelManager.init();
        SharedSpellCooldowns.init();
        github.com.gengyoubo.sscfa.network.SpellbookStatusSync.init();
        github.com.gengyoubo.sscfa.spell.pocket.PocketSpaceManager.init();
        SscAddonNetworking.registerServerReceivers();
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(SpellRegistry.INSTANCE));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> SpellDamageSource.bind(event.getServer()));

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AddonServerConfig.SPEC);
    }
}
