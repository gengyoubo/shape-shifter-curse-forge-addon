package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.ControlledAbilityInterruption;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ControlledEffectInterruptionMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN"))
    private void ssca$interruptOnControl(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && effect.getEffect() == SscAddon.FROZEN) {
            github.com.gengyoubo.sscfa.anomaly.FrozenControl.applied((LivingEntity)(Object)this);
        }
        if (cir.getReturnValueZ() && (effect.getEffect() == SscAddon.STUN
                || effect.getEffect() == SscAddon.PLAYING_DEAD
                || effect.getEffect() == ModEffects.ENTANGLED_FULL.get())
                && (Object) this instanceof ServerPlayer player) ControlledAbilityInterruption.interrupt(player);
    }
}
