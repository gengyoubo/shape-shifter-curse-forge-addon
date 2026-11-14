package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.util.InstinctAnchorSavedData;
import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import net.onixary.shapeShifterCurseForge.power.InstinctService;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InstinctAnchorGameTests {
    @GameTest(template = "gametest_floor")
    public static void anchorFreezesInstinctAndBlocksTransformation(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_anchor_test"));
        var spider = new ResourceLocation("shape-shifter-curse", "spider_1");
        var bat = new ResourceLocation("shape-shifter-curse", "bat_3");
        helper.assertTrue(FormManager.setForm(player, spider), "test form must be available");
        var data = SscApi.currentForm(player).orElseThrow();
        data.setInstinctValue(45);
        try {
            var result = InstinctAnchors.apply(player, true);
            helper.assertTrue(result == InstinctAnchors.Result.FIXED, "anchor should activate, got " + result + " for " + FormManager.current(player));
            helper.assertTrue(InstinctAnchors.apply(player, true) == InstinctAnchors.Result.ALREADY_FIXED, "duplicate anchor must fail");
            InstinctService.add(player, "test", 25, 0, true);
            InstinctService.tick(player);
            InstinctService.reset(player);
            helper.assertTrue(data.getInstinctValue() == 45, "anchoring must preserve instinct value");
            helper.assertTrue(!FormManager.setForm(player, bat) && !TransformManager.startTransform(player, bat), "anchor must reject form changes");
            helper.assertTrue(FormManager.current(player).id().equals(spider), "blocked changes must keep the current form");
            helper.assertTrue(InstinctAnchors.apply(player, false) == InstinctAnchors.Result.RELEASED, "release should remove this anchor");
            helper.assertTrue(InstinctAnchors.apply(player, false) == InstinctAnchors.Result.NOT_FIXED, "release must not clear another lock");
            helper.assertTrue(FormManager.setForm(player, bat), "form changes should work after release");
        } finally {
            InstinctAnchorSavedData.get(player.getServer()).setLocked(player.getUUID(), false);
            TransformManager.cancel(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void anchorsPersistAndRemainPlayerSpecific(GameTestHelper helper) {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var data = new InstinctAnchorSavedData();
        data.setLocked(first, true);
        var restored = InstinctAnchorSavedData.load(data.save(new CompoundTag()));
        helper.assertTrue(restored.isLocked(first) && !restored.isLocked(second), "saved anchor must belong to its player");
        restored.setLocked(first, false);
        helper.assertTrue(!InstinctAnchorSavedData.load(restored.save(new CompoundTag())).isLocked(first), "release must persist");
        helper.succeed();
    }
}
