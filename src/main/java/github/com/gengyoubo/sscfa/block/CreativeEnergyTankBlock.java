package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 创造能量储罐方块（jackcooper）：仅创造模式可获取的无限能量源。
 * <p>放置后作为能量网络成员接入相邻网络（继承储罐的放置/破坏拓扑广播），
 * 每 20 tick 把网络内所有储罐补满（见 {@link CreativeEnergyTankBlockEntity#tick}）。
 * 自身不参与能量统计（贡献 0/0），避免污染网络能量显示。
 * 物品带附魔光效（见 RegAddonBlocks 注册处的 glint BlockItem）。
 */
public class CreativeEnergyTankBlock extends EnergyStorageTankBlock {

	public CreativeEnergyTankBlock(Properties settings) {
		super(settings);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CreativeEnergyTankBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		// 仅服务端 tick（补满判定服务端权威）
		return world.isClientSide ? null
				: createTickerHelper(type, AddonBlockEntities.CREATIVE_ENERGY_TANK.get(), CreativeEnergyTankBlockEntity::tick);
	}

	/** 右键动作栏显示网络能量（同储罐交互习惯；BE 类型不同故在此覆写）。 */
	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!world.isClientSide) {
			IEnergyNetwork net = EnergyNetwork.of(EnergyNetworkTopology.collect(world, pos));
			player.displayClientMessage(Component.translatable("message.ssc_addon.energy_network.status",
					net.getStoredEnergy(), net.getCapacity()), true);
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
