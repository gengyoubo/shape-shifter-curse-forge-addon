package github.com.gengyoubo.sscfa.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
 * 濞夈劑鐡熼崣甯礄jackcooper閿涘鈧倸褰搁柨顔藉ⅵ瀵偓閻ｅ矂娼伴敍灞界窔娑旓附蝎閺€鐐箑鐏忔﹢鐡熷▔鏇氬姛 + 閻曞啯鏋″Σ鑺ユ杹閺堝牆鐨圭划?缁绢垱娅犻崗鍛《閸旀冻绱?
 * 鏉堝墽绮℃宀勬閸婄厧鎮楅柊宥呮値婵夋垵鑸伴弽绋跨妇閸楀洨楠囨鏃€纭舵稊锔衡偓鍌滅壃閸у繑妞傞弫锝堟儰閸愬懎鎯堥悧鈺佹惂閵?
 * <p>1.20.1 娑?onUse/onStateReplaced/rotate/mirror 閻ㄥ嫯顩崘娆忓涧鐎涙ê婀禍?deprecated 閻栧墎琚弬瑙勭《閿?
 * 鏉╂瑦妲搁崢鐔哄 BlockWithEntity 閻ㄥ嫭鐖ｉ崙鍡楀晸濞夋洩绱滻DE 鐠€锕€鎲＄仦鐐额嚖閹躲儻绱濈猾鑽ら獓閹舵垵鍩楅妴?/p>
 */
@SuppressWarnings("deprecation")
public class InfusionAltarBlock extends BaseEntityBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public InfusionAltarBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// 濮濓綁娼伴張婵嗘倻閺€鍓х枂閼板拑绱欐稉搴″斧閻楀牐顔夐崣棰佺閼疯揪绱?
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new InfusionAltarBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (world.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		MenuProvider factory = state.getMenuProvider(world, pos);
		if (factory != null) {
			player.openMenu(factory);
		}
		return InteractionResult.CONSUME;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		if (world.isClientSide) {
			return null;
		}
		return createTickerHelper(type, RegAddonBlockEntities.INFUSION_ALTAR_BE, InfusionAltarBlockEntity::tick);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof InfusionAltarBlockEntity altar) {
				net.minecraft.world.Containers.dropContents(world, pos, altar.getItems());
				world.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, world, pos, newState, moved);
		}
	}
}
