package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import github.com.gengyoubo.sscfa.sound.SoundRangeRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(PlaySoundCommand.class)
public abstract class SoundRangeCommandMixin {
    @Redirect(method = "playSound", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/sounds/SoundEvent;createVariableRangeEvent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/sounds/SoundEvent;"))
    private static SoundEvent ssca$extendSkillCommand(ResourceLocation id, CommandSourceStack command,
            Collection<ServerPlayer> players, ResourceLocation sound, SoundSource category, Vec3 position,
            float volume, float pitch, float minVolume) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        return SoundRangeRules.isAddonPlayback() ? AddonSoundRange.extend(event, volume) : event;
    }
}
