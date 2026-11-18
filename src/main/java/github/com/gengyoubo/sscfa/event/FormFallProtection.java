package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reject immune landings before vanilla emits the hurt animation and landing damage sound. */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class FormFallProtection {
    private static final ResourceLocation FALL_IMMUNITY = ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "fall_immunity");

    private FormFallProtection() {}

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && ResourceBars.hasPowerId(player, FALL_IMMUNITY)) {
            event.setCanceled(true);
        }
    }
}
