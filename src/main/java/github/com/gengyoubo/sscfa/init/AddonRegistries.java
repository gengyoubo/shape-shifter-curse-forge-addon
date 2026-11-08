package github.com.gengyoubo.sscfa.init;

import net.minecraftforge.eventbus.api.IEventBus;

/** Attaches each deferred registry to the mod lifecycle. */
public final class AddonRegistries {
    private AddonRegistries() {}

    public static void init(IEventBus modBus) {
        AddonBlocks.BLOCKS.register(modBus);
        AddonBlockEntities.BLOCK_ENTITIES.register(modBus);
        AddonItems.ITEMS.register(modBus);
        AddonItems.TABS.register(modBus);
    }
}
