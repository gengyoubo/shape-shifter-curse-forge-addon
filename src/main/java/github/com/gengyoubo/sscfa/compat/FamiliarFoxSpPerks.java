package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** 使魔 SP 药水/效果兼容占位。 */
public final class FamiliarFoxSpPerks {
    private FamiliarFoxSpPerks() {}

    public static boolean preservesEffectDuration(LivingEntity entity, MobEffect effect) {
        return false;
    }

    public static MobEffectInstance levelOneWithOriginalDuration(MobEffectInstance effect) {
        return effect;
    }
}
