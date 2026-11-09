package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSoundPacket.class)
public abstract class SoundRangePacketMixin {
    @Shadow @Final @Mutable private Holder<SoundEvent> sound;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;DDDFFJ)V", at = @At("RETURN"))
    private void ssca$extendDirectSound(Holder<SoundEvent> event, SoundSource category,
                                       double x, double y, double z, float volume, float pitch, long seed,
                                       CallbackInfo ci) {
        sound = AddonSoundRange.extendPlayback(sound, volume);
    }
}
