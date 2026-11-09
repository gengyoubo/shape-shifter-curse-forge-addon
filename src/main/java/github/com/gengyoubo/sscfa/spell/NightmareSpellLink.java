package github.com.gengyoubo.sscfa.spell;

import java.util.function.Supplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 食梦魔形态协同占位。 */
public final class NightmareSpellLink {
    private NightmareSpellLink() {}
    public static <T> T with(Entity owner, Supplier<T> action) { return action.get(); }
    public static float damage(LivingEntity target, DamageSource source, float amount) { return amount; }
    public static MobEffectInstance debuff(LivingEntity target, MobEffectInstance effect) { return effect; }
    public static void register() {}
}
