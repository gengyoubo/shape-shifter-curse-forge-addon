package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.client.SpiderMoonWeaverSwingClient.LocalSwing;
import github.com.gengyoubo.sscfa.entity.SpiderSwingBullet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;

/** 月织蛛「蛛丝荡漾」绳索渲染器。 */
public final class SpiderMoonWeaverSwingRenderer {

    private static final ResourceLocation ROPE_TEXTURE = new ResourceLocation("my_addon", "textures/entity/web_swing_rope.png");
    private static final float ROPE_HALF_WIDTH = 0.04f;

    private SpiderMoonWeaverSwingRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                render(event);
            }
        });
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) return;

        float tickDelta = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityCutoutNoCull(ROPE_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        float age = (float) world.getGameTime() + tickDelta;

        boolean drew = false;
        java.util.Iterator<Map.Entry<UUID, LocalSwing>> swings = SpiderMoonWeaverSwingClient.iterator();
        while (swings.hasNext()) {
            Map.Entry<UUID, LocalSwing> entry = swings.next();
            LocalSwing swing = entry.getValue();
            if (!swing.active) continue;
            Player player = world.getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            Vec3 hand = player.getPosition(tickDelta).add(0, player.getBbHeight() * 0.5, 0);
            Vec3 anchor;
            if (swing.state == SpiderMoonWeaverSwingClient.STATE_TETHER) {
                Entity target = world.getEntity(swing.tetherEntityId);
                if (target == null) continue;
                anchor = target.getPosition(tickDelta).add(0, target.getBbHeight() * 0.5, 0);
            } else {
                anchor = new Vec3(swing.anchorX, swing.anchorY, swing.anchorZ);
            }

            matrices.pushPose();
            matrices.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
            drawRope(matrices, consumer, hand.x - anchor.x, hand.y - anchor.y, hand.z - anchor.z, age, swing.state);
            matrices.popPose();
            drew = true;
        }

        if (SpiderSwingBullet.isBulletRecentlySeen(world.getGameTime())) {
            for (Entity entity : world.entitiesForRendering()) {
                if (!(entity instanceof SpiderSwingBullet bullet)) continue;
                if (bullet.owner == null) continue;
                Vec3 bulletPos = bullet.getPosition(tickDelta);
                Vec3 hand = bullet.owner.getPosition(tickDelta).add(0, bullet.owner.getBbHeight() * 0.5, 0);
                matrices.pushPose();
                matrices.translate(hand.x - cameraPos.x, hand.y - cameraPos.y, hand.z - cameraPos.z);
                drawRope(matrices, consumer, bulletPos.x - hand.x, bulletPos.y - hand.y, bulletPos.z - hand.z,
                        age, SpiderMoonWeaverSwingClient.STATE_FIRING);
                matrices.popPose();
                drew = true;
            }
        }

        if (drew) buffers.endBatch(renderType);
    }

    private static void drawRope(PoseStack matrices, VertexConsumer consumer, double relX, double relY, double relZ,
                                 float age, int state) {
        double full = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        if (full < 0.05) return;
        double inv = 1.0 / full;
        double normalizedY = relY * inv;
        float pitch = (float) Math.acos(Mth.clamp(normalizedY, -1.0, 1.0));
        float yaw = (float) Math.atan2(relZ * inv, relX * inv);

        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotation(1.5707964f - yaw));
        matrices.mulPose(Axis.XP.rotation(pitch));

        PoseStack.Pose pose = matrices.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();

        float length = (float) full;
        float halfWidth = ROPE_HALF_WIDTH;
        float scroll = -(age * (state == SpiderMoonWeaverSwingClient.STATE_FIRING ? 0.3f : 0.05f));
        float v0 = scroll;
        float v1 = length * 1.5f + scroll;
        float red = 0.93f;
        float green = 0.93f;
        float blue = 0.96f;
        float alpha = state == SpiderMoonWeaverSwingClient.STATE_FIRING ? 0.95f : 0.8f;

        for (int quad = 0; quad < 2; quad++) {
            float offsetX = (quad == 0) ? halfWidth : 0f;
            float offsetZ = (quad == 0) ? 0f : halfWidth;
            vertex(consumer, position, normal, -offsetX, 0f, -offsetZ, 0f, v1, red, green, blue, alpha);
            vertex(consumer, position, normal, -offsetX, length, -offsetZ, 0f, v0, red, green, blue, alpha);
            vertex(consumer, position, normal, offsetX, length, offsetZ, 1f, v0, red, green, blue, alpha);
            vertex(consumer, position, normal, offsetX, 0f, offsetZ, 1f, v1, red, green, blue, alpha);
        }
        matrices.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(position, x, y, z).color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0f, 1f, 0f).endVertex();
    }
}
