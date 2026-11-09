package github.com.gengyoubo.sscfa.item;

/** 客户端冷却查询占位实现：待客户端 HUD 子系统移植后替换。 */
public final class LifesavingCatTailClient {
    private LifesavingCatTailClient() {}

    public static int getCooldownRemaining(Object item, int max) {
        return 0;
    }

    public static boolean isOnCooldown(Object item) {
        return false;
    }
}
