package github.com.gengyoubo.sscfa.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class FrozenProjectileKnockbackMixin {
    @WrapOperation(method = "onHitEntity",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private void ssca$ignoreArrowKnockback(LivingEntity target,double x,double y,double z,Operation<Void> original) {
        if (!FrozenControl.blocked(target)) original.call(target,x,y,z);
    }
}
