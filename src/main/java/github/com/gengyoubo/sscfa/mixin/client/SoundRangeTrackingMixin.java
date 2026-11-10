package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import github.com.gengyoubo.sscfa.sound.ExtendedRangeSound;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityBoundSoundInstance.class)
public abstract class SoundRangeTrackingMixin implements ExtendedRangeSound {
    @Unique private boolean ssca$extendedRange;
    @Override public boolean ssca$hasExtendedRange() { return ssca$extendedRange; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ssca$readTrackingRange(SoundEvent event, SoundSource category, float volume, float pitch,
                                       Entity entity, long seed, CallbackInfo ci) {
        ssca$extendedRange = AddonSoundRange.isExtended(event, volume);
    }
}
