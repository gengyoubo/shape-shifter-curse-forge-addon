package github.com.gengyoubo.sscfa.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import net.onixary.shapeShifterCurseForge.client.render.HudPosition;

/** HUD 位置工具：与 SSC Forge 的九宫格定位保持一致。 */
public final class UIPositionUtils {
    private UIPositionUtils() {}

    public static Tuple<Integer, Integer> getCorrectPosition(int posType, int offsetX, int offsetY) {
        var window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        return new Tuple<>(HudPosition.x(width, posType, offsetX), HudPosition.y(height, posType, offsetY));
    }
}
