package github.com.gengyoubo.sscfa.client.tooltip;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Wrap item descriptions to the available GUI width while preserving translated text and styles. */
public final class AddonTooltipFormatter {
    private AddonTooltipFormatter() {}

    public static void wrap(Font renderer, List<Component> lines, int screenWidth) {
        int maxWidth = Math.max(1, Math.min(260, screenWidth - 24));
        List<Component> wrapped = new ArrayList<>();
        for (Component source : lines) {
            if (renderer.width(source) <= maxWidth && !source.getString().contains("\n")) {
                wrapped.add(source);
                continue;
            }
            for (var ordered : renderer.split(source, maxWidth)) {
                var line = Component.empty();
                ordered.accept((index, style, codePoint) -> {
                    line.append(Component.literal(Character.toString(codePoint)).setStyle(style));
                    return true;
                });
                wrapped.add(line);
            }
        }
        lines.clear();
        lines.addAll(wrapped);
    }
}
