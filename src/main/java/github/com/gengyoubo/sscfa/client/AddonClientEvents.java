package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.balance.BalanceClient;
import github.com.gengyoubo.sscfa.balance.SscBalanceSchema;
import github.com.gengyoubo.sscfa.client.renderer.EnergyStorageTankRenderer;
import github.com.gengyoubo.sscfa.client.renderer.EnergyBottlerRenderer;
import github.com.gengyoubo.sscfa.client.screen.EnergyBottlerScreen;
import github.com.gengyoubo.sscfa.client.renderer.PotionStorageBoxRenderer;
import github.com.gengyoubo.sscfa.client.screen.PotionStorageBoxScreen;
import github.com.gengyoubo.sscfa.client.screen.SpellbookScreen;
import github.com.gengyoubo.sscfa.client.screen.PotionBagScreen;
import github.com.gengyoubo.sscfa.client.mana.AllaySPManaBar;
import github.com.gengyoubo.sscfa.client.mana.AnubisWolfSPSoulBar;
import github.com.gengyoubo.sscfa.client.mana.BatDesmodusBloodBar;
import github.com.gengyoubo.sscfa.client.mana.MancianimaResistanceBar;
import github.com.gengyoubo.sscfa.client.mana.SimpleResourceBarRenderer;
import github.com.gengyoubo.sscfa.client.mana.SnowFoxSPManaBar;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import github.com.gengyoubo.sscfa.init.AddonMenus;
import github.com.gengyoubo.sscfa.init.AddonEntities;
import github.com.gengyoubo.sscfa.init.AddonParticles;
import github.com.gengyoubo.sscfa.particle.client.InwardIceParticle;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.InfiniteEnergyPotionItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AddonClientEvents {
    private AddonClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AddonBlockEntities.ENERGY_STORAGE_TANK.get(), EnergyStorageTankRenderer::new);
        event.registerBlockEntityRenderer(AddonBlockEntities.ENERGY_BOTTLER.get(), EnergyBottlerRenderer::new);
        event.registerBlockEntityRenderer(AddonBlockEntities.POTION_STORAGE_BOX.get(), PotionStorageBoxRenderer::new);
        // 瀹炰綋娓叉煋鍣ㄥ皻鏈縼绉伙細鍏堟敞鍐屼笉鍙鐨勫崰浣嶆覆鏌撳櫒锛堢己娓叉煋鍣ㄧ殑瀹炰綋涓€杩涘叆瑙嗛噹瀹㈡埛绔氨浼氬穿婧冿級
        AddonEntities.shellProjectiles().forEach(type -> event.registerEntityRenderer(type.get(), NoopRenderer::new));
        event.registerEntityRenderer(AddonEntities.SPELL_FIRE_BOLT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AddonEntities.SPELL_FROST_SPIKE.get(), github.com.gengyoubo.sscfa.client.renderer.SpellFrostSpikeRenderer::new);
        event.registerEntityRenderer(AddonEntities.SPELL_METEOR.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AddonEntities.SPELL_CURSE_MARK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AddonEntities.BEEP_SHEEP.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AddonEntities.SPELL_MOONLIGHT_ARROW.get(), github.com.gengyoubo.sscfa.client.renderer.MoonlightArrowRenderer::new);
        event.registerEntityRenderer(AddonEntities.LUNAR_SPIRIT.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.GHOST_CAT.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.WITCH_FAMILIAR.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.AXOLOTL_SHIFTER.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.TIDAL_ORB.get(), github.com.gengyoubo.sscfa.client.renderer.TidalOrbRenderer::new);
        event.registerEntityRenderer(AddonEntities.GHOST_CAT.get(), github.com.gengyoubo.sscfa.client.renderer.GhostCatRenderer::new);
        event.registerEntityRenderer(AddonEntities.FROST_THORN.get(), github.com.gengyoubo.sscfa.client.renderer.FrostThornEntityRenderer::new);
        event.registerEntityRenderer(AddonEntities.FROST_ARRAY.get(), github.com.gengyoubo.sscfa.client.renderer.FrostArrayRenderer::new);
        event.registerEntityRenderer(AddonEntities.FROST_STORM.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.LASER_BEAM.get(), github.com.gengyoubo.sscfa.client.renderer.FluorescentLaserRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        SpellcastKeybindings.init();
        SpellcastKeybindings.register(event);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        github.com.gengyoubo.sscfa.client.tutorial.FormTutorialLessons.register(event);
        github.com.gengyoubo.sscfa.client.renderer.AllaySkinColor.init(event);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AddonParticles.INWARD_ICE.get(), InwardIceParticle.Factory::new);
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            runMixinAuditIfRequested();
            BalanceClient.init(SscBalanceSchema.create());
            CountdownClient.register();
            SkillCooldownClient.init();
            AddonSkillKeys.init();
            ClientTickCache.register();
            ClawClientState.register();
            DashClientState.register();
            NovaLeapClientState.register();
            FallenAllayVexClientState.register();
            AxolotlSprintKeyClient.register();
            FluorescentKeyClient.register();
            JumpKillClient.register();
            UpgradeAxolotlSkillClient.register();
            VenomSkillClient.register();
            VortexChargeClient.register();
            PlayDeadEndClient.register();
            SheepFormCameraController.register();
            WindSpiritClawClient.register();
            WindDashClient.register();
            MancianimaCrosshairTracker.register();
            NightmareSpookClient.register();
            WebHighlightClient.register();
            DreamVeilRenderer.register();
            NightmareFearClient.register();
            JumpKillSilkClient.register();
            UpgradeAxolotlSpearRenderState.register();
            FrostSpikeChargeClientState.register();
            FrostSpikeClient.register();
            SpiderMoonWeaverSwingClient.register();
            SpiderMoonWeaverSwingRenderer.register();
            SpiderMoonWeaverDoubleJumpClient.register();
            SpiderMoonWeaverAnimDebugHud.register();
            github.com.gengyoubo.sscfa.client.renderer.TidalTetherBeamRenderer.register();
            github.com.gengyoubo.sscfa.client.evolution.JobChangeSelectScreen.register();
            github.com.gengyoubo.sscfa.client.screen.WhitelistManageScreen.register();
            github.com.gengyoubo.sscfa.client.evolution.EvolutionClientSync.register();
            SpellcastClient.register();
            CastingVisualState.register();
            SpellbookStatusClient.init();
            github.com.gengyoubo.sscfa.client.hud.SpellbookHudRenderer.register();
            github.com.gengyoubo.sscfa.client.hud.SkillCooldownBarRenderer.register();
            github.com.gengyoubo.sscfa.client.renderer.CurseMarkIconRenderer.register();
            github.com.gengyoubo.sscfa.client.renderer.DomainRenderer.register();
            github.com.gengyoubo.sscfa.client.particle.FirstPersonParticles.init();
            github.com.gengyoubo.sscfa.client.SustainedVisualClient.init();
            github.com.gengyoubo.sscfa.client.ParametricVisualClient.init();
            github.com.gengyoubo.sscfa.client.AnomalyClient.init();
            github.com.gengyoubo.sscfa.client.renderer.AnomalyBadgeRenderer.register();
            github.com.gengyoubo.sscfa.client.DeathFinaleClient.register();
            github.com.gengyoubo.sscfa.client.tutorial.FormTutorialClient.register();
            github.com.gengyoubo.sscfa.client.tutorial.SpellTutorialClient.register();
            github.com.gengyoubo.sscfa.client.FormationResearchClient.register();
            MenuScreens.register(AddonMenus.SPELL_RESEARCH_TABLE_SCREEN_HANDLER.get(),
                    github.com.gengyoubo.sscfa.client.screen.SpellResearchTableScreen::new);
            MenuScreens.register(AddonMenus.ENERGY_BOTTLER.get(), EnergyBottlerScreen::new);
            MenuScreens.register(AddonMenus.POTION_STORAGE_BOX.get(), PotionStorageBoxScreen::new);
        MenuScreens.register(AddonMenus.SPELLBOOK_SCREEN_HANDLER.get(), SpellbookScreen::new);
        MenuScreens.register(AddonMenus.POTION_BAG_SCREEN_HANDLER.get(), PotionBagScreen::new);
        SimpleResourceBarRenderer.register(new AllaySPManaBar());
        SimpleResourceBarRenderer.register(new AnubisWolfSPSoulBar());
        SimpleResourceBarRenderer.register(new BatDesmodusBloodBar());
        SimpleResourceBarRenderer.register(new MancianimaResistanceBar());
        SimpleResourceBarRenderer.register(new SnowFoxSPManaBar());
        github.com.gengyoubo.sscfa.client.NineLivesHudRenderer.register();
        github.com.gengyoubo.sscfa.client.SeedEnergyHudRenderer.register();
        github.com.gengyoubo.sscfa.client.MoistureWaterRecoveryClient.register();
            var empty = new ResourceLocation(SscAddonForge.MOD_ID, "empty");
            for (var item : java.util.List.of(AddonItems.INFINITE_ENERGY_POTION, AddonItems.INFINITE_ENERGY_POTION_SPLASH, AddonItems.INFINITE_ENERGY_POTION_LINGERING)) {
                ItemProperties.register(item.get(), empty, (stack, world, entity, seed) -> {
                    var level = world != null ? world : entity != null ? entity.level() : null;
                    return (level != null ? InfiniteEnergyPotionItem.isRecharging(stack, level)
                            : InfiniteEnergyPotionItem.isEmptyByNbt(stack)) ? 1.0f : 0.0f;
                });
            }
        });
    }

    /** 开发用：以 {@code -Dssca.mixinAudit=true} 启动时，把客户端 mixin 的目标类全部加载一遍并把失败项写进日志。 */
    private static void runMixinAuditIfRequested() {
        if (!Boolean.getBoolean("ssca.mixinAudit")) return;
        var failures = github.com.gengyoubo.sscfa.util.MixinAudit.run("mixins", "client");
        var logger = org.slf4j.LoggerFactory.getLogger("ssc_addon/MixinAudit");
        failures.forEach(failure -> logger.error("MixinAudit FAILED {}", failure));
        logger.info("MixinAudit finished: {} failure(s)", failures.size());
    }
}
