package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.client.model.AxolotlShifterModel;
import github.com.gengyoubo.sscfa.entity.AxolotlShifterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;
import java.util.Random;

public class AxolotlShifterRenderer extends GeoEntityRenderer<AxolotlShifterEntity> {

	private static final float MODEL_SCALE = 0.9f;

	private static final String BODY_TAIL_CHAIN = "tail";
	private static final int BODY_TAIL_LENGTH = 4;
	private static final String[] HEAD_TAIL_CHAINS = {"tail_head_a", "tail_head_b", "tail_head_c",
			"tail_head_d", "tail_head_e", "tail_head_f"};

	private static final float SWAY_RATE = 0.33333334f * 0.5f;
	private static final float SWAY_SCALE = 0.05f;

	private static final WeakEntityStateMap<AxolotlShifterEntity, float[]> TAIL_STATES = new WeakEntityStateMap<>();
	private static final String EYE_ROOT_BONE = "eyeRoot";
	private static final float EYE_OPEN_SCALE = 1.0f;
	private static final float EYE_CLOSED_SCALE = 0.01f;
	private static final int BLINK_MIN_INTERVAL = 60;
	private static final int BLINK_MAX_INTERVAL = 140;
	private static final int BLINK_TICKS = 4;
	private static final WeakEntityStateMap<AxolotlShifterEntity, int[]> BLINK_STATES = new WeakEntityStateMap<>();

	private float cachedLimbAngle;
	private float cachedLimbDistance;
	private float cachedHeadYaw;
	private float cachedHeadPitch;
	private float cachedPartialTick;
	private float cachedAge;

	public AxolotlShifterRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new AxolotlShifterModel());
		this.shadowRadius = 0.35f;
	}

	@Override
	public void render(AxolotlShifterEntity entity, float entityYaw, float partialTick,
	                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		if (entity.isRemoved()) {
			TAIL_STATES.remove(entity);
			BLINK_STATES.remove(entity);
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

		float[] state = TAIL_STATES.computeIfAbsent(entity, k -> new float[6]);
		float targetDrag = Mth.lerp(partialTick, state[1], state[0]);
		state[4] = Mth.lerp(0.04f, state[4], targetDrag);
		float targetVerticalDrag = Mth.lerp(partialTick, state[3], state[2]);
		state[5] = Mth.lerp(0.04f, state[5], targetVerticalDrag);

		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

		updateTailDrag(entity, state);
	}

	@Override
	protected void applyRotations(AxolotlShifterEntity animatable, PoseStack poseStack,
	                              float ageInTicks, float rotationYaw, float partialTick) {
		super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
		poseStack.mulPose(Axis.YP.rotationDegrees(180f));
	}

	@Override
	public void preRender(PoseStack poseStack, AxolotlShifterEntity animatable, BakedGeoModel model,
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

	private void processModel(AxolotlShifterEntity entity) {
		resetBone("bipedHead");
		resetBone("bipedBody");
		resetBone("bipedLeftArm");
		resetBone("bipedRightArm");
		resetBone("bipedLeftLeg");
		resetBone("bipedRightLeg");

		float headPitchRad = cachedHeadPitch * Mth.DEG_TO_RAD * 0.5f;
		float headYawRad = cachedHeadYaw * Mth.DEG_TO_RAD * 0.5f;
		setRotation("bipedHead", headPitchRad, headYawRad, 0);

		float rightArmPitch = Mth.cos(cachedLimbAngle * 0.6662f + (float) Math.PI)
				* 2.0f * cachedLimbDistance * 0.5f;
		float leftArmPitch = Mth.cos(cachedLimbAngle * 0.6662f)
				* 2.0f * cachedLimbDistance * 0.5f;

		float handSwing = entity.getAttackAnim(cachedPartialTick);
		float bodyRotY = 0;
		if (handSwing > 0) {
			bodyRotY = Mth.sin(Mth.sqrt(handSwing) * (float) (Math.PI * 2)) * 0.2f;
			rightArmPitch -= Mth.sin(Mth.sqrt(handSwing) * (float) Math.PI) * 1.2f;
		}
		setRotation("bipedBody", 0, bodyRotY, 0);

		setRotation("bipedRightArm", rightArmPitch, 0, 0);
		setRotation("bipedLeftArm", leftArmPitch, 0, 0);

		float rightLegPitch = Mth.cos(cachedLimbAngle * 0.6662f)
				* 1.4f * cachedLimbDistance;
		float leftLegPitch = Mth.cos(cachedLimbAngle * 0.6662f + (float) Math.PI)
				* 1.4f * cachedLimbDistance;
		setRotation("bipedRightLeg", rightLegPitch, 0, 0);
		setRotation("bipedLeftLeg", leftLegPitch, 0, 0);

		float[] state = TAIL_STATES.getOrDefault(entity, new float[6]);
		setTailRotations(cachedLimbAngle, cachedLimbDistance, cachedAge, state[4], state[5]);
		setHeadTailRotations(cachedHeadYaw, cachedAge, state[4], state[5]);

		updateEyeBlink(entity);
	}

	private void updateEyeBlink(AxolotlShifterEntity entity) {
		Optional<GeoBone> eyeRootOpt = getGeoModel().getBone(EYE_ROOT_BONE);
		if (eyeRootOpt.isEmpty()) return;
		GeoBone eyeRoot = eyeRootOpt.get();

		int[] blink = BLINK_STATES.computeIfAbsent(entity, k -> {
			Random r = new Random();
			return new int[]{BLINK_MIN_INTERVAL + r.nextInt(BLINK_MAX_INTERVAL - BLINK_MIN_INTERVAL + 1), 0, 0, -1};
		});

		if (blink[3] == -1 || entity.tickCount != blink[3]) {
			int elapsed = Mth.clamp(entity.tickCount - blink[3], 1, 100);
			if (blink[3] == -1) elapsed = 1;
			blink[3] = entity.tickCount;
			for (int i = 0; i < elapsed; i++) {
				if (blink[2] == 1) {
					blink[1]++;
					if (blink[1] >= BLINK_TICKS) {
						blink[2] = 0;
						blink[1] = 0;
						Random r = new Random();
						blink[0] = BLINK_MIN_INTERVAL + r.nextInt(BLINK_MAX_INTERVAL - BLINK_MIN_INTERVAL + 1);
					}
				} else {
					if (blink[0] > 0) blink[0]--;
					if (blink[0] <= 0) {
						blink[2] = 1;
						blink[1] = 0;
					}
				}
			}
		}

		float scaleY = EYE_OPEN_SCALE;
		if (blink[2] == 1) {
			float progress = Mth.clamp(blink[1] / (float) BLINK_TICKS, 0.0f, 1.0f);
			float closeAmount = progress <= 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
			scaleY = Mth.lerp(closeAmount, EYE_OPEN_SCALE, EYE_CLOSED_SCALE);
		}
		eyeRoot.setScaleY(scaleY);
	}

	private void setTailRotations(float limbAngle, float limbDistance, float age,
	                              float tailDragAmount, float tailDragAmountVertical) {
		Optional<GeoBone> firstBone = getGeoModel().getBone(BODY_TAIL_CHAIN + "_0");
		if (firstBone.isEmpty()) return;

		GeoBone tail = firstBone.get();
		float tailSway = SWAY_SCALE * Mth.cos(age * SWAY_RATE
				+ ((float) Math.PI / 3.0f) * 0.75f);
		float tailBalance = Mth.cos(limbAngle * 0.6662f) * 0.325f * limbDistance;

		tail.setRotY(-Mth.lerp(limbDistance, tailSway, tailBalance)
				- tailDragAmount * 0.75f);
		tail.setRotX(-tailDragAmountVertical * 0.75f);

		float offset = 0.0f;
		for (int i = 1; i < BODY_TAIL_LENGTH; i++) {
			Optional<GeoBone> nextBone = getGeoModel().getBone(BODY_TAIL_CHAIN + "_" + i);
			if (nextBone.isEmpty()) continue;

			GeoBone bone = nextBone.get();
			bone.setRotY(-Mth.lerp(limbDistance,
					SWAY_SCALE * Mth.cos(age * SWAY_RATE
							- ((float) Math.PI / 3.0f) * offset),
					0.0f) - tailDragAmount * 0.75f);
			bone.setRotX(-tailDragAmountVertical * 0.75f * (offset + 0.75f));
			offset += 0.75f;
		}
	}

	private void setHeadTailRotations(float headAngle, float age,
	                                  float tailDragAmount, float tailDragAmountVertical) {
		for (String prefix : HEAD_TAIL_CHAINS) {
			Optional<GeoBone> firstBone = getGeoModel().getBone(prefix + "_0");
			if (firstBone.isEmpty()) continue;

			GeoBone gill = firstBone.get();
			float tailSway = SWAY_SCALE * Mth.cos(age * SWAY_RATE
					+ ((float) Math.PI / 3.0f) * 0.75f);
			float tailBalance = Mth.cos(headAngle * 0.6662f) * 0.325f * 0.1f;

			gill.setRotY(-Mth.lerp(0.1f, tailSway, tailBalance)
					- tailDragAmount * 0.75f);
			gill.setRotX(-tailDragAmountVertical * 0.75f);
		}
	}

	private void updateTailDrag(AxolotlShifterEntity entity, float[] state) {
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
