package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionAccess;
import github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ParasiticAbsorptionMixin implements ParasiticAbsorptionAccess {
    @Unique private ParasiticAbsorptionState ssca$parasiticAbsorption;

    @Override public ParasiticAbsorptionState ssca$parasiticAbsorption() {
        if (ssca$parasiticAbsorption == null) ssca$parasiticAbsorption = new ParasiticAbsorptionState();
        return ssca$parasiticAbsorption;
    }

    @Inject(method = "setAbsorptionAmount", at = @At("HEAD"))
    private void ssca$consume(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) && ssca$parasiticAbsorption != null)
            ssca$parasiticAbsorption.onChanged(self, self.getAbsorptionAmount(), amount);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ssca$save(CompoundTag nbt, CallbackInfo ci) { ssca$parasiticAbsorption().write(nbt); }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ssca$load(CompoundTag nbt, CallbackInfo ci) {
        ssca$parasiticAbsorption().read(nbt, (LivingEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ssca$expire(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && ssca$parasiticAbsorption != null && ssca$parasiticAbsorption.remaining() > 0
                && (!self.hasEffect(SscAddon.BAT_ABSORPTION)
                || ssca$parasiticAbsorption.expired(self.level().getGameTime()))) {
            ssca$parasiticAbsorption.remove(self);
            self.removeEffect(SscAddon.BAT_ABSORPTION);
        }
    }
}
