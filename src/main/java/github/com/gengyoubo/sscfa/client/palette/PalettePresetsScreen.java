package github.com.gengyoubo.sscfa.client.palette;

import github.com.gengyoubo.sscfa.client.colorpicker.AdvancedColorBridge;
import github.com.gengyoubo.sscfa.client.colorpicker.AdvancedColorScreen;
import github.com.gengyoubo.sscfa.client.colorpicker.ColorTutorialOverlay;
import github.com.gengyoubo.sscfa.palette.PaletteCodec;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 配色预设管理界面（左上角固定预览 + 槽位列表可滚动）。
 */
public class PalettePresetsScreen extends Screen {
    private final Screen parent;

    private static final class SlotRow {
        EditBox nameField;
        EditBox codeField;
        Button applyBtn;
        Button exportBtn;
    }

    private final List<SlotRow> slotRows = new ArrayList<>();

    private Component statusLine = Component.empty();
    private int previewSlotIdx = -1;

    private boolean slotsDirty = false;
    private boolean dataLoaded;
    private long lastDirtyMs = 0L;
    private static final long SAVE_DEBOUNCE_MS = 500L;

    private static final int ROW_H        = 22;
    private static final int NAME_W       = 80;
    private static final int CODE_W       = 180;
    private static final int BTN_W        = 44;
    private static final int GAP          = 4;
    private static final int ROW_TOTAL_W  = NAME_W + GAP + CODE_W + GAP + BTN_W + GAP + BTN_W;

    private static final int PREVIEW_X       = 12;
    private static final int PREVIEW_Y       = 24;
    private static final int PREVIEW_W       = 110;
    private static final int PREVIEW_H       = 140;
    private static final int PREVIEW_LABEL_Y = PREVIEW_Y + PREVIEW_H + 4;

    private static final int VIEW_TOP_OFFSET = 60;
    private static final int VISIBLE_ROWS    = 8;

    private int viewportY;
    private int viewportH;
    private int viewportLeft;
    private int viewportRight;
    private int contentTotalH;
    private int maxScroll;
    private int scrollOffset = 0;

    public PalettePresetsScreen(Screen parent) {
        super(Component.translatable("text.ssc_addon.palette.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!dataLoaded) {
            PalettePresetStore.get().reloadForCurrentScope();
            dataLoaded = true;
        }
        slotRows.clear();

        addRenderableWidget(Button.builder(getSyncBtnText(), btn -> {
            PalettePresetStore store = PalettePresetStore.get();
            if (!flushSlotsIfDirty()) return;
            if (!store.setGlobalSync(!store.isGlobalSync())) {
                setStatus("text.ssc_addon.palette.status.save_failed");
                return;
            }
            Minecraft.getInstance().setScreen(new PalettePresetsScreen(parent));
        }).bounds(width - 330, 22, 160, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.palette.btn.export_current"),
                btn -> exportCurrent()).bounds(width - 165, 22, 160, 20).build());

        int openFolderBtnW = 100;
        int openFolderBtnX = PREVIEW_X + (PREVIEW_W - openFolderBtnW) / 2;
        int openFolderBtnY = PREVIEW_LABEL_Y + 12;
        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.palette.btn.open_folder"),
                btn -> openConfigFolder()).bounds(openFolderBtnX, openFolderBtnY, openFolderBtnW, 20).build());

        final int startX = PREVIEW_X + PREVIEW_W + 14;
        this.viewportLeft   = startX;
        this.viewportRight  = Math.min(width - 14, startX + ROW_TOTAL_W);
        this.viewportY      = VIEW_TOP_OFFSET;
        this.viewportH      = VISIBLE_ROWS * ROW_H;
        this.contentTotalH  = PalettePresetStore.SLOT_COUNT * ROW_H;
        this.maxScroll      = Math.max(0, contentTotalH - viewportH);
        this.scrollOffset   = Math.min(scrollOffset, maxScroll);

        List<PalettePresetStore.Slot> slots = PalettePresetStore.get().getSlots();
        int baseY = viewportY;
        for (int i = 0; i < PalettePresetStore.SLOT_COUNT; i++) {
            final int idx = i;
            int x = startX;
            int y = baseY + i * ROW_H;
            SlotRow row = new SlotRow();

            row.nameField = new EditBox(this.font, x, y, NAME_W, 18,
                    Component.translatable("text.ssc_addon.palette.slot_name_placeholder"));
            row.nameField.setMaxLength(24);
            row.nameField.setValue(slots.get(idx).name);
            row.nameField.setResponder(s -> {
                PalettePresetStore.get().getSlots().get(idx).name = s;
                markSlotsDirty();
            });
            addRenderableWidget(row.nameField);
            x += NAME_W + GAP;

            row.codeField = new EditBox(this.font, x, y, CODE_W, 18,
                    Component.translatable("text.ssc_addon.palette.code_placeholder"));
            row.codeField.setMaxLength(64);
            row.codeField.setValue(slots.get(idx).code);
            row.codeField.setResponder(s -> onCodeFieldChanged(idx, s, row.codeField));
            addRenderableWidget(row.codeField);
            x += CODE_W + GAP;

            row.applyBtn = Button.builder(Component.translatable("text.ssc_addon.palette.btn.apply"),
                    b -> doApply(idx)).bounds(x, y - 1, BTN_W, 20).build();
            addRenderableWidget(row.applyBtn);
            x += BTN_W + GAP;

            row.exportBtn = Button.builder(Component.translatable("text.ssc_addon.palette.btn.export"),
                    b -> doExport(idx)).bounds(x, y - 1, BTN_W, 20).build();
            addRenderableWidget(row.exportBtn);

            slotRows.add(row);
        }

        final int botBtnH = 20;
        final int botBtnW = 120;
        final int botGap  = 10;
        int closeY = viewportY + viewportH + 28;
        int botTotalW = botBtnW * 3 + botGap * 2;
        int botStartX = (width - botTotalW) / 2;

        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.palette.btn.to_editor"),
                b -> {
                    if (this.parent instanceof AdvancedColorScreen) {
                        Minecraft.getInstance().setScreen(this.parent);
                    } else {
                        Minecraft.getInstance().setScreen(new AdvancedColorScreen(this.parent));
                    }
                }).bounds(botStartX, closeY, botBtnW, botBtnH).build());

        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.config.close"),
                b -> onClose()).bounds(botStartX + botBtnW + botGap, closeY, botBtnW, botBtnH).build());

        addRenderableWidget(Button.builder(Component.translatable("text.ssc_addon.palette.btn.export_all"),
                btn -> exportAllToFile()).bounds(botStartX + (botBtnW + botGap) * 2, closeY, botBtnW, botBtnH).build());

        updateRowPositions();

        maybeContinuePresetTutorial(botStartX, closeY, botBtnW);
    }

    private void maybeContinuePresetTutorial(int botStartX, int closeY, int botBtnW) {
        if (this.minecraft == null) return;
        if (!AdvancedColorScreen.PENDING_PRESET_TUTORIAL) return;
        AdvancedColorScreen.PENDING_PRESET_TUTORIAL = false;

        List<ColorTutorialOverlay.TutorialStep> steps = new ArrayList<>();
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{width - 332, 20, 164, 24},
                "text.ssc_addon.palette.tutorial.step.sync_scope"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{width - 167, 20, 164, 24},
                "text.ssc_addon.palette.tutorial.step.export_current"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{viewportLeft - 2, viewportY - 2, (viewportRight - viewportLeft) + 4, ROW_H * 2 + 4},
                "text.ssc_addon.palette.tutorial.step.slot_save"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{botStartX + (botBtnW + 10) * 2 - 2, closeY - 2, botBtnW + 4, 24},
                "text.ssc_addon.palette.tutorial.step.export_all"));
        steps.add(new ColorTutorialOverlay.TutorialStep(
                () -> new int[]{botStartX - 2, closeY - 2, botBtnW + 4, 24},
                "text.ssc_addon.palette.tutorial.step.back_editor"));

        this.minecraft.setScreen(new ColorTutorialOverlay(this, () -> {}, steps));
    }

    private void updateRowPositions() {
        int baseY = viewportY;
        int top = viewportY;
        int bottom = viewportY + viewportH;
        for (int i = 0; i < slotRows.size(); i++) {
            int y = baseY + i * ROW_H - scrollOffset;
            SlotRow row = slotRows.get(i);
            row.nameField.setY(y);
            row.codeField.setY(y);
            row.applyBtn.setY(y - 1);
            row.exportBtn.setY(y - 1);
            boolean visible = y + 18 > top && y < bottom;
            setRowVisible(row, visible);
        }
    }

    private static void setRowVisible(SlotRow row, boolean visible) {
        row.nameField.visible = visible;
        row.nameField.active = visible;
        row.codeField.visible = visible;
        row.codeField.active = visible;
        row.applyBtn.visible = visible;
        row.applyBtn.active = visible;
        row.exportBtn.visible = visible;
        row.exportBtn.active = visible;
    }

    private void onCodeFieldChanged(int idx, String text, EditBox field) {
        String trimmed = text == null ? "" : text.trim();
        PalettePresetStore.Slot slot = PalettePresetStore.get().getSlots().get(idx);
        if (trimmed.isEmpty()) {
            slot.code = "";
            markSlotsDirty();
            field.setTextColor(0xFFFFFF);
            return;
        }
        try {
            PaletteCodec.decode(trimmed);
            slot.code = trimmed.toUpperCase();
            if (slot.name == null || slot.name.isEmpty()) {
                slot.name = "Preset " + (idx + 1);
                if (idx < slotRows.size()) slotRows.get(idx).nameField.setValue(slot.name);
            }
            markSlotsDirty();
            field.setTextColor(0xFFFFFF);
            setStatus("text.ssc_addon.palette.status.auto_saved", idx + 1);
        } catch (PaletteCodec.DecodeException e) {
            field.setTextColor(0xFF5555);
        }
    }

    private void markSlotsDirty() {
        slotsDirty = true;
        PalettePresetStore.get().markDirty();
        lastDirtyMs = System.currentTimeMillis();
    }

    private boolean flushSlotsIfDirty() {
        if (!slotsDirty) return true;
        if (PalettePresetStore.get().save()) {
            slotsDirty = false;
            return true;
        }
        lastDirtyMs = System.currentTimeMillis();
        setStatus("text.ssc_addon.palette.status.save_failed");
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (slotsDirty && System.currentTimeMillis() - lastDirtyMs >= SAVE_DEBOUNCE_MS) {
            flushSlotsIfDirty();
        }
        if (this.parent instanceof AdvancedColorScreen owner) {
            AdvancedColorBridge.WorkingState working = owner.getWorkingForPreview();
            if (working != null) {
                AdvancedColorBridge.applyPreview(working);
            }
        }
    }

    @Override
    public void removed() {
        flushSlotsIfDirty();
        super.removed();
    }

    private void doApply(int idx) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || p.connection == null) { setStatus("text.ssc_addon.palette.status.no_player"); return; }
        PalettePresetStore.Slot slot = PalettePresetStore.get().getSlots().get(idx);
        if (slot.isEmpty()) { setStatus("text.ssc_addon.palette.status.empty_slot"); return; }
        PaletteCodec.PaletteData data;
        try { data = PaletteCodec.decode(slot.code); }
        catch (PaletteCodec.DecodeException e) { setStatus("text.ssc_addon.palette.status.invalid_code"); return; }
        if (this.parent instanceof AdvancedColorScreen owner) {
            owner.applyPaletteData(data);
            setStatus("text.ssc_addon.palette.status.apply_preview", idx + 1);
            return;
        }
        PaletteConfigSync.apply(data);
        p.connection.sendCommand("ssc_addon palette apply " + slot.code);
        setStatus("text.ssc_addon.palette.status.apply_sent", idx + 1);
    }

    private void doExport(int idx) {
        PalettePresetStore.Slot slot = PalettePresetStore.get().getSlots().get(idx);
        if (slot.isEmpty()) { setStatus("text.ssc_addon.palette.status.empty_slot"); return; }
        Minecraft.getInstance().keyboardHandler.setClipboard(slot.code);
        setStatus("text.ssc_addon.palette.status.exported", idx + 1);
    }

    private void exportAllToFile() {
        java.nio.file.Path p = PalettePresetStore.get().exportAllToSharedFile();
        if (p == null) { setStatus("text.ssc_addon.palette.status.export_all_failed"); return; }
        Minecraft.getInstance().keyboardHandler.setClipboard(p.toString());
        setStatus("text.ssc_addon.palette.status.export_all_ok", p.toString());
    }

    private void exportCurrent() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) { setStatus("text.ssc_addon.palette.status.no_player"); return; }
        try {
            PlayerSkinData skin = SscApi.currentSkin(p).orElse(null);
            if (skin == null) { setStatus("text.ssc_addon.palette.status.record_failed"); return; }
            FormTextureUtils.ColorSetting cs = skin.getFormColor();
            String code = PaletteCodec.encode(
                    FormTextureUtils.abgr2Rgba(cs.primaryColor()),
                    FormTextureUtils.abgr2Rgba(cs.accentColor1()),
                    FormTextureUtils.abgr2Rgba(cs.accentColor2()),
                    FormTextureUtils.abgr2Rgba(cs.eyeColorA()),
                    FormTextureUtils.abgr2Rgba(cs.eyeColorB()),
                    cs.primaryGreyReverse(), cs.accent1GreyReverse(), cs.accent2GreyReverse());
            Minecraft.getInstance().keyboardHandler.setClipboard(code);
            setStatus("text.ssc_addon.palette.status.current_exported");
        } catch (RuntimeException e) {
            setStatus("text.ssc_addon.palette.status.record_failed");
        }
    }

    private void setStatus(String key, Object... args) { this.statusLine = Component.translatable(key, args); }

    private void openConfigFolder() {
        java.nio.file.Path dir = FMLPaths.CONFIGDIR.get().resolve("ssca");
        try { java.nio.file.Files.createDirectories(dir); } catch (java.io.IOException ignored) {}
        Util.getPlatform().openFile(dir.toFile());
        setStatus("text.ssc_addon.palette.status.folder_opened");
    }

    private Component getSyncBtnText() {
        PalettePresetStore store = PalettePresetStore.get();
        Component state = Component.translatable(store.isGlobalSync()
                ? "text.ssc_addon.palette.sync.global"
                : "text.ssc_addon.palette.sync.per_scope");
        return Component.translatable("text.ssc_addon.palette.sync.label", state);
    }

    private String getPreviewCode() {
        if (previewSlotIdx < 0) return null;
        return PalettePresetStore.get().getSlots().get(previewSlotIdx).code;
    }

    private void updatePreviewSelection() {
        int newIdx = -1;
        for (int i = 0; i < slotRows.size(); i++) {
            SlotRow r = slotRows.get(i);
            if (!r.nameField.visible) continue;
            if (r.nameField.isFocused() || r.codeField.isFocused()) { newIdx = i; break; }
        }
        if (newIdx >= 0) {
            String code = PalettePresetStore.get().getSlots().get(newIdx).code;
            if (code == null || code.isEmpty()) newIdx = -1;
        }
        this.previewSlotIdx = newIdx;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= viewportLeft && mouseX <= viewportRight
                && mouseY >= viewportY && mouseY <= viewportY + viewportH
                && maxScroll > 0) {
            int delta = (int) (-amount * ROW_H);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + delta));
            updateRowPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void onClose() {
        if (flushSlotsIfDirty()) Minecraft.getInstance().setScreen(parent);
    }

    private static void drawBorder(GuiGraphics context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        updateRowPositions();
        updatePreviewSelection();
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        context.fill(PREVIEW_X, PREVIEW_Y, PREVIEW_X + PREVIEW_W, PREVIEW_Y + PREVIEW_H, 0x88000000);
        drawBorder(context, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H, 0xFFAAAAAA);

        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            int cx = PREVIEW_X + PREVIEW_W / 2;
            int cy = PREVIEW_Y + PREVIEW_H - 12;
            PalettePreviewRenderer.render(context, cx, cy, 38,
                    cx - mouseX, cy - 50 - mouseY, p, getPreviewCode());
        }

        Component previewLabel = previewSlotIdx >= 0
                ? Component.translatable("text.ssc_addon.palette.preview.slot", previewSlotIdx + 1)
                : Component.translatable("text.ssc_addon.palette.preview.current");
        context.drawCenteredString(this.font, previewLabel,
                PREVIEW_X + PREVIEW_W / 2, PREVIEW_LABEL_Y, 0xCCCCCC);

        context.drawString(this.font,
                Component.translatable("text.ssc_addon.palette.header.name"),
                viewportLeft + 4, viewportY - 12, 0xFFFFAA);
        context.drawString(this.font,
                Component.translatable("text.ssc_addon.palette.header.code"),
                viewportLeft + NAME_W + GAP + 4, viewportY - 12, 0xFFFFAA);

        if (maxScroll > 0) {
            int trackX = viewportRight + 4;
            int trackW = 4;
            context.fill(trackX, viewportY, trackX + trackW, viewportY + viewportH, 0x44FFFFFF);
            int thumbH = Math.max(20, viewportH * viewportH / contentTotalH);
            int thumbY = viewportY + (viewportH - thumbH) * scrollOffset / maxScroll;
            context.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, 0xFFAAAAAA);
        }

        if (statusLine != null) {
            context.drawCenteredString(this.font, statusLine,
                    this.width / 2, viewportY + viewportH + 8, 0xAAFFAA);
        }
    }
}
