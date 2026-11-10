package github.com.gengyoubo.sscfa.mixin;

import java.util.Map;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Explosion.class)
public abstract class FrozenExplosionKnockbackMixin {
    @WrapOperation(method = "explode",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void ssca$ignoreBlastImpulse(Entity target,Vec3 velocity,Operation<Void> original) {
        if (!(target instanceof LivingEntity living) || !FrozenControl.blocked(living)) original.call(target,velocity);
    }
    @WrapOperation(method = "explode",at = @At(value = "INVOKE",target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ssca$removeClientBlastImpulse(Map<Player,Vec3> affected,Object key,Object value,Operation<Object> original) {
        return original.call(affected,key,key instanceof LivingEntity living && FrozenControl.blocked(living) ? Vec3.ZERO : value);
    }
}
