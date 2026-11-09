package github.com.gengyoubo.sscfa.spell;

import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 王庭/蝙蝠/食梦协同占位：执行回调但不改变结算。 */
public final class CourtSpellLink {
    private CourtSpellLink() {}
    public static Object of(Entity entity) { return null; }
    public static <T> T with(Object court, Supplier<T> action) { return action.get(); }
    public static boolean damage(Object court, Entity owner, LivingEntity target, Runnable action) {
        action.run();
        return true;
    }
    public static void register() {}
}
