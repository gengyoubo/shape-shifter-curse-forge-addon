package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.anomaly.AnomalyEffectAccess;
import github.com.gengyoubo.sscfa.anomaly.AnomalyEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Status changes update a lazy cache; damage/heal never scan all status effects or world entities. */
@Mixin(LivingEntity.class)
public abstract class AnomalyEffectMixin implements AnomalyEffectAccess {
    @Unique private AnomalyEffects.Cache ssca$anomalyEffects;
    @Unique private long ssca$lastAnomalyZoneHitTick = Long.MIN_VALUE;
    @Override public AnomalyEffects.Cache ssca$anomalyEffects() { return ssca$anomalyEffects; }
    @Override public void ssca$anomalyEffects(AnomalyEffects.Cache cache) { ssca$anomalyEffects = cache; }
    @Override public long ssca$lastAnomalyZoneHitTick() { return ssca$lastAnomalyZoneHitTick; }
    @Override public void ssca$lastAnomalyZoneHitTick(long tick) { ssca$lastAnomalyZoneHitTick = tick; }

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void ssca$anomalyApplied(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (AnomalyEffects.isManaged(effect.getEffect()))
            AnomalyEffects.refresh((LivingEntity)(Object)this);
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void ssca$anomalyUpgraded(MobEffectInstance effect, boolean reapply, Entity source, CallbackInfo ci) {
        if (AnomalyEffects.isManaged(effect.getEffect()))
            AnomalyEffects.refresh((LivingEntity)(Object)this);
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void ssca$anomalyRemoved(MobEffectInstance effect, CallbackInfo ci) {
        if (AnomalyEffects.isManaged(effect.getEffect()))
            AnomalyEffects.refresh((LivingEntity)(Object)this);
    }

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float ssca$anomalyHealing(float amount) {
        return AnomalyEffects.modifyHealing((LivingEntity)(Object)this, amount);
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void ssca$anomalySprint(boolean sprinting, CallbackInfo ci) {
        if (sprinting && AnomalyEffects.sprintBlocked((LivingEntity)(Object)this)) ci.cancel();
    }
}
