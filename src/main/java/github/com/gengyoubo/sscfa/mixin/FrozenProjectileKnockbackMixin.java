package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class FrozenProjectileKnockbackMixin {
    @Redirect(method = "onHitEntity",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private void ssca$ignoreArrowKnockback(LivingEntity target,double x, double y, double z) {
        if (!FrozenControl.blocked(target)) target.push(x, y, z);
    }
}
