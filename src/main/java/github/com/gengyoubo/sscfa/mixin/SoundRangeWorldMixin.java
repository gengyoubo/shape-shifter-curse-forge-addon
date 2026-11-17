package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.sound.AddonSoundRange;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class SoundRangeWorldMixin {
    @ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), argsOnly = true)
    private Holder<SoundEvent> ssca$extendPositionedSound(Holder<SoundEvent> event, Player except, double x, double y,
                                                          double z, Holder<SoundEvent> sound, SoundSource source,
                                                          float volume) {
        return AddonSoundRange.extendPlayback(event, volume);
    }

    @ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), argsOnly = true)
    private Holder<SoundEvent> ssca$extendAttachedSound(Holder<SoundEvent> event, Player except, Entity entity,
                                                        Holder<SoundEvent> sound, SoundSource source, float volume) {
        return AddonSoundRange.extendPlayback(event, volume);
    }
}
