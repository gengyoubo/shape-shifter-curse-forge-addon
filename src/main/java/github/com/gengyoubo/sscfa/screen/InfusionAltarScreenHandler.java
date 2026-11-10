package github.com.gengyoubo.sscfa.screen;

import github.com.gengyoubo.sscfa.block.RegAddonBlockEntities;
import github.com.gengyoubo.sscfa.item.MoonDustSpellbookItem;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/**
 * 婵炲鍔戦悺鐔煎矗閹殿喗娅曢梻鍫涘灩椤旀劙宕抽妸锝傚亾閸屾瑧鐦堟俊鍙夋灮缁?=濞戞搩鍘奸妵搴㈩洨閺冣偓绾埖绋婇敂琛″亾?=闁绘洖鍟弸锟犲Υ?=闁稿矈鍓欑€垫煡濡?-8=闁稿浚鍙€婵啴寮伴悢鍝ャ€婇梻鍐х祷椤危婵劏鍋?
 * 闁稿繐鎳撻崗姗€鎮芥潏銊︾厵闁秆勵殔閻ゅ嫭鎷呴幘瀵告Ж缂佸甯炵划銊х不濡ゅ绀夐柛妤€娲ㄦ鍥閳ь剚绋夌拠鎻捫楅柣鎰嚀閸ゎ噣鏁嶅☉姘珪闂傚牄鍨肩粈瀣嫻閿濆棙鏉归柛娆愮墱婢у潡宕担椋庣憿閻忕偞娲滈妵姘▕閿斿じ绻嗛柟顓у灛閳?
 *
 * <p>闁稿浚鍙€婵啴寮伴悢娲绘健婵℃彃鈧噥娼愰柛鎺撶懕缁辩増绋婇敂鍙偊寮悩鍗炲闁哄啫鐖奸弨锝団偓瑙勭啲缁遍亶寮垫径澶婂闁哄啳鍩栫€垫粍绋婇敂鍓ф惣缂佺嫏鍠版帡鏌ㄦ担绛嬫健濞达絽绋勭槐娆愮▔閳ь剛鐥閸?1 閻?/ 濞存粌鐬兼?3 閻?/ 濞戞挸顦辨?6 閻熸瑦甯槐姘舵晬?
 * 闂佸じ绀侀悾鍓ф喆?canInsert=false闁挎稑鐗嗚ぐ鏌ュ矗閺嵮冩瘔濞戞挸绉磋ぐ鏌ュ绩閹冨汲闁挎稑顦埀顒€鍊介～妤€危閼恒儲鏆柛鏂诲妿缁?Inventory.setStack 闁搞儳鍋犻惃鐔哥▔婢跺鍋撴担鍛婂€辨慨婵勫劙閸?NBT闁?/p>
 */
public class InfusionAltarScreenHandler extends AbstractContainerMenu {
	public static final int GUI_WIDTH = 312, GUI_HEIGHT = 232;
	public static final int FORMATION_END = 3 + SpellbookData.MAX_FORMATION_SLOTS;
	public static final int MAINTENANCE_INK = FORMATION_END, MAINTENANCE_SCHEME = FORMATION_END + 1, MAINTENANCE_SCROLL = FORMATION_END + 2;
	public static final int ALTAR_SLOT_COUNT = FORMATION_END + 3;
	public static final int CONFIGURE = 20, MAINTENANCE = 21;
	private boolean maintenance;
	public boolean isMaintenance() { return maintenance; }
	public void setMaintenance(boolean value) { maintenance = value; }
	@Override public boolean clickMenuButton(Player player, int action) {
		if ((action != CONFIGURE && action != MAINTENANCE) || !stillValid(player)) return false;
		maintenance = action == MAINTENANCE; return true;
	}
	public static final int INVENTORY_X = 12, INVENTORY_Y = 148, HOTBAR_Y = 206;
	public static final int BOOK_X = 88, BOOK_Y = 63;
	public static final int FUEL_X = 212, CATALYST_X = 264, MATERIAL_Y = 110;
	private final Container inventory;

	/** 濞?C2S 闁告娲ㄦ鍥礌閸涱厾鏆板ù锝呯У閺佺偞顩╅弬鍨叡闁哄倻鎳撳锛勨偓鍦仒缂嶅鏁嶉崼鐔哥疀闁告棑绱曢顒勫级閸愩劉鏋嗛梺鎻掔Ч閻涙瑩鎮介…鎺旂闁?*/
	public Container getInventory() {
		return this.inventory;
	}

	/** 婵繐绲介崣姘舵嚍閹烘挻袝閻熸瑦甯楄潕闁挎稒鐭懙鎴ｇ疀?(96,71)闁挎稑鑻畷鎰嚗?34闁挎稒绋戞稊蹇涘矗閸愭彃纾归悗纭呮瑜?34閼寸qrt(3)/2 闁汇劌瀚弳锝夊极閹峰瞼绠ù鑲╊儠閳?*/
	public static final int[][] HEXAGRAM_SLOT_POS = {
			{88, 29},  // 0 濡炪倖鍎奸～妤呮晬閸︾垝1 閻熸瑱缍侀弨锝夋晬?
			{59, 46},  // 1 鐎归潻缂氱粭鍌炴晬閸︾垝2 閻熸瑱缍侀弨锝夋晬?
			{117, 46}, // 2 闁告瑥鍘栫粭鍌炴晬閸︾垝2 閻熸瑱缍侀弨锝夋晬?
			{59, 80},  // 3 鐎归潻缂氱粭鍛存晬閸︾垝3 閻熸瑱缍侀弨锝夋晬?
			{117, 80}, // 4 闁告瑥鍘栫粭鍛存晬閸︾垝3 閻熸瑱缍侀弨锝夋晬?
			{88, 97}   // 5 閹煎瓨娲濋～妤呮晬閸︾垝3 閻熸瑱缍侀弨锝夋晬濞戞绠介柣锝嗙懃鐢偊寮垫径澶屽畨婵¤尙鏅▓?NBT 缂傚倹鐗曡ぐ鍧楁晬?
	};

	public InfusionAltarScreenHandler(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, new SimpleContainer(ALTAR_SLOT_COUNT));
	}

	public InfusionAltarScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
		super(RegAddonBlockEntities.INFUSION_ALTAR_SH, syncId);
		checkContainerSize(inventory, ALTAR_SLOT_COUNT);
		this.inventory = inventory;
		inventory.startOpen(playerInventory.player);

		// 濞戞棑闄勮潕闁挎稑鐗嗛崣姘舵嚍閹烘挻袝濞戞搩鍘肩缓楣冩晬?
		this.addSlot(new Slot(inventory, 0, BOOK_X, BOOK_Y) {
			@Override public boolean isActive() { return !maintenance; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.getItem() instanceof MoonDustSpellbookItem;
			}

			@Override
			public int getMaxStackSize(ItemStack stack) {
				return 1;
			}
		});
		// 闁哄鍔栭弸锟犲礌閻氬绐楅柣鏇炲暞閺嬧剝绋婇悢绋跨倒濞撴碍绋戝畷宀€鐥婢у秹妫侀埀顒勬儍閸曨厼鍤遍柡鍛扮堪閳?
		this.addSlot(new Slot(inventory, 1, FUEL_X, MATERIAL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() == github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST
						|| stack.getItem() == github.com.gengyoubo.sscfa.compat.RegCustomItem.MOONDUST_CRYSTAL_SHARD;
			}
		});
		// 闁稿矈鍓欑€垫彃危閺傘倗绐楀┑澶嬪灥閼镐即寮界粙璺ㄥ闁?
		this.addSlot(new Slot(inventory, 2, CATALYST_X, MATERIAL_Y) {
			@Override public boolean isActive() { return !maintenance; }
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isActive() && stack.getItem() == github.com.gengyoubo.sscfa.compat.RegCustomItem.MORPHSCALE_CORE;
			}
		});
		// 闁稿浚鍙€婵啴寮伴悢鍝ャ€婇梻鍐х祷椤危?3-8
		for (int i = 0; i < SpellbookData.MAX_FORMATION_SLOTS; i++) {
			final int formationSlot = i;
			this.addSlot(new Slot(inventory, 3 + i, HEXAGRAM_SLOT_POS[i][0], HEXAGRAM_SLOT_POS[i][1]) {
				@Override public boolean isActive() { return !maintenance; }
				@Override
				public boolean mayPlace(ItemStack stack) {
					// 闁哄啰濮抽崝鐔煎箣閺嶎剦娼″ù锝呯У濠€顓犳喆閿濆鏁?闁?闂佸じ绀侀悾楣冩晬濞戞瑦绠掑☉鏃撶細缁楁牜鎲撮敐澶嬫暁 闁?闁告瑯浜濋弫鐟扳枖閺囥垺鈻?
					ItemStack book = container.getItem(0);
					if (!isActive() || !(book.getItem() instanceof MoonDustSpellbookItem) || book.isEmpty()) {
						return false;
					}
					if (!SpellbookData.isFormationSlotUnlocked(book, formationSlot)) {
						return false;
					}
					return FormationData.isFormation(stack) && !github.com.gengyoubo.sscfa.spell.ArcaneAnalysis.isUnanalyzed(stack);
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					return 1;
				}
			});
		}
		this.addSlot(new Slot(inventory, MAINTENANCE_INK, 117, 46) {
			@Override public boolean isActive() { return maintenance; }
			@Override public boolean mayPlace(ItemStack stack) { return isActive() && stack.getItem() instanceof github.com.gengyoubo.sscfa.item.FormationInkItem; }
		});
		this.addSlot(new Slot(inventory, MAINTENANCE_SCHEME, 59, 46) {
			@Override public boolean isActive() { return maintenance; }
			@Override public boolean mayPlace(ItemStack stack) { return isActive() && stack.is(github.com.gengyoubo.sscfa.SscAddon.SPELL_FORMATION); }
			@Override public int getMaxStackSize() { return 1; }
		});
		this.addSlot(new Slot(inventory, MAINTENANCE_SCROLL, BOOK_X, BOOK_Y) {
			@Override public boolean isActive() { return maintenance; }
			@Override public boolean mayPlace(ItemStack stack) { return isActive() && stack.getItem() instanceof github.com.gengyoubo.sscfa.item.MagicScrollItem; }
			@Override public int getMaxStackSize() { return 1; }
		});

		// 闁绘壕鏅涢宥夋嚄鐏炶棄鐦?
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
			}
		}
		// 闊浂鍋呭畵搴ㄥ冀?
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
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
		if (slot != null && slot.isActive() && slot.hasItem()) {
			ItemStack original = slot.getItem();
			newStack = original.copy();
			if (index < ALTAR_SLOT_COUNT) {
				if (!this.moveItemStackTo(original, ALTAR_SLOT_COUNT, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (maintenance) {
				int target = original.getItem() instanceof github.com.gengyoubo.sscfa.item.MagicScrollItem ? MAINTENANCE_SCROLL
						: original.is(github.com.gengyoubo.sscfa.SscAddon.SPELL_FORMATION) ? MAINTENANCE_SCHEME
						: original.getItem() instanceof github.com.gengyoubo.sscfa.item.FormationInkItem ? MAINTENANCE_INK : 1;
				if (!this.moveItemStackTo(original, target, target + 1, false)) return ItemStack.EMPTY;
			} else if (original.getItem() instanceof MoonDustSpellbookItem) {
				// 濞村吋锚閸樻稒绋婇敂鍙?
				if (!this.moveItemStackTo(original, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (FormationData.isFormation(original)) {
				// shift 婵炲娲熷Ο鈧?闁?閻忓繑绻嗛惁顖炲礂椤撯€愁潏闁哄嫮鍠曢～妤€危?
				if (!this.moveItemStackTo(original, 3, FORMATION_END, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!this.moveItemStackTo(original, 1, 3, false)) {
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
