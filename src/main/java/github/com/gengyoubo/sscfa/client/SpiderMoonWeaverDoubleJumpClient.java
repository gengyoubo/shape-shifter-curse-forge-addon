package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** 月织蛛/跳蛛二段跳客户端：跳跃键边沿触发，本地施加跳跃速度并通知服务端广播特效。 */
public final class SpiderMoonWeaverDoubleJumpClient {
    private static boolean wasJumpPressed = false;
    private static int airTicks = 999;
    private static final int MIN_AIR_TICKS = 3;
    private static boolean jumpAvailable = false;

    private SpiderMoonWeaverDoubleJumpClient() {
    }

    public static boolean isJumpAvailable() {
        return jumpAvailable;
    }

    public static int getAirTicks() {
        return airTicks;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            wasJumpPressed = false;
            airTicks = 999;
            jumpAvailable = false;
            return;
        }
        if (player.onGround()) {
            airTicks = 0;
            jumpAvailable = true;
        } else if (airTicks < MIN_AIR_TICKS + 10) {
            airTicks++;
        }
        boolean pressed = client.options.keyJump.isDown();
        if (pressed && !wasJumpPressed && !player.onGround() && jumpAvailable && airTicks >= MIN_AIR_TICKS
                && (FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER)
                        || FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE))
                && !SpiderMoonWeaverSwingClient.isLocalActive()) {
            doDoubleJump(player);
            jumpAvailable = false;
            SscAddonNetworking.sendEmpty(SscAddonNetworking.PACKET_SPIDER_MOON_WEAVER_DOUBLE_JUMP);
        }
        wasJumpPressed = pressed;
    }

    private static void doDoubleJump(LocalPlayer player) {
        float jumpVelocity = 0.42f;
        var jumpBoost = player.getEffect(MobEffects.JUMP);
        if (jumpBoost != null) {
            jumpVelocity += 0.1f * (jumpBoost.getAmplifier() + 1);
        }
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, jumpVelocity, velocity.z);
        if (player.isSprinting()) {
            float yaw = player.getYRot() * (Mth.PI / 180f);
            player.setDeltaMovement(player.getDeltaMovement()
                    .add(-Mth.sin(yaw) * 0.2f, 0.0, Mth.cos(yaw) * 0.2f));
        }
        player.hasImpulse = true;
    }
}
