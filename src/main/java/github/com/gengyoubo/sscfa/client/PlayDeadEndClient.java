package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** 美西螈装死：装死期间再按副技能键请求提前结束。 */
public final class PlayDeadEndClient {
    private static boolean wasKeyPressed = false;
    private static boolean wasPlayingDead = false;
    private static boolean secondaryNeedsRelease = false;
    private static long playDeadStartTime = 0L;
    private static final int END_GRACE_TICKS = 10;

    private PlayDeadEndClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
    }

    public static boolean blocksSkillInput(boolean primary) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (player.hasEffect(SscAddon.PLAYING_DEAD)
                || !primary && secondaryNeedsRelease && SscAddonKeybindings.isSecondaryRawPressed());
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            wasKeyPressed = false;
            wasPlayingDead = false;
            secondaryNeedsRelease = false;
            return;
        }
        boolean isPlayingDead = player.hasEffect(SscAddon.PLAYING_DEAD);
        if (isPlayingDead && !wasPlayingDead) {
            playDeadStartTime = client.level.getGameTime();
        }
        boolean pressed = SscAddonKeybindings.isSecondaryRawPressed();
        if (!pressed) secondaryNeedsRelease = false;
        else if (isPlayingDead) secondaryNeedsRelease = true;
        if (isPlayingDead && pressed && !wasKeyPressed
                && (client.level.getGameTime() - playDeadStartTime) >= END_GRACE_TICKS) {
            SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_PLAY_DEAD_END);
        }
        wasKeyPressed = pressed;
        wasPlayingDead = isPlayingDead;
    }
}
