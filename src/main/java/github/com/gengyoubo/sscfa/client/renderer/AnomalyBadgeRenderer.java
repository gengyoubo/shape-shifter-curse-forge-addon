package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.sscfa.anomaly.AnomalyLedger;
import github.com.gengyoubo.sscfa.client.AnomalyClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.util.Map;

/** Small, depth-tested living-entity badges drawn from the authoritative anomaly mirror. */
public final class AnomalyBadgeRenderer {
    private static final ResourceLocation ATLAS =
            new ResourceLocation("ssc_addon", "textures/gui/anomaly/badges64.png");
    private static final float[][] CORNERS = {{1, 0}, {1, 1}, {0, 1}, {0, 0}};
    private static final Matrix4f BILLBOARD = new Matrix4f();
    private static final Vector3f RIGHT = new Vector3f();

    private AnomalyBadgeRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) render(event);
        });
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui
                || AnomalyClient.trackedCount() == 0) return;
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        Vector3f right = RIGHT.set(-1, 0, 0).rotate(camera.rotation());
        float delta = event.getPartialTick();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.text(ATLAS);
        VertexConsumer vertices = null;

        var targets = AnomalyClient.accumulationTargets();
        for (LivingEntity target : targets) {
            if (target.isSpectator() || target.isInvisibleTo(client.player)
                    || target == client.getCameraEntity() && client.options.getCameraType().isFirstPerson()) {
                continue;
            }
            Map<String, Double> amounts = AnomalyClient.accumulation(target);
            if (amounts.isEmpty()) continue;
            double x = Mth.lerp(delta, target.xo, target.getX()) - cameraPosition.x;
            double y = Mth.lerp(delta, target.yo, target.getY()) - cameraPosition.y;
            double z = Mth.lerp(delta, target.zo, target.getZ()) - cameraPosition.z;
            if (x * x + y * y + z * z > AnomalyBadgeLayout.MAX_DISTANCE_SQUARED
                    || DomainRenderer.blocksTargetingClient(target)) continue;
            if (event.getFrustum() != null
                    && !event.getFrustum().isVisible(target.getBoundingBox().inflate(1))) continue;

            float radius = AnomalyBadgeLayout.bodyRadius(target.getBbWidth(), target.getBbHeight())
                    * (Math.abs(right.x) + Math.abs(right.z))
                    + target.getBbHeight() * .5f * Math.abs(right.y);
            float scale = AnomalyBadgeLayout.displayScale(target instanceof Player, target.getHealth());
            if (vertices == null) vertices = buffers.getBuffer(renderType);
            BILLBOARD.set(matrices.last().pose())
                    .translate((float) x,
                            (float) y + AnomalyBadgeLayout.top(target.getBbHeight(), amounts.size(), scale),
                            (float) z);
            faceCamera(BILLBOARD, camera.rotation());
            scaleAndOffset(BILLBOARD, radius, scale);
            badges(vertices, BILLBOARD, amounts, AnomalyBadgeLayout.SIZE * .5f);
        }
        if (vertices != null) buffers.endBatch(renderType);
    }

    static void faceCamera(Matrix4f matrix, Quaternionfc cameraRotation) {
        matrix.rotate(cameraRotation).rotateY((float) Math.PI);
    }

    static void scaleAndOffset(Matrix4f matrix, float bodyRadius, float scale) {
        matrix.translate(bodyRadius + AnomalyBadgeLayout.MODEL_GAP, 0, 0).scale(scale);
    }

    static int badges(VertexConsumer vertices, Matrix4f matrix, Map<String, Double> amounts, float left) {
        int slot = 0;
        for (int i = 0; i < AnomalyBadgeLayout.ELEMENTS.size(); i++) {
            var element = AnomalyBadgeLayout.ELEMENTS.get(i);
            Double amount = amounts.get(element.id());
            if (amount == null || !Double.isFinite(amount) || !(amount > 0)) continue;
            float x = left + AnomalyBadgeLayout.x(slot);
            float y = AnomalyBadgeLayout.y(slot++);
            tile(vertices, matrix, element.baseTile(), x, y, 0);
            progress(vertices, matrix, element.progressTile(), x, y,
                    AnomalyBadgeLayout.fraction(amount, AnomalyLedger.MAXIMUM));
        }
        return slot;
    }

    static void tile(VertexConsumer vertices, Matrix4f matrix, int tile, float x, float y, float z) {
        float half = AnomalyBadgeLayout.SIZE * .5f;
        float u = AnomalyBadgeLayout.u(tile);
        float v = AnomalyBadgeLayout.v(tile);
        float uEnd = u + 1f / AnomalyBadgeLayout.ATLAS_COLUMNS;
        float vEnd = v + 1f / AnomalyBadgeLayout.ATLAS_ROWS;
        vertex(vertices, matrix, x - half, y + half, z, u, v);
        vertex(vertices, matrix, x - half, y - half, z, u, vEnd);
        vertex(vertices, matrix, x + half, y - half, z, uEnd, vEnd);
        vertex(vertices, matrix, x + half, y + half, z, uEnd, v);
    }

    static void progress(VertexConsumer vertices, Matrix4f matrix, int tile, float x, float y, double fraction) {
        if (!(fraction > 0) || !Double.isFinite(fraction)) return;
        if (fraction >= 1) {
            tile(vertices, matrix, tile, x, y, .001f);
            return;
        }
        double end = fraction * Math.PI * 2;
        float fromX = .5f;
        float fromY = 0;
        for (int corner = 0; corner < 4; corner++) {
            double angle = Math.PI * .25 + corner * Math.PI * .5;
            if (angle >= end) break;
            float toX = CORNERS[corner][0];
            float toY = CORNERS[corner][1];
            sector(vertices, matrix, tile, x, y, fromX, fromY, toX, toY);
            fromX = toX;
            fromY = toY;
        }
        double dx = Math.sin(end);
        double dy = -Math.cos(end);
        double scale = .5 / Math.max(Math.abs(dx), Math.abs(dy));
        sector(vertices, matrix, tile, x, y, fromX, fromY,
                (float) (.5 + dx * scale), (float) (.5 + dy * scale));
    }

    private static void sector(VertexConsumer vertices, Matrix4f matrix, int tile, float x, float y,
                               float fromX, float fromY, float toX, float toY) {
        local(vertices, matrix, tile, x, y, .5f, .5f);
        local(vertices, matrix, tile, x, y, toX, toY);
        local(vertices, matrix, tile, x, y, fromX, fromY);
        local(vertices, matrix, tile, x, y, fromX, fromY);
    }

    private static void local(VertexConsumer vertices, Matrix4f matrix, int tile, float x, float y,
                              float u, float v) {
        vertex(vertices, matrix, x + (u - .5f) * AnomalyBadgeLayout.SIZE,
                y + (.5f - v) * AnomalyBadgeLayout.SIZE, .001f,
                AnomalyBadgeLayout.u(tile) + u / AnomalyBadgeLayout.ATLAS_COLUMNS,
                AnomalyBadgeLayout.v(tile) + v / AnomalyBadgeLayout.ATLAS_ROWS);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f m, float x, float y, float z,
                               float u, float v) {
        vertices.vertex(
                        m.m00() * x + m.m10() * y + m.m20() * z + m.m30(),
                        m.m01() * x + m.m11() * y + m.m21() * z + m.m31(),
                        m.m02() * x + m.m12() * y + m.m22() * z + m.m32())
                .color(255, 255, 255, 255).uv(u, v).uv2(0xF000F0).endVertex();
    }
}
