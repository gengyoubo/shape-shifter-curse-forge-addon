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

/** 仅保护口袋维度外壳和出口凹槽；内部搭建与其他维度不受影响。拒绝时重发方块更新实现「破坏后立即补回」。 */
@Mixin(Level.class)
public abstract class PocketSpaceBoundaryMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At("HEAD"), cancellable = true)
	private void sscAddon$protectPocketBoundary(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerLevel world && !PocketSpaceManager.canChangeBlock(world, pos)) {
			// 自愈：拒绝写入的同时重发原方块状态 → 客户端上表现为「破坏后立刻补回一个」
			world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), Block.UPDATE_ALL);
			cir.setReturnValue(false);
		}
	}
}
