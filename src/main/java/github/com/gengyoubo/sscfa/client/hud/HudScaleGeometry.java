package github.com.gengyoubo.sscfa.client.hud;

/** HUD 缩放几何（Forge 简化）。 */
public final class HudScaleGeometry {
    private HudScaleGeometry() {}
    public record Box(int left, int top, int width, int height) {}
}
