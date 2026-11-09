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
 * 蛛丝搭路工具（jackcooper）：从原?SSC {@code WebBridgeAction} 移植的搭路逻辑?
 * 仅一处行为差异——放置桥/梯块时若目标格是附属的减速蛛网膜（web_membrane），
 * 先静默拆除网膜（顺带?{@link WebMembraneOwners} 施法者记录）再放置，
 * 实现「搭路方块替代网膜」；其余放置规则（空?/ 既有桥块）与原版一致?
 *
 * <p>放置的方块沿用原?{@link RegCustomBlock#TEMP_WEB_BRIDGE}（继承其寿命 /
 * 邻居塌落 / 渲染等全部行为），故对游戏内已有蛛丝桥的存档兼容?/p>
 */
public final class AddonWebBridgeAction {

	private AddonWebBridgeAction() {}

	/** 搭路放置门槛：空?/ 既有桥块 / 附属网膜（替代） ?可放?*/
	public static boolean SetWebBlock(Level world, BlockPos pos, Block webBlock, Direction facing) {
		BlockState blockState = world.getBlockState(pos);
		if (blockState.is(AddonBlocks.WEB_MEMBRANE.get())) {
			// 网膜让位：拆掉后视作空气继续放置（removeBlock 不触发邻居连锁，后续 setBlockState 全量同步?
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
	 * 命中方块后建蛛丝天梯（从原版 BuildWebLadder 移植，放块改?{@link #SetWebBlock}）?
	 * 命中顶面 ?向上爬升；命中底??向下延伸；命中侧??贴墙向下搭。加宽档（tier2+?
	 * 沿途随机补侧面邻格?
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
	 * 脚下平铺蛛丝桥（从原?BuildWebBridge 移植，放块改?{@link #SetWebBlock}）?
	 * ?pos 为起点沿 direction ?length 长、两侧各 width 格的桥面?
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

	/** 天梯配置（对应原?WebLadderConfig record）?*/
	public record WebLadderConfig(int SideBlockNum, int BottomBlockNum, int TopBlockNum,
								  boolean LargerLadder, float LargerLadderCountPercent) {}

	/** 桥面配置（对应原?WebBridgeConfig record）?*/
	public record WebBridgeConfig(int Length, int Width) {}
}
