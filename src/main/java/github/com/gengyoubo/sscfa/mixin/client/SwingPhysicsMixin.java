package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.ability.SwingMotion;
import github.com.gengyoubo.sscfa.client.SpiderMoonWeaverSwingClient;
import github.com.gengyoubo.sscfa.client.SpiderMoonWeaverSwingClient.LocalSwing;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 月织蛛「蛛丝荡漾」客户端摆锤物理。 */
@Mixin(LocalPlayer.class)
public abstract class SwingPhysicsMixin {

    @Shadow public Input input;

    @Unique private boolean ssca$jumpHeld;
    @Unique private boolean ssca$sneakHeld;
    @Unique private float ssca$moveForward;
    @Unique private float ssca$moveSideways;

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void ssca$swingHead(CallbackInfo ci) {
        if (!SpiderMoonWeaverSwingClient.isLocalActive() || this.input == null) {
            ssca$jumpHeld = false;
            ssca$sneakHeld = false;
            return;
        }
        ssca$jumpHeld = this.input.jumping;
        ssca$sneakHeld = this.input.shiftKeyDown;
        ssca$moveForward = this.input.forwardImpulse;
        ssca$moveSideways = this.input.leftImpulse;
        this.input.jumping = false;
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    private void ssca$blockSwingAutoJump(CallbackInfo ci) {
        if (SpiderMoonWeaverSwingClient.isLocalActive() && this.input != null) this.input.jumping = false;
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    private void ssca$swingTail(CallbackInfo ci) {
        LocalSwing swing = SpiderMoonWeaverSwingClient.getLocalSwing();
        if (swing == null || !swing.active) return;

        LocalPlayer self = (LocalPlayer) (Object) this;
        if (swing.state == SpiderMoonWeaverSwingClient.STATE_TETHER) return;
        if (swing.state != SpiderMoonWeaverSwingClient.STATE_SWINGING) return;

        final double MAX = github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.MAX_ROPE_REACH;
        Vec3 anchor = new Vec3(swing.anchorX, swing.anchorY, swing.anchorZ);
        Vec3 torso = self.position().add(0, 1.0, 0);
        Vec3 rope = torso.subtract(anchor);
        double distance = rope.length();
        boolean onGround = self.onGround();
        Vec3 velocity = self.getDeltaMovement();

        int reelIntent = 0;
        if (ssca$jumpHeld) {
            reelIntent = 1;
        } else if (ssca$sneakHeld && !onGround && swing.canExtend) {
            reelIntent = -1;
        }
        swing.reelIntent = reelIntent;

        if (onGround && distance <= MAX) {
            Vec3 direction = distance > 1.0e-6 ? anchor.subtract(torso).scale(1.0 / distance) : new Vec3(0, 1, 0);
            SwingMotion.Step step = SwingMotion.groundStep(velocity, direction, distance, swing.ropeLen, ssca$jumpHeld);
            swing.ropeLen = step.ropeLength();
            swing.reelIntent = step.reelIntent();
            self.setDeltaMovement(step.velocity());
            swing.physicsStarted = false;
            return;
        }

        self.setNoGravity(false);
        final double GRAVITY = 0.075;
        final double DAMPING = 0.99;
        Vec3 v;
        if (swing.physicsStarted) {
            v = swing.swingVelocity.scale(DAMPING);
            v = v.add(0, -GRAVITY, 0);
        } else {
            v = self.getDeltaMovement();
            swing.physicsStarted = true;
        }
        Vec3 realVelocity = self.getDeltaMovement();
        if (self.horizontalCollision) v = new Vec3(realVelocity.x, v.y, realVelocity.z);
        if (self.verticalCollision) v = new Vec3(v.x, realVelocity.y, v.z);

        Vec3 toAnchor = anchor.subtract(torso);
        double anchorDistance = toAnchor.length();
        Vec3 direction = anchorDistance > 0.001 ? toAnchor.scale(1.0 / anchorDistance) : new Vec3(0, 1, 0);
        double pushX = 0;
        double pushZ = 0;
        if (ssca$moveForward != 0f || ssca$moveSideways != 0f) {
            float yaw = self.getYRot();
            Vec3 forward = Vec3.directionFromRotation(0f, yaw);
            Vec3 right = Vec3.directionFromRotation(0f, yaw + 90f);
            Vec3 push = forward.scale(ssca$moveForward).add(right.scale(-ssca$moveSideways));
            double pushLength = Math.sqrt(push.x * push.x + push.z * push.z);
            if (pushLength > 0.01) {
                pushX = push.x / pushLength * 0.004;
                pushZ = push.z / pushLength * 0.004;
            }
        }

        SwingMotion.Step step = SwingMotion.step(v, direction, anchorDistance, swing.ropeLen,
                ssca$jumpHeld, ssca$sneakHeld, swing.canExtend, pushX, pushZ);
        v = step.velocity();
        swing.ropeLen = step.ropeLength();
        swing.reelIntent = step.reelIntent();

        self.setDeltaMovement(v);
        swing.swingVelocity = v;
    }

    @Unique private double ssca$prevTetherDist = -1;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void ssca$tetherAfterMovement(CallbackInfo ci) {
        LocalSwing swing = SpiderMoonWeaverSwingClient.getLocalSwing();
        if (swing != null && swing.active && swing.state == SpiderMoonWeaverSwingClient.STATE_TETHER) {
            ssca$tetherPhysics((LocalPlayer) (Object) this, swing);
        }
    }

    @Unique
    private void ssca$tetherPhysics(LocalPlayer self, LocalSwing swing) {
        if (self.level() == null) return;
        var target = self.level().getEntity(swing.tetherEntityId);
        if (target == null) {
            ssca$prevTetherDist = -1;
            return;
        }
        final double MIN = github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.MIN_ROPE_LEN;
        final double REEL = github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.REEL_SPEED;

        Vec3 playerPos = self.position().add(0, 1.0, 0);
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double distance = playerPos.distanceTo(targetPos);

        boolean approaching = ssca$prevTetherDist >= 0 && distance < ssca$prevTetherDist - 0.02;
        boolean fleeing = ssca$prevTetherDist >= 0 && distance > ssca$prevTetherDist + 0.02;
        ssca$prevTetherDist = distance;

        boolean stuck = self.horizontalCollision;
        boolean playerMoving = Math.abs(ssca$moveForward) > 0.01 || Math.abs(ssca$moveSideways) > 0.01;
        int reelIntent = 0;
        if (ssca$jumpHeld && !stuck) {
            double factor = approaching ? 1.5 : (fleeing ? 0.5 : 1.0);
            swing.ropeLen = Math.max(MIN, swing.ropeLen - REEL * factor);
            reelIntent = 1;
        }
        swing.reelIntent = reelIntent;

        double ropeLen = Math.max(swing.ropeLen, MIN);
        double softLen = Math.max(MIN, ropeLen
                - github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.TETHER_SOFT_BUFFER);
        final double PULL_GAIN = github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.TETHER_PULL_GAIN;
        final double HARD_GAIN = github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.TETHER_HARD_GAIN;
        double resist = 0.0;
        if (target instanceof net.minecraft.world.entity.LivingEntity living) {
            var instance = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (instance != null) resist = Mth.clamp(instance.getValue(), 0.0, 1.0);
        }
        if (distance > softLen) {
            Vec3 direction = targetPos.subtract(playerPos).normalize();
            if (self.onGround() && direction.y < 0) {
                direction = new Vec3(direction.x, 0, direction.z);
                double horizontal = direction.length();
                direction = horizontal > 0.01 ? direction.scale(1.0 / horizontal) : Vec3.ZERO;
            }
            AABB box = self.getBoundingBox();
            double dx = direction.x;
            double dy = direction.y;
            double dz = direction.z;
            if (dx != 0 && !self.level().noCollision(self, box.move(Math.signum(dx) * 0.25, 0, 0))) dx = 0;
            if (dy != 0 && !self.level().noCollision(self, box.move(0, Math.signum(dy) * 0.25, 0))) dy = 0;
            if (dz != 0 && !self.level().noCollision(self, box.move(0, 0, Math.signum(dz) * 0.25))) dz = 0;
            Vec3 validDirection = new Vec3(dx, dy, dz);
            double length = validDirection.length();
            if (length > 0.01) {
                validDirection = validDirection.scale(1.0 / length);
                double softPull = (distance - softLen) * PULL_GAIN;
                double hardPull = (distance > ropeLen) ? (distance - ropeLen) * HARD_GAIN : 0.0;
                double pull = (playerMoving ? hardPull : (softPull + hardPull)) * (0.2 + 0.8 * resist);
                double cap = (distance > ropeLen) ? 0.9 : 0.6;
                Vec3 velocity = self.getDeltaMovement();
                double toward = velocity.dot(validDirection);
                if (toward < pull) velocity = velocity.add(validDirection.scale(pull - toward));
                double speed = velocity.length();
                if (speed > cap) velocity = velocity.scale(cap / speed);
                self.setDeltaMovement(velocity);
                self.hasImpulse = true;
            }
        }
    }
}
