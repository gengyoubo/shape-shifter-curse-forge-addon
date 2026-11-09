package github.com.gengyoubo.sscfa.util;

import net.minecraft.util.Tuple;

/** HUD 位置工具占位实现：Forge 侧暂按原始偏移返回。 */
public final class UIPositionUtils {
    private UIPositionUtils() {}

    public static Tuple<Integer, Integer> getCorrectPosition(int posType, int offsetX, int offsetY) {
        return new Tuple<>(offsetX, offsetY);
    }
}
