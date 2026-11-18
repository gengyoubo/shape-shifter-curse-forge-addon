package github.com.gengyoubo.sscfa.client.evolution;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.client.codex.StartBookScreenV2;
import net.onixary.shapeShifterCurseForge.client.codex.BookOfShapeShifterScreenV2_P1;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;

@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID, value = Dist.CLIENT)
public final class SscaStartBookHook {
    private static final String OPEN_BOOK_KEY = "screen.shape-shifter-curse.book_of_shape_shifter.start_button_text";
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private SscaStartBookHook() {}

    @SubscribeEvent
    public static void addEvolutionButton(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof BookOfShapeShifterScreenV2_P1 book) {
            if (EvolutionScreen.resolveRoute() == null || !EvolutionScreen.resolveRoute().enabled) return;
            int scale = SscClientConfig.BIG_CODEX.get() ? 2 : 1;
            int left = book.width / 2 - 350 * scale / 2;
            int top = book.height / 2 - 220 * scale / 2;
            event.addListener(Button.builder(Component.translatable("text.ssc_addon.evolution.book.button"),
                    button -> Minecraft.getInstance().setScreen(new EvolutionScreen(book)))
                    .bounds(left + 35 * scale, top + 2 * scale, 70 * scale, 12 * scale).build());
            return;
        }
        if (!(event.getScreen() instanceof StartBookScreenV2 book)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        for (var listener : event.getListenersList()) {
            if (listener instanceof Button openBook
                    && openBook.getMessage().getContents() instanceof TranslatableContents label
                    && OPEN_BOOK_KEY.equals(label.getKey())) {
                int width = Math.min(BUTTON_WIDTH, openBook.getWidth());
                int x = openBook.getX() + (openBook.getWidth() - width) / 2;
                int lastButtonY = book.height - BUTTON_HEIGHT - GAP;
                int y = Math.min(openBook.getY() + openBook.getHeight() + GAP, lastButtonY);
                openBook.setY(y - GAP - openBook.getHeight());
                event.addListener(Button.builder(Component.translatable("evolution.my_addon.start.button"),
                        button -> openConfirmation(client, book)).bounds(x, y, width, BUTTON_HEIGHT).build());
                return;
            }
        }
    }

    private static void openConfirmation(Minecraft client, StartBookScreenV2 book) {
        client.setScreen(new ConfirmScreen(
                confirmed -> client.setScreen(confirmed ? new SscaFormSelectScreen(book) : book),
                Component.translatable("evolution.my_addon.start.confirm.title"),
                Component.translatable("evolution.my_addon.start.confirm.message"),
                Component.translatable("evolution.my_addon.start.confirm.yes"),
                Component.translatable("evolution.my_addon.start.confirm.no")));
    }
}
