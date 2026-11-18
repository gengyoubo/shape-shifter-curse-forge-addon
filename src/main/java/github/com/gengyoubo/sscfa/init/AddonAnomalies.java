package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.anomaly.FrozenControl;

import static github.com.gengyoubo.sscfa.init.AddonAnomalyEffects.*;

/** Registers the built-in anomaly bases and reactions during common setup. */
public final class AddonAnomalies {
    private static boolean initialized;

    private AddonAnomalies() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        registerBase("fire", (target, context) -> reactionDamage(target, context, 8, true));
        registerBase("ice", (target, context) -> FrozenControl.freeze(target, FrozenControl.DEFAULT_DURATION_TICKS));
        registerBase("lunar", (target, context) -> apply(target, LUNAR_ECLIPSE.get(), 120, 0));
        registerBase("curse", (target, context) -> apply(target, CURSE_WEAKNESS.get(), 120, 0));
        registerBase("summon", (target, context) -> apply(target, SUMMON_MARK.get(), 160, 0));
        registerBase("void", (target, context) -> apply(target, VOID_EROSION.get(), 120, 0));
        registerBase("space", (target, context) -> apply(target, SPACE_BIND.get(), 100, 0));

        registerReaction("fire", "ice", (target, context) -> area(target, context, 5, other -> {
            reactionDamage(other, context, 8, false);
            if (other.isAlive()) apply(other, FROST_SLOW.get(), 100, 0);
        }));
        registerReaction("fire", "lunar", (target, context) -> apply(target, LUNAR_ECLIPSE.get(), 120, 1));
        registerReaction("fire", "curse", (target, context) -> area(target, context, 3,
                other -> apply(other, CURSE_WEAKNESS.get(), 100, 0)));
        registerReaction("fire", "summon", (target, context) -> budget(target, FLAME_SPIRIT.get()));
        registerReaction("fire", "void", (target, context) -> reactionDamage(target, context, 4, false));
        registerReaction("fire", "space", AddonAnomalyEffects::fireZone);
        registerReaction("ice", "lunar", (target, context) -> extend(target, LUNAR_ECLIPSE.get(), 180, 0));
        registerReaction("ice", "curse", (target, context) -> apply(target, CURSE_WEAKNESS.get(), 120, 2));
        registerReaction("ice", "summon", (target, context) -> budget(target, FROST_HUNT.get()));
        registerReaction("ice", "void", (target, context) -> reactionDamage(target, context, 6, false));
        registerReaction("ice", "space", (target, context) -> apply(target, NO_SPRINT.get(), 100, 0));
        registerReaction("lunar", "curse", (target, context) -> {
            extend(target, LUNAR_ECLIPSE.get(), 180, 0); extend(target, CURSE_WEAKNESS.get(), 180, 0);
        });
        registerReaction("lunar", "summon", (target, context) -> budget(target, SPIRIT_MOON.get()));
        registerReaction("lunar", "void", (target, context) -> apply(target, VOID_EROSION.get(), 120, 1));
        registerReaction("lunar", "space", (target, context) -> area(target, context, 3,
                other -> apply(other, LUNAR_ECLIPSE.get(), 100, 0)));
        registerReaction("curse", "summon", (target, context) -> apply(target, SUMMON_WEAKNESS.get(), 120, 0));
        registerReaction("curse", "void", (target, context) -> {
            apply(target, CURSE_WEAKNESS.get(), 120, 1); apply(target, VOID_EROSION.get(), 120, 1);
        });
        registerReaction("curse", "space", (target, context) -> area(target, context, 3, other -> {
            apply(other, CURSE_WEAKNESS.get(), 100, 0); apply(other, NO_SPRINT.get(), 100, 0);
        }));
        registerReaction("summon", "void", (target, context) -> apply(target, SUMMON_MARK.get(), 160, 1));
        registerReaction("summon", "space", (target, context) -> area(target, context, 3,
                other -> apply(other, SUMMON_MARK.get(), 160, 0)));
        registerReaction("void", "space", (target, context) -> area(target, context, 3,
                other -> apply(other, VOID_EROSION.get(), 100, 0)));

        AddonAnomalyEffects.init();
    }
}
