package github.com.gengyoubo.sscfa.moonmarrow;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.evolution.EvolutionManager;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.story.MoonScarStoryState;
import github.com.gengyoubo.sscfa.util.InstinctAnchorSavedData;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.TransformManager;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoonMarrowGameTests {
    @GameTest(template = "gametest_floor")
    public static void branchGateRequiresBothUnlocks(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_moon_gate"));
        var oldId = new ResourceLocation("my_addon", "upgrade_axolotl");
        helper.assertTrue(FormManager.setForm(player, oldId), "upgrade axolotl must exist");
        var data = EvolutionManager.get(player);
        data.setRoute("axolotl");
        helper.assertTrue(MoonMarrowRing.targetOf(FormIdentifiers.UPGRADE_AXOLOTL).equals(MoonMarrowRing.AXOLOTL_SP),
                "renamed form must resolve to the ring target");
        helper.assertTrue(!MoonMarrowRing.passesEvolutionGate(player), "locked branches must block evolution");
        data.unlock("branch_moon_ring");
        helper.assertTrue(!MoonMarrowRing.passesEvolutionGate(player), "one branch is not sufficient");
        data.unlock("branch_stone");
        helper.assertTrue(MoonMarrowRing.passesEvolutionGate(player), "both branches must allow evolution");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void timedRedFormRevertsButPermanentRedRemains(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_red_timeout"));
        helper.assertTrue(FormManager.setForm(player, FormIdentifiers.FAMILIAR_FOX_RED), "red form must be registered");
        player.tickCount = 20;
        TemporaryRedForm.tick(player);
        helper.assertTrue(FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED), "red without a deadline must remain");
        var tag = "ssc_addon_red_expire:" + (player.level().getGameTime() - 1);
        player.addTag(tag);
        TemporaryRedForm.tick(player);
        helper.assertTrue(FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_SP), "expired red form must revert to familiar fox SP");
        helper.assertTrue(!player.getTags().contains(tag), "successful reversion must clear the deadline");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void pendingRedTransformationRetainsDeadline(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_red_pending"));
        helper.assertTrue(FormManager.setForm(player, MoonMarrowRing.FAMILIAR_FOX_3), "base familiar fox must exist");
        helper.assertTrue(TransformManager.startTransform(player, FormIdentifiers.FAMILIAR_FOX_RED), "red transformation must start");
        String tag = "ssc_addon_red_expire:" + (player.level().getGameTime() + 12000);
        player.addTag(tag);
        player.tickCount = 20;
        TemporaryRedForm.tick(player);
        helper.assertTrue(player.getTags().contains(tag), "deadline must survive the transformation animation");
        TransformManager.cancel(player);
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void storyRedRevertsForFreeAndKeepsStateWhenBlocked(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_red_story"));
        helper.assertTrue(FormManager.setForm(player, FormIdentifiers.FAMILIAR_FOX_RED), "story red form must exist");
        var state = MoonScarStoryState.get(player.getServer());
        state.storyRedPlayers.add(player.getUUID());
        var anchor = InstinctAnchorSavedData.get(player.getServer());
        var ring = new ItemStack(AddonItems.SP_UPGRADE_THING.get());
        try {
            anchor.setLocked(player.getUUID(), true);
            ring.finishUsingItem(player.level(), player);
            helper.assertTrue(ring.getCount() == 1 && state.storyRedPlayers.contains(player.getUUID()),
                    "blocked reversion must keep the ring and story state");
            anchor.setLocked(player.getUUID(), false);
            ring.finishUsingItem(player.level(), player);
            helper.assertTrue(ring.getCount() == 1 && TransformManager.isTransforming(player),
                    "story reversion should start without consuming the ring");
            helper.assertTrue(!state.storyRedPlayers.contains(player.getUUID()), "successful reversion must clear story state");
        } finally {
            state.storyRedPlayers.remove(player.getUUID());
            anchor.setLocked(player.getUUID(), false);
            TransformManager.cancel(player);
        }
        helper.succeed();
    }
}
