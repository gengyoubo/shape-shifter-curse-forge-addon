package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.entity.SpellFireBoltEntity;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScrollCastingGameTests {
    @GameTest(template = "gametest_floor")
    public static void rightClickCastsScrollWithLoadedSpellConfig(GameTestHelper helper) {
        var world = helper.getLevel();
        helper.assertTrue(SpellRegistry.all().stream().allMatch(spell -> spell.getConfig().manaCostConfigured),
                "world startup must publish the spell configurations");

        var player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "ssca_scroll_test"));
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 3, 1.5)));
        BalanceHandshake.complete(player);
        var scroll = ScrollData.create("fire_bolt");
        int uses = ScrollData.getUses(scroll);
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll);

        var result = player.gameMode.useItem(player, world, scroll, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "right-click should start a solo cast");
        player.releaseUsingItem();
        helper.succeedWhen(() -> {
            helper.assertTrue(ScrollData.getUses(scroll) == uses - 1, "a cast must spend one use");
            helper.assertTrue(ScrollData.getCooldownEnd(scroll) > world.getGameTime(), "a cast must set a cooldown");
            helper.assertTrue(!world.getEntitiesOfClass(SpellFireBoltEntity.class, player.getBoundingBox().inflate(12),
                    bolt -> bolt.getOwner() == player).isEmpty(), "the cast must launch a fire bolt");
        });
    }
}
