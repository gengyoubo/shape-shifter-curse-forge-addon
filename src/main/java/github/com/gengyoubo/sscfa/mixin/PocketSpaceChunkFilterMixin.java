package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.pocket.PocketSpaceManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 閸欙綀顣扮紒鏉戝闁劗甯虹€硅泛灏崸妤勭箖濠娿倧绱伴崠鍝勬健閺佺増宓侀崣顏嗙病 {@code sendChunkPacket} 娑撯偓婢跺嫬褰傞崙鐚寸礄1.20.1 濠ф劗鐖滈弽绋跨杽閿涘绱?
 * 閸︺劍顒濋幏锔跨瑓娑撳秴鐫樻禍搴ｅ负鐎硅埖澧嶉崷銊﹀煣闂傚瓨蝎娴ｅ秶娈戦崠鍝勬健 閳?鐎广垺鍩涚粩顖氼暰婢舵牗妗堟潻婊冨涧閺堝娅勭粚鎭掆偓鍌氬従娴犳牜娣惔锔跨瑝閸欐濂栭崫宥冣偓?
 */
@Mixin(ServerPlayer.class)
public abstract class PocketSpaceChunkFilterMixin {
	@Inject(method = "trackChunk", at = @At("HEAD"), cancellable = true)
	private void sscAddon$filterPocketChunks(ChunkPos chunkPos, Packet<?> chunkDataPacket, CallbackInfo ci) {
		if (!PocketSpaceManager.shouldSendChunk((ServerPlayer) (Object) this, chunkPos)) {
			ci.cancel();
		}
	}
}
