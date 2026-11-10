package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.entity.FrostThornEntity;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** 寒棘狐冰刺：主技能长按蓄力/点按发射；副技能凝棘蓄力。 */
public final class FrostSpikeClient {
    private static final long HOLD_MS = 250L;

    private static boolean wasPressed = false;
    private static boolean startSent = false;
    private static long pressDownMs = 0L;
    private static boolean secondaryWasPressed = false;
    private static boolean secondaryCharging = false;

    private FrostSpikeClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        KeyMapping key = SscAddonKeybindings.getPrimaryKey();
        KeyMapping secondaryKey = SscAddonKeybindings.getSecondaryKey();
        if (player == null || client.level == null || key == null
                || !FormUtils.isForm(player, FormIdentifiers.SNOW_FOX_FROSTSPINE)) {
            if (wasPressed && startSent) {
                SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_CHARGE_RELEASE);
            }
            if (secondaryCharging) {
                SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_SECONDARY_RELEASE);
                secondaryCharging = false;
            }
            wasPressed = false;
            startSent = false;
            secondaryWasPressed = false;
            return;
        }

        boolean pressed = key.isDown();
        long now = System.currentTimeMillis();
        if (pressed && !wasPressed) {
            pressDownMs = now;
            startSent = false;
        } else if (pressed) {
            if (!startSent && (now - pressDownMs) >= HOLD_MS) {
                SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_CHARGE_START);
                startSent = true;
            }
        } else if (wasPressed) {
            if (startSent) {
                SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_CHARGE_RELEASE);
            } else {
                SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_FIRE);
            }
        }
        wasPressed = pressed;

        boolean secondaryPressed = secondaryKey != null && secondaryKey.isDown();
        if (secondaryPressed && !secondaryWasPressed) {
            if (hasHoverThorn(player)) {
                secondaryCharging = true;
                SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_SECONDARY_START);
            }
        } else if (!secondaryPressed && secondaryWasPressed && secondaryCharging) {
            SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_FROST_SPIKE_SECONDARY_RELEASE);
            secondaryCharging = false;
        }
        secondaryWasPressed = secondaryPressed;
    }

    public static boolean isSecondaryCharging() {
        return secondaryCharging;
    }

    private static boolean hasHoverThorn(LocalPlayer player) {
        return !player.level().getEntitiesOfClass(FrostThornEntity.class,
                player.getBoundingBox().inflate(3.0),
                thorn -> thorn.isHover() && player.getUUID().equals(thorn.getOwnerUuid().orElse(null))).isEmpty();
    }
}
