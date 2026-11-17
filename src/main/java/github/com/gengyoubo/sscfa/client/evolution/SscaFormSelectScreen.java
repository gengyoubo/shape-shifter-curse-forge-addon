package github.com.gengyoubo.sscfa.client.evolution;

import github.com.gengyoubo.sscfa.evolution.EvolutionRegistry;
import github.com.gengyoubo.sscfa.evolution.EvolutionRoute;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class SscaFormSelectScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 210;
    private static final int PORTRAIT_SIZE = 48;

    private record StartForm(ResourceLocation id, ResourceLocation portrait, @Nullable ResourceLocation decorations,
                             boolean axolotl, Component name, Component description) {
        private static StartForm fromRoute(EvolutionRoute route) {
            ResourceLocation legacyId = FormIdMigration.legacy(route.startForm);
            String namespace = legacyId.getNamespace();
            String path = legacyId.getPath();
            return new StartForm(route.startForm, EvolutionSelectionArtwork.portrait(route.routeId, route.getBaseNodeId()),
                    EvolutionSelectionArtwork.decorations(route.routeId), "axolotl".equals(route.routeId),
                    Component.translatable("origin." + namespace + ".form_" + path + ".name"),
                    Component.translatable("evolution." + namespace + ".start.form." + path + ".desc"));
        }
    }

    private final Screen parent;
    private List<StartForm> forms = List.of();
    private int page;

    public SscaFormSelectScreen(Screen parent) {
        super(Component.translatable("evolution.my_addon.start.select.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        forms = EvolutionRegistry.INSTANCE.all().values().stream()
                .filter(route -> route.enabled && route.startForm != null)
                .sorted(Comparator.comparingInt((EvolutionRoute route) -> switch (route.routeId) {
                    case "familiar_fox" -> 0;
                    case "axolotl" -> 1;
                    default -> 2;
                }).thenComparing(route -> route.routeId))
                .map(StartForm::fromRoute)
                .toList();
        page = Math.max(0, Math.min(page, forms.size() - 1));

        int centerX = width / 2;
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        boolean multiplePages = forms.size() > 1;

        addRenderableWidget(new PaperButton(left + 40, top + 95, 22, 22,
                Component.literal("<"), button -> changePage(-1), -1)).active = multiplePages;
        addRenderableWidget(new PaperButton(left + PANEL_WIDTH - 62, top + 95, 22, 22,
                Component.literal(">"), button -> changePage(1), 1)).active = multiplePages;
        addRenderableWidget(new PaperButton(centerX - 80, top + PANEL_HEIGHT - 54, 160, 20,
                Component.translatable("evolution.my_addon.start.select.choose"), button -> chooseCurrentForm(), 0))
                .active = !forms.isEmpty();
        addRenderableWidget(new PaperButton(centerX - 60, top + PANEL_HEIGHT - 30, 120, 20,
                Component.translatable("gui.back"), button -> onClose(), 0));
    }

    private void changePage(int direction) {
        page = Math.floorMod(page + direction, forms.size());
    }

    private void chooseCurrentForm() {
        if (forms.isEmpty()) return;
        var buffer = AddonNetwork.buffer();
        buffer.writeUtf(forms.get(page).id().toString());
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_SSCA_START_ROUTE, buffer);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0, 0, 400);
            graphics.fill(0, 0, width, height, 0x32000000);
            graphics.flush();
            EvolutionSelectionArtwork.paper(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
            StartForm form = forms.isEmpty() ? null : forms.get(page);
            if (form != null && form.decorations() != null) {
                pose.translate(0, 0, 1);
                EvolutionSelectionArtwork.margins(graphics, form.decorations(), form.axolotl(),
                        left, top, PANEL_WIDTH, PANEL_HEIGHT);
            }

            pose.translate(0, 0, 10);
            drawCentered(graphics, title, centerX, top + 12, 216);
            EvolutionSelectionArtwork.divider(graphics, centerX, top + 36);
            if (form != null) {
                drawCentered(graphics, Component.translatable("evolution.my_addon.start.select.page", page + 1, forms.size()),
                        centerX, top + 25, 64);
                int portraitX = centerX - PORTRAIT_SIZE / 2;
                int portraitY = top + 44;
                EvolutionSelectionArtwork.PORTRAIT_FRAME.draw(graphics, portraitX - 4, portraitY - 3, 56, 54);
                EvolutionSelectionArtwork.icon(graphics, form.portrait(), portraitX, portraitY, PORTRAIT_SIZE);

                int labelWidth = Math.min(176, font.width(form.name()) + 20);
                EvolutionSelectionArtwork.NAME_FRAME.draw(graphics, centerX - labelWidth / 2, top + 97, labelWidth, 17);
                drawCentered(graphics, form.name(), centerX, top + 102, labelWidth - 16);
                drawDescription(graphics, form.description(), centerX, top + 119, top + PANEL_HEIGHT - 60);
            }
            super.render(graphics, mouseX, mouseY, delta);
        } finally {
            graphics.flush();
            pose.popPose();
        }
    }

    private void drawDescription(GuiGraphics graphics, Component text, int centerX, int top, int bottom) {
        var lines = font.split(text, 180);
        float scale = Math.min(1, (bottom - top) / (float) Math.max(1, lines.size() * 10));
        for (int i = 0; i < lines.size(); i++) {
            drawLine(graphics, lines.get(i), centerX, top + i * 10 * scale, scale);
        }
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y, int maxWidth) {
        var line = text.getVisualOrderText();
        float scale = Math.min(1, maxWidth / (float) Math.max(1, font.width(line)));
        drawLine(graphics, line, centerX, y, scale);
    }

    private void drawLine(GuiGraphics graphics, FormattedCharSequence line, int centerX, float y, float scale) {
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(centerX - font.width(line) * scale / 2, y, 0);
            pose.scale(scale, scale, 1);
            graphics.drawString(font, line, 0, 0, EvolutionSelectionArtwork.INK, false);
        } finally {
            pose.popPose();
        }
    }

    private final class PaperButton extends Button {
        private final int direction;

        PaperButton(int x, int y, int width, int height, Component label, OnPress onPress, int direction) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            EvolutionSelectionArtwork.BUTTON_FRAME.draw(graphics, x, y, width, height);
            if (active && (isFocused() || direction == 0 && isHovered())) {
                EvolutionSelectionArtwork.PORTRAIT_FRAME.draw(graphics, x + 2, y + 2, width - 4, height - 4);
            }
            if (direction == 0) {
                drawCentered(graphics, getMessage(), x + width / 2, y + (height - 8) / 2, width - 14);
            } else {
                EvolutionSelectionArtwork.arrow(graphics, x + (width - 14) / 2, y + (height - 14) / 2, direction > 0);
            }
            if (!active) {
                for (int dot = x + 5; dot < x + width - 4; dot += 4) {
                    graphics.fill(dot, y + height - 4, dot + 1, y + height - 3, EvolutionSelectionArtwork.INK);
                }
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
