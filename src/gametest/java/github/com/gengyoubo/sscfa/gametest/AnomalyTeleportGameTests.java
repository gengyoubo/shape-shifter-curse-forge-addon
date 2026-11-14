package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.anomaly.AnomalyEffects;
import github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.spell.spells.PocketSpaceSpell;
import github.com.gengyoubo.sscfa.spell.spells.SpaceBlinkSpell;
import github.com.gengyoubo.sscfa.spell.spells.SpaceRecallSpell;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnomalyTeleportGameTests {
    @GameTest(template = "gametest_floor")
    public static void spaceBindBlocksActiveTeleportsUntilRemoved(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_space_test"));
        var blink = new SpaceBlinkSpell();
        try {
            helper.assertTrue(!AnomalyTeleportGuard.reject(player) && blink.canCast(player), "unbound player should be allowed");
            player.addEffect(new MobEffectInstance(AnomalyEffects.SPACE_BIND.get(), 200));
            helper.assertTrue(AnomalyEffects.teleportBlocked(player), "space bind should update the cached state");
            helper.assertTrue(AnomalyTeleportGuard.reject(player) && !blink.canCast(player), "space bind should block active teleport");
            player.removeEffect(AnomalyEffects.SPACE_BIND.get());
            helper.assertTrue(!AnomalyTeleportGuard.reject(player) && blink.canCast(player), "removing space bind should restore teleport");
        } finally {
            player.removeEffect(AnomalyEffects.SPACE_BIND.get());
        }
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void onlyOwnedTeleportSpellsAreClassified(GameTestHelper helper) {
        helper.assertTrue(AnomalyTeleportGuard.isTeleportSpell(new SpaceBlinkSpell()), "blink should be guarded");
        helper.assertTrue(AnomalyTeleportGuard.isTeleportSpell(new SpaceRecallSpell()), "recall should be guarded");
        helper.assertTrue(AnomalyTeleportGuard.isTeleportSpell(new PocketSpaceSpell()), "pocket space should be guarded");
        Spell otherNamespace = new Spell(new ResourceLocation("other", "space_blink"), SpellRarity.WHITE) {
            @Override public void cast(ServerPlayer caster, float power, boolean solo) {}
        };
        helper.assertTrue(!AnomalyTeleportGuard.isTeleportSpell(otherNamespace), "other namespaces must not be intercepted");
        helper.assertTrue(!AnomalyTeleportGuard.isTeleportSpell(null), "null is not a teleport spell");
        helper.succeed();
    }
}
