package github.com.gengyoubo.sscfa.mixin.effect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.spell.FrostArmorAccess;
import net.minecraft.world.effect.AbsoptionMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbsoptionMobEffect.class)
public abstract class FrostArmorVanillaAbsorptionMixin {
    @WrapOperation(method = "removeAttributeModifiers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V"))
    private void ssca$preserveFrostArmor(LivingEntity entity, float amount, Operation<Void> original) {
        github.com.gengyoubo.sscfa.spell.FrostArmorState.removeOtherTo(
                entity, amount, value -> original.call(entity, value));
    }
}
