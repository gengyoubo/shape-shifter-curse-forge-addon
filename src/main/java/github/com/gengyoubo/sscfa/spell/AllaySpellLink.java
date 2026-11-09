package github.com.gengyoubo.sscfa.spell;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 悦灵形态协同（Forge 移植占位）。保留 DreadWhisperSpell 等所需的 {@link Cast} 形状；
 * 实际法力/共鸣逻辑待能力层移植后补回。
 */
public final class AllaySpellLink {
    private AllaySpellLink() {}

    public static final class Cast {
        public final int kind;
        public Cast(int kind) { this.kind = kind; }
        public boolean chain(ServerPlayer caster, LivingEntity target, Set<UUID> originals,
                             Function<LivingEntity, Boolean> effect) {
            return false;
        }
    }

    public static Cast context(Entity entity) { return null; }
    public static Entity owner(Entity entity) { return entity; }
    public static Cast create(ServerPlayer player, double actual, Object record, boolean directCurse) { return null; }
    public static void register() {}
}
