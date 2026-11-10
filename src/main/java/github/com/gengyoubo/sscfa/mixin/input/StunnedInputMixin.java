package github.com.gengyoubo.sscfa.mixin.input;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class StunnedInputMixin {

	@Shadow public LocalPlayer player;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
		if (player != null && (player.hasEffect(SscAddon.STUN)
				|| github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked(player))) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void onDoItemUse(CallbackInfo ci) {
		if (player != null && (player.hasEffect(SscAddon.STUN)
				|| github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked(player))) {
			ci.cancel();
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void onHandleBlockBreaking(boolean breaking, CallbackInfo ci) {
		if (player != null && (player.hasEffect(SscAddon.STUN)
				|| github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked(player)) && breaking) {
			ci.cancel();
		}
	}
}
