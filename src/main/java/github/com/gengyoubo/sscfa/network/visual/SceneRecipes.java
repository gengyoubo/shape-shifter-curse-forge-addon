package github.com.gengyoubo.sscfa.network.visual;

import java.util.random.RandomGenerator;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.Kind;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.View;
import github.com.gengyoubo.sscfa.network.visual.recipes.MistFormRecipes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Shared sampling and endpoints; viewers never consume the recipe RNG for local jitter or culling. */
public final class SceneRecipes {
    private SceneRecipes() {}
    public static boolean active(View v, int age) {
        if (age < 0) return false;
        return switch (v.key().kind()) {
            case CORRUPT_MIST -> age >= 1 && age < v.parameter(1);
            case METEOR_WARNING -> age >= 1 && age <= v.duration();
            case DOMAIN_EDGE -> age < v.duration();
            case DOMAIN_AMBIENT -> age < v.duration() && v.parameter(1) + age * v.parameter(2) <= v.parameter(3);
            case MIST_FORM_CHARGE -> age >= v.parameter(3) && age < v.duration();
            default -> v.duration() <= 0 || age < v.duration();
        };
    }
    /** Latest single due sample only. No burst catch-up after lag, late join or a world-clock correction. */
    public static int latestSampleAge(View v, int age) {
        if (!active(v, age)) return -1;
        int period = 1, offset = 0, first = 0;
        switch (v.key().kind()) {
            case CORRUPT_MIST -> { period = 5; offset = Math.floorMod((int)v.parameter(1), 5); first = offset == 0 ? 5 : offset; }
            case BLUE_FIRE_RING -> period = 4;
            case MIST_FORM_CHARGE -> { period = 2; first = (int)v.parameter(3); }
            case DOMAIN_AMBIENT -> {
                period = 5;
                int counter = (int)v.parameter(1), step = (int)v.parameter(2);
                offset = -1;
                for (int i = 0; i < 5; i++) if (Math.floorMod(counter + i * step,5) == 0) { offset = i; break; }
                if (offset < 0) return -1;
                first = offset;
            }
            case METEOR_WARNING -> first = 1;
            default -> { }
        }
        if (age < first) return -1;
        int sample = age - Math.floorMod(age - offset, period);
        return sample >= first && active(v, sample) ? sample : -1;
    }
    public static void emit(View v, int age, long tick, RandomGenerator random, SceneEmitter sink) {
        if (!active(v, age)) return;
        switch (v.key().kind()) {
            case CORRUPT_MIST -> mist((long)v.parameter(1)-age,v.parameter(0),random,sink);
            case METEOR_WARNING -> meteorWarning(age+(long)v.parameter(1),v.parameter(0),sink);
            case DOMAIN_EDGE -> domainEdge(Math.max(0,Math.min(v.parameter(2),v.parameter(0)+age*v.parameter(1))),sink);
            case DOMAIN_AMBIENT -> domainAmbient((long)v.parameter(1)+age*(long)v.parameter(2),v.parameter(0),random,sink);
            case MIST_FORM_AMBIENT -> MistFormRecipes.ambient(v.parameter(0),(int)v.parameter(1),sink);
            case MIST_FORM_CHARGE -> MistFormRecipes.charge(age,(int)v.parameter(0),v.parameter(1),(int)v.parameter(2),random::nextDouble,sink);
            default -> blueTrail(v.key().kind(),age,tick,v.width(),v.eyeHeight(),v.parameters(),random,sink);
        }
    }

    /** Coordinates are relative to the caster's feet, preserving the original uniform disk. */
    private static void mist(long ticksRemaining, double radius, RandomGenerator random, SceneEmitter sink) {
        if (ticksRemaining <= 0 || ticksRemaining % 5 != 0) return;
        int count = (int) Math.max(10, radius * radius * 4);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            double y = 0.1 + random.nextDouble() * 0.9;
            boolean green = random.nextInt(4) == 0;
            batch(sink, green ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.DRAGON_BREATH,
                    x, y, z, 1, 0.15, 0.1, 0.15, green ? 0 : 0.002, true);
        }
    }

    /** Coordinates are relative to the fixed impact position, rather than the hovering meteor. */
    private static void meteorWarning(long entityAge, double radius, SceneEmitter sink) {
        int ringCount = (int) Math.max(12, radius * 10);
        for (int i = 0; i < ringCount; i++) {
            double angle = i * 2 * Math.PI / ringCount + entityAge * 0.15;
            double r = radius * 0.95;
            batch(sink, ParticleTypes.FLAME, Math.cos(angle) * r, 0.1, Math.sin(angle) * r,
                    1, 0.02, 0.02, 0.02, 0.005, false);
        }
        batch(sink, ParticleTypes.LARGE_SMOKE, 0, 0.5, 0, 3, 0.3, 0.5, 0.3, 0.02, false);
    }

    /** Relative to (center block X + .5, centerY, center block Z + .5). */
    private static void domainEdge(double radius, SceneEmitter sink) {
        if (radius < 1) return;
        int flameCount = Math.min((int) (radius * 2), 32);
        for (int i = 0; i < flameCount; i++) {
            double angle = Math.PI * 2 * i / flameCount;
            batch(sink, ParticleTypes.SOUL_FIRE_FLAME, Math.cos(angle) * radius, 1, Math.sin(angle) * radius,
                    1, 0, 0.3, 0, 0.01, false);
        }
        int soulCount = Math.min((int) (radius * 1.5), 24);
        for (int i = 0; i < soulCount; i++) {
            double angle = Math.PI * 2 * i / soulCount;
            batch(sink, ParticleTypes.SOUL, Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius,
                    1, 0.2, 0.4, 0.2, 0.02, false);
        }
    }

    /** Keep the existing linear-radius distribution and the skill's actual phase counter. */
    private static void domainAmbient(long phaseCounter, double radius, RandomGenerator random, SceneEmitter sink) {
        if (phaseCounter % 5 != 0) return;
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            double y = random.nextDouble() * 3;
            batch(sink, ParticleTypes.SOUL, x, y, z, 1, 0.5, 0.5, 0.5, 0.02, true);
        }
    }

    /** Original SP blue-ring and projectile batch geometry, shared by client and legacy fallback. */
    private static void blueTrail(Kind kind, int age, long worldTime, float width, float eye,
                                  double[] params, RandomGenerator random, SceneEmitter sink) {
        if (age < 0) return;
        switch (kind) {
            case BLUE_FIRE_RING -> {
                if (age % 4 != 0) return;
                double radius = params[0];
                blueLayer(ParticleTypes.SOUL_FIRE_FLAME, radius, width, eye, sink);
                if (params.length > 1 && params[1] != 0) {
                    blueLayer(ParticleTypes.FLAME, radius, width, eye, sink);
                }
            }
            case SSC_WEB_TRAIL -> {
                int tier = (int) params[0];
                switch (tier) {
                    case 1 -> trail(ParticleTypes.ASH, 3, .05, .01, sink);
                    case 2 -> trail(ParticleTypes.SPIT, 1, .05, .01, sink);
                    case 3 -> trail(ParticleTypes.CLOUD, 2, .05, .01, sink);
                    default -> { /* SSC emits no trail for an unsupported Tier. */ }
                }
            }
            case FIRE_BOLT_TRAIL -> trail(ParticleTypes.FLAME, 2, .1, .01, sink);
            case LUNAR_BOLT_TRAIL -> trail(ParticleTypes.END_ROD, 1, .02, .002, sink);
            default -> { }
        }
    }

    private static void blueLayer(ParticleOptions particle, double radius, float width, float eye,
                                  SceneEmitter sink) {
        // The command's center batch uses absolute spread; the SSC circle scales spread by size.
        sink.emit(particle, 0, .5, 0, 10, 3, .1, 3, .02, true, false);
        for (int i = 0; i < 64; i++) {
            double angle = 2 * Math.PI * i / 64;
            sink.emit(particle, radius * Math.cos(angle), .5, radius * Math.sin(angle),
                    1, .1 * width, .1 * eye, .1 * width, .02, true, false);
        }
    }

    private static void trail(ParticleOptions particle, int count, double spread, double speed,
                               SceneEmitter sink) {
        // These three original trails were ordinary projectile cues, outside decoration scopes.
        sink.emit(particle, 0, 0, 0, count, spread, spread, spread, speed, false, true);
    }

    private static void batch(SceneEmitter sink, ParticleOptions particle, double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed, boolean decoration) {
        // ClientboundLevelParticlesPacket narrowed these four values to float before the old client generated jitter.
        sink.emit(particle, x, y, z, count, (float) dx, (float) dy, (float) dz, (float) speed, decoration, false);
    }
}
