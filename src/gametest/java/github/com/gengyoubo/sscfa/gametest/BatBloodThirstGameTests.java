package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.UUID;

/**
 * 吸血蝙蝠血渴值：普攻命中累积、内部冷却、高阶段吸血，走真实的伤害流程（Forge 伤害事件）与 SSC Forge 资源存储。
 * 运行：{@code ./gradlew runGameTestServer}。
 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatBloodThirstGameTests {
    private static final ResourceLocation BAT_DESMODUS = new ResourceLocation("my_addon", "bat_desmodus");
    private static final String TEMPLATE = "gametest_floor";

    private BatBloodThirstGameTests() {}

    /** 普攻命中 +8；0.3 秒内部冷却内再命中不叠加。 */
    @GameTest(template = TEMPLATE)
    public static void meleeHitGainsBloodWithInternalCooldown(GameTestHelper helper) {
        ServerPlayer bat = player(helper, BAT_DESMODUS);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        target.hurt(bat.damageSources().playerAttack(bat), 2.0f);
        int blood = ResourceBars.get(bat, BarKeys.BAT_BLOOD);
        helper.assertTrue(blood == 8, "a melee hit should add 8 blood thirst, was " + blood);
        target.invulnerableTime = 0;
        target.hurt(bat.damageSources().playerAttack(bat), 2.0f);
        helper.assertTrue(ResourceBars.get(bat, BarKeys.BAT_BLOOD) == 8, "a second hit inside 0.3s must not add more");
        helper.succeed();
    }

    /** 75-100 阶段：按原始伤害的 52.5% 吸血。 */
    @GameTest(template = TEMPLATE)
    public static void highStageLifesteal(GameTestHelper helper) {
        ServerPlayer bat = player(helper, BAT_DESMODUS);
        ResourceBars.set(bat, BarKeys.BAT_BLOOD, 80);
        helper.assertTrue(ResourceBars.get(bat, BarKeys.BAT_BLOOD) == 80, "blood thirst should be settable to 80");
        bat.setHealth(10.0f);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        target.hurt(bat.damageSources().playerAttack(bat), 4.0f);
        float healed = bat.getHealth() - 10.0f;
        helper.assertTrue(Math.abs(healed - 4.0f * 0.525f) < 1e-4, "stage 3 lifesteal should heal 2.1, healed " + healed);
        helper.succeed();
    }

    /** 不是吸血蝙蝠：命中不累积血渴。 */
    @GameTest(template = TEMPLATE)
    public static void otherFormsDoNotGainBlood(GameTestHelper helper) {
        ServerPlayer other = player(helper, null);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        target.hurt(other.damageSources().playerAttack(other), 2.0f);
        helper.assertTrue(!ResourceBars.has(other, BarKeys.BAT_BLOOD) && ResourceBars.get(other, BarKeys.BAT_BLOOD) == 0,
                "a non-bat form has no blood thirst bar");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, ResourceLocation form) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca-gametest"));
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1.0, 0.5)));
        if (form != null) {
            FormManager.setForm(player, form);
            helper.assertTrue(form.equals(FormManager.current(player).id()), "fake player should now be " + form);
        }
        return player;
    }
}
