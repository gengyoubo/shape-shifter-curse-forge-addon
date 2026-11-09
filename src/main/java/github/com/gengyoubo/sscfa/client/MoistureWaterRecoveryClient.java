package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.compat.MoistureWaterRecovery;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Client-side half of the shallow-water correction so the HUD stops flickering. */
public final class MoistureWaterRecoveryClient {
    private MoistureWaterRecoveryClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                MoistureWaterRecovery.capture(net.minecraft.client.Minecraft.getInstance().player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                MoistureWaterRecovery.restore(net.minecraft.client.Minecraft.getInstance().player);
            }
        });
    }
}
