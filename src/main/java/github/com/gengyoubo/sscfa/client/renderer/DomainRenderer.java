package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.spell.DomainRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 领域客户端镜像：壳几何、相机钳制、视线/声音隔离与球壳渲染。 */
public final class DomainRenderer {
    private record View(UUID owner, Vec3 center, double headHeight, boolean active, int elapsed,
                        double innerRadius, double outerRadius) {
        int durationTicks() { return DomainRules.durationTicks(); }
        int chargeTicks() { return DomainRules.chargeTicks(); }
        int expandStartTick() { return DomainRules.expandStartTick(); }

        double expansionRadius(double chargeElapsed) {
            return DomainRules.expansionRadius(chargeElapsed, DomainRules.expandStartTick(),
                    DomainRules.chargeTicks(), DomainRules.expandDurationTicks(), innerRadius);
        }
    }

    private static List<View> fields = List.of();
    private static ClientLevel world;
    private static long receivedAt;
    private static List<DomainRules.Shell> shellCache;
    private static long shellCacheTick = Long.MIN_VALUE;

    private DomainRenderer() {
    }

    public static void register() {
        github.com.gengyoubo.sscfa.client.DomainSound.register();
        AddonNetwork.registerClientReceiver(DomainManager.STATE, buf -> {
            ResourceLocation dimension = buf.readResourceLocation();
            int count = Math.max(0, Math.min(1024, buf.readVarInt()));
            List<View> next = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                next.add(new View(buf.readUUID(),
                        new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                        buf.readDouble(), buf.readBoolean(), buf.readVarInt(),
                        buf.isReadable() ? buf.readDouble() : DomainRules.innerRadius(),
                        buf.isReadable() ? buf.readDouble() : DomainRules.outerRadius()));
            }
            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            if (level == null || !level.dimension().location().equals(dimension)) return;
            world = level;
            receivedAt = level.getGameTime();
            fields = List.copyOf(next);
            shellCacheTick = Long.MIN_VALUE;
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            fields = List.of();
            world = null;
            shellCache = null;
            shellCacheTick = Long.MIN_VALUE;
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) render(event);
        });
    }

    private static boolean valid() {
        return world != null && Minecraft.getInstance().level == world
                && world.getGameTime() - receivedAt < 40;
    }

    public static boolean isCharging(UUID owner) {
        return valid() && fields.stream().anyMatch(field -> !field.active && field.owner.equals(owner));
    }

    public static List<DomainRules.Shell> clientShells() {
        if (!valid()) return null;
        long now = world.getGameTime();
        if (shellCacheTick == now) return shellCache;
        List<DomainRules.Shell> shells = new ArrayList<>();
        for (View field : fields) {
            float elapsed = field.elapsed + now - receivedAt;
            if (field.active) {
                if (elapsed < field.durationTicks()) {
                    shells.add(new DomainRules.Shell(field.center, field.innerRadius, field.outerRadius, true));
                }
            } else if (elapsed >= field.expandStartTick() && elapsed < field.chargeTicks() + 20) {
                double radius = field.expansionRadius(Math.min(elapsed, field.chargeTicks()));
                if (radius > 0.1) {
                    shells.add(new DomainRules.Shell(field.center, radius, field.outerRadius, false));
                }
            }
        }
        shellCache = shells.isEmpty() ? null : shells;
        shellCacheTick = now;
        return shellCache;
    }

    public static Vec3 clampCamera(Vec3 from, Vec3 target) {
        if (!valid()) return target;
        Vec3 best = target;
        for (View field : fields) {
            float elapsed = field.elapsed + world.getGameTime() - receivedAt;
            double inner;
            if (field.active) {
                if (elapsed >= field.durationTicks()) continue;
                inner = field.innerRadius;
            } else {
                if (elapsed < field.expandStartTick()) continue;
                inner = field.expansionRadius(Math.min(elapsed, field.chargeTicks()));
                if (inner <= 0.1) continue;
            }
            Vec3 start = from.subtract(field.center);
            Vec3 end = target.subtract(field.center);
            boolean fromInside = start.lengthSqr() <= (inner + 1) * (inner + 1);
            if (!fromInside) continue;
            boolean cross = field.active
                    ? DomainRules.crosses(start.x, start.y, start.z, end.x, end.y, end.z, 0.3,
                            field.innerRadius, field.outerRadius)
                    : DomainRules.crossesOutward(start.x, start.y, start.z, end.x, end.y, end.z, inner);
            if (!cross) continue;
            Vec3 delta = target.subtract(from);
            double low = 0;
            double high = 1;
            for (int iteration = 0; iteration < 18; iteration++) {
                double middle = (low + high) * 0.5;
                Vec3 mid = from.add(delta.scale(middle)).subtract(field.center);
                boolean midCross = field.active
                        ? DomainRules.crosses(start.x, start.y, start.z, mid.x, mid.y, mid.z, 0.3,
                                field.innerRadius, field.outerRadius)
                        : DomainRules.crossesOutward(start.x, start.y, start.z, mid.x, mid.y, mid.z, inner);
                if (midCross) high = middle;
                else low = middle;
            }
            best = from.add(delta.scale(Math.max(0, low - 0.0001)));
        }
        return best;
    }

    public static Vec3 projectInside(Vec3 eye, Vec3 cameraPos) {
        if (!valid()) return null;
        for (View field : fields) {
            float elapsed = field.elapsed + world.getGameTime() - receivedAt;
            double inner;
            if (field.active) {
                if (elapsed >= field.durationTicks()) continue;
                inner = field.innerRadius;
            } else {
                if (elapsed < field.expandStartTick()) continue;
                inner = field.expansionRadius(Math.min(elapsed, field.chargeTicks()));
                if (inner <= 0.1) continue;
            }
            Vec3 eyeRelative = eye.subtract(field.center);
            double limit = Math.max(0.1, inner - 0.3);
            if (eyeRelative.lengthSqr() > limit * limit) continue;
            Vec3 cameraRelative = cameraPos.subtract(field.center);
            if (cameraRelative.lengthSqr() <= limit * limit) continue;
            Vec3 clamped = DomainRules.slideInside(eyeRelative, cameraRelative.subtract(eyeRelative), limit)
                    .add(eyeRelative);
            return field.center.add(clamped);
        }
        return null;
    }

    public static boolean blocksCrossBoundaryClient(Vec3 eye, Vec3 target) {
        if (!valid()) return false;
        for (View field : fields) {
            float elapsed = field.elapsed + world.getGameTime() - receivedAt;
            double inner;
            if (field.active) {
                if (elapsed >= field.durationTicks()) continue;
                inner = field.innerRadius;
            } else {
                if (elapsed < field.expandStartTick()) continue;
                inner = field.expansionRadius(Math.min(elapsed, field.chargeTicks()));
                if (inner <= 0.1) continue;
            }
            if (DomainRules.separates(eye.subtract(field.center), target.subtract(field.center), inner + 1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocksTargetingClient(Entity target) {
        var player = Minecraft.getInstance().player;
        return player != null && target != null && target != player
                && blocksCrossBoundaryClient(player.position(), target.position());
    }

    public static boolean blocksSoundForListener(double x, double y, double z) {
        if (!valid()) return false;
        var listener = Minecraft.getInstance().player;
        if (listener == null) return false;
        Vec3 sound = new Vec3(x, y, z);
        for (View field : fields) {
            if (field.center.distanceToSqr(sound) < 4) return false;
        }
        boolean listenerInside = false;
        boolean soundInside = false;
        for (View field : fields) {
            float elapsed = field.elapsed + world.getGameTime() - receivedAt;
            double inner;
            if (field.active) {
                if (elapsed >= field.durationTicks()) continue;
                inner = field.innerRadius;
            } else {
                if (elapsed < field.expandStartTick()) continue;
                inner = field.expansionRadius(Math.min(elapsed, field.chargeTicks()));
                if (inner <= 0.1) continue;
            }
            double outer = inner + 1;
            if (field.center.distanceToSqr(listener.position()) <= outer * outer) listenerInside = true;
            if (field.center.distanceToSqr(sound) <= outer * outer) soundInside = true;
        }
        return listenerInside != soundInside;
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!valid() || client.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.debugQuads();
        VertexConsumer vertices = buffers.getBuffer(renderType);
        float tickDelta = event.getPartialTick();

        for (View field : fields) {
            float elapsed = field.elapsed + world.getGameTime() - receivedAt + tickDelta;
            if (elapsed >= (field.active ? field.durationTicks() : field.chargeTicks() + 20)) continue;
            matrices.pushPose();
            matrices.translate(field.center.x - camera.x, field.center.y - camera.y, field.center.z - camera.z);
            double shellRadius = field.active
                    ? field.innerRadius
                    : field.expansionRadius(Math.min(elapsed, field.chargeTicks()));
            Matrix4f matrix = matrices.last().pose();
            if (field.active) {
                sphere(vertices, matrix, field.innerRadius);
                sphere(vertices, matrix, field.outerRadius);
            } else if (shellRadius > 0.1) {
                sphere(vertices, matrix, shellRadius);
                sphere(vertices, matrix, shellRadius + 1);
            }
            float alpha = field.active ? 0.9f : Math.min(1, elapsed / 12f);
            CastingCircleRenderer.drawGround(vertices, matrices, field.innerRadius, 0.04,
                    elapsed * 0.002, alpha, MagicCircleGeometry.RED_PRIMARY);
            if (!field.active) {
                circle(vertices, matrices, 3, field.headHeight, elapsed * 0.013, alpha);
                double second = DomainRules.layerProgress(elapsed, 60);
                if (second > 0) {
                    circle(vertices, matrices, 5 * second, field.headHeight + 3 * second,
                            -elapsed * 0.009, (float) second);
                }
                double third = DomainRules.layerProgress(elapsed, 120);
                if (third > 0) {
                    circle(vertices, matrices, 2 * third, field.headHeight + 3 + 3 * third,
                            elapsed * 0.017, (float) third);
                }
            }
            matrices.popPose();
        }
        buffers.endBatch(renderType);
    }

    private static void circle(VertexConsumer vertices, PoseStack matrices, double radius, double height,
                               double rotation, float alpha) {
        matrices.pushPose();
        matrices.translate(0, height, 0);
        matrices.scale((float) radius, 1, (float) radius);
        Matrix4f matrix = matrices.last().pose();
        double width = Math.max(0.009, 0.085 / radius);
        int crimson = 0xF52246;
        int bright = 0xFF797D;
        for (double ring : new double[]{1, 0.97, 0.85, 0.80, 0.58, 0.21}) {
            CastingCircleRenderer.ring(vertices, matrix, ring, width, 0, ring == 0.97 ? bright : crimson, alpha);
        }
        CastingCircleRenderer.ring(vertices, matrix, 0.36, width * 0.75, 0, bright, alpha * 0.55f);
        for (int index = 0; index < 12; index++) {
            double angle = rotation + index * Math.PI / 6;
            double next = angle + Math.PI * 5 / 6;
            CastingCircleRenderer.line(vertices, matrix, Math.cos(angle) * 0.78, Math.sin(angle) * 0.78,
                    Math.cos(next) * 0.78, Math.sin(next) * 0.78, width, crimson, alpha * 0.85f);
            double radialX = Math.cos(-rotation + index * Math.PI / 6);
            double radialZ = Math.sin(-rotation + index * Math.PI / 6);
            for (int stroke = 0; stroke < 3; stroke++) {
                double distance = 0.865 + stroke * 0.029;
                CastingCircleRenderer.line(vertices, matrix,
                        radialX * distance - radialZ * 0.022, radialZ * distance + radialX * 0.022,
                        radialX * distance + radialZ * 0.022, radialZ * distance - radialX * 0.022,
                        width * 0.85, bright, alpha);
            }
            CastingCircleRenderer.line(vertices, matrix, radialX * 0.86, radialZ * 0.86,
                    radialX * 0.95, radialZ * 0.95, width, crimson, alpha);
        }
        matrices.popPose();
    }

    private static void sphere(VertexConsumer vertices, Matrix4f matrix, double radius) {
        if (CircleVertexBatch.drawDomainSphere(vertices, matrix, radius)) return;
        for (int vertex = 0; vertex < DomainSphereGeometry.vertexCount(); vertex++) {
            vertices.vertex(matrix, DomainSphereGeometry.x(vertex, radius), DomainSphereGeometry.y(vertex, radius),
                    DomainSphereGeometry.z(vertex, radius)).color(0, 0, 0, 255).endVertex();
        }
    }
}
