package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.ability.fruit.FruitLayerLedger;
import github.com.gengyoubo.sscfa.ability.fruit.FruitSelection;
import github.com.gengyoubo.sscfa.ability.fruit.ParasiticFruitAbility;
import github.com.gengyoubo.sscfa.ability.fruit.ParasiticFruitLayers;
import github.com.gengyoubo.sscfa.power.ParasiticFruitSeedPower;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/** 寄生果实选择：账本、双击切换、跨形态重置、种植与结果、效果叠加层的可撤销性。 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParasiticFruitGameTests {
    private ParasiticFruitGameTests() {}

    private static ServerPlayer fruitBat(GameTestHelper helper, String name) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        helper.assertTrue(FormManager.setForm(player, FormIdentifiers.BAT_PARASITIC_FRUIT), "form change");
        BalanceHandshake.complete(player);
        helper.assertTrue(ParasiticFruitSeedPower.active(player) != null, "fruit bat has the seed power");
        return player;
    }

    @GameTest(template = "gametest_floor")
    public static void ledgerKeepsThreeLayersFifo(GameTestHelper helper) {
        var ledger = new FruitLayerLedger();
        UUID host = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        ledger.add(host, a, FruitSelection.VINE, 0, 100, 20, 3);
        ledger.add(host, b, FruitSelection.SOUR, 1, 100, 20, 3);
        ledger.add(host, a, FruitSelection.BITTER, 2, 100, 20, 3);
        ledger.add(host, b, FruitSelection.ROTTEN, 3, 100, 20, 3);
        var layers = ledger.layers(host);
        helper.assertTrue(layers.size() == 3, "three layers total per host");
        helper.assertTrue(layers.get(0).fruit() == FruitSelection.SOUR, "the oldest layer was replaced");
        helper.assertTrue(!layers.get(0).rooted(20) && layers.get(0).rooted(21), "rooting delay");
        UUID[] hosts = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        var owned = new FruitLayerLedger();
        for (int i = 0; i < hosts.length; i++) owned.add(hosts[i], a, FruitSelection.VINE, i, 100, 0, 3);
        helper.assertTrue(owned.layers(hosts[0]).isEmpty() && owned.layers(hosts[3]).size() == 1,
                "a caster's fourth host evicts its oldest host");
        helper.assertTrue(ledger.prune(200, layer -> true).contains(host) && ledger.layers(host).isEmpty(), "expiry");
        boolean rejected = false;
        try {
            ledger.add(host, a, FruitSelection.AUTO, 0, 1, 0, 1);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "AUTO must be resolved before planting");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void doubleTapCyclesAndFormChangeResets(GameTestHelper helper) {
        var player = fruitBat(helper, "ssca_fruit_cycle");
        var power = ParasiticFruitSeedPower.active(player).id();
        ParasiticFruitAbility.setSelection(player, FruitSelection.AUTO);
        ParasiticFruitAbility.pressed(player, power);
        ParasiticFruitAbility.released(player);
        helper.assertTrue(ParasiticFruitAbility.selection(player) == FruitSelection.AUTO, "a single tap does not cycle");
        ParasiticFruitAbility.pressed(player, power);
        ParasiticFruitAbility.released(player);
        helper.assertTrue(ParasiticFruitAbility.selection(player) == FruitSelection.VINE, "a double tap cycles to vine");
        helper.assertTrue(!ParasiticFruitAbility.charging(player), "short taps never leave a charge behind");
        ParasiticFruitAbility.tick(player);
        FormManager.setForm(player, FormIdentifiers.BAT_DESMODUS);
        ParasiticFruitAbility.tick(player);
        helper.assertTrue(ParasiticFruitAbility.selection(player) == FruitSelection.AUTO, "changing form resets the selection");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor", timeoutTicks = 120)
    public static void plantedFruitPulsesAndIsReversible(GameTestHelper helper) {
        var player = fruitBat(helper, "ssca_fruit_plant");
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
        ParasiticFruitSeedPower.plantSeedFrom(player, zombie, 200, FruitSelection.VINE);
        ParasiticFruitSeedPower.plantSeedFrom(player, zombie, 200, FruitSelection.VINE);
        helper.assertTrue(ParasiticFruitLayers.layers(helper.getLevel(), zombie.getUUID()).size() == 2, "two layers planted");
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    var slow = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    helper.assertTrue(slow != null && slow.getAmplifier() >= 1,
                            "two vine layers apply Slowness II after rooting, got " + slow);
                    ParasiticFruitLayers.removeOwner(player);
                })
                .thenExecuteAfter(2, () -> {
                    var slow = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    helper.assertTrue(slow != null && slow.getAmplifier() == 0 && slow.getDuration() > 300,
                            "removing the fruit restores the external Slowness I baseline, got " + slow);
                    helper.assertTrue(ParasiticFruitLayers.layers(helper.getLevel(), zombie.getUUID()).isEmpty(), "layers cleared");
                })
                .thenSucceed();
    }
}
