package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.power.AddonPowerTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.CustomPowerTypeService;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

import java.util.UUID;

/**
 * 技能键 → SSC Forge 自定义能力类型 → {@code fail_aware_active_self} → 统一冷却 的整条链路，用假玩家跑。
 * 按键走 SSC Forge 处理按键包的同一入口 {@link FormActivePowerService#setKeyPressed}。
 *
 * <p>被测技能：SP 美西螈的水球（{@code my_addon:form_axolotl_sp_water_ball}，副技能键，冷却 60 tick，
 * 条件：站在方块上、潜行、未装死、有空气）。运行：{@code ./gradlew runGameTestServer}。</p>
 *
 * <p>用 Forge 的 {@link FakePlayerFactory} 而不是原版 {@code makeMockServerPlayerInLevel}：后者在 Forge 1.20.1
 * 上会因模拟连接没有网络管道而在 {@code placeNewPlayer} 里报空指针。</p>
 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SkillKeyGameTests {
    private static final ResourceLocation AXOLOTL_SP = new ResourceLocation("my_addon", "axolotl_sp");
    private static final String WATER_BALL = "my_addon:form_axolotl_sp_water_ball";
    private static final String PLAY_DEAD = "my_addon:form_axolotl_sp_play_dead_activate";
    private static final String TEMPLATE = "gametest_floor";

    private SkillKeyGameTests() {}

    /** 满足条件时按键：技能执行并按 JSON 冷却；冷却中再按不重置。 */
    @GameTest(template = TEMPLATE)
    public static void skillKeyStartsJsonCooldown(GameTestHelper helper) {
        ServerPlayer player = axolotlOnFloor(helper);
        try {
            player.setShiftKeyDown(true);
            press(player);
            int remaining = SkillCooldowns.remaining(player, WATER_BALL);
            helper.assertTrue(remaining == 60, "water ball cooldown should be 60 ticks from its JSON, was " + remaining);

            press(player);
            helper.assertTrue(SkillCooldowns.remaining(player, WATER_BALL) == 60, "pressing again during cooldown must not restart it");
            helper.succeed();
        } finally {
            CustomPowerTypeService.forget(player);
        }
    }

    /** 不满足 power 的 condition（没潜行）时按键：不施放、不进冷却。 */
    @GameTest(template = TEMPLATE)
    public static void skillKeyRespectsPowerCondition(GameTestHelper helper) {
        ServerPlayer player = axolotlOnFloor(helper);
        try {
            player.setShiftKeyDown(false);
            press(player);
            int remaining = SkillCooldowns.remaining(player, WATER_BALL);
            helper.assertTrue(remaining == 0, "an unmet condition must not cast or start a cooldown, was " + remaining);
            helper.succeed();
        } finally {
            CustomPowerTypeService.forget(player);
        }
    }

    /**
     * 嵌在 apoli:multiple 里的技能（装死 {@code form_axolotl_sp_play_dead} 的子能力 {@code activate}，同为副技能键、
     * 条件为不潜行、冷却 620）：按 Apoli 子能力 ID {@code ..._play_dead_activate} 结算冷却。
     */
    @GameTest(template = TEMPLATE)
    public static void nestedSkillUsesChildId(GameTestHelper helper) {
        ServerPlayer player = axolotlOnFloor(helper);
        try {
            player.setShiftKeyDown(false);
            press(player);
            int remaining = SkillCooldowns.remaining(player, PLAY_DEAD);
            helper.assertTrue(remaining == 620, "play dead should cool down under its child id for 620 ticks, was " + remaining);
            helper.assertTrue(SkillCooldowns.remaining(player, "my_addon:form_axolotl_sp_play_dead") == 0,
                    "the parent id must not carry the child's cooldown");
            helper.succeed();
        } finally {
            CustomPowerTypeService.forget(player);
        }
    }

    /** 别的形态没有这个技能：同一个键不触发它。 */
    @GameTest(template = TEMPLATE)
    public static void skillKeyIgnoredWithoutPower(GameTestHelper helper) {
        ServerPlayer player = playerOnFloor(helper);
        try {
            player.setShiftKeyDown(true);
            CustomPowerTypeService.tick(player);
            press(player);
            helper.assertTrue(SkillCooldowns.remaining(player, WATER_BALL) == 0, "a form without the power must not use it");
            helper.succeed();
        } finally {
            CustomPowerTypeService.forget(player);
        }
    }

    private static ServerPlayer playerOnFloor(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca-gametest"));
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        player.setOnGround(true);
        return player;
    }

    private static ServerPlayer axolotlOnFloor(GameTestHelper helper) {
        ServerPlayer player = playerOnFloor(helper);
        FormManager.setForm(player, AXOLOTL_SP);
        helper.assertTrue(AXOLOTL_SP.equals(FormManager.current(player).id()), "fake player should now be SP axolotl");
        // 一次能力 tick：让 SSC Forge 把该形态的自定义类型能力登记为已激活（onAdded）
        CustomPowerTypeService.tick(player);
        return player;
    }

    private static void press(ServerPlayer player) {
        FormActivePowerService.setKeyPressed(player, AddonPowerTypes.SP_SECONDARY_KEY, true);
        FormActivePowerService.setKeyPressed(player, AddonPowerTypes.SP_SECONDARY_KEY, false);
    }
}
