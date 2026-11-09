package github.com.gengyoubo.sscfa.spell;

import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;

/** 食梦魔形态协同占位。 */
public final class NightmareSpellLink {
    private NightmareSpellLink() {}
    public static <T> T with(Entity owner, Supplier<T> action) { return action.get(); }
    public static void register() {}
}
