package github.com.gengyoubo.sscfa.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;

/** 法术书 HUD 图层：隔离绘制变换，与调用方 finally 中的 popPose 配对。 */
public final class SpellbookHudLayer {
    private SpellbookHudLayer() {}
    public static void push(PoseStack pose) {
        pose.pushPose();
    }
}
