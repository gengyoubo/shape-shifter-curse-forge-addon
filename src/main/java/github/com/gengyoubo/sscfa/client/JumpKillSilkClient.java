package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 跳蛛「安全丝」客户端：维护锚点镜像，并在实体渲染后画一条蛛丝。 */
public final class JumpKillSilkClient {

    private static final ResourceLocation ROPE_TEXTURE = new ResourceLocation("my_addon", "textures/entity/web_swing_rope.png");
    private static final float ROPE_HALF_WIDTH = 0.04f;

    private static final Map<UUID, Vec3> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ANCHOR_DEADLINES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ANCHOR_WINDOWS = new ConcurrentHashMap<>();

    private JumpKillSilkClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_JUMP_KILL_SILK_STATE, buf -> {
            UUID uuid = buf.readUUID();
            boolean active = buf.readBoolean();
            double ax = buf.readDouble();
            double ay = buf.readDouble();
            double az = buf.readDouble();
            int windowTicks = buf.readVarInt();
            long serverNowTick = buf.readLong();
            if (active && windowTicks > 0) {
                ANCHORS.put(uuid, new Vec3(ax, ay, az));
                ANCHOR_DEADLINES.put(uuid, serverNowTick + windowTicks);
                ANCHOR_WINDOWS.put(uuid, windowTicks);
            } else {
                ANCHORS.remove(uuid);
                ANCHOR_DEADLINES.remove(uuid);
                ANCHOR_WINDOWS.remove(uuid);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            ANCHORS.clear();
            ANCHOR_DEADLINES.clear();
            ANCHOR_WINDOWS.clear();
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                render(event);
            }
        });
    }

    public static double recallWindowFraction() {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null) return -1.0;
        Long deadline = ANCHOR_DEADLINES.get(self.getUUID());
        Integer window = ANCHOR_WINDOWS.get(self.getUUID());
        if (deadline == null || window == null || window <= 0) return -1.0;
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return -1.0;
        double remain = Math.max(0, deadline - world.getGameTime());
        return Math.min(1.0, remain / window);
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || ANCHORS.isEmpty()) return;

        float tickDelta = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityCutoutNoCull(ROPE_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        float age = (float) world.getGameTime() + tickDelta;

        for (Map.Entry<UUID, Vec3> entry : ANCHORS.entrySet()) {
            Player player = world.getPlayerByUUID(entry.getKey());
            if (player == null || player.isRemoved()) continue;
            Vec3 anchor = entry.getValue();
            Vec3 hand = player.getPosition(tickDelta).add(0, player.getBbHeight() * 0.5, 0);
            matrices.pushPose();
            matrices.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
            drawRope(matrices, consumer, hand.x - anchor.x, hand.y - anchor.y, hand.z - anchor.z, age);
            matrices.popPose();
        }
        buffers.endBatch(renderType);
    }

    private static void drawRope(PoseStack matrices, VertexConsumer consumer, double relX, double relY, double relZ, float age) {
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
        float scroll = -(age * 0.05f);
        float v0 = scroll;
        float v1 = length * 1.5f + scroll;
        float red = 0.93f;
        float green = 0.93f;
        float blue = 0.96f;
        float alpha = 0.8f;

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
