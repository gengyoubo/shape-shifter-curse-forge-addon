package github.com.gengyoubo.sscfa.neomagic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static github.com.gengyoubo.sscfa.neomagic.INeoModel.clamp;

/**
 * 新魔法的确定性规则（构成校验、报价、风险）。不含任何 Minecraft 或旧魔法状态。
 *
 * <p>载体搭配、构成校验、控制节点管线、消耗系数都是由 {@link Builder} 填好的规则表；
 * {@link #INSTANCE} 用 {@link #defaults()} 构建。数据驱动的调校可以从 {@link #defaults()} 出发覆盖部分条目再 {@code build()}。</p>
 */
public final class NeoModel implements INeoModel {
    public static final int VERSION = 2;
    public static final NeoModel INSTANCE = defaults().build();

    /**
     * 控制节点对管线的作用：{@code fanOut} 为分支倍数（1 表示不分裂），{@code merges} 表示把分支收拢为 1
     * （收拢时只有原本多于 1 个分支才乘保留率），{@code retention} 为保留率，{@code load} 为负载。
     */
    public record ControlRule(int fanOut, boolean merges, double retention, double load) {
        public ControlRule {
            if (fanOut < 1) throw new IllegalArgumentException("fanOut must be >= 1");
            if (merges && fanOut != 1) throw new IllegalArgumentException("A merging control cannot also split");
        }
        public static ControlRule split(int fanOut, double load) { return new ControlRule(fanOut, false, 1, load); }
        public static ControlRule merge(double retention, double load) { return new ControlRule(1, true, retention, load); }
        public static ControlRule of(double retention, double load) { return new ControlRule(1, false, retention, load); }
    }

    /** 某控制节点最多出现 {@code max} 次，超出报 {@code error}。 */
    public record Limit(Control control, int max, String error) {}

    /** 核心或载体（为 null 表示任意）与某控制节点互斥，违反报 {@code error}。 */
    public record Exclusion(Core core, Carrier carrier, Control control, String error) {
        boolean matches(Blueprint b) {
            return (core == null || b.core() == core) && (carrier == null || b.carrier() == carrier)
                    && b.controls().contains(control);
        }
    }

    private final Map<Core, Set<Carrier>> carriers;
    private final Map<Core, Coefficients> coefficients;
    private final Map<Control, ControlRule> controls;
    private final Map<Core, Set<Core>> conversions;
    private final Map<Control, Double> rangeExtensions;
    private final List<Limit> limits;
    private final List<Exclusion> exclusions;
    private final int maxNameLength, minPrep, maxPrep, capacity, maxBranches;
    private final double minScale, maxScale, minRange, maxRange;

    private NeoModel(Builder b) {
        carriers = freezeSets(b.carriers, Core.class);
        coefficients = Collections.unmodifiableMap(new EnumMap<>(b.coefficients));
        controls = Collections.unmodifiableMap(new EnumMap<>(b.controls));
        conversions = freezeSets(b.conversions, Core.class);
        rangeExtensions = Collections.unmodifiableMap(new EnumMap<>(b.rangeExtensions));
        limits = List.copyOf(b.limits);
        exclusions = List.copyOf(b.exclusions);
        maxNameLength = b.maxNameLength;
        minScale = b.minScale; maxScale = b.maxScale;
        minPrep = b.minPrep; maxPrep = b.maxPrep;
        minRange = b.minRange; maxRange = b.maxRange;
        capacity = b.capacity;
        maxBranches = b.maxBranches;
    }

    private static <K extends Enum<K>, V extends Enum<V>> Map<K, Set<V>> freezeSets(Map<K, EnumSet<V>> source, Class<K> type) {
        Map<K, Set<V>> out = new EnumMap<>(type);
        source.forEach((k, v) -> out.put(k, Collections.unmodifiableSet(EnumSet.copyOf(v))));
        return Collections.unmodifiableMap(out);
    }

    /** 空白构建器：所有规则表都要自己填。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 预填默认规则的构建器。 */
    public static Builder defaults() {
        return builder()
                // 载体搭配
                .carriers(Core.THERMAL, Carrier.CONTACT, Carrier.PROJECTILE, Carrier.AREA)
                .carriers(Core.FROST, Carrier.CONTACT, Carrier.PROJECTILE, Carrier.AREA)
                .carriers(Core.VOID, Carrier.CONTACT, Carrier.PROJECTILE, Carrier.AREA)
                .carriers(Core.LUNAR, Carrier.values())
                .carriers(Core.CURSE, Carrier.CONTACT, Carrier.PROJECTILE, Carrier.LOCK)
                .carriers(Core.SUMMON, Carrier.AREA, Carrier.LOCK)
                .carriers(Core.SPACE, Carrier.SELF, Carrier.LOCK)
                // 消耗系数
                .coefficients(Core.THERMAL, 6, 18)
                .coefficients(Core.FROST, 5, 15)
                .coefficients(Core.LUNAR, 8, 20)
                .coefficients(Core.CURSE, 4, 12)
                .coefficients(Core.SUMMON, 10, 30)
                .coefficients(Core.VOID, 12, 36)
                .coefficients(Core.SPACE, 14, 42)
                // 控制节点管线
                .control(Control.SPLIT, ControlRule.split(3, 2))
                .control(Control.MERGE, ControlRule.merge(.9, 2.5))
                .control(Control.STORE, ControlRule.of(.85, 3))
                .control(Control.CONSTRAIN, ControlRule.of(1, 2))
                .control(Control.BRIDGE, ControlRule.of(1, 2))
                .control(Control.CONVERT, ControlRule.of(.8, 4))
                // 构成校验
                .maxNameLength(32)
                .scale(.5, 4)
                .prep(0, 80)
                .range(1, 32)
                .extendRange(Control.BRIDGE, 64)
                .capacity(6)
                .limit(Control.STORE, 1, "repeat")
                .limit(Control.CONVERT, 1, "repeat")
                .limit(Control.BRIDGE, 1, "repeat")
                .limit(Control.CONSTRAIN, 2, "constraints")
                .conversion(Core.THERMAL, Core.FROST)
                .conversion(Core.FROST, Core.THERMAL)
                .conversion(Core.LUNAR, Core.VOID)
                .maxBranches(6)
                .exclude(Carrier.SELF, Control.SPLIT, "self_split")
                .exclude(Core.SUMMON, Control.MERGE, "summon_merge");
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public boolean supports(Core core, Carrier carrier) {
        return carriers.get(core).contains(carrier);
    }

    @Override
    public List<String> errors(Blueprint b) {
        List<String> e = new ArrayList<>();
        if (b.name().length() > maxNameLength) e.add("name");
        if (!Double.isFinite(b.scale()) || b.scale() < minScale || b.scale() > maxScale) e.add("scale");
        if (b.prep() < minPrep || b.prep() > maxPrep) e.add("prep");
        double rangeCap = maxRange;
        for (var ext : rangeExtensions.entrySet())
            if (b.controls().contains(ext.getKey())) rangeCap = Math.max(rangeCap, ext.getValue());
        if (!Double.isFinite(b.range()) || b.range() < minRange || b.range() > rangeCap) e.add("range");
        if (!supports(b.core(), b.carrier()) || !supports(b.output(), b.carrier())) e.add("carrier");
        if (b.controls().size() > capacity) e.add("capacity");
        for (Limit limit : limits)
            if (b.count(limit.control()) > limit.max()) e.add(limit.error());
        if (b.count(Control.CONVERT) > 0 && !conversions.getOrDefault(b.core(), Set.of()).contains(b.conversion()))
            e.add("conversion");
        int branches = 1;
        for (Control c : b.controls()) {
            ControlRule rule = controls.get(c);
            branches = rule.merges() ? 1 : branches * rule.fanOut();
            if (branches > maxBranches) e.add("branches");
        }
        for (Exclusion exclusion : exclusions)
            if (exclusion.matches(b)) e.add(exclusion.error());
        return List.copyOf(new LinkedHashSet<>(e));
    }

    @Override
    public Plan plan(Blueprint b) {
        int branches = 1;
        double retention = 1, load = 0;
        boolean focused = false;
        for (Control c : b.controls()) {
            ControlRule rule = controls.get(c);
            if (rule.merges()) {
                if (branches > 1) retention *= rule.retention();
                branches = 1;
                focused = true;
            } else {
                retention *= rule.retention();
                if (rule.fanOut() > 1) {
                    branches *= rule.fanOut();
                    focused = false;
                }
            }
            load += rule.load();
        }
        return new Plan(branches, retention, load, focused, b.controls().contains(Control.STORE), b.count(Control.CONSTRAIN));
    }

    @Override
    public Coefficients coefficients(Core core) {
        return coefficients.get(core);
    }

    @Override
    public Quote quote(Blueprint b, double mastery, Coefficients c, Tuning tuning) {
        Plan p = plan(b);
        double path = b.range() / 16;
        if (b.core() == Core.SPACE) path *= path;
        double normal = c.a() * b.scale() + c.b() * b.scale() * b.scale() / (b.prep() / 20.0 + .5) + 1.5 * path + 2 * p.load();
        double floor = b.controls().contains(Control.MERGE) ? .35 : b.controls().contains(Control.SPLIT) ? .4 : .5;
        if (tuning.floor() > 0) floor = tuning.floor();
        normal *= tuning.cost();
        return new Quote(normal, normal * (1 - (1 - floor) * clamp(mastery, 0, 1)), floor, p);
    }

    @Override
    public double failureChance(Blueprint b, double current, double q, Profile f) {
        double excess = Math.max(0, current - f.safety());
        if (excess == 0) return 0;
        if (current > 2 * f.safety()) return 1;
        double scaled = excess / Math.max(1, f.safety()) * 60;
        return clamp(.03 * Math.pow(scaled / 10, 1.3) * Math.pow(q / 20, .7) * f.failureFor(b), 0, 1);
    }

    @Override
    public Risk risk(Blueprint b, double current, double q, Profile f, double failure) {
        double excess = Math.max(0, current - f.safety());
        if (excess == 0) return new Risk(0, 0, 0, 0);
        // 危险范围为 N～2N：按当前有效 N 归一化，保留连续风险曲线与形态修正。
        double scaled = excess / Math.max(1, f.safety()) * 60;
        double constraint = Math.pow(.65, b.count(Control.CONSTRAIN));
        return new Risk(excess, failure,
                clamp(.08 * (scaled / 10) * Math.sqrt(q / 20) * f.offset() * constraint, 0, 1),
                Math.min(12, .18 * Math.pow(scaled, .7) * Math.pow(q / 20, .35)) * constraint);
    }

    @Override
    public int cooldown(Blueprint b) {
        return 60 + 20 * b.tier();
    }

    @Override
    public double masteryNeeded(Blueprint b) {
        return 20.0 * b.tier() * b.tier();
    }

    /** 规则表构建器。{@link #build()} 要求每个核心都有载体表和消耗系数、每个控制节点都有管线规则。 */
    public static final class Builder {
        private final Map<Core, EnumSet<Carrier>> carriers = new EnumMap<>(Core.class);
        private final Map<Core, Coefficients> coefficients = new EnumMap<>(Core.class);
        private final Map<Control, ControlRule> controls = new EnumMap<>(Control.class);
        private final Map<Core, EnumSet<Core>> conversions = new EnumMap<>(Core.class);
        private final Map<Control, Double> rangeExtensions = new EnumMap<>(Control.class);
        private final List<Limit> limits = new ArrayList<>();
        private final List<Exclusion> exclusions = new ArrayList<>();
        private int maxNameLength = Integer.MAX_VALUE, minPrep = 0, maxPrep = Integer.MAX_VALUE;
        private int capacity = Integer.MAX_VALUE, maxBranches = Integer.MAX_VALUE;
        private double minScale = 0, maxScale = Double.MAX_VALUE, minRange = 0, maxRange = Double.MAX_VALUE;

        private Builder() {}

        /** 设置（替换）该核心可用的载体。 */
        public Builder carriers(Core core, Carrier... allowed) {
            EnumSet<Carrier> set = EnumSet.noneOf(Carrier.class);
            Collections.addAll(set, allowed);
            carriers.put(Objects.requireNonNull(core), set);
            return this;
        }

        public Builder coefficients(Core core, double a, double b) {
            coefficients.put(Objects.requireNonNull(core), new Coefficients(a, b));
            return this;
        }

        public Builder control(Control control, ControlRule rule) {
            controls.put(Objects.requireNonNull(control), Objects.requireNonNull(rule));
            return this;
        }

        public Builder maxNameLength(int max) {
            maxNameLength = max;
            return this;
        }

        public Builder scale(double min, double max) {
            minScale = min; maxScale = max;
            return this;
        }

        public Builder prep(int min, int max) {
            minPrep = min; maxPrep = max;
            return this;
        }

        public Builder range(double min, double max) {
            minRange = min; maxRange = max;
            return this;
        }

        /** 蓝图含该控制节点时射程上限放宽到 {@code max}。 */
        public Builder extendRange(Control control, double max) {
            rangeExtensions.put(Objects.requireNonNull(control), max);
            return this;
        }

        /** 控制节点总数上限。 */
        public Builder capacity(int max) {
            capacity = max;
            return this;
        }

        /** 按添加顺序校验，同一错误键只报一次。 */
        public Builder limit(Control control, int max, String error) {
            limits.add(new Limit(Objects.requireNonNull(control), max, Objects.requireNonNull(error)));
            return this;
        }

        /** 允许转化节点把 {@code from} 核心转为 {@code to}。 */
        public Builder conversion(Core from, Core to) {
            conversions.computeIfAbsent(Objects.requireNonNull(from), k -> EnumSet.noneOf(Core.class)).add(Objects.requireNonNull(to));
            return this;
        }

        /** 管线中任意时刻的分支数上限。 */
        public Builder maxBranches(int max) {
            maxBranches = max;
            return this;
        }

        public Builder exclude(Carrier carrier, Control control, String error) {
            exclusions.add(new Exclusion(null, Objects.requireNonNull(carrier), Objects.requireNonNull(control), Objects.requireNonNull(error)));
            return this;
        }

        public Builder exclude(Core core, Control control, String error) {
            exclusions.add(new Exclusion(Objects.requireNonNull(core), null, Objects.requireNonNull(control), Objects.requireNonNull(error)));
            return this;
        }

        public NeoModel build() {
            for (Core core : Core.values()) {
                if (!carriers.containsKey(core)) throw new IllegalStateException("No carriers for core " + core);
                if (!coefficients.containsKey(core)) throw new IllegalStateException("No coefficients for core " + core);
            }
            for (Control control : Control.values())
                if (!controls.containsKey(control)) throw new IllegalStateException("No pipeline rule for control " + control);
            return new NeoModel(this);
        }
    }
}
