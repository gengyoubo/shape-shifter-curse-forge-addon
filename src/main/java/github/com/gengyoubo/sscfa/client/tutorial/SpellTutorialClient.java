package github.com.gengyoubo.sscfa.client.tutorial;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.tutorial.SpellTutorialManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

/** 等世界、加载遮罩与其它界面就绪；断线即丢弃本连接的待播请求。 */
public final class SpellTutorialClient {
    private record Offer(ClientPacketListener connection, UUID nonce, UUID world, int[] glyphs) {}

    private static Offer pending;
    private static int delay;

    private SpellTutorialClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SpellTutorialManager.OFFER, buf -> {
            if (buf.readableBytes() != 36) return;
            UUID nonce = buf.readUUID();
            UUID world = buf.readUUID();
            boolean automatic = buf.readBoolean();
            int[] glyphs = {buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()};
            for (int glyph : glyphs) if (glyph >= 18) return;
            Minecraft client = Minecraft.getInstance();
            ClientPacketListener connection = client.getConnection();
            client.execute(() -> {
                pending = new Offer(connection, nonce, world, glyphs);
                delay = automatic ? 60 : 0;
            });
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            pending = null;
            delay = 0;
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
    }

    private static void tick(Minecraft client) {
        if (pending == null) return;
        if (client.getConnection() != pending.connection()) {
            pending = null;
            return;
        }
        if (client.level == null || client.player == null || !client.player.isAlive()
                || client.getOverlay() != null || client.screen != null) return;
        if (delay > 0) {
            delay--;
            return;
        }
        Offer shown = pending;
        pending = null;
        client.setScreen(new SpellTutorialScreen(shown.glyphs()));
        FriendlyByteBuf ack = AddonNetwork.buffer();
        ack.writeByte(1);
        ack.writeUUID(shown.nonce());
        AddonNetwork.sendToServer(SpellTutorialManager.ACTION, ack);
    }

    public static void replay() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.closeContainer();
        FriendlyByteBuf packet = AddonNetwork.buffer();
        packet.writeByte(0);
        AddonNetwork.sendToServer(SpellTutorialManager.ACTION, packet);
    }
}
