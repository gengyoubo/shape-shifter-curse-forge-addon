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
 * 药品存储箱方块（jackcooper）：专存压缩能量药水（feed_potion），支持漏斗互通。
 * <p>右键打开界面（8 个存储槽）；破坏时掉落槽内物品。
 * <p>储药柜造型（门面朝 {@link #FACING}），放置时门面朝向放置者（同汲取器/装瓶器的开口朝向逻辑）。
 */
@SuppressWarnings("deprecation") // 覆写 vanilla @Deprecated 的 Block 交互/状态替换/旋转镜像方法，统一抑制
public class PotionStorageBoxBlock extends BaseEntityBlock {

	/** 门面所朝的水平方向。 */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public PotionStorageBoxBlock(Properties settings) {
		super(settings);
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// 门面朝向放置者：玩家水平朝向的反向（放置后门正对玩家）
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
		return new PotionStorageBoxBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		// 仅服务端 tick（自动合并同类药水）
		return world.isClientSide ? null
				: createTickerHelper(type, AddonBlockEntities.POTION_STORAGE_BOX.get(), PotionStorageBoxBlockEntity::tick);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!world.isClientSide) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof PotionStorageBoxBlockEntity box) {
				player.openMenu(box);
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof PotionStorageBoxBlockEntity box) {
				dropInventory(world, pos, box);
			}
			super.onRemove(state, world, pos, newState, moved);
		}
	}

	/** 破坏时把槽内物品在方块中心掉落。 */
	private static void dropInventory(Level world, BlockPos pos, PotionStorageBoxBlockEntity be) {
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
