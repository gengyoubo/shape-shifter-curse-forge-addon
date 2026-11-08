package github.com.gengyoubo.sscfa.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 发现已加载节点，并在网络拓扑改变时失效缓存。 */
public final class EnergyNetworkTopology {
    private EnergyNetworkTopology() {}

	/** 单个网络最多遍历的成员数上限，防止超大网络造成卡顿。 */
	public static final int MAX_NETWORK = 256;
	private static final java.util.Map<Level, Long> TOPOLOGY = new java.util.WeakHashMap<>();

	static void invalidateWorld(Level world) {
		TOPOLOGY.merge(world, 1L, Long::sum);
	}

	static void clearTopology() { TOPOLOGY.clear(); }

	/** 缓存节点坐标；重新读取时不会加载区块。 */
	public static final class Cache {
		private final java.lang.ref.WeakReference<Level> owner;
		private final long version;
		private final List<BlockPos> positions;

		public Cache(Level world, List<IEnergyNode> members) {
			owner = new java.lang.ref.WeakReference<>(world);
			version = TOPOLOGY.getOrDefault(world, 0L);
			positions = members.stream().map(member -> ((BlockEntity) member).getBlockPos().immutable()).toList();
		}

		/** 世界或拓扑已变化时返回 null，由调用方重新发现网络。 */
		public List<IEnergyNode> resolve(Level world) {
			if (owner.get() != world || version != TOPOLOGY.getOrDefault(world, 0L)) return null;
			List<IEnergyNode> members = new ArrayList<>(positions.size());
			for (BlockPos position : positions) {
				BlockEntity be = getLoadedBlockEntity(world, position);
				if (!(be instanceof IEnergyNode member) || !member.conductsEnergy() || be.isRemoved()) return null;
				members.add(member);
			}
			return members;
		}
	}

	public static BlockEntity getLoadedBlockEntity(Level world, BlockPos pos) {
		return world != null && world.hasChunkAt(pos) ? world.getBlockEntity(pos) : null;
	}

	/**
	 * 从 {@code start} 沿六个方向收集已加载的导通节点，最多收集 {@link #MAX_NETWORK} 个。
	 * 起点没有已加载的导通节点时返回空列表，不主动加载区块。
	 */
	public static List<IEnergyNode> collect(Level world, BlockPos start) {
		List<IEnergyNode> members = new ArrayList<>();
		if (world == null) return members;
		Set<BlockPos> visited = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start.immutable());
		visited.add(start.immutable());
		while (!queue.isEmpty() && members.size() < MAX_NETWORK) {
			BlockPos p = queue.poll();
			BlockEntity be = getLoadedBlockEntity(world, p);
			if (be instanceof IEnergyNode member && member.conductsEnergy() && !be.isRemoved()) {
				members.add(member);
				for (Direction dir : Direction.values()) {
					BlockPos np = p.relative(dir);
					if (visited.add(np)) {
						queue.add(np);
					}
				}
			}
		}
		return members;
	}

	/**
	 * 节点放置或移除后失效缓存，并分别刷新变化位置周围的各个导通网络。
	 * 非导通机器只收到缓存失效通知，不会将相邻网络连起来。
	 */
	public static void broadcastInvalidate(Level world, BlockPos origin) {
		if (world.isClientSide) return;
		invalidateWorld(world);
		Set<BlockPos> processed = new HashSet<>();
		List<BlockPos> starts = new ArrayList<>();
		starts.add(origin);
		for (Direction direction : Direction.values()) starts.add(origin.relative(direction));
		for (BlockPos start : starts) {
			BlockEntity neighbor = getLoadedBlockEntity(world, start);
			if (neighbor instanceof IEnergyMachine machine) machine.markNetworkDirty();
			if (processed.contains(start)) continue;
			List<IEnergyNode> component = collect(world, start);
			if (component.isEmpty()) continue;
			for (IEnergyNode node : component) {
				BlockPos pos = ((BlockEntity) node).getBlockPos();
				processed.add(pos);
				node.markNetworkDirty();
				for (Direction direction : Direction.values()) {
					BlockEntity adjacent = getLoadedBlockEntity(world, pos.relative(direction));
					if (adjacent instanceof IEnergyMachine machine) machine.markNetworkDirty();
				}
			}
			TankEnergy.equalizeTanks(component);
			TankEnergy.refreshTankDisplays(component);
		}
	}
}
