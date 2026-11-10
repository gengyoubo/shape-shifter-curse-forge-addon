package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 婵炵繝鐒﹀顕€宕崱鏇熺溄濞存嚎鍊栧Σ妤€鈻旈妸銉ュ汲 - 1%婵帒鍊诲濂稿船椤旂厧绀岄梻鍐ㄧ仢婵偄袙閺冣偓閺屽寮堕崘鈺傜稑濞戞挸锕﹀▓鎴濐潩鐎涙ɑ鐝? * 濞寸娀鏀遍悧鎼佹晬濮樺崬顔?0濞戞搩浜幑鍥儗?+ 缂?2濞戞搩浜炵挒銏⑩偓瑙勭箘閻撳爼鏁嶉崺?闂傚懎绻戝┃鈧繛鎼枛婵晠鏁? */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderTradeMixin {

	/**
	 * 闁革负鍔嶇粊锕€霉椤忓嫭娅屽ù婊冩惈閿濈偤宕楅崨顒佸攭闁哄嫭鎸搁崹顏嗘偘閵娿儲鍊甸柨娑樺娴?%婵帒鍊诲鍏兼交閽樺顫ｉ梻鍐ㄧ仢婵偄袙閺冣偓閺屽顫濈€涙ɑ鐝峰ù婧垮€栧Σ妤呭Υ?	 * 婵炲鐓夌槐鏉款潰閵堝拋妲遍柛鎺旂帛閸撶増绌卞┑鍫熸畬 mixin闁炽儲鏌￠埀顒佹憵abric TradeOfferHelper 濞村吋纰嶆俊鍛婄閵堝棙顫滄繛澶堝妼閸炶姤娼诲☉妯绘珜濞存粏妗ㄥ锕傚及閹惧娼ㄩ柛娆忓€风粭宀勫箮娴犲鍋?	 * 闁挎稑鐗嗗畷浼存偨?5+1 濞存嚎鍊栧Σ妤€危婵劏鍋撴担璇℃搐闁绘粌娲╅～锕€效閻樺眰浜ｉ悘蹇撶箳閳诲牓鏌屾繝蹇曠闁挎稑鏈Λ銈呪枖閺囩姷鎼煎ù鐘崇仛閳ь剙鑻敐鐐哄礂閸涱厽鍊甸柣娆樺墰閻?1% 濡増绻傞ˇ缁樻交閽樺顫ｉ柕鍡曟缁楀宕￠悩鎵款偊濡村鍥ㄧ暠闁告鍠曢銏＄▕婢跺牃鍋?	 */
	@Inject(method = "updateTrades", at = @At("TAIL"), require = 0)
	private void sscAddon$injectAnubisCrystalTrade(CallbackInfo ci) {
		WanderingTrader trader = (WanderingTrader) (Object) this;
		MerchantOffers offers = trader.getOffers();
		if (offers == null) return;

		// 1%婵帒鍊诲?		if (trader.getRandom().nextFloat() >= 0.01f) return;

		// 闂傚懎绻戝┃鈧ù鐘绘敱閻楁悂鏁嶅鑸靛磳闁?20閸?, 缂備礁鐏濋悿鍌炴儗?32閸?
		int diamondCount = 20 + trader.getRandom().nextIntBetweenInclusive(-5, 5);
		int emeraldCount = 32 + trader.getRandom().nextIntBetweenInclusive(-5, 5);

		// TradeOffer(缂佹鍏涚粩鏉懳ｉ幋鎺旂Т, 缂佹鍏涚花鈺佄ｉ幋鎺旂Т, 閺夊牊鎸搁崵? 闁哄牃鍋撳鍫嗗倸鈻忛柣顫妽椤愬ジ寮? 缂備礁绻橀悰? 濞寸娀鏀遍悧鍛婄▕濡粯娈?
		MerchantOffer crystalOffer = new MerchantOffer(
				new ItemStack(Items.DIAMOND, diamondCount),
				new ItemStack(Items.EMERALD, emeraldCount),
				new ItemStack(SscAddon.ANUBIS_CRYSTAL),
				1, 0, 0.0f);
		offers.add(crystalOffer);
	}
}