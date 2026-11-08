package github.com.gengyoubo.sscfa.energy;

import java.util.List;
import java.util.function.ToIntFunction;

/** 一组导通节点的快照；能量由储能节点持有。 */
public final class EnergyNetwork implements IEnergyNetwork {
    private final List<IEnergyNode> nodes;

    private EnergyNetwork(List<IEnergyNode> nodes) { this.nodes = List.copyOf(nodes); }

    public static EnergyNetwork of(List<IEnergyNode> nodes) { return new EnergyNetwork(nodes); }

    @Override
    public List<IEnergyNode> nodes() { return nodes; }

	/** 网络当前总能量。 */
	@Override
	public int getStoredEnergy() {
		return sumStorage(IEnergyStorageNode::getStoredEnergy);
	}

	/** 网络总能量上限。 */
	@Override
	public int getCapacity() {
		return sumStorage(IEnergyStorageNode::getEnergyCapacity);
	}

	/** 向网络注入 {@code amount} 点能量（按成员顺序填满），返回实际接受量。 */
	@Override
	public int insert(int amount) {
		return transfer(amount, true);
	}

	/** 从网络抽取 {@code amount} 点能量（按成员顺序排空），返回实际抽取量。 */
	@Override
	public int extract(int amount) {
		return transfer(amount, false);
	}

	private int sumStorage(ToIntFunction<IEnergyStorageNode> value) {
		int sum = 0;
		for (IEnergyNode node : nodes) {
			if (node instanceof IEnergyStorageNode storage) sum += value.applyAsInt(storage);
		}
		return sum;
	}

	/** inserting 为 true 时存入能量，否则取出能量。 */
	private int transfer(int amount, boolean inserting) {
		if (amount <= 0) return 0;
		int remaining = amount;
		for (IEnergyNode node : nodes) {
			if (remaining == 0) break;
			if (node instanceof IEnergyStorageNode storage) {
				remaining -= transferStorage(storage, remaining, inserting);
			}
		}
		if (remaining != amount) {
			TankEnergy.equalizeTanks(nodes);
			TankEnergy.refreshTankDisplays(nodes);
		}
		return amount - remaining;
	}

	/** 存入受剩余容量限制，取出受当前储量限制。 */
	private static int transferStorage(IEnergyStorageNode storage, int amount, boolean inserting) {
		int stored = storage.getStoredEnergy();
		int available = inserting ? storage.getEnergyCapacity() - stored : stored;
		int transferred = Math.min(amount, Math.max(0, available));
		if (transferred > 0) storage.setStoredEnergy(inserting ? stored + transferred : stored - transferred);
		return transferred;
	}

}
