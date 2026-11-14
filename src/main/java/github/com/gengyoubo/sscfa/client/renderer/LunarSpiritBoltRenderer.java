package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.entity.LunarSpiritBoltEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 月灵光弹：程序化八面体发光体，按变体染色并自转。 */
public class LunarSpiritBoltRenderer extends EntityRenderer<LunarSpiritBoltEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("my_addon", "textures/entity/moon_spirit_green.png");
    private static final float BOLT_SIZE = 0.18f;
    private static final Vec3 TOP = new Vec3(0, 1, 0);
    private static final Vec3 BOTTOM = new Vec3(0, -1, 0);
    private static final Vec3 EQ0 = new Vec3(0.71, 0, 0.71);
    private static final Vec3 EQ1 = new Vec3(-0.71, 0, 0.71);
    private static final Vec3 EQ2 = new Vec3(-0.71, 0, -0.71);
    private static final Vec3 EQ3 = new Vec3(0.71, 0, -0.71);

    public LunarSpiritBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(LunarSpiritBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        int variant = entity.getVariant();
        float r = variant == 0 ? 1.0f : variant == 1 ? 0.55f : 0.62f;
        float g = variant == 0 ? 0.62f : variant == 1 ? 0.88f : 0.92f;
        float b = variant == 0 ? 0.75f : variant == 1 ? 0.96f : 0.65f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 30.0f));
        float size = BOLT_SIZE * (1.0f + (float) Math.sin(age * 0.5f) * 0.1f);
        poseStack.scale(size, size, size);
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        renderOctahedron(consumer, pose, r, g, b);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderOctahedron(VertexConsumer consumer, PoseStack.Pose pose, float r, float g, float b) {
        tri(consumer, pose, TOP, EQ0, EQ1, r, g, b);
        tri(consumer, pose, TOP, EQ1, EQ2, r, g, b);
        tri(consumer, pose, TOP, EQ2, EQ3, r, g, b);
        tri(consumer, pose, TOP, EQ3, EQ0, r, g, b);
        tri(consumer, pose, BOTTOM, EQ1, EQ0, r, g, b);
        tri(consumer, pose, BOTTOM, EQ2, EQ1, r, g, b);
        tri(consumer, pose, BOTTOM, EQ3, EQ2, r, g, b);
        tri(consumer, pose, BOTTOM, EQ0, EQ3, r, g, b);
    }

    private static void tri(VertexConsumer consumer, PoseStack.Pose pose,
                            Vec3 a, Vec3 b, Vec3 c, float r, float g, float blue) {
        vertex(consumer, pose, a, r, g, blue);
        vertex(consumer, pose, b, r, g, blue);
        vertex(consumer, pose, c, r, g, blue);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 v, float r, float g, float b) {
        consumer.vertex(pose.pose(), (float) v.x, (float) v.y, (float) v.z)
                .color(r, g, b, 1.0f)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(LunarSpiritBoltEntity entity) {
        return TEXTURE;
    }
}
