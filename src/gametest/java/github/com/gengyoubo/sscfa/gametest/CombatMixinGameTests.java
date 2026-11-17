package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.ability.MancianimaAggroTracker;
import github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements;
import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatMixinGameTests {
    @GameTest(template = "gametest_floor")
    public static void frozenEntityShattersOnlyOnEffectiveDamage(GameTestHelper helper) {
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        target.setNoAi(true);
        helper.assertTrue(FrozenControl.freeze(target, 60), "the target should start frozen");

        target.setInvulnerable(true);
        helper.assertTrue(!target.hurt(target.damageSources().generic(), 1) && FrozenControl.blocked(target),
                "rejected damage must not shatter ice");
        target.setInvulnerable(false);
        helper.assertTrue(target.hurt(target.damageSources().generic(), 1) && !FrozenControl.blocked(target)
                        && target.hasEffect(AddonEffects.FROZEN_RECOVERY.get()),
                "successful damage must shatter ice and apply recovery");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void mancianimaBareHandAttackKeepsDamageAndProvocation(GameTestHelper helper) {
        var world = helper.getLevel();
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "ssca_attack_test"));
        player.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0, 0);
        helper.assertTrue(FormManager.setForm(player, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA),
                "the player should become a Mancianima");
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        float health = target.getHealth();
        float expectedDamage = NpcCombatEnhancements.fixedDamage(player, target, 2.0F);
        player.attack(target);
        helper.assertTrue(Math.abs(health - target.getHealth() - expectedDamage) < 0.01F,
                "the attack must apply fixed damage through SSC Forge's combat hook");
        helper.assertTrue(MancianimaAggroTracker.isAngered(target.getUUID(), player.getUUID()),
                "the successful attack must provoke its target");
        helper.succeed();
    }
}
