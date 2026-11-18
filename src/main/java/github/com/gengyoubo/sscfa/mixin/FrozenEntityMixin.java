package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import github.com.gengyoubo.sscfa.init.AddonAnomalyEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.SscAddon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(LivingEntity.class)
public abstract class FrozenEntityMixin {
    @Unique private Deque<Boolean> ssca$frozenHits;

    // Capture after cancellable HEAD hooks, before Forge dispatches the first damage event.
    @Inject(method = "hurt", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/common/ForgeHooks;onLivingAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void ssca$captureFreeze(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) return;
        if (ssca$frozenHits == null) ssca$frozenHits = new ArrayDeque<>();
        ssca$frozenHits.push(FrozenControl.blocked(target));
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void ssca$shatterOnEffectiveDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) return;
        boolean wasFrozen = ssca$frozenHits != null && !ssca$frozenHits.isEmpty() && ssca$frozenHits.pop();
        if (!cir.getReturnValueZ()) return;
        if (wasFrozen) FrozenControl.breakFromHit(target);
        github.com.gengyoubo.sscfa.anomaly.AnomalyManager.damageObserved(target);
        AddonAnomalyEffects.afterDamage(target, source, true);
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
