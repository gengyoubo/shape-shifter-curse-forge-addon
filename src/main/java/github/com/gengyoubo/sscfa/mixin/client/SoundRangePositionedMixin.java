package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import github.com.gengyoubo.sscfa.sound.ExtendedRangeSound;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleSoundInstance.class)
public abstract class SoundRangePositionedMixin implements ExtendedRangeSound {
    @Unique private boolean ssca$extendedRange;
    @Override public boolean ssca$hasExtendedRange() { return ssca$extendedRange; }

    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/util/RandomSource;ZILnet/minecraft/client/resources/sounds/SoundInstance$Attenuation;DDD)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/sounds/SoundEvent;getLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation ssca$originalSoundId(ResourceLocation id) {
        return AddonSoundRange.originalId(id);
    }

    @Inject(method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/util/RandomSource;ZILnet/minecraft/client/resources/sounds/SoundInstance$Attenuation;DDD)V", at = @At("RETURN"))
    private void ssca$readSoundRange(SoundEvent event, SoundSource category, float volume, float pitch,
                                    RandomSource random, boolean repeat, int delay, SoundInstance.Attenuation attenuation,
                                    double x, double y, double z, CallbackInfo ci) {
        ssca$extendedRange = AddonSoundRange.isExtended(event, volume);
    }
}
