package github.com.gengyoubo.sscfa.network;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Explicit opt-in for self decoration. Other viewers receive the original vanilla particle packet. */
public final class DecorationParticles {
    public static final ResourceLocation ID = new ResourceLocation("ssc_addon", "self_decoration_v1");
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("SSCA_DecorationParticles");
    private static boolean loggedOnce;

    private DecorationParticles() {}

    /** Compatibility for clients without the scene protocol, after the caller's original range checks. */
    public static void sendVisualFallback(ServerLevel world, ServerPlayer viewer, ClientboundLevelParticlesPacket packet,
                                          java.util.UUID owner, boolean projectile) {
        if (viewer.getUUID().equals(owner) && AddonNetwork.canSend(viewer, ID))
            sendEnvelope(world, viewer, packet, projectile);
        else viewer.connection.send(packet);
    }

    public static void spawn(ServerLevel world, Entity owner, ParticleOptions effect,
                             double x, double y, double z, int count,
                             double dx, double dy, double dz, double speed) {
        ClientboundLevelParticlesPacket original = new ClientboundLevelParticlesPacket(effect, false, x, y, z,
                (float) dx, (float) dy, (float) dz, (float) speed, count);
        for (ServerPlayer viewer : world.players()) {
            if (owner == viewer && AddonNetwork.canSend(viewer, ID)) {
                // 本人：走附属网络通道直发，
                // 不走 sendToPlayerIfNearby（其 32 格判定 + packet 参数在部分环境下可能被绕过）
                sendEnvelope(world, viewer, original);
            } else {
                // 等同原版 ServerLevel 的非强制粒子发送：32 格内才发
                if (viewer.blockPosition().closerToCenterThan(new net.minecraft.world.phys.Vec3(x, y, z), 32.0)) viewer.connection.send(original);
            }
        }
    }

    /** Same spherical range and force flag as ParticleUtils, including its 64-block variant. */
    public static void forced(ServerLevel world, Entity owner, ParticleOptions effect,
                              double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed, double range) {
        ClientboundLevelParticlesPacket original = new ClientboundLevelParticlesPacket(effect, true, x, y, z,
                (float) dx, (float) dy, (float) dz, (float) speed, count);
        for (ServerPlayer viewer : world.players()) {
            if (viewer.distanceToSqr(x, y, z) > range * range) continue;
            if (owner == viewer && AddonNetwork.canSend(viewer, ID)) {
                sendEnvelope(world, viewer, original);
            } else {
                viewer.connection.send(original);
            }
        }
    }

    /** Called after vanilla range/recipient checks, for explicitly scoped cosmetic actions only. */
    public static boolean trySendScoped(ServerLevel world, ServerPlayer viewer, Packet<?> packet) {
        if (DecorationParticleScope.isProtected()) return false;
        if (DecorationParticleScope.owner() != viewer || viewer.level() != world
                || !(packet instanceof ClientboundLevelParticlesPacket particles) || !AddonNetwork.canSend(viewer, ID)) return false;
        sendEnvelope(world, viewer, particles, DecorationParticleScope.isProjectileScope());
        return true;
    }

    /** 本人信封直发 + 一次性日志：若实机再失效，日志可直接定位断在哪一环。 */
    private static void sendEnvelope(ServerLevel world, ServerPlayer viewer, ClientboundLevelParticlesPacket original) {
        sendEnvelope(world, viewer, original, false);
    }

    /** 协议 v1.1：尾部追加 1 字节弹道标记（旧客户端读不到该字节按普通装饰处理，兼容）。 */
    private static void sendEnvelope(ServerLevel world, ServerPlayer viewer, ClientboundLevelParticlesPacket original,
                                     boolean projectile) {
        var buf = AddonNetwork.buffer();
        buf.writeResourceLocation(world.dimension().location());
        original.write(buf);
        buf.writeBoolean(projectile);
        AddonNetwork.sendToPlayer(viewer, ID, buf);
        if (!loggedOnce) {
            loggedOnce = true;
            LOGGER.info("[SSCA_DecorationParticles] first self-decoration envelope sent to {} via channel {}",
                    viewer.getName().getString(), ID);
        }
    }
}
