package github.com.gengyoubo.sscfa.screen;

import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.block.PotionStorageBoxBlockEntity;
import github.com.gengyoubo.sscfa.init.AddonMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 药品存储箱容器（jackcooper）：8 个存储槽（单排），仅接受压缩能量药水（feed_potion），每格叠放上限
 * {@link PotionStorageBoxBlockEntity#MAX_PER_SLOT}。
 */
public class PotionStorageBoxScreenHandler extends AbstractContainerMenu {

	private static final int CONTAINER_SLOTS = PotionStorageBoxBlockEntity.SLOT_COUNT;

	private final Container inventory;

	/** 客户端构造。 */
	public PotionStorageBoxScreenHandler(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, new SimpleContainer(CONTAINER_SLOTS));
	}

	/** 服务端构造。 */
	public PotionStorageBoxScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
		super(AddonMenus.POTION_STORAGE_BOX.get(), syncId);
		checkContainerSize(inventory, CONTAINER_SLOTS);
		this.inventory = inventory;
		inventory.startOpen(playerInventory.player);

		// 8 个存储槽（单排，居中）：坐标对齐 GUI 贴图槽位内部（贴图内边框起点 x=16/y=20，物品区从 x=17/y=21 起）
		for (int i = 0; i < CONTAINER_SLOTS; i++) {
			this.addSlot(new Slot(inventory, i, 17 + i * 18, 21) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return EnergyBottlerBlockEntity.isEnergyBottle(stack);
				}

				@Override
				public int getMaxStackSize() {
					return PotionStorageBoxBlockEntity.MAX_PER_SLOT;
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					return PotionStorageBoxBlockEntity.MAX_PER_SLOT;
				}
			});
		}

		// 玩家背包（3 行）：对齐贴图内部 y=50/68/86
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 50 + row * 18));
			}
		}
		// 快捷栏：对齐贴图内部 y=108
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 108));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.inventory.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack originalStack = slot.getItem();
			newStack = originalStack.copy();
			if (index < CONTAINER_SLOTS) {
				// 存储箱 → 玩家背包
				if (!this.moveItemStackTo(originalStack, CONTAINER_SLOTS, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (EnergyBottlerBlockEntity.isEnergyBottle(originalStack)) {
				// 玩家背包 → 存储箱（仅能量瓶）：不论形态，按箱槽上限（8）合并——
				// 「无 power 不可叠」只限制在玩家身上，自有容器不套用
				int before = originalStack.getCount();
				this.insertIntoBoxSlots(originalStack);
				if (originalStack.getCount() == before) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}

			if (originalStack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return newStack;
	}

	/**
	 * 玩家背包 → 存储箱的自定义插入（不论形态）：先并入未满同类堆（至 MAX_PER_SLOT），
	 * 再放入空槽。绕开原版 insertItem 的 power 门控——箱槽叠放不限制形态。
	 */
	private void insertIntoBoxSlots(ItemStack stack) {
		int before = stack.getCount();
		// 先合并进未满的同类堆
		for (int i = 0; i < CONTAINER_SLOTS && !stack.isEmpty(); i++) {
			ItemStack target = this.inventory.getItem(i);
			if (!target.isEmpty() && ItemStack.isSameItemSameTags(target, stack)) {
				int room = PotionStorageBoxBlockEntity.MAX_PER_SLOT - target.getCount();
				if (room > 0) {
					int moved = Math.min(room, stack.getCount());
					target.grow(moved);
					stack.shrink(moved);
				}
			}
		}
		// 再放空槽
		for (int i = 0; i < CONTAINER_SLOTS && !stack.isEmpty(); i++) {
			if (this.inventory.getItem(i).isEmpty()) {
				int moved = Math.min(PotionStorageBoxBlockEntity.MAX_PER_SLOT, stack.getCount());
				this.inventory.setItem(i, stack.split(moved));
			}
		}
		if (stack.getCount() != before) this.inventory.setChanged();
	}
}
