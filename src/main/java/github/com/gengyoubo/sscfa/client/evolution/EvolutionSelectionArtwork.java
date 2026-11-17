package github.com.gengyoubo.sscfa.client.evolution;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class EvolutionSelectionArtwork {
    static final int INK = 0xFF493621;
    private static final int TILE_SIZE = 64;
    private static final int PAPER_WIDTH = 768;
    private static final int PAPER_HEIGHT = 512;
    private static final ResourceLocation PAPER = texture("background_base");
    private static final ResourceLocation ARROW = texture("icons/back_arrow");

    // Visible regions of the 64×64 frames, excluding their transparent padding.
    static final Frame PORTRAIT_FRAME = new Frame(texture("skin/node_frame"), 4, 5, 56, 54);
    static final Frame NAME_FRAME = new Frame(texture("skin/label_frame"), 4, 21, 56, 22);
    static final Frame BUTTON_FRAME = new Frame(texture("skin/confirm_frame"), 4, 20, 56, 24);
    private static final ResourceLocation DIVIDER = texture("skin/divider_ornament");
    private static final ResourceLocation LINE = texture("skin/line_horizontal");

    private EvolutionSelectionArtwork() {}

    static ResourceLocation portrait(String routeId, @Nullable String baseNodeId) {
        var routePortrait = findTexture("icons/" + routeId);
        if (routePortrait != null) return routePortrait;
        var basePortrait = baseNodeId == null ? null : findTexture("icons/" + baseNodeId);
        return basePortrait == null ? texture("icons/mana_system") : basePortrait;
    }

    @Nullable
    static ResourceLocation decorations(String routeId) {
        return findTexture("decorations/" + routeId);
    }

    @Nullable
    private static ResourceLocation findTexture(String path) {
        ResourceLocation id = ResourceLocation.tryParse("my_addon:textures/gui/evolution/" + path + ".png");
        return id != null && Minecraft.getInstance().getResourceManager().getResource(id).isPresent() ? id : null;
    }

    static void paper(GuiGraphics graphics, int x, int y, int width, int height) {
        blit(graphics, PAPER, x, y, width, height, 0, 0, PAPER_WIDTH, PAPER_HEIGHT, PAPER_WIDTH, PAPER_HEIGHT);
    }

    static void margins(GuiGraphics graphics, ResourceLocation texture, boolean axolotl,
                        int x, int y, int width, int height) {
        int margin = width * 100 / PAPER_WIDTH;
        int right = x + width - margin;
        blit(graphics, texture, x, y, margin, height, 0, 0, 100, PAPER_HEIGHT, PAPER_WIDTH, PAPER_HEIGHT);
        if (axolotl) {
            int coralTop = height * 236 / PAPER_HEIGHT;
            int coralBottom = height * 357 / PAPER_HEIGHT;
            blit(graphics, texture, right, y, margin, coralTop, 430, 0, 100, 236, PAPER_WIDTH, PAPER_HEIGHT);
            blit(graphics, texture, right + width * 8 / PAPER_WIDTH, y + coralTop, margin, coralBottom - coralTop,
                    430, 236, 100, 121, PAPER_WIDTH, PAPER_HEIGHT);
            blit(graphics, texture, right, y + coralBottom, margin, height - coralBottom,
                    430, 357, 100, PAPER_HEIGHT - 357, PAPER_WIDTH, PAPER_HEIGHT);
        } else {
            blit(graphics, texture, right, y, margin, height, 430, 0, 100, PAPER_HEIGHT, PAPER_WIDTH, PAPER_HEIGHT);
        }
    }

    static void icon(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        blit(graphics, texture, x, y, size, size, 0, 0, TILE_SIZE, TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    static void arrow(GuiGraphics graphics, int x, int y, boolean next) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + (next ? 14 : 0), y + (next ? 14 : 0), 0);
            if (next) graphics.pose().scale(-1, -1, 1);
            icon(graphics, ARROW, 0, 0, 14);
        } finally {
            graphics.pose().popPose();
        }
    }

    static void divider(GuiGraphics graphics, int centerX, int y) {
        blit(graphics, LINE, centerX - 92, y - 1, 80, 3, 5, 30, 54, 4, TILE_SIZE, TILE_SIZE);
        blit(graphics, LINE, centerX + 12, y - 1, 80, 3, 5, 30, 54, 4, TILE_SIZE, TILE_SIZE);
        blit(graphics, DIVIDER, centerX - 10, y - 3, 20, 7, 4, 25, 56, 14, TILE_SIZE, TILE_SIZE);
    }

    record Frame(ResourceLocation texture, int u, int v, int sourceWidth, int sourceHeight) {
        void draw(GuiGraphics graphics, int x, int y, int width, int height) {
            int cornerW = Math.min(12, sourceWidth / 3);
            int cornerH = Math.min(12, sourceHeight / 3);
            int edgeW = Math.min(6, width / 3);
            int edgeH = Math.min(6, height / 3);
            int[] sourceX = {u, u + cornerW, u + sourceWidth - cornerW, u + sourceWidth};
            int[] sourceY = {v, v + cornerH, v + sourceHeight - cornerH, v + sourceHeight};
            int[] destX = {x, x + edgeW, x + width - edgeW, x + width};
            int[] destY = {y, y + edgeH, y + height - edgeH, y + height};
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    blit(graphics, texture, destX[column], destY[row], destX[column + 1] - destX[column],
                            destY[row + 1] - destY[row], sourceX[column], sourceY[row],
                            sourceX[column + 1] - sourceX[column], sourceY[row + 1] - sourceY[row], TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation("my_addon", "textures/gui/evolution/" + path + ".png");
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                             int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
        RenderSystem.disableBlend();
    }
}
