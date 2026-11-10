package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.entity.TidalOrbEntity;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 荧光幼灵「潮汐束缚」守卫者激光渲染器：从水球向每个被拴目标画光束。 */
public final class TidalTetherBeamRenderer {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/guardian_beam.png");
    private static final int EXPIRE_TICKS = 60;

    private static final Map<Integer, Entry> ACTIVE = new ConcurrentHashMap<>();
    private static ClientLevel activeWorld;

    private TidalTetherBeamRenderer() {
    }

    private static final class Entry {
        final int[] targetIds;
        final long expireTime;
        final TidalOrbEntity source;

        Entry(int[] targetIds, long expireTime, TidalOrbEntity source) {
            this.targetIds = targetIds;
            this.expireTime = expireTime;
            this.source = source;
        }
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_TIDAL_TETHER, buf -> {
            int orbId = buf.readVarInt();
            int count = buf.readVarInt();
            int[] targetIds = new int[Math.max(0, count)];
            for (int i = 0; i < targetIds.length; i++) targetIds[i] = buf.readVarInt();
            ClientLevel world = Minecraft.getInstance().level;
            if (world != activeWorld) {
                clear();
                activeWorld = world;
            }
            if (world == null) return;
            if (targetIds.length == 0) {
                ACTIVE.remove(orbId);
                return;
            }
            Entity source = world.getEntity(orbId);
            if (!(source instanceof TidalOrbEntity orb)) return;
            ACTIVE.put(orbId, new Entry(targetIds, world.getGameTime() + EXPIRE_TICKS, orb));
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                render(event);
            }
        });
    }

    public static void clear() {
        ACTIVE.clear();
        activeWorld = null;
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world != activeWorld) {
            clear();
            activeWorld = world;
        }
        if (world == null || ACTIVE.isEmpty()) return;

        long now = world.getGameTime();
        float tickDelta = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityCutoutNoCull(BEAM_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        float age = (float) now + tickDelta;
        boolean drew = false;

        Iterator<Map.Entry<Integer, Entry>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Entry> mapEntry = iterator.next();
            Entry entry = mapEntry.getValue();
            if (now > entry.expireTime) {
                iterator.remove();
                continue;
            }
            Entity orb = world.getEntity(mapEntry.getKey());
            if (orb != entry.source || orb.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!entry.source.isTetherActive()) continue;
            Vec3 orbPos = orb.getPosition(tickDelta).add(0.0, orb.getBbHeight() * 0.5, 0.0);
            for (int targetId : entry.targetIds) {
                Entity target = world.getEntity(targetId);
                if (target == null || !target.isAlive()) continue;
                Vec3 targetPos = target.getPosition(tickDelta).add(0.0, target.getBbHeight() * 0.5, 0.0);
                matrices.pushPose();
                matrices.translate(orbPos.x - cameraPos.x, orbPos.y - cameraPos.y, orbPos.z - cameraPos.z);
                drawBeam(matrices, consumer, targetPos.x - orbPos.x, targetPos.y - orbPos.y, targetPos.z - orbPos.z, age);
                matrices.popPose();
                drew = true;
            }
        }
        if (drew) buffers.endBatch(renderType);
    }

    private static void drawBeam(PoseStack matrices, VertexConsumer consumer, double relX, double relY, double relZ, float age) {
        double full = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        if (full < 1.0e-3) return;
        double inv = 1.0 / full;
        double nx = relX * inv;
        double ny = relY * inv;
        double nz = relZ * inv;
        float pitch = (float) Math.acos(Mth.clamp(ny, -1.0, 1.0));
        float yaw = (float) Math.atan2(nz, nx);

        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotation(1.5707964f - yaw));
        matrices.mulPose(Axis.XP.rotation(pitch));

        PoseStack.Pose pose = matrices.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();

        float length = (float) full;
        float halfWidth = 0.14f;
        float scroll = -(age * 0.06f);
        float v0 = scroll;
        float v1 = length * 0.7f + scroll;
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.35f);
        float red = 0.45f + 0.40f * pulse;
        float green = 0.75f;
        float blue = 1.00f;
        float alpha = 0.85f;

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
