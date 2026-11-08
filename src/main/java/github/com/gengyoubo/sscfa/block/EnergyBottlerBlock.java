package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.init.AddonBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 能量装瓶器方块（jackcooper）：类炼药台的三线并行装瓶机。
 * <p>右键打开界面（3 空瓶输入槽 + 3 能量瓶输出槽 + 能量条 + 手动/自动按钮）；逻辑由
 * {@link EnergyBottlerBlockEntity#tick} 驱动。从相邻能量网络抽能量，破坏时掉落槽内物品。
 */
@SuppressWarnings("deprecation") // 覆写 vanilla @Deprecated 的 Block 交互/状态替换/旋转镜像方法，统一抑制
public class EnergyBottlerBlock extends BaseEntityBlock {

	/** 开口所朝的水平方向（同汲取器：放置时开口朝向放置者）。 */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public EnergyBottlerBlock(Properties settings) {
		super(settings);
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// 开口朝向放置者（同汲取器）：玩家水平朝向的反向
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnergyBottlerBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		return world.isClientSide ? null
				: createTickerHelper(type, AddonBlockEntities.ENERGY_BOTTLER.get(), EnergyBottlerBlockEntity::tick);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!world.isClientSide) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof EnergyBottlerBlockEntity bottler) {
				player.openMenu(bottler);
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof EnergyBottlerBlockEntity bottler) {
				dropInventory(world, pos, bottler);
			}
			super.onRemove(state, world, pos, newState, moved);
		}
	}

	/** 破坏时把槽内物品在方块中心掉落。 */
	private static void dropInventory(Level world, BlockPos pos, EnergyBottlerBlockEntity be) {
		for (int i = 0; i < be.getContainerSize(); i++) {
			ItemStack stack = be.getItem(i);
			if (!stack.isEmpty()) {
				world.addFreshEntity(new ItemEntity(world,
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
			}
		}
		be.clearContent();
	}
}
