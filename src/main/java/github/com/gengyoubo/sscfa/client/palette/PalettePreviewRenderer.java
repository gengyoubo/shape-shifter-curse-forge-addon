package github.com.gengyoubo.sscfa.client.palette;

import github.com.gengyoubo.sscfa.palette.PaletteCodec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

/**
 * 在附属自定义 Screen 内渲染玩家当前形态的小预览（附属内部副本）。
 *
 * <p>渲染前临时把玩家皮肤数据换成预览配色，渲染后立刻还原，避免污染真实状态。
 */
public final class PalettePreviewRenderer {
    private PalettePreviewRenderer() {}

    /**
     * @param previewCode 槽位代码；null 或空表示预览当前真实配色
     */
    public static void render(GuiGraphics context, int x, int y, int size,
                              int mouseX, int mouseY,
                              LocalPlayer player, String previewCode) {
        if (player == null) return;

        FormTextureUtils.ColorSetting backupColor = null;
        boolean backupEnabled = false;
        boolean swapped = false;
        PlayerSkinData skin = null;

        if (previewCode != null && !previewCode.isEmpty()) {
            try {
                PaletteCodec.PaletteData d = PaletteCodec.decode(previewCode);
                skin = SscApi.currentSkin(player).orElse(null);
                if (skin == null) {
                    drawEntityFollowMouse(context, x, y, size, mouseX, mouseY, player);
                    return;
                }
                backupColor = skin.getFormColor();
                backupEnabled = skin.isEnableFormColor();
                FormTextureUtils.ColorSetting preview = new FormTextureUtils.ColorSetting(
                        FormTextureUtils.rgba2Abgr(d.primaryRGBA()),
                        FormTextureUtils.rgba2Abgr(d.accent1RGBA()),
                        FormTextureUtils.rgba2Abgr(d.accent2RGBA()),
                        FormTextureUtils.rgba2Abgr(d.eyeARGBA()),
                        FormTextureUtils.rgba2Abgr(d.eyeBRGBA()),
                        d.primaryGreyReverse(), d.accent1GreyReverse(), d.accent2GreyReverse());
                skin.setFormColor(preview);
                skin.setEnableFormColor(true);
                swapped = true;
            } catch (PaletteCodec.DecodeException ignored) {
                // 解码失败：回退到当前真实配色
            }
        }

        try {
            drawEntityFollowMouse(context, x, y, size, mouseX, mouseY, player);
        } finally {
            if (swapped) {
                skin.setFormColor(backupColor);
                skin.setEnableFormColor(backupEnabled);
            }
        }
    }

    /** 复刻原版的鼠标跟随旋转。 */
    private static void drawEntityFollowMouse(GuiGraphics context, int x, int y, int size,
                                              int mouseX, int mouseY, LivingEntity entity) {
        InventoryScreen.renderEntityInInventoryFollowsAngle(context, x, y, size,
                (float) (x - mouseX), (float) (y - 50 - mouseY), entity);
    }
}
