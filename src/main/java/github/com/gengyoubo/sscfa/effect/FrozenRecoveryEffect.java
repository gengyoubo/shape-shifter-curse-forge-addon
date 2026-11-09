package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** The exact twenty-five-percent movement reduction after an ice enclosure shatters. */
public final class FrozenRecoveryEffect extends MobEffect {
    public FrozenRecoveryEffect() {
        super(MobEffectCategory.HARMFUL, 0x8DC9E8);
        // AnomalyEffects selects the strongest active anomaly slow using one shared modifier.
    }
}
