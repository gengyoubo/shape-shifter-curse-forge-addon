package github.com.gengyoubo.sscfa.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 迁移占位：带水平朝向的功能方块（注入祭坛、法术研究台）。保留注册 ID、方块状态（facing）、放置朝向与碰撞形状，
 * 让方块状态、模型、掉落表可以正常加载；界面与方块实体待法术系统迁移后接回。
 */
public class FacingShellBlock extends HorizontalDirectionalBlock {
	private final Map<Direction, VoxelShape> shapes;

	public FacingShellBlock(Properties properties, @Nullable VoxelShape northShape) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
		shapes = northShape == null ? null : rotations(northShape);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 正面朝向放置者（与原版讲台一致）
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes == null ? Shapes.block() : shapes.get(state.getValue(FACING));
	}

	private static Map<Direction, VoxelShape> rotations(VoxelShape north) {
		Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
		VoxelShape current = north;
		for (Direction dir : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			map.put(dir, current);
			current = rotateClockwise(current);
		}
		return map;
	}

	/** 绕 Y 轴顺时针转 90°（北 → 东）。 */
	private static VoxelShape rotateClockwise(VoxelShape shape) {
		VoxelShape[] result = {Shapes.empty()};
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
				result[0] = Shapes.or(result[0], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
		return result[0];
	}
}
