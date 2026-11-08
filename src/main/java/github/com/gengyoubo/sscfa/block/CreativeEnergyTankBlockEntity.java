package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.energy.TankEnergy;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.energy.IEnergyMachine;
import github.com.gengyoubo.sscfa.energy.IEnergyStorageNode;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * 创造能量储罐方块实体（jackcooper）：每 {@link #FILL_INTERVAL} tick 把所在网络内
 * 所有储罐补满（含汲取器缓冲）。自身计入网络连通（拓扑遍历用）但不参与能量统计
 * 不实现储能接口，因此不计入动作栏与装瓶器的储能统计。
 * 补满后调用 {@link EnergyNetwork#refreshTankDisplays} 刷新液面显示。
 */
public class CreativeEnergyTankBlockEntity extends BlockEntity implements IEnergyMachine {

	/** 补满节拍（tick）。 */
	private static final int FILL_INTERVAL = 20;

	public CreativeEnergyTankBlockEntity(BlockPos pos, BlockState state) {
		super(AddonBlockEntities.CREATIVE_ENERGY_TANK.get(), pos, state);
	}

	public static void tick(Level world, BlockPos pos, BlockState state, CreativeEnergyTankBlockEntity be) {
		if (world.isClientSide || world.getGameTime() % FILL_INTERVAL != 0) {
			return;
		}
		// 只遍历一次网络，对有储能能力的节点补满。
		List<IEnergyNode> net = EnergyNetworkTopology.collect(world, pos);
		boolean filled = false;
		for (IEnergyNode m : net) {
			if (m instanceof IEnergyStorageNode storage && storage.getStoredEnergy() < storage.getEnergyCapacity()) {
				storage.setStoredEnergy(storage.getEnergyCapacity());
				filled = true;
			}
		}
		if (filled) {
			// 补满后刷新全网储罐液面显示（仅服务端同步变化过的）
			TankEnergy.refreshTankDisplays(net);
		}
	}

	// ==================== 能量网络成员（统计贡献 0，仅参与连通） ====================

	@Override
	public boolean conductsEnergy() { return true; }

	@Override
	public void markNetworkDirty() {
		// 拓扑缓存由各成员自持，无需处理
	}
}
