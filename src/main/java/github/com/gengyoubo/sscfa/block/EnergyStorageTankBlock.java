package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.energy.TankEnergy;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 能量储罐方块（jackcooper）：正方体被动存储节点。
 * <p>相邻储罐/汲取器构成能量网络（见 {@link EnergyNetwork}），多个相邻即可叠加总上限；
 * 同一网络内的储罐能量始终自动均分（破坏储罐时其储能转移给邻近储罐后再全网均分）。
 * 右键在动作栏显示所在网络能量；放置/破坏时广播刷新网络拓扑缓存（事件驱动，非高频扫描）。
 * <p>液面显示：不再用 LEVEL 方块状态档位贴图，改由 BER 在玻璃内画无级液面
 * （见 {@code EnergyStorageTankRenderer}），比例由服务端同步（见
 * {@link EnergyStorageTankBlockEntity#syncFillRatio}）。
 */
@SuppressWarnings("deprecation") // 覆写 vanilla @Deprecated 的 Block 交互/状态替换方法，统一抑制
public class EnergyStorageTankBlock extends BaseEntityBlock {

	public EnergyStorageTankBlock(Properties settings) {
		super(settings);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnergyStorageTankBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!world.isClientSide) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof EnergyStorageTankBlockEntity tank) {
				IEnergyNetwork net = tank.getNetwork();
				player.displayClientMessage(Component.translatable("message.ssc_addon.energy_network.status",
						net.getStoredEnergy(), net.getCapacity()), true);
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onPlace(state, world, pos, oldState, notify);
		if (!world.isClientSide && !state.is(oldState.getBlock())) {
			EnergyNetworkTopology.broadcastInvalidate(world, pos);
		}
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			// 破坏前先抢救本罐储能（BE 会随 super 移除）
			BlockEntity be = world.getBlockEntity(pos);
			int rescued = (be instanceof EnergyStorageTankBlockEntity tank) ? tank.getStoredEnergy() : 0;
			super.onRemove(state, world, pos, newState, moved);
			if (!world.isClientSide) {
				EnergyNetworkTopology.broadcastInvalidate(world, pos);
				// 能量转移给邻近储罐并全网均分；周围无任何储罐则按设计丢失
				TankEnergy.transferBrokenTankEnergy(world, pos, rescued);
			}
		}
	}
}
