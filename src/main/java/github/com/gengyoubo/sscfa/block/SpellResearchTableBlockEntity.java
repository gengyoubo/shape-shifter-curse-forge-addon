package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.MagicScrollItem;
import github.com.gengyoubo.sscfa.item.FormationItem;
import github.com.gengyoubo.sscfa.item.AnalyzedSpellDiagramItem;
import github.com.gengyoubo.sscfa.spell.*;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import org.jetbrains.annotations.Nullable;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/**
 * 婵炲娲樺﹢鎶芥儘閺冨倵鏁€闁告瑧澧楅弻鐔煎锤濡も偓閻ゅ嫭鎷呴幙鍕jackcooper闁挎稑顦埀顒€鍊风花鎻捨ｉ弬銈囩獥0=缂佸矁娅ｅ▍褍鈻旈弴銏♀枅缂佺偓鎮堕埀?=婵炲瞼鎳撻埅鐑藉Υ?=闁哄牆鐗嗛惃褰掓晬閸繍鍔呭☉鏃傚Ь閳ь剚顨嗗妤呮晬婢跺牃鍋?=濞存籂鍐ㄦ瘔闁?=闁哄牆鐗嗛惃鍦棯椤栨稒鐝烽柕?
 *
 * <p>闁硅泛瀚崯鎾绘晬閸喎鐦婚梺?C2S 濡炵懓宕慨鈺呮晬鐏炵偓绠涢柛鏃撶磿椤忣剟寮堕崘銊㈡瀱闂佹彃绉归悰娆撴晬婢舵稓绐楅柤?缂佹崘顕? + 閻庣數鎳撶花鑼寲缂佹藟濠⒀岀厑濡ゅ啰鎼肩紒?闁?濞存籂鍐ㄦ瘔閻庣數鎳撶花鑼驳婢跺矂鐛撴繛澶嬫礋濡偓闁?
 * 閻庢冻缂氱弧鍕晬閸繃鍊卞☉鎾愁煭缁辨岸鏁嶅鏂ュ亾?闁哄牜浜滄慨鐐差啅閵夛附绠戝☉鏂款儏閻?閼?2閼村磭绮垫径宀勭崜) 闁?閻庢冻缂氱弧鍕啅閼奸鍞剁憸鐗堟礃绾爼姊奸悙鍏夊亾?
 * 闁衡偓椤栨稑鐦繝鏇炵箲閺嬬喖鏁嶅顐ょ憪闁哄倻鎳撻幏鐗堢瑹瑜斿浼村矗椤栨艾寮崇紒?濠?閻?缂佺虎鍨卞▍鐘绘晬鐏炶偐鐟撻柡鍌滄嚀瑜把囧礄鏉炴壆鐟濋弶鈺傜◤閳?/p>
 */
public class SpellResearchTableBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

	public static final int SLOT_PAPER = 0;
	public static final int SLOT_INK = 1;
	public static final int SLOT_MOONDUST = 2;
	public static final int SLOT_OUTPUT = 3;
	public static final int SLOT_CATALYST = 4;
	public static final int SLOT_ANALYSIS = 5;
	public static final int SLOT_RESEARCH_OUTPUT = 6;
	public static final int SLOT_UPGRADE_SCROLL = 7;
	public static final int SLOT_COUNT = 8;
	public static final int ANALYSIS_TICKS = 200;
	private int analysisTicks;
	private ItemStack analysisInput = ItemStack.EMPTY;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

	public SpellResearchTableBlockEntity(BlockPos pos, BlockState state) {
		super(RegAddonBlockEntities.SPELL_RESEARCH_TABLE_BE, pos, state);
	}

	// ---- NamedScreenHandlerFactory ----
	@Override
	public Component getDisplayName() {
		return Component.translatable("block.ssc_addon.spell_research_table");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new SpellResearchTableScreenHandler(syncId, playerInventory, this);
	}

	// ---- NBT ----
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		items.clear();
		ContainerHelper.loadAllItems(nbt, items);
		// Keep the old slot indices; only move pending research products into the appended output.
		if (items.get(SLOT_OUTPUT).is(github.com.gengyoubo.sscfa.SscAddon.SPELL_FORMATION)
				&& items.get(SLOT_RESEARCH_OUTPUT).isEmpty()) {
			items.set(SLOT_RESEARCH_OUTPUT, items.get(SLOT_OUTPUT));
			items.set(SLOT_OUTPUT, ItemStack.EMPTY);
		}
		analysisTicks = Math.max(0, Math.min(ANALYSIS_TICKS, nbt.getInt("AnalysisTicks")));
		analysisInput = ItemStack.of(nbt.getCompound("AnalysisInput"));
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		ContainerHelper.saveAllItems(nbt, items);
		nbt.putInt("AnalysisTicks", analysisTicks);
		nbt.put("AnalysisInput", analysisInput.save(new CompoundTag()));
	}

	public int getAnalysisTicks() { return analysisTicks; }
	public enum AnalysisStatus { EMPTY, UNSUPPORTED, NO_PAPER, OUTPUT_FULL, READY, COMPLETE }
	public static boolean acceptsAnalysisInput(ItemStack stack) {
		return stack.getItem() instanceof MagicScrollItem && ScrollData.getSpell(stack) != null
				|| stack.getItem() instanceof FormationItem && FormationData.getElement(stack) != null && ArcaneAnalysis.isUnanalyzed(stack);
	}
	public static AnalysisStatus analysisStatus(net.minecraft.world.Container inventory) {
		return analysisStatus(inventory, inventory instanceof SpellResearchTableBlockEntity table ? table.getAnalysisTicks() : 0);
	}
	public static AnalysisStatus analysisStatus(net.minecraft.world.Container inventory, int ticks) {
		ItemStack input = inventory.getItem(SLOT_ANALYSIS);
		Spell spell = ScrollData.getSpell(input);
		if (!acceptsAnalysisInput(input)) return AnalysisStatus.EMPTY;
		if (input.getCount() != 1) return AnalysisStatus.UNSUPPORTED;
		if (input.getItem() instanceof MagicScrollItem && !ArcaneAnalysis.isUnanalyzed(input)
				&& !SlottedSpellRecipes.available(SlottedSpellRecipes.get(spell.getId().getPath()), ScrollData.getLevel(input))) return AnalysisStatus.UNSUPPORTED;
		if (!inventory.getItem(SLOT_OUTPUT).isEmpty()) return AnalysisStatus.OUTPUT_FULL;
		if (ticks == ANALYSIS_TICKS) return AnalysisStatus.COMPLETE;
		if (!(inventory.getItem(SLOT_PAPER).getItem() instanceof BlankFormationPaperItem)) return AnalysisStatus.NO_PAPER;
		return AnalysisStatus.READY;
	}
	public void advanceAnalysis(WorldRuneState language) {
		ItemStack input = getItem(SLOT_ANALYSIS);
		if (input.getTag() != null && input.getTag().contains(RuneScheme.KEY)
				&& RuneScheme.authoritativeScroll(language, input) == null) { resetAnalysis(); return; }
		RuneScheme.normalizeUnmodified(language, input);
		if (analysisTicks == ANALYSIS_TICKS && ItemStack.matches(input, analysisInput)) return;
		if (analysisStatus(this) != AnalysisStatus.READY) {
			resetAnalysis(); return;
		}
		if (!ItemStack.matches(input, analysisInput)) { analysisTicks = 0; analysisInput = input.copy(); }
		analysisTicks++;
		if (analysisTicks < ANALYSIS_TICKS) { if (analysisTicks % 20 == 0) setChanged(); return; }
		ItemStack result = input.getItem() instanceof MagicScrollItem
				? AnalyzedSpellDiagramItem.create(language, input) : ItemStack.EMPTY;
		boolean returnOriginal = result.isEmpty() && ArcaneAnalysis.isUnanalyzed(input);
		if (returnOriginal) { result = input.copy(); ArcaneAnalysis.identify(result); }
		if (result.isEmpty()) { resetAnalysis(); return; }
		getItem(SLOT_PAPER).shrink(1);
		if (returnOriginal) setItem(SLOT_ANALYSIS, ItemStack.EMPTY);
		else ArcaneAnalysis.identify(input);
		setItem(SLOT_OUTPUT, result);
		if (returnOriginal) resetAnalysis();
		else {
			analysisTicks = ANALYSIS_TICKS;
			analysisInput = input.copy();
		}
		setChanged();
	}
	private void resetAnalysis() {
		if (analysisTicks != 0 || !analysisInput.isEmpty()) { analysisTicks = 0; analysisInput = ItemStack.EMPTY; setChanged(); }
	}

	// ---- Inventory ----
	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack s : items) {
			if (!s.isEmpty()) {
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
		if (slot == SLOT_ANALYSIS) resetAnalysis();
		return ContainerHelper.removeItem(items, slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot == SLOT_ANALYSIS) resetAnalysis();
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == SLOT_ANALYSIS) resetAnalysis();
		items.set(slot, stack);
		if (stack.getCount() > getMaxStackSize()) {
			stack.setCount(getMaxStackSize());
		}
		setChanged();
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && !level.isClientSide) {
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
				&& player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clearContent() {
		resetAnalysis();
		items.clear();
	}

	/** 濞撴碍绋掗弻鐔煎锤濡ゅ啰澹冮柛褍绻戝鍌炲极閿濆牊鍎伴柣妞绘櫅閹佳囧Υ?*/
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	// ---- SidedInventory闁挎稑鐗婄槐锟犲棘濡や焦鏆滈柟闀愮筏缁?----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{SLOT_OUTPUT, SLOT_RESEARCH_OUTPUT} : new int[]{SLOT_PAPER, SLOT_INK, SLOT_MOONDUST, SLOT_CATALYST, SLOT_ANALYSIS};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
		if (slot == SLOT_ANALYSIS) return acceptsAnalysisInput(stack);
		if (slot == SLOT_PAPER) {
			return stack.getItem() instanceof BlankFormationPaperItem;
		}
		if (slot == SLOT_INK) {
			return stack.getItem() instanceof FormationInkItem;
		}
		if (slot == SLOT_MOONDUST) {
			return stack.getItem() == github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST;
		}
		if (slot == SLOT_CATALYST) {
			return stack.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.MOONDUST_CRYSTAL_SHARD);
		}
		return false; // 濞存籂鍐ㄦ瘔婵″弶鍨濈粭澶愬矗椤栨艾寮?
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
		return slot == SLOT_OUTPUT || slot == SLOT_RESEARCH_OUTPUT; // 闁告瑯浜滈崢鎴犳媼閸涘﹤鈻曢柛鎴ｆ〃妤犲洭宕?
	}
}
