package github.com.gengyoubo.sscfa.client.mana;

import github.com.gengyoubo.sscfa.client.hud.HudScaling;
import github.com.gengyoubo.sscfa.util.ClientResourceCache;
import github.com.gengyoubo.sscfa.util.ManaBarPos;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.UIPositionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/** 简单资源条 HUD 渲染基类（Forge 版）：统一「读取资源 → 按百分比横向裁剪双层贴图」。 */
public abstract class SimpleResourceBarRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final List<SimpleResourceBarRenderer> BARS = new ArrayList<>();
    private static boolean registered;

    protected abstract ResourceLocation resourceId();

    protected abstract ResourceLocation texFull();

    protected abstract ResourceLocation texEmpty();

    protected int width() {
        return 80;
    }

    protected int height() {
        return 5;
    }

    protected boolean requirePower() {
        return false;
    }

    protected int[] manaBarPosArgs() {
        return ManaBarPos.get(8, 100, -17);
    }

    protected int barX(Tuple<Integer, Integer> pos) {
        return pos.getA();
    }

    protected int barY(Tuple<Integer, Integer> pos) {
        return pos.getB();
    }

    public static void register(SimpleResourceBarRenderer bar) {
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
                for (SimpleResourceBarRenderer renderer : BARS) {
                    renderer.render(event.getGuiGraphics(), event.getPartialTick());
                }
            });
        }
        BARS.add(bar);
    }

    public void render(GuiGraphics context, float tickDelta) {
        HudScaling.render(context, 1, () -> renderUnscaled(context, tickDelta));
    }

    private void renderUnscaled(GuiGraphics context, float tickDelta) {
        if (MC.options.hideGui || MC.player == null) return;
        Player player = MC.player;

        if (requirePower() && !ClientResourceCache.has(player, resourceId())) return;

        int[] valMax = PowerUtils.getClientResourceValueAndMax(player, resourceId());
        int current = valMax[0];
        int max = valMax[1];
        if (!requirePower()) {
            if (current <= 0 && max <= 1) return;
        }
        if (current < 0) current = 0;
        if (max <= 0) return;
        double percent = (double) current / (double) max;

        int[] mp = manaBarPosArgs();
        Tuple<Integer, Integer> pos = UIPositionUtils.getCorrectPosition(mp[0], mp[1], mp[2]);
        int x = barX(pos);
        int y = barY(pos);

        int w = width();
        int h = height();
        context.blit(texEmpty(), x, y, 0, 0, w, h, w, h);
        if (current > 0) {
            int filledWidth = (int) Math.ceil(w * percent);
            filledWidth = Math.max(0, Math.min(w, filledWidth));
            if (filledWidth > 0) {
                context.blit(texFull(), x, y, 0, 0, filledWidth, h, w, h);
            }
        }
    }
}
