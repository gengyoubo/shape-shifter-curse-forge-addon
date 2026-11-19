package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.sscfa.client.CastingVisualState;
import github.com.gengyoubo.sscfa.client.ExplosionEffects;
import github.com.gengyoubo.sscfa.client.SpellcastClient;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.ExplosionManager;
import github.com.gengyoubo.sscfa.spell.ExplosionRules;
import github.com.gengyoubo.sscfa.spell.FormSpellStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;

/**
 * 爆裂魔法世界视觉（移植自 Fabric 版）。接收 {@code ssc_addon:explosion_start} 快照（爆心 + 已流逝 tick，
 * 每 10 tick 校准），客户端按收包后的世界时间推进演出，无逐粒子网络包：
 * <ol>
 *   <li>地面 2 格红紫法阵（锁点并开始蓄力时出现）；</li>
 *   <li>T-8s 主题音频起播，红色 16 棱光柱以 32 格/秒升至 128 格；</li>
 *   <li>光柱经过各高度时浮现七层空中法阵；</li>
 *   <li>爆前红白球从中心长至 12 格，蓄力完成结算爆炸（纯视觉，伤害由服务端独立结算）；</li>
 *   <li>爆炸后冲击波球扩张至外沿，七层法阵保持 0.7 秒后自上而下逐层加速上飞并淡出。</li>
 * </ol>
 * 全部视觉开启深度测试，可被方块与实体遮挡。超距失败只中止对应序列，不影响其他玩家的爆裂。
 */
public final class ExplosionRenderer {
    private static final Map<UUID, View> SEQUENCES = new HashMap<>();
    private static final Map<UUID, Long> RANGE_SUPPRESSED = new HashMap<>();
    private static ClientLevel world;
    private static boolean registered;

    static final class Geometry {
        static final Sphere SHOCKWAVE = new Sphere(12, 24);
        static final Sphere BALL = new Sphere(8, 24);
        static final double[] BEAM_COS = new double[17];
        static final double[] BEAM_SIN = new double[17];

        static {
            for (int i = 0; i <= 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                BEAM_COS[i] = Math.cos(angle);
                BEAM_SIN[i] = Math.sin(angle);
            }
        }

        private Geometry() {}
    }

    /** 极角坐标的单位球面四边形条带（只缓存方向，半径与颜色在绘制时决定）。 */
    static final class Sphere {
        private final double[] x, y, z;

        Sphere(int latitudes, int longitudes) {
            x = new double[latitudes * longitudes * 4];
            y = new double[x.length];
            z = new double[x.length];
            int vertex = 0;
            for (int lat = 0; lat < latitudes; lat++) {
                double phi0 = Math.PI * lat / latitudes, phi1 = Math.PI * (lat + 1) / latitudes;
                for (int lon = 0; lon < longitudes; lon++) {
                    double theta0 = 2 * Math.PI * lon / longitudes, theta1 = 2 * Math.PI * (lon + 1) / longitudes;
                    double s0 = Math.sin(phi0), c0 = Math.cos(phi0), s1 = Math.sin(phi1), c1 = Math.cos(phi1);
                    put(vertex++, Math.cos(theta0) * s0, c0, Math.sin(theta0) * s0);
                    put(vertex++, Math.cos(theta1) * s0, c0, Math.sin(theta1) * s0);
                    put(vertex++, Math.cos(theta1) * s1, c1, Math.sin(theta1) * s1);
                    put(vertex++, Math.cos(theta0) * s1, c1, Math.sin(theta0) * s1);
                }
            }
        }

        private void put(int vertex, double px, double py, double pz) {
            x[vertex] = px;
            y[vertex] = py;
            z[vertex] = pz;
        }

        int vertexCount() { return x.length; }
    }

    private static final class View {
        final Vec3 center;
        int elapsed;
        int originalPre, preparation;
        boolean detonated;
        int suppressedAt = -1;
        long receivedAt;
        double coreRadius, outerRadius;
        final ExplosionEffects effects = new ExplosionEffects();

        View(Vec3 center, int elapsed, boolean detonated) {
            this.center = center;
            this.elapsed = elapsed;
            this.detonated = detonated;
        }

        float realAge(float delta) { return elapsed + Math.max(0, world.getGameTime() - receivedAt) + delta; }

        float age(float delta) {
            return (float) FormSpellStyle.Timing.logical(realAge(delta), originalPre, preparation);
        }

        double coreRadius() { return coreRadius > 0 ? coreRadius : ExplosionRules.coreRadius(); }

        double outerRadius() { return outerRadius > 0 ? outerRadius : ExplosionRules.outerRadius(); }
    }

    private ExplosionRenderer() {}

    /** 超距失败事件可能先于序列快照到达；两种顺序都只停一次音频，且只停这一个序列。 */
    public static void interruptForRange(UUID id, ClientLevel currentWorld, long now) {
        if (id == null) return;
        if (world != currentWorld) {
            clearSequences();
            world = currentWorld;
        }
        RANGE_SUPPRESSED.put(id, now + ExplosionRules.AFTER_GLOW_TICKS);
        View view = SEQUENCES.get(id);
        if (view != null) view.effects.interrupt(view.center, false);
    }

    private static Map<UUID, View> readSequences(FriendlyByteBuf buf) {
        int count = Math.max(0, Math.min(256, buf.readVarInt()));
        Map<UUID, View> parsed = new HashMap<>();
        for (int i = 0; i < count; i++) {
            UUID owner = buf.readUUID();
            Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int elapsed = buf.readVarInt();
            boolean detonated = buf.readBoolean();
            View incoming = new View(center, elapsed, detonated);
            incoming.coreRadius = buf.readDouble();
            incoming.outerRadius = buf.readDouble();
            incoming.originalPre = buf.readVarInt();
            incoming.preparation = buf.readVarInt();
            incoming.suppressedAt = buf.readVarInt();
            parsed.put(owner, incoming);
        }
        return parsed;
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        AddonNetwork.registerClientReceiver(ExplosionManager.START, buf -> {
            ResourceLocation dimension = buf.readResourceLocation();
            Map<UUID, View> parsed = readSequences(buf);
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || !client.level.dimension().location().equals(dimension)) return;
            if (world != client.level) clearSequences();
            world = client.level;
            SEQUENCES.entrySet().removeIf(entry -> {
                if (parsed.containsKey(entry.getKey())) return false;
                if (!entry.getValue().detonated) entry.getValue().effects.stop();
                return true;
            });
            parsed.forEach((id, incoming) -> {
                View view = SEQUENCES.computeIfAbsent(id, key -> incoming);
                view.elapsed = incoming.elapsed;
                view.originalPre = incoming.originalPre;
                view.preparation = incoming.preparation;
                view.detonated = incoming.detonated;
                view.suppressedAt = incoming.suppressedAt;
                if (view.suppressedAt >= 0) {
                    view.effects.interrupt(view.center,
                            incoming.elapsed == view.suppressedAt && !RANGE_SUPPRESSED.containsKey(id));
                }
                view.receivedAt = world.getGameTime();
                if (incoming.coreRadius > 0) view.coreRadius = incoming.coreRadius;
                if (incoming.outerRadius > 0) view.outerRadius = incoming.outerRadius;
            });
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            clearSequences();
            world = null;
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) render(event);
        });
    }

    private static void tick(Minecraft client) {
        if (client.level != world) {
            clearSequences();
            world = client.level;
        }
        if (world == null) return;
        if (!RANGE_SUPPRESSED.isEmpty()) RANGE_SUPPRESSED.values().removeIf(until -> world.getGameTime() > until);
        SEQUENCES.values().removeIf(view -> {
            if (world.getGameTime() - view.receivedAt <= 45) return false;
            view.effects.stop();
            return true;
        });
        for (View view : SEQUENCES.values()) {
            if (view.suppressedAt < 0) {
                view.effects.tick(world, view.center, (int) view.age(0), view.detonated,
                        view.coreRadius(), view.outerRadius());
            }
        }
    }

    private static void clearSequences() {
        SEQUENCES.values().forEach(view -> view.effects.stop());
        SEQUENCES.clear();
        RANGE_SUPPRESSED.clear();
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        float tickDelta = event.getPartialTick();
        Vec3 selected = SpellcastClient.targetSelectionPreview();
        boolean anyCaster = false;
        for (var player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && CastingVisualState.isExplosion(player.getUUID())) {
                anyCaster = true;
                break;
            }
        }
        if (selected == null && !anyCaster && (world != level || SEQUENCES.isEmpty())) return;
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.debugQuads();
        VertexConsumer vertices = buffers.getBuffer(renderType);
        if (selected != null) {
            matrices.pushPose();
            matrices.translate(selected.x - camera.x, selected.y - camera.y, selected.z - camera.z);
            drawTargetPointer(vertices, matrices.last().pose());
            matrices.popPose();
        }
        // 法术 ID 随共享的施法视觉状态同步，其他玩家的爆裂施法也能看到脚下法阵。
        for (var player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || !CastingVisualState.isExplosion(player.getUUID())) continue;
            Vec3 feet = player.getPosition(tickDelta);
            if (feet.distanceToSqr(camera) > ExplosionRules.VIEW_RANGE * ExplosionRules.VIEW_RANGE) continue;
            matrices.pushPose();
            matrices.translate(feet.x - camera.x, feet.y + 0.025 - camera.y, feet.z - camera.z);
            double rotation = (player.tickCount + tickDelta) * 0.02;
            CastingCircleRenderer.drawGround(vertices, matrices, 2, 0, rotation,
                    CastingVisualState.circleAlpha(player.getUUID()), MagicCircleGeometry.RED_PRIMARY);
            matrices.popPose();
        }
        if (world == level) {
            var listener = client.player;
            for (View view : SEQUENCES.values()) {
                if (listener == null || !ExplosionRules.visualsInRange(listener.distanceToSqr(view.center))) continue;
                float age = view.age(tickDelta);
                matrices.pushPose();
                matrices.translate(view.center.x - camera.x, view.center.y - camera.y, view.center.z - camera.z);
                if (view.suppressedAt >= 0) {
                    renderFinale(matrices, vertices, view.realAge(tickDelta) - view.suppressedAt, view);
                } else if (!view.detonated) {
                    float elapsed = Math.min(ExplosionRules.EXPLODE_TICKS, age);
                    CastingCircleRenderer.drawGround(vertices, matrices, 2.0, 0.025, elapsed * 0.02,
                            Math.min(1f, elapsed / 6f), MagicCircleGeometry.RED_PRIMARY);
                    renderBeam(matrices, vertices, elapsed);
                    renderLayerCircles(matrices, vertices, elapsed);
                    renderBall(matrices, vertices, elapsed);
                } else {
                    renderFinale(matrices, vertices, age - ExplosionRules.EXPLODE_TICKS, view);
                }
                matrices.popPose();
            }
        }
        buffers.endBatch(renderType);
    }

    /** 定位指针：下方倒四棱锥（1 格宽、2 格高、尖点在落点），上方同底正四棱锥（高 2/3 格），整体成八面体。 */
    private static void drawTargetPointer(VertexConsumer vertices, Matrix4f matrix) {
        double[][] corners = {{-0.5, -0.5}, {0.5, -0.5}, {0.5, 0.5}, {-0.5, 0.5}};
        int[] colors = {0xFF3A32, 0xE02020, 0xB81420, 0xFF6550};
        for (int side = 0; side < 4; side++) {
            double[] a = corners[side], b = corners[(side + 1) % 4];
            quad(vertices, matrix, a[0], 2, a[1], b[0], 2, b[1], 0, 0, 0, 0, 0, 0, colors[side], 0.95f);
        }
        quad(vertices, matrix, -0.5, 2, -0.5, 0.5, 2, -0.5, 0.5, 2, 0.5, -0.5, 2, 0.5, 0xFF7660, 0.95f);
        double apex = 2.0 + 2.0 / 3.0;
        int[] topColors = {0xFF6550, 0xFF3A32, 0xE02020, 0xB81420};
        for (int side = 0; side < 4; side++) {
            double[] a = corners[side], b = corners[(side + 1) % 4];
            quad(vertices, matrix, a[0], 2, a[1], b[0], 2, b[1], 0.0, apex, 0.0, 0.0, 0.0, 0.0, topColors[side], 0.95f);
        }
    }

    /** 红色光柱：16 棱圆柱，亮红/深红交替增强立体感；爆前向下收缩归零。 */
    private static void renderBeam(PoseStack matrices, VertexConsumer vertices, float elapsed) {
        double height = ExplosionRules.beamRenderHeight(elapsed);
        if (height <= 0) return;
        double radius = ExplosionRules.BEAM_WIDTH / 2;
        Matrix4f matrix = matrices.last().pose();
        for (int i = 0; i < 16; i++) {
            int color = i % 2 == 0 ? 0xFF5040 : 0xD03020;
            quad(vertices, matrix,
                    Geometry.BEAM_COS[i] * radius, 0, Geometry.BEAM_SIN[i] * radius,
                    Geometry.BEAM_COS[i + 1] * radius, 0, Geometry.BEAM_SIN[i + 1] * radius,
                    Geometry.BEAM_COS[i + 1] * radius, height, Geometry.BEAM_SIN[i + 1] * radius,
                    Geometry.BEAM_COS[i] * radius, height, Geometry.BEAM_SIN[i] * radius, color, 0.9f);
        }
    }

    /** 光柱经过后出现的七层法阵，旋转方向逐层交替。 */
    private static void renderLayerCircles(PoseStack matrices, VertexConsumer vertices, float elapsed) {
        double height = ExplosionRules.beamHeight(elapsed);
        for (int layer = 0; layer < ExplosionRules.CIRCLE_HEIGHTS.length; layer++) {
            if (height < ExplosionRules.CIRCLE_HEIGHTS[layer]) break;
            float appeared = elapsed - (ExplosionRules.SOUND_START_TICKS
                    + (float) (ExplosionRules.CIRCLE_HEIGHTS[layer] / ExplosionRules.BEAM_BLOCKS_PER_TICK));
            float grow = Math.min(1f, appeared / 8f);
            drawLayer(matrices, vertices, layer, ExplosionRules.CIRCLE_HEIGHTS[layer],
                    ExplosionRules.CIRCLE_RADII[layer] * grow, elapsed * (layer % 2 == 0 ? 0.015 : -0.012), grow);
        }
    }

    /** 终章：冲击波球扩张至外沿；七层法阵保持后自上而下逐层加速上飞并线性淡出。超距中止时冻结在中止时刻。 */
    private static void renderFinale(PoseStack matrices, VertexConsumer vertices, float sinceBlast, View view) {
        if (sinceBlast < 0) return;
        if (view.suppressedAt < 0 && sinceBlast < ExplosionRules.SHOCKWAVE_TICKS) {
            float progress = sinceBlast / (float) ExplosionRules.SHOCKWAVE_TICKS;
            renderSphere(matrices, vertices, Geometry.SHOCKWAVE, view.outerRadius() * progress,
                    ExplosionRules.SHOCKWAVE_ALPHA * (1f - progress), false);
        }
        float frozenAge = view.suppressedAt < 0 ? ExplosionRules.EXPLODE_TICKS
                : (float) FormSpellStyle.Timing.logical(view.suppressedAt, view.originalPre, view.preparation);
        double visibleHeight = ExplosionRules.beamHeight(frozenAge);
        int highest = ExplosionRules.CIRCLE_HEIGHTS.length - 1;
        while (highest >= 0 && visibleHeight < ExplosionRules.CIRCLE_HEIGHTS[highest]) highest--;
        for (int layer = 0; layer <= highest; layer++) {
            float appeared = frozenAge - (ExplosionRules.SOUND_START_TICKS
                    + (float) (ExplosionRules.CIRCLE_HEIGHTS[layer] / ExplosionRules.BEAM_BLOCKS_PER_TICK));
            float grow = Math.max(0f, Math.min(1f, appeared / 8f));
            int fromTop = highest - layer;
            float start = ExplosionRules.FINALE_HOLD_TICKS + fromTop * ExplosionRules.FINALE_STAGGER_TICKS;
            float since = sinceBlast - start;
            if (since < 0) {
                drawLayer(matrices, vertices, layer, ExplosionRules.CIRCLE_HEIGHTS[layer],
                        ExplosionRules.CIRCLE_RADII[layer] * grow, sinceBlast * (layer % 2 == 0 ? 0.015 : -0.012), grow);
                continue;
            }
            float progress = since / (float) ExplosionRules.FINALE_RISE_TICKS;
            if (progress >= 1f) continue;
            double rotation = (ExplosionRules.EXPLODE_TICKS + since) * (layer % 2 == 0 ? 0.015 : -0.012);
            drawLayer(matrices, vertices, layer,
                    ExplosionRules.CIRCLE_HEIGHTS[layer] + ExplosionRules.finaleLift(fromTop, since),
                    ExplosionRules.CIRCLE_RADII[layer] * grow, rotation, (1f - progress) * grow);
        }
    }

    /** 爆前红白球：从中心生长至球半径，红白纬度条带交替。 */
    private static void renderBall(PoseStack matrices, VertexConsumer vertices, float elapsed) {
        float since = elapsed - ExplosionRules.BALL_START_TICKS;
        if (since <= 0) return;
        float grow = Math.min(1f, since / (float) ExplosionRules.BALL_LEAD_TICKS);
        renderSphere(matrices, vertices, Geometry.BALL, ExplosionRules.BALL_RADIUS * grow, 0.5f + 0.3f * grow, true);
    }

    private static void renderSphere(PoseStack matrices, VertexConsumer vertices, Sphere sphere, double radius,
                                     float alpha, boolean striped) {
        if (radius <= 0.05 || alpha <= 0f) return;
        Matrix4f matrix = matrices.last().pose();
        for (int v = 0; v < sphere.vertexCount(); v += 4) {
            int color = !striped ? 0xFFFFFF : v / (24 * 4) % 2 == 0 ? 0xFF5040 : 0xFFFFFF;
            quad(vertices, matrix,
                    sphere.x[v] * radius, sphere.y[v] * radius, sphere.z[v] * radius,
                    sphere.x[v + 1] * radius, sphere.y[v + 1] * radius, sphere.z[v + 1] * radius,
                    sphere.x[v + 2] * radius, sphere.y[v + 2] * radius, sphere.z[v + 2] * radius,
                    sphere.x[v + 3] * radius, sphere.y[v + 3] * radius, sphere.z[v + 3] * radius, color, alpha);
        }
    }

    private static void drawLayer(PoseStack matrices, VertexConsumer vertices, int layer, double height,
                                  double radius, double rotation, float alpha) {
        CastingCircleRenderer.drawPattern(vertices, matrices, radius, height, rotation, alpha * 0.94f,
                MagicCircleGeometry.layerStrokes(layer), MagicCircleGeometry.GLOW_COLOR);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3,
                             int color, float alpha) {
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, b = (color & 255) / 255f;
        vertices.vertex(matrix, (float) x0, (float) y0, (float) z0).color(r, g, b, alpha).endVertex();
        vertices.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, alpha).endVertex();
        vertices.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, alpha).endVertex();
        vertices.vertex(matrix, (float) x3, (float) y3, (float) z3).color(r, g, b, alpha).endVertex();
    }
}
