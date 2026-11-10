package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.entity.LaserBeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 荧光幼灵法阵激光期间带加速度的视角平滑限速。 */
@Mixin(Entity.class)
public abstract class ViewRateLimitMixin {

    @Shadow public float yRotO;
    @Shadow public float xRotO;

    private static final double RATE_START = 25.0;
    private static final double RATE_END = 5.0;
    private static final double FROSTSPINE_LIMIT = 50.0;
    private static final int CHARGE_5S_TICK = 100;
    private static final int CHARGE_7S_TICK = 140;
    private static final double ACCEL_TIME_SEC = 0.5;
    private static final double DECEL_TIME_SEC = 1.5;

    @Unique private static double ssca$velYaw = 0.0;
    @Unique private static double ssca$velPitch = 0.0;
    @Unique private static long ssca$lastNanos = 0L;

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void ssca$smoothView(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (client == null || self != client.player) return;
        LocalPlayer player = client.player;
        if (github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked(player)) {
            ssca$velYaw = 0.0;
            ssca$velPitch = 0.0;
            ssca$lastNanos = 0L;
            ci.cancel();
            return;
        }

        LaserBeamEntity laser = LaserBeamEntity.getActiveForClient(player);
        boolean frostForge = laser == null
                && github.com.gengyoubo.sscfa.client.FrostSpikeClient.isSecondaryCharging()
                && github.com.gengyoubo.sscfa.util.FormUtils.isForm(
                        player, github.com.gengyoubo.sscfa.util.FormIdentifiers.SNOW_FOX_FROSTSPINE);
        if (laser == null && !frostForge) {
            ssca$velYaw = 0.0;
            ssca$velPitch = 0.0;
            ssca$lastNanos = 0L;
            return;
        }

        double limit = frostForge ? FROSTSPINE_LIMIT : currentLimit(laser);
        long now = System.nanoTime();
        if (ssca$lastNanos == 0L) ssca$lastNanos = now;
        double dt = Math.min(0.1, (now - ssca$lastNanos) / 1.0e9);
        ssca$lastNanos = now;

        double inYaw = cursorDeltaX;
        double inPitch = cursorDeltaY;
        double inputMagnitude = Math.sqrt(inYaw * inYaw + inPitch * inPitch);
        boolean hasInput = inputMagnitude > 0.5;

        if (hasInput) {
            double targetYaw = inYaw / inputMagnitude * limit;
            double targetPitch = inPitch / inputMagnitude * limit;
            double maxStep = limit / ACCEL_TIME_SEC * dt;
            ssca$velYaw = approach(ssca$velYaw, targetYaw, maxStep);
            ssca$velPitch = approach(ssca$velPitch, targetPitch, maxStep);
        } else {
            double maxStep = limit / DECEL_TIME_SEC * dt;
            ssca$velYaw = approach(ssca$velYaw, 0.0, maxStep);
            ssca$velPitch = approach(ssca$velPitch, 0.0, maxStep);
        }

        double speed = Math.sqrt(ssca$velYaw * ssca$velYaw + ssca$velPitch * ssca$velPitch);
        if (speed > limit && speed > 1.0e-6) {
            double scale = limit / speed;
            ssca$velYaw *= scale;
            ssca$velPitch *= scale;
        }

        double deltaYaw = ssca$velYaw * dt;
        double deltaPitch = ssca$velPitch * dt;
        if (Math.abs(deltaYaw) < 1.0e-4 && Math.abs(deltaPitch) < 1.0e-4) {
            ci.cancel();
            return;
        }
        player.setYRot(player.getYRot() + (float) deltaYaw);
        player.setXRot(Mth.clamp(player.getXRot() + (float) deltaPitch, -90.0F, 90.0F));
        this.yRotO += (float) deltaYaw;
        this.xRotO = Mth.clamp(this.xRotO + (float) deltaPitch, -90.0F, 90.0F);
        ci.cancel();
    }

    private static double currentLimit(LaserBeamEntity laser) {
        int phaseId = laser.getPhaseId();
        int phaseTick = laser.getPhaseTick();
        if (phaseId == 0) {
            if (phaseTick < CHARGE_5S_TICK) return RATE_START;
            if (phaseTick < CHARGE_7S_TICK) {
                double progress = (phaseTick - CHARGE_5S_TICK) / (double) (CHARGE_7S_TICK - CHARGE_5S_TICK);
                return RATE_START + (RATE_END - RATE_START) * progress;
            }
            return RATE_END;
        }
        return RATE_END;
    }

    private static double approach(double current, double target, double maxStep) {
        double difference = target - current;
        if (Math.abs(difference) <= maxStep) return target;
        return current + Math.signum(difference) * maxStep;
    }
}
