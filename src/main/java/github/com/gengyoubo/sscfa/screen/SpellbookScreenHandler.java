package github.com.gengyoubo.sscfa.screen;

import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 月尘魔法书配置界面容器（jackcooper）。卷轴槽数随书等级动态（3/5/7），
 * 通过 Fabric {@code ExtendedScreenHandler} 把槽数与等级/法力快照传给客户端?
 * 卷轴内容序列化进书自?NBT ?{@code Items} 列表（与 {@link SpellbookData} 同结构）?
 */
public class SpellbookScreenHandler extends AbstractContainerMenu {
	private final Container inventory;
	private final ItemStack bookStack;
	private final Inventory ownerInventory;
	public final int bookInventorySlot;
	private boolean loading;
	private final boolean clientView;
	public final int slotCount;
	public final int bookLevel;
	public final int bookExp;
	public final int bookMana;
	public final int bookMaxMana;
	public final CompoundTag playerState;

	/** 服务端构造：拿到真实魔法?stack?*/
	public SpellbookScreenHandler(int syncId, Inventory playerInv, ItemStack bookStack) {
		this(syncId, playerInv, bookStack,
				SpellbookData.getSlotCount(bookStack),
				SpellbookData.getLevel(bookStack),
				SpellbookData.getExpTen(bookStack),
				SpellbookData.getMana(bookStack),
				SpellbookData.getMaxMana(bookStack), findBookSlot(playerInv, bookStack), false,
				playerInv.player instanceof net.minecraft.server.level.ServerPlayer p ? github.com.gengyoubo.sscfa.neomagic.NeoNetwork.hudState(p) : new CompoundTag());
	}

	/** 客户端构造：从开屏数据包读取槽数与等?法力快照?*/
	public SpellbookScreenHandler(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
		this(syncId, playerInv, ItemStack.EMPTY,
				buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), true, buf.isReadable()?buf.readNbt():new CompoundTag());
	}

	private SpellbookScreenHandler(int syncId, Inventory playerInv, ItemStack bookStack,
			int slotCount, int level, int exp, int mana, int maxMana, int bookInventorySlot, boolean clientView, CompoundTag playerState) {
		super(github.com.gengyoubo.sscfa.init.AddonMenus.SPELLBOOK_SCREEN_HANDLER.get(), syncId);
		this.bookStack = bookStack;
		this.ownerInventory = playerInv;
		this.bookInventorySlot = bookInventorySlot;
		this.clientView = clientView;
		this.playerState = playerState == null ? new CompoundTag() : playerState.copy();
		this.slotCount = Math.max(1, Math.min(SpellbookData.MAX_SLOTS, slotCount));
		this.bookLevel = level;
		this.bookExp = exp;
		this.bookMana = mana;
		this.bookMaxMana = maxMana;

		this.inventory = new SimpleContainer(this.slotCount) {
			@Override
			public void setChanged() {
				super.setChanged();
				SpellbookScreenHandler.this.saveToNbt();
			}

			@Override
			public int getMaxStackSize() {
				return 1; // 每槽一张卷?
			}

			@Override
			public boolean canPlaceItem(int slot, ItemStack stack) {
				return ScrollData.isScroll(stack) && !github.com.gengyoubo.sscfa.spell.ArcaneAnalysis.isUnanalyzed(stack);
			}
		};

		// Loading is a read: do not write a partial inventory back after each setStack.
		loading = true;
		loadFromNbt(bookStack.getTag());
		loading = false;
		inventory.startOpen(playerInv.player);

		// 卷轴槽横排居中（GUI ?176?
		int totalWidth = this.slotCount * 18;
		int startX = (176 - totalWidth) / 2 + 1;
		for (int i = 0; i < this.slotCount; ++i) {
			this.addSlot(new Slot(inventory, i, startX + i * 18, 42) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return container.canPlaceItem(getContainerSlot(), stack);
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					return 1;
				}
			});
		}

		// 玩家背包
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		// 快捷?
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
		}
	}

	public static int findBookSlot(Inventory inventory, ItemStack book) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (!book.isEmpty() && inventory.getItem(i) == book) return i;
		}
		return -1;
	}

	@Override
	public void clicked(int slotIndex, int button, ClickType action, Player player) {
		if (!stillValid(player)) return;
		if (slotIndex >= 0 && slotIndex < slots.size()) {
			Slot slot = slots.get(slotIndex);
			if (slot.container == ownerInventory && slot.getContainerSlot() == bookInventorySlot) return;
		}
		// Number keys and F can otherwise move the open book from a different clicked slot.
		if (action == ClickType.SWAP && button == bookInventorySlot) return;
		super.clicked(slotIndex, button, action, player);
	}

	@Override
	public boolean stillValid(Player player) {
		return player.getInventory() == ownerInventory && bookInventorySlot >= 0
				&& (clientView || (!bookStack.isEmpty() && ownerInventory.getItem(bookInventorySlot) == bookStack));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.container == ownerInventory && slot.getContainerSlot() == bookInventorySlot) return ItemStack.EMPTY;
		if (slot != null && slot.hasItem()) {
			ItemStack originalStack = slot.getItem();
			newStack = originalStack.copy();
			if (index < this.inventory.getContainerSize()) {
				if (!this.moveItemStackTo(originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(originalStack, 0, this.inventory.getContainerSize(), false)) {
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

	private void loadFromNbt(CompoundTag nbt) {
		if (nbt != null && nbt.contains(SpellbookData.NBT_ITEMS, 9)) {
			ListTag list = nbt.getList(SpellbookData.NBT_ITEMS, 10);
			for (int i = 0; i < list.size(); ++i) {
				CompoundTag itemTag = list.getCompound(i);
				int slot = itemTag.getByte("Slot") & 255;
				if (slot >= 0 && slot < inventory.getContainerSize()) {
					inventory.setItem(slot, ItemStack.of(itemTag));
				}
			}
		}
	}

	private void saveToNbt() {
		if (!loading && !bookStack.isEmpty() && bookInventorySlot >= 0
				&& ownerInventory.getItem(bookInventorySlot) == bookStack) {
			ListTag list = new ListTag();
			for (int i = 0; i < inventory.getContainerSize(); ++i) {
				ItemStack stack = inventory.getItem(i);
				if (!stack.isEmpty()) {
					CompoundTag itemTag = new CompoundTag();
					itemTag.putByte("Slot", (byte) i);
					stack.save(itemTag);
					list.add(itemTag);
				}
			}
			bookStack.getOrCreateTag().put(SpellbookData.NBT_ITEMS, list);
		}
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		saveToNbt();
	}
}
