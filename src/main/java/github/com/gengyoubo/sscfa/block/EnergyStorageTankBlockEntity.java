package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyStorageNode;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * 能量储罐方块实体（jackcooper）：被动存储节点，作为 {@link IEnergyNode} 加入能量网络。
 * <p>自身不产能、不消耗，仅提供 {@link #MAX_ENERGY} 的存储上限；相邻储罐/汲取器构成同一共享池，
 * 多个储罐相邻即可叠加网络总上限。能量由汲取器注入、被装瓶器抽取，储罐本身无需 tick。
 * <p>液面显示：不再用 11 档方块状态贴图，而是把所在网络的能量比例 {@code clientRatio} 同步到客户端，
 * 由 {@code EnergyStorageTankRenderer} 在玻璃区域内画无级液面（参照 FluidTank 的 BER 方案）。
 */
public class EnergyStorageTankBlockEntity extends BlockEntity implements IEnergyStorageNode {

	/** 单个储罐的能量上限。 */
	public static final int MAX_ENERGY = 1000;

	private int energy = 0;

	/** 客户端液面比例（0~1）。服务端记录上次已同步值用于节流；-1 表示尚未同步。 */
	private float clientRatio = -1f;

	/** 液面比例同步节流阈值：变化小于该值不发包（避免汲取器每 tick 注入时高频刷包）。 */
	private static final float RATIO_SYNC_EPSILON = 0.004f;

	private EnergyNetworkTopology.Cache networkCache;
	private boolean networkDirty = true;

	public EnergyStorageTankBlockEntity(BlockPos pos, BlockState state) {
		super(AddonBlockEntities.ENERGY_STORAGE_TANK.get(), pos, state);
	}

	// ==================== 能量网络成员 ====================

	@Override
	public int getStoredEnergy() {
		return energy;
	}

	@Override
	public void setStoredEnergy(int value) {
		energy = Math.max(0, Math.min(MAX_ENERGY, value));
		setChanged();
	}

	// ==================== 液面比例同步（服务端 → 客户端） ====================

	/**
	 * 服务端调用：更新并按需同步液面比例。
	 * 比例变化超过 {@link #RATIO_SYNC_EPSILON} 才发 BE 数据包，避免每 tick 注入/抽取时高频刷包。
	 */
	public void syncFillRatio(float ratio) {
		ratio = Math.max(0f, Math.min(1f, ratio));
		if (Math.abs(ratio - clientRatio) <= RATIO_SYNC_EPSILON && clientRatio >= 0f) {
			return; // 变化太小，不发包
		}
		clientRatio = ratio;
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** 客户端读取液面比例（0~1）；负数表示尚未收到同步（渲染器应跳过绘制）。 */
	public float getClientFillRatio() {
		return clientRatio;
	}

	@Override
	public int getEnergyCapacity() {
		return MAX_ENERGY;
	}

	@Override
	public void markNetworkDirty() {
		networkDirty = true;
	}

	/** 获取当前网络快照；缓存失效时重新发现节点。 */
	public IEnergyNetwork getNetwork() {
        List<IEnergyNode> members = networkCache == null ? null : networkCache.resolve(level);
        if (networkDirty || members == null) {
            members = EnergyNetworkTopology.collect(this.level, this.worldPosition);
            networkCache = new EnergyNetworkTopology.Cache(level, members);
            networkDirty = false;
        }
        return EnergyNetwork.of(members);
    }

	// ==================== NBT 持久化 + 客户端同步 ====================

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("Energy", energy);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		energy = nbt.getInt("Energy");
		// 客户端 BE 数据包路径：读液面比例（服务端磁盘加载读到也无妨，会被 syncFillRatio 覆盖）
		clientRatio = nbt.getFloat("ClientRatio");
	}

	/** BE 更新包：客户端收到后走 readNbt 刷新液面。 */
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	/** 区块数据包：进存档/重进世界时客户端也能拿到液面比例。
	 * <p>修复：不再写 {@code clientRatio}（那是服务端的同步缓存，不持久化，服务器重启后为 -1，
	 * 导致进游戏时包里永远是 0、液面不显示），改为按当前所在网络实时计算比例写入；
	 * BE 尚无 world（区块加载极早期）时降级用自身 energy/MAX_ENERGY 兜底。 */
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();
		nbt.putInt("Energy", energy);
		float ratio;
		if (level != null) {
			IEnergyNetwork net = getNetwork();
			int cap = net.getCapacity();
			ratio = cap > 0 ? (float) net.getStoredEnergy() / cap : 0f;
		} else {
			ratio = (float) energy / MAX_ENERGY;
		}
		nbt.putFloat("ClientRatio", Math.max(0f, Math.min(1f, ratio)));
		return nbt;
	}
}
