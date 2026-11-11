package github.com.gengyoubo.sscfa.client.colorpicker;

import github.com.gengyoubo.sscfa.client.palette.PalettePresetsScreen;
import github.com.gengyoubo.sscfa.config.SSCAddonClientConfig;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import github.com.gengyoubo.sscfa.palette.PaletteCodec;
import github.com.gengyoubo.sscfa.util.ClientSaveUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * 高级调色 GUI：
 *  - 顶部：标题 + 启用复选框
 *  - 第二行：5 个色块切换“当前编辑哪一项颜色”
 *  - 三栏并排：左 RGBA 文本框 / 中 R/G/B/A 滑条 / 右 HSV 全色图（共享同一 working color，全联动）
 *  - 中下：3 个 GreyReverse 复选框（独立常驻）
 *  - 下方居中：玩家预览框
 *  - 底部：保存 / 取消（带二次确认）
 *
 * 所有同步都走 {@link AdvancedColorBridge}。
 */
public class AdvancedColorScreen extends Screen {
    private final Screen parent;

    private AdvancedColorBridge.WorkingState working;
    private AdvancedColorBridge.WorkingState initialSnapshot;
    private AdvancedColorBridge.PreviewBackup previewBackup;

    private int editingIdx = AdvancedColorBridge.IDX_PRIMARY;

    private final Button[] colorTabs = new Button[AdvancedColorBridge.COLOR_COUNT];
    private EditBox rField, gField, bField, aField, hexField;
    private RgbSlider sliderR, sliderG, sliderB, sliderA;
    private HsvPickerWidget hsvPicker;
    private final Checkbox[] grChecks = new Checkbox[AdvancedColorBridge.GR_COUNT];
    private Checkbox enableCheck;

    private boolean suppressCallbacks = false;

    private Component statusLine = Component.empty();
    private long statusSetTimeMs = 0L;
    private static final long STATUS_HOLD_MS = 5000L;
    private static final long STATUS_FADE_MS = 2000L;

    @SuppressWarnings("unused")
    private void setStatus(Component t) {
        this.statusLine = t;
        this.statusSetTimeMs = System.currentTimeMillis();
    }

    public AdvancedColorScreen(Screen parent) {
        super(Component.translatable("text.ssc_addon.adv_color.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (working == null) {
            this.initialSnapshot = AdvancedColorBridge.snapshotFromConfig();
            this.working = initialSnapshot.copy();
            this.previewBackup = AdvancedColorBridge.backupPreview();
            AdvancedColorBridge.applyPreview(working);
        }

        int topY = 26;
        enableCheck = new Checkbox(12, topY, 20, 20,
                Component.translatable("text.ssc_addon.adv_color.enable"), working.enabled) {
            @Override
            public void onPress() {
                super.onPress();
                working.enabled = this.selected();
                AdvancedColorBridge.applyPreview(working);
            }
        };
        addRenderableWidget(enableCheck);

        int swatchSize = 22;
        int swatchGap  = 6;
        int swatchTotalW = AdvancedColorBridge.COLOR_COUNT * swatchSize + (AdvancedColorBridge.COLOR_COUNT - 1) * swatchGap;
        int swatchStartX = (width - swatchTotalW) / 2;
        for (int i = 0; i < AdvancedColorBridge.COLOR_COUNT; i++) {
            final int idx = i;
            int sx = swatchStartX + i * (swatchSize + swatchGap);
            SwatchButton btn = new SwatchButton(sx, topY, swatchSize, swatchSize,
                    b -> { editingIdx = idx; refreshAllFromWorking(); });
            colorTabs[i] = btn;
            addRenderableWidget(btn);
        }

        int colTop = 70;
        int colH   = 170;

        int totalAvail = width - 24;
        int gapBetween = 14;
        int colW = (totalAvail - 2 * gapBetween) / 3;

        int leftX  = 12;
        int midX   = leftX + colW + gapBetween;
        int rightX = midX + colW + gapBetween;

        int lineY = colTop + 14;
        int labelW = 18;
        int fieldW = Math.min(60, colW - labelW - 4);
        rField = makeNumField(leftX + labelW, lineY, fieldW, 0, 255, c -> updateColorChannel(0, c));
        gField = makeNumField(leftX + labelW, lineY + 22, fieldW, 0, 255, c -> updateColorChannel(1, c));
        bField = makeNumField(leftX + labelW, lineY + 44, fieldW, 0, 255, c -> updateColorChannel(2, c));
        aField = makeNumField(leftX + labelW, lineY + 66, fieldW, 0, 255, c -> updateColorChannel(3, c));
        addRenderableWidget(rField); addRenderableWidget(gField); addRenderableWidget(bField); addRenderableWidget(aField);

        hexField = new EditBox(this.font, leftX, lineY + 92,
                Math.min(110, colW), 18, Component.literal("#"));
        hexField.setMaxLength(9);
        hexField.setResponder(s -> { if (!suppressCallbacks) applyHexInput(s); });
        addRenderableWidget(hexField);

        int sliderW = colW;
        int sliderH = 14;
        int sliderGap = 18;
        int sliderTopY = colTop + 90 + 12;
        sliderR = new RgbSlider(midX, sliderTopY,                  sliderW, sliderH, "R", 0,   c -> updateColorChannel(0, c));
        sliderG = new RgbSlider(midX, sliderTopY + sliderGap,      sliderW, sliderH, "G", 0,   c -> updateColorChannel(1, c));
        sliderB = new RgbSlider(midX, sliderTopY + sliderGap * 2,  sliderW, sliderH, "B", 0,   c -> updateColorChannel(2, c));
        sliderA = new RgbSlider(midX, sliderTopY + sliderGap * 3,  sliderW, sliderH, "A", 255, c -> updateColorChannel(3, c));
        addRenderableWidget(sliderR); addRenderableWidget(sliderG); addRenderableWidget(sliderB); addRenderableWidget(sliderA);

        int svSize = Math.min(96, colH - 10);
        int hueW   = 12;
        int hsvX = rightX + Math.max(0, (colW - (svSize + 4 + hueW)) / 2);
        hsvPicker = new HsvPickerWidget(hsvX, lineY, svSize, hueW, rgb -> {
            if (suppressCallbacks) return;
            int curAlpha = (working.colorsARGB[editingIdx] >>> 24) & 0xFF;
            int newARGB = (curAlpha << 24) | (rgb & 0x00FFFFFF);
            working.colorsARGB[editingIdx] = newARGB;
            refreshLeftMidFromWorking();
            AdvancedColorBridge.applyPreview(working);
        });

        int grStartY = lineY + svSize + 10;
        int grRowH = 22;
        for (int i = 0; i < AdvancedColorBridge.GR_COUNT; i++) {
            final int idx = i;
            String key = switch (i) {
                case 0 -> "text.ssc_addon.adv_color.grey_reverse.primary";
                case 1 -> "text.ssc_addon.adv_color.grey_reverse.accent1";
                default -> "text.ssc_addon.adv_color.grey_reverse.accent2";
            };
            grChecks[i] = new Checkbox(rightX, grStartY + i * grRowH, 20, 20,
                    Component.translatable(key), working.greyReverse[idx]) {
                @Override
                public void onPress() {
                    super.onPress();
                    working.greyReverse[idx] = this.selected();
                    AdvancedColorBridge.applyPreview(working);
                }
            };
            addRenderableWidget(grChecks[i]);
        }

        final int botBtnH = 20;
        final int botBtnW = 100;
        final int botGap  = 10;
        int btnY = height - 28;
        int botTotalW = botBtnW * 3 + botGap * 2;
        int botStartX = (width - botTotalW) / 2;
        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.adv_color.btn.to_presets"),
                b -> Minecraft.getInstance().setScreen(new PalettePresetsScreen(this)))
                .bounds(botStartX, btnY, botBtnW, botBtnH).build());
        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.adv_color.btn.cancel"),
                b -> requestCancel()).bounds(botStartX + botBtnW + botGap, btnY, botBtnW, botBtnH).build());
        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.adv_color.btn.save"),
                b -> doSave()).bounds(botStartX + (botBtnW + botGap) * 2, btnY, botBtnW, botBtnH).build());

        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.adv_color.tutorial.replay"),
                b -> {
                    tutorialPrompted = true;
                    this.minecraft.setScreen(new ColorTutorialOverlay(this, () -> {}, buildTutorialSteps()));
                }).bounds(8, height - 22, 64, 16).build());

        refreshAllFromWorking();

        maybeShowTutorial();
    }

    private boolean tutorialPrompted = false;

    private void maybeShowTutorial() {
        if (this.minecraft == null) return;
        if (tutorialPrompted) return;
        String saveId = ClientSaveUtils.getCurrentSaveId();
        SSCAddonClientConfig cfg = SSCAddonConfig.client();
        if (cfg.colorTutorialSeenSaves.contains(saveId)) return;
        tutorialPrompted = true;

        Runnable markSeen = () -> {
            if (!cfg.colorTutorialSeenSaves.contains(saveId)) {
                cfg.colorTutorialSeenSaves.add(saveId);
            }
        };

        ConfirmScreen confirm = new ConfirmScreen(
                yes -> {
                    if (yes) {
                        this.minecraft.setScreen(new ColorTutorialOverlay(this, markSeen, buildTutorialSteps()));
                    } else {
                        markSeen.run();
                        this.minecraft.setScreen(this);
                    }
                },
                Component.translatable("text.ssc_addon.adv_color.tutorial.prompt.title"),
                Component.translatable("text.ssc_addon.adv_color.tutorial.prompt.body"),
                Component.translatable("text.ssc_addon.adv_color.tutorial.prompt.yes"),
                Component.translatable("text.ssc_addon.adv_color.tutorial.prompt.no"));
        this.minecraft.setScreen(confirm);
    }

    private java.util.List<ColorTutorialOverlay.TutorialStep> buildTutorialSteps() {
        java.util.List<ColorTutorialOverlay.TutorialStep> steps = new java.util.ArrayList<>();
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> rectOf(colorTabs[0], colorTabs[colorTabs.length - 1]),
                "text.ssc_addon.adv_color.tutorial.step.swatches"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{8, 64, this.width - 16, 180},
                "text.ssc_addon.adv_color.tutorial.step.edit_area"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{(width - (100 * 3 + 10 * 2)) / 2 - 2, height - 30, 100 * 3 + 10 * 2 + 4, 24},
                "text.ssc_addon.adv_color.tutorial.step.save_export"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{(width - (100 * 3 + 10 * 2)) / 2 - 2, height - 30, 100 + 4, 24},
                "text.ssc_addon.adv_color.tutorial.step.goto_presets",
                () -> {
                    PENDING_PRESET_TUTORIAL = true;
                    Minecraft.getInstance().setScreen(new PalettePresetsScreen(this));
                }));
        return steps;
    }

    public static boolean PENDING_PRESET_TUTORIAL = false;

    private static int[] rectOf(AbstractWidget a, AbstractWidget b) {
        int x1 = Math.min(a.getX(), b.getX());
        int y1 = Math.min(a.getY(), b.getY());
        int x2 = Math.max(a.getX() + a.getWidth(), b.getX() + b.getWidth());
        int y2 = Math.max(a.getY() + a.getHeight(), b.getY() + b.getHeight());
        return new int[]{x1 - 2, y1 - 2, x2 - x1 + 4, y2 - y1 + 4};
    }

    private EditBox makeNumField(int x, int y, int w, int min, int max, java.util.function.IntConsumer onValid) {
        EditBox f = new EditBox(this.font, x, y, w, 18, Component.literal(""));
        f.setMaxLength(3);
        f.setResponder(s -> {
            if (suppressCallbacks) return;
            try {
                int v = Integer.parseInt(s.trim());
                if (v >= min && v <= max) onValid.accept(v);
            } catch (NumberFormatException ignored) {}
        });
        return f;
    }

    private void updateColorChannel(int channel, int v) {
        int argb = working.colorsARGB[editingIdx];
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        switch (channel) {
            case 0: r = v; break;
            case 1: g = v; break;
            case 2: b = v; break;
            case 3: a = v; break;
        }
        working.colorsARGB[editingIdx] = (a << 24) | (r << 16) | (g << 8) | b;
        refreshOthersExceptChannel(channel);
        AdvancedColorBridge.applyPreview(working);
    }

    private void refreshOthersExceptChannel(int changedChannel) {
        suppressCallbacks = true;
        try {
            int argb = working.colorsARGB[editingIdx];
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;
            rField.setValue(String.valueOf(r)); sliderR.setIntValue(r);
            gField.setValue(String.valueOf(g)); sliderG.setIntValue(g);
            bField.setValue(String.valueOf(b)); sliderB.setIntValue(b);
            aField.setValue(String.valueOf(a)); sliderA.setIntValue(a);
            hexField.setValue(toHexString(argb));
            hsvPicker.setFromRgb(argb & 0x00FFFFFF);
        } finally {
            suppressCallbacks = false;
        }
    }

    private void refreshLeftMidFromWorking() {
        suppressCallbacks = true;
        try {
            int argb = working.colorsARGB[editingIdx];
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;
            rField.setValue(String.valueOf(r)); sliderR.setIntValue(r);
            gField.setValue(String.valueOf(g)); sliderG.setIntValue(g);
            bField.setValue(String.valueOf(b)); sliderB.setIntValue(b);
            aField.setValue(String.valueOf(a)); sliderA.setIntValue(a);
            hexField.setValue(toHexString(argb));
        } finally {
            suppressCallbacks = false;
        }
    }

    private void refreshAllFromWorking() {
        refreshLeftMidFromWorking();
        suppressCallbacks = true;
        try {
            hsvPicker.setFromRgb(working.colorsARGB[editingIdx] & 0x00FFFFFF);
        } finally {
            suppressCallbacks = false;
        }
    }

    private void applyHexInput(String s) {
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (!t.matches("[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8}")) return;
        long parsed;
        try { parsed = Long.parseLong(t, 16); } catch (NumberFormatException e) { return; }
        int argb;
        if (t.length() == 6) {
            int curA = (working.colorsARGB[editingIdx] >>> 24) & 0xFF;
            argb = (curA << 24) | ((int) parsed & 0x00FFFFFF);
        } else {
            argb = (int) parsed;
        }
        working.colorsARGB[editingIdx] = argb;
        suppressCallbacks = true;
        try {
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;
            rField.setValue(String.valueOf(r)); sliderR.setIntValue(r);
            gField.setValue(String.valueOf(g)); sliderG.setIntValue(g);
            bField.setValue(String.valueOf(b)); sliderB.setIntValue(b);
            aField.setValue(String.valueOf(a)); sliderA.setIntValue(a);
            hsvPicker.setFromRgb(argb & 0x00FFFFFF);
        } finally {
            suppressCallbacks = false;
        }
        AdvancedColorBridge.applyPreview(working);
    }

    private static String toHexString(int argb) {
        return String.format("%08X", argb);
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredString(this.font, this.title, width / 2, 8, 0xFFFFFF);

        for (int i = 0; i < AdvancedColorBridge.COLOR_COUNT; i++) {
            Button btn = colorTabs[i];
            int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
            int col = working.colorsARGB[i] | 0xFF000000;
            ctx.fill(bx + 2, by + 2, bx + bw - 2, by + bh - 2, col);
            int border = (i == editingIdx) ? 0xFFFFFF55 : 0xFF000000;
            ctx.fill(bx, by, bx + bw, by + 1, border);
            ctx.fill(bx, by + bh - 1, bx + bw, by + bh, border);
            ctx.fill(bx, by, bx + 1, by + bh, border);
            ctx.fill(bx + bw - 1, by, bx + bw, by + bh, border);

            if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh) {
                ctx.renderTooltip(this.font, slotNameText(i), mouseX, mouseY);
            }
        }

        if (colorTabs[0] != null) {
            int labelY = colorTabs[0].getY() + colorTabs[0].getHeight() + 4;
            ctx.drawCenteredString(this.font,
                    Component.translatable("text.ssc_addon.adv_color.current_editing", slotNameText(editingIdx)),
                    width / 2, labelY, 0xFFFFAA);
        }

        int colTop = 70;
        int totalAvail = width - 24;
        int gapBetween = 14;
        int colW = (totalAvail - 2 * gapBetween) / 3;
        int leftX  = 12;
        int midX   = leftX + colW + gapBetween;
        int rightX = midX + colW + gapBetween;
        int lineY = colTop + 14;
        ctx.drawString(this.font, Component.translatable("text.ssc_addon.adv_color.col.rgba"), leftX, colTop, 0xFFFFAA);
        ctx.drawString(this.font, Component.translatable("text.ssc_addon.adv_color.col.wheel"), rightX, colTop, 0xFFFFAA);
        int sliderTopY = colTop + 90 + 12;
        ctx.drawString(this.font, Component.translatable("text.ssc_addon.adv_color.col.slider"), midX, sliderTopY - 11, 0xFFFFAA);

        ctx.drawString(this.font, Component.literal("R"), leftX + 4, lineY + 5,  0xFF6666);
        ctx.drawString(this.font, Component.literal("G"), leftX + 4, lineY + 27, 0x66FF66);
        ctx.drawString(this.font, Component.literal("B"), leftX + 4, lineY + 49, 0x6666FF);
        ctx.drawString(this.font, Component.literal("A"), leftX + 4, lineY + 71, 0xCCCCCC);

        hsvPicker.render(ctx, mouseX, mouseY);

        int previewW = 76, previewH = 90;
        int previewX = midX + (colW - previewW) / 2;
        int previewY = colTop;
        ctx.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0x88000000);
        drawBorder(ctx, previewX, previewY, previewW, previewH, 0xFFAAAAAA);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            int cx = previewX + previewW / 2;
            int cy = previewY + previewH - 4;
            InventoryScreen.renderEntityInInventoryFollowsAngle(ctx, cx, cy, 40,
                    (float) (cx - mouseX), (float) (cy - 50 - mouseY), player);
        }

        if (statusLine != null && !statusLine.getString().isEmpty() && statusSetTimeMs > 0L) {
            long elapsed = System.currentTimeMillis() - statusSetTimeMs;
            int alpha;
            if (elapsed < STATUS_HOLD_MS) {
                alpha = 0xFF;
            } else if (elapsed < STATUS_HOLD_MS + STATUS_FADE_MS) {
                long fadeIn = elapsed - STATUS_HOLD_MS;
                alpha = (int) (0xFF * (1.0 - (double) fadeIn / STATUS_FADE_MS));
                if (alpha < 0) alpha = 0;
                if (alpha > 0xFF) alpha = 0xFF;
            } else {
                alpha = 0;
            }
            if (alpha > 4) {
                int color = (alpha << 24) | 0xAAFFAA;
                ctx.drawCenteredString(this.font, statusLine, width / 2, height - 28 - 14, color);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (working != null) {
            AdvancedColorBridge.applyPreview(working);
        }
    }

    public AdvancedColorBridge.WorkingState getWorkingForPreview() {
        return working;
    }

    public void applyPaletteData(PaletteCodec.PaletteData data) {
        if (working == null) {
            this.initialSnapshot = AdvancedColorBridge.snapshotFromConfig();
            this.working = initialSnapshot.copy();
            this.previewBackup = AdvancedColorBridge.backupPreview();
        }
        working.enabled = true;
        working.colorsARGB[AdvancedColorBridge.IDX_PRIMARY] = rgbaToArgb(data.primaryRGBA());
        working.colorsARGB[AdvancedColorBridge.IDX_ACCENT1] = rgbaToArgb(data.accent1RGBA());
        working.colorsARGB[AdvancedColorBridge.IDX_ACCENT2] = rgbaToArgb(data.accent2RGBA());
        working.colorsARGB[AdvancedColorBridge.IDX_EYE_A]   = rgbaToArgb(data.eyeARGBA());
        working.colorsARGB[AdvancedColorBridge.IDX_EYE_B]   = rgbaToArgb(data.eyeBRGBA());
        working.greyReverse[AdvancedColorBridge.GR_IDX_PRIMARY] = data.primaryGreyReverse();
        working.greyReverse[AdvancedColorBridge.GR_IDX_ACCENT1] = data.accent1GreyReverse();
        working.greyReverse[AdvancedColorBridge.GR_IDX_ACCENT2] = data.accent2GreyReverse();
        AdvancedColorBridge.applyPreview(working);
    }

    private static int rgbaToArgb(int rgba) {
        return ((rgba & 0xFF) << 24) | ((rgba >>> 8) & 0x00FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hsvPicker != null && hsvPicker.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (hsvPicker != null && hsvPicker.mouseDragged(mx, my, button, dx, dy)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (hsvPicker != null && hsvPicker.mouseReleased(mx, my, button)) return true;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        requestCancel();
    }

    private void requestCancel() {
        if (isDirty()) {
            Minecraft.getInstance().setScreen(new UnsavedConfirmScreen(this));
        } else {
            performCancel();
        }
    }

    private boolean isDirty() {
        if (initialSnapshot == null) return false;
        if (working.enabled != initialSnapshot.enabled) return true;
        for (int i = 0; i < AdvancedColorBridge.COLOR_COUNT; i++) {
            if (working.colorsARGB[i] != initialSnapshot.colorsARGB[i]) return true;
        }
        for (int i = 0; i < AdvancedColorBridge.GR_COUNT; i++) {
            if (working.greyReverse[i] != initialSnapshot.greyReverse[i]) return true;
        }
        return false;
    }

    private void performCancel() {
        AdvancedColorBridge.restoreFromBackup(previewBackup);
        Minecraft.getInstance().setScreen(parent);
    }

    private void doSave() {
        AdvancedColorBridge.commitSave(working);
        Minecraft.getInstance().setScreen(parent);
    }

    private static Component slotNameText(int idx) {
        return switch (idx) {
            case AdvancedColorBridge.IDX_PRIMARY -> Component.translatable("text.ssc_addon.adv_color.slot.primary");
            case AdvancedColorBridge.IDX_ACCENT1 -> Component.translatable("text.ssc_addon.adv_color.slot.accent1");
            case AdvancedColorBridge.IDX_ACCENT2 -> Component.translatable("text.ssc_addon.adv_color.slot.accent2");
            case AdvancedColorBridge.IDX_EYE_A   -> Component.translatable("text.ssc_addon.adv_color.slot.eye_a");
            case AdvancedColorBridge.IDX_EYE_B   -> Component.translatable("text.ssc_addon.adv_color.slot.eye_b");
            default -> Component.literal("?");
        };
    }

    private static class RgbSlider extends AbstractSliderButton {
        private final String channelLabel;
        private final java.util.function.IntConsumer onValueChanged;

        RgbSlider(int x, int y, int w, int h, String channelLabel, int initial, java.util.function.IntConsumer onValueChanged) {
            super(x, y, w, h, Component.literal(channelLabel + ": " + initial), initial / 255.0);
            this.channelLabel = channelLabel;
            this.onValueChanged = onValueChanged;
            updateMessage();
        }

        public void setIntValue(int v) {
            this.value = Math.max(0, Math.min(255, v)) / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int v = (int) Math.round(this.value * 255.0);
            this.setMessage(Component.literal(channelLabel + ": " + v));
        }

        @Override
        protected void applyValue() {
            if (onValueChanged != null) {
                int v = (int) Math.round(this.value * 255.0);
                onValueChanged.accept(v);
            }
        }
    }

    /** 色块按钮：仅作为可点击区域，vanilla 背景全部抑制；色块本身由 Screen.render() 自行绘制。 */
    private static class SwatchButton extends Button {
        SwatchButton(int x, int y, int w, int h, OnPress onPress) {
            super(x, y, w, h, Component.literal(""), onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
            // 不画任何 vanilla 背景，避免 hover 灰色覆盖外部色块绘制。
        }
    }

    /** 未保存退出确认：3 选项（保存并退出 / 直接退出 / 返回）。 */
    private static class UnsavedConfirmScreen extends Screen {
        private final AdvancedColorScreen owner;

        protected UnsavedConfirmScreen(AdvancedColorScreen owner) {
            super(Component.translatable("text.ssc_addon.adv_color.confirm.unsaved_title"));
            this.owner = owner;
        }

        @Override
        protected void init() {
            int btnW = 160;
            int btnH = 20;
            int gap  = 6;
            int x = (width - btnW) / 2;
            int y = height / 2 + 20;

            addRenderableWidget(Button.builder(
                    Component.translatable("text.ssc_addon.adv_color.confirm.save_and_exit"),
                    b -> owner.doSave()
            ).bounds(x, y, btnW, btnH).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("text.ssc_addon.adv_color.confirm.discard_and_exit"),
                    b -> owner.performCancel()
            ).bounds(x, y + (btnH + gap), btnW, btnH).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("text.ssc_addon.adv_color.confirm.back"),
                    b -> Minecraft.getInstance().setScreen(owner)
            ).bounds(x, y + (btnH + gap) * 2, btnW, btnH).build());
        }

        @Override
        public void render(GuiGraphics ctx, int mx, int my, float delta) {
            this.renderBackground(ctx);
            ctx.drawCenteredString(this.font, this.title, width / 2, height / 2 - 40, 0xFFFFFF);
            ctx.drawCenteredString(this.font,
                    Component.translatable("text.ssc_addon.adv_color.confirm.unsaved_msg"),
                    width / 2, height / 2 - 20, 0xC0C0C0);
            super.render(ctx, mx, my, delta);
        }

        @Override
        public void tick() {
            AdvancedColorBridge.applyPreview(owner.working);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(owner);
        }
    }
}
