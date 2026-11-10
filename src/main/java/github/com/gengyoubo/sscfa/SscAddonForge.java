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

        // 客户端表现层 S2C 通道声明：服务端据此判断对端能否接收
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_CLAW_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_DASH_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_NOVA_LEAP_WINDOW);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_FALLEN_ALLAY_VEX_WINDOW);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_SPOOK_GHOST);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_WEB_HIGHLIGHT);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_DREAM_VEIL);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_FEAR_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_FEAR_HIDE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_FEAR_REVEAL);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_JUMP_KILL_SILK_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_SPEAR_CHARGE_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_FROST_SPIKE_CHARGE_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_SPIDER_MOON_WEAVER_SWING_STATE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_TIDAL_TETHER);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_OPEN_JOB_CHANGE);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_WHITELIST_GUI_SYNC);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_EVO_ROUTES_SYNC);
        AddonNetwork.clientbound(SscAddonNetworking.PACKET_ANIM_DEBUG_TOGGLE);
        AddonNetwork.clientbound(DomainManager.STATE);
        AddonNetwork.clientbound(DomainManager.SOUND);
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.network.DecorationParticles.ID);
        github.com.gengyoubo.sscfa.network.SustainedVisuals.init();
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.network.SustainedVisuals.STATE);
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.anomaly.AnomalyNetwork.STATE);
        github.com.gengyoubo.sscfa.spell.DeathFinaleManager.init();
        github.com.gengyoubo.sscfa.tutorial.SpellTutorialManager.register();
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.tutorial.SpellTutorialManager.OFFER);
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.spell.DeathFinaleManager.SNAPSHOT);

        BalanceIntegration.initServer(SscBalanceSchema.create());
        SkillCooldownSync.init();
        github.com.gengyoubo.sscfa.network.CountdownSync.init();
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.network.CountdownSync.STATE);
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
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.tutorial.FormTutorialManager.OFFER);
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent.SYNC);
        github.com.gengyoubo.sscfa.tutorial.FormTutorialManager.register();
        github.com.gengyoubo.sscfa.story.MoonScarStoryManager.register();
        github.com.gengyoubo.sscfa.story.TideSpiritStoryManager.register();
        github.com.gengyoubo.sscfa.evolution.EvolutionManager.register();
        SscAddonNetworking.registerServerReceivers();
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(SpellRegistry.INSTANCE);
            event.addListener(github.com.gengyoubo.sscfa.evolution.EvolutionRegistry.INSTANCE);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            github.com.gengyoubo.sscfa.anomaly.AnomalyManager.init();
            github.com.gengyoubo.sscfa.anomaly.FrozenControl.init();
            SpellDamageSource.bind(event.getServer());
        });

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AddonServerConfig.SPEC);
    }
}
