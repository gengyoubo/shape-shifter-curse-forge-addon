package github.com.gengyoubo.sscfa.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabric {@code LootTableEvents.MODIFY} on Forge's {@link LootTableLoadEvent}, with the same lambda
 * shape so the ported loot classes keep their bodies. Forge refuses unnamed or duplicate pool names,
 * so each added pool gets a unique {@code ssc_addon:} name.
 */
public final class LootTableMods {
    private static final AtomicInteger POOLS = new AtomicInteger();

    @FunctionalInterface
    public interface TableBuilder {
        void withPool(LootPool.Builder pool);
    }

    /** Arguments mirror Fabric's; the resource manager, loot manager and source are not provided. */
    @FunctionalInterface
    public interface Handler {
        void modify(Object resourceManager, Object lootManager, ResourceLocation id, TableBuilder tableBuilder, Object source);
    }

    private LootTableMods() {}

    public static void register(Handler handler) {
        MinecraftForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> handler.modify(null, null, event.getName(),
                pool -> event.getTable().addPool(pool.name("ssc_addon:pool_" + POOLS.incrementAndGet()).build()), null));
    }
}
