package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Independent full freeze; normal status removal also restores movement speed immediately. */
public final class FrozenEffect extends MobEffect {
    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0x9DE8FF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "5175e13e-dd9a-4c37-8282-c150357ae65e",
                -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
