package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.pocket.PocketSpaceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 浠呬繚鎶ゅ彛琚嬬淮搴﹀澹冲拰鍑哄彛鍑规Ы锛涘唴閮ㄦ惌寤轰笌鍏朵粬缁村害涓嶅彈褰卞搷銆傛嫆缁濇椂閲嶅彂鏂瑰潡鏇存柊瀹炵幇銆岀牬鍧忓悗绔嬪嵆琛ュ洖銆嶃€?*/
@Mixin(Level.class)
public abstract class PocketSpaceBoundaryMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
			at = @At("HEAD"), cancellable = true)
	private void sscAddon$protectPocketBoundary(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerLevel world && !PocketSpaceManager.canChangeBlock(world, pos)) {
			// 鑷剤锛氭嫆缁濆啓鍏ョ殑鍚屾椂閲嶅彂鍘熸柟鍧楃姸鎬?鈫?瀹㈡埛绔笂琛ㄧ幇涓恒€岀牬鍧忓悗绔嬪埢琛ュ洖涓€涓€?			world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), Block.UPDATE_ALL);
			cir.setReturnValue(false);
		}
	}
}