package github.com.gengyoubo.sscfa.spell.pocket;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 口袋空间功能方块（Forge 版，DeferredRegister 注册，无 BlockItem）。 */
public final class PocketSpaceBlocks {
	public static final DirectionProperty PORTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "ssc_addon");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ssc_addon");

	public static final Block FLOOR = new FloorBlock();
	public static final Block BEDROCK_WALL = new InvisibleBedrockBlock();
	public static final Block PORTAL = new PortalBlock();

	public static final RegistryObject<BlockEntityType<VoidWallBlockEntity>> VOID_WALL_TYPE =
			BLOCK_ENTITIES.register("pocket_space_bedrock_wall",
					() -> BlockEntityType.Builder.of(VoidWallBlockEntity::new, BEDROCK_WALL).build(null));
	public static final RegistryObject<BlockEntityType<PortalBlockEntity>> PORTAL_TYPE =
			BLOCK_ENTITIES.register("pocket_space_portal",
					() -> BlockEntityType.Builder.of(PortalBlockEntity::new, PORTAL).build(null));

	static {
		BLOCKS.register("pocket_space_floor", () -> FLOOR);
		BLOCKS.register("pocket_space_bedrock_wall", () -> BEDROCK_WALL);
		BLOCKS.register("pocket_space_portal", () -> PORTAL);
	}

	private PocketSpaceBlocks() {}

	public static void init() {}

	public static BlockState portalState(PocketSpaceLayout layout, int blockX, int blockZ) {
		Direction facing = switch (layout.portalRotation(blockX, blockZ)) {
			case 90 -> Direction.EAST;
			case 180 -> Direction.SOUTH;
			case 270 -> Direction.WEST;
			default -> Direction.NORTH;
		};
		return PORTAL.defaultBlockState().setValue(PORTAL_FACING, facing);
	}

	private static final class FloorBlock extends Block implements GameMasterBlock {
		private FloorBlock() {
			super(BlockBehaviour.Properties.copy(Blocks.SMOOTH_QUARTZ)
					.strength(-1.0f, 3_600_000.0f).noLootTable().pushReaction(PushReaction.BLOCK));
		}
	}

	public static final class InvisibleBedrockBlock extends BaseEntityBlock implements GameMasterBlock {
		private InvisibleBedrockBlock() {
			super(BlockBehaviour.Properties.copy(Blocks.BEDROCK)
					.strength(-1.0f, 3_600_000.0f).noLootTable().pushReaction(PushReaction.BLOCK)
					.noOcclusion());
		}
		@Override
		public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
		@Override
		public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) { return true; }
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VoidWallBlockEntity(pos, state); }
	}

	public static final class VoidWallBlockEntity extends BlockEntity {
		public VoidWallBlockEntity(BlockPos pos, BlockState state) { super(VOID_WALL_TYPE.get(), pos, state); }
	}

	public static final class PortalBlockEntity extends BlockEntity {
		public PortalBlockEntity(BlockPos pos, BlockState state) { super(PORTAL_TYPE.get(), pos, state); }
	}

	private static final class PortalBlock extends BaseEntityBlock implements GameMasterBlock {
		private static final VoxelShape BASE = Shapes.box(0, 0, 0, 1, PocketSpaceLayout.PORTAL_BASE_HEIGHT, 1);
		private static final double INSET = PocketSpaceLayout.PORTAL_INSET;
		private static final double BASE_HEIGHT = PocketSpaceLayout.PORTAL_BASE_HEIGHT;
		private static final double TOP_HEIGHT = PocketSpaceLayout.PORTAL_TOP_HEIGHT;
		private static final VoxelShape NORTH_SHAPE = Shapes.or(BASE, Shapes.box(INSET, BASE_HEIGHT, INSET, 1, TOP_HEIGHT, 1));
		private static final VoxelShape EAST_SHAPE = Shapes.or(BASE, Shapes.box(0, BASE_HEIGHT, INSET, 1 - INSET, TOP_HEIGHT, 1));
		private static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE, Shapes.box(0, BASE_HEIGHT, 0, 1 - INSET, TOP_HEIGHT, 1 - INSET));
		private static final VoxelShape WEST_SHAPE = Shapes.or(BASE, Shapes.box(INSET, BASE_HEIGHT, 0, 1, TOP_HEIGHT, 1 - INSET));

		private PortalBlock() {
			super(BlockBehaviour.Properties.copy(Blocks.SMOOTH_QUARTZ).strength(-1.0f, 3_600_000.0f)
					.noLootTable().pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(state -> 12));
			registerDefaultState(getStateDefinition().any().setValue(PORTAL_FACING, Direction.NORTH));
		}
		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PORTAL_FACING); }
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PortalBlockEntity(pos, state); }
		@Override
		public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
		@Override
		@Deprecated
		public BlockState rotate(BlockState state, Rotation rotation) {
			return state.setValue(PORTAL_FACING, rotation.rotate(state.getValue(PORTAL_FACING)));
		}
		@Override
		@Deprecated
		public BlockState mirror(BlockState state, Mirror mirror) {
			return rotate(state, mirror.getRotation(state.getValue(PORTAL_FACING)));
		}
		@Override
		@Deprecated
		public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
			return switch (state.getValue(PORTAL_FACING)) {
				case EAST -> EAST_SHAPE;
				case SOUTH -> SOUTH_SHAPE;
				case WEST -> WEST_SHAPE;
				default -> NORTH_SHAPE;
			};
		}
	}
}
