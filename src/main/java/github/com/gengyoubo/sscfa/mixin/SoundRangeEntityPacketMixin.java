package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSoundEntityPacket.class)
public abstract class SoundRangeEntityPacketMixin {
    @Shadow @Final @Mutable private Holder<SoundEvent> sound;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/entity/Entity;FFJ)V", at = @At("RETURN"))
    private void ssca$extendTrackingSound(Holder<SoundEvent> event, SoundSource category,
                                         Entity entity, float volume, float pitch, long seed, CallbackInfo ci) {
        sound = AddonSoundRange.extendPlayback(sound, volume);
    }
}
