package github.com.gengyoubo.sscfa.spell;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 蝙蝠（德古拉）形态协同占位。 */
public final class BatSpellLink {
    private BatSpellLink() {}

    public interface ProjectileData {
        Cast ssca$bat();
        void ssca$bat(Cast cast);
    }

    public static final class Cast {
        public UUID owner;
        public int kind;
        public Cast() {}
        public Cast(UUID owner, int kind) { this.owner = owner; this.kind = kind; }
        public CompoundTag write() {
            CompoundTag n = new CompoundTag();
            if (owner != null) n.putUUID("Owner", owner);
            n.putInt("Kind", kind);
            return n;
        }
        public static Cast read(CompoundTag n) {
            Cast c = new Cast();
            c.owner = n.hasUUID("Owner") ? n.getUUID("Owner") : null;
            c.kind = n.getInt("Kind");
            return c;
        }
    }

    public static Cast current() { return null; }
    public static Cast of(Entity entity) { return entity instanceof ProjectileData data ? data.ssca$bat() : null; }
    public static <T> T with(Object bat, Supplier<T> action) { return action.get(); }
    public static void applied(Cast cast, LivingEntity target, MobEffectInstance effect, Entity source) {}
    public static void cured(LivingEntity target, MobEffect effect) {}
    public static void register() {}
}
