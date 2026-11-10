package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.screen.SpellbookScreenHandler;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 閺堝牆鐨规鏃€纭舵稊锔肩礄jackcooper閿涘鈧倽顥婇崗銉ㄥ殰瀵?Trinkets 妤楁澘鎼уΣ?{@code moonlit/spellbook} 閸氬骸褰查悽銊ユ彥閹圭兘鏁柌濠冩杹娑旓箑鍞存鏃€纭堕妴?
 *
 * <p>娑旓妇鐡戠痪褋鈧礁宕岀痪褏绮℃灞烩偓浣稿祹鏉炴番鈧焦纭堕梼闈涘挤閸愬嘲宓堢€涙ü鍔?NBT閿涙稒寮х痪褍鎮楅惃鍕翱闁艾鐡ㄩ悳鈺侇啀濡楋絾顢嶉妴?
 * 鐟?{@link SpellbookData}閵嗗倷鎹㈡担鏇炶埌閹礁娼庨崣顖欏兊閹磋揪绱欑紒褎澹?{@link AccessoryItem}閿涘畱anEquip 姒涙顓婚弨鎹愵攽閿涘鈧?/p>
 */
public class MoonDustSpellbookItem extends AccessoryItem {

	public MoonDustSpellbookItem(Properties settings) {
		super(settings);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		// 鐢悂鈹楅崢鐔哄闂勫嫰鐡熷ù浣稿帨閿涘牅绮庣憴鍡氼潕閿涘奔绗夐崡鐘垫暏閻喎鐤勯梽鍕摕閿?
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		// 濞兼粏顢?+ 閸欐娊鏁敍姘槑閻㈢喐鐏夐摝鐘偓灞炬尡缁夊秲鈧秳绱崗鍫礄閼?1 缁夊秴鐡欓崶?10 娑旓附纭堕張顖氣偓纭风幢閺堫亜鎳℃稉顓熸蒋娴犺泛娲栭拃钘夌磻闁板秶鐤嗛悾宀勬桨閿?
		if (user.isShiftKeyDown() && !world.isClientSide && user instanceof ServerPlayer sp
				&& github.com.gengyoubo.sscfa.spell.FormCastingStyle.trySeedSow(sp, stack)) {
			return InteractionResultHolder.success(stack);
		}
		// 濞兼粏顢?+ 閸欐娊鏁敍姘尡缁夊秵婀憴锕€褰傞敍鍫ユ姜鐎靛嫮鏁撻弸婊嗘浆/閺冪姷顫掔€?娑旓附寮ч敍澶嬫娑撳秴娲栭拃钘夌磻閻ｅ矂娼伴敍灞肩箽閻ｆ瑦缍旂悰灞惧閸旇儻顕㈡稊澶婂叡閸戔偓
		if (user.isShiftKeyDown()) {
            return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
		}
		// 閺咁噣鈧艾褰搁柨顕嗙窗閹垫挸绱戞鏃€纭舵稊锕傚帳缂冾喚鏅棃顫礄2026-09-17 閻㈣鲸缍旂悰灞藉礁闁款喗鏁兼稉鍝勫礁闁款噯绱濋悽銊﹀煕閹稿洤鐣鹃敍?
		if (!world.isClientSide) {
			net.minecraftforge.network.NetworkHooks.openScreen((ServerPlayer) user,
					new net.minecraft.world.SimpleMenuProvider(
							(syncId, inv, player) -> new SpellbookScreenHandler(syncId, inv, stack),
							stack.getHoverName()),
					buf -> {
						buf.writeInt(SpellbookData.getSlotCount(stack));
						buf.writeInt(SpellbookData.getLevel(stack));
						buf.writeInt(SpellbookData.getExpTen(stack));
						buf.writeInt(SpellbookData.getMana(stack));
						buf.writeInt(SpellbookData.getMaxMana(stack));
						buf.writeInt(SpellbookScreenHandler.findBookSlot(user.getInventory(), stack));
						buf.writeNbt(github.com.gengyoubo.sscfa.neomagic.NeoNetwork.hudState((ServerPlayer) user));
					});
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		int level = SpellbookData.getLevel(stack);
		int slots = SpellbookData.getSlotCount(stack);
		int mana = SpellbookData.getMana(stack);
		int maxMana = SpellbookData.getMaxMana(stack);
		tooltip.add(Component.translatable("item.ssc_addon.moon_dust_spellbook.tip_level", level, slots).withStyle(ChatFormatting.AQUA));
		tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("book_bonus",String.format(java.util.Locale.ROOT,"%.1f",github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.safetyBonus(stack)),String.format(java.util.Locale.ROOT,"%.2f",github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.recoveryMultiplier(stack))).withStyle(ChatFormatting.BLUE));
		int need = SpellbookData.getExpToNext(stack);
		if (need > 0) {
			// 閺堫亝寮х痪褝绱伴弰鍓с仛閸楀洨楠囨潻娑樺閿? 娴ｅ秴鐨弫甯礆
			tooltip.add(Component.translatable("item.ssc_addon.moon_dust_spellbook.tip_exp",
					String.format(java.util.Locale.ROOT, "%.1f", SpellbookData.getExpFloat(stack)),
					String.format(java.util.Locale.ROOT, "%.1f", need / 10.0f)).withStyle(ChatFormatting.GRAY));
		}
		// 娴犲孩婀囬崝锛勵伂閸氬本顒為惃鍕负鐎硅埖銆傚鍫濈潔缁€铏圭翱闁熬绱濋弻銉ф箙娴犺缍嶆稉鈧張顑垮姛闁姤妯夌粈鍝勬倱娑撯偓閻溾晛顔嶆潻娑樺閵?
		int mastery=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.clientExpTen();
		int tier=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.tier(mastery);
		int bonus=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.bonus(mastery);
		{
			int unit=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.thresholdTen(tier);
			tooltip.add(Component.translatable("text.ssc_addon.player_mastery.progress",tier,
					String.format(java.util.Locale.ROOT,"%.1f",github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.progressTen(mastery)/10.0),
					String.format(java.util.Locale.ROOT,"%.1f",unit/10.0),bonus).withStyle(ChatFormatting.GOLD));
		}
		tooltip.add(Component.translatable("text.ssc_addon.player_mastery.tip").withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("book_help").withStyle(ChatFormatting.DARK_GRAY));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
