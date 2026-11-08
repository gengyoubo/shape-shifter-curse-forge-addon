package github.com.gengyoubo.sscfa.energy;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 游戏事件总线上的网络缓存生命周期。 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnergyNetworkEvents {
    private EnergyNetworkEvents() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) EnergyNetworkTopology.invalidateWorld(level);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) EnergyNetworkTopology.invalidateWorld(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        EnergyNetworkTopology.clearTopology();
    }
}
