package github.com.gengyoubo.sscfa.spell;

import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;

/** 月织蛛/跳蛛形态协同占位：保留投射物织网接口形状。 */
public final class SpiderSpellLink {
    private SpiderSpellLink() {}

    public interface WebProjectile {
        Impact ssca$weave();
    }

    public static final class Impact {
        public boolean enabled() { return false; }
        public void enable() {}
        public void enable(UUID group) {}
        public void enable(Set<UUID> group) {}
        public void loaded(ServerLevel world, UUID projectile) {}
        public void direct() {}
        public void ground(ServerPlayer caster, BlockHitResult hit) {}
        public void write(CompoundTag nbt) {}
        public void read(CompoundTag nbt) {}
    }

    public static void directHit(Entity source, LivingEntity target) {}
    public static void register() {}
}
