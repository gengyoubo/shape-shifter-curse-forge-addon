package github.com.gengyoubo.sscfa.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

import java.util.Map;
import java.util.Set;

/** 帧级相机/规避强度快照；Forge 侧不接入 AsyncParticles。 */
public final class AsyncParticleCompatibility {
    private static volatile Camera camera;
    private static volatile float tickDelta;
    private static volatile ParticleAvoidance.Strength frameStrength;

    private AsyncParticleCompatibility() {
    }

    public static void prepare(Particle particle) {
    }

    public static void begin(Camera currentCamera, float delta) {
        camera = currentCamera;
        tickDelta = delta;
    }

    public static void end() {
        camera = null;
        frameStrength = null;
    }

    public static ParticleAvoidance.Strength frameStrength() {
        return frameStrength;
    }

    public static void snapshotStrength(ParticleAvoidance.Strength strength) {
        frameStrength = strength;
    }

    public static Set<Particle> syncBatch(Map<ParticleRenderType, Set<Particle>> queues,
                                          ParticleRenderType sheet, Set<Particle> original) {
        Camera frameCamera = camera;
        float frameDelta = tickDelta;
        if (frameCamera == null) return original;
        return ParticleRenderRouting.syncBatch(queues, sheet, original,
                ParticleRenderType.PARTICLE_SHEET_OPAQUE, ParticleRenderType.PARTICLE_SHEET_LIT,
                ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
                particle -> particle instanceof OwnedDecoration owned
                        && owned.ssca$cameraVisibility(frameCamera, frameDelta) < 1);
    }
}
