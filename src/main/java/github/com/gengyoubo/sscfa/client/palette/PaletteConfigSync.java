package github.com.gengyoubo.sscfa.client.palette;

import github.com.gengyoubo.sscfa.palette.PaletteCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.color.FormColorData;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;

/**
 * 把分享码同步回 Forge SSC 客户端配置 / 皮肤数据。
 */
public final class PaletteConfigSync {
    private PaletteConfigSync() {}

    public static void apply(PaletteCodec.PaletteData data) {
        int primary = rgbaToArgb(data.primaryRGBA());
        int accent1 = rgbaToArgb(data.accent1RGBA());
        int accent2 = rgbaToArgb(data.accent2RGBA());
        int eyeA = rgbaToArgb(data.eyeARGBA());
        int eyeB = rgbaToArgb(data.eyeBRGBA());

        SscClientConfig.CUSTOM_ENABLE_FORM_COLOR.set(true);
        SscClientConfig.CUSTOM_PRIMARY_COLOR.set(primary);
        SscClientConfig.CUSTOM_ACCENT_COLOR_1.set(accent1);
        SscClientConfig.CUSTOM_ACCENT_COLOR_2.set(accent2);
        SscClientConfig.CUSTOM_EYE_COLOR_A.set(eyeA);
        SscClientConfig.CUSTOM_EYE_COLOR_B.set(eyeB);
        SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE.set(data.primaryGreyReverse());
        SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE.set(data.accent1GreyReverse());
        SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE.set(data.accent2GreyReverse());

        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                PlayerSkinData skin = SscApi.currentSkin(player).orElse(null);
                if (skin != null) {
                    skin.setEnableFormColor(true);
                    skin.setFormColor(new FormTextureUtils.ColorSetting(
                            FormTextureUtils.argb2Abgr(primary),
                            FormTextureUtils.argb2Abgr(accent1),
                            FormTextureUtils.argb2Abgr(accent2),
                            FormTextureUtils.argb2Abgr(eyeA),
                            FormTextureUtils.argb2Abgr(eyeB),
                            data.primaryGreyReverse(), data.accent1GreyReverse(), data.accent2GreyReverse()));
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            FormColorData.client().writeToConfig();
        } catch (Throwable ignored) {
        }
        try {
            SscClientConfig.SPEC.save();
        } catch (Throwable ignored) {
        }
    }

    /** RGBA(R高字节) → ARGB(A高字节)：A 抽到最高位，其余左移补 0。 */
    private static int rgbaToArgb(int rgba) {
        return ((rgba & 0xFF) << 24) | ((rgba >>> 8) & 0x00FFFFFF);
    }
}
