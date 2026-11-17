package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import github.com.gengyoubo.sscfa.util.ClientResourceCache;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpellTabAndResourceHudGameTests {
    @GameTest(template = "gametest_floor")
    public static void spellTabContainsBoundSpellsAndFormations(GameTestHelper helper) {
        var world = helper.getLevel();
        var tab = AddonItems.SPELL_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(world.enabledFeatures(), false, world.registryAccess()));
        var entries = tab.getDisplayItems();
        var scrolls = entries.stream().filter(stack -> stack.is(AddonItems.MAGIC_SCROLL.get())).toList();
        helper.assertTrue(!scrolls.isEmpty() && scrolls.stream().allMatch(ScrollData::isScroll),
                "the spells tab should contain bound scrolls");
        for (var spell : SpellRegistry.all()) {
            for (int level = 1; level <= spell.getMaxLevel(); level++) {
                helper.assertTrue(tab.contains(ScrollData.create(spell.getId().getPath(), level)),
                        "missing creative scroll " + spell.getId() + " at level " + level);
            }
        }
        helper.assertTrue(tab.contains(FormationData.create(FormationElement.FIRE, FormationData.MAX_FORMATION_LEVEL)),
                "the spells tab should contain leveled elemental formations");
        helper.assertTrue(tab.contains(FormationData.create(FormationElement.UNIVERSAL, 1, FormationData.VARIANT_RECOVERY)),
                "the spells tab should contain the Recovery formation variant");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void resourceBarTracksPowerAcrossFormChanges(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_hud_test"));
        var seed = FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY;
        var nineLives = FormIdentifiers.OCELOT_NOVA_NINE_LIVES;
        helper.assertTrue(PowerUtils.getResourceMax(seed) > 1 && !ClientResourceCache.has(player, seed),
                "a defined resource must stay hidden when the player lacks the power");

        FormManager.setForm(player, FormIdentifiers.BAT_PARASITIC_FRUIT);
        PowerUtils.setResourceValue(player, seed, 0);
        helper.assertTrue(ClientResourceCache.has(player, seed) && !ClientResourceCache.has(player, nineLives),
                "a fruit bat should show its seed bar even when empty");

        FormManager.setForm(player, FormIdentifiers.OCELOT_NOVA);
        helper.assertTrue(!ClientResourceCache.has(player, seed) && ClientResourceCache.has(player, nineLives),
                "switching to Nova should replace the seed bar with Nine Lives");
        helper.succeed();
    }
}
