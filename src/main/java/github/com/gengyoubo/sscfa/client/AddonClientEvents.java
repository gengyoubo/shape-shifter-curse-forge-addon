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
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        SpellcastKeybindings.init();
        SpellcastKeybindings.register(event);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AddonParticles.INWARD_ICE.get(), InwardIceParticle.Factory::new);
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BalanceClient.init(SscBalanceSchema.create());
            SkillCooldownClient.init();
            AddonSkillKeys.init();
            SpellcastClient.register();
            CastingVisualState.register();
            SpellbookStatusClient.init();
            github.com.gengyoubo.sscfa.client.hud.SpellbookHudRenderer.register();
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
        github.com.gengyoubo.sscfa.client.SkillCooldownClient.init();
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

}
