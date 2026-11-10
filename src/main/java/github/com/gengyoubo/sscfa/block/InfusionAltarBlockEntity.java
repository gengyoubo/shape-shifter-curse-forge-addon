package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.item.MoonDustSpellbookItem;
import github.com.gengyoubo.sscfa.screen.InfusionAltarScreenHandler;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * 婵炲鍔戦悺鐔煎矗閻楀牊鐓欓柛褎顨呴悿鍕媴閹垮嫮绀刯ackcooper闁挎稑顦埀顒€鍊风弧鈧俊鍙夋灮缁?=濮掑嫭姊圭涵鑸电▕閿旇　鍋?=闁绘洖鍟弸锟犳晬閸喐绠戦悘蹇旑焽閻?闁哄牆鐗嗛惃鍦棯椤栨稒鐝烽柨娑橆槶閳?=闁稿矈鍓欑€垫煡鏁嶉崼婵愭晞鐟滆埇鍨洪悧瀹犵疀閸愯法绀嗛柕?
 * 3-8=闁稿浚鍙€婵啴寮伴悢鍝ャ€婇梻鍐ㄧ仛铦庨柨娑樼墔閸旂喖寮ㄩ幆褍寮冲☉鎿冨幖缁洪箖寮捄鍝勑楅柟顑跨劍濡绮堟潪鏉垮闁告劕鎳庨崙锛勬啑閸涱収妲垫繛澶嬫礋濡偓闁挎稑顦埀?
 *
 * <p>閻犲洦娲熼悰娆撴偨閺夊灝娑уǎ鍥ㄧ箘閺嗏偓闁告娲ㄦ鍥ㄧ▔鎼淬垻銆婇梻鍐暬閸樸倗绱旈鍡欑濞戞挸绉撮崯鈧☉鎾跺劋濡偊鎷戝┑鍥ㄨ拫闁煎浜滄慨鈺佲槈閸絺鍋撳Δ鍐ㄦ珣闁哄倹鐟ｉ埀?/p>
 *
 * <p><b>闁稿浚鍙€婵啴寮伴悢宄拌摕闁告碍鍨甸幃鎾愁潰閵夘垳绀勫☉鎾愁槹閳ь兛绀侀幓鈺呮偂瑜濈槐?/b>闁挎稒鐭禍鎺楀Υ鐏炶偐鐟愬☉鎾亾婵炲棌鈧櫕鍊辨慨婵勫劥缁绘ɑ绋婇敂鐐暠闁告劕鎳庨鎰板Υ瀹ュ嫯绀嬮柛鈺佹惈閸ｎ垳鈧絻顫夐惁顕€鍨鹃弬琛″亾?
 * 婵¤尪妫勮ぐ澶愭晬閸垹璐熼悗纭呮硾濠€顏嗘喆閹烘挃顐﹀绩?闁告瑦鐗楃涵鍫曟⒓绾板绀嗛柍?闁硅泛锕ヨ潕闁告劕鎳庨鎰板礃濞嗗繑绀€濞?NBT闁挎稒绋愰崝鐔煎矗濮楀牏绀勯柣婧炬櫅椤斿秹鎯勭€涙ê澶嶉柟璇℃線缁ㄢ剝绋婇敂鑲╃闁?闁硅泛锕ラ弻濠冪▕?NBT 闁告梻濮惧ù鍥ㄦ交濞戞粠娼℃俊鎻掝潟閳?
 * 闁圭柉鍎婚摂瀣▕閿曗偓閹鎲撮幒鎾旑偄銆掗崨顖楁晞闁挎稑鐗婄涵鍫曟⒓闂堟稑鍤掗梻鍛箣閸旂喓鏁敃浣芥巢闁挎稑顧€缁辨繃绋夌€ｂ晝顏遍柡鍫厸閸旂喖寮ㄩ幆褍寮抽柡鍐ㄧ埣閸ｆ悂寮弶鍨潱閺夌偛顫曢埀?/p>
 */
public class InfusionAltarBlockEntity extends BlockEntity implements MenuProvider, Container {

	private final NonNullList<ItemStack> items = NonNullList.withSize(InfusionAltarScreenHandler.ALTAR_SLOT_COUNT, ItemStack.EMPTY);
	/** 濞戞挸锕ｇ粩鏉戔枎閳ヨ櫕鍊辨慨婵勫劥缁绘ɑ绋婇敂鐐暠闁稿浚鍙€婵啴寮伴悢宄版暥閻庡湱鎳撻幓鈺呮偂瑜濈槐妾攗ll = 濞寸姴瀛╁﹢顓㈠触鐏炵虎鍔勯柨娑樼焸濞撳墎鎲版担绋跨仴濠殿喖顑呮慨鐐存姜閺傘倗绀嗛柕?*/
	@Nullable
	private CompoundTag lastSyncedFormations;

	public InfusionAltarBlockEntity(BlockPos pos, BlockState state) {
		super(RegAddonBlockEntities.INFUSION_ALTAR_BE, pos, state);
	}

	/** 濞戞棑绠戝畷宀€鐥缁盯骞愭径濠傛枾缂備礁绻橀悰娆撳椽鐏炵偓缍忛柡鍌涚懆琚欓梺澶哥劍铦庡ù锝呯▌缁辨繃绋夊鍡樻毉闁告瑦顭囩敮铏光偓纭呭煐婢规瑦娼崐鐔风槺闁搞儲鐣埀?*/
	private boolean upgradeReady() {
		ItemStack book=items.get(0),fuel=items.get(1),catalyst=items.get(2);
		return book.getItem() instanceof MoonDustSpellbookItem && SpellbookData.canLevelUp(book)
				&& catalyst.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.MORPHSCALE_CORE) && !catalyst.isEmpty()
				&& fuel.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.MOONDUST_CRYSTAL_SHARD) && !fuel.isEmpty();
	}

	public void tryUpgrade(Player player) {
		Level world=getLevel();
		if(world==null||world.isClientSide||!upgradeReady())return;
		items.get(2).shrink(1);
		items.get(1).shrink(1);
		ItemStack book=items.get(0);
		SpellbookData.setLevel(book,SpellbookData.getLevel(book)+1);
		SpellbookData.setExpTen(book,0);
		this.lastSyncedFormations=null;
		syncFormations();setChanged();
		world.playSound(null,worldPosition,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,0.8f,1.4f);
		if(player instanceof ServerPlayer sp&&sp.containerMenu!=null){
			sp.containerMenu.broadcastChanges();sp.containerMenu.broadcastFullState();
		}
	}

	public static void tick(Level world, BlockPos pos, BlockState state, InfusionAltarBlockEntity be) {
		if (world.isClientSide || world.getGameTime() % 20 != 0) {
			return;
		}
		// 闁稿浚鍙€婵啴寮伴悢宄拌摕闁告碍鍨甸幃鎾愁潰閵夘垳绀勬慨锝呯箳椤绱掗幘鍓佹毈濞戞挴鍋撴繛鍡忔缁辨繄鎲伴崱娆愮０闁靛棗鏈Λ?GUI 闁烩晛鐡ㄧ敮鎾绩閸欍儱濮?婵犳洖绻戦弸鐔煎绩閸欍儱濮涢柕鍡楃Т濠р偓闁哄拋鍨界槐?
		be.syncFormations();
        // The altar keeps upgrade and formation configuration. Detached books have no C to refill.
        // In particular, do not consume fuel for their retired Mana field.
    }

	// ---- 闁稿浚鍙€婵啴寮伴悢鍓佺憦闁诡兛绀侀幃鎾愁潰?----

	/**
	 * 闁稿浚鍙€婵啴寮伴悢宄拌摕闁告碍鍨甸幃鎾愁潰閵夘垳绐楅柟璺猴龚椤危?3-8)鐟滅増鎸告晶鐘诲礃閸涱収鍟囧☉?{@link #lastSyncedFormations} 閻庝絻顫夐惁顕€鏁?
	 * <ul>
	 *   <li><b>闁哄啰濮抽崝?/b>闁挎稒纰嶇粩鑽ょ矚閸濆嫭褰ラ柣鎾楀倻鐟㈤悷娆愬笚铦庨柡鍕⒔閵囨岸鏁嶉崼鐔恒€婇梻鍐涧閸戯繝姊捐箛搴″閻㈩垽绠掗摂瀣晬婢舵稓骞?/li>
	 *   <li><b>闁告帗绻傞～?/b>闁挎稑鐗嗛幓鈺呮偂瑜岀拹?null闁挎稑顧€缁辩増绋?NBT 闁?閻熸瑦甯楄潕闁?/li>
	 *   <li><b>婵¤尪妫勮ぐ?/b>闁挎稑鐗愰～妤€危閽樺鏁堕悗褰掆偓娑氱憿闊浂鍋嗛崣搴㈢▔瀹ュ嫮顏遍柤鐤彧缁辨岸鏁嶅鎰佹健婵?闁?濞?NBT闁?/li>
	 *   <li><b>濞戞棑绠戣ぐ?/b>闁挎稑鐗呴崝?NBT 闁?Formations 濞戞挸楠搁幓鈺呮偂瑜岀粭澶嬬▔閳ь剟鎳涙潏鍓х闁挎稒鐭崝?NBT 闁?閻熸瑦甯楄潕闁?/li>
	 * </ul>
	 * 濞戞棑缂氱粭灞轿ｉ挊澶嬪€遍柡鍐硾瑜板鏁嶉崼鐔衡偓顒備焊韫囨棏娲ら柣婊冩祫缁辨岸寮張鍏哥鞍婵¤尪妫勮ぐ澶嬪濡搫甯ラ柨娑樼墢鐢櫣鈧娉涢崹浼村箼瀹ュ嫮绋婇悗鐟扮灱濞堟垿鎮惧畝鍕〃闁绘鍩栭埀顑挎鐠愮喖宕欓崱顓犵闁?
	 */
	public void syncFormations() {
		if (level == null || level.isClientSide) {
			return;
		}
		ItemStack book = items.get(0);
		if (!(book.getItem() instanceof MoonDustSpellbookItem) || book.isEmpty()) {
			// 闁哄啰濮抽崝鐔兼晬濮橆厾顏搁煫鍥跺亞閸?+ 婵炴挸鎳愰埞鏍喆閹烘挃顐﹀及閸撗佷粵
			this.lastSyncedFormations = null;
			for (int i = 3; i < InfusionAltarScreenHandler.FORMATION_END; i++) {
				items.set(i, ItemStack.EMPTY);
			}
			return;
		}
		if (this.lastSyncedFormations == null) {
			// 闁告帗绻傞～鎰版晬濮橆偄濮?闁?婵?
			loadFromBook();
			return;
		}
		CompoundTag slotsSnapshot = slotsSnapshotOf();
		CompoundTag bookSnapshot = bookSnapshotOf(book);
		boolean slotsChanged = !slotsSnapshot.equals(this.lastSyncedFormations);
		boolean bookChanged = !bookSnapshot.equals(this.lastSyncedFormations);
		if (slotsChanged) {
			// 婵?闁?濞戞棑璁ｇ槐娆撴偝閳轰緡鍟€闁革负鍔忛～妤€危閼恒儲鏉?闁告瑦鐗旂花鈥斥枖閺囥垺鈻堥柨?
			for (int slot = 0; slot < SpellbookData.MAX_FORMATION_SLOTS; slot++) {
				ItemStack stack = items.get(3 + slot);
				SpellbookData.setFormation(book, slot,
						(!stack.isEmpty() && FormationData.isFormation(stack)) ? stack : ItemStack.EMPTY);
			}
			this.lastSyncedFormations = bookSnapshotOf(book);
			setChanged();
			level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.3f);
		} else if (bookChanged) {
			// 濞?闁?婵″弶鏋荤槐娆撳箲椤厼濮?/ 濞?NBT 濠㈣埖鐗犻崕鎾矗濡搫顕ч柨?
			loadFromBook();
		}
	}

	/** 濞?闁?婵¤尪妫勬慨鐐存姜閺傘倗绀夋鐐舵硾閸╂盯寮弶鎸庡渐闁绘挆浣插亾?*/
	private void loadFromBook() {
		ItemStack book = items.get(0);
		for (int slot = 0; slot < SpellbookData.MAX_FORMATION_SLOTS; slot++) {
			items.set(3 + slot, SpellbookData.getFormation(book, slot).copy());
		}
		this.lastSyncedFormations = bookSnapshotOf(book);
		setChanged();
	}

	/** 閻熸瑦甯楄潕(3-8)闁告劕鎳庨鎰疀椤愩倕寮鹃柨娑樻剑ey=閻熸瑦甯婄紞?0-4闁挎稑顦埀?*/
	private CompoundTag slotsSnapshotOf() {
		CompoundTag snapshot = new CompoundTag();
		for (int slot = 0; slot < SpellbookData.MAX_FORMATION_SLOTS; slot++) {
			ItemStack stack = items.get(3 + slot);
			if (!stack.isEmpty() && FormationData.isFormation(stack)) {
				CompoundTag tag = new CompoundTag();
				tag.putByte("Slot", (byte) slot);
				stack.save(tag);
				snapshot.put(String.valueOf(slot), tag);
			}
		}
		return snapshot;
	}

	/** 濞?NBT Formations 闁汇劌瀚幓鈺呮偂瑜濈槐娆愮瑹濞戞﹩鍤犳慨锝嗘缁辨岸濡?*/
	private static CompoundTag bookSnapshotOf(ItemStack book) {
		CompoundTag snapshot = new CompoundTag();
		for (int slot = 0; slot < SpellbookData.MAX_FORMATION_SLOTS; slot++) {
			ItemStack stack = SpellbookData.getFormation(book, slot);
			if (!stack.isEmpty()) {
				CompoundTag tag = new CompoundTag();
				tag.putByte("Slot", (byte) slot);
				stack.save(tag);
				snapshot.put(String.valueOf(slot), tag);
			}
		}
		return snapshot;
	}

	/** 闁绘壕鏅涢宥夊捶?GUI 闂佹彃鏈弫濂稿礉閵婎煈娼℃俊鑼舵閹鎮?ScreenHandler 閻犲鍟伴弫銈夋晬閸粎鐟濈紒?1 缂?tick闁挎稑鏈幖閿嬫媴濠婂啫绁柡鍐ㄥ閺佹捇寮崼顒傜闁?*/
	public void onFormationSlotsChanged() {
		syncFormations();
	}

	// ---- NamedScreenHandlerFactory ----
	@Override
	public Component getDisplayName() {
		return Component.translatable("block.ssc_addon.infusion_altar");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new InfusionAltarScreenHandler(syncId, playerInventory, this);
	}

	// ---- NBT ----
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		items.clear();
		ContainerHelper.loadAllItems(nbt, items);
		// 閻犲洨绮妴鍌炲触鎼粹剝褰ラ柣鎾楀啨浜奸柡浣哥墳缁辨繃锛冮弽顭戝仹 tick 闂佹彃绉甸弻濠囧礉閻樼儤绁?
		this.lastSyncedFormations = null;
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		ContainerHelper.saveAllItems(nbt, items);
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
		ItemStack result = ContainerHelper.removeItem(items, slot, amount);
		if (!result.isEmpty()) {
			setChanged();
			syncFormations();
		}
		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack result = ContainerHelper.takeItem(items, slot);
		if (!result.isEmpty()) {
			setChanged();
			syncFormations();
		}
		return result;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (stack.getCount() > getMaxStackSize()) {
			stack.setCount(getMaxStackSize());
		}
		setChanged();
		syncFormations();
	}

	@Override
	public boolean stillValid(Player player) {
		return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
				&& player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	/** 濞撴碍绋掗弻鐔煎锤濡ゅ啰澹冮柛褍绻戝鍌炲极閿濆牊鍎伴柣妞绘櫅閹佳囧Υ?*/
	public NonNullList<ItemStack> getItems() {
		NonNullList<ItemStack> drops=NonNullList.withSize(items.size(),ItemStack.EMPTY);
		boolean book=items.get(0).getItem() instanceof MoonDustSpellbookItem;
		for(int i=0;i<items.size();i++)
			if(!book||i<3||i>=InfusionAltarScreenHandler.FORMATION_END)drops.set(i,items.get(i).copy());
		return drops;
	}
}
