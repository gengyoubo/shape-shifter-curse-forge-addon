package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.sound.ExtendedRangeSound;
import github.com.gengyoubo.sscfa.sound.SoundRangeRules;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.resources.sounds.Sound;

@Mixin(SoundEngine.class)
public abstract class SoundRangeSystemMixin {
    @Redirect(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/Sound;getAttenuationDistance()I"))
    private int ssca$extendAttenuation(Sound resolved, SoundInstance sound) {
        int distance = resolved.getAttenuationDistance();
        boolean extended = sound instanceof ExtendedRangeSound ranged && ranged.ssca$hasExtendedRange();
        return extended || SoundRangeRules.isAddonPlayback() ? distance * SoundRangeRules.MULTIPLIER : distance;
    }
}
