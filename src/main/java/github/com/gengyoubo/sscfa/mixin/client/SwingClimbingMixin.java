package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.SpiderMoonWeaverSwingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 挂着蛛丝摆荡时，攀爬判定不应抢占垂直移动。 */
@Mixin(LivingEntity.class)
public abstract class SwingClimbingMixin {

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void ssca$ropeControlsClimbing(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if ((Object) this != Minecraft.getInstance().player) return;
        var swing = SpiderMoonWeaverSwingClient.getLocalSwing();
        if (swing != null && swing.active && swing.state == SpiderMoonWeaverSwingClient.STATE_SWINGING) {
            cir.setReturnValue(false);
        }
    }
}
