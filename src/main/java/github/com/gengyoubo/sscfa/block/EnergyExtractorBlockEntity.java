package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.energy.IEnergyStorageNode;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;

/**
 * 能量汲取器方块实体（jackcooper）。
 * <p>使魔系玩家走进方块本格站立时，每 {@link #STEP_INTERVAL} tick 消耗 {@link #MANA_PER_STEP} 点 mana，
 * 按 2 点魔力转换为 3 点能量并注入所在能量网络（{@link EnergyNetwork}）。本方块自身仅作小容量缓冲
 * （上限 {@link #MAX_ENERGY}），能量会溢流进相邻的能量储罐。不再自行合成能量瓶——合成交由能量装瓶器。
 */
public class EnergyExtractorBlockEntity extends BlockEntity implements IEnergyStorageNode {
    private static final Set<String> FAMILIAR_FORMS = Set.of(
            "my_addon:familiar_fox_spirit_realm_lord", "my_addon:familiar_fox_sp",
            "my_addon:familiar_fox_upgrade", "my_addon:upgrade_familiar_fox",
            "my_addon:familiar_fox_red", "my_addon:familiar_fox_mancianima");

	/** 汲取器自身能量缓冲上限（较小，主要缓冲；储存靠储罐扩容）。 */
	public static final int MAX_ENERGY = 200;
	/** 转化节拍（每多少 tick 结算一次 mana→能量）。 */
	private static final int STEP_INTERVAL = 5;
	/** 每个节拍消耗的 mana。 */
	private static final int MANA_PER_STEP = 2;
	/** 每个节拍转化得到的能量（2026-08-27 效率提升 50%：2→3）。 */
	private static final int ENERGY_PER_STEP = 3;

	/** 当前存储能量。 */
	private int energy = 0;

	/** 所在网络成员缓存（事件驱动失效，避免每 tick 洪泛扫描）。 */
	private EnergyNetworkTopology.Cache networkCache;
	private boolean networkDirty = true;

	public EnergyExtractorBlockEntity(BlockPos pos, BlockState state) {
		super(AddonBlockEntities.ENERGY_EXTRACTOR.get(), pos, state);
	}

	// ==================== 每 tick 逻辑（仅服务端） ====================

	public static void tick(Level world, BlockPos pos, BlockState state, EnergyExtractorBlockEntity be) {
		if (world.isClientSide) {
			return;
		}
		if (world.getGameTime() % STEP_INTERVAL != 0) {
			return;
		}
		IEnergyNetwork network = be.getNetwork();
		// 空间不足以容纳整次产出时不消耗魔力。
		if ((long) network.getCapacity() - network.getStoredEnergy() < ENERGY_PER_STEP) {
			return;
		}
		ServerPlayer user = findFamiliarUser(world, pos);
		if (user == null) {
			return;
		}
		FormActivePowerService.consumeMana(user, MANA_PER_STEP);
		// 前置模组的 consumeMana 会同步玩家魔力。
		network.insert(ENERGY_PER_STEP);
		// 转化嗡鸣（降频，避免过吵）
		if (world.getGameTime() % 40 == 0) {
			world.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.6f);
		}
	}

	/** 查找站在本方块格内的使魔系玩家（服务端权威，范围检测扫服务端实体）。 */
	private static ServerPlayer findFamiliarUser(Level world, BlockPos pos) {
		AABB box = new AABB(pos);
		List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, box,
				p -> p.isAlive() && FormActivePowerService.hasFamiliarFoxMana(p)
						&& FormActivePowerService.mana(p) >= MANA_PER_STEP
                        && SscApi.currentForm(p).map(data -> FAMILIAR_FORMS.contains(data.getFormId())).orElse(false));
		return players.isEmpty() ? null : players.get(0);
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

	@Override
	public int getEnergyCapacity() {
		return MAX_ENERGY;
	}

	@Override
	public void markNetworkDirty() {
		networkDirty = true;
	}

	/** 获取所在网络成员（缓存，脏时重建）。 */
	public IEnergyNetwork getNetwork() {
        List<IEnergyNode> members = networkCache == null ? null : networkCache.resolve(level);
        if (networkDirty || members == null) {
            members = EnergyNetworkTopology.collect(this.level, this.worldPosition);
            networkCache = new EnergyNetworkTopology.Cache(level, members);
            networkDirty = false;
        }
        return EnergyNetwork.of(members);
    }

	// ==================== NBT 持久化 ====================

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("Energy", energy);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		energy = nbt.getInt("Energy");
	}
}
