package github.com.gengyoubo.sscfa.client.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.block.RegAddonBlocks;
import github.com.gengyoubo.sscfa.client.SpellcastKeybindings;
import github.com.gengyoubo.sscfa.item.MoonDustReliefItem;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.research.RuneRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** 独立画面中的自动演示；只绘制示意物，不操作真实物品、研究进度或施法。 */
public final class SpellTutorialScreen extends Screen {
    public static final int PAGE_COUNT = 9;
    private static final ResourceLocation PANEL = new ResourceLocation("textures/gui/demo_background.png");
    private static final ResourceLocation RUNES =
            new ResourceLocation("ssc_addon", "textures/gui/formation_research/runes.png");
    private static final String[] PAGES = {
            "overview", "analysis", "identify", "runes", "upgrade", "enhance", "casting", "collapse", "growth"};

    private final int[] glyphs;
    private int page;
    private int elapsed;
    private int ticks;
    private int bodyScroll;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private boolean paused;
    private Button previous;
    private Button next;
    private Button pause;

    public SpellTutorialScreen(int[] glyphs) {
        super(text("title"));
        this.glyphs = glyphs.clone();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("tutorial.ssc_addon." + key, args);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(390, width - 16);
        panelHeight = Math.min(232, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int buttonWidth = (panelWidth - 32) / 4;
        int buttonTop = top + panelHeight - 28;
        previous = addRenderableWidget(Button.builder(text("previous"), b -> move(-1))
                .bounds(left + 10, buttonTop, buttonWidth, 20).build());
        pause = addRenderableWidget(Button.builder(text(paused ? "play" : "pause"), b -> {
            paused = !paused;
            refresh();
        }).bounds(left + 14 + buttonWidth, buttonTop, buttonWidth, 20).build());
        next = addRenderableWidget(Button.builder(text("next"), b -> move(1))
                .bounds(left + 18 + buttonWidth * 2, buttonTop, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(text("skip"), b -> onClose())
                .bounds(left + 22 + buttonWidth * 3, buttonTop, buttonWidth, 20).build());
        refresh();
    }

    private void refresh() {
        previous.active = page > 0;
        next.setMessage(text(page == PAGE_COUNT - 1 ? "finish" : "next"));
        pause.setMessage(text(paused ? "play" : "pause"));
    }

    private void move(int offset) {
        if (page + offset >= PAGE_COUNT) {
            onClose();
            return;
        }
        page = Math.max(0, page + offset);
        elapsed = 0;
        bodyScroll = 0;
        refresh();
    }

    @Override
    public void tick() {
        ticks++;
        if (!paused && ++elapsed >= duration()) move(1);
    }

    private int duration() {
        return 240;
    }

    private Component body() {
        if (page == 6) {
            Component cast = SpellcastKeybindings.KEY_CAST == null || SpellcastKeybindings.KEY_CAST.isUnbound()
                    ? text("unbound") : SpellcastKeybindings.KEY_CAST.getTranslatedKeyMessage();
            Component change = SpellcastKeybindings.KEY_SWITCH == null || SpellcastKeybindings.KEY_SWITCH.isUnbound()
                    ? text("unbound") : SpellcastKeybindings.KEY_SWITCH.getTranslatedKeyMessage();
            return text("casting.body", cast, change);
        }
        return text(PAGES[page] + ".body");
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.blit(PANEL, left, top, panelWidth, panelHeight, 0, 0, 248, 166, 256, 256);
        context.drawString(font, font.plainSubstrByWidth(text(PAGES[page] + ".title").getString(), panelWidth - 72),
                left + 12, top + 12, 0xff292929, false);
        Component counter = text("page", page + 1, PAGE_COUNT);
        context.drawString(font, counter, left + panelWidth - 12 - font.width(counter),
                top + 12, 0xff555555, false);
        context.fill(left + 10, top + 30, left + panelWidth - 10, top + 98, 0xff12212b);
        drawBorder(context, left + 10, top + 30, panelWidth - 20, 68, 0xff526c75);
        renderDiagram(context);
        List<FormattedCharSequence> lines = font.split(body(), panelWidth - 28);
        int visible = Math.max(1, (panelHeight - 150) / 11);
        bodyScroll = Math.max(0, Math.min(bodyScroll, Math.max(0, lines.size() - visible)));
        for (int line = 0; line < visible && line + bodyScroll < lines.size(); line++) {
            context.drawString(font, lines.get(line + bodyScroll), left + 14, top + 106 + line * 11,
                    0xff2e2e2e, false);
        }
        if (lines.size() > visible) {
            context.drawString(font, Component.literal(bodyScroll > 0 ? "▲" : "▼"),
                    left + panelWidth - 14, top + 106, 0xff555555, false);
        }
        int progressWidth = (int) ((panelWidth - 24) * Math.min(1, (elapsed + delta) / duration()));
        context.fill(left + 12, top + panelHeight - 36, left + panelWidth - 12,
                top + panelHeight - 34, 0xff7c7c7c);
        context.fill(left + 12, top + panelHeight - 36, left + 12 + progressWidth,
                top + panelHeight - 34, 0xff3d9d9a);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderDiagram(GuiGraphics context) {
        switch (page) {
            case 0 -> flow(context, new ItemStack[]{new ItemStack(Items.CHEST),
                            ScrollData.create("fire_bolt", 1),
                            new ItemStack(RegAddonBlocks.SPELL_RESEARCH_TABLE),
                            new ItemStack(SscAddon.MOON_DUST_SPELLBOOK)},
                    new String[]{"explore", "scroll", "research", "book"});
            case 1 -> flow(context, new ItemStack[]{ScrollData.create("fire_bolt", 1),
                            new ItemStack(SscAddon.BLANK_FORMATION_PAPER),
                            new ItemStack(RegAddonBlocks.SPELL_RESEARCH_TABLE),
                            new ItemStack(SscAddon.ANALYZED_SPELL_DIAGRAM)},
                    new String[]{"scroll", "paper", "analysis.short", "diagram"});
            case 2 -> {
                RuneRole[] roles = {RuneRole.SOURCE, RuneRole.GAIN, RuneRole.STABLE};
                for (int i = 0; i < 3; i++) {
                    int cx = left + panelWidth * (i + 1) / 4;
                    glyph(context, glyphs[i], cx - 12, top + 38, 24, 1);
                    Component role = Component.translatable("research.ssc_addon.slotted.role." + roles[i].ordinal());
                    context.drawCenteredString(font, role, cx, top + 76, 0xffd9dfd7);
                }
            }
            case 3 -> {
                int cx = left + panelWidth / 2;
                int cy = top + 63;
                int[][] points = {{cx, cy - 21}, {cx - 34, cy + 19}, {cx + 34, cy + 19}};
                for (int i = 0; i < 3; i++) {
                    int[] a = points[i];
                    int[] b = points[(i + 1) % 3];
                    line(context, a[0], a[1], b[0], b[1], 0xffb6a06b);
                    drawBorder(context, a[0] - 10, a[1] - 10, 20, 20, 0xffb6a06b);
                }
                glyph(context, glyphs[0], points[0][0] - 8, points[0][1] - 8, 16, .35f);
                double phase = (ticks % 100) / 100.0;
                int handX = (int) ((left + 35) * (1 - phase) + (points[0][0] - 8) * phase);
                int handY = (int) ((top + 58) * (1 - phase) + (points[0][1] - 8) * phase);
                glyph(context, glyphs[0], handX, handY, 16, 1);
                context.drawCenteredString(font, text("manual"), left + panelWidth - 54, top + 61, 0xffd9dfd7);
            }
            case 4 -> flow(context, new ItemStack[]{ScrollData.create("fire_bolt", 1),
                            new ItemStack(RegAddonBlocks.SPELL_RESEARCH_TABLE),
                            new ItemStack(SscAddon.FORMATION_INK_FIRE),
                            ScrollData.create("fire_bolt", 2)},
                    new String[]{"lower_scroll", "runes.short", "materials", "higher_scroll"});
            case 5 -> flow(context, new ItemStack[]{new ItemStack(SscAddon.SPELL_FORMATION),
                            ScrollData.create("fire_bolt", 1),
                            new ItemStack(RegAddonBlocks.INFUSION_ALTAR),
                            ScrollData.create("fire_bolt", 1)},
                    new String[]{"scheme", "scroll", "preparation", "imprint"});
            case 6 -> flow(context, new ItemStack[]{ScrollData.create("fire_bolt", 1),
                            new ItemStack(SscAddon.MOON_DUST_SPELLBOOK),
                            new ItemStack(SscAddon.MOON_DUST_SPELLBOOK),
                            new ItemStack(Items.FIRE_CHARGE)},
                    new String[]{"scroll", "insert", "equip", "cast"});
            case 7 -> {
                int bx = left + 26;
                int bw = panelWidth - 52;
                int by = top + 61;
                double value = (ticks % 180) / 180.0 * 1.4;
                context.fill(bx, by, bx + bw, by + 12, 0xff3b3c3c);
                int safe = bw / 2;
                context.fill(bx, by, bx + Math.min(safe, (int) (value * safe)), by + 12, 0xff45bec0);
                if (value > 1) context.fill(bx + safe, by, bx + (int) (value * safe), by + 12, 0xffcb7956);
                drawBorder(context, bx - 1, by - 1, bw + 2, 14, 0xffa8bac1);
                context.fill(bx + safe, by - 3, bx + safe + 1, by + 15, 0xfff2f2e9);
                context.drawCenteredString(font, text("collapse.demo"), left + panelWidth / 2, top + 39, 0xffd9dfd7);
                context.drawString(font, text("safe"), bx, by + 19, 0xff8bd4cd, false);
                Component danger = text("danger");
                context.drawString(font, danger, bx + bw - font.width(danger), by + 19, 0xffffba88, false);
            }
            case 8 -> flow(context, new ItemStack[]{new ItemStack(Items.ARMOR_STAND),
                            new ItemStack(Items.EXPERIENCE_BOTTLE),
                            new ItemStack(MoonDustReliefItem.BOTTLE),
                            new ItemStack(SscAddon.MOON_DUST_SPELLBOOK)},
                    new String[]{"player", "experience", "recovery", "replay.short"});
        }
    }

    private void flow(GuiGraphics context, ItemStack[] stacks, String[] captions) {
        int spacing = (panelWidth - 28) / stacks.length;
        for (int i = 0; i < stacks.length; i++) {
            int cx = left + 14 + spacing * i + spacing / 2;
            context.pose().pushPose();
            context.pose().translate(cx - 12, top + 38, 0);
            context.pose().scale(1.5f, 1.5f, 1);
            context.renderItem(stacks[i], 0, 0);
            context.pose().popPose();
            String label = font.plainSubstrByWidth(text(captions[i]).getString(), spacing - 7);
            context.drawCenteredString(font, Component.literal(label), cx, top + 76, 0xffd9dfd7);
            if (i + 1 < stacks.length) {
                context.drawString(font, Component.literal("→"), cx + spacing / 2 - 4, top + 47,
                        0xffc7bd88, false);
            }
        }
    }

    private static void glyph(GuiGraphics context, int glyph, int x, int y, int size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        context.blit(RUNES, x, y, size, size, glyph * 64, 0, 64, 64, 1152, 64);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void line(GuiGraphics context, int ax, int ay, int bx, int by, int color) {
        int steps = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
        for (int i = 0; i <= steps; i++) {
            int x = ax + (bx - ax) * i / Math.max(1, steps);
            int y = ay + (by - ay) * i / Math.max(1, steps);
            context.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawBorder(GuiGraphics context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        bodyScroll -= Math.round((float) amount);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            paused = !paused;
            refresh();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_ENTER) {
            move(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            move(-1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }
}
