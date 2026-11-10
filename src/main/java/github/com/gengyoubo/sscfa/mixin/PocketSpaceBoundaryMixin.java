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

/** 娴犲懍绻氶幎銈呭經鐞氬娣惔锕€顦绘竟鍐叉嫲閸戝搫褰涢崙瑙勑敍娑樺敶闁劍鎯屽杞扮瑢閸忔湹绮紒鏉戝娑撳秴褰堣ぐ鍗炴惙閵嗗倹瀚嗙紒婵囨闁插秴褰傞弬鐟版健閺囧瓨鏌婄€圭偟骞囬妴宀€鐗崸蹇撴倵缁斿宓嗙悰銉ユ礀閵?*/
@Mixin(Level.class)
public abstract class PocketSpaceBoundaryMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
			at = @At("HEAD"), cancellable = true)
	private void sscAddon$protectPocketBoundary(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerLevel world && !PocketSpaceManager.canChangeBlock(world, pos)) {
			// 閼奉亝鍓ら敍姘珕缂佹繂鍟撻崗銉ф畱閸氬本妞傞柌宥呭絺閸樼喐鏌熼崸妤冨Ц??鐎广垺鍩涚粩顖欑瑐鐞涖劎骞囨稉鎭掆偓宀€鐗崸蹇撴倵缁斿鍩㈢悰銉ユ礀娑撯偓娑?			world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), Block.UPDATE_ALL);
			cir.setReturnValue(false);
		}
	}
}