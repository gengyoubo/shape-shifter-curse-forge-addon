package github.com.gengyoubo.sscfa.client;

import net.minecraft.client.KeyMapping;

/** 键位适配（Forge）：转发到 {@link AddonSkillKeys} 的虚拟技能键。 */
public final class SscAddonKeybindings {
    private SscAddonKeybindings() {}

    public static KeyMapping getPrimaryKey() {
        return AddonSkillKeys.primaryKey();
    }

    public static KeyMapping getSecondaryKey() {
        return AddonSkillKeys.secondaryKey();
    }

    /** 裸物理按键状态：跳过界面/控制拦截，供装死等需要绕过屏蔽的场景使用。 */
    public static boolean isSecondaryRawPressed() {
        net.minecraft.client.KeyMapping key = net.onixary.shapeShifterCurseForge.client.FormKeyMappings.ACTIVE_SKILL_2;
        return (key != null && key.isDown())
                || net.onixary.shapeShifterCurseForge.client.render.MagicHudRenderer.virtualPressed(1);
    }

    public static void register() {
        AddonSkillKeys.init();
    }
}
