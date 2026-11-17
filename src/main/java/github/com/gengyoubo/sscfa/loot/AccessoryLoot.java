package github.com.gengyoubo.sscfa.loot;

import github.com.gengyoubo.sscfa.init.AddonItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Where the addon's accessories drop: one extra pool per (vanilla table, item), gated by a random
 * chance. Port of the per-item Fabric {@code registerLootTable()} hooks (LootTableEvents.MODIFY);
 * on Forge, {@link LootTableLoadEvent} adds the same pools when the vanilla table loads.
 */
public final class AccessoryLoot {
    private record Drop(String table, RegistryObject<Item> item, float chance, NumberProvider rolls) {}

    private static final List<Drop> DROPS = List.of(
            drop("chests/desert_pyramid", AddonItems.ANKH_STONE, 0.15F, UniformGenerator.between(1.0F, 2.0F)),
            drop("chests/desert_pyramid", AddonItems.ANUBIS_CRYSTAL, 0.15F),
            drop("chests/desert_pyramid", AddonItems.EROSION_SAND_PRISM, 0.15F),
            drop("chests/desert_pyramid", AddonItems.WITHERED_SAND_RING, 0.15F),
            drop("chests/simple_dungeon", AddonItems.NIGHTMARE_RING, 0.15F),
            drop("chests/igloo_chest", AddonItems.FROST_SPINE_COLLAR, 0.15F),
            drop("chests/simple_dungeon", AddonItems.FROST_SPINE_COLLAR, 0.10F),
            drop("chests/abandoned_mineshaft", AddonItems.VENOM_GLAND, 0.15F),
            drop("chests/simple_dungeon", AddonItems.VENOM_GLAND, 0.10F),
            drop("chests/abandoned_mineshaft", AddonItems.BLOOD_GARNET, 0.20F),
            drop("chests/abandoned_mineshaft", AddonItems.BLOODLUST_RING, 0.20F),
            drop("chests/abandoned_mineshaft", AddonItems.HUMUS_RING, 0.20F),
            drop("chests/shipwreck_treasure", AddonItems.SEA_CRYSTAL_PENDANT, 0.15F),
            drop("chests/buried_treasure", AddonItems.SEA_CRYSTAL_PENDANT, 0.15F),
            drop("chests/pillager_outpost", AddonItems.BINDING_ANKLET, 0.25F),
            // Cats drop their lifesaving tail very rarely.
            drop("entities/cat", AddonItems.LIFESAVING_CAT_TAIL, 0.01F));

    private static boolean registered;

    private AccessoryLoot() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(AccessoryLoot::onLoad);
    }

    private static void onLoad(LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        if (!"minecraft".equals(id.getNamespace())) return;
        for (Drop drop : DROPS) {
            if (!drop.table().equals(id.getPath())) continue;
            event.getTable().addPool(LootPool.lootPool()
                    .name("ssc_addon:" + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(drop.item().get()).getPath())
                    .setRolls(drop.rolls())
                    .when(LootItemRandomChanceCondition.randomChance(drop.chance()))
                    .add(LootItem.lootTableItem(drop.item().get()))
                    .build());
        }
    }

    private static Drop drop(String table, RegistryObject<Item> item, float chance) {
        return drop(table, item, chance, ConstantValue.exactly(1.0F));
    }

    private static Drop drop(String table, RegistryObject<Item> item, float chance, NumberProvider rolls) {
        return new Drop(table, item, chance, rolls);
    }
}
