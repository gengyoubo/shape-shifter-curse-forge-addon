package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Resolve the wire marker to the original resource/stop ID before creating a sound instance. */
@Mixin(AbstractSoundInstance.class)
public abstract class SoundRangeIdMixin {
    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/util/RandomSource;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/sounds/SoundEvent;getLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation ssca$originalSoundId(ResourceLocation id) {
        return AddonSoundRange.originalId(id);
    }
}
