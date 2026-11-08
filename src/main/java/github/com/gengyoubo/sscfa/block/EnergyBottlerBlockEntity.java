package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.SidedInventoryCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyMachine;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.screen.EnergyBottlerScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.registry.ModPotions;

import java.util.Collections;
import java.util.List;

/**
 * 能量装瓶器方块实体（jackcooper）：类炼药台的三线并行装瓶机。
 * <p>从相邻能量网络（{@link EnergyNetwork}，除正下方外任一面相邻的储罐/汲取器）抽取能量，
 * 每条线独立地以 {@link #ENERGY_PER_BOTTLE} 能量 + 1 个空玻璃瓶、经 {@link #CRAFT_TIME} tick 合成 1 瓶
 * 压缩能量药水（feed_potion）。三条线并行；手动/自动为总开关。输出槽每格可叠 {@link #OUTPUT_MAX} 瓶。
 * 支持漏斗：顶/侧插入空玻璃瓶到输入槽，底部抽取成品。
 */
public class EnergyBottlerBlockEntity extends BlockEntity
		implements WorldlyContainer, MenuProvider, IEnergyMachine {

    private final SidedInventoryCapabilities inventoryCapabilities = new SidedInventoryCapabilities(this);

	/** 合成一瓶消耗的能量。 */
	public static final int ENERGY_PER_BOTTLE = 25;
	/** 合成一瓶所需时间（tick），3 秒。 */
	public static final int CRAFT_TIME = 60;
	/** 并行合成线数量。 */
	public static final int LINES = 3;
	/** 每个输出槽最多叠放的能量瓶数。 */
	public static final int OUTPUT_MAX = 8;

	/** 输入槽（空玻璃瓶）索引 0~2；输出槽（能量瓶）索引 3~5。 */
	private final NonNullList<ItemStack> items = NonNullList.withSize(LINES * 2, ItemStack.EMPTY);

	/** 每条线的合成进度。 */
	private final int[] progress = new int[LINES];
	/** 手动模式下每条线的单次合成请求（完成一瓶后清除）。 */
	private final boolean[] manualRequested = new boolean[LINES];
	/** 是否自动合成（总开关，作用于全部三条线）。 */
	private boolean autoMode = false;

	/** 相邻能量网络缓存（消费者：事件驱动失效，非每 tick 洪泛）。 */
	private EnergyNetworkTopology.Cache networkCache;
	private boolean networkDirty = true;

	/** 客户端槽位镜像（仅渲染用）：索引 0~5 对应输入 0~2 / 输出 0~2。 */
	private final NonNullList<ItemStack> clientItems = NonNullList.withSize(LINES * 2, ItemStack.EMPTY);
	/** 上 tick 槽位非空快照（服务端，对比用，炼药台同款思路）。 */
	private final boolean[] slotsFilledLastTick = new boolean[LINES * 2];

	/** 同步给客户端 GUI 的属性：0=网络能量 1=网络上限 2/3/4=三线进度 5=是否自动。 */
	private final ContainerData propertyDelegate = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> EnergyNetwork.of(getEnergyNetwork()).getStoredEnergy();
				case 1 -> EnergyNetwork.of(getEnergyNetwork()).getCapacity();
				case 2 -> progress[0];
				case 3 -> progress[1];
				case 4 -> progress[2];
				case 5 -> autoMode ? 1 : 0;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case 2 -> progress[0] = value;
				case 3 -> progress[1] = value;
				case 4 -> progress[2] = value;
				case 5 -> autoMode = value != 0;
				default -> {
				}
			}
		}

		@Override
		public int getCount() {
			return 6;
		}
	};

	public EnergyBottlerBlockEntity(BlockPos pos, BlockState state) {
		super(AddonBlockEntities.ENERGY_BOTTLER.get(), pos, state);
	}

	// ==================== 每 tick 逻辑（仅服务端） ====================

	public static void tick(Level world, BlockPos pos, BlockState state, EnergyBottlerBlockEntity be) {
		if (world.isClientSide) {
			return;
		}
		boolean dirty = false;
		for (int i = 0; i < LINES; i++) {
			if (be.canCraft(i) && (be.autoMode || be.manualRequested[i])) {
				be.progress[i]++;
				if (be.progress[i] >= CRAFT_TIME) {
					if (!be.craftOne(i)) {
						be.progress[i] = 0;
						dirty = true;
						continue;
					}
					be.progress[i] = 0;
					if (!be.autoMode) {
						be.manualRequested[i] = false;
					}
					world.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 1.0f);
				}
				dirty = true;
			} else if (be.progress[i] != 0) {
				be.progress[i] = 0;
				dirty = true;
			} else if (be.manualRequested[i] && !be.canCraft(i)) {
				be.manualRequested[i] = false;
				dirty = true;
			}
		}
		if (dirty) {
			be.setChanged();
		}
		// 槽位快照对比（炼药台同款）：任一槽位空↔非空变化时同步给客户端渲染瓶子
		boolean[] filled = new boolean[LINES * 2];
		for (int i = 0; i < LINES * 2; i++) {
			filled[i] = !be.items.get(i).isEmpty();
		}
		if (!java.util.Arrays.equals(filled, be.slotsFilledLastTick)) {
			System.arraycopy(filled, 0, be.slotsFilledLastTick, 0, filled.length);
			be.syncItemsToClient();
		}
	}

	/** 服务端：把全部槽位 ItemStack 发给客户端（BE 数据包，走 readNbt 更新镜像）。 */
	private void syncItemsToClient() {
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** 客户端：渲染器读取的槽位镜像。 */
	public ItemStack getClientStack(int slot) {
		return clientItems.get(slot);
	}

	/** 第 i 条线是否可合成：网络能量≥25、该输入槽有空玻璃瓶、该输出槽能容纳一瓶能量瓶。 */
	public boolean canCraft(int line) {
		if (EnergyNetwork.of(getEnergyNetwork()).getStoredEnergy() < ENERGY_PER_BOTTLE) {
			return false;
		}
		if (!items.get(line).is(Items.GLASS_BOTTLE)) {
			return false;
		}
		ItemStack out = items.get(LINES + line);
		return out.isEmpty() || (out.is(AddonItems.UNIVERSAL_ENERGY_POTION.get()) && out.getCount() < OUTPUT_MAX);
	}

	/** 结算第 i 条线一瓶：从网络抽 25 能量、耗 1 空瓶、输出槽 +1 瓶通用能量药水。 */
	private boolean craftOne(int line) {
		var network = EnergyNetwork.of(getEnergyNetwork());
		int paid = network.extract(ENERGY_PER_BOTTLE);
		if (paid != ENERGY_PER_BOTTLE) {
			if (paid > 0) network.insert(paid);
			return false;
		}
		items.get(line).shrink(1);
		ItemStack out = items.get(LINES + line);
		if (out.isEmpty()) {
			items.set(LINES + line, makeEnergyBottle());
		} else {
			out.grow(1);
		}
		return true;
	}

	/** 能量瓶 = 通用能量药水（饮用回 25 mana，持有能量条或原版魔力体系的形态生效）。 */
	public static ItemStack makeEnergyBottle() {
		return new ItemStack(AddonItems.UNIVERSAL_ENERGY_POTION.get());
	}

	/** 是否为通用能量药水（新装瓶器产出）或旧版压缩能量药水瓶（兼容旧存档）。 */
	public static boolean isEnergyBottle(ItemStack stack) {
		if (stack.getItem() == AddonItems.UNIVERSAL_ENERGY_POTION.get()) {
			return true;
		}
		if (!stack.is(Items.POTION)) {
			return false;
		}
		Potion potion = PotionUtils.getPotion(stack);
		return potion == ModPotions.FEED.get();
	}

	// ==================== 能量网络消费者 ====================

	@Override
	public void markNetworkDirty() {
		networkDirty = true;
	}

	/** 定位并缓存相邻能量网络（除正下方外任一面相邻的能量源，取其整个共享网络）。 */
	public List<IEnergyNode> getEnergyNetwork() {
        List<IEnergyNode> members = networkCache == null ? null : networkCache.resolve(level);
        if (networkDirty || members == null) {
            members = findAdjacentNetwork();
            networkCache = new EnergyNetworkTopology.Cache(level, members);
            networkDirty = false;
        }
        return members;
    }

	private List<IEnergyNode> findAdjacentNetwork() {
		if (level == null) {
			return Collections.emptyList();
		}
		for (Direction dir : Direction.values()) {
			if (dir == Direction.DOWN) {
				continue; // 除正下方外任一面相邻
			}
			BlockEntity be = EnergyNetworkTopology.getLoadedBlockEntity(level, worldPosition.relative(dir));
			if (be instanceof IEnergyNode node && node.conductsEnergy() && !be.isRemoved()) {
				return EnergyNetworkTopology.collect(level, worldPosition.relative(dir));
			}
		}
		return Collections.emptyList();
	}

	// ==================== GUI 按钮回调（服务端） ====================

	/** 切换手动/自动总开关。 */
	public void toggleAutoMode() {
		autoMode = !autoMode;
		if (!autoMode) {
			for (int i = 0; i < LINES; i++) {
				manualRequested[i] = false;
			}
		}
		setChanged();
	}

	/** 手动模式下：对全部三条线各请求一次合成。 */
	public void requestManualCraft() {
		if (!autoMode) {
			for (int i = 0; i < LINES; i++) {
				manualRequested[i] = true;
			}
			setChanged();
		}
	}

	// ==================== NBT 持久化 ====================

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		ContainerHelper.saveAllItems(nbt, items);
		nbt.putIntArray("Progress", progress.clone());
		nbt.putBoolean("AutoMode", autoMode);
		int mask = 0;
		for (int i = 0; i < LINES; i++) {
			if (manualRequested[i]) {
				mask |= (1 << i);
			}
		}
		nbt.putInt("ManualMask", mask);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		items.clear();
		ContainerHelper.loadAllItems(nbt, items);
		// 客户端 BE 数据包路径：刷新渲染镜像（服务端磁盘加载读到也无妨，会被快照同步覆盖）
		clientItems.clear();
		NonNullList<ItemStack> mirror = NonNullList.withSize(LINES * 2, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(nbt, mirror);
		for (int i = 0; i < LINES * 2 && i < mirror.size(); i++) {
			clientItems.set(i, mirror.get(i));
		}
		int[] p = nbt.getIntArray("Progress");
		for (int i = 0; i < LINES; i++) {
			progress[i] = i < p.length ? p[i] : 0;
		}
		autoMode = nbt.getBoolean("AutoMode");
		int mask = nbt.getInt("ManualMask");
		for (int i = 0; i < LINES; i++) {
			manualRequested[i] = (mask & (1 << i)) != 0;
		}
	}

	// ==================== ScreenHandlerFactory ====================

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.ssc_addon.energy_bottler");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new EnergyBottlerScreenHandler(syncId, playerInventory, this, propertyDelegate);
	}

	// ==================== 客户端渲染同步（动态瓶子） ====================

	/** BE 更新包：客户端收到后走 readNbt 刷新瓶子镜像。 */
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** 区块数据包：进存档/重进世界时客户端也能拿到槽位内容渲染瓶子。Items 必须在根级（客户端 readNbt 才能读回）。 */
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();
		ContainerHelper.saveAllItems(nbt, items);
		return nbt;
	}

	// ==================== SidedInventory（漏斗互通） ====================

	@Override
	public int[] getSlotsForFace(Direction side) {
		int[] slots = new int[LINES * 2];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = i;
		}
		return slots;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
		// 仅允许往输入槽插入空玻璃瓶
		return slot < LINES && stack.is(Items.GLASS_BOTTLE);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
		// 仅允许从输出槽抽取
		return slot >= LINES;
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack result = ContainerHelper.removeItem(items, slot, amount);
		if (!result.isEmpty()) {
			setChanged();
		}
		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		int max = slot < LINES ? 64 : OUTPUT_MAX;
		if (stack.getCount() > max) {
			stack.setCount(max);
		}
		setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return OUTPUT_MAX;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < LINES && stack.is(Items.GLASS_BOTTLE);
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
			return false;
		}
		return player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clearContent() {
		items.clear();
	}
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return inventoryCapabilities.get(side).cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); inventoryCapabilities.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); inventoryCapabilities.revive(); }

}
