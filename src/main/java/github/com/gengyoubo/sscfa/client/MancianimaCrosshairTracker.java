package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.client.renderer.DomainRenderer;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;
import java.util.function.Predicate;

/** 契约准星目标缓存：每 tick 做一次 32 格射线，供本地绿色描边使用。 */
public final class MancianimaCrosshairTracker {
    private static final double MAX_DIST = 32.0;
    private static volatile UUID currentTarget = null;
    private static int tickCounter = 0;
    private static float lastYaw = Float.NaN;
    private static float lastPitch = Float.NaN;
    private static double lastX = Double.NaN;
    private static double lastY = Double.NaN;
    private static double lastZ = Double.NaN;

    private MancianimaCrosshairTracker() {
    }

    public static boolean isCurrent(UUID uuid) {
        UUID current = currentTarget;
        return current != null && uuid != null && current.equals(uuid);
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onTick(Minecraft.getInstance());
        });
    }

    private static void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel world = client.level;
        if (player == null || world == null) {
            currentTarget = null;
            return;
        }
        if (!FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) {
            currentTarget = null;
            return;
        }
        if (tickCounter++ % 4 == 0
                || player.getYRot() != lastYaw || player.getXRot() != lastPitch
                || player.getX() != lastX || player.getY() != lastY || player.getZ() != lastZ) {
            lastYaw = player.getYRot();
            lastPitch = player.getXRot();
            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();
            raycastTarget(world, player);
        }
    }

    private static void raycastTarget(ClientLevel world, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(MAX_DIST));

        var blockHit = world.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistSq = MAX_DIST * MAX_DIST;
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxDistSq = eye.distanceToSqr(blockHit.getLocation());
        }

        AABB box = player.getBoundingBox().expandTowards(look.scale(MAX_DIST)).inflate(1.0);
        Predicate<Entity> filter = entity -> entity != player && entity.isAlive()
                && entity instanceof LivingEntity
                && !DomainRenderer.blocksTargetingClient(entity)
                && !(entity instanceof Player)
                && !(entity instanceof TamableAnimal tame && tame.getOwnerUUID() != null);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box, filter, maxDistSq);
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            currentTarget = hit.getEntity().getUUID();
        } else {
            currentTarget = null;
        }
    }
}
