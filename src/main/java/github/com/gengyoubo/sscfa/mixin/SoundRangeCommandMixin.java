package github.com.gengyoubo.sscfa.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import github.com.gengyoubo.sscfa.sound.SoundRangeRules;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlaySoundCommand.class)
public abstract class SoundRangeCommandMixin {
    @ModifyExpressionValue(method = "playSound", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/sounds/SoundEvent;createVariableRangeEvent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/sounds/SoundEvent;"))
    private static SoundEvent ssca$extendSkillCommand(SoundEvent event,
                                                     @Local(argsOnly = true, ordinal = 0) float volume) {
        return SoundRangeRules.isAddonPlayback() ? AddonSoundRange.extend(event, volume) : event;
    }
}
