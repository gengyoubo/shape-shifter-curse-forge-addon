package github.com.gengyoubo.sscfa.mixin.effect;

import net.minecraft.world.effect.AbsoptionMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbsoptionMobEffect.class)
public abstract class FrostArmorVanillaAbsorptionMixin {
    @Redirect(method = "removeAttributeModifiers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V"))
    private void ssca$preserveFrostArmor(LivingEntity entity, float amount) {
        github.com.gengyoubo.sscfa.spell.FrostArmorState.removeOtherTo(
                entity, amount, entity::setAbsorptionAmount);
    }
}
