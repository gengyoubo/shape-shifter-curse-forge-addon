package github.com.gengyoubo.sscfa.network.visual;

import net.minecraft.core.particles.ParticleOptions;

/** Relative particle batches without an allocation for every sample point. */
@FunctionalInterface
public interface SceneEmitter {
    void emit(ParticleOptions particle, double x, double y, double z, int count,
              double dx, double dy, double dz, double speed, boolean decoration, boolean projectile);
}
