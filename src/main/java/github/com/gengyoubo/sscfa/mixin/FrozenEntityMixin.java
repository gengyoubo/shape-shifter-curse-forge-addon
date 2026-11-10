package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.com.gengyoubo.sscfa.SscAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FrozenEntityMixin {
    @WrapMethod(method = "hurt")
    private boolean ssca$shatterOnEffectiveDamage(DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity target = (LivingEntity)(Object)this;
        boolean wasFrozen = !target.level().isClientSide && FrozenControl.blocked(target);
        boolean damaged = original.call(source, amount);
        if (wasFrozen && damaged) FrozenControl.breakFromHit(target);
        if (damaged && !target.level().isClientSide)
            github.com.gengyoubo.sscfa.anomaly.AnomalyManager.damageObserved(target);
        github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.afterDamage(target, source, damaged);
        return damaged;
    }
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 ssca$freezeInput(Vec3 movementInput) {
        return FrozenControl.blocked((LivingEntity)(Object)this) ? Vec3.ZERO : movementInput;
    }
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void ssca$ignoreAttackKnockback(double strength,double x,double z,CallbackInfo ci) {
        if (FrozenControl.blocked((LivingEntity)(Object)this)) ci.cancel();
    }
    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void ssca$releaseRemovedFreeze(MobEffectInstance effect, CallbackInfo ci) {
        LivingEntity target = (LivingEntity)(Object)this;
        if (effect.getEffect() == SscAddon.FROZEN && !target.level().isClientSide) FrozenControl.release(target);
    }
}
