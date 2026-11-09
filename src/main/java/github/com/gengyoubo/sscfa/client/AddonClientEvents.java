package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.balance.BalanceClient;
import github.com.gengyoubo.sscfa.balance.SscBalanceSchema;
import github.com.gengyoubo.sscfa.client.renderer.EnergyStorageTankRenderer;
import github.com.gengyoubo.sscfa.client.renderer.EnergyBottlerRenderer;
import github.com.gengyoubo.sscfa.client.screen.EnergyBottlerScreen;
import github.com.gengyoubo.sscfa.client.renderer.PotionStorageBoxRenderer;
import github.com.gengyoubo.sscfa.client.screen.PotionStorageBoxScreen;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import github.com.gengyoubo.sscfa.init.AddonMenus;
import github.com.gengyoubo.sscfa.init.AddonEntities;
import github.com.gengyoubo.sscfa.init.AddonParticles;
import github.com.gengyoubo.sscfa.particle.client.InwardIceParticle;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
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
        // 实体渲染器尚未迁移：先注册不可见的占位渲染器（缺渲染器的实体一进入视野客户端就会崩溃）
        AddonEntities.shellProjectiles().forEach(type -> event.registerEntityRenderer(type.get(), NoopRenderer::new));
        event.registerEntityRenderer(AddonEntities.LUNAR_SPIRIT.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.GHOST_CAT.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.WITCH_FAMILIAR.get(), NoopRenderer::new);
        event.registerEntityRenderer(AddonEntities.AXOLOTL_SHIFTER.get(), NoopRenderer::new);
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
            MenuScreens.register(AddonMenus.ENERGY_BOTTLER.get(), EnergyBottlerScreen::new);
            MenuScreens.register(AddonMenus.POTION_STORAGE_BOX.get(), PotionStorageBoxScreen::new);
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
