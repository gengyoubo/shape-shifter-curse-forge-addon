package github.com.gengyoubo.sscfa.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormAbilityEvents;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apoli night vision is rendered by SSC's night-vision mixins, without a short potion effect. */
@Mixin(value = FormAbilityEvents.class, remap = false)
public class FormNightVisionEffectMixin {
    @Inject(method = "applyEffect", at = @At("HEAD"), cancellable = true)
    private static void ssca$skipSyntheticNightVision(Player player, FormDefinition form, String flag,
                                                     MobEffect effect, CallbackInfo ci) {
        if ("night_vision".equals(flag)) ci.cancel();
    }
}
