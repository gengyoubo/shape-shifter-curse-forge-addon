package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;

/** 变羊渲染主体：用原版成年羊本体 + 羊毛双层模型绘制。 */
public final class SheepFormClientHooks {
    private static final ResourceLocation TEX_BODY = new ResourceLocation("textures/entity/sheep/sheep.png");
    private static final ResourceLocation TEX_FUR = new ResourceLocation("textures/entity/sheep/sheep_fur.png");

    private static BodyModel bodyModel;
    private static FurModel furModel;
    private static boolean initialized;

    private SheepFormClientHooks() {
    }

    private static void ensureInitialized() {
        if (initialized) return;
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        bodyModel = new BodyModel(models.bakeLayer(ModelLayers.SHEEP));
        furModel = new FurModel(models.bakeLayer(ModelLayers.SHEEP_FUR));
        initialized = true;
    }

    public static boolean renderAsSheep(LivingEntity entity, float yaw, float tickDelta,
                                        PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        ensureInitialized();
        if (bodyModel == null || furModel == null) return false;

        float limbDistance = Math.min(entity.walkAnimation.speed(tickDelta), 1.0F);
        float limbAngle = entity.walkAnimation.position(tickDelta) * 1.3F;
        float headYaw = Mth.rotLerp(tickDelta, entity.yHeadRotO, entity.yHeadRot)
                - Mth.rotLerp(tickDelta, entity.yBodyRotO, entity.yBodyRot);
        float headPitch = Mth.lerp(tickDelta, entity.xRotO, entity.getXRot());

        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float scale = 1.3F / 1.375F;
        matrices.scale(scale, scale, scale);

        bodyModel.pose(headYaw, headPitch, limbAngle, limbDistance);
        furModel.pose(headYaw, headPitch, limbAngle, limbDistance);

        RenderType bodyType = RenderType.entityCutoutNoCull(TEX_BODY);
        VertexConsumer bodyConsumer = vertexConsumers.getBuffer(bodyType);
        bodyModel.renderToBuffer(matrices, bodyConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        RenderType furType = RenderType.entityCutoutNoCull(TEX_FUR);
        VertexConsumer furConsumer = vertexConsumers.getBuffer(furType);
        furModel.renderToBuffer(matrices, furConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        matrices.popPose();
        return true;
    }

    private static final class BodyModel extends SheepModel<Sheep> {
        BodyModel(ModelPart root) {
            super(root);
        }

        void pose(float headYaw, float headPitch, float limbAngle, float limbDistance) {
            this.head.yRot = (float) Math.toRadians(-headYaw);
            this.head.xRot = (float) Math.toRadians(-headPitch);
            this.rightHindLeg.xRot = Mth.cos(limbAngle) * 1.4F * limbDistance;
            this.leftHindLeg.xRot = Mth.cos(limbAngle + Mth.PI) * 1.4F * limbDistance;
            this.rightFrontLeg.xRot = Mth.cos(limbAngle + Mth.PI) * 1.4F * limbDistance;
            this.leftFrontLeg.xRot = Mth.cos(limbAngle) * 1.4F * limbDistance;
        }
    }

    private static final class FurModel extends SheepFurModel<Sheep> {
        FurModel(ModelPart root) {
            super(root);
        }

        void pose(float headYaw, float headPitch, float limbAngle, float limbDistance) {
            this.head.yRot = (float) Math.toRadians(-headYaw);
            this.head.xRot = (float) Math.toRadians(-headPitch);
            this.rightHindLeg.xRot = Mth.cos(limbAngle) * 1.4F * limbDistance;
            this.leftHindLeg.xRot = Mth.cos(limbAngle + Mth.PI) * 1.4F * limbDistance;
            this.rightFrontLeg.xRot = Mth.cos(limbAngle + Mth.PI) * 1.4F * limbDistance;
            this.leftFrontLeg.xRot = Mth.cos(limbAngle) * 1.4F * limbDistance;
        }
    }
}
