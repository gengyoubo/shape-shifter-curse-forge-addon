package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.SpellcastClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Local targeting feedback, independent of entity glow flags and server effects. */
@Mixin(Minecraft.class)
public class LunarTargetOutlineMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void ssca$lunarOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SpellcastClient.isLunarTarget(entity)) cir.setReturnValue(true);
    }
}
