package github.com.gengyoubo.sscfa.client.particle;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.DecorationParticleScope;
import github.com.gengyoubo.sscfa.network.DecorationParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** 本人装饰粒子的归属作用域：发射、网络接收与 tag 打标。 */
public final class FirstPersonParticles {
    private static final ThreadLocal<UUID> SOURCE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PROJECTILE_SCOPE = new ThreadLocal<>();

    private FirstPersonParticles() {
    }

    public static void init() {
        AddonNetwork.registerClientReceiver(DecorationParticles.ID, buf -> {
            ResourceLocation dimension = buf.readResourceLocation();
            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(buf);
            boolean projectile;
            try {
                projectile = buf.readBoolean();
            } catch (Exception legacy) {
                projectile = false;
            }
            boolean isProjectile = projectile;
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.getConnection() == null || client.level == null || client.player == null
                        || !client.level.dimension().location().equals(dimension)) {
                    return;
                }
                emitScoped(client.player.getUUID(), isProjectile,
                        () -> client.getConnection().handleParticleEvent(packet));
            });
        });
    }

    public static void emit(UUID owner, Runnable emission) {
        emitScoped(owner, false, emission);
    }

    public static void emitProjectile(UUID owner, Runnable emission) {
        emitScoped(owner, true, emission);
    }

    private static void emitScoped(UUID owner, boolean projectile, Runnable emission) {
        UUID previous = SOURCE.get();
        Boolean previousProjectile = PROJECTILE_SCOPE.get();
        SOURCE.set(owner);
        PROJECTILE_SCOPE.set(projectile);
        try {
            emission.run();
        } finally {
            if (previous == null) SOURCE.remove();
            else SOURCE.set(previous);
            if (previousProjectile == null) PROJECTILE_SCOPE.remove();
            else PROJECTILE_SCOPE.set(previousProjectile);
        }
    }

    public static void tag(Particle particle) {
        if (DecorationParticleScope.isProtected()) return;
        UUID owner = SOURCE.get();
        boolean projectile = Boolean.TRUE.equals(PROJECTILE_SCOPE.get());
        var scopedOwner = DecorationParticleScope.owner();
        if (owner == null && scopedOwner != null) {
            owner = scopedOwner.getUUID();
            projectile = DecorationParticleScope.isProjectileScope();
        }
        if (owner == null || particle == null) return;
        Minecraft client = Minecraft.getInstance();
        var player = client == null ? null : client.player;
        if (player != null && owner.equals(player.getUUID())
                && particle instanceof ParticleOwnership decoration) {
            decoration.ssca$setDecorationOwner(owner);
            if (projectile) decoration.ssca$markProjectileDecoration();
        }
    }
}
