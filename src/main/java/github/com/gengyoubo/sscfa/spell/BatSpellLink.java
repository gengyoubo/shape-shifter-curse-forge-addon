package github.com.gengyoubo.sscfa.spell;

import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 蝙蝠（德古拉）形态协同占位。 */
public final class BatSpellLink {
    private BatSpellLink() {}
    public static Object of(Entity entity) { return null; }
    public static <T> T with(Object bat, Supplier<T> action) { return action.get(); }
    public static void register() {}
}
