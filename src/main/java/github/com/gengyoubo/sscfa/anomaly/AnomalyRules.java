package github.com.gengyoubo.sscfa.anomaly;

/** 异常规则占位。 */
public final class AnomalyRules {
    private AnomalyRules() {}

    public static int syntheticAmount(String key, int level) {
        return level;
    }
}
