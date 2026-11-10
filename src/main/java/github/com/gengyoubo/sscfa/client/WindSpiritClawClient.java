package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** 风灵「疾风连爪」客户端：左键按住上报 + 副技能增伤 buff 边沿触发。 */
public final class WindSpiritClawClient {
    private static boolean lastHold = false;
    private static boolean lastSecondaryPressed = false;

    private WindSpiritClawClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            lastHold = false;
            ClawClientState.reset();
            return;
        }
        boolean isOcelot = FormUtils.isOcelotSP(player);

        boolean hold = isOcelot && client.screen == null && client.options.keyAttack.isDown();
        if (hold != lastHold) {
            FriendlyByteBuf buf = AddonNetwork.buffer();
            buf.writeBoolean(hold);
            AddonNetwork.sendToServer(SscAddonNetworking.PACKET_CLAW_HOLD, buf);
            lastHold = hold;
        }

        boolean secondaryPressed = isOcelot && client.screen == null
                && SscAddonKeybindings.isSecondaryRawPressed();
        if (secondaryPressed && !lastSecondaryPressed) {
            SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_CLAW_BUFF);
        }
        lastSecondaryPressed = secondaryPressed;

        if (!isOcelot) {
            ClawClientState.reset();
        }
    }
}
