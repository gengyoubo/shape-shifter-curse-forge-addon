package github.com.gengyoubo.sscfa.screen;

import github.com.gengyoubo.sscfa.item.InfiniteEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.PotionBagItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PotionBagScreenHandler extends AbstractContainerMenu {
	private final Container inventory;
	private final ItemStack bagStack;
	private final Player player;
	private boolean loading;

	public PotionBagScreenHandler(int syncId, Inventory playerInventory, ItemStack bagStack) {
		super(github.com.gengyoubo.sscfa.init.AddonMenus.POTION_BAG_SCREEN_HANDLER.get(), syncId);
		this.bagStack = bagStack;
		this.player = playerInventory.player;
		// 1 Row x 9 Columns (Standard Single Chest Layout) = 9 Slots
		this.inventory = new SimpleContainer(9) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (!PotionBagScreenHandler.this.loading) PotionBagScreenHandler.this.saveToNbt();
			}

			@Override
			public int getMaxStackSize() {
				// 淇濈暀宸叉湁16绠″瓨妗ｏ紱鐜╁鏉冮檺鍜岀墿鍝佺绫荤殑鎻掑叆涓婇檺鐢辨Ы浣嶇粺涓€鎺у埗銆?
				return 64;
			}

			@Override
			public boolean canPlaceItem(int slot, ItemStack stack) {
				return PotionBagItem.isStorable(stack);
			}
		};

		// Load NBT
		loading = true;
		try {
			loadFromNbt(bagStack.hasTag() ? bagStack.getTag().copy() : null);
		} finally {
			loading = false;
		}
		// 娓呯悊宸插厖婊＄殑鏃犻檺鑽按绌虹摱鏍囪锛屼娇鎵撳紑鏃惰鍐呮樉绀烘甯稿悕绉?
		cleanRechargedInfinitePotions();
		saveToNbt();

		inventory.startOpen(playerInventory.player);

		// 1 Row x 9 Columns for Potion Bag
		// Standard start X=8, Y=18
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return PotionBagItem.isStorable(stack);
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					if (PotionBagItem.isStorable(stack)) return PotionBagItem.maxPerSlot(stack, PotionBagScreenHandler.this.player);
					return super.getMaxStackSize(stack);
				}
			});
		}

		// Player Inventory
		// Standard single chest GUI (1 row) places player inventory at Y=51 approx (actually 17 + 1*18 + 14 = 49)
		// With standard generic_54 texture, single row implies rows=1
		// Let's assume standard positioning:
		// Top chest rows end at Y = 17 + rows*18. Padding = 14. So player inv Y = 17 + 18 + 14 = 49
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 50 + row * 18));
			}
		}

		// Hotbar (Y = Player Inv End + 4 = 49 + 54 + 4 = 107) -> 108 usually for 1 row chest
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 108));
		}
	}

	// Client Constructor
	public PotionBagScreenHandler(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, ItemStack.EMPTY);
	}


	@Override
	public boolean stillValid(Player player) {
		if (player.level().isClientSide) return this.inventory.stillValid(player);
		if (player != this.player || !bagStack.is(github.com.gengyoubo.sscfa.init.AddonItems.POTION_BAG.get())) return false;
		// 褰㈡€佸洖鏀朵細鍊掑嚭琚嬪唴鐗╁搧锛涙棫鐣岄潰鐨勭嫭绔嬪簱瀛樹笉鑳藉啀琚彇鍑轰竴娆°€?
		// 蹇呴』鏍稿鍘熺墿鍝佸紩鐢紝鍙︿竴鍙浉鍚孨BT鐨勮瀛愪笉鑳界户鎵胯繖涓晫闈€?
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i) == bagStack) return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack originalStack = slot.getItem();
			newStack = originalStack.copy();
			if (index < this.inventory.getContainerSize()) {
				if (!this.moveItemStackTo(originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertIntoBagInventory(originalStack)) {
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

	/** Shift鍏ヨ鎸夊甫鐗╁搧鍙傛暟鐨勪笂闄愬悎骞讹紝閬垮厤鍘熺増绌烘Ы璺緞璇敤鑳屽悗inventory鐨?4涓婇檺銆?*/
	private boolean insertIntoBagInventory(ItemStack source) {
		if (!PotionBagItem.isStorable(source)) return false;
		int before = source.getCount();
		for (int i = 0; i < inventory.getContainerSize() && !source.isEmpty(); i++) {
			Slot slot = getSlot(i);
			ItemStack target = slot.getItem();
			if (target.isEmpty() || !ItemStack.isSameItemSameTags(target, source)) continue;
			int amount = Math.min(source.getCount(), Math.max(0, slot.getMaxStackSize(source) - target.getCount()));
			if (amount > 0) { target.grow(amount); source.shrink(amount); slot.setChanged(); }
		}
		for (int i = 0; i < inventory.getContainerSize() && !source.isEmpty(); i++) {
			Slot slot = getSlot(i);
			if (!slot.hasItem() && slot.mayPlace(source)) {
				int amount = Math.min(source.getCount(), slot.getMaxStackSize(source));
				slot.setByPlayer(source.copyWithCount(amount)); source.shrink(amount);
			}
		}
		return source.getCount() != before;
	}

	private void loadFromNbt(CompoundTag nbt) {
		if (nbt != null && nbt.contains("Items", 9)) {
			ListTag list = nbt.getList("Items", 10);
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
		if (!bagStack.isEmpty()) {
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
			bagStack.getOrCreateTag().put("Items", list);
		}
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		saveToNbt(); // Ensure save on close
	}

	/**
	 * 鍚屾鍐呭鍓嶅厛娓呯悊宸插厖婊＄殑鏃犻檺鑽按绌虹摱鏍囪锛屼娇鎵撳紑鏈熼棿鍏呮弧鐨勮嵂姘村疄鏃舵仮澶嶆甯稿悕绉帮紙浠呮湇鍔＄锛夈€?
	 */
	@Override
	public void broadcastChanges() {
		cleanRechargedInfinitePotions();
		super.broadcastChanges();
	}

	/**
	 * 娓呴櫎琚嬪唴宸插厖婊＄殑鏃犻檺鍘嬬缉鑳介噺鑽按鐨勭┖鐡舵爣璁帮紙浠呮湇鍔＄锛夈€?
	 * 鑽按琚嬪瓨鍌ㄧ墿涓嶈蛋鐗╁搧 inventoryTick锛岄渶鍦ㄦ涓诲姩娓呯悊锛屽惁鍒欏厖婊″悗浠嶆樉绀恒€岋紙绌猴級銆嶃€?
	 */
	private void cleanRechargedInfinitePotions() {
		if (player == null || player.level() == null || player.level().isClientSide) {
			return;
		}
		net.minecraft.world.level.Level world = player.level();
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack s = inventory.getItem(i);
			if (s.getItem() instanceof InfiniteEnergyPotionItem
					&& InfiniteEnergyPotionItem.clearRechargeMarkIfDone(s, world)) {
				inventory.setChanged(); // 瑙﹀彂鍐欏洖鑽按琚?NBT
			}
		}
	}

	/**
	 * 璇诲彇鑽按鍖呮寚瀹氭Ы浣嶅瓨鍌ㄧ殑鐗╁搧锛圢BT 缁撴瀯涓?{@link #saveToNbt} 涓€鑷达級銆?
	 * 渚?PotionBagItem 鐨勫揩鎹锋姇鏀炬爮锛堟Ы浣?0锛夊湪涓嶆墦寮€ GUI 鏃剁洿鎺ヨ鍙栵紝澶氫汉涓嬭鐨勬槸鏈嶅姟绔悓姝ヨ繃鏉ョ殑鎵嬫寔鐗?NBT銆?
	 *
	 * @return 璇ユЫ浣嶇墿鍝侊紱涓嶅瓨鍦ㄦ椂杩斿洖 {@link ItemStack#EMPTY}
	 */
	public static ItemStack getStoredStack(ItemStack bagStack, int slot) {
		CompoundTag nbt = bagStack.getTag();
		if (nbt == null || !nbt.contains("Items", 9)) {
			return ItemStack.EMPTY;
		}
		ListTag list = nbt.getList("Items", 10);
		for (int i = 0; i < list.size(); ++i) {
			CompoundTag itemTag = list.getCompound(i);
			if ((itemTag.getByte("Slot") & 255) == slot) {
				return ItemStack.of(itemTag);
			}
		}
		return ItemStack.EMPTY;
	}

	/**
	 * 鍐欏洖鑽按鍖呮寚瀹氭Ы浣嶇殑鐗╁搧锛坽@code stack} 涓虹┖鍒欑Щ闄よ妲戒綅鏉＄洰锛夈€?
	 * 渚涘揩鎹锋姇鏀炬爮娑堣€楄嵂姘村悗鏇存柊瀛樺偍锛屼笌 GUI 鐨?{@link #saveToNbt} 浣跨敤鍚屼竴 NBT 缁撴瀯銆?
	 */
	public static void setStoredStack(ItemStack bagStack, int slot, ItemStack stack) {
		CompoundTag nbt = bagStack.getOrCreateTag();
		ListTag list = nbt.contains("Items", 9) ? nbt.getList("Items", 10) : new ListTag();
		for (int i = list.size() - 1; i >= 0; --i) {
			if ((list.getCompound(i).getByte("Slot") & 255) == slot) {
				list.remove(i);
			}
		}
		if (!stack.isEmpty()) {
			CompoundTag itemTag = new CompoundTag();
			itemTag.putByte("Slot", (byte) slot);
			stack.save(itemTag);
			list.add(itemTag);
		}
		nbt.put("Items", list);
	}

}
