package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 濠㈡儼绮炬禒楣冩晬濮橆厽鎷遍柡鍫ｆ鐢櫣鈧鍎奸崵婊堢叕椤愩埄妲卞ù?DEAFEN 闁绘鍩栭埀顑跨劍濡炲倿妫冨▎鎾跺従闁圭鍋撻柡鍫濐槸閿涙劙妫呯粵鍦耿濞戞挸绉存總鏍传瀹ュ懎寰撳ù鐘崇墪椤撳綊骞嬫搴紓
@Mixin(SoundManager.class)
public abstract class SoundManagerDeafenMixin {
	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void ssc_addon$deafenMute(SoundInstance sound, CallbackInfo ci) {
		Player player = Minecraft.getInstance().player;
		if (player != null && player.hasEffect(SscAddon.DEAFEN)) {
			ci.cancel();
		}
	}
}
