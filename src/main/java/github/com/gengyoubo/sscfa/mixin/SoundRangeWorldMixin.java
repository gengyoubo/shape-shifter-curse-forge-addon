package github.com.gengyoubo.sscfa.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class SoundRangeWorldMixin {
    @ModifyVariable(method = {
            "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"
    }, at = @At("HEAD"), argsOnly = true)
    private Holder<SoundEvent> ssca$extendSound(Holder<SoundEvent> event,
                                                     @Local(argsOnly = true, ordinal = 0) float volume) {
        return AddonSoundRange.extendPlayback(event, volume);
    }
}
