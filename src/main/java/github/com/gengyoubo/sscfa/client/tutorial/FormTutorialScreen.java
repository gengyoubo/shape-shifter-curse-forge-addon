package github.com.gengyoubo.sscfa.client.tutorial;

import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** One illustrated mechanic per page, with contents navigation and the player's real key names. */
public final class FormTutorialScreen extends Screen {
    private static final Pattern KEY_TOKEN =
            Pattern.compile("\\{(primary|secondary|sneak|attack|use|jump|sprint)\\}");

    private final Screen book;
    private final ResourceLocation form;
    private final Runnable onShown;
    private ClientPacketListener connection;
    private List<FormTutorialLessons.Lesson> lessons = List.of();
    private final List<Button> entries = new ArrayList<>();
    private int page;
    private int bodyScroll;
    private int indexScroll;
    private int ticks;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentHeight;
    private int bodyLeft;
    private int bodyTop;
    private int bodyWidth;
    private int bodyHeight;
    private int diagramLeft;
    private int diagramTop;
    private int diagramWidth;
    private int diagramHeight;
    private boolean shown;
    private boolean indexOpen;
    private String language = "";
    private Component body = Component.empty();
    private List<FormattedCharSequence> lines = List.of();
    private Button previous;
    private Button next;
    private Button index;

    public FormTutorialScreen(Screen book, ResourceLocation form, Runnable onShown) {
        super(text("title", Component.translatable(
                "codex.form." + form.getNamespace() + "." + form.getPath() + ".name")));
        this.book = book;
        this.form = form;
        this.onShown = onShown;
    }

    public static MutableComponent text(String key, Object... args) {
        return Component.translatable("tutorial.ssc_addon.form." + key, args);
    }

    @Override
    protected void init() {
        if (connection == null) connection = minecraft.getConnection();
        panelWidth = Math.min(660, width - 16);
        panelHeight = Math.min(370, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        contentTop = top + 47;
        contentHeight = Math.max(24, panelHeight - 91);
        diagramLeft = left + 12;
        diagramTop = contentTop;
        diagramWidth = (panelWidth - 36) / 2;
        diagramHeight = contentHeight;
        bodyLeft = diagramLeft + diagramWidth + 12;
        bodyTop = contentTop;
        bodyWidth = panelWidth - diagramWidth - 36;
        bodyHeight = contentHeight;
        int gap = 5;
        int buttonWidth = (panelWidth - 24 - gap * 3) / 4;
        int buttonTop = top + panelHeight - 28;
        previous = addRenderableWidget(Button.builder(text("previous"), button -> move(-1))
                .bounds(left + 12, buttonTop, buttonWidth, 20).build());
        index = addRenderableWidget(Button.builder(text("contents"), button -> toggleIndex())
                .bounds(left + 12 + buttonWidth + gap, buttonTop, buttonWidth, 20).build());
        next = addRenderableWidget(Button.builder(text("next"), button -> move(1))
                .bounds(left + 12 + (buttonWidth + gap) * 2, buttonTop, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(text("skip"), button -> onClose())
                .bounds(left + 12 + (buttonWidth + gap) * 3, buttonTop, buttonWidth, 20).build());
        entries.clear();
        int rows = Math.max(1, contentHeight / 25);
        for (int row = 0; row < rows; row++) {
            final int offset = row;
            entries.add(addRenderableWidget(Button.builder(Component.empty(),
                            button -> select(indexScroll + offset))
                    .bounds(left + 14, contentTop + row * 25, panelWidth - 36, 22).build()));
        }
        rebuild();
    }

    private void rebuild() {
        lessons = FormTutorialLessons.forForm(form);
        language = minecraft.options.languageCode;
        page = Math.max(0, Math.min(page, Math.max(0, lessons.size() - 1)));
        previous.active = page > 0;
        next.active = !lessons.isEmpty();
        next.setMessage(text(page == lessons.size() - 1 ? "finish" : "next"));
        index.setMessage(text(indexOpen ? "read_page" : "contents"));
        body = Component.empty();
        if (!lessons.isEmpty()) {
            var lesson = lessons.get(page);
            MutableComponent result = Component.empty();
            appendSection(result, "how", resolve(lesson, "trigger"), bodyHeight < 150);
            appendSection(result, "result", resolve(lesson, "effect"), bodyHeight < 150);
            appendSection(result, "remember", resolve(lesson, "note"), bodyHeight < 150);
            body = result;
        }
        lines = font.split(body, Math.max(20, bodyWidth - 8));
        bodyScroll = Math.max(0, Math.min(bodyScroll, maxScroll()));
        updateIndex();
    }

    private static void appendSection(MutableComponent result, String title, Component description, boolean compact) {
        if (!result.getString().isEmpty()) result.append(compact ? "\n" : "\n\n");
        result.append(text(title).withStyle(style -> style.withColor(0x404040).withBold(true)))
                .append("\n").append(description);
    }

    private Component resolve(FormTutorialLessons.Lesson lesson, String section) {
        String value = Component.translatable(lesson.key(section)).getString();
        var matcher = KEY_TOKEN.matcher(value);
        MutableComponent result = Component.empty();
        int cursor = 0;
        while (matcher.find()) {
            result.append(value.substring(cursor, matcher.start()));
            result.append(FormTutorialKeys.key(minecraft, form, matcher.group(1)));
            cursor = matcher.end();
        }
        return result.append(value.substring(cursor));
    }

    public Component content() {
        return body;
    }

    public ResourceLocation formId() {
        return form;
    }

    public int lessonCount() {
        return lessons.size();
    }

    public String lessonId() {
        return lessons.isEmpty() ? "" : lessons.get(page).id();
    }

    private void move(int offset) {
        if (page + offset >= lessons.size()) {
            onClose();
            return;
        }
        select(Math.max(0, page + offset));
    }

    private void select(int selected) {
        if (selected < 0 || selected >= lessons.size()) return;
        page = selected;
        bodyScroll = 0;
        indexOpen = false;
        rebuild();
    }

    private void toggleIndex() {
        indexOpen = !indexOpen;
        updateIndex();
        index.setMessage(text(indexOpen ? "read_page" : "contents"));
    }

    private void updateIndex() {
        indexScroll = Math.max(0, Math.min(indexScroll, Math.max(0, lessons.size() - entries.size())));
        for (int row = 0; row < entries.size(); row++) {
            Button button = entries.get(row);
            int number = indexScroll + row;
            button.visible = indexOpen && number < lessons.size();
            if (number < lessons.size()) {
                var lesson = lessons.get(number);
                button.setMessage(Component.literal((number + 1) + ". ")
                        .append(Component.translatable(lesson.key("title"))).append("  ")
                        .append(text("kind." + lesson.kind().name().toLowerCase(Locale.ROOT))));
                button.active = number != page;
            }
        }
    }

    private int visibleLines() {
        return Math.max(1, bodyHeight / 10);
    }

    private int maxScroll() {
        return Math.max(0, lines.size() - visibleLines());
    }

    private void scroll(int amount) {
        if (indexOpen) {
            indexScroll += amount;
            updateIndex();
        } else {
            bodyScroll = Math.max(0, Math.min(maxScroll(), bodyScroll + amount));
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (minecraft.player == null || !minecraft.player.isAlive()
                || !form.equals(FormTutorialClient.currentForm(minecraft))) {
            onClose();
            return;
        }
        if (lessons != FormTutorialLessons.forForm(form) || !language.equals(minecraft.options.languageCode)) {
            rebuild();
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        FormTutorialTextures.window(context, left, top, panelWidth, panelHeight);
        context.drawString(font, font.plainSubstrByWidth(title.getString(), panelWidth - 90),
                left + 13, top + 9, 0xff404040, false);
        Component counter = text("page", page + 1, lessons.size());
        context.drawString(font, counter, left + panelWidth - 13 - font.width(counter),
                top + 9, 0xff404040, false);
        if (!lessons.isEmpty()) {
            var lesson = lessons.get(page);
            Component kind = text("kind." + lesson.kind().name().toLowerCase(Locale.ROOT));
            int kindWidth = font.width(kind) + 10;
            FormTutorialTextures.inset(context, left + 12, top + 25, kindWidth, 16);
            context.drawString(font, kind, left + 17, top + 28, 0xffe0e0e0, false);
            context.drawString(font, font.plainSubstrByWidth(
                            Component.translatable(lesson.key("title")).getString(), panelWidth - kindWidth - 35),
                    left + 21 + kindWidth, top + 28, 0xff404040, false);
            if (indexOpen) {
                renderIndex(context);
            } else {
                FormTutorialDiagram.render(context, font, minecraft, lesson,
                        diagramLeft, diagramTop, diagramWidth, diagramHeight, ticks + delta);
                context.enableScissor(bodyLeft - 2, bodyTop, bodyLeft + bodyWidth, bodyTop + bodyHeight);
                for (int i = 0; i < visibleLines() && i + bodyScroll < lines.size(); i++) {
                    context.drawString(font, lines.get(i + bodyScroll), bodyLeft, bodyTop + i * 10,
                            0xff303030, false);
                }
                context.disableScissor();
                scrollbar(context, bodyLeft + bodyWidth - 2, bodyTop, bodyHeight,
                        lines.size(), visibleLines(), bodyScroll);
                renderStatus(context, lesson);
            }
        }
        super.render(context, mouseX, mouseY, delta);
        if (!shown && !lessons.isEmpty()) {
            shown = true;
            onShown.run();
        }
    }

    private void renderIndex(GuiGraphics context) {
        FormTutorialTextures.inset(context, left + 12, contentTop - 2, panelWidth - 24, contentHeight + 2);
        scrollbar(context, left + panelWidth - 18, contentTop, contentHeight,
                lessons.size(), entries.size(), indexScroll);
    }

    private void renderStatus(GuiGraphics context, FormTutorialLessons.Lesson lesson) {
        String hintKey = maxScroll() > 0 ? "reading_scroll" : "reading_hint";
        Component status = text(hintKey);
        if (!lesson.node().isEmpty() && minecraft.player != null) {
            var evolution = RegEvolutionComponent.EVOLUTION.get(minecraft.player);
            if (evolution != null && !evolution.isUnlocked(lesson.node())) status = text("locked");
        }
        context.drawString(font, font.plainSubstrByWidth(status.getString(), panelWidth - 26),
                left + 13, top + panelHeight - 41, 0xff404040, false);
    }

    private static void scrollbar(GuiGraphics context, int x, int y, int height,
                                  int total, int visible, int offset) {
        if (total <= visible) return;
        int thumb = Math.max(6, height * visible / total);
        int start = y + (height - thumb) * offset / (total - visible);
        FormTutorialTextures.scroll(context, x - 3, y, height, thumb, start - y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll(-Math.round((float) amount * (indexOpen ? 1 : 3)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (indexOpen) toggleIndex();
                else onClose();
            }
            case GLFW.GLFW_KEY_DOWN -> scroll(1);
            case GLFW.GLFW_KEY_UP -> scroll(-1);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll(indexOpen ? entries.size() : visibleLines());
            case GLFW.GLFW_KEY_PAGE_UP -> scroll(indexOpen ? -entries.size() : -visibleLines());
            case GLFW.GLFW_KEY_HOME -> {
                if (indexOpen) {
                    indexScroll = 0;
                    updateIndex();
                } else {
                    bodyScroll = 0;
                }
            }
            case GLFW.GLFW_KEY_END -> {
                if (indexOpen) {
                    indexScroll = lessons.size();
                    updateIndex();
                } else {
                    bodyScroll = maxScroll();
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> move(1);
            case GLFW.GLFW_KEY_LEFT -> move(-1);
            default -> {
                return super.keyPressed(key, scan, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(minecraft.level != null && minecraft.player != null && minecraft.player.isAlive()
                && minecraft.getConnection() == connection ? book : null);
    }
}
