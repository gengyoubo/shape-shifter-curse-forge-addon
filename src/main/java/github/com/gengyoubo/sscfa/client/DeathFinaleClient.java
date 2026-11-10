package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.client.renderer.CastingCircleRenderer;
import github.com.gengyoubo.sscfa.client.renderer.DeathFinaleGeometry;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.DeathFinaleManager;
import github.com.gengyoubo.sscfa.spell.DeathFinaleRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 死亡终章客户端表现：法阵/边界圆环 + 危险目标屏幕晕影。 */
public final class DeathFinaleClient {
    private static final ResourceLocation FROST = new ResourceLocation("textures/misc/powder_snow_outline.png");
    private static final Map<UUID, View> VIEWS = new HashMap<>();
    private static ClientLevel world;

    private record View(UUID owner, Vec3 center, double radius, int duration, int elapsed,
                        boolean released, boolean danger, long received) {
        float age(float delta) {
            return elapsed + Math.max(0, world.getGameTime() - received) + delta;
        }

        Vec3 displayCenter(float delta) {
            var caster = world.getPlayerByUUID(owner);
            return !released && caster != null ? caster.getPosition(delta) : center;
        }
    }

    private DeathFinaleClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(DeathFinaleManager.SNAPSHOT, buf -> readSnapshot(buf));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            VIEWS.clear();
            world = null;
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft client = Minecraft.getInstance();
                if (world != client.level) {
                    VIEWS.clear();
                    world = client.level;
                }
                if (world != null) VIEWS.values().removeIf(v -> world.getGameTime() - v.received() > 30);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) renderWorld(event);
        });
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                renderHud(event.getGuiGraphics(), event.getPartialTick()));
    }

    private static void readSnapshot(FriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        int count = Math.max(0, Math.min(1024, buf.readVarInt()));
        Map<UUID, View> incoming = new HashMap<>();
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            UUID owner = buf.readUUID();
            Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            double radius = buf.readDouble();
            int duration = buf.readVarInt();
            int elapsed = buf.readVarInt();
            incoming.put(id, new View(owner, center, radius, duration, elapsed,
                    buf.readBoolean(), buf.readBoolean(), 0));
        }
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !level.dimension().location().equals(dimension)) return;
        world = level;
        VIEWS.clear();
        incoming.forEach((id, v) -> VIEWS.put(id, new View(v.owner(), v.center(), v.radius(),
                v.duration(), v.elapsed(), v.released(), v.danger(), world.getGameTime())));
    }

    private static void renderWorld(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (world == null || world != client.level) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        RenderType renderType = RenderType.debugQuads();
        VertexConsumer vertices = buffers.getBuffer(renderType);
        float delta = event.getPartialTick();
        boolean drew = false;

        for (View v : VIEWS.values()) {
            float age = v.age(delta);
            float progress = Math.min(1, age / v.duration());
            float alpha = v.released()
                    ? Math.max(0, 1 - (age - v.duration()) / DeathFinaleRules.AFTERGLOW_TICKS) : 1;
            if (alpha <= 0) continue;
            Vec3 center = v.displayCenter(delta);
            matrices.pushPose();
            matrices.translate(center.x - camera.x, center.y + .035 - camera.y, center.z - camera.z);
            CastingCircleRenderer.drawPattern(vertices, matrices, 2.8, 0, age * .008, alpha,
                    DeathFinaleGeometry.STROKES, DeathFinaleGeometry.OCHRE);
            CastingCircleRenderer.ring(vertices, matrices.last().pose(), v.radius(), .055, 0,
                    DeathFinaleGeometry.OCHRE, alpha * .85f);
            for (int axis = 0; axis < 2; axis++) {
                matrices.pushPose();
                matrices.mulPose((axis == 0 ? Axis.XP : Axis.ZP).rotationDegrees(90));
                CastingCircleRenderer.ring(vertices, matrices.last().pose(), v.radius(), .025, 0,
                        DeathFinaleGeometry.OCHRE, alpha * .32f);
                matrices.popPose();
            }
            int marks = Math.max(1, Math.round(v.duration() / 20f));
            int remaining = (int) Math.ceil(marks * (1 - progress));
            for (int i = 0; i < remaining; i++) {
                double angle = -Math.PI / 2 + i * Math.PI * 2 / marks;
                CastingCircleRenderer.line(vertices, matrices.last().pose(),
                        Math.cos(angle) * v.radius() * .92, Math.sin(angle) * v.radius() * .92,
                        Math.cos(angle) * v.radius(), Math.sin(angle) * v.radius(), .10,
                        DeathFinaleGeometry.LIGHT, alpha);
            }
            double movingRadius = v.released()
                    ? v.radius() * Math.min(1, (age - v.duration()) / 8)
                    : Math.max(0.1, v.radius() * (1 - progress));
            CastingCircleRenderer.ring(vertices, matrices.last().pose(), movingRadius, .065, 0,
                    DeathFinaleGeometry.LIGHT, alpha * .65f);
            matrices.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch(renderType);
    }

    private static void renderHud(GuiGraphics ctx, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (world == null || client.level != world || client.player == null || !client.player.isAlive()
                || client.player.isSpectator() || client.options.hideGui) return;
        View danger = VIEWS.values().stream().filter(v -> v.danger() && !v.released())
                .min(java.util.Comparator.comparingDouble(v -> v.duration() - v.age(delta))).orElse(null);
        if (danger == null) return;
        float age = danger.age(delta);
        float progress = Math.min(1, age / danger.duration());
        float pulse = (float) (.5 + .5 * Math.sin(age * (progress > .77f ? .63 : .31)));
        float opacity = .38f + .26f * progress + .10f * pulse;
        int width = ctx.guiWidth();
        int height = ctx.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, .065f, .035f, opacity);
        try {
            ctx.blit(FROST, 0, 0, 0f, 0f, width, height, width, height);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
        int seconds = DeathFinaleRules.secondsLeft((int) age, danger.duration());
        ctx.drawCenteredString(client.font,
                Component.translatable("hud.ssc_addon.death_finale.warning", seconds),
                width / 2, height / 5, 0xFF6655);
    }
}
