package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.init.AddonBlocks;
import github.com.gengyoubo.sscfa.block.WebMembraneOwners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import github.com.gengyoubo.sscfa.block.TempWebBridgeBlock;

/**
 * 铔涗笣鎼矾宸ュ叿锛坖ackcooper锛夛細浠庡師鐗?SSC {@code WebBridgeAction} 绉绘鐨勬惌璺€昏緫锛?
 * 浠呬竴澶勮涓哄樊寮傗€斺€旀斁缃ˉ/姊潡鏃惰嫢鐩爣鏍兼槸闄勫睘鐨勫噺閫熻洓缃戣啘锛坵eb_membrane锛夛紝
 * 鍏堥潤榛樻媶闄ょ綉鑶滐紙椤哄甫娓?{@link WebMembraneOwners} 鏂芥硶鑰呰褰曪級鍐嶆斁缃紝
 * 瀹炵幇銆屾惌璺柟鍧楁浛浠ｇ綉鑶溿€嶏紱鍏朵綑鏀剧疆瑙勫垯锛堢┖姘?/ 鏃㈡湁妗ュ潡锛変笌鍘熺増涓€鑷淬€?
 *
 * <p>鏀剧疆鐨勬柟鍧楁部鐢ㄥ師鐗?{@link RegCustomBlock#TEMP_WEB_BRIDGE}锛堢户鎵垮叾瀵垮懡 /
 * 閭诲眳濉岃惤 / 娓叉煋绛夊叏閮ㄨ涓猴級锛屾晠瀵规父鎴忓唴宸叉湁铔涗笣妗ョ殑瀛樻。鍏煎銆?/p>
 */
public final class AddonWebBridgeAction {

	private AddonWebBridgeAction() {}

	/** 鎼矾鏀剧疆闂ㄦ锛氱┖姘?/ 鏃㈡湁妗ュ潡 / 闄勫睘缃戣啘锛堟浛浠ｏ級 鈫?鍙斁銆?*/
	public static boolean SetWebBlock(Level world, BlockPos pos, Block webBlock, Direction facing) {
		BlockState blockState = world.getBlockState(pos);
		if (blockState.is(AddonBlocks.WEB_MEMBRANE.get())) {
			// 缃戣啘璁╀綅锛氭媶鎺夊悗瑙嗕綔绌烘皵缁х画鏀剧疆锛坮emoveBlock 涓嶈Е鍙戦偦灞呰繛閿侊紝鍚庣画 setBlockState 鍏ㄩ噺鍚屾锛?
			world.removeBlock(pos, false);
			WebMembraneOwners.remove(world, pos);
			blockState = world.getBlockState(pos);
		}
		if (blockState.isAir() || blockState.is(webBlock)) {
			BlockState state = webBlock.defaultBlockState().setValue(TempWebBridgeBlock.HORIZONTAL_FACING, facing);
			world.setBlockAndUpdate(pos, state);
			return true;
		}
		return false;
	}

	/**
	 * 鍛戒腑鏂瑰潡鍚庡缓铔涗笣澶╂锛堜粠鍘熺増 BuildWebLadder 绉绘锛屾斁鍧楁敼璧?{@link #SetWebBlock}锛夈€?
	 * 鍛戒腑椤堕潰 鈫?鍚戜笂鐖崌锛涘懡涓簳闈?鈫?鍚戜笅寤朵几锛涘懡涓晶闈?鈫?璐村鍚戜笅鎼€傚姞瀹芥。锛坱ier2+锛?
	 * 娌块€旈殢鏈鸿ˉ渚ч潰閭绘牸銆?
	 */
	public static void BuildWebLadder(Level world, BlockHitResult blockHitResult, WebLadderConfig config, Block ladderBlock) {
		BlockPos pos = blockHitResult.getBlockPos();
		Direction direction = blockHitResult.getDirection();

		Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		RandomSource random = world.getRandom();

		BlockPos nowPos;
		Direction ladderDirection;
		int length;
		switch (direction) {
			case UP -> {
				nowPos = pos.above();
				ladderDirection = Direction.UP;
				length = config.TopBlockNum;
			}
			case DOWN -> {
				nowPos = pos.below();
				ladderDirection = Direction.DOWN;
				length = config.BottomBlockNum;
			}
			default -> {
				nowPos = pos.relative(direction);
				ladderDirection = Direction.DOWN;
				length = config.SideBlockNum;
			}
		}

		int largerLadderCount = (int) (config.LargerLadderCountPercent * length);

		for (int i = 0; i < length; i++) {
			Direction randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
			if (!SetWebBlock(world, nowPos, ladderBlock, randomFacing)) {
				break;
			}
			if (config.LargerLadder && largerLadderCount > 0) {
				randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, nowPos.east(), ladderBlock, randomFacing);
				randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, nowPos.west(), ladderBlock, randomFacing);
				randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, nowPos.north(), ladderBlock, randomFacing);
				randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, nowPos.south(), ladderBlock, randomFacing);
				largerLadderCount--;
			}
			nowPos = nowPos.relative(ladderDirection);
		}
	}

	/**
	 * 鑴氫笅骞抽摵铔涗笣妗ワ紙浠庡師鐗?BuildWebBridge 绉绘锛屾斁鍧楁敼璧?{@link #SetWebBlock}锛夈€?
	 * 浠?pos 涓鸿捣鐐规部 direction 閾?length 闀裤€佷袱渚у悇 width 鏍肩殑妗ラ潰銆?
	 */
	public static void BuildWebBridge(Level world, BlockPos pos, Direction direction, WebBridgeConfig config, Block webBlock) {
		if (direction == Direction.UP || direction == Direction.DOWN) {
			return;
		}
		Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		RandomSource random = world.getRandom();
		BlockPos nowPos = pos;
		for (int k = -config.Width; k <= config.Width; k++) {
			for (int m = -config.Width; m <= config.Width; m++) {
				Direction randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, pos.offset(k, 0, m), webBlock, randomFacing);
			}
		}
		for (int i = 0; i < config.Length; i++) {
			Direction randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
			SetWebBlock(world, nowPos, webBlock, randomFacing);
			BlockPos tempPos = nowPos;
			Direction tempDirection = direction.getClockWise();
			for (int j = 0; j < config.Width; j++) {
				tempPos = tempPos.relative(tempDirection);
				randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, tempPos, webBlock, randomFacing);
			}
			tempPos = nowPos;
			tempDirection = direction.getCounterClockWise();
			for (int j = 0; j < config.Width; j++) {
				tempPos = tempPos.relative(tempDirection);
				randomFacing = horizontalDirections[random.nextInt(horizontalDirections.length)];
				SetWebBlock(world, tempPos, webBlock, randomFacing);
			}
			nowPos = nowPos.relative(direction);
		}
	}

	/** 澶╂閰嶇疆锛堝搴斿師鐗?WebLadderConfig record锛夈€?*/
	public record WebLadderConfig(int SideBlockNum, int BottomBlockNum, int TopBlockNum,
								  boolean LargerLadder, float LargerLadderCountPercent) {}

	/** 妗ラ潰閰嶇疆锛堝搴斿師鐗?WebBridgeConfig record锛夈€?*/
	public record WebBridgeConfig(int Length, int Width) {}
}
