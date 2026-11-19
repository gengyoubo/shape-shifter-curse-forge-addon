package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.compat.AddonPerkTrees;
import github.com.gengyoubo.sscfa.compat.CTPUtils;
import github.com.gengyoubo.sscfa.compat.ManaUtils;
import github.com.gengyoubo.sscfa.neomagic.NeoRuntime;
import github.com.gengyoubo.sscfa.spell.CollapsePenalties;
import github.com.gengyoubo.sscfa.spell.PlayerSpellMastery;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import github.com.gengyoubo.sscfa.spell.research.RuneScheme;
import github.com.gengyoubo.sscfa.spell.research.WorldRuneState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

/** 2026-11-19 TODO 中占位实现的服务端回归：perk 镜像、崩解惩罚、熟练度、符文方案授权与 CTP 识别。 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlaceholderPortGameTests {
    private PlaceholderPortGameTests() {}

    @GameTest(template = "gametest_floor")
    public static void addonPerkTreesMirrorSscTrees(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        var powers = FormPowerRegistry.all();
        for (String[] mirror : AddonPerkTrees.describe()) {
            String form = mirror[0];
            var source = SscJavaRegistries.perkTree(ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", mirror[1]));
            var tree = SscJavaRegistries.perkTree(AddonPerkTrees.treeId(form));
            if (source.isEmpty() || tree.isEmpty()) {
                problems.add(form + ": tree missing");
                continue;
            }
            if (source.get().perks().size() != tree.get().perks().size()
                    || source.get().edges().size() != tree.get().edges().size()) problems.add(form + ": shape differs");
            if (!tree.get().matchesForm(ResourceLocation.fromNamespaceAndPath("my_addon", form))) problems.add(form + ": requiredForm");
            for (ResourceLocation perkId : tree.get().perks()) {
                var perk = SscJavaRegistries.perk(perkId).orElse(null);
                if (perk == null) {
                    problems.add(perkId + ": unregistered");
                    continue;
                }
                for (ResourceLocation added : perk.powersAdded()) {
                    if (!powers.containsKey(added)) problems.add(perkId + " adds unknown power " + added);
                }
            }
        }
        var water = SscJavaRegistries.perk(ResourceLocation.fromNamespaceAndPath("my_addon",
                "axolotl_sp/axolotl_water_magic_efficiency")).orElse(null);
        helper.assertTrue(water != null && water.powersAdded().contains(
                ResourceLocation.fromNamespaceAndPath("my_addon", "perk_axolotl_sp_water_magic_efficiency")),
                "axolotl_sp water magic perk must grant the SP power");
        helper.assertTrue(problems.isEmpty(), "perk mirror problems: " + problems);
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void collapsePenaltyFollowsBurdenAndClears(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_collapse_test"));
        var data = NeoRuntime.data(player);
        double safety = NeoRuntime.profile(player).safety();
        double before = data.burden;
        try {
            helper.assertTrue(CollapsePenalties.stage(safety, safety) == 0, "C == N is safe");
            helper.assertTrue(CollapsePenalties.stage(safety * 1.2, safety) == 1, "boundary belongs to the lower band");
            data.burden = safety * 1.5;
            CollapsePenalties.tick(player);
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(CollapsePenalties.currentStage(player) == 3, "1.5x safety is stage 3");
            helper.assertTrue(speed.getModifiers().stream().anyMatch(m -> Math.abs(m.getAmount() + 0.40) < 1e-9),
                    "stage 3 applies the 40% slowdown");
            data.burden = 0;
            CollapsePenalties.tick(player);
            helper.assertTrue(speed.getModifiers().stream().noneMatch(m -> "SSCA collapse slowdown".equals(m.getName())),
                    "recovering below safety removes the slowdown");
        } finally {
            data.burden = before;
            CollapsePenalties.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void masteryGrantCountsOnce(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_mastery_test"));
        var data = NeoRuntime.data(player);
        int before = data.spellbookMasteryExpTen;
        PlayerSpellMastery.grant(player, null, 100);
        helper.assertTrue(data.spellbookMasteryExpTen - before == SpellbookData.boostedExpTen(100),
                "one grant adds the boosted amount exactly once");
        PlayerSpellMastery.grant(player, null, 0);
        helper.assertTrue(data.spellbookMasteryExpTen - before == SpellbookData.boostedExpTen(100), "zero grants nothing");
        data.spellbookMasteryExpTen = before;
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void runeSchemeRequiresWorldAuthority(GameTestHelper helper) {
        var world = WorldRuneState.get(helper.getLevel().getServer());
        CompoundTag definition = new CompoundTag();
        definition.putUUID("World", world.worldId());
        definition.putInt("Version", 3);
        definition.putString("Spell", "fire_bolt");
        definition.putInt("Level", 2);
        CompoundTag mirror = definition.copy();
        mirror.putUUID("Id", world.registerScheme(definition));
        helper.assertTrue(RuneScheme.authoritative(world, mirror) != null, "registered mirror is authorized");
        CompoundTag tampered = mirror.copy();
        tampered.putInt("Level", 3);
        helper.assertTrue(RuneScheme.authoritative(world, tampered) == null, "edited mirror is rejected");
        CompoundTag foreign = mirror.copy();
        foreign.putUUID("World", UUID.randomUUID());
        helper.assertTrue(RuneScheme.authoritative(world, foreign) == null, "another world's scheme is rejected");
        helper.assertTrue(RuneScheme.authoritative(world, new CompoundTag()) == null, "missing ids are rejected");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void ctpAndManaTypeLookups(GameTestHelper helper) {
        helper.assertTrue(CTPUtils.getCTPFormIDFromNBT(null) == null, "null tag");
        CompoundTag tag = new CompoundTag();
        helper.assertTrue(CTPUtils.getCTPFormIDFromNBT(tag) == null, "no target form");
        tag.putString(CTPUtils.TARGET_FORM_KEY, "shape-shifter-curse:bat_1");
        helper.assertTrue(new ResourceLocation("shape-shifter-curse", "bat_1").equals(CTPUtils.getCTPFormIDFromNBT(tag)),
                "target form is read");
        tag.putString(CTPUtils.TARGET_FORM_KEY, "Not A Valid Id");
        helper.assertTrue(CTPUtils.getCTPFormIDFromNBT(tag) == null, "malformed id is ignored");
        tag.putInt(CTPUtils.TARGET_FORM_KEY, 3);
        helper.assertTrue(CTPUtils.getCTPFormIDFromNBT(tag) == null, "wrong tag type is ignored");
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_mana_test"));
        String type = ManaUtils.getPlayerManaTypeID(player);
        helper.assertTrue(type != null && !type.isEmpty(), "mana type id is never empty");
        helper.succeed();
    }
}
