package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

/** 风灵「风之冲刺」客户端：主技能键边沿发包 + 悬浮期绿色落点预览。 */
public final class WindDashClient {
    private static final Vector3f GREEN = new Vector3f(0.3f, 0.95f, 0.4f);
    private static final int PREVIEW_INTERVAL = 2;

    private static boolean lastPressed = false;
    private static int previewTick = 0;

    private WindDashClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel world = client.level;
        if (player == null || world == null) {
            lastPressed = false;
            return;
        }

        boolean isWindSpirit = FormUtils.isOcelotSP(player);
        boolean pressed = isWindSpirit && client.screen == null
                && SscAddonKeybindings.getPrimaryKey().isDown();
        if (pressed && !lastPressed) {
            SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_WIND_DASH);
        }
        lastPressed = pressed;

        if (isWindSpirit && DashClientState.phase == DashClientState.PHASE_HOVER) {
            if ((previewTick++ % PREVIEW_INTERVAL) == 0) {
                Vec3 landing = computeHorizontalLanding(world, player);
                if (landing != null) renderGreenLanding(world, landing);
            }
        }

        if (!isWindSpirit) {
            DashClientState.reset();
        }
    }

    private static Vec3 computeHorizontalLanding(ClientLevel world, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f).normalize();
        Vec3 flatLook = new Vec3(look.x, 0, look.z);
        double flatLength = flatLook.length();
        double range = DashClientState.previewRange;
        double max3D = flatLength > 1.0e-4 ? range / flatLength : range;
        Vec3 end3D = eye.add(look.scale(max3D));
        BlockHitResult hit = world.clip(new ClipContext(eye, end3D,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 location = hit.getLocation();
            double groundY = findGroundYClient(world, location.x, location.z, Math.min(location.y, player.getY()));
            return new Vec3(location.x, groundY, location.z);
        }
        Vec3 flatDir;
        if (flatLength > 1.0e-4) {
            flatDir = flatLook.normalize();
        } else {
            float yaw = player.getYRot();
            flatDir = new Vec3(-Mth.sin(yaw * 0.017453292f), 0, Mth.cos(yaw * 0.017453292f));
        }
        double x = player.getX() + flatDir.x * range;
        double z = player.getZ() + flatDir.z * range;
        double groundY = findGroundYClient(world, x, z, player.getY());
        return new Vec3(x, groundY, z);
    }

    private static double findGroundYClient(ClientLevel world, double x, double z, double fromY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(fromY) + 1;
        for (int y = startY; y >= startY - 20 && y >= world.getMinBuildHeight(); y--) {
            pos.set(x, y, z);
            var state = world.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(world, pos).isEmpty()) {
                return y + 1;
            }
        }
        return fromY;
    }

    private static void renderGreenLanding(ClientLevel world, Vec3 landing) {
        DustParticleOptions dust = new DustParticleOptions(GREEN, 1.4f);
        double cx = landing.x;
        double cy = landing.y + 0.05;
        double cz = landing.z;
        world.addParticle(dust, cx, cy, cz, 0, 0, 0);
        int ringPoints = 24;
        for (int i = 0; i < ringPoints; i++) {
            double angle = (Math.PI * 2.0 / ringPoints) * i;
            world.addParticle(dust, cx + Math.cos(angle) * DashClientState.previewRadius, cy,
                    cz + Math.sin(angle) * DashClientState.previewRadius, 0, 0, 0);
        }
        for (int i = 1; i <= 4; i++) {
            world.addParticle(dust, cx, cy + i * 0.4, cz, 0, 0, 0);
        }
    }
}
