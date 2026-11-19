package github.com.gengyoubo.sscfa.ability.fruit;

/** 存档与投掷物使用的稳定取值；AUTO 在命中时按宿主状态解析。 */
public enum FruitSelection {
    AUTO("auto"), VINE("vine"), BITTER("bitter"), ROTTEN("rotten"), SOUR("sour");

    /** 达到最小蓄力才可投掷；30 tick 满蓄力。 */
    public static final int MIN_CHARGE = 10, FULL_CHARGE = 30;

    private final String key;

    FruitSelection(String key) { this.key = key; }

    public String key() { return key; }

    public FruitSelection next() { return values()[(ordinal() + 1) % values().length]; }

    public static FruitSelection fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : AUTO;
    }

    /** 投掷速度：最小蓄力 0.5，满蓄力 1.4。 */
    public static float speed(int ticks) {
        return 0.5f + 0.9f * Math.max(0, Math.min(20, ticks - MIN_CHARGE)) / 20.0f;
    }

    public static boolean canRelease(int ticks) { return ticks >= MIN_CHARGE; }
}
