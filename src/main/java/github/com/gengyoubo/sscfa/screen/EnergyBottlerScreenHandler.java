package github.com.gengyoubo.sscfa.screen;

import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.init.AddonMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 能量装瓶器容器（jackcooper）。
 * <p>三条独立合成线：左 3 个空玻璃瓶输入槽、右 3 个能量瓶输出槽（每格叠放上限
 * {@link EnergyBottlerBlockEntity#OUTPUT_MAX}）。网络能量 / 三线进度 / 手动自动经 {@link ContainerData} 同步。
 * GUI 按钮经 {@link #clickMenuButton} 服务端处理（id 0=切总开关，1=手动对三线各请求一次合成）。
 */
public class EnergyBottlerScreenHandler extends AbstractContainerMenu {

	/** 按钮 id：切换手动/自动总开关。 */
	public static final int BUTTON_TOGGLE_MODE = 0;
	/** 按钮 id：手动请求三线各合成一次。 */
	public static final int BUTTON_MANUAL_CRAFT = 1;

	private static final int CONTAINER_SLOTS = EnergyBottlerBlockEntity.LINES * 2;

	private final Container inventory;
	private final ContainerData propertyDelegate;

	/** 客户端构造。 */
	public EnergyBottlerScreenHandler(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(6));
	}

	/** 服务端构造。 */
	public EnergyBottlerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
		super(AddonMenus.ENERGY_BOTTLER.get(), syncId);
		checkContainerSize(inventory, CONTAINER_SLOTS);
		this.inventory = inventory;
		this.propertyDelegate = propertyDelegate;
		inventory.startOpen(playerInventory.player);
		this.addDataSlots(new SplitIntPropertyDelegate(propertyDelegate, 2));

		int lines = EnergyBottlerBlockEntity.LINES;
		// 三条线：输入槽（空玻璃瓶）与输出槽（能量瓶）；Y 上移 1px 对齐背景贴图槽位（用户校准）
		for (int i = 0; i < lines; i++) {
			int y = 17 + i * 24;
			// 输入槽
			this.addSlot(new Slot(inventory, i, 44, y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(Items.GLASS_BOTTLE);
				}

				@Override
				public int getMaxStackSize() {
					return 64;
				}
			});
		}
		for (int i = 0; i < lines; i++) {
			int y = 17 + i * 24;
			// 输出槽（仅取出，可叠 OUTPUT_MAX）
			this.addSlot(new Slot(inventory, lines + i, 108, y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}

				@Override
				public int getMaxStackSize() {
					return EnergyBottlerBlockEntity.OUTPUT_MAX;
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					return EnergyBottlerBlockEntity.OUTPUT_MAX;
				}
			});
		}

		// 玩家背包（3 行）
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
			}
		}
		// 快捷栏
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 160));
		}
	}

	public int getEnergy() {
		return propertyDelegate.get(0);
	}

	public int getCapacity() {
		return propertyDelegate.get(1);
	}

	public int getProgress(int line) {
		return propertyDelegate.get(2 + line);
	}

	public boolean isAutoMode() {
		return propertyDelegate.get(5) != 0;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (inventory instanceof EnergyBottlerBlockEntity be) {
			if (id == BUTTON_TOGGLE_MODE) {
				be.toggleAutoMode();
				return true;
			}
			if (id == BUTTON_MANUAL_CRAFT) {
				be.requestManualCraft();
				return true;
			}
		}
		return false;
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
				// 容器 → 玩家背包
				if (!this.moveItemStackTo(originalStack, CONTAINER_SLOTS, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (originalStack.is(Items.GLASS_BOTTLE)) {
				// 玩家背包 → 输入槽（仅空玻璃瓶，索引 0~2）
				if (!this.moveItemStackTo(originalStack, 0, EnergyBottlerBlockEntity.LINES, false)) {
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
}
