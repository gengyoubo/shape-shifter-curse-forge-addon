package github.com.gengyoubo.sscfa.network.visual;

import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端授权的装饰性视觉场景。Forge 移植阶段先保留与游戏逻辑一致的空实现：
 * 粒子/场景纯客户端表现，等客户端渲染层移植后再通过 {@link github.com.gengyoubo.sscfa.network.AddonNetwork}
 * 下发场景包。伤害、资源、碰撞从不委托给客户端。
 */
public final class ParametricVisuals {
    public enum Kind {
        CORRUPT_MIST(32, false), METEOR_WARNING(32, false), DOMAIN_EDGE(512, true),
        DOMAIN_AMBIENT(512, true), BLUE_FIRE_RING(512, true), SSC_WEB_TRAIL(32, false),
        FIRE_BOLT_TRAIL(512, true), LUNAR_BOLT_TRAIL(32, false),
        MIST_FORM_AMBIENT(512, true), MIST_FORM_CHARGE(512, true);
        public final double range;
        public final boolean force;
        Kind(double range, boolean force) { this.range = range; this.force = force; }
    }

    private ParametricVisuals() {}

    public static void init() {}

    public static void touch(Entity anchor, UUID eventId, Kind kind, UUID decorationOwner, Vec3 origin,
                             boolean follows, long epoch, int duration, double extent, double... parameters) {}

    public static void stop(UUID eventId, Kind kind) {}

    public static void circle(Entity actor, ParticleOptions particle, boolean force, int count, double speed,
                              Vec3 spread, Vec3 offset, double radius, int samples,
                              Predicate<ServerPlayer> audience, UUID decorationOwner) {}
}
