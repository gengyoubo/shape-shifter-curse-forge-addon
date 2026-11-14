package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.compat.RegCustomItem;
import github.com.gengyoubo.sscfa.init.AddonBlocks;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent;
import github.com.gengyoubo.sscfa.spell.FormationKnowledgeSavedData;
import github.com.gengyoubo.sscfa.spell.ResearchTableManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchTableGameTests {
    @GameTest(template = "gametest_floor")
    public static void placedTableOpensMenuAndPersistsItems(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var world = helper.getLevel();
        world.setBlockAndUpdate(pos, AddonBlocks.SPELL_RESEARCH_TABLE.get().defaultBlockState());
        var entity = world.getBlockEntity(pos);
        helper.assertTrue(entity instanceof SpellResearchTableBlockEntity, "placed research table must create its block entity");
        var table = (SpellResearchTableBlockEntity) entity;
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "ssca_research_test"));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var menu = table.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof SpellResearchTableScreenHandler && menu.stillValid(player), "table must provide a usable menu");
        table.setItem(SpellResearchTableBlockEntity.SLOT_PAPER, new ItemStack(AddonItems.BLANK_FORMATION_PAPER.get(), 2));
        var restored = new SpellResearchTableBlockEntity(pos, table.getBlockState());
        restored.load(table.saveWithFullMetadata());
        helper.assertTrue(restored.getItem(SpellResearchTableBlockEntity.SLOT_PAPER).getCount() == 2, "table contents must survive save and load");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void researchConsumesMaterialsAndProducesFormation(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var world = helper.getLevel();
        world.setBlockAndUpdate(pos, AddonBlocks.SPELL_RESEARCH_TABLE.get().defaultBlockState());
        var table = (SpellResearchTableBlockEntity) world.getBlockEntity(pos);
        var knowledge = new FormationKnowledgeComponent();
        knowledge.record(FormationElement.FIRE, null, 1);
        table.setItem(SpellResearchTableBlockEntity.SLOT_MOONDUST, new ItemStack(RegCustomItem.UNTREATED_MOONDUST, 2));
        helper.assertTrue(ResearchTableManager.transact(table, knowledge, "fire", null, 1, false, 0).equals("ok"), "recorded formation should be learnable");
        helper.assertTrue(table.getItem(SpellResearchTableBlockEntity.SLOT_MOONDUST).isEmpty(), "learning must consume dust");
        table.setItem(SpellResearchTableBlockEntity.SLOT_PAPER, new ItemStack(AddonItems.BLANK_FORMATION_PAPER.get()));
        table.setItem(SpellResearchTableBlockEntity.SLOT_INK, new ItemStack(AddonItems.INK_FIRE.get()));
        helper.assertTrue(ResearchTableManager.transact(table, knowledge, "fire", null, 1, true, 0).equals("ok"), "learned formation should be scribed");
        helper.assertTrue(FormationData.getElement(table.getItem(SpellResearchTableBlockEntity.SLOT_OUTPUT)) == FormationElement.FIRE, "output must be a fire formation");
        helper.assertTrue(table.getItem(SpellResearchTableBlockEntity.SLOT_PAPER).isEmpty()
                && table.getItem(SpellResearchTableBlockEntity.SLOT_INK).isEmpty(), "scribing must consume paper and ink");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void knowledgePersistsPerPlayer(GameTestHelper helper) {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        var data = new FormationKnowledgeSavedData();
        var knowledge = data.getOrCreate(first);
        knowledge.record(FormationElement.FIRE, null, 2);
        knowledge.learn(FormationElement.FIRE, null, 2);
        knowledge.recordSpell("fire_bolt");
        knowledge.research().rank = 2;
        var restored = FormationKnowledgeSavedData.load(data.save(new CompoundTag()));
        var saved = restored.getOrCreate(first);
        helper.assertTrue(saved.hasRecorded(FormationElement.FIRE, null, 2)
                && saved.hasLearned(FormationElement.FIRE, null, 2)
                && saved.hasSpell("fire_bolt") && saved.research().rank == 2,
                "recorded, learned, spell and research progress must survive a world save");
        helper.assertTrue(!restored.getOrCreate(second).hasRecorded(FormationElement.FIRE, null, 2),
                "one player's knowledge must not leak to another player");
        helper.succeed();
    }
}
