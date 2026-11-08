package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.client.renderer.EnergyStorageTankRenderer;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AddonClientEvents {
    private AddonClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AddonBlockEntities.ENERGY_STORAGE_TANK.get(), EnergyStorageTankRenderer::new);
    }

}
