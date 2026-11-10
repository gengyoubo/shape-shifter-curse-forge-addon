package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.client.model.GhostCatModel;
import github.com.gengyoubo.sscfa.entity.GhostCatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

/** 幽灵野猫渲染器：程序化四足骨骼驱动。 */
public class GhostCatRenderer extends GeoEntityRenderer<GhostCatEntity> {
    private static final float MODEL_SCALE = 0.75f;
    private static final float HALF_PI = (float) (Math.PI / 2);

    private static final String BODY_TAIL_CHAIN = "tail";
    private static final int BODY_TAIL_LENGTH = 4;
    private static final String[] EAR_CHAINS = {"ear_a", "ear_b"};
    private static final float SWAY_RATE = 0.33333334f * 0.5f;
    private static final float SWAY_SCALE = 0.05f;

    private static final WeakEntityStateMap<GhostCatEntity, float[]> TAIL_STATES = new WeakEntityStateMap<>();

    private float cachedLimbAngle;
    private float cachedLimbDistance;
    private float cachedHeadYaw;
    private float cachedHeadPitch;
    private float cachedPartialTick;
    private float cachedAge;

    public GhostCatRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostCatModel());
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(GhostCatEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isRemoved()) {
            TAIL_STATES.remove(entity);
            return;
        }
        cachedPartialTick = partialTick;
        cachedLimbDistance = entity.walkAnimation.speed(partialTick);
        cachedLimbAngle = entity.walkAnimation.position(partialTick);
        if (cachedLimbDistance > 1.0f) cachedLimbDistance = 1.0f;

        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        cachedHeadYaw = headYaw - bodyYaw;
        cachedHeadPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        cachedAge = entity.tickCount + partialTick;

        float[] state = TAIL_STATES.computeIfAbsent(entity, key -> new float[6]);
        float targetDrag = Mth.lerp(partialTick, state[1], state[0]);
        state[4] = Mth.lerp(0.04f, state[4], targetDrag);
        float targetVerticalDrag = Mth.lerp(partialTick, state[3], state[2]);
        state[5] = Mth.lerp(0.04f, state[5], targetVerticalDrag);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        updateTailDrag(entity, state);
    }

    @Override
    protected void applyRotations(GhostCatEntity animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
    }

    @Override
    public void preRender(PoseStack poseStack, GhostCatEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        if (!isReRender) {
            poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        if (!isReRender) {
            processModel(animatable);
        }
    }

    private void processModel(GhostCatEntity entity) {
        resetBone("bipedHead");
        resetBone("bipedBody");
        resetBone("bipedLeftArm");
        resetBone("bipedRightArm");
        resetBone("bipedLeftLeg");
        resetBone("bipedRightLeg");

        setRotation("bipedBody", HALF_PI, 0, 0);
        getGeoModel().getBone("bipedBody").ifPresent(bone -> bone.setPosY(-8f));

        float headPitchRad = cachedHeadPitch * Mth.DEG_TO_RAD * 0.5f;
        float headYawRad = cachedHeadYaw * Mth.DEG_TO_RAD * 0.5f;
        setRotation("bipedHead", headPitchRad - HALF_PI, 0, headYawRad);

        float rightArmPitch = -HALF_PI + Mth.cos(cachedLimbAngle * 0.6662f + (float) Math.PI)
                * 2.0f * cachedLimbDistance * 0.5f;
        float leftArmPitch = -HALF_PI + Mth.cos(cachedLimbAngle * 0.6662f)
                * 2.0f * cachedLimbDistance * 0.5f;

        float handSwing = entity.getAttackAnim(cachedPartialTick);
        float bodyRotY = 0;
        if (handSwing > 0) {
            bodyRotY = Mth.sin(Mth.sqrt(handSwing) * (float) (Math.PI * 2)) * 0.2f;
            rightArmPitch -= Mth.sin(Mth.sqrt(handSwing) * (float) Math.PI) * 1.2f;
        }
        setRotation("bipedBody", HALF_PI, bodyRotY, 0);
        setRotation("bipedRightArm", rightArmPitch, 0, 0);
        setRotation("bipedLeftArm", leftArmPitch, 0, 0);

        float rightLegPitch = Mth.cos(cachedLimbAngle * 0.6662f) * 1.4f * cachedLimbDistance;
        float leftLegPitch = Mth.cos(cachedLimbAngle * 0.6662f + (float) Math.PI) * 1.4f * cachedLimbDistance;
        setRotation("bipedRightLeg", rightLegPitch, 0, 0);
        setRotation("bipedLeftLeg", leftLegPitch, 0, 0);
        getGeoModel().getBone("bipedRightLeg").ifPresent(bone -> bone.setPosZ(-14f));
        getGeoModel().getBone("bipedLeftLeg").ifPresent(bone -> bone.setPosZ(-14f));

        float[] state = TAIL_STATES.getOrDefault(entity, new float[6]);
        setTailRotations(cachedLimbAngle, cachedLimbDistance, cachedAge, state[4], state[5]);
        setHeadTailRotations(cachedHeadYaw, cachedAge, state[4], state[5]);
    }

    private void setTailRotations(float limbAngle, float limbDistance, float age,
                                  float tailDragAmount, float tailDragAmountVertical) {
        Optional<GeoBone> firstBoneOpt = getGeoModel().getBone(BODY_TAIL_CHAIN + "_0");
        if (firstBoneOpt.isEmpty()) return;
        GeoBone tail = firstBoneOpt.get();
        float tailSway = SWAY_SCALE * Mth.cos(age * SWAY_RATE + ((float) Math.PI / 3.0f) * 0.75f);
        float tailBalance = Mth.cos(limbAngle * 0.6662f) * 0.325f * limbDistance;
        tail.setRotZ(Mth.lerp(limbDistance, tailSway, tailBalance) + tailDragAmount * 0.75f);
        tail.setRotX(-tailDragAmountVertical * 0.75f);

        float offset = 0.0f;
        for (int i = 1; i < BODY_TAIL_LENGTH; i++) {
            Optional<GeoBone> nextBone = getGeoModel().getBone(BODY_TAIL_CHAIN + "_" + i);
            if (nextBone.isEmpty()) continue;
            GeoBone bone = nextBone.get();
            bone.setRotZ(Mth.lerp(limbDistance,
                    SWAY_SCALE * Mth.cos(age * SWAY_RATE - ((float) Math.PI / 3.0f) * offset),
                    0.0f) + tailDragAmount * 0.75f);
            bone.setRotX(-tailDragAmountVertical * 0.75f * (offset + 0.75f));
            offset += 0.75f;
        }
    }

    private void setHeadTailRotations(float headAngle, float age,
                                      float tailDragAmount, float tailDragAmountVertical) {
        for (String prefix : EAR_CHAINS) {
            Optional<GeoBone> firstBone = getGeoModel().getBone(prefix + "_0");
            if (firstBone.isEmpty()) continue;
            GeoBone ear = firstBone.get();
            float tailSway = SWAY_SCALE * Mth.cos(age * SWAY_RATE + ((float) Math.PI / 3.0f) * 0.75f);
            float tailBalance = Mth.cos(headAngle * 0.6662f) * 0.325f * 0.1f;
            ear.setRotY(-Mth.lerp(0.1f, tailSway, tailBalance) - tailDragAmount * 0.75f);
            ear.setRotX(-tailDragAmountVertical * 0.75f);
        }
    }

    private void updateTailDrag(GhostCatEntity entity, float[] state) {
        state[1] = state[0];
        state[0] *= 0.75f;
        state[0] -= (float) (Math.toRadians(entity.yBodyRot - entity.yBodyRotO) * 0.55f);
        state[0] = Mth.clamp(state[0], -1.6f, 1.6f);

        float verticalSpeed = (float) entity.getDeltaMovement().y;
        float targetVerticalDrag = Mth.clamp(verticalSpeed * 1.5f, -1.6f, 1.6f);
        state[3] = state[2];
        state[2] *= 0.8f;
        state[2] += targetVerticalDrag * 0.15f;
        state[2] = Mth.clamp(state[2], -1.6f, 1.6f);
    }

    private void resetBone(String boneName) {
        getGeoModel().getBone(boneName).ifPresent(bone -> {
            bone.setPosX(0);
            bone.setPosY(0);
            bone.setPosZ(0);
            bone.setRotX(0);
            bone.setRotY(0);
            bone.setRotZ(0);
            bone.setScaleX(1);
            bone.setScaleY(1);
            bone.setScaleZ(1);
        });
    }

    private void setRotation(String boneName, float rx, float ry, float rz) {
        getGeoModel().getBone(boneName).ifPresent(bone -> {
            bone.setRotX(rx);
            bone.setRotY(ry);
            bone.setRotZ(rz);
        });
    }
}
