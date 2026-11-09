package github.com.gengyoubo.sscfa.spell;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** 月织蛛/跳蛛形态协同占位：保留投射物织网接口形状。 */
public final class SpiderSpellLink {
    private SpiderSpellLink() {}

    public interface WebProjectile {
        RollGroup ssca$weave();
    }

    public static final class RollGroup {
        public boolean enabled() { return false; }
        public void enable(UUID id) {}
        public void loaded(Level world, UUID id) {}
        public void direct() {}
        public void ground(net.minecraft.server.level.ServerPlayer caster, net.minecraft.world.phys.BlockHitResult ground) {}
    }

    public static void directHit(Entity source, LivingEntity target) {}
    public static void register() {}
}
