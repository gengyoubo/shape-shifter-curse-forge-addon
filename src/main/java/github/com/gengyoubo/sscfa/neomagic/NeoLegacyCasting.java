package github.com.gengyoubo.sscfa.neomagic;

/** 旧版施法桥（Forge 占位）：保留法术代码使用的查询入口，实际魔法崩解运行时待移植。 */
public final class NeoLegacyCasting {
    private NeoLegacyCasting() {}
    public static boolean reversed() { return false; }
    public static boolean shifted() { return false; }
    public static boolean wrongTarget() { return false; }
    public static boolean missed() { return false; }
    public static void miss() {}
    public static int actualCost(int cost) { return cost; }
    public static float power(float damage) { return damage; }
}
