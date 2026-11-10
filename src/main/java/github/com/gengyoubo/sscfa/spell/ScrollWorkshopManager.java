package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.block.InfusionAltarBlockEntity;
import github.com.gengyoubo.sscfa.screen.InfusionAltarScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/** Scroll maintenance belongs to the preparation table; production and upgrading require runes. */
public final class ScrollWorkshopManager {
	private ScrollWorkshopManager() {}

	// 骞宠　杩佺Щ锛坰ystems.workshop锛夛細宸ュ潑鎴愭湰/涓婇檺杩愯鏃朵粠蹇収璇诲彇锛涘揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺
	private static final BalanceReader BAL = new BalanceReader("systems.workshop");

	/** 鍒朵綔绛夌骇涓婇檺锛圠v3+ 璧板崌绾х嚎锛岄槻姝㈢粫杩囪Е濯掗棬妲涚洿鎺ラ€犻珮闃讹級锛涢粯璁ゅ€硷紝balance 鍙皟銆?*/
	public static final int CRAFT_MAX_LEVEL = 2;

	/** 娌瑰ⅷ闇€姹傛暟 = 姣忕骇娌瑰ⅷ绯绘暟 脳 鐩爣绛夌骇锛堜笌娉曢樀鎶勫啓鍚屾儻渚嬶紱榛樿 1/绾э級銆?*/
	public static int upgradeInkCost(int level) {
		return (int) Math.round(BAL.d("ink_per_level", 1.0) * level);
	}

	/** 瑙﹀獟锛堟湀灏樼函鏅讹級闇€姹傛暟锛氱洰鏍囩瓑绾?- 鍋忕Щ锛堥粯璁?2锛屽嵆 Lv3=1銆丩v4=2銆丩v5=3锛夈€?*/
	public static int upgradeCatalystCost(int targetLevel) {
		return Math.max(0, targetLevel - BAL.i("catalyst_offset", 2));
	}
    public static int repairInkCost() { return BAL.i("repair_ink",1); }
    public static int repairDustCost() { return BAL.i("repair_dust",2); }

	/** 鍗疯酱绯诲埆瀵瑰簲娌瑰ⅷ绫诲瀷锛堟硶鏈?element 鈫?娌瑰ⅷ Type锛夈€?*/
	private static FormationInkItem.Type inkTypeOf(Spell spell) {
		FormationElement element = spell.getElement();
		if (element == null) {
			return FormationInkItem.Type.NORMAL;
		}
		for (FormationInkItem.Type t : FormationInkItem.Type.values()) {
			if (t.element == element) {
				return t;
			}
		}
		return FormationInkItem.Type.NORMAL;
	}

	/** 鐮旂┒鍙颁笂涓嬫枃锛堟墦寮€鐨勭晫闈?+ 鏂瑰潡瀹炰綋锛涗笉鍦ㄧ爺绌跺彴鍓?= 闈欓粯澶辫触锛夈€?*/
	private static InfusionAltarBlockEntity context(ServerPlayer player) {
		if (player.containerMenu instanceof InfusionAltarScreenHandler sh
				&& sh.isMaintenance() && sh.stillValid(player) && sh.getInventory() instanceof InfusionAltarBlockEntity be) {
			return be;
		}
		return null;
	}

	/** 娌瑰ⅷ妲芥槸鍚︿负鎸囧畾绫诲瀷涓旀暟閲忚冻澶熴€?*/
	private static boolean inkMatches(ItemStack ink, FormationInkItem.Type type, int count) {
		return ink.getItem() instanceof FormationInkItem inkItem
				&& inkItem.getType() == type && ink.getCount() >= count;
	}

    /** Retired packet entrypoints remain inert for older clients. */
    public static void craft(ServerPlayer player, String spellPath, int level) { requireRunes(player); }
    public static void upgrade(ServerPlayer player, String spellPath, int level) { requireRunes(player); }
    private static void requireRunes(ServerPlayer player) {
        if (player != null) player.displayClientMessage(Component.translatable("research.ssc_addon.slotted.manual_required"), true);
    }

	// ==================== 淇 ====================

	/**
	 * 淇鍗疯酱瀹屾暣搴︼細浜у嚭妲芥斁鏈弧娆℃暟鍗疯酱 + 瀵瑰簲绯诲ⅷ脳1 + 灏樏? 鈫?Uses 鍥炴弧銆?
	 * 绾㈡。锛坢axUses=0锛夋棤瀹屾暣搴﹁涔夛紝涓嶅弬涓庝慨澶嶃€?
	 */
	public static void repair(ServerPlayer player) {
		InfusionAltarBlockEntity be = context(player);
		if (be == null) {
			return;
		}
		ItemStack scroll = be.getItem(InfusionAltarScreenHandler.MAINTENANCE_SCROLL);
		if (ArcaneAnalysis.isUnanalyzed(scroll)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.analysis.required").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (scroll.isEmpty() || ScrollData.getSpell(scroll) == null) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.no_scroll").withStyle(ChatFormatting.RED), true);
			return;
		}
		int max = ScrollData.getMaxUses(scroll);
		if (max <= 0) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.cannot_repair").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		int uses = ScrollData.getUses(scroll);
		if (uses >= max) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.already_full").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		Spell spell = ScrollData.getSpell(scroll);
		FormationInkItem.Type inkType = inkTypeOf(spell);
		int repairInk = repairInkCost();
		ItemStack ink = be.getItem(InfusionAltarScreenHandler.MAINTENANCE_INK);
		if (!inkMatches(ink, inkType, repairInk)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.no_ink",
					Component.translatable(inkType.element == null
							? "element.ssc_addon.universal" : inkType.element.getNameKey()), repairInk)
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		int repairDust = repairDustCost();
		ItemStack dust = be.getItem(1);
		if (dust.getItem() != github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST || dust.getCount() < repairDust) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.no_dust", repairDust)
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		ink.shrink(repairInk);
		dust.shrink(repairDust);
		ScrollData.setUses(scroll, max);
		be.setChanged();
		player.level().playSound(null, be.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE,
				SoundSource.BLOCKS, 0.8f, 1.6f);
		player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.repaired",
				Component.translatable(spell.getNameKey())).withStyle(ChatFormatting.GREEN), true);
	}

	// ==================== 鎷嗚В ====================

	/**
	 * 鎷嗚В鍗疯酱锛氳繑杩樻湀灏樏梒eil(绛夌骇/2)锛堢害鏅€氭潗鏂欎环鍊肩殑 25%锛屄?.4銆?0-30%銆嶅尯闂村唴锛夈€?
	 * 涓嶈繑姣曚笟鍑瘉锛堝綋鍓嶆棤鍑瘉鐗╁搧锛岃Е濯掍笉杩旓級銆傚彛琚嬬┖闂村嵎杞寸粰缁戝畾淇濈暀鎻愮ず銆?
	 */
	public static void salvage(ServerPlayer player) {
		InfusionAltarBlockEntity be = context(player);
		if (be == null) {
			return;
		}
		ItemStack scroll = be.getItem(InfusionAltarScreenHandler.MAINTENANCE_SCROLL);
		if (scroll.isEmpty() || ScrollData.getSpell(scroll) == null) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.no_scroll").withStyle(ChatFormatting.RED), true);
			return;
		}
		Spell spell = ScrollData.getSpell(scroll);
		int level = ScrollData.getLevel(scroll);
		// 鍙ｈ绌洪棿缁戝畾鎻愮ず锛堜笉闃绘鎷嗚В锛涚┖闂翠繚鐣欎笉鑷姩鍒犻櫎锛?
		if (spell.getId().getPath().equals("pocket_space")) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.salvage_pocket_hint")
					.withStyle(ChatFormatting.YELLOW), false);
		}
		int divisor = Math.max(1, BAL.i("salvage_divisor", 2));
		int refund = (level + divisor - 1) / divisor;
		scroll.shrink(1);
		ItemStack dust = new ItemStack(github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST, refund);
		if (!player.getInventory().add(dust)) {
			player.drop(dust, false);
		}
		be.setChanged();
		player.level().playSound(null, be.getBlockPos(), SoundEvents.GRINDSTONE_USE,
				SoundSource.BLOCKS, 1.0f, 0.8f);
		player.displayClientMessage(Component.translatable("message.ssc_addon.workshop.salvaged",
				Component.translatable(spell.getNameKey()), refund).withStyle(ChatFormatting.GREEN), true);
	}

    public static void imprint(ServerPlayer player){
        var table=context(player);if(table==null)return;
        int result=github.com.gengyoubo.sscfa.spell.research.RuneScheme.imprint(table,github.com.gengyoubo.sscfa.spell.research.WorldRuneState.get(player.getServer()),InfusionAltarScreenHandler.MAINTENANCE_SCHEME,InfusionAltarScreenHandler.MAINTENANCE_SCROLL);
        player.displayClientMessage(Component.translatable("research.ssc_addon.runes.imprint."+result),true);
        if(result==0){player.containerMenu.broadcastChanges();player.level().playSound(null,table.getBlockPos(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,.8f,1.1f);}
    }
}
