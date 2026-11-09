package github.com.gengyoubo.sscfa.util;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;

/** 完全控制（晕眩、冰冻、装死、完全缠绕）禁止主动技能；只限制移动的定身和施法减速不禁止。 */
public final class AbilityUseGuard {
    private AbilityUseGuard() {}

    public interface PowerOwner {
        LivingEntity ssca$abilityOwner();
    }

    public static boolean fullyControlled(LivingEntity entity) {
        return entity != null && (entity.hasEffect(AddonEffects.STUN.get())
                || entity.hasEffect(AddonEffects.FROZEN.get())
                || entity.hasEffect(AddonEffects.PLAYING_DEAD.get())
                || entity.hasEffect(ModEffects.ENTANGLED_FULL.get()));
    }

    /** 完全控制或变羊期间禁止使用技能。 */
    public static boolean blocked(LivingEntity entity) {
        return fullyControlled(entity) || entity != null && entity.hasEffect(AddonEffects.SHEEP_FORM.get());
    }
}
