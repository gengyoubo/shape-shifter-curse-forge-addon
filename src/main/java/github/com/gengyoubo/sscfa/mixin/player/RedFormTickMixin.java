package github.com.gengyoubo.sscfa.mixin.player;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鐜╁瀹㈡埛绔瑷€鎹曡幏寰?mixin锛坖ackcooper锛夈€? *
 * <p>鍘?{@code tick} 娉ㄥ叆锛圫P 浣块瓟鍙樼孩 / 鎮︾伒鐗╁搧鍙戞斁 / 瓒呮椂鍥為€€锛夊凡杩佸埌
 * {@link github.com.gengyoubo.sscfa.ability.RedFormTickManager}锛堜笘鐣岀骇 tick 浜嬩欢锛夛紝
 * 鏈枃浠朵粎淇濈暀 {@code setClientSettings} 璇█鎹曡幏鈥斺€擣abric 1.20.1 鏃犲搴斾簨浠? * 锛圝OIN 鏃?ClientSettingsC2SPacket 鍙兘鏈埌銆佷篃涓嶈拷韪父鎴忎腑鏀硅瑷€锛夛紝蹇呴』淇濈暀姝ゆ敞鍏ャ€?/p>
 */
@Mixin(ServerPlayer.class)
public class RedFormTickMixin {

	// 鎹曡幏鐜╁瀹㈡埛绔瑷€璁剧疆锛屽瓨鍏scAddon.PLAYER_LANGUAGES
	@Inject(method = "updateOptions", at = @At("HEAD"))
	private void onSetClientSettings(ServerboundClientInformationPacket packet, CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		SscAddon.PLAYER_LANGUAGES.put(player.getUUID(), packet.language());
	}
}
