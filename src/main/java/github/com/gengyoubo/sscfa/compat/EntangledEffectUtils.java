package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;

/** 缂犵粫鏁堟灉宸ュ叿鍗犱綅銆?*/
public final class EntangledEffectUtils {
    private EntangledEffectUtils() {}

    public static void applyEntangledEffect(net.minecraft.world.entity.Entity owner, LivingEntity target, int ticks) {
        if (target != null) {
            target.addEffect(new MobEffectInstance(ModEffects.ENTANGLED_FULL.get(), Math.max(1, ticks), 0, false, true, true));
        }
    }
}
