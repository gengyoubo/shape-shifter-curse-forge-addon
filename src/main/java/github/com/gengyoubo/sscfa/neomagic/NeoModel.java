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
 * 新魔法的确定性规则（构成校验、报价、风险、施法会话节奏与偏移后果、冷却）。不含任何 Minecraft 或旧魔法状态。
 *
 * <p>所有规则表和曲线常数都由 {@link Builder} 填写；{@link #INSTANCE} 用 {@link #defaults()} 构建。
 * 数据驱动的调校可以从 {@link #defaults()} 出发覆盖部分条目再 {@code build()}。</p>
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

    /** 反转概率 = min({@code cap}, 超载 / {@code divisor})，超载口径同 {@link Builder#riskScale}。 */
    public record Reversal(double divisor, double cap) {}

    /** 核心或载体（为 null 表示任意）与某控制节点互斥，违反报 {@code error}。 */
    public record Exclusion(Core core, Carrier carrier, Control control, String error) {
        boolean matches(Blueprint b) {
            return (core == null || b.core() == core) && (carrier == null || b.carrier() == carrier)
                    && b.controls().contains(control);
        }
    }

    /**
     * 风险曲线：{@code base × 超载^overloadExponent × (报价/报价单位)^quoteExponent}。
     * 超载的口径见各曲线的构建器方法。
     */
    public record Curve(double base, double overloadExponent, double quoteExponent) {}

    // ===== 构成与管线 =====
    private final Map<Core, Set<Carrier>> carriers;
    private final Map<Core, Coefficients> coefficients;
    private final Map<Control, ControlRule> controls;
    private final Map<Core, Set<Core>> conversions;
    private final Map<Control, Double> rangeExtensions;
    private final List<Limit> limits;
    private final List<Exclusion> exclusions;
    private final int maxNameLength, minPrep, maxPrep, capacity, maxBranches;
    private final double minScale, maxScale, minRange, maxRange;
    // ===== 报价 =====
    private final double pathUnit, pathWeight, loadWeight, prepTicks, prepOffset, defaultFloor;
    private final Set<Core> squaredPath;
    private final Map<Control, Double> masteryFloors;
    // ===== 风险 =====
    private final double overloadScale, overloadUnit, quoteUnit, certainFailure, maxAngle;
    private final Curve failureCurve, offsetCurve, angleCurve;
    private final Map<Control, Double> riskDamping;
    // ===== 施法会话 =====
    private final int checkpointInterval, paymentInterval;
    private final Map<Core, Double> attenuations;
    private final Map<Core, Reversal> reversals;
    private final Map<Carrier, Double> wrongTargets;
    // ===== 冷却与熟练度 =====
    private final int cooldownBase, cooldownPerTier;
    private final double masteryPerTierSquared;

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

        pathUnit = b.pathUnit; pathWeight = b.pathWeight;
        squaredPath = Collections.unmodifiableSet(b.squaredPath.isEmpty() ? EnumSet.noneOf(Core.class) : EnumSet.copyOf(b.squaredPath));
        loadWeight = b.loadWeight;
        prepTicks = b.prepTicks; prepOffset = b.prepOffset;
        defaultFloor = b.defaultFloor;
        masteryFloors = Collections.unmodifiableMap(new EnumMap<>(b.masteryFloors));

        overloadScale = b.overloadScale; overloadUnit = b.overloadUnit; quoteUnit = b.quoteUnit;
        certainFailure = b.certainFailure;
        failureCurve = b.failureCurve; offsetCurve = b.offsetCurve; angleCurve = b.angleCurve;
        maxAngle = b.maxAngle;
        riskDamping = Collections.unmodifiableMap(new EnumMap<>(b.riskDamping));

        checkpointInterval = b.checkpointInterval; paymentInterval = b.paymentInterval;
        attenuations = Collections.unmodifiableMap(new EnumMap<>(b.attenuations));
        reversals = Collections.unmodifiableMap(new EnumMap<>(b.reversals));
        wrongTargets = Collections.unmodifiableMap(new EnumMap<>(b.wrongTargets));

        cooldownBase = b.cooldownBase; cooldownPerTier = b.cooldownPerTier;
        masteryPerTierSquared = b.masteryPerTierSquared;
    }

    private static <K extends Enum<K>, V extends Enum<V>> Map<K, Set<V>> freezeSets(Map<K, EnumSet<V>> source, Class<K> type) {
        Map<K, Set<V>> out = new EnumMap<>(type);
        source.forEach((k, v) -> out.put(k, Collections.unmodifiableSet(EnumSet.copyOf(v))));
        return Collections.unmodifiableMap(out);
    }

    /** 空白构建器：所有规则表和曲线常数都要自己填。 */
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
                .exclude(Core.SUMMON, Control.MERGE, "summon_merge")
                // 报价
                .pathCost(16, 1.5)
                .squaredPath(Core.SPACE)
                .loadCost(2)
                .prepRelief(20, .5)
                .masteryFloor(.5)
                .masteryFloor(Control.MERGE, .35)
                .masteryFloor(Control.SPLIT, .4)
                // 风险
                .riskScale(60, 10, 20)
                .certainFailure(2)
                .failureCurve(new Curve(.03, 1.3, .7))
                .offsetCurve(new Curve(.08, 1, .5))
                .angleCurve(new Curve(.18, .7, .35), 12)
                .riskDamping(Control.CONSTRAIN, .65)
                // 施法会话：节奏与偏移后果
                .checkpointInterval(10)
                .paymentInterval(2)
                .attenuation(Core.LUNAR, .75)
                .attenuation(Core.SUMMON, .7)
                .attenuation(Core.SPACE, .7)
                .reversal(Core.LUNAR, new Reversal(80, .8))
                .wrongTarget(Carrier.LOCK, .4)
                // 冷却与熟练度
                .cooldown(60, 20)
                .mastery(20);
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
        double path = b.range() / pathUnit;
        if (squaredPath.contains(b.core())) path *= path;
        double normal = c.a() * b.scale() + c.b() * b.scale() * b.scale() / (b.prep() / prepTicks + prepOffset)
                + pathWeight * path + loadWeight * p.load();
        double floor = defaultFloor;
        for (var entry : masteryFloors.entrySet())
            if (b.controls().contains(entry.getKey())) floor = Math.min(floor, entry.getValue());
        if (tuning.floor() > 0) floor = tuning.floor();
        normal *= tuning.cost();
        return new Quote(normal, normal * (1 - (1 - floor) * clamp(mastery, 0, 1)), floor, p);
    }

    /** 超载量归一化：超出安全阈值的部分按当前有效 N 折算，危险范围为 N～certainFailure×N。 */
    private double overload(double excess, Profile f) {
        return excess / Math.max(1, f.safety()) * overloadScale;
    }

    @Override
    public double failureChance(Blueprint b, double current, double q, Profile f) {
        double excess = Math.max(0, current - f.safety());
        if (excess == 0) return 0;
        if (current > certainFailure * f.safety()) return 1;
        double scaled = overload(excess, f);
        return clamp(failureCurve.base() * pow(scaled / overloadUnit, failureCurve.overloadExponent())
                * pow(q / quoteUnit, failureCurve.quoteExponent()) * f.failureFor(b), 0, 1);
    }

    @Override
    public Risk risk(Blueprint b, double current, double q, Profile f, double failure) {
        double excess = Math.max(0, current - f.safety());
        if (excess == 0) return new Risk(0, 0, 0, 0);
        double scaled = overload(excess, f);
        double damping = 1;
        for (var entry : riskDamping.entrySet()) damping *= Math.pow(entry.getValue(), b.count(entry.getKey()));
        return new Risk(excess, failure,
                clamp(offsetCurve.base() * pow(scaled / overloadUnit, offsetCurve.overloadExponent())
                        * pow(q / quoteUnit, offsetCurve.quoteExponent()) * f.offset() * damping, 0, 1),
                Math.min(maxAngle, angleCurve.base() * pow(scaled, angleCurve.overloadExponent())
                        * pow(q / quoteUnit, angleCurve.quoteExponent())) * damping);
    }

    /** 指数为 1/2 时用 sqrt，结果与原公式逐位一致。 */
    private static double pow(double x, double exponent) {
        return exponent == .5 ? Math.sqrt(x) : Math.pow(x, exponent);
    }

    @Override
    public int checkpointInterval() {
        return checkpointInterval;
    }

    @Override
    public int paymentInterval() {
        return paymentInterval;
    }

    @Override
    public double attenuation(Core output) {
        return attenuations.getOrDefault(output, 1.0);
    }

    @Override
    public double reversalChance(Core output, double excess, Profile f) {
        Reversal reversal = reversals.get(output);
        if (reversal == null) return 0;
        // 运算顺序与旧公式一致（先乘口径再除以 N），保证逐位相同。
        return Math.min(reversal.cap(), overloadScale * excess / Math.max(1, f.safety()) / reversal.divisor());
    }

    @Override
    public double wrongTargetChance(Carrier carrier) {
        return wrongTargets.getOrDefault(carrier, 0.0);
    }

    @Override
    public int cooldown(Blueprint b) {
        return cooldownBase + cooldownPerTier * b.tier();
    }

    @Override
    public double masteryNeeded(Blueprint b) {
        return masteryPerTierSquared * b.tier() * b.tier();
    }

    /**
     * 规则表构建器。{@link #build()} 要求：每个核心都有载体表和消耗系数、每个控制节点都有管线规则，
     * 报价、风险、冷却、熟练度的常数全部填写。构成校验的上下限不填则不限制。
     */
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

        private double pathUnit = Double.NaN, pathWeight = Double.NaN, loadWeight = Double.NaN;
        private double prepTicks = Double.NaN, prepOffset = Double.NaN, defaultFloor = Double.NaN;
        private final EnumSet<Core> squaredPath = EnumSet.noneOf(Core.class);
        private final Map<Control, Double> masteryFloors = new EnumMap<>(Control.class);

        private double overloadScale = Double.NaN, overloadUnit = Double.NaN, quoteUnit = Double.NaN;
        private double certainFailure = Double.NaN, maxAngle = Double.NaN;
        private Curve failureCurve, offsetCurve, angleCurve;
        private final Map<Control, Double> riskDamping = new EnumMap<>(Control.class);

        private int checkpointInterval, paymentInterval;
        private final Map<Core, Double> attenuations = new EnumMap<>(Core.class);
        private final Map<Core, Reversal> reversals = new EnumMap<>(Core.class);
        private final Map<Carrier, Double> wrongTargets = new EnumMap<>(Carrier.class);

        private Integer cooldownBase, cooldownPerTier;
        private double masteryPerTierSquared = Double.NaN;

        private Builder() {}

        // ===== 构成与管线 =====

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

        // ===== 报价：标准消耗 = a×规模 + b×规模²/(准备/prepTicks + prepOffset) + pathWeight×(射程/pathUnit) + loadWeight×负载 =====

        /** 射程项：射程按 {@code unit} 折算，再乘 {@code weight}。 */
        public Builder pathCost(double unit, double weight) {
            pathUnit = unit; pathWeight = weight;
            return this;
        }

        /** 这些核心的射程项取平方（远距离代价急剧增长）。 */
        public Builder squaredPath(Core... cores) {
            Collections.addAll(squaredPath, cores);
            return this;
        }

        /** 管线负载项权重。 */
        public Builder loadCost(double weight) {
            loadWeight = weight;
            return this;
        }

        /** 规模平方项的分母：准备时间按 {@code ticks} 折算后加 {@code offset}，准备越久越省。 */
        public Builder prepRelief(double ticks, double offset) {
            prepTicks = ticks; prepOffset = offset;
            return this;
        }

        /** 满熟练度时实际消耗占标准消耗的默认下限。 */
        public Builder masteryFloor(double floor) {
            defaultFloor = floor;
            return this;
        }

        /** 含该控制节点时的熟练度下限；多个节点同时出现取最低值。单个法术的调校下限优先于这里。 */
        public Builder masteryFloor(Control control, double floor) {
            masteryFloors.put(Objects.requireNonNull(control), floor);
            return this;
        }

        // ===== 风险 =====

        /**
         * 超载口径：超载 = 超出安全阈值的负担 / N × {@code scale}；失败与偏移曲线再除以 {@code unit}，
         * 偏转角曲线直接用超载；报价按 {@code quoteUnit} 折算。
         */
        public Builder riskScale(double scale, double unit, double quoteUnit) {
            overloadScale = scale; overloadUnit = unit; this.quoteUnit = quoteUnit;
            return this;
        }

        /** 负担超过 {@code multiple}×N 时必定失败。 */
        public Builder certainFailure(double multiple) {
            certainFailure = multiple;
            return this;
        }

        /** 整次施法失败概率（再乘形态失败系数）。 */
        public Builder failureCurve(Curve curve) {
            failureCurve = Objects.requireNonNull(curve);
            return this;
        }

        /** 每次检查点的偏移概率（再乘形态偏移系数）。 */
        public Builder offsetCurve(Curve curve) {
            offsetCurve = Objects.requireNonNull(curve);
            return this;
        }

        /** 偏移时的最大偏转角（度），上限 {@code max}。 */
        public Builder angleCurve(Curve curve, double max) {
            angleCurve = Objects.requireNonNull(curve); maxAngle = max;
            return this;
        }

        /** 每出现一次该控制节点，偏移概率与偏转角乘 {@code factor}。 */
        public Builder riskDamping(Control control, double factor) {
            riskDamping.put(Objects.requireNonNull(control), factor);
            return this;
        }

        // ===== 施法会话 =====

        /** 每隔 {@code ticks} tick 记录检查点并抽一次偏移（必须 ≥ 1）。 */
        public Builder checkpointInterval(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("checkpointInterval must be >= 1");
            checkpointInterval = ticks;
            return this;
        }

        /** 报价每 {@code ticks} tick 支付一期（必须 ≥ 1）。 */
        public Builder paymentInterval(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("paymentInterval must be >= 1");
            paymentInterval = ticks;
            return this;
        }

        /** 偏移时该输出元素的效果乘 {@code factor}；不设置则不衰减。 */
        public Builder attenuation(Core output, double factor) {
            attenuations.put(Objects.requireNonNull(output), factor);
            return this;
        }

        /** 偏移时该输出元素可能反转；不设置则不会反转。 */
        public Builder reversal(Core output, Reversal reversal) {
            reversals.put(Objects.requireNonNull(output), Objects.requireNonNull(reversal));
            return this;
        }

        /** 偏移时该载体以 {@code chance} 打错目标；不设置则不会。 */
        public Builder wrongTarget(Carrier carrier, double chance) {
            wrongTargets.put(Objects.requireNonNull(carrier), chance);
            return this;
        }

        // ===== 冷却与熟练度 =====

        /** 冷却 = {@code base} + {@code perTier}×阶级（tick）。 */
        public Builder cooldown(int base, int perTier) {
            cooldownBase = base; cooldownPerTier = perTier;
            return this;
        }

        /** 所需熟练度 = {@code perTierSquared}×阶级²。 */
        public Builder mastery(double perTierSquared) {
            masteryPerTierSquared = perTierSquared;
            return this;
        }

        public NeoModel build() {
            for (Core core : Core.values()) {
                if (!carriers.containsKey(core)) throw new IllegalStateException("No carriers for core " + core);
                if (!coefficients.containsKey(core)) throw new IllegalStateException("No coefficients for core " + core);
            }
            for (Control control : Control.values())
                if (!controls.containsKey(control)) throw new IllegalStateException("No pipeline rule for control " + control);
            requireSet(pathUnit, "pathCost");
            requireSet(pathWeight, "pathCost");
            requireSet(loadWeight, "loadCost");
            requireSet(prepTicks, "prepRelief");
            requireSet(prepOffset, "prepRelief");
            requireSet(defaultFloor, "masteryFloor");
            requireSet(overloadScale, "riskScale");
            requireSet(overloadUnit, "riskScale");
            requireSet(quoteUnit, "riskScale");
            requireSet(certainFailure, "certainFailure");
            requireSet(maxAngle, "angleCurve");
            requireSet(masteryPerTierSquared, "mastery");
            if (failureCurve == null) throw new IllegalStateException("failureCurve is not set");
            if (offsetCurve == null) throw new IllegalStateException("offsetCurve is not set");
            if (angleCurve == null) throw new IllegalStateException("angleCurve is not set");
            if (cooldownBase == null || cooldownPerTier == null) throw new IllegalStateException("cooldown is not set");
            if (checkpointInterval == 0) throw new IllegalStateException("checkpointInterval is not set");
            if (paymentInterval == 0) throw new IllegalStateException("paymentInterval is not set");
            return new NeoModel(this);
        }

        private static void requireSet(double value, String setter) {
            if (Double.isNaN(value)) throw new IllegalStateException(setter + " is not set");
        }
    }
}
