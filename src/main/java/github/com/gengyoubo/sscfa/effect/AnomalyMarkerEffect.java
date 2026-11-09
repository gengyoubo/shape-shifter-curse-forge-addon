package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Standard, synchronized and purifiable status; combat magnitudes live in the effect registry. */
public final class AnomalyMarkerEffect extends MobEffect {
    public AnomalyMarkerEffect(int color) { super(MobEffectCategory.HARMFUL, color); }
}
