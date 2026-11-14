package github.com.gengyoubo.sscfa.block;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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

/**
 * 濞夋洘婀抽惍鏃傗敀閸欏府绱檍ackcooper閿涘鈧倸褰搁柨顔藉ⅵ瀵偓閸欏矂銆夌粵鍓ф櫕闂堫澁绱?
 * 閵嗗本纭堕梼鍨Ρ閸愭瑣鈧稄绱欑粚铏规濞夋洟妯€缁?濞岀懓鈪?閳?閹跺嫬鍟撳鎻掝劅娑旂姵纭堕梼纰夌礆娑撳簺鈧本纭堕張顖氼劅娑旂姰鈧稄绱欓張鍫濈毠 閳?鐎涳缚绡勫鑼额唶瑜版洘纭堕梼纰夌礆閵?
 * 閻潙娼栭弮鑸垫殠閽€钘夊敶閸氼偆澧块崫浣碘偓?
 * <p>1.20.1 娑?onUse/onStateReplaced/rotate/mirror 閻ㄥ嫯顩崘娆忓涧鐎涙ê婀禍?deprecated 閻栧墎琚弬瑙勭《閿?
 * 鏉╂瑦妲搁崢鐔哄 BlockWithEntity 閻ㄥ嫭鐖ｉ崙鍡楀晸濞夋洩绱滻DE 鐠€锕€鎲＄仦鐐额嚖閹躲儻绱濈猾鑽ら獓閹舵垵鍩楅妴?/p>
 */
@SuppressWarnings("deprecation")
public class SpellResearchTableBlock extends BaseEntityBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** 閺堟繂瀵抽敍鍦楢CING=north閿涘鐔€閸戝棛顫幘鐐殿唸閿涙艾娲撶憴鎺擃攽閼?0~10) + 濡楀矂娼?10~12) + 閼冲本婢?12~16閿涘本膩閸ㄥ顫嬬憴澶愮彯 18 鐡掑懎鍤弬鐟版健娑撳﹦鏅敍宀€顫幘鐐村焻閸?16)閵?
	 * 濡楀奔绗傛晶銊︽寜濮?缂囪姤鐦虹粭?娑旓附婀?缁剧绱剁粵澶庢澖鐏忓繗顥婃棰佺瑝鏉╂稓顫幘鐑囩礉闁灝鍘ら幐鈩冨閵?*/
	private static final VoxelShape SHAPE_NORTH = Shapes.or(
			Block.box(0, 0, 0, 2, 10, 2),     // 濡楀矁鍚欒矾鐟楀灝瀵?
			Block.box(14, 0, 0, 16, 10, 2),   // 濡楀矁鍚欒矾娑撴粌瀵?
			Block.box(14, 0, 14, 16, 10, 16), // 濡楀矁鍚欒矾娑撴粌宕?
			Block.box(0, 0, 14, 2, 10, 16),   // 濡楀矁鍚欒矾鐟楀灝宕?
			Block.box(0, 10, 0, 16, 12, 16),  // 濡楀矂娼?
			Block.box(0, 12, 0, 16, 16, 1)    // 閼冲本婢橀敍鍫濆娓氀嶇礆
	);

	/** 閸氬嫭婀為崥鎴濊埌閻樿绱欐稉?blockstate 閻?y 閺冨娴嗙痪锕€鐣炬稉鈧懛杈剧礉缂?Y 鏉炲鏁遍崺鍝勫櫙瑜般垻濮搁悽鐔稿灇閿涘鈧?*/
	private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			SHAPES.put(dir, rotateShape(Direction.NORTH, dir, SHAPE_NORTH));
		}
	}

	public SpellResearchTableBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// 閼冲本婢橀張婵嗘倻閻溾晛顔嶇€靛綊娼伴敍娆禔CING 閻╁瓨甯撮崣鏍负鐎硅埖鎸夐獮铏篂閸氭埊绱欏宀勬桨/閺傛粓娼版稊锔筋劀鐎佃鏂佺純顔库偓鍜冪礆
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	/** 缂?Y 鏉炲瓨濡?VoxelShape 娴?from 閺冨娴嗛崚?to閿涘苯宕熷▎鈩冩鏉烆兛绗?blockstate "y": 90 缁涘鐜敍鍫仦閸栨椽鈧氨鏁ょ€圭偟骞囬敍灞芥倱 EnergyExtractorBlock閿涘鈧?*/
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
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
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
		return new SpellResearchTableBlockEntity(pos, state);
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
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
				github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent.sync(serverPlayer);
			player.openMenu(factory);
		}
		return InteractionResult.CONSUME;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		if (world.isClientSide || !type.equals(RegAddonBlockEntities.SPELL_RESEARCH_TABLE_BE)) return null;
		return (serverWorld, pos, blockState, entity) -> {
					if (entity instanceof SpellResearchTableBlockEntity table && (!table.getItem(SpellResearchTableBlockEntity.SLOT_ANALYSIS).isEmpty() || table.getAnalysisTicks() > 0))
						table.advanceAnalysis(github.com.gengyoubo.sscfa.spell.research.WorldRuneState.get(serverWorld.getServer()));
				};
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof SpellResearchTableBlockEntity table) {
				net.minecraft.world.Containers.dropContents(world, pos, table.getItems());
				world.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, world, pos, newState, moved);
		}
	}
}
