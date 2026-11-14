package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.EvolutionStoneItem;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.form.TransformManager;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EvolutionStoneGameTests {
    @GameTest(template = "gametest_floor")
    public static void formAliasesResolveToRegisteredForm(GameTestHelper helper) {
        var oldId = new ResourceLocation("my_addon", "axolotl_sp");
        var oldForm = FormRegistry.get(oldId);
        helper.assertTrue(oldForm != null, "legacy form must remain registered");
        helper.assertTrue(oldForm == FormRegistry.get(FormIdentifiers.AXOLOTL_SP), "new form id must resolve to the same definition");
        helper.assertTrue(FormIdentifiers.AXOLOTL_FLUORESCENT.equals(EvolutionStoneItem.targetOf(new ResourceLocation("my_addon", "upgrade_axolotl"))),
                "legacy evolution route must resolve to its target");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void stoneStartsSpiderEvolutionAndConsumesOnce(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_stone_test"));
        helper.assertTrue(FormManager.setForm(player, new ResourceLocation("shape-shifter-curse", "spider_3")), "spider form must be available");
        ItemStack stack = new ItemStack(AddonItems.EVOLUTION_STONE.get());
        stack.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(TransformManager.isTransforming(player), "stone must start transformation");
        helper.assertTrue(stack.isEmpty(), "stone must be consumed after transformation starts");
        TransformManager.cancel(player);
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void stoneKeepsItemForUnsupportedForm(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_stone_invalid"));
        ItemStack stack = new ItemStack(AddonItems.EVOLUTION_STONE.get());
        stack.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(stack.getCount() == 1, "unsupported form must not consume the stone");
        helper.assertTrue(!TransformManager.isTransforming(player), "unsupported form must not start transformation");
        helper.succeed();
    }
}
