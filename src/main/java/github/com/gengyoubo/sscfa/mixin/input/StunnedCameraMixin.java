package github.com.gengyoubo.sscfa.mixin.input;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to lock camera movement when player is stunned
 */
@Mixin(MouseHandler.class)
public class StunnedCameraMixin {

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void onUpdateMouse(CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null && client.player.hasEffect(SscAddon.STUN)) {
			// Cancel all mouse movement updates when stunned - this locks the camera
			ci.cancel();
		}
	}
}
