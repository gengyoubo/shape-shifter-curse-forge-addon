package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;


/**
 * 能量转变器：将内部能量单向转换为 Forge Energy。
 * <p>右键切换 {@link #ACTIVE} 激活态（激活时微发光，方块状态持久化）；激活后由
 * {@link EnergyConverterBlockEntity#tick} 驱动：从相邻能量网络抽 SSCA 能量，
 * 按 1:16 转成 FE 并向相邻的能量接收方输出。
 * <p>右键除切换激活外，同时动作栏显示所在网络能量（同储罐/汲取器交互习惯）。
 */
@SuppressWarnings("deprecation") // 覆写 vanilla @Deprecated 的 Block 交互/状态方法，统一抑制
public class EnergyConverterBlock extends BaseEntityBlock {

	/** 是否激活（激活后才开始转换输出）。 */
	public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

	public EnergyConverterBlock(Properties settings) {
		super(settings);
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/** 激活时微发光（核心运转的视觉反馈，luminance 注册于 RegAddonBlocks 的 Settings）。 */
	public static int luminanceOf(BlockState state) {
		return state.getValue(ACTIVE) ? 7 : 0;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnergyConverterBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		// 仅服务端 tick（能量转换判定与推送全部服务端权威）
		return world.isClientSide ? null
				: createTickerHelper(type, AddonBlockEntities.ENERGY_CONVERTER.get(), EnergyConverterBlockEntity::tick);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!world.isClientSide) {
			boolean active = !state.getValue(ACTIVE);
			world.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
			world.playSound(null, pos,
					active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
					SoundSource.BLOCKS, 0.6f, active ? 1.4f : 1.0f);
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof EnergyConverterBlockEntity conv) {
				// 无缓冲实时结算架构：关闭即断供（extract 守卫激活态），无需清退动作
				IEnergyNetwork net = conv.getEnergyNetwork();
				player.displayClientMessage(Component.translatable(
						active ? "message.ssc_addon.energy_converter.on" : "message.ssc_addon.energy_converter.off",
						net.getStoredEnergy(), net.getCapacity()), true);
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	/** 邻居方块更新：激活态下网络/邻接变化时唤醒（转换 tick 自轮询，这里无需额外处理）。 */
	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
		super.neighborChanged(state, world, pos, block, fromPos, notify);
		if (!world.isClientSide && state.getValue(ACTIVE)) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof EnergyConverterBlockEntity conv) {
				conv.markNetworkDirty();
			}
		}
	}

	/** 破坏：无缓冲实时结算架构下无残留能量可退，直接移除。 */
	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		super.onRemove(state, world, pos, newState, moved);
	}
}
