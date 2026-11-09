package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.power.AddonPowerTypes;
import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.CustomPowerTypeService;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

import java.util.UUID;

/**
 * 吸血蝙蝠的两个技能：音波（副技能键）与幽雾化形（主技能键），按键走 SSC Forge 的按键入口。
 * 假玩家不会被服务器 tick，所以每个 tick 手动驱动一次 SSC Forge 的自定义能力 tick。
 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatSkillGameTests {
    private static final ResourceLocation BAT_DESMODUS = new ResourceLocation("my_addon", "bat_desmodus");
    private static final String SONIC_WAVE = "my_addon:form_bat_desmodus_sonic_wave";
    private static final String MIST_FORM = "my_addon:form_bat_desmodus_mist_form";
    private static final String TEMPLATE = "gametest_floor";

    private BatSkillGameTests() {}

    /** 音波：前方僵尸受伤并获得反胃、失明；冷却 160；血渴 = 技能命中 12 + 玩家攻击命中 8。 */
    @GameTest(template = TEMPLATE)
    public static void sonicWaveHitsTargetAhead(GameTestHelper helper) {
        ServerPlayer bat = batFacingSouth(helper);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        press(bat, AddonPowerTypes.SP_SECONDARY_KEY);
        helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the sonic wave should damage the zombie ahead");
        helper.assertTrue(target.hasEffect(MobEffects.CONFUSION) && target.hasEffect(MobEffects.BLINDNESS),
                "a non-player target should get nausea and blindness");
        int remaining = SkillCooldowns.remaining(bat, SONIC_WAVE);
        helper.assertTrue(remaining == 160, "sonic wave cooldown should be 160, was " + remaining);
        int blood = ResourceBars.get(bat, BarKeys.BAT_BLOOD);
        helper.assertTrue(blood == 20, "one skill hit adds 12 plus 8 for the player-attack hit, was " + blood);
        CustomPowerTypeService.forget(bat);
        helper.succeed();
    }

    /** 幽雾化形：化雾缩小碰撞箱（结束后回到形态自身的尺寸）；满 1 秒再按开始蓄力，1 秒后爆破、解除雾化并从结束时起算 200 冷却。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void mistFormBurstCycle(GameTestHelper helper) {
        ServerPlayer bat = batFacingSouth(helper);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        helper.onEachTick(() -> CustomPowerTypeService.tick(bat));
        float formWidth = bat.getBbWidth();
        helper.startSequence()
                .thenExecute(() -> {
                    press(bat, AddonPowerTypes.SP_PRIMARY_KEY);
                    helper.assertTrue(bat.hasEffect(AddonEffects.MIST_FORM.get()) && bat.hasEffect(MobEffects.INVISIBILITY),
                            "pressing the primary key should turn the bat into mist");
                    helper.assertTrue(Math.abs(bat.getBbWidth() - 0.2f) < 1e-4, "mist hitbox should be 0.2 wide, was " + bat.getBbWidth());
                    helper.assertTrue(SkillCooldowns.remaining(bat, MIST_FORM) == 0, "on_end cooldown must not start while in mist");
                    press(bat, AddonPowerTypes.SP_PRIMARY_KEY);
                    helper.assertTrue(!bat.hasEffect(AddonEffects.MIST_CHARGING.get()), "a burst needs one second in mist first");
                })
                .thenIdle(21)
                .thenExecute(() -> {
                    press(bat, AddonPowerTypes.SP_PRIMARY_KEY);
                    helper.assertTrue(bat.hasEffect(AddonEffects.MIST_CHARGING.get()), "after one second the key should start the burst charge");
                })
                .thenIdle(22)
                .thenExecute(() -> {
                    helper.assertTrue(!bat.hasEffect(AddonEffects.MIST_FORM.get()), "the burst should end the mist");
                    helper.assertTrue(Math.abs(bat.getBbWidth() - formWidth) < 1e-4,
                            "hitbox should return to the form's own width " + formWidth + ", was " + bat.getBbWidth());
                    helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the burst should damage the nearby zombie");
                    int remaining = SkillCooldowns.remaining(bat, MIST_FORM);
                    helper.assertTrue(remaining > 190 && remaining <= 200, "cooldown should start at 200 when the mist ends, was " + remaining);
                    CustomPowerTypeService.forget(bat);
                })
                .thenSucceed();
    }

    /** 面朝南（+Z）、略微低头对准前方两格的僵尸中心。 */
    private static ServerPlayer batFacingSouth(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca-gametest"));
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1.0, 0.5)));
        player.setOnGround(true);
        player.setYRot(0f);
        player.setXRot(38f);
        FormManager.setForm(player, BAT_DESMODUS);
        helper.assertTrue(BAT_DESMODUS.equals(FormManager.current(player).id()), "fake player should now be bat_desmodus");
        BalanceHandshake.complete(player);
        CustomPowerTypeService.tick(player);
        return player;
    }

    private static void press(ServerPlayer player, String key) {
        FormActivePowerService.setKeyPressed(player, key, true);
        FormActivePowerService.setKeyPressed(player, key, false);
    }
}
