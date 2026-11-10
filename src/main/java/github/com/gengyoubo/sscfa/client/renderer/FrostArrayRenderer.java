package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.entity.FrostArrayEntity;
import github.com.gengyoubo.sscfa.entity.FrostThornEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** 寒棘狐「凝棘」蓄力法阵：青蓝雪花法阵 + 中央预览冰锥。 */
public class FrostArrayRenderer extends EntityRenderer<FrostArrayEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    private static final float[] ICE_CYAN = {0.45f, 0.85f, 1.00f, 0.85f};
    private static final float[] ICE_BLUE = {0.35f, 0.60f, 1.00f, 0.80f};
    private static final float[] ICE_WHITE = {0.85f, 0.95f, 1.00f, 0.90f};

    private final ItemRenderer itemRenderer;
    private final ItemStack previewThorn;

    public FrostArrayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.previewThorn = new ItemStack(SscAddon.FROST_THORN);
        this.previewThorn.getOrCreateTag().putInt("CustomModelData", 1);
    }

    @Override
    public void render(FrostArrayEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource buffers, int light) {
        Entity owner = entity.level().getEntity(entity.getTrackedOwnerId());
        if (!(owner instanceof LivingEntity living)) return;

        float ownerYaw = Mth.lerp(tickDelta, owner.yRotO, owner.getYRot());
        float ownerPitch = Mth.lerp(tickDelta, owner.xRotO, owner.getXRot());
        double ex = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double ey = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double ez = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        Vec3 hover = FrostThornEntity.hoverTarget(living, 0);

        matrices.pushPose();
        matrices.translate(hover.x - ex, hover.y - ey, hover.z - ez);
        matrices.mulPose(Axis.YP.rotationDegrees(-ownerYaw));
        matrices.mulPose(Axis.XP.rotationDegrees(ownerPitch));

        float age = entity.tickCount + tickDelta;
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        drawSnowArray(buffer, matrices.last().pose(), matrices.last().normal(), age);
        matrices.popPose();

        Vec3 eye = living.getEyePosition();
        Vec3 aim = eye.add(living.getViewVector(1.0f).scale(16.0));
        Vec3 fly = aim.subtract(hover).normalize();
        double horizontal = Math.sqrt(fly.x * fly.x + fly.z * fly.z);
        float flyYaw = (float) (Math.atan2(-fly.x, fly.z) * (180.0 / Math.PI));
        float flyPitch = (float) (Math.atan2(-fly.y, horizontal) * (180.0 / Math.PI));
        matrices.pushPose();
        matrices.translate(hover.x - ex, hover.y - ey, hover.z - ez);
        float scale = 0.25f * (1.0f + 1.0f * entity.getLevel());
        matrices.scale(scale, scale, scale);
        matrices.mulPose(Axis.YP.rotationDegrees(-flyYaw + 180.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(-flyPitch));
        matrices.translate((8.0 - 1.5) / 16.0, (8.0 - 0.5) / 16.0, (8.0 - 3.0) / 16.0);
        this.itemRenderer.renderStatic(this.previewThorn, ItemDisplayContext.GROUND, 0xF000F0,
                OverlayTexture.NO_OVERLAY, matrices, buffers, entity.level(), entity.getId());
        matrices.popPose();

        super.render(entity, yaw, tickDelta, matrices, buffers, light);
    }

    private void drawSnowArray(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float age) {
        float radius = 0.5f;
        float spin = age * 0.02f;
        float width = 0.02f;
        ringBand(buffer, pose, normal, radius, width * 1.3f, 48, spin, ICE_CYAN);
        ringBand(buffer, pose, normal, radius * 0.55f, width, 36, -spin * 1.4f, ICE_BLUE);
        for (int i = 0; i < 6; i++) {
            double angle = spin + Math.PI * 2 * i / 6;
            float tx = (float) Math.cos(angle) * radius * 0.92f;
            float ty = (float) Math.sin(angle) * radius * 0.92f;
            thickSeg(buffer, pose, normal, 0f, 0f, tx, ty, width * 0.8f, ICE_WHITE);
            float bx = (float) Math.cos(angle) * radius * 0.55f;
            float by = (float) Math.sin(angle) * radius * 0.55f;
            float branchRadius = radius * 0.26f;
            double a1 = angle + Math.PI / 4;
            double a2 = angle - Math.PI / 4;
            thickSeg(buffer, pose, normal, bx, by,
                    bx + (float) Math.cos(a1) * branchRadius, by + (float) Math.sin(a1) * branchRadius,
                    width * 0.55f, ICE_WHITE);
            thickSeg(buffer, pose, normal, bx, by,
                    bx + (float) Math.cos(a2) * branchRadius, by + (float) Math.sin(a2) * branchRadius,
                    width * 0.55f, ICE_WHITE);
        }
    }

    private void ringBand(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float radius, float halfWidth,
                          int segments, float spin, float[] color) {
        float inner = radius - halfWidth;
        float outer = radius + halfWidth;
        for (int i = 0; i < segments; i++) {
            double a1 = spin + Math.PI * 2 * i / segments;
            double a2 = spin + Math.PI * 2 * (i + 1) / segments;
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);
            quad(buffer, pose, normal,
                    inner * c1, inner * s1, 0f, outer * c1, outer * s1, 0f,
                    outer * c2, outer * s2, 0f, inner * c2, inner * s2, 0f, color);
        }
    }

    private void thickSeg(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                          float x1, float y1, float x2, float y2, float halfWidth, float[] color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0e-4f) length = 1f;
        float px = -dy / length * halfWidth;
        float py = dx / length * halfWidth;
        quad(buffer, pose, normal,
                x1 - px, y1 - py, 0f, x2 - px, y2 - py, 0f,
                x2 + px, y2 + py, 0f, x1 + px, y1 + py, 0f, color);
    }

    private void quad(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz, float[] color) {
        vertex(buffer, pose, normal, ax, ay, az, color);
        vertex(buffer, pose, normal, bx, by, bz, color);
        vertex(buffer, pose, normal, cx, cy, cz, color);
        vertex(buffer, pose, normal, dx, dy, dz, color);
        vertex(buffer, pose, normal, dx, dy, dz, color);
        vertex(buffer, pose, normal, cx, cy, cz, color);
        vertex(buffer, pose, normal, bx, by, bz, color);
        vertex(buffer, pose, normal, ax, ay, az, color);
    }

    private void vertex(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                        float x, float y, float z, float[] color) {
        buffer.vertex(pose, x, y, z).color(color[0], color[1], color[2], color[3]).uv(0.5f, 0.5f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0f, 0f, 1f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FrostArrayEntity entity) {
        return TEXTURE;
    }
}
