package github.com.gengyoubo.sscfa.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;

/** 法术书 HUD 图层（Forge 简化）：保留 push 入口，不做额外图层排序。 */
public final class SpellbookHudLayer {
    private SpellbookHudLayer() {}
    public static void push(PoseStack pose) {}
}
