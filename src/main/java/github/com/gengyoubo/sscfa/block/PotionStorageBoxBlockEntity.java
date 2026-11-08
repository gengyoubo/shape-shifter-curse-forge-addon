package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.SidedInventoryCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import github.com.gengyoubo.sscfa.init.AddonBlockEntities;

import github.com.gengyoubo.sscfa.screen.PotionStorageBoxScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 药品存储箱方块实体（jackcooper）：专存压缩能量药水（feed_potion）。
 * <p>共 {@link #SLOT_COUNT} 个槽，每格最多叠放 {@link #MAX_PER_SLOT} 瓶。实现 {@link WorldlyContainer} 支持漏斗
 * 任意面插入 feed_potion / 抽取成品（注意：原版漏斗合并药水受物品自身 maxCount=1 限制，漏斗插入每格仅到 1，
 * GUI/手动可叠满 8）。
 */
public class PotionStorageBoxBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    private final SidedInventoryCapabilities inventoryCapabilities = new SidedInventoryCapabilities(this);

	/** 存储槽数量。 */
	public static final int SLOT_COUNT = 8;
	/** 每格最多叠放的能量瓶数。 */
	public static final int MAX_PER_SLOT = 8;
	/** 主动抽取相邻漏斗的节拍（对齐漏斗 8t 冷却）。 */
	private static final int PULL_INTERVAL = 8;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

	/** 客户端槽位镜像（仅渲染用）：按总装填档位在柜内 4 个展示位显示药水瓶。 */
	private final NonNullList<ItemStack> clientItems = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	/** 上次已同步的展示档位（服务端，对比用；-1 表示尚未同步）。 */
	private int lastTier = -1;

	public PotionStorageBoxBlockEntity(BlockPos pos, BlockState state) {
		super(AddonBlockEntities.POTION_STORAGE_BOX.get(), pos, state);
	}

	// ==================== 每 tick 逻辑（仅服务端） ====================

	public static void tick(Level world, BlockPos pos, BlockState state, PotionStorageBoxBlockEntity be) {
		if (world.isClientSide) {
			return;
		}
		// 主动从「出口朝向本箱」的相邻漏斗抽取能量瓶并叠入（箱子自己插入，不受原版药水 maxCount=1 限制），
		// 从而像原版叠同类物品那样自然填满 8格×8=64。非漏斗投入的零散堆会在下次抽取时被优先补满。
		if (world.getGameTime() % PULL_INTERVAL == 0) {
			be.pullFromFeederHoppers(world, pos);
		}
		// 展示档位对比：总装填每跨 25% 档才发 BE 包（柜内瓶数变化但未跨档时不刷包，省带宽）
		int tier = displayTier(totalBottles(be.items));
		if (tier != be.lastTier) {
			be.lastTier = tier;
			if (!world.isClientSide) {
				world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
			}
		}
	}

	/** 柜内总瓶数（所有槽的堆叠数之和）。 */
	public static int totalBottles(java.util.List<ItemStack> stacks) {
		int total = 0;
		for (ItemStack s : stacks) {
			if (!s.isEmpty()) {
				total += s.getCount();
			}
		}
		return total;
	}

	/**
	 * 展示档位（0~8）：总装填每 12.5% 一档（满柜 8槽×8=64 瓶，即每 8 瓶 +1 档）；
	 * 有任何瓶子时至少 1 档，满柜 8 档。渲染器与同步触发共用本公式，保证两端一致。
	 */
	public static int displayTier(int totalBottles) {
		if (totalBottles <= 0) return 0;
		return Math.min(8, (totalBottles + 7) / 8);
	}

	/** 客户端：渲染器读取的槽位镜像。 */
	public ItemStack getClientStack(int slot) {
		return clientItems.get(slot);
	}

	/** 抽取所有「出口方向正对本箱」的相邻漏斗里的能量瓶（仅馈送漏斗，不抽取从本箱取货的漏斗）。 */
	private void pullFromFeederHoppers(Level world, BlockPos boxPos) {
		for (Direction dir : Direction.values()) {
			BlockPos hpos = boxPos.relative(dir);
			BlockState hs = world.getBlockState(hpos);
			if (!hs.is(Blocks.HOPPER)) {
				continue;
			}
			// 仅处理出口（FACING）正对本箱的馈送漏斗；从本箱取货的漏斗出口背向本箱，不会被误抽
			if (!hpos.relative(hs.getValue(HopperBlock.FACING)).equals(boxPos)) {
				continue;
			}
			if (world.getBlockEntity(hpos) instanceof Container hopperInv) {
				pullFrom(hopperInv);
			}
		}
	}

	/** 从一个馈送漏斗里尽量抽取能量瓶并叠入本箱。 */
	private void pullFrom(Container hopperInv) {
		for (int i = 0; i < hopperInv.getContainerSize(); i++) {
			ItemStack s = hopperInv.getItem(i);
			if (s.isEmpty() || !EnergyBottlerBlockEntity.isEnergyBottle(s)) {
				continue;
			}
			boolean moved = false;
			while (!s.isEmpty() && insertOverstackOne(s)) {
				s.shrink(1);
				moved = true;
			}
			if (s.isEmpty()) {
				hopperInv.setItem(i, ItemStack.EMPTY);
			}
			if (moved) {
				hopperInv.setChanged();
			}
		}
	}

	/** 把 1 瓶能量瓶叠入本箱：先填未满的同类堆（叠至 {@link #MAX_PER_SLOT}），再用空槽；成功返回 true。 */
	private boolean insertOverstackOne(ItemStack potion) {
		for (int i = 0; i < SLOT_COUNT; i++) {
			ItemStack a = items.get(i);
			if (!a.isEmpty() && a.getCount() < MAX_PER_SLOT && ItemStack.isSameItemSameTags(a, potion)) {
				a.grow(1);
				setChanged();
				return true;
			}
		}
		for (int i = 0; i < SLOT_COUNT; i++) {
			if (items.get(i).isEmpty()) {
				ItemStack one = potion.copy();
				one.setCount(1);
				items.set(i, one);
				setChanged();
				return true;
			}
		}
		return false;
	}

	// ==================== NBT 持久化 ====================

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		ContainerHelper.saveAllItems(nbt, items);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		items.clear();
		ContainerHelper.loadAllItems(nbt, items);
		// 客户端 BE 数据包路径：刷新柜内瓶子渲染镜像
		clientItems.clear();
		NonNullList<ItemStack> mirror = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(nbt, mirror);
		for (int i = 0; i < SLOT_COUNT && i < mirror.size(); i++) {
			clientItems.set(i, mirror.get(i));
		}
	}

	// ==================== ScreenHandlerFactory ====================

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.ssc_addon.potion_storage_box");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new PotionStorageBoxScreenHandler(syncId, playerInventory, this);
	}

	// ==================== 客户端渲染同步（柜内动态瓶子） ====================

	/** BE 更新包：客户端收到后走 readNbt 刷新柜内瓶子镜像。 */
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** 区块数据包：进存档/重进世界时客户端也能拿到槽位内容渲染柜内瓶子。 */
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();
		ContainerHelper.saveAllItems(nbt, items);
		return nbt;
	}

	// ==================== SidedInventory（漏斗互通） ====================

	@Override
	public int[] getSlotsForFace(Direction side) {
		int[] slots = new int[SLOT_COUNT];
		for (int i = 0; i < SLOT_COUNT; i++) {
			slots[i] = i;
		}
		return slots;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
		return EnergyBottlerBlockEntity.isEnergyBottle(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
		return true;
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
		if (stack.getCount() > MAX_PER_SLOT) {
			stack.setCount(MAX_PER_SLOT);
		}
		setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return MAX_PER_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return EnergyBottlerBlockEntity.isEnergyBottle(stack);
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
