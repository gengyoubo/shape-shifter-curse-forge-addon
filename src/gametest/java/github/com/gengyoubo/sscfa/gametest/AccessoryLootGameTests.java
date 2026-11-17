package github.com.gengyoubo.sscfa.gametest;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The accessory drop pools are added to the vanilla tables they belong to (loot.AccessoryLoot). */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AccessoryLootGameTests {
    private AccessoryLootGameTests() {}

    @GameTest(template = "gametest_floor")
    public static void accessoryPoolsAreInjected(GameTestHelper helper) {
        var loot = helper.getLevel().getServer().getLootData();
        assertPool(helper, loot, "chests/desert_pyramid", "ssc_addon:ankh_stone");
        assertPool(helper, loot, "chests/abandoned_mineshaft", "ssc_addon:venom_gland");
        assertPool(helper, loot, "chests/simple_dungeon", "ssc_addon:venom_gland");
        assertPool(helper, loot, "entities/cat", "ssc_addon:lifesaving_cat_tail");
        helper.succeed();
    }

    private static void assertPool(GameTestHelper helper, net.minecraft.world.level.storage.loot.LootDataManager loot,
                                   String table, String pool) {
        var lootTable = loot.getLootTable(new ResourceLocation("minecraft", table));
        helper.assertTrue(lootTable.getPool(pool) != null, table + " is missing pool " + pool);
    }
}
