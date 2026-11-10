package github.com.gengyoubo.sscfa.mixin.player;

import github.com.gengyoubo.sscfa.ability.PlayerGravityLease;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import github.com.gengyoubo.sscfa.ability.PlayDeadAbsorptionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PlayerSkillStateSaveMixin {
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void ssc_addon$excludeTemporaryGravity(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            PlayerGravityLease.writePersistentGravity(player, cir.getReturnValue());
            PlayDeadAbsorptionManager.writePlayerData(player, cir.getReturnValue());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void ssc_addon$restoreOwnedAbsorption(CompoundTag nbt, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) PlayDeadAbsorptionManager.readPlayerData(player, nbt);
    }
}
