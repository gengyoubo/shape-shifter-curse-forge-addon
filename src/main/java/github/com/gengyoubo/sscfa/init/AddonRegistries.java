package github.com.gengyoubo.sscfa.init;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Attaches each deferred registry to the mod lifecycle. */
public final class AddonRegistries {
    private AddonRegistries() {}

    public static void init(IEventBus modBus) {
        AddonBlocks.BLOCKS.register(modBus);
        AddonBlockEntities.BLOCK_ENTITIES.register(modBus);
        AddonMenus.MENUS.register(modBus);
        AddonEffects.EFFECTS.register(modBus);
        AddonEffects.POTIONS.register(modBus);
        AddonRecipes.SERIALIZERS.register(modBus);
        AddonItems.ITEMS.register(modBus);
        AddonItems.TABS.register(modBus);
        modBus.addListener(AddonRegistries::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AddonEffects::registerBrewing);
    }
}
