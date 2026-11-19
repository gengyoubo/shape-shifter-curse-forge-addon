package github.com.gengyoubo.sscfa.ability.fruit;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 果实效果的可撤销叠加层：宿主上同类型效果拆成「外部基线」（独立计时）与各果实组的贡献，
 * 合成后写回实体。层数减少时按比例降级（缓慢/虚弱/中毒），果实组失效时只撤掉自己的部分，
 * 外部来源的同名效果按自己的剩余时长保留。由 {@code ParasiticFruitStatusMixin} 观察实体上的效果变化。
 */
public final class FruitStatusOverlay {
    private record Stamp(MobEffectInstance effect, long at, int layers, boolean scales) {}

    private static final class Slot {
        Stamp external;
        final Map<String, Stamp> fruit = new LinkedHashMap<>();
    }

    private static final class State {
        final Map<MobEffect, Slot> slots = new LinkedHashMap<>();
        boolean dirty;
    }

    private record Capture(LivingEntity target, String group, int layers, Set<MobEffect> seen) {}

    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> MUTATING = ThreadLocal.withInitial(() -> false);

    private FruitStatusOverlay() {}

    /** 在 {@code action} 内施加的效果记为 {@code group} 的贡献；本次未再施加的同组效果撤掉。 */
    public static void pulse(LivingEntity target, String group, int layers, Runnable action) {
        Capture previous = CAPTURE.get();
        var capture = new Capture(target, group, layers, new HashSet<>());
        CAPTURE.set(capture);
        try {
            action.run();
        } finally {
            if (previous == null) CAPTURE.remove();
            else CAPTURE.set(previous);
            State state = STATES.get(target);
            if (state != null) {
                state.slots.forEach((effect, slot) -> {
                    if (!capture.seen().contains(effect)) slot.fruit.remove(group);
                });
                reconcile(target, state);
            }
        }
    }

    /** 在可取消的入口检查之后、原版合并效果之前调用。 */
    public static void attempted(LivingEntity target, MobEffectInstance effect, boolean forced) {
        if (target.level().isClientSide || MUTATING.get() || effect == null
                || effect.getEffect() == AddonEffects.BAT_ABSORPTION.get()) return;
        Capture capture = CAPTURE.get();
        State tracked = STATES.get(target);
        // 普通效果只有在该类型已被叠加时才进入记账。
        if ((capture == null || capture.target() != target)
                && (tracked == null || !tracked.slots.containsKey(effect.getEffect()))) return;
        if (!forced && !target.canBeAffected(effect)) return;
        long now = target.level().getGameTime();
        if (capture != null && capture.target() == target) {
            State state = STATES.computeIfAbsent(target, ignored -> new State());
            Slot slot = state.slots.computeIfAbsent(effect.getEffect(), ignored -> {
                var value = new Slot();
                MobEffectInstance existing = target.getEffect(effect.getEffect());
                if (existing != null) value.external = new Stamp(copy(existing), now, 0, false);
                return value;
            });
            boolean scales = !capture.group().equals("support") && !capture.group().equals("outline")
                    && (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN || effect.getEffect() == MobEffects.WEAKNESS
                    || effect.getEffect() == MobEffects.POISON);
            slot.fruit.put(capture.group(), new Stamp(copy(effect), now, capture.layers(), scales));
            capture.seen().add(effect.getEffect());
            state.dirty = true;
        } else {
            Slot slot = tracked.slots.get(effect.getEffect());
            if (slot == null) return;
            MobEffectInstance baseline = forced ? null : aged(slot.external, now);
            if (baseline == null) baseline = copy(effect);
            else baseline.update(copy(effect));
            slot.external = new Stamp(baseline, now, 0, false);
            tracked.dirty = true;
        }
    }

    /** 牛奶、净化等显式移除不得复活外部基线。 */
    public static void removed(LivingEntity target, MobEffect effect) {
        if (target.level().isClientSide || MUTATING.get()) return;
        State state = STATES.get(target);
        if (state == null) return;
        state.slots.remove(effect);
        if (state.slots.isEmpty()) STATES.remove(target);
    }

    /** 按当前生效的各组层数更新贡献：层数为 0 或过期的撤掉，层数变化的按比例调整等级。 */
    public static void update(LivingEntity target, Map<String, Integer> activeGroups) {
        State state = STATES.get(target);
        if (state == null) return;
        long now = target.level().getGameTime();
        for (Slot slot : state.slots.values()) {
            var iterator = slot.fruit.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                Stamp value = entry.getValue();
                int count = activeGroups.getOrDefault(entry.getKey(), 0);
                MobEffectInstance remaining = aged(value, now);
                if (count <= 0 || remaining == null) {
                    iterator.remove();
                    state.dirty = true;
                } else if (count != value.layers()) {
                    if (value.scales()) {
                        remaining = new MobEffectInstance(remaining.getEffect(), remaining.getDuration(),
                                Math.max(0, remaining.getAmplifier() + count - value.layers()),
                                remaining.isAmbient(), remaining.isVisible(), remaining.showIcon());
                    }
                    entry.setValue(new Stamp(remaining, now, count, value.scales()));
                    state.dirty = true;
                }
            }
        }
        if (state.dirty) reconcile(target, state);
    }

    public static void clear(LivingEntity target) {
        State state = STATES.get(target);
        if (state == null) return;
        state.slots.values().forEach(slot -> slot.fruit.clear());
        reconcile(target, state);
    }

    private static void reconcile(LivingEntity target, State state) {
        long now = target.level().getGameTime();
        var iterator = state.slots.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Slot slot = entry.getValue();
            MobEffectInstance combined = aged(slot.external, now);
            for (Stamp stamp : slot.fruit.values()) {
                MobEffectInstance effect = aged(stamp, now);
                if (effect == null) continue;
                if (combined == null) combined = effect;
                else combined.update(effect);
            }
            MobEffectInstance current = target.getEffect(entry.getKey());
            if (!same(current, combined)) {
                boolean previous = MUTATING.get();
                MUTATING.set(true);
                try {
                    target.removeEffect(entry.getKey());
                    if (combined != null) target.forceAddEffect(combined, null);
                } finally {
                    MUTATING.set(previous);
                }
            }
            if (slot.fruit.isEmpty()) iterator.remove();
        }
        state.dirty = false;
        if (state.slots.isEmpty()) STATES.remove(target);
    }

    private static boolean same(MobEffectInstance a, MobEffectInstance b) {
        return a == null ? b == null : b != null && a.save(new CompoundTag()).equals(b.save(new CompoundTag()));
    }

    private static MobEffectInstance copy(MobEffectInstance effect) {
        return Objects.requireNonNull(MobEffectInstance.load(effect.save(new CompoundTag())));
    }

    private static MobEffectInstance aged(Stamp value, long now) {
        if (value == null) return null;
        CompoundTag nbt = age(value.effect().save(new CompoundTag()), Math.max(0, now - value.at()));
        return nbt == null ? null : MobEffectInstance.load(nbt);
    }

    private static CompoundTag age(CompoundTag value, long ticks) {
        CompoundTag hidden = value.contains("HiddenEffect") ? age(value.getCompound("HiddenEffect"), ticks) : null;
        if (hidden == null) value.remove("HiddenEffect");
        else value.put("HiddenEffect", hidden);
        int duration = value.getInt("Duration");
        if (duration >= 0) {
            long left = duration - ticks;
            if (left <= 0) return hidden;
            value.putInt("Duration", (int) Math.min(Integer.MAX_VALUE, left));
        }
        return value;
    }

    /** 测试用：当前跟踪的宿主数。 */
    static int trackedCount() { return new HashMap<>(STATES).size(); }
}
