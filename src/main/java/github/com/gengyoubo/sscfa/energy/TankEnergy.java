package github.com.gengyoubo.sscfa.energy;

import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/** 储罐的能量均分、显示同步和破坏后的能量转移。 */
public final class TankEnergy {
    private TankEnergy() {}

	/**
	 * 事件驱动刷新网络内所有能量储罐的液面显示（仅服务端生效）。
	 * 所有储罐统一显示所在网络的总能量/总容量比例；比例变化超过阈值时经 BE 数据包同步客户端
	 * （见 {@link EnergyStorageTankBlockEntity#syncFillRatio}，BER 无级液面渲染）。
	 */
	public static void refreshTankDisplays(List<IEnergyNode> members) {
		if (members == null || members.isEmpty()) {
			return;
		}
		IEnergyNetwork network = EnergyNetwork.of(members);
		int cap = network.getCapacity();
		float ratio = cap > 0 ? (float) network.getStoredEnergy() / cap : 0f;
		for (IEnergyNode m : members) {
			if (!(m instanceof EnergyStorageTankBlockEntity tank)) {
				continue;
			}
			Level w = tank.getLevel();
			if (w == null || w.isClientSide) {
				continue;
			}
			tank.syncFillRatio(ratio);
		}
	}

	/** 收集成员列表中的全部储罐（汲取器缓冲不参与均分）。 */
	private static List<EnergyStorageTankBlockEntity> collectTanks(List<IEnergyNode> members) {
		List<EnergyStorageTankBlockEntity> tanks = new ArrayList<>();
		for (IEnergyNode m : members) {
			if (m instanceof EnergyStorageTankBlockEntity tank && tank.getLevel() != null && !tank.getLevel().isClientSide) {
				tanks.add(tank);
			}
		}
		return tanks;
	}

	/**
	 * 把网络内所有储罐的能量均分（仅服务端）。
	 * 总量取各储罐之和，按「每罐 total/n、前 rem 罐 +1」分配，结果相等或最多相差 1。
	 */
	public static void equalizeTanks(List<IEnergyNode> members) {
		equalizeTanksWithBonus(members, 0);
	}

	/** 被破坏储罐的能量回归网络：并入储罐总量后整体均分（须存在至少一个储罐，否则能量丢失）。 */
	public static void donateBrokenTankEnergy(List<IEnergyNode> members, int amount) {
		equalizeTanksWithBonus(members, amount);
	}

	/**
	 * 破坏储罐后调用：把 {@code amount} 能量就近交给任一相邻储罐所在网络均分并刷新显示；
	 * 周围没有任何相邻储罐时返回 false，能量按设计丢失。
	 */
	public static boolean transferBrokenTankEnergy(Level world, BlockPos origin, int amount) {
		if (amount <= 0 || world.isClientSide) {
			return false;
		}
		for (Direction dir : Direction.values()) {
			BlockEntity be = EnergyNetworkTopology.getLoadedBlockEntity(world, origin.relative(dir));
			if (be instanceof EnergyStorageTankBlockEntity tank) {
				List<IEnergyNode> net = tank.getNetwork().nodes();
				donateBrokenTankEnergy(net, amount);
				refreshTankDisplays(net);
				return true;
			}
		}
		return false;
	}

	private static void equalizeTanksWithBonus(List<IEnergyNode> members, int bonus) {
		List<EnergyStorageTankBlockEntity> tanks = collectTanks(members);
		int n = tanks.size();
		if (n == 0 || (n == 1 && bonus == 0)) {
			return;
		}
		long total = bonus;
		for (EnergyStorageTankBlockEntity t : tanks) {
			total += t.getStoredEnergy();
		}
		int per = (int) (total / n);
		int rem = (int) (total % n);
		for (int i = 0; i < n; i++) {
			EnergyStorageTankBlockEntity t = tanks.get(i);
			// 钳制到单罐上限，防止异常超总量时溢出崩溃
			int target = Math.min(t.getEnergyCapacity(), per + (i < rem ? 1 : 0));
			if (t.getStoredEnergy() != target) {
				t.setStoredEnergy(target);
			}
		}
	}
}
