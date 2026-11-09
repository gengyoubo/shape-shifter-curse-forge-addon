package github.com.gengyoubo.sscfa.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
public final class SpellIconTooltipComponent implements ClientTooltipComponent {
	private static final int ICON_SIZE = 18;
	private static final int TITLE_ROW_HEIGHT = 12;
	private static final int COMPONENT_HEIGHT = 8;

	private final ResourceLocation texture;

	public SpellIconTooltipComponent(ResourceLocation texture) {
		this.texture = texture;
	}

	public static void formatTooltip(Font textRenderer, List<Component> lines, int screenWidth) {
		if (lines.isEmpty()) return;
		String padding = " ";
		while (textRenderer.width(padding) < ICON_SIZE + 4) padding += " ";
		List<Component> wrapped = new ArrayList<>();
		int maxWidth = Math.max(1, Math.min(240, screenWidth - 24));
		for (int index = 0; index < lines.size(); index++) {
			Component source = index == 0 ? Component.literal(padding).append(lines.get(index)) : lines.get(index);
			if (textRenderer.width(source) <= maxWidth) {
				wrapped.add(source);
				continue;
			}
			for (var ordered : textRenderer.split(source, maxWidth)) {
				var line = Component.empty();
				ordered.accept((characterIndex, style, codePoint) -> {
					line.append(Component.literal(Character.toString(codePoint)).setStyle(style));
					return true;
				});
				wrapped.add(line);
			}
		}
		lines.clear();
		lines.addAll(wrapped);
	}

	@Override
	public int getHeight() {
		return COMPONENT_HEIGHT;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return ICON_SIZE;
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics context) {
		context.blit(texture, x, y - TITLE_ROW_HEIGHT,
				ICON_SIZE, ICON_SIZE, 0, 0, 32, 32, 32, 32);
	}
}