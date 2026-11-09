package github.com.gengyoubo.sscfa.spell;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 悦灵形态协同（Forge 占位）：保留投射物/施法所需的 Cast 形状。 */
public final class AllaySpellLink {
    private AllaySpellLink() {}

    public interface ProjectileData {
        Cast ssca$allay();
        void ssca$allay(Cast cast);
    }

    public static final class Cast {
        public final int kind;
        public Cast(int kind) { this.kind = kind; }
        public CompoundTag write() { CompoundTag n = new CompoundTag(); n.putInt("Kind", kind); return n; }
        public static Cast read(CompoundTag n) { return new Cast(n.getInt("Kind")); }
        public boolean chain(ServerPlayer caster, LivingEntity target, Set<UUID> originals,
                             Function<LivingEntity, Boolean> effect) { return false; }
    }

    public static Cast context(Entity entity) {
        return entity instanceof ProjectileData data ? data.ssca$allay() : null;
    }
    public static Entity owner(Entity entity) { return entity; }
    public static Cast create(ServerPlayer player, double actual, Object record, boolean directCurse) { return null; }
    public static void register() {}
}
