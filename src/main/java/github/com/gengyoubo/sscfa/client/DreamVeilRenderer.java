package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.sscfa.ability.NightmareDreamManager;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

/**
 * 食梦魔「入梦」目标屏幕粉色晕影：复用原版反胃灰度渐晕贴图，按粉色染色 + 加法混合，
 * 只有贴图白色边缘在屏幕四周叠粉光。仅入梦目标本人收到 S2C 状态包。
 */
public final class DreamVeilRenderer {
    private static final ResourceLocation VEIL_TEXTURE = new ResourceLocation("textures/misc/nausea.png");

    private static final float TINT_R = 1.0F;
    private static final float TINT_G = 0.43F;
    private static final float TINT_B = 0.78F;
    private static final float BRIGHTNESS = 0.55F;
    private static final int FADE_TICKS = 25;

    private static long startWorldTime = -1L;
    private static long endWorldTime = -1L;

    private DreamVeilRenderer() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_DREAM_VEIL, buf -> {
            int duration = buf.readVarInt();
            UUID nightmareUuid = buf.readUUID();
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            long now = client.level.getGameTime();
            if (duration <= 0) {
                NightmareDreamManager.clientUpdateDreamedBy(nightmareUuid, -1L);
                if (!NightmareDreamManager.clientHasAnyDream(now)) {
                    startWorldTime = -1L;
                    endWorldTime = -1L;
                }
                return;
            }
            NightmareDreamManager.clientUpdateDreamedBy(nightmareUuid, now + duration);
            if (startWorldTime < 0L || endWorldTime <= now) {
                startWorldTime = now;
            }
            endWorldTime = now + duration;
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            startWorldTime = -1L;
            endWorldTime = -1L;
            NightmareDreamManager.clientClear();
        });
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                render(event.getGuiGraphics(), event.getPartialTick()));
    }

    private static void render(GuiGraphics context, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.player.isDeadOrDying()) return;
        if (startWorldTime < 0L) return;

        long now = client.level.getGameTime();
        if (endWorldTime <= now) {
            startWorldTime = -1L;
            endWorldTime = -1L;
            return;
        }

        float progressNow = now + tickDelta;
        float fadeIn = (progressNow - startWorldTime) / FADE_TICKS;
        float fadeOut = (endWorldTime - progressNow) / FADE_TICKS;
        float strength = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
        if (strength <= 0.0F) return;

        int w = context.guiWidth();
        int h = context.guiHeight();

        PoseStack matrices = context.pose();
        matrices.pushPose();
        float scale = Mth.lerp(strength, 2.0F, 1.0F);
        matrices.translate(w / 2.0F, h / 2.0F, 0.0F);
        matrices.scale(scale, scale, scale);
        matrices.translate(-w / 2.0F, -h / 2.0F, 0.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        float a = strength * BRIGHTNESS;
        RenderSystem.setShaderColor(TINT_R * a, TINT_G * a, TINT_B * a, 1.0F);
        context.blit(VEIL_TEXTURE, 0, 0, 0.0F, 0.0F, w, h, w, h);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        matrices.popPose();
    }
}
