package github.com.gengyoubo.sscfa.neomagic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 新魔法的规则约定：法术构成的类型（核心、载体、控制节点、蓝图……）以及校验、报价、风险等计算接口。
 * 不含任何 Minecraft 或旧魔法状态。
 *
 * <p>默认实现是 {@link NeoModel#INSTANCE}。数据驱动的调校层（按核心的消耗系数、按法术的调校、按形态的风险档案）
 * 应实现本接口并委托默认实现，而不是改动 {@link NeoModel}；施法会话 {@link NeoSession} 只依赖本接口。</p>
 */
public interface INeoModel {
    enum Core { THERMAL, FROST, LUNAR, CURSE, SUMMON, VOID, SPACE }
    enum Carrier { SELF, CONTACT, PROJECTILE, AREA, LOCK }
    enum Control { SPLIT, MERGE, STORE, CONSTRAIN, BRIDGE, CONVERT }

    record Blueprint(String name, Core core, Carrier carrier, List<Control> controls,
                     Core conversion, double scale, int prep, double range) {
        public Blueprint {
            Objects.requireNonNull(name); Objects.requireNonNull(core); Objects.requireNonNull(carrier);
            controls = List.copyOf(controls); Objects.requireNonNull(conversion);
        }
        public static Blueprint starter() {
            return new Blueprint("", Core.THERMAL, Carrier.PROJECTILE, List.of(), Core.FROST, 1, 40, 32);
        }
        public String identity() {
            return core + ":" + carrier + ":" + controls + (controls.contains(Control.CONVERT) ? ":" + conversion : "");
        }
        public int band() { return scale <= 1 ? 0 : scale <= 2 ? 1 : scale <= 3 ? 2 : 3; }
        public int tier() { return 1 + controls.size() + band(); }
        public Core output() { return controls.contains(Control.CONVERT) ? conversion : core; }
        public int count(Control c) { return Collections.frequency(controls, c); }
    }

    /** 形态风险档案：安全阈值 N、失败/偏移系数，以及合并/分裂的失败修正。 */
    record Profile(double safety, double failure, double offset, double merge, double split, boolean reference) {
        public double failureFor(Blueprint b) {
            return failure * (b.controls().contains(Control.MERGE) ? merge : 1) * (b.controls().contains(Control.SPLIT) ? split : 1);
        }
    }
    /** 消耗系数：线性项 a、规模平方项 b。 */
    record Coefficients(double a, double b) {}
    /** 单个法术的调校：消耗倍率、适应下限、冷却、所需熟练度（0 表示用默认规则）。 */
    record Tuning(double cost, double floor, int cooldown, double mastery) {
        public static Tuning defaults() { return new Tuning(1, 0, 0, 0); }
    }
    record Plan(int branches, double retention, double load, boolean focused, boolean stored, int constraints) {}
    record Quote(double normal, double actual, double floor, Plan plan) {}
    record Risk(double excess, double failure, double offset, double angle) {}
    record Checkpoint(int tick, double burden, double paid, Risk risk, boolean failed, boolean shifted,
                      double yaw, double pitch, double attenuation, boolean reversed, boolean wrongTarget) {}

    /** 规则版本，用于和客户端/存档中的蓝图对齐。 */
    int version();

    /** 核心能否搭配该载体。 */
    boolean supports(Core core, Carrier carrier);

    /** 蓝图的全部校验错误键；空列表表示合法。 */
    List<String> errors(Blueprint blueprint);

    /** 控制节点管线的分支数、保留率与负载。 */
    Plan plan(Blueprint blueprint);

    /** 核心的默认消耗系数。 */
    Coefficients coefficients(Core core);

    /** 报价：标准消耗与按熟练度折减后的实际消耗。 */
    Quote quote(Blueprint blueprint, double mastery, Coefficients coefficients, Tuning tuning);

    default Quote quote(Blueprint blueprint, double mastery, Coefficients coefficients) {
        return quote(blueprint, mastery, coefficients, Tuning.defaults());
    }

    /** 整次施法的失败概率预测（纯计算；实际随机只由 {@link NeoSession} 抽一次）。 */
    double failureChance(Blueprint blueprint, double current, double quote, Profile profile);

    /** 以给定失败概率计算当前负担下的风险（偏移概率与角度随实时负担变化）。 */
    Risk risk(Blueprint blueprint, double current, double quote, Profile profile, double failure);

    /** 完整风险预测：失败概率按当前负担重新计算。 */
    default Risk risk(Blueprint blueprint, double current, double quote, Profile profile) {
        return risk(blueprint, current, quote, profile, failureChance(blueprint, current, quote, profile));
    }

    /** 只看实时偏移：不重新计算整次施法已冻结的失败概率。 */
    default Risk offsetRisk(Blueprint blueprint, double current, double quote, Profile profile) {
        return risk(blueprint, current, quote, profile, 0);
    }

    // ===== 施法会话（NeoSession）节奏与偏移后果 =====

    /** 每隔多少 tick 记录一次检查点并抽一次偏移。 */
    int checkpointInterval();

    /** 报价按多少 tick 一期分期支付（准备结束时结清余数）。 */
    int paymentInterval();

    /** 偏移发生时该输出元素的效果衰减倍数；1 表示不衰减。 */
    double attenuation(Core output);

    /** 偏移发生时该输出元素反转的概率；0 表示不会反转（也不消耗随机数）。 */
    double reversalChance(Core output, double excess, Profile profile);

    /** 偏移发生时该载体打错目标的概率；0 表示不会（也不消耗随机数）。 */
    double wrongTargetChance(Carrier carrier);

    /** 冷却（tick）。 */
    int cooldown(Blueprint blueprint);

    /** 掌握该法术所需熟练度。 */
    double masteryNeeded(Blueprint blueprint);

    static double clamp(double n, double low, double high) {
        return Math.max(low, Math.min(high, n));
    }
}
