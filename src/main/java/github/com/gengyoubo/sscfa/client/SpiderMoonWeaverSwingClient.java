package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 月织蛛「蛛丝荡漾」客户端输入采集 + 状态镜像。 */
public final class SpiderMoonWeaverSwingClient {

    public static final int STATE_FIRING = 1;
    public static final int STATE_SWINGING = 2;
    public static final int STATE_TETHER = 3;

    public static final class LocalSwing {
        public boolean active;
        public int state;
        public double anchorX;
        public double anchorY;
        public double anchorZ;
        public int tetherEntityId = -1;
        public double ropeLen;
        public boolean canExtend = true;
        public int reelIntent;
        public Vec3 swingVelocity = Vec3.ZERO;
        public boolean physicsStarted;
    }

    private static final Map<UUID, LocalSwing> DATA = new ConcurrentHashMap<>();
    private static boolean wasSecondaryPressed = false;

    private SpiderMoonWeaverSwingClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_SPIDER_MOON_WEAVER_SWING_STATE, buf -> {
            UUID uuid = buf.readUUID();
            boolean active = buf.readBoolean();
            double ax = buf.readDouble();
            double ay = buf.readDouble();
            double az = buf.readDouble();
            double ropeLen = buf.readDouble();
            int state = buf.readVarInt();
            boolean canExtend = buf.readBoolean();
            int tetherEntityId = buf.readInt();
            onStateSync(uuid, active, ax, ay, az, ropeLen, state, canExtend, tetherEntityId);
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            clear();
            github.com.gengyoubo.sscfa.entity.SpiderSwingBullet.resetClientState();
        });
    }

    public static LocalSwing getLocalSwing() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return null;
        return DATA.get(client.player.getUUID());
    }

    public static boolean isLocalActive() {
        LocalSwing swing = getLocalSwing();
        return swing != null && swing.active;
    }

    public static Iterator<Map.Entry<UUID, LocalSwing>> iterator() {
        return DATA.entrySet().iterator();
    }

    public static void clear() {
        DATA.clear();
        wasSecondaryPressed = false;
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel world = client.level;
        if (player == null || world == null) return;
        KeyMapping secondaryKey = SscAddonKeybindings.getSecondaryKey();
        if (secondaryKey == null) return;

        if (!FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER)) {
            wasSecondaryPressed = false;
            return;
        }

        boolean pressed = secondaryKey.isDown();
        if (pressed && !wasSecondaryPressed) {
            sendPress();
        }
        wasSecondaryPressed = pressed;

        LocalSwing swing = DATA.get(player.getUUID());
        if (swing != null && swing.active && swing.state == STATE_SWINGING) {
            if ((player.tickCount & 1) == 0) {
                sendSync(swing.ropeLen, swing.reelIntent);
            }
        }
    }

    private static void sendPress() {
        SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_SPIDER_MOON_WEAVER_SWING_PRESS);
    }

    private static void sendSync(double ropeLen, int reel) {
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeDouble(ropeLen);
        buf.writeVarInt(reel);
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_SPIDER_MOON_WEAVER_SWING_SYNC, buf);
    }

    public static void onStateSync(UUID uuid, boolean active, double ax, double ay, double az,
                                   double ropeLen, int state, boolean canExtend, int tetherEntityId) {
        if (!active) {
            DATA.remove(uuid);
            return;
        }
        Minecraft client = Minecraft.getInstance();
        boolean isLocal = client.player != null && client.player.getUUID().equals(uuid);
        LocalSwing swing = DATA.computeIfAbsent(uuid, key -> new LocalSwing());
        boolean wasActive = swing.active && (swing.state == STATE_SWINGING || swing.state == STATE_TETHER);
        swing.active = true;
        swing.anchorX = ax;
        swing.anchorY = ay;
        swing.anchorZ = az;
        swing.state = state;
        swing.canExtend = canExtend;
        swing.tetherEntityId = tetherEntityId;
        if (isLocal) {
            if (!wasActive && (state == STATE_SWINGING || state == STATE_TETHER)) {
                swing.ropeLen = ropeLen;
                swing.physicsStarted = false;
            }
        } else {
            swing.ropeLen = ropeLen;
        }
    }
}
