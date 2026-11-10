package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** 诅咒标记头顶 2D 图标渲染器（Billboard，纯客户端）。 */
public final class CurseMarkIconRenderer {
    private static final ResourceLocation ICON_TEXTURE =
            new ResourceLocation("ssc_addon", "textures/gui/curse_mark_icon.png");
    private static final float SIZE = 0.5f;
    private static final float LIFT = 0.45f;

    private static long lastScanTick = -1;
    private static final List<Integer> CACHED_IDS = new ArrayList<>();

    private CurseMarkIconRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) render(event);
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        long now = client.level.getGameTime();
        if (now != lastScanTick && now % 10 == 0) {
            scanMarkedEntities(client);
            lastScanTick = now;
        }
        if (CACHED_IDS.isEmpty()) return;

        float tickDelta = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(ICON_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        Vector3f cameraRight = new Vector3f(1, 0, 0).rotate(camera.rotation());
        Vector3f cameraUp = new Vector3f(0, 1, 0).rotate(camera.rotation());
        boolean drew = false;

        for (int id : CACHED_IDS) {
            var entity = client.level.getEntity(id);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            if (!living.hasEffect(SscAddon.CURSE_MARK)) continue;
            if (DomainRenderer.blocksTargetingClient(living)) continue;
            Vec3 pos = living.getPosition(tickDelta)
                    .add(0.0, living.getBbHeight() + LIFT - SIZE * 0.5f, 0.0);
            matrices.pushPose();
            matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
            drawCameraFacingQuad(matrices, consumer, SIZE, cameraRight, cameraUp);
            matrices.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch(renderType);
    }

    private static void scanMarkedEntities(Minecraft client) {
        CACHED_IDS.clear();
        var world = client.level;
        if (world == null || client.player == null) return;
        for (var entity : world.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            if (!living.hasEffect(SscAddon.CURSE_MARK)) continue;
            if (living.distanceToSqr(client.player) > 64.0 * 64.0) continue;
            CACHED_IDS.add(living.getId());
        }
    }

    private static void drawCameraFacingQuad(PoseStack matrices, VertexConsumer consumer, float size,
                                             Vector3f right, Vector3f up) {
        float half = size * 0.5f;
        float rx = right.x * half;
        float ry = right.y * half;
        float rz = right.z * half;
        float ux = up.x * half;
        float uy = up.y * half;
        float uz = up.z * half;
        int light = 15728880;
        PoseStack.Pose pose = matrices.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, position, normal, -rx + ux, -ry + uy, -rz + uz, 0f, 0f, light);
        vertex(consumer, position, normal, -rx - ux, -ry - uy, -rz - uz, 0f, 1f, light);
        vertex(consumer, position, normal, rx - ux, ry - uy, rz - uz, 1f, 1f, light);
        vertex(consumer, position, normal, rx + ux, ry + uy, rz + uz, 1f, 0f, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float u, float v, int light) {
        consumer.vertex(position, x, y, z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0, 1, 0).endVertex();
    }

    public static void clear() {
        CACHED_IDS.clear();
        lastScanTick = -1;
    }
}
