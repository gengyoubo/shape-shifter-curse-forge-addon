package github.com.gengyoubo.sscfa.gametest;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.util.MixinAudit;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** 双端 mixin 的目标类全部加载一遍：注入写错的 mixin 在这里失败，而不是等游戏里第一次用到才崩。 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MixinAuditGameTests {
    private static final String TEMPLATE = "gametest_floor";

    @GameTest(template = TEMPLATE)
    public static void commonMixinsApply(GameTestHelper helper) {
        List<String> failures = MixinAudit.run("mixins", "server");
        helper.assertTrue(failures.isEmpty(), "mixin 应用失败：" + String.join("；", failures));
        helper.succeed();
    }
}
