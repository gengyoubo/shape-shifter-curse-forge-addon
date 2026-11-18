package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormAbilityEvents;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FormPassiveGameTests {
    @GameTest(template = "gametest_floor")
    public static void windSpiritLandingIsCanceledBeforeHurtFeedback(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca-fall"));
        FormManager.setForm(player, new ResourceLocation("my_addon", "ocelot_wind_spirit"));
        var fall = new LivingFallEvent(player, 6.0f, 1.0f);
        MinecraftForge.EVENT_BUS.post(fall);
        helper.assertTrue(fall.isCanceled(), "fall immunity must cancel the landing damage path before sound and hurt feedback");
        helper.assertTrue(!player.causeFallDamage(6, 1, player.damageSources().fall()), "immune landing must not be reported as a hit");
        FormManager.setForm(player, new ResourceLocation("shape-shifter-curse", "original_shifter"));
        var normalFall = new LivingFallEvent(player, 6, 1);
        MinecraftForge.EVENT_BUS.post(normalFall);
        helper.assertTrue(!normalFall.isCanceled(), "ordinary players must keep fall damage");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void spiderNightVisionDoesNotCreateFlickeringPotion(GameTestHelper helper) {
        for (String form : new String[]{"my_addon:spider_salticidae", "my_addon:spider_moon_weaver", "shape-shifter-curse:spider_3"}) {
            var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca-night"));
            FormManager.setForm(player, new ResourceLocation(form));
            player.tickCount = 40;
            FormAbilityEvents.tick(new LivingEvent.LivingTickEvent(player));
            helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), form + " must use the power's render strength instead of an 80 tick potion");
        }
        helper.succeed();
    }
}
