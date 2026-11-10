package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import github.com.gengyoubo.sscfa.sound.ExtendedRangeSound;
import github.com.gengyoubo.sscfa.sound.SoundRangeRules;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class SoundRangeSystemMixin {
    @ModifyExpressionValue(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/Sound;getAttenuationDistance()I"))
    private int ssca$extendAttenuation(int distance, @Local(argsOnly = true) SoundInstance sound) {
        boolean extended = sound instanceof ExtendedRangeSound ranged && ranged.ssca$hasExtendedRange();
        return extended || SoundRangeRules.isAddonPlayback() ? distance * SoundRangeRules.MULTIPLIER : distance;
    }
}
