package github.com.gengyoubo.sscfa.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** 临时蛛网桥方块（Forge 简化）：保留水平朝向状态供搭路逻辑使用。 */
public class TempWebBridgeBlock extends Block {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TempWebBridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }
}
