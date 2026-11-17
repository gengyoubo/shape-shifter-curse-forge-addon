package github.com.gengyoubo.sscfa.power;

/**
 * Fabric-era entry point that skill code still calls when an action fails.
 * On Forge the skill power is {@link FailAwareActiveSelf}; this forwards to it so the
 * cast settles on {@code fail_cooldown} instead of counting as a success.
 */
public final class FailAwareActiveSelfPower {
    private FailAwareActiveSelfPower() {}

    public static void markFail() {
        FailAwareActiveSelf.markFail();
    }
}
