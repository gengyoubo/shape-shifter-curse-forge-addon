/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.client.colorpicker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 颜色编辑器首次新手教程覆盖层。
 * <p>
 * 以半透明遮罩覆盖在 {@link AdvancedColorScreen} 之上，逐步用高亮框 + 箭头 + 文字介绍各功能区，
 * 提供「下一步」与「跳过」。看完或跳过后由调用方记录“本存档已看过”。
 */
public class ColorTutorialOverlay extends Screen {
    /** 目标高亮矩形：[x, y, w, h]，由 supplier 在当前屏幕尺寸下实时计算。 */
    public record TutorialStep(Supplier<int[]> targetRect, String textKey, Runnable clickTargetAction, boolean boxAbove) {
        public TutorialStep(Supplier<int[]> targetRect, String textKey) {
            this(targetRect, textKey, null, false);
        }
        public TutorialStep(Supplier<int[]> targetRect, String textKey, Runnable clickTargetAction) {
            this(targetRect, textKey, clickTargetAction, false);
        }
        public TutorialStep(Supplier<int[]> targetRect, String textKey, boolean boxAbove) {
            this(targetRect, textKey, null, boxAbove);
        }
    }

    private final Screen background;
    private final Runnable onFinish;
    private final List<TutorialStep> steps = new ArrayList<>();
    private int index = 0;

    private Button skipButton;

    /**
     * 教程是否激活中（全局）。背景界面在 render 时检查此标志，跳过自身直接绘制的装饰文字。
     */
    public static boolean ACTIVE = false;

    public ColorTutorialOverlay(Screen background, Runnable onFinish, List<TutorialStep> steps) {
        super(Component.translatable("text.ssc_addon.adv_color.tutorial.title"));
        this.background = background;
        this.onFinish = onFinish;
        this.steps.addAll(steps);
        ACTIVE = true;
    }

    @Override
    protected void init() {
        int btnW = 70, btnH = 20;
        skipButton = Button.builder(Component.translatable("text.ssc_addon.adv_color.tutorial.skip"),
                b -> finish()).bounds(this.width - btnW - 10, 10, btnW, btnH).build();
        addRenderableWidget(skipButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            TutorialStep step = index < steps.size() ? steps.get(index) : null;
            if (step != null && step.clickTargetAction() != null) {
                int[] r = step.targetRect().get();
                if (r != null && mouseX >= r[0] && mouseX <= r[0] + r[2]
                        && mouseY >= r[1] && mouseY <= r[1] + r[3]) {
                    step.clickTargetAction().run();
                }
                return true;
            }
            advance();
            return true;
        }
        return false;
    }

    private void advance() {
        if (index >= steps.size() - 1) {
            finish();
        } else {
            index++;
        }
    }

    private void finish() {
        ACTIVE = false;
        if (onFinish != null) onFinish.run();
        if (this.minecraft != null) this.minecraft.setScreen(background);
    }

    @Override
    public void removed() {
        super.removed();
        ACTIVE = false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (background != null) {
            background.render(context, -1, -1, delta);
            context.flush();
        }

        context.pose().pushPose();
        context.pose().translate(0, 0, 300);

        TutorialStep step = index < steps.size() ? steps.get(index) : null;
        int[] rect = step != null ? step.targetRect().get() : null;

        int dim = 0xAA000000;
        if (rect != null) {
            int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
            context.fill(0, 0, this.width, y, dim);
            context.fill(0, y + h, this.width, this.height, dim);
            context.fill(0, y, x, y + h, dim);
            context.fill(x + w, y, this.width, y + h, dim);
            int border = 0xFF7CF77C;
            context.fill(x - 2, y - 2, x + w + 2, y, border);
            context.fill(x - 2, y + h, x + w + 2, y + h + 2, border);
            context.fill(x - 2, y, x, y + h, border);
            context.fill(x + w, y, x + w + 2, y + h, border);
        } else {
            context.fill(0, 0, this.width, this.height, dim);
        }

        if (step != null) {
            Component body = Component.translatable(step.textKey());
            Component hint = (step.clickTargetAction() != null)
                    ? Component.translatable("text.ssc_addon.adv_color.tutorial.click_target")
                    : Component.translatable("text.ssc_addon.adv_color.tutorial.click_continue");
            int boxW = Math.min(340, this.width - 40);
            int boxX = (this.width - boxW) / 2;
            List<FormattedCharSequence> lines = this.font.split(body, boxW - 12);
            int textTop = 18;
            int lineH = 10;
            int hintH = 12;
            int boxH = textTop + lines.size() * lineH + hintH + 4;
            boolean boxOnTop = step.boxAbove();
            int boxY = boxOnTop ? 50 : this.height - 36 - boxH;
            context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF000000);
            context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF101820);
            context.hLine(boxX, boxX + boxW, boxY, 0xFF7CF77C);
            context.hLine(boxX, boxX + boxW, boxY + boxH, 0xFF7CF77C);
            context.vLine(boxX, boxY, boxY + boxH, 0xFF7CF77C);
            context.vLine(boxX + boxW, boxY, boxY + boxH, 0xFF7CF77C);
            Component counter = Component.literal((index + 1) + " / " + steps.size());
            context.drawString(this.font, counter, boxX + 6, boxY + 4, 0xFF7CF77C);
            int ty = boxY + textTop;
            for (FormattedCharSequence line : lines) {
                context.drawString(this.font, line, boxX + 6, ty, 0xFFFFFFFF);
                ty += lineH;
            }
            context.drawString(this.font, hint, boxX + 6, boxY + boxH - hintH, 0xFF7CF77C);

            if (rect != null) {
                int cx = rect[0] + rect[2] / 2;
                int cy = rect[1] + rect[3] / 2;
                int sx = Mth.clamp(cx, boxX + 4, boxX + boxW - 4);
                int sy = (cy < boxY) ? boxY : boxY + boxH;
                drawArrow(context, sx, sy, cx, cy);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        context.flush();
        context.pose().popPose();
    }

    /** 画一条从 (x1,y1) 指向 (x2,y2) 的简易箭头（绿色）。 */
    private void drawArrow(GuiGraphics context, int x1, int y1, int x2, int y2) {
        int color = 0xFF7CF77C;
        int steps = 24;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int px = Math.round(Mth.lerp(t, x1, x2));
            int py = Math.round(Mth.lerp(t, y1, y2));
            context.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
        context.fill(x2 - 3, y2 - 3, x2 + 3, y2 + 3, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
