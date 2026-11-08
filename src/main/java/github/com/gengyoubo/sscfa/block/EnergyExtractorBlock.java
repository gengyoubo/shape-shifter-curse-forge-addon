package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;

/**
 * 能量汲取器方块（jackcooper）：单格空心舱，一面开口。
 * <p>开口朝向放置者（{@link #FACING} = 开口所朝方向）。碰撞体为「底板 + 后壁 + 左右壁」，
 * 前面与顶部开口，使魔系玩家可从开口走进本格站立（上半身从顶部露出）。右键显示网络能量。
 * 魔力转化由 {@link EnergyExtractorBlockEntity#tick} 驱动。
 */
@SuppressWarnings("deprecation") // 覆写多个 vanilla @Deprecated 的 Block 方法（形状/交互/状态替换/旋转镜像），统一抑制
public class EnergyExtractorBlock extends BaseEntityBlock {

	/** 开口所朝的水平方向。 */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** 开口朝北（-Z）时的基准舱壳形状：底板 + 后壁(南) + 左壁(西) + 右壁(东)，前(北)与顶开口。 */
	private static final VoxelShape SHAPE_NORTH = Shapes.or(
			Block.box(0, 0, 0, 16, 2, 16),   // 底板
			Block.box(0, 2, 14, 16, 14, 16), // 后壁（南 +Z）
			Block.box(0, 2, 0, 2, 14, 16),   // 左壁（西 -X）
			Block.box(14, 2, 0, 16, 14, 16)  // 右壁（东 +X）
	);

	/** 各朝向的舱壳形状（由基准形状绕 Y 旋转生成）。 */
	private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			SHAPES.put(dir, rotateShape(Direction.NORTH, dir, SHAPE_NORTH));
		}
	}

	public EnergyExtractorBlock(Properties settings) {
		super(settings);
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	/** 绕 Y 轴把 VoxelShape 从 from 旋转到 to（社区通用实现）。 */
	private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
		VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
		int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
		for (int i = 0; i < times; i++) {
			buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
					buffer[1] = Shapes.or(buffer[1],
							Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
			buffer[0] = buffer[1];
			buffer[1] = Shapes.empty();
		}
		return buffer[0];
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// 开口朝向放置者：玩家水平朝向的反向（放置后开口正对玩家，方便直接走进）
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnergyExtractorBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		// 仅服务端 tick（转化与合成均为服务端权威）
		return world.isClientSide ? null
				: createTickerHelper(type, AddonBlockEntities.ENERGY_EXTRACTOR.get(), EnergyExtractorBlockEntity::tick);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!world.isClientSide) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof EnergyExtractorBlockEntity ext) {
				// 右键在动作栏显示所在网络的能量总量（汲取器自身无 GUI）
				IEnergyNetwork net = ext.getNetwork();
				player.displayClientMessage(Component.translatable("message.ssc_addon.energy_network.status",
						net.getStoredEnergy(), net.getCapacity()), true);
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onPlace(state, world, pos, oldState, notify);
		// 放置后广播刷新所在能量网络拓扑缓存（事件驱动，非高频扫描）
		if (!world.isClientSide && !state.is(oldState.getBlock())) {
			EnergyNetworkTopology.broadcastInvalidate(world, pos);
		}
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			super.onRemove(state, world, pos, newState, moved);
			// 破坏后广播刷新网络（此时本方块实体已移除，从邻接洪泛遍历其余成员）
			if (!world.isClientSide) {
				EnergyNetworkTopology.broadcastInvalidate(world, pos);
			}
		}
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
