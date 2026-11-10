package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 月织蛛二段跳调试图文记录：开启后把跳跃物理状态写入日志（不依赖 playerAnimator）。 */
public final class SpiderMoonWeaverAnimDebugHud {
    private static final Logger LOGGER = LoggerFactory.getLogger("SSCA_AnimDebug");
    private static boolean recording = false;
    private static Boolean lastOnGround = null;
    private static int ticksSinceLog = 0;

    private SpiderMoonWeaverAnimDebugHud() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_ANIM_DEBUG_TOGGLE, buf -> toggleRecording());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
    }

    public static boolean toggleRecording() {
        recording = !recording;
        if (!recording) {
            lastOnGround = null;
            ticksSinceLog = 0;
        }
        return recording;
    }

    private static void onClientTick(Minecraft client) {
        if (!recording) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            lastOnGround = null;
            return;
        }
        boolean onGround = player.onGround();
        ticksSinceLog++;
        boolean groundedChanged = lastOnGround == null || lastOnGround != onGround;
        if (groundedChanged || ticksSinceLog >= 20) {
            ticksSinceLog = 0;
            lastOnGround = onGround;
            LOGGER.info("onGround={} velY={} fallDist={} airTicks={} djAvail={} sprint={}",
                    onGround,
                    String.format("%.3f", player.getDeltaMovement().y),
                    String.format("%.3f", player.fallDistance),
                    SpiderMoonWeaverDoubleJumpClient.getAirTicks(),
                    SpiderMoonWeaverDoubleJumpClient.isJumpAvailable(),
                    player.isSprinting());
        }
    }
}
