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
 * 口袋维度逐玩家区块过滤：区块数据只经 {@code sendChunkPacket} 一处发出（1.20.1 源码核实），
 * 在此拦下不属于玩家所在房间槽位的区块 → 客户端墙外永远只有虚空。其他维度不受影响。
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
