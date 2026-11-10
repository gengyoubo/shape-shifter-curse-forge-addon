package github.com.gengyoubo.sscfa.screen;

import github.com.gengyoubo.sscfa.block.RegAddonBlockEntities;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.item.MagicScrollItem;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/**
 * 婵炲娲樺﹢鎶芥儘閺冨倵鏁€闁告瑦澹嗛弲顐︽閵忕媭鍟囬柛锝庣厜缁辨獚ackcooper闁挎稑顦埀顒€鍊风花鎻捨ｉ弬銈囩獥0=缂佸矁娅ｅ▍褍鈻旈弴銏♀枅缂佺偓鎮堕埀?=婵炲瞼鎳撻埅鐑藉Υ?=闁哄牆鐗嗛惃褰掑Υ?=濞存籂鍐ㄦ瘔闁?=闁哄牆鐗嗛惃鍦棯椤栨稒鐝烽柕?
 * 闁硅泛瀚崯?閻庢冻缂氱弧鍕偨闁秲鈧绮甸悙顒€鐦婚梺?C2S 闁告牕鎳橀埞宥夊礉椤帞绀勯柡鍫濈Т婵喓绮╅娑欑秬濠电偘绶氶崳鍛婎殽鐏炶姤鍊甸柟纰夌稻濞兼寮▎娆戠闁挎稑鐬奸弲顐︽閵忕姴娑ч悹鎰枙閻寮ㄩ幆褍绲块柣妞绘櫅閹佳囧Υ?
 */
public class SpellResearchTableScreenHandler extends AbstractContainerMenu {
	public static final int GUI_WIDTH = 312;
	public static final int GUI_HEIGHT = 212;
	public static final int INVENTORY_X = 76;
	public static final int RESEARCH_OUTPUT_X = 285, RESEARCH_OUTPUT_Y = 109;
	private final Container inventory;
	private final ContainerData analysis;
	private int activePage;
	public static final int UPGRADE_ON = 5, UPGRADE_OFF = 6;
	private boolean runeUpgrade;
	public boolean isRuneUpgrade() { return runeUpgrade; }
	public void setRuneUpgrade(boolean value) { runeUpgrade = value; }
	public void setActivePage(int page) { if (page >= 0 && page <= 4) activePage = page; }
	@Override public boolean clickMenuButton(Player player, int page) {
		if ((page == UPGRADE_ON || page == UPGRADE_OFF) && stillValid(player)) {
			runeUpgrade = page == UPGRADE_ON; return true;
		}
		if (page < 0 || page > 4 || !stillValid(player)) return false;
		activePage = page; return true;
	}
	public int getAnalysisTicks() { return analysis.get(0); }

	/** 濞?C2S 闁告牕鎳庨悾鐐媴瀹ュ洨鍩＄紒宀冩硾瑜版挳寮悷鐗堝仴閻庡湱鍋樼紞瀣晬閸喐绠涢柛鏃撶磿椤忣剟寮堕崘銊㈡瀱闂佹彃绉归悰娆撴偨椤帞绀嗛柕?*/
	public Container getInventory() {
		return this.inventory;
	}

	public SpellResearchTableScreenHandler(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, new SimpleContainer(SpellResearchTableBlockEntity.SLOT_COUNT));
	}

	public SpellResearchTableScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
		super(RegAddonBlockEntities.SPELL_RESEARCH_TABLE_SH, syncId);
		checkContainerSize(inventory, SpellResearchTableBlockEntity.SLOT_COUNT);
		this.inventory = inventory;
		this.analysis = inventory instanceof SpellResearchTableBlockEntity table ? new ContainerData() {
			public int get(int index) { return table.getAnalysisTicks(); }
			public void set(int index, int value) { }
			public int getCount() { return 1; }
		} : new SimpleContainerData(1);
		addDataSlots(this.analysis);
		inventory.startOpen(playerInventory.player);

		this.addSlot(new Slot(inventory, 0, 154, 72) {
			@Override public boolean isActive() { return activePage != 2; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.getItem() instanceof BlankFormationPaperItem;
			}
		});
		this.addSlot(new Slot(inventory, 1, 182, 72) {
			@Override public boolean isActive() { return activePage != 0 && activePage != 2; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.getItem() instanceof FormationInkItem;
			}
		});
		this.addSlot(new Slot(inventory, 2, 210, 72) {
			@Override public boolean isActive() { return activePage != 0 && activePage != 2; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.getItem() == github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST;
			}
		});
		this.addSlot(new Slot(inventory, 3, 282, 72) {
			@Override public boolean isActive() { return activePage != 2; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addSlot(new Slot(inventory, SpellResearchTableBlockEntity.SLOT_CATALYST, 240, 72) {
			@Override public boolean isActive() { return activePage != 0 && activePage != 2; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.MOONDUST_CRYSTAL_SHARD);
			}
		});

		this.addSlot(new Slot(inventory, SpellResearchTableBlockEntity.SLOT_ANALYSIS, 32, 72) {
			@Override public boolean isActive() { return activePage == 0; }
			@Override public boolean mayPlace(ItemStack stack) { return isActive() && SpellResearchTableBlockEntity.acceptsAnalysisInput(stack); }
			@Override public int getMaxStackSize() { return 1; }
		});
		this.addSlot(new Slot(inventory, SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT, RESEARCH_OUTPUT_X, RESEARCH_OUTPUT_Y) {
			@Override public boolean isActive() { return activePage == 1; }
			@Override public boolean mayPlace(ItemStack stack) { return false; }
			@Override public int getMaxStackSize() { return 1; }
		});
		// Appended after existing IDs so old table inventories retain every item.
		this.addSlot(new Slot(inventory, SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL, 249, 90) {
			@Override public boolean isActive() { return activePage == 1 && runeUpgrade; }
			@Override public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.getItem() instanceof MagicScrollItem
						&& ScrollData.getSpell(stack) != null;
			}
			@Override public int getMaxStackSize() { return 1; }
		});
		// 闁绘壕鏅涢宥夋嚄鐏炶棄鐦?
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INVENTORY_X + col * 18, 130 + row * 18));
			}
		}
		// 闊浂鍋呭畵搴ㄥ冀?
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, 188));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.inventory.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.isActive() && slot.hasItem()) {
			ItemStack original = slot.getItem();
			newStack = original.copy();
			if (index < SpellResearchTableBlockEntity.SLOT_COUNT) {
				if (!this.moveItemStackTo(original, SpellResearchTableBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				boolean routed = activePage == 1 && runeUpgrade && original.getItem() instanceof MagicScrollItem
						? this.moveItemStackTo(original, SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL, SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL + 1, false)
						: activePage == 0
						? !this.getSlot(SpellResearchTableBlockEntity.SLOT_ANALYSIS).hasItem()
								&& this.moveItemStackTo(original, SpellResearchTableBlockEntity.SLOT_ANALYSIS, SpellResearchTableBlockEntity.SLOT_ANALYSIS + 1, false)
								|| this.moveItemStackTo(original, 0, 1, false)
						: this.moveItemStackTo(original, 3, 4, false)
						|| this.moveItemStackTo(original, SpellResearchTableBlockEntity.SLOT_CATALYST, SpellResearchTableBlockEntity.SLOT_CATALYST + 1, false)
						|| this.moveItemStackTo(original, 0, 1, false)   // 缂?
						|| this.moveItemStackTo(original, 1, 2, false)          // 濠?
						|| this.moveItemStackTo(original, 2, 3, false);         // 閻?
				if (!routed) {
					return ItemStack.EMPTY;
				}
			}
			if (original.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return newStack;
	}
}
