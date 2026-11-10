package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.particle.AsyncParticleCompatibility;
import github.com.gengyoubo.sscfa.client.particle.OwnedDecoration;
import github.com.gengyoubo.sscfa.client.particle.ParticleAvoidance;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 为单面粒子提供第一人称装饰规避所需的相机距离/透明度元数据。 */
@Mixin(SingleQuadParticle.class)
public abstract class FirstPersonBillboardParticleMixin extends Particle implements OwnedDecoration {

    protected FirstPersonBillboardParticleMixin(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Shadow
    public abstract float getQuadSize(float tickDelta);

    @Override
    public double ssca$cameraDistance(Camera camera, float tickDelta) {
        var eye = camera.getPosition();
        double dx = Mth.lerp(tickDelta, xo, x) - eye.x;
        double dy = Mth.lerp(tickDelta, yo, y) - eye.y;
        double dz = Mth.lerp(tickDelta, zo, z) - eye.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public float ssca$getRenderAlpha() {
        return alpha;
    }

    @Override
    public void ssca$setRenderAlpha(float value) {
        alpha = value;
    }

    @Override
    public float ssca$cameraVisibility(Camera camera, float tickDelta) {
        var owner = ssca$getDecorationOwner();
        if (owner == null) return 1;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || camera.getEntity() != client.player
                || !client.options.getCameraType().isFirstPerson() || camera.isDetached()
                || !owner.equals(client.player.getUUID())) return 1;
        ParticleAvoidance.Strength strength = AsyncParticleCompatibility.frameStrength();
        if (strength == null) {
            strength = SSCAddonConfig.client().firstPersonParticleAvoidance;
        }
        if (strength == null || strength == ParticleAvoidance.Strength.OFF) return 1;
        var eye = camera.getPosition();
        double dx = Mth.lerp(tickDelta, xo, x) - eye.x;
        double dy = Mth.lerp(tickDelta, yo, y) - eye.y;
        double dz = Mth.lerp(tickDelta, zo, z) - eye.z;
        double squaredDistance = dx * dx + dy * dy + dz * dz;
        double distance = Math.sqrt(squaredDistance);
        if (distance < ParticleAvoidance.NEAR_ZONE && !ParticleAvoidance.keepNearSample(this)) return 0;
        float visibility = ParticleAvoidance.visibility(strength, true, true, distance, getQuadSize(tickDelta));
        boolean projectile = ssca$isProjectileDecoration();
        if (distance > 1.0e-4 && distance < strength.radius() + ParticleAvoidance.RECOVERY) {
            double inv = 1.0 / distance;
            double yaw = Math.toRadians(camera.getYRot());
            double pitch = Math.toRadians(camera.getXRot());
            double dot = (-Math.sin(yaw) * Math.cos(pitch) * dx - Math.sin(pitch) * dy
                    + Math.cos(yaw) * Math.cos(pitch) * dz) * inv;
            visibility = ParticleAvoidance.applyAngular(visibility, distance, strength, dot, projectile, this);
        } else if (distance <= 1.0e-4) {
            visibility = ParticleAvoidance.applyAngular(visibility, distance, strength, 1, projectile, this);
        }
        return Math.max(0, Math.min(1, visibility));
    }
}
