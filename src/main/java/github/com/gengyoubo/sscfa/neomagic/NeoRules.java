package github.com.gengyoubo.sscfa.neomagic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.util.FormIdMigration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据包驱动的新魔法规则（{@code data/ssca_neomagic/rules/defaults.json}）：按核心的消耗系数、按形态的风险档案、
 * 按法术的调校、法术负担的恢复参数。
 *
 * <p>作为 {@link INeoModel} 使用时，消耗系数取数据包的值（构建进底层 {@link NeoModel}），冷却与所需熟练度优先取法术调校，
 * 其余规则全部委托底层模型。实例不可变；重载时整体替换 {@link #current()}，进行中的施法会话继续用锁定时的报价与规则。</p>
 */
public final class NeoRules implements INeoModel {
    /** 数据包里没有配置的形态使用的风险档案（标记为参考值）。 */
    public static final Profile DEFAULT_PROFILE = new Profile(100, 1, 1, 1, 1, true);
    /** 数据包加载前的规则：默认消耗系数、无形态档案、无法术调校。 */
    public static final NeoRules DEFAULT = new NeoRules(NeoModel.INSTANCE, Map.of(), Map.of(), 120, .25, "v2-default");

    private static volatile NeoRules current = DEFAULT;

    private final NeoModel base;
    private final Map<String, Profile> forms;
    private final Map<String, Tuning> spells;
    private final int recoveryDelay;
    private final double recoveryRate;
    private final String dataVersion;

    private NeoRules(NeoModel base, Map<String, Profile> forms, Map<String, Tuning> spells,
                     int recoveryDelay, double recoveryRate, String dataVersion) {
        this.base = base;
        this.forms = Map.copyOf(forms);
        this.spells = Map.copyOf(spells);
        this.recoveryDelay = recoveryDelay;
        this.recoveryRate = recoveryRate;
        this.dataVersion = dataVersion;
    }

    /** 当前生效的规则（服务端最近一次数据包重载的结果）。 */
    public static NeoRules current() {
        return current;
    }

    static void install(NeoRules rules) {
        current = rules;
    }

    // ===== 数据 =====

    /** 规则版本：引擎（模组）版本 + 数据版本与内容哈希，用于和客户端对齐。 */
    public String rulesVersion() {
        return NeoMagic.engineVersion() + ":" + dataVersion;
    }

    /** 该形态的风险档案；旧形态 ID 会先映射到新 ID，未配置的形态返回 {@link #DEFAULT_PROFILE}。 */
    public Profile profile(String formId) {
        return forms.getOrDefault(FormIdMigration.canonical(formId), DEFAULT_PROFILE);
    }

    public Map<String, Profile> profiles() {
        return forms;
    }

    /** 法术调校：先按「构成#规模档」查，再按构成查，都没有则不调校。 */
    public Tuning tuning(Blueprint b) {
        return spells.getOrDefault(b.identity() + "#" + b.band(), spells.getOrDefault(b.identity(), Tuning.defaults()));
    }

    public Map<String, Tuning> tunings() {
        return spells;
    }

    /** 法术负担停止支付后多少 tick 开始恢复。 */
    public int recoveryDelay() {
        return recoveryDelay;
    }

    /** 每 tick 恢复的负担。 */
    public double recoveryRate() {
        return recoveryRate;
    }

    /** 按数据包消耗系数与法术调校报价。 */
    public Quote quote(Blueprint b, double mastery) {
        return quote(b, mastery, coefficients(b.core()), tuning(b));
    }

    // ===== INeoModel：冷却与熟练度按调校覆盖，其余委托底层模型 =====

    @Override
    public int cooldown(Blueprint b) {
        Tuning tuning = tuning(b);
        return tuning.cooldown() > 0 ? tuning.cooldown() : base.cooldown(b);
    }

    @Override
    public double masteryNeeded(Blueprint b) {
        Tuning tuning = tuning(b);
        return tuning.mastery() > 0 ? tuning.mastery() : base.masteryNeeded(b);
    }

    @Override public int version() { return base.version(); }
    @Override public boolean supports(Core core, Carrier carrier) { return base.supports(core, carrier); }
    @Override public List<String> errors(Blueprint blueprint) { return base.errors(blueprint); }
    @Override public Plan plan(Blueprint blueprint) { return base.plan(blueprint); }
    @Override public Coefficients coefficients(Core core) { return base.coefficients(core); }
    @Override public Quote quote(Blueprint b, double mastery, Coefficients c, Tuning tuning) { return base.quote(b, mastery, c, tuning); }
    @Override public double failureChance(Blueprint b, double current, double q, Profile f) { return base.failureChance(b, current, q, f); }
    @Override public Risk risk(Blueprint b, double current, double q, Profile f, double failure) { return base.risk(b, current, q, f, failure); }
    @Override public int checkpointInterval() { return base.checkpointInterval(); }
    @Override public int paymentInterval() { return base.paymentInterval(); }
    @Override public double attenuation(Core output) { return base.attenuation(output); }
    @Override public double reversalChance(Core output, double excess, Profile f) { return base.reversalChance(output, excess, f); }
    @Override public double wrongTargetChance(Carrier carrier) { return base.wrongTargetChance(carrier); }

    // ===== 解析 =====

    /**
     * 解析规则 JSON。任一字段非法都会抛 {@link IllegalArgumentException}，调用方据此保留旧规则。
     * 同一形态同时写了新旧 ID 时以新 ID 为准。
     */
    public static NeoRules parse(JsonObject root) {
        Map<String, Profile> forms = new HashMap<>();
        root.getAsJsonObject("forms").entrySet().forEach(e -> {
            JsonObject p = e.getValue().getAsJsonObject();
            double s = p.get("safety").getAsDouble(), f = p.get("failure").getAsDouble(), j = p.get("offset").getAsDouble();
            double m = optional(p, "merge", 1), split = optional(p, "split", 1);
            if (!Double.isFinite(s + f + j + m + split) || s <= 0 || f <= 0 || j <= 0 || m <= 0 || split <= 0)
                throw new IllegalArgumentException("Invalid form: " + e.getKey());
            String id = FormIdMigration.canonical(e.getKey());
            Profile profile = new Profile(s, f, j, m, split, false);
            if (id.equals(e.getKey())) forms.put(id, profile);
            else forms.putIfAbsent(id, profile);
        });

        NeoModel.Builder model = NeoModel.defaults();
        for (Core c : Core.values()) {
            JsonObject p = root.getAsJsonObject("costs").getAsJsonObject(c.name());
            if (p == null) throw new IllegalArgumentException("Missing cost: " + c);
            double a = p.get("a").getAsDouble(), b = p.get("b").getAsDouble();
            if (!Double.isFinite(a + b) || a <= 0 || b <= 0) throw new IllegalArgumentException("Invalid cost: " + c);
            model.coefficients(c, a, b);
        }

        int delay = root.get("recovery_delay").getAsInt();
        double rate = root.get("recovery_per_tick").getAsDouble();
        if (delay < 0 || !Double.isFinite(rate) || rate <= 0) throw new IllegalArgumentException("Invalid recovery");

        Map<String, Tuning> spells = new HashMap<>();
        if (root.has("spells")) root.getAsJsonObject("spells").entrySet().forEach(entry -> {
            JsonObject o = entry.getValue().getAsJsonObject();
            Tuning tune = new Tuning(optional(o, "cost", 1), optional(o, "floor", 0),
                    o.has("cooldown") ? o.get("cooldown").getAsInt() : 0, optional(o, "mastery", 0));
            if (!Double.isFinite(tune.cost() + tune.floor() + tune.mastery()) || tune.cost() <= 0 || tune.cooldown() < 0
                    || tune.mastery() < 0 || (tune.floor() != 0 && (tune.floor() < .35 || tune.floor() > .5)))
                throw new IllegalArgumentException("Invalid spell tuning: " + entry.getKey());
            spells.put(entry.getKey(), tune);
        });

        String version = root.get("version").getAsString() + ":" + Integer.toHexString(root.toString().hashCode());
        return new NeoRules(model.build(), forms, spells, delay, rate, version);
    }

    private static double optional(JsonObject o, String key, double fallback) {
        JsonElement e = o.get(key);
        return e == null ? fallback : e.getAsDouble();
    }
}
