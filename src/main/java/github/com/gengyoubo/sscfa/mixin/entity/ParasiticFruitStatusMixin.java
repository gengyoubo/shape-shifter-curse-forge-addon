package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.ability.fruit.FruitStatusOverlay;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让寄生果实叠加层观察实体上的效果施加、强制设置与移除。 */
@Mixin(LivingEntity.class)
public abstract class ParasiticFruitStatusMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void ssca$trackFruitMerge(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        FruitStatusOverlay.attempted((LivingEntity) (Object) this, effect, false);
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"))
    private void ssca$trackForcedExternalEffect(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        FruitStatusOverlay.attempted((LivingEntity) (Object) this, effect, true);
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void ssca$trackEffectRemoval(MobEffectInstance effect, CallbackInfo ci) {
        FruitStatusOverlay.removed((LivingEntity) (Object) this, effect.getEffect());
    }
}
