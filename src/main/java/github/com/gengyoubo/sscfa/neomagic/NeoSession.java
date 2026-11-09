package github.com.gengyoubo.sscfa.neomagic;

import github.com.gengyoubo.sscfa.neomagic.INeoModel.Blueprint;
import github.com.gengyoubo.sscfa.neomagic.INeoModel.Checkpoint;
import github.com.gengyoubo.sscfa.neomagic.INeoModel.Profile;
import github.com.gengyoubo.sscfa.neomagic.INeoModel.Quote;
import github.com.gengyoubo.sscfa.neomagic.INeoModel.Risk;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 一次施法的准备过程：按期分期支付锁定的报价、整次只抽一次失败、定期记录检查点并抽偏移。
 * 支付与检查点节奏、风险、偏移后果全部由构造时传入的 {@link INeoModel} 决定。
 */
public final class NeoSession {
    public final INeoModel model;
    public final Blueprint blueprint;
    public final Quote quote;
    public final List<Checkpoint> checkpoints = new ArrayList<>();
    public double paid, yaw, pitch, attenuation = 1, survival = 1;
    public double failureProbability, failureBasis;
    public int failureDraws;
    /** Special lock is exclusive; ordinary completion checks its final payment before releasing. */
    public final int failureBoundary;
    public boolean failed, complete, reversed, wrongTarget, hasShifted;
    private int previous = -1;
    private int failureAt = -1;
    private final int duration;
    private final int latestFailureTick;
    private final int checkpointInterval;
    private final int paymentInterval;

    public NeoSession(INeoModel model, Blueprint b, Quote q) {
        this(model, b, q, b.prep());
    }

    /** Existing spells may have longer preparation than the research editor's 80 tick range. */
    public NeoSession(INeoModel model, Blueprint b, Quote q, int duration) {
        this(model, b, q, duration, duration);
    }

    public NeoSession(INeoModel model, Blueprint b, Quote q, int duration, int lockInTick) {
        this.model = model;
        checkpointInterval = model.checkpointInterval();
        paymentInterval = model.paymentInterval();
        blueprint = b;
        quote = q;
        this.duration = Math.max(0, duration);
        failureBoundary = lockInTick < 0 ? this.duration : Math.min(this.duration, lockInTick);
        // Ordinary completion checks payment before releasing its effect. An earlier
        // special lock protects every tick at/after that lock, including held casts.
        latestFailureTick = this.duration == 0 ? 0 : failureBoundary < this.duration ? failureBoundary - 1 : this.duration;
    }

    /** Only risk events need the current form; ordinary payment ticks need no profile allocation. */
    public boolean needsRisk(int age) {
        return !failed && !complete && age > previous && (failureDraws == 0 || age % checkpointInterval == 0
                || failureAt >= 0 && age >= failureAt && age <= latestFailureTick);
    }

    private void drawFailure(double current, Profile form, RandomGenerator rng) {
        failureBasis = current + quote.actual();
        failureProbability = model.failureChance(blueprint, failureBasis, quote.actual(), form);
        failureDraws = 1;
        survival = 1 - failureProbability;
        if (rng.nextDouble() < failureProbability) {
            int earliest = firstUnsafeTick(current, form.safety());
            if (earliest <= latestFailureTick)
                failureAt = earliest + rng.nextInt(latestFailureTick - earliest + 1);
        }
    }

    private int firstUnsafeTick(double initial, double safety) {
        if (initial > safety || duration == 0) return 0;
        if (quote.actual() <= 0 || initial + quote.actual() <= safety) return Integer.MAX_VALUE;
        double crossing = (safety - initial) * duration / quote.actual();
        int first = (int) Math.min(duration, (Math.floor(crossing / paymentInterval) + 1) * paymentInterval);
        // Correct floating point equality at an exact payment boundary.
        while (first < duration && initial + quote.actual() * first / duration <= safety) first = Math.min(duration, first + paymentInterval);
        return first;
    }

    /** Caller adds returned delta to its sole C before consuming this checkpoint. */
    public double advance(int age, double current, Profile form, RandomGenerator rng) {
        return advance(age, current, form, rng, true);
    }

    public double advance(int age, double current, Profile form, RandomGenerator rng, boolean finishAtDuration) {
        if (failed || complete || age <= previous) return 0;
        if (age != previous + 1) throw new IllegalArgumentException("Session must advance one tick at a time");
        previous = age;
        if (failureDraws == 0) {
            // One whole-cast draw, using the initial C plus this cast's actual locked quote.
            // Keep the result and scheduled time on the server; later C changes never redraw it.
            drawFailure(current, form, rng);
        }
        int billed = age >= duration ? duration : age / paymentInterval * paymentInterval;
        double due = duration == 0 ? quote.actual() : quote.actual() * billed / duration;
        double delta = Math.max(0, due - paid);
        paid += delta;
        // One frozen draw and one scheduled time. Recovery can defer the failure,
        // but it can never fire while the live body is within its safety threshold.
        failed = failureAt >= 0 && age >= failureAt && age <= latestFailureTick
                && form != null && current + delta > form.safety();
        boolean checkpoint = age % checkpointInterval == 0;
        if (checkpoint || failed) {
            Risk r = model.risk(blueprint, current + delta, quote.actual(), form, failureProbability);
            boolean shiftDraw = !failed && checkpoint && r.offset() > 0 && rng.nextDouble() < r.offset();
            if (!failed && shiftDraw) {
                hasShifted = true;
                yaw += (rng.nextDouble() * 2 - 1) * r.angle();
                pitch += (rng.nextDouble() * 2 - 1) * r.angle();
                attenuation *= model.attenuation(blueprint.output());
                double reversal = model.reversalChance(blueprint.output(), r.excess(), form);
                if (reversal > 0 && rng.nextDouble() < reversal) reversed = true;
                double misfire = model.wrongTargetChance(blueprint.carrier());
                if (misfire > 0 && rng.nextDouble() < misfire) wrongTarget = true;
            }
            checkpoints.add(new Checkpoint(age, current + delta, paid, r, failed, !failed && shiftDraw, yaw, pitch,
                    attenuation, reversed, wrongTarget));
        }
        complete = !failed && finishAtDuration && age >= duration;
        return delta;
    }
}
