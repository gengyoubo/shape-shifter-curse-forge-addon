package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.sscfa.client.particle.AsyncParticleCompatibility;
import github.com.gengyoubo.sscfa.client.particle.FadedParticleBuffer;
import github.com.gengyoubo.sscfa.client.particle.FirstPersonParticles;
import github.com.gengyoubo.sscfa.client.particle.OwnedDecoration;
import github.com.gengyoubo.sscfa.client.particle.ParticleAvoidance;
import github.com.gengyoubo.sscfa.client.particle.ParticleOpacity;
import github.com.gengyoubo.sscfa.client.particle.ParticleOwnership;
import github.com.gengyoubo.sscfa.client.particle.ParticleRenderRouting;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

/** 绗竴浜虹О瑁呴グ绮掑瓙锛氬綊灞炵户鎵裤€佹寜甯ф妸娣″嚭绮掑瓙璺敱鍒板崐閫忔槑鎵规骞舵寜鍙搴︾粯鍒躲€?*/
@Mixin(ParticleEngine.class)
public abstract class DecorationParticleManagerMixin {
    @Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;

    @Unique private Map<ParticleRenderType, Queue<Particle>> ssca$frameParticles;
    @Unique private FadedParticleBuffer ssca$fadedParticles;
    @Unique private ParticleRenderType ssca$drawingSheet;
    @Unique private Camera ssca$routingCamera;
    @Unique private Vec3 ssca$routingEye;
    @Unique private float ssca$routingYaw;
    @Unique private float ssca$routingPitch;
    @Unique private float ssca$routingDelta;
    @Unique private long ssca$routingTick = Long.MIN_VALUE;
    @Unique private Map<ParticleRenderType, Queue<Particle>> ssca$routingResult;

    @Unique
    private static Minecraft client() {
        return Minecraft.getInstance();
    }

    @Unique
    private boolean ssca$sameFrameState(Camera camera, float tickDelta) {
        return ssca$routingResult != null
                && client().level != null
                && ssca$routingTick == client().level.getGameTime()
                && ssca$routingDelta == tickDelta
                && camera == ssca$routingCamera
                && camera.getYRot() == ssca$routingYaw && camera.getXRot() == ssca$routingPitch
                && camera.getPosition().equals(ssca$routingEye);
    }

    @Inject(method = "add", at = @At("HEAD"))
    private void ssca$tag(Particle particle, CallbackInfo ci) {
        FirstPersonParticles.tag(particle);
    }

    @WrapOperation(method = "tickParticle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void ssca$inheritDecoration(Particle particle, Operation<Void> original) {
        var owner = particle instanceof ParticleOwnership owned ? owned.ssca$getDecorationOwner() : null;
        if (owner == null) {
            original.call(particle);
        } else if (particle instanceof ParticleOwnership own && own.ssca$isProjectileDecoration()) {
            FirstPersonParticles.emitProjectile(owner, () -> original.call(particle));
        } else {
            FirstPersonParticles.emit(owner, () -> original.call(particle));
        }
    }

    @Inject(remap = false, method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("HEAD"))
    private void ssca$prepareTranslucentBatch(PoseStack matrices, MultiBufferSource.BufferSource consumers,
                                              LightTexture lightmap, Camera camera, float tickDelta, Frustum frustum,
                                              CallbackInfo ci) {
        ssca$frameParticles = null;
        ssca$drawingSheet = null;
        if (ssca$fadedParticles != null) ssca$fadedParticles.discard();
        AsyncParticleCompatibility.end();
        Minecraft client = client();
        if (client.player == null || camera.getEntity() != client.player || camera.isDetached()
                || !client.options.getCameraType().isFirstPerson()) return;
        ParticleAvoidance.Strength strength = SSCAddonConfig.client().firstPersonParticleAvoidance;
        if (strength == null || strength == ParticleAvoidance.Strength.OFF) return;
        AsyncParticleCompatibility.begin(camera, tickDelta);
        AsyncParticleCompatibility.snapshotStrength(strength);
        if (ssca$sameFrameState(camera, tickDelta)) {
            ssca$frameParticles = ssca$routingResult;
            return;
        }
        ssca$routingResult = ParticleRenderRouting.forFrame(particles,
                ParticleRenderType.PARTICLE_SHEET_OPAQUE, ParticleRenderType.PARTICLE_SHEET_LIT,
                ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
                particle -> particle instanceof OwnedDecoration owned
                        && owned.ssca$cameraVisibility(camera, tickDelta) < 1);
        ssca$frameParticles = ssca$routingResult;
        ssca$routingCamera = camera;
        ssca$routingEye = camera.getPosition();
        ssca$routingYaw = camera.getYRot();
        ssca$routingPitch = camera.getXRot();
        ssca$routingDelta = tickDelta;
        ssca$routingTick = client.level == null ? Long.MIN_VALUE : client.level.getGameTime();
    }

    @ModifyExpressionValue(remap = false, method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;particles:Ljava/util/Map;"))
    private Map<ParticleRenderType, Queue<Particle>> ssca$renderQueues(Map<ParticleRenderType, Queue<Particle>> original) {
        return ssca$frameParticles == null ? original : ssca$frameParticles;
    }

    @WrapOperation(remap = false, method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleRenderType;begin(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
    private void ssca$beginSheet(ParticleRenderType sheet, com.mojang.blaze3d.vertex.BufferBuilder buffer,
                                 TextureManager textures, Operation<Void> original) {
        ssca$drawingSheet = sheet;
        original.call(sheet, buffer, textures);
    }

    @WrapOperation(remap = false, method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleRenderType;end(Lcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void ssca$drawSheet(ParticleRenderType sheet, Tesselator tessellator, Operation<Void> original) {
        try {
            original.call(sheet, tessellator);
            if (ssca$fadedParticles != null) ssca$fadedParticles.draw();
        } finally {
            ssca$drawingSheet = null;
            if (ssca$fadedParticles != null) ssca$fadedParticles.discard();
        }
    }

    @WrapOperation(remap = false, method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private void ssca$drawWithOpacity(Particle particle, VertexConsumer vertices, Camera camera, float tickDelta,
                                      Operation<Void> original) {
        if (!(particle instanceof OwnedDecoration decoration)) {
            original.call(particle, vertices, camera, tickDelta);
            return;
        }
        float visibility = decoration.ssca$cameraVisibility(camera, tickDelta);
        if (visibility >= 1) {
            original.call(particle, vertices, camera, tickDelta);
            return;
        }
        if (visibility <= 0) return;
        VertexConsumer target = vertices;
        if (ssca$drawingSheet == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT) {
            if (ssca$fadedParticles == null) ssca$fadedParticles = new FadedParticleBuffer();
            target = ssca$fadedParticles.vertices();
        }
        VertexConsumer fadedVertices = target;
        ParticleOpacity.draw(decoration, visibility,
                () -> original.call(particle, fadedVertices, camera, tickDelta));
    }

    @Inject(remap = false, method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("RETURN"))
    private void ssca$clearRenderQueues(PoseStack matrices, MultiBufferSource.BufferSource consumers,
                                        LightTexture lightmap, Camera camera, float tickDelta, Frustum frustum,
                                        CallbackInfo ci) {
        ssca$frameParticles = null;
        AsyncParticleCompatibility.end();
    }
}

