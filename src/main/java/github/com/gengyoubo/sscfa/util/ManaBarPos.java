package github.com.gengyoubo.sscfa.util;


import java.lang.reflect.Field;

/**
 * 主模组客户端配置?manaBar 位置字段的缓存反射读取??
 * <p>原本?HUD 每??{@code getClass().getField(...)} 反射查找 3 个字段，开销大；
 * 这里?{@link Field} 对象缓存（只查找一次），每帧仅??{@code Field.getInt}??
 * 保留反射是为兼容主模组不同版本的 config（编译期不硬引用其字段）??
 */
public final class ManaBarPos {
    private static Field fType;
    private static Field fX;
    private static Field fY;
    private static boolean resolved = false;
    private static boolean failed = false;

    private ManaBarPos() {
    }

    private static void ensureFields(Object config) {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> c = config.getClass();
            fType = c.getField("manaBarPosType");
            fX = c.getField("manaBarPosOffsetX");
            fY = c.getField("manaBarPosOffsetY");
        } catch (Exception e) {
            failed = true;
        }
    }

    /**
     * 读??manaBar 位??{@code [posType, offsetX, offsetY]}??
     * config 缺失或反射失败时返回传入的默认值?
     */
    public static int[] get(int defType, int defX, int defY) {
        Object config = null;
        if (config == null) {
            return new int[]{defType, defX, defY};
        }
        ensureFields(config);
        if (failed || fType == null) {
            return new int[]{defType, defX, defY};
        }
        try {
            return new int[]{fType.getInt(config), fX.getInt(config), fY.getInt(config)};
        } catch (Exception e) {
            return new int[]{defType, defX, defY};
        }
    }
}
