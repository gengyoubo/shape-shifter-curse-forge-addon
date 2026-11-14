package github.com.gengyoubo.sscfa.client.colorpicker;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.color.FormColorData;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;

/**
 * 高级调色桥接层：与 Forge SSC 的客户端颜色配置 / 皮肤数据解耦。
 *
 * <p>职责：
 * <ul>
 *   <li>提供本地“工作态”快照（5 颜色 + 3 GreyReverse + enable 开关），所有 UI 改动只更新此快照。</li>
 *   <li>预览：把工作态临时写入客户端 {@link PlayerSkinData}，不发包，仅本地可见。</li>
 *   <li>保存：写回 {@link SscClientConfig} 与 {@link FormColorData}，持久化后同步客户端皮肤数据。</li>
 *   <li>取消：恢复 Screen 打开前的皮肤状态（颜色 + enable）。</li>
 * </ul>
 */
public final class AdvancedColorBridge {
    private AdvancedColorBridge() {}

    /** 颜色字段索引常量，便于 Screen 内统一寻址。 */
    public static final int IDX_PRIMARY  = 0;
    public static final int IDX_ACCENT1  = 1;
    public static final int IDX_ACCENT2  = 2;
    public static final int IDX_EYE_A    = 3;
    public static final int IDX_EYE_B    = 4;
    public static final int COLOR_COUNT  = 5;

    public static final int GR_IDX_PRIMARY = 0;
    public static final int GR_IDX_ACCENT1 = 1;
    public static final int GR_IDX_ACCENT2 = 2;
    public static final int GR_COUNT       = 3;

    /** Screen 当前工作态（ARGB 格式，与 Forge SSC 配置字段保持一致）。 */
    public static final class WorkingState {
        public final int[] colorsARGB = new int[COLOR_COUNT];
        public final boolean[] greyReverse = new boolean[GR_COUNT];
        public boolean enabled;

        public WorkingState copy() {
            WorkingState s = new WorkingState();
            System.arraycopy(this.colorsARGB, 0, s.colorsARGB, 0, COLOR_COUNT);
            System.arraycopy(this.greyReverse, 0, s.greyReverse, 0, GR_COUNT);
            s.enabled = this.enabled;
            return s;
        }
    }

    private static int getInt(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        if (value == null) return fallback;
        try {
            Integer v = value.get();
            return v == null ? fallback : v;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean getBool(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        if (value == null) return fallback;
        try {
            Boolean v = value.get();
            return v == null ? fallback : v;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /** 从当前 Forge SSC 客户端配置读出工作态。 */
    public static WorkingState snapshotFromConfig() {
        WorkingState s = new WorkingState();
        s.enabled = getBool(SscClientConfig.CUSTOM_ENABLE_FORM_COLOR, true);
        s.colorsARGB[IDX_PRIMARY] = getInt(SscClientConfig.CUSTOM_PRIMARY_COLOR, 0);
        s.colorsARGB[IDX_ACCENT1] = getInt(SscClientConfig.CUSTOM_ACCENT_COLOR_1, 0);
        s.colorsARGB[IDX_ACCENT2] = getInt(SscClientConfig.CUSTOM_ACCENT_COLOR_2, 0);
        s.colorsARGB[IDX_EYE_A]   = getInt(SscClientConfig.CUSTOM_EYE_COLOR_A, 0);
        s.colorsARGB[IDX_EYE_B]   = getInt(SscClientConfig.CUSTOM_EYE_COLOR_B, 0);
        s.greyReverse[GR_IDX_PRIMARY] = getBool(SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE, true);
        s.greyReverse[GR_IDX_ACCENT1] = getBool(SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE, true);
        s.greyReverse[GR_IDX_ACCENT2] = getBool(SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE, true);
        return s;
    }

    /** 备份打开 Screen 时的客户端皮肤状态（颜色 + enable），用于取消时回滚预览。 */
    public static final class PreviewBackup {
        FormTextureUtils.ColorSetting colorABGR;
        boolean enabled;
    }

    private static PlayerSkinData currentSkin() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return null;
        return SscApi.currentSkin(p).orElse(null);
    }

    /** 备份本地预览状态。 */
    public static PreviewBackup backupPreview() {
        PlayerSkinData skin = currentSkin();
        if (skin == null) return null;
        PreviewBackup b = new PreviewBackup();
        b.colorABGR = skin.getFormColor();
        b.enabled = skin.isEnableFormColor();
        return b;
    }

    private static FormTextureUtils.ColorSetting toColorSetting(WorkingState s) {
        return new FormTextureUtils.ColorSetting(
                FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_PRIMARY]),
                FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_ACCENT1]),
                FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_ACCENT2]),
                FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_EYE_A]),
                FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_EYE_B]),
                s.greyReverse[GR_IDX_PRIMARY],
                s.greyReverse[GR_IDX_ACCENT1],
                s.greyReverse[GR_IDX_ACCENT2]);
    }

    /** 把工作态推到客户端皮肤数据实现本地预览（不发包、不持久化）。 */
    public static void applyPreview(WorkingState s) {
        if (s == null) return;
        PlayerSkinData skin = currentSkin();
        if (skin == null) return;
        skin.setEnableFormColor(s.enabled);
        skin.setFormColor(toColorSetting(s));
    }

    /** 取消：把备份的客户端状态还原回去（仅本地，不发包）。 */
    public static void restoreFromBackup(PreviewBackup b) {
        if (b == null) return;
        PlayerSkinData skin = currentSkin();
        if (skin == null) return;
        skin.setFormColor(b.colorABGR);
        skin.setEnableFormColor(b.enabled);
    }

    /**
     * 提交保存：写回 Forge SSC 配置 + 持久化 + 同步客户端皮肤数据。
     */
    public static void commitSave(WorkingState s) {
        if (s == null) return;

        SscClientConfig.CUSTOM_ENABLE_FORM_COLOR.set(s.enabled);
        SscClientConfig.CUSTOM_PRIMARY_COLOR.set(s.colorsARGB[IDX_PRIMARY]);
        SscClientConfig.CUSTOM_ACCENT_COLOR_1.set(s.colorsARGB[IDX_ACCENT1]);
        SscClientConfig.CUSTOM_ACCENT_COLOR_2.set(s.colorsARGB[IDX_ACCENT2]);
        SscClientConfig.CUSTOM_EYE_COLOR_A.set(s.colorsARGB[IDX_EYE_A]);
        SscClientConfig.CUSTOM_EYE_COLOR_B.set(s.colorsARGB[IDX_EYE_B]);
        SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE.set(s.greyReverse[GR_IDX_PRIMARY]);
        SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE.set(s.greyReverse[GR_IDX_ACCENT1]);
        SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE.set(s.greyReverse[GR_IDX_ACCENT2]);

        // 立即更新本地可见皮肤状态。
        applyPreview(s);

        // 写入 FormColorData 的无形态全局槽位，并持久化。
        try {
            FormColorData data = FormColorData.client();
            String formKey = "";
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) {
                PlayerFormData form = SscApi.currentForm(p).orElse(null);
                if (form != null && form.getFormId() != null) formKey = form.getFormId();
            }
            if (formKey.isEmpty()) formKey = "ssc_addon_custom";
            ResourceLocation id = ResourceLocation.tryParse(formKey);
            String key = id != null ? id.toString() : formKey;
            data.customSetting.put(key, toColorSetting(s));
            data.writeToConfig();
        } catch (Throwable ignored) {
            // 持久化失败不应阻断界面关闭。
        }

        try {
            SscClientConfig.SPEC.save();
        } catch (Throwable ignored) {
            // 配置落盘失败时仍保留内存值。
        }

        // 没有等价的 Forge 附属同步包时保持无操作，避免引用 Fabric 网络层。
        try {
            sendColors(s);
        } catch (Throwable ignored) {
        }
    }

    /** 兼容 API：Forge 移植没有对应的附属颜色同步包，因此不做任何事。 */
    public static void sendColors(WorkingState s) {
        if (s == null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerSkinData skin = SscApi.currentSkin(player).orElse(null);
        boolean keepOriginal = skin != null && skin.isKeepOriginalSkin();
        net.onixary.shapeShifterCurseForge.network.ModNetwork.CHANNEL.sendToServer(
                new net.onixary.shapeShifterCurseForge.network.UpdateSkinPacket(
                        true,
                        keepOriginal,
                        s.enabled,
                        FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_PRIMARY]),
                        FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_ACCENT1]),
                        FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_ACCENT2]),
                        FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_EYE_A]),
                        FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_EYE_B]),
                        s.greyReverse[GR_IDX_PRIMARY],
                        s.greyReverse[GR_IDX_ACCENT1],
                        s.greyReverse[GR_IDX_ACCENT2]));
    }

    /** 把颜色编码进本地缓冲区（不发送）。 */
    public static FriendlyByteBuf encodeColors(WorkingState s) {
        FriendlyByteBuf cbuf = AddonNetwork.buffer();
        cbuf.writeBoolean(false);
        cbuf.writeInt(FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_PRIMARY]));
        cbuf.writeInt(FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_ACCENT1]));
        cbuf.writeInt(FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_ACCENT2]));
        cbuf.writeInt(FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_EYE_A]));
        cbuf.writeInt(FormTextureUtils.argb2Abgr(s.colorsARGB[IDX_EYE_B]));
        cbuf.writeBoolean(s.greyReverse[GR_IDX_PRIMARY]);
        cbuf.writeBoolean(s.greyReverse[GR_IDX_ACCENT1]);
        cbuf.writeBoolean(s.greyReverse[GR_IDX_ACCENT2]);
        return cbuf;
    }
}
