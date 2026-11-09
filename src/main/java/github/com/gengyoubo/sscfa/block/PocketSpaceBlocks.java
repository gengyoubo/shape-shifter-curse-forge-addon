package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 口袋空间功能方块（墙壁、传送台）。与 Fabric 版一致：不可破坏（GameMasterBlock，同屏障）、没有物品形态。
 * 口袋空间的生成与传送逻辑在法术系统中，尚未迁移。
 */
public final class PocketSpaceBlocks {
	public static final DirectionProperty PORTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final double PORTAL_BASE_HEIGHT = 1.5 / 16.0;
	public static final double PORTAL_TOP_HEIGHT = 2.0 / 16.0;
	public static final double PORTAL_INSET = 2.0 / 16.0;

	private PocketSpaceBlocks() {}

	/** 墙壁/顶盖：隐形基岩，隐形渲染但保留轮廓箱，天光可穿透。 */
	public static final class InvisibleBedrockBlock extends BaseEntityBlock implements GameMasterBlock {
		public InvisibleBedrockBlock() {
			super(BlockBehaviour.Properties.copy(Blocks.BEDROCK)
					.strength(-1.0f, 3_600_000.0f).noLootTable().pushReaction(PushReaction.BLOCK).noOcclusion());
		}

		@Override
		public RenderShape getRenderShape(BlockState state) {
			return RenderShape.INVISIBLE;
		}

		@Override
		public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
			return true;
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new VoidWallBlockEntity(pos, state);
		}
	}

	/** 传送台：低矮台面，不可破坏。 */
	@SuppressWarnings("deprecation")
	public static final class PortalBlock extends BaseEntityBlock implements GameMasterBlock {
		private static final VoxelShape BASE = Shapes.box(0, 0, 0, 1, PORTAL_BASE_HEIGHT, 1);
		private static final VoxelShape NORTH_SHAPE = Shapes.or(BASE,
				Shapes.box(PORTAL_INSET, PORTAL_BASE_HEIGHT, PORTAL_INSET, 1, PORTAL_TOP_HEIGHT, 1));
		private static final VoxelShape EAST_SHAPE = Shapes.or(BASE,
				Shapes.box(0, PORTAL_BASE_HEIGHT, PORTAL_INSET, 1 - PORTAL_INSET, PORTAL_TOP_HEIGHT, 1));
		private static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE,
				Shapes.box(0, PORTAL_BASE_HEIGHT, 0, 1 - PORTAL_INSET, PORTAL_TOP_HEIGHT, 1 - PORTAL_INSET));
		private static final VoxelShape WEST_SHAPE = Shapes.or(BASE,
				Shapes.box(PORTAL_INSET, PORTAL_BASE_HEIGHT, 0, 1, PORTAL_TOP_HEIGHT, 1 - PORTAL_INSET));

		public PortalBlock() {
			super(BlockBehaviour.Properties.copy(Blocks.SMOOTH_QUARTZ).strength(-1.0f, 3_600_000.0f)
					.noLootTable().pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(state -> 12));
			registerDefaultState(getStateDefinition().any().setValue(PORTAL_FACING, Direction.NORTH));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(PORTAL_FACING);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new PortalBlockEntity(pos, state);
		}

		@Override
		public RenderShape getRenderShape(BlockState state) {
			return RenderShape.MODEL;
		}

		@Override
		public BlockState rotate(BlockState state, Rotation rotation) {
			return state.setValue(PORTAL_FACING, rotation.rotate(state.getValue(PORTAL_FACING)));
		}

		@Override
		public BlockState mirror(BlockState state, Mirror mirror) {
			return rotate(state, mirror.getRotation(state.getValue(PORTAL_FACING)));
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return switch (state.getValue(PORTAL_FACING)) {
				case EAST -> EAST_SHAPE;
				case SOUTH -> SOUTH_SHAPE;
				case WEST -> WEST_SHAPE;
				default -> NORTH_SHAPE;
			};
		}
	}

	/** 保留已注册的空方块实体以兼容旧存档。 */
	public static final class VoidWallBlockEntity extends BlockEntity {
		public VoidWallBlockEntity(BlockPos pos, BlockState state) {
			super(AddonBlockEntities.POCKET_SPACE_BEDROCK_WALL.get(), pos, state);
		}
	}

	public static final class PortalBlockEntity extends BlockEntity {
		public PortalBlockEntity(BlockPos pos, BlockState state) {
			super(AddonBlockEntities.POCKET_SPACE_PORTAL.get(), pos, state);
		}
	}
}
