package github.com.gengyoubo.sscfa.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/** HUD 缩放（Forge 简化）：暂不缩放，直接执行绘制。 */
public final class HudScaling {
    private HudScaling() {}
    public static void render(GuiGraphics context, int scaleId, Runnable action) { action.run(); }
}
