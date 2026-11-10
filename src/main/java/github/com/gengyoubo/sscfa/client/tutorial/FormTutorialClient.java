package github.com.gengyoubo.sscfa.client.tutorial;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.tutorial.FormTutorialCatalog;
import github.com.gengyoubo.sscfa.tutorial.FormTutorialManager;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.onixary.shapeShifterCurseForge.client.codex.BookOfShapeShifterScreenV2_P1;
import net.onixary.shapeShifterCurseForge.client.codex.BookOfShapeShifterScreenV2_P2;

import java.util.UUID;

/** Open only from the player's book, after init has finished and the server has checked its ledger. */
public final class FormTutorialClient {
    private record Request(Screen book, ClientPacketListener connection, ResourceLocation form,
                           boolean manual, UUID id) {}

    private record Offer(Request request, UUID nonce) {}

    private static Request queued;
    private static Request requested;
    private static Offer pending;
    private static Screen lastBook;
    private static ResourceLocation lastForm;
    private static boolean registered;

    private FormTutorialClient() {
    }

    public static ResourceLocation currentForm(Minecraft client) {
        if (client.player == null) return null;
        ResourceLocation form = FormUtils.currentFormId(client.player);
        return form == null ? null : FormIdMigration.canonical(form);
    }

    private static boolean ownBook(Minecraft client, Screen screen) {
        return client.player != null
                && (screen instanceof BookOfShapeShifterScreenV2_P1 first && first.currentPlayer == client.player
                || screen instanceof BookOfShapeShifterScreenV2_P2 second && second.currentPlayer == client.player);
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            Minecraft client = Minecraft.getInstance();
            Screen screen = event.getScreen();
            if (!ownBook(client, screen)) return;
            ResourceLocation form = currentForm(client);
            if (!FormTutorialCatalog.supports(form)) return;
            if (screen instanceof BookOfShapeShifterScreenV2_P1) {
                int scale = event.getScreen().width >= 400 ? 2 : 1;
                int x = event.getScreen().width / 2 - BookOfShapeShifterScreenV2_P1.BOOK_SIZE_X * scale / 2;
                int y = event.getScreen().height / 2 - BookOfShapeShifterScreenV2_P1.BOOK_SIZE_Y * scale / 2;
                Button replay = Button.builder(FormTutorialScreen.text("replay"),
                                button -> schedule(client, screen, true))
                        .bounds(x + 35 * scale, y + 81 * scale, 70 * scale, 12 * scale).build();
                event.addListener(replay);
            }
            if (lastBook != screen || !form.equals(lastForm)) {
                lastBook = screen;
                lastForm = form;
                schedule(client, screen, false);
            }
        });
        AddonNetwork.registerClientReceiver(FormTutorialManager.OFFER, buf -> {
            final UUID requestId;
            final UUID nonce;
            final ResourceLocation form;
            final boolean manual;
            try {
                requestId = buf.readUUID();
                nonce = buf.readUUID();
                form = buf.readResourceLocation();
                manual = buf.readBoolean();
                if (buf.isReadable() || !FormTutorialCatalog.supports(form)) return;
            } catch (RuntimeException malformed) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            ClientPacketListener connection = client.getConnection();
            client.execute(() -> {
                Request request = requested;
                if (request != null && request.connection == connection && client.getConnection() == connection
                        && request.id.equals(requestId) && request.form.equals(form)
                        && request.manual == manual && matches(client, request)) {
                    pending = new Offer(request, nonce);
                }
            });
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            queued = null;
            requested = null;
            pending = null;
            lastBook = null;
            lastForm = null;
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
    }

    private static void tick(Minecraft client) {
        if (queued != null) {
            Request request = queued;
            queued = null;
            if (matches(client, request)) {
                requested = request;
                pending = null;
                FriendlyByteBuf packet = AddonNetwork.buffer();
                packet.writeUUID(request.id);
                packet.writeResourceLocation(request.form);
                packet.writeBoolean(request.manual);
                AddonNetwork.sendToServer(FormTutorialManager.REQUEST, packet);
            }
        }
        if (pending == null) return;
        Offer offer = pending;
        if (matches(client, offer.request) && client.getOverlay() != null) return;
        pending = null;
        if (!matches(client, offer.request)) return;
        requested = null;
        Runnable acknowledge = () -> {
            if (client.getConnection() != offer.request.connection
                    || !offer.request.form.equals(currentForm(client))) {
                return;
            }
            FriendlyByteBuf packet = AddonNetwork.buffer();
            packet.writeUUID(offer.nonce);
            AddonNetwork.sendToServer(FormTutorialManager.ACTION, packet);
        };
        client.setScreen(new FormTutorialScreen(offer.request.book, offer.request.form, acknowledge));
    }

    private static void schedule(Minecraft client, Screen book, boolean manual) {
        ResourceLocation form = currentForm(client);
        if (FormTutorialCatalog.supports(form) && client.getConnection() != null) {
            queued = new Request(book, client.getConnection(), form, manual, UUID.randomUUID());
        }
    }

    private static boolean matches(Minecraft client, Request request) {
        return client.screen == request.book && validContext(client, request);
    }

    private static boolean validContext(Minecraft client, Request request) {
        return client.level != null && client.player != null && client.player.isAlive()
                && client.getConnection() == request.connection
                && ownBook(client, request.book) && request.form.equals(currentForm(client));
    }
}
