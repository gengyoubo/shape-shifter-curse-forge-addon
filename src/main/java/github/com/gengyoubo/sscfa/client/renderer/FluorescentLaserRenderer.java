package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.entity.LaserBeamEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 荧光幼灵法阵激光：法阵 + 释放光柱 + 螺旋粒子。 */
public class FluorescentLaserRenderer extends EntityRenderer<LaserBeamEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    private static final float[] CYAN = {0.35f, 0.90f, 1.00f, 0.85f};
    private static final float[] BLUE = {0.35f, 0.55f, 1.00f, 0.85f};
    private static final float[] PURPLE = {0.70f, 0.40f, 1.00f, 0.85f};
    private static final float[] GOLD = {1.00f, 0.85f, 0.40f, 0.90f};
    private static final float[] WHITE = {0.90f, 0.97f, 1.00f, 0.90f};
    private static final float[] BEAM = {0.55f, 0.92f, 1.00f, 0.45f};
    private static final float[] BEAM_CORE = {0.95f, 0.99f, 1.00f, 0.85f};

    private static final int FADE_TICKS = 30;
    private static final int ENH_ARRAY_COUNT = 3;
    private static final double ENH_ARRAY_BACK = 1.6;
    private static final double ENH_ARRAY_SIDE = 1.3;
    private static final double ENH_ARRAY_UP = 1.7;

    private static final int LINE_EXTEND_END = 50;
    private static final int LINE_ROTATE_END = 130;
    private static final float CORNER_START = 2.4f;
    private static final float CORNER_END = 0.55f;

    private static final DustParticleOptions SPIRAL_WHITE =
            new DustParticleOptions(new Vector3f(0.92f, 0.98f, 1.0f), 1.2f);
    private static final DustParticleOptions SPIRAL_CYAN =
            new DustParticleOptions(new Vector3f(0.35f, 0.90f, 1.0f), 1.3f);

    public FluorescentLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LaserBeamEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource buffers, int light) {
        if (entity.isEnhanced()) {
            renderEnhanced(entity, tickDelta, matrices, buffers);
            return;
        }
        Entity owner = entity.level().getEntity(entity.getTrackedOwnerId());
        if (owner == null) return;

        float ownerYaw = Mth.lerp(tickDelta, owner.yRotO, owner.getYRot());
        float ownerPitch = Mth.lerp(tickDelta, owner.xRotO, owner.getXRot());
        double yawRad = Math.toRadians(ownerYaw);
        double pitchRad = Math.toRadians(ownerPitch);
        double ax = -Math.sin(yawRad) * Math.cos(pitchRad);
        double ay = -Math.sin(pitchRad);
        double az = Math.cos(yawRad) * Math.cos(pitchRad);
        double arrayDistance = LaserBeamEntity.arrayDist();

        double ox = Mth.lerp(tickDelta, owner.xo, owner.getX());
        double oy = Mth.lerp(tickDelta, owner.yo, owner.getY()) + owner.getEyeHeight();
        double oz = Mth.lerp(tickDelta, owner.zo, owner.getZ());
        double ex = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double ey = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double ez = Mth.lerp(tickDelta, entity.zo, entity.getZ());

        matrices.pushPose();
        matrices.translate((ox - ex) + ax * arrayDistance, (oy - ey) + ay * arrayDistance,
                (oz - ez) + az * arrayDistance);
        matrices.mulPose(Axis.YP.rotationDegrees(-ownerYaw));
        matrices.mulPose(Axis.XP.rotationDegrees(ownerPitch));

        int phaseId = entity.getPhaseId();
        float phaseTick = entity.getPhaseTick() + tickDelta;
        float age = phaseTick + phaseId * 200f;
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());

        drawArray(buffer, matrices.last().pose(), matrices.last().normal(), age);

        if (phaseId == 0) {
            drawChargeLines(buffer, matrices.last().pose(), matrices.last().normal(),
                    phaseTick * 140f / entity.getChargeTicks(), entity.beamLength());
            spawnArrayCoreParticleIfThirdPerson(ox + ax * arrayDistance, oy + ay * arrayDistance,
                    oz + az * arrayDistance);
        }

        if (phaseId >= 1) {
            float radius = 2.5f;
            if (phaseId == 2) {
                radius *= Math.max(0.0f, 1.0f - phaseTick / FADE_TICKS);
            }
            if (radius > 0.02f) {
                drawBeam(buffer, matrices.last().pose(), matrices.last().normal(),
                        radius, (float) entity.beamLength());
                spawnBeamSpiralClient(entity, ox + ax * arrayDistance, oy + ay * arrayDistance,
                        oz + az * arrayDistance, new Vec3(ax, ay, az), radius, entity.getPhaseTick());
            }
        }

        matrices.popPose();
        super.render(entity, yaw, tickDelta, matrices, buffers, light);
    }

    private void renderEnhanced(LaserBeamEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource buffers) {
        Entity owner = entity.level().getEntity(entity.getTrackedOwnerId());
        if (owner == null) return;
        float ownerYaw = Mth.lerp(tickDelta, owner.yRotO, owner.getYRot());
        float ownerPitch = Mth.lerp(tickDelta, owner.xRotO, owner.getXRot());
        double yawRad = Math.toRadians(ownerYaw);
        double pitchRad = Math.toRadians(ownerPitch);
        double ax = -Math.sin(yawRad) * Math.cos(pitchRad);
        double ay = -Math.sin(pitchRad);
        double az = Math.cos(yawRad) * Math.cos(pitchRad);
        double ox = Mth.lerp(tickDelta, owner.xo, owner.getX());
        double oy = Mth.lerp(tickDelta, owner.yo, owner.getY()) + owner.getEyeHeight();
        double oz = Mth.lerp(tickDelta, owner.zo, owner.getZ());
        double ex = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double ey = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double ez = Mth.lerp(tickDelta, entity.zo, entity.getZ());

        Vec3 aim = new Vec3(ax, ay, az);
        Vec3 right = aim.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 base = new Vec3(ox, oy, oz).subtract(aim.scale(ENH_ARRAY_BACK));
        Vec3[] positions = new Vec3[]{
                base.subtract(right.scale(ENH_ARRAY_SIDE)),
                base.add(right.scale(ENH_ARRAY_SIDE)),
                base.add(0, ENH_ARRAY_UP, 0)
        };
        int left = entity.getArraysLeft();
        int firingIndex = entity.isFiring() ? entity.getFiringIdx() : -1;
        float age = entity.getPhaseTick() + tickDelta;
        float scale = entity.enhArrayScale();
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        Vec3 lock = entity.getLockPoint();

        for (int i = ENH_ARRAY_COUNT - left; i < ENH_ARRAY_COUNT; i++) {
            if (i < 0 || i > 2) continue;
            Vec3 p = positions[i];
            float renderYaw = -ownerYaw;
            float renderPitch = ownerPitch;
            if (lock != null) {
                Vec3 direction = lock.subtract(p);
                if (direction.lengthSqr() > 1.0e-6) {
                    direction = direction.normalize();
                    renderYaw = -(float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                    renderPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-direction.y, -1.0, 1.0)));
                }
            }
            matrices.pushPose();
            matrices.translate(p.x - ex, p.y - ey, p.z - ez);
            matrices.mulPose(Axis.YP.rotationDegrees(renderYaw));
            matrices.mulPose(Axis.XP.rotationDegrees(renderPitch));
            matrices.scale(scale, scale, scale);
            drawArray(buffer, matrices.last().pose(), matrices.last().normal(), age);
            matrices.popPose();
        }

        if (firingIndex >= 0 && firingIndex <= 2) {
            Vec3 fireLock = entity.getFireLock();
            Vec3 from = positions[firingIndex];
            Vec3 direction = fireLock.subtract(from);
            double length = direction.length();
            if (length > 1.0e-4) {
                Vec3 normalized = direction.scale(1.0 / length);
                float fireYaw = (float) Math.toDegrees(Math.atan2(-normalized.x, normalized.z));
                float firePitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-normalized.y, -1.0, 1.0)));
                matrices.pushPose();
                matrices.translate(from.x - ex, from.y - ey, from.z - ez);
                matrices.mulPose(Axis.YP.rotationDegrees(-fireYaw));
                matrices.mulPose(Axis.XP.rotationDegrees(firePitch));
                matrices.pushPose();
                matrices.scale(scale, scale, scale);
                drawArray(buffer, matrices.last().pose(), matrices.last().normal(), age);
                matrices.popPose();
                drawBeam(buffer, matrices.last().pose(), matrices.last().normal(),
                        entity.enhBeamRadius(), (float) length);
                matrices.popPose();
            }
        }
    }

    private void spawnArrayCoreParticleIfThirdPerson(double wx, double wy, double wz) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        if (client.options.getCameraType() == CameraType.FIRST_PERSON) return;
        client.level.addParticle(ParticleTypes.END_ROD, wx, wy, wz,
                (client.level.random.nextDouble() - 0.5) * 0.02,
                (client.level.random.nextDouble() - 0.5) * 0.02,
                (client.level.random.nextDouble() - 0.5) * 0.02);
    }

    private void drawChargeLines(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float progress, double beamLength) {
        float length;
        float spacing;
        float rotation;
        if (progress <= LINE_EXTEND_END) {
            float p = progress / LINE_EXTEND_END;
            length = (float) beamLength * p;
            spacing = CORNER_START;
            rotation = 0f;
        } else if (progress <= LINE_ROTATE_END) {
            float p = (progress - LINE_EXTEND_END) / (LINE_ROTATE_END - LINE_EXTEND_END);
            length = (float) beamLength;
            spacing = CORNER_START + (CORNER_END - CORNER_START) * (p * p);
            float smooth = p * p * (3 - 2 * p);
            rotation = smooth * (float) (Math.PI * 6.0);
        } else {
            length = (float) beamLength;
            spacing = CORNER_END;
            rotation = (float) (Math.PI * 6.0);
        }
        float halfWidth = 0.05f;
        for (int i = 0; i < 4; i++) {
            float angle = rotation + i * (float) (Math.PI / 2) + (float) (Math.PI / 4);
            float ox = (float) Math.cos(angle) * spacing;
            float oy = (float) Math.sin(angle) * spacing;
            quad(buffer, pose, normal,
                    ox - halfWidth, oy - halfWidth, 0f, ox - halfWidth, oy - halfWidth, length,
                    ox + halfWidth, oy + halfWidth, length, ox + halfWidth, oy + halfWidth, 0f, WHITE);
        }
    }

    private void drawArray(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float age) {
        float radius = 2.2f;
        float spin = age * 0.03f;
        float width = radius * 0.035f;
        ringBand(buffer, pose, normal, radius * 0.98f, width * 1.3f, 64, spin, CYAN);
        ringBand(buffer, pose, normal, radius * 0.72f, width, 56, -spin * 1.5f, PURPLE);
        ringBand(buffer, pose, normal, radius * 0.45f, width, 40, spin * 2.0f, BLUE);
        hexagram(buffer, pose, normal, radius * 0.82f, spin * 0.8f, width * 0.9f, GOLD);
        for (int i = 0; i < 24; i++) {
            double angle = spin + Math.PI * 2 * i / 24;
            thickRadial(buffer, pose, normal, angle, radius * 0.82f, radius * 0.98f, width * 0.7f, WHITE);
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

    private void hexagram(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float radius, float spin,
                          float halfWidth, float[] color) {
        triThick(buffer, pose, normal, radius, spin + (float) (Math.PI / 2), halfWidth, color);
        triThick(buffer, pose, normal, radius, spin + (float) (Math.PI / 6), halfWidth, color);
    }

    private void triThick(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float radius, float start,
                          float halfWidth, float[] color) {
        for (int i = 0; i < 3; i++) {
            double a1 = start + Math.PI * 2 * i / 3;
            double a2 = start + Math.PI * 2 * (i + 1) / 3;
            thickSeg(buffer, pose, normal,
                    (float) (Math.cos(a1) * radius), (float) (Math.sin(a1) * radius),
                    (float) (Math.cos(a2) * radius), (float) (Math.sin(a2) * radius), halfWidth, color);
        }
    }

    private void thickRadial(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, double angle,
                             float r1, float r2, float halfWidth, float[] color) {
        float ca = (float) Math.cos(angle);
        float sa = (float) Math.sin(angle);
        float px = -sa * halfWidth;
        float py = ca * halfWidth;
        quad(buffer, pose, normal,
                r1 * ca - px, r1 * sa - py, 0f, r2 * ca - px, r2 * sa - py, 0f,
                r2 * ca + px, r2 * sa + py, 0f, r1 * ca + px, r1 * sa + py, 0f, color);
    }

    private void thickSeg(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float x1, float y1,
                          float x2, float y2, float halfWidth, float[] color) {
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

    private void drawBeam(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float radius, float length) {
        int segments = 28;
        for (int layer = 0; layer < 2; layer++) {
            float r = (layer == 0) ? radius : radius * 0.45f;
            float[] color = (layer == 0) ? BEAM : BEAM_CORE;
            for (int i = 0; i < segments; i++) {
                double a1 = Math.PI * 2 * i / segments;
                double a2 = Math.PI * 2 * (i + 1) / segments;
                float x1 = (float) (Math.cos(a1) * r);
                float y1 = (float) (Math.sin(a1) * r);
                float x2 = (float) (Math.cos(a2) * r);
                float y2 = (float) (Math.sin(a2) * r);
                quad(buffer, pose, normal, x1, y1, 0f, x1, y1, length, x2, y2, length, x2, y2, 0f, color);
            }
        }
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

    private void spawnBeamSpiralClient(LaserBeamEntity entity, double x, double y, double z,
                                       Vec3 aim, double radius, int phaseTick) {
        if (radius <= 0.01) return;
        if (entity.clientParticleGate == phaseTick) return;
        entity.clientParticleGate = phaseTick;
        Vec3 right = aim.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(aim).normalize();
        double beamLength = entity.beamLength();
        int steps = (int) (beamLength / 0.8);
        double baseAngle = phaseTick * 0.6;
        for (int s = 0; s <= steps; s++) {
            double distance = beamLength * s / steps;
            double angle1 = baseAngle + distance * 0.9;
            double angle2 = angle1 + Math.PI;
            Vec3 axis = new Vec3(x, y, z).add(aim.scale(distance));
            Vec3 o1 = right.scale(Math.cos(angle1) * radius).add(up.scale(Math.sin(angle1) * radius));
            Vec3 o2 = right.scale(Math.cos(angle2) * radius).add(up.scale(Math.sin(angle2) * radius));
            if (s % 2 == 0) {
                Vec3 p1 = axis.add(o1);
                entity.level().addParticle(SPIRAL_WHITE, p1.x, p1.y, p1.z, 0, 0, 0);
                Vec3 p2 = axis.add(o2);
                entity.level().addParticle(SPIRAL_CYAN, p2.x, p2.y, p2.z, 0, 0, 0);
            }
            if (s % 3 == 0) {
                entity.level().addParticle(ParticleTypes.END_ROD, axis.x, axis.y, axis.z, 0.05, 0.05, 0.05);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBeamEntity entity) {
        return TEXTURE;
    }
}
