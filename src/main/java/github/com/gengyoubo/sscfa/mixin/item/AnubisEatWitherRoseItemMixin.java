/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 闂備礁鎲￠崝鎴﹀磿閹绢啟澶愬幢濞戞瑥娈岄梺鍛婁緱閸嬧偓闁稿鎸炬禒锕傛嚍閵夈儮鍋撻弽顓熺厱闁瑰瓨绻嶉崕鏃€绻涢弶鎴濃偓鍧楀箖閹剁瓔鏁冮柕鍫濇濞堝矂姊洪棃娑欘棞缂佸瀚Σ鎰板箥椤旇棄顎涢梺瀹犳〃缁€渚€寮閺岋絽螖閳ь剟鎮ч崱娑橀棷闁汇垹鎲￠埛鏍煕閹捐尙顦︾紓鍌涘哺閺岋綁鎮㈠畡鎵泿缂備浇椴搁悷鈺侇嚕椤掑嫭鍋￠柣妤€鐗嗘禒鎾⒑缂佹ǜ浠滈柡鍛洴楠炴牞顦归柡浣哥У瀵板嫭绻濋崘顓炴儓 SP闂傚倸鍊搁崯顐︽倶濠靛洦鍙忔い鎰ㄦ嚕閹烘绫嶉柍褜鍓熷缁樺緞閹邦剛顓奸柣搴岛閺呯偤宕㈤鍕厱婵炲棙鍏涢幋锕€鍨傛慨妯挎硾閻銇勯弽銊х畼缂佺姾顕ч—? * 闂?2t 闂佽崵濮村ú銊у垝椤栨凹鐒芥い鏍ㄧ⊕娴溿倖绻涢幋娆忕労婵☆偁鍔戦弻娑㈠棘鐠囨彃顬嬮梺杞扮窔缁犳牠寮鍥︽勃闁兼亽鍎遍埀顒勪憾閺屾稑顫濋鈧弳鐔奉熆瑜岄崡鎶界嵁閹烘绠抽柣鎰暩椤︻偊姊洪棃娑欘棞闁稿﹨顕х叅闁秆勵殔缁?mixin 闂備礁鍚嬪姗€鎯岄崒姘肩劷?custom_edible闂備焦瀵х粙鎴炵附閺冨倹宕叉慨妯块哺鐎氭艾霉閿濆洦娅曠紒鐘侯嚙椤啰鈧綆鍓欓弸搴♀攽椤旇娅婇柟顔荤矙閹筹繝濡堕崒妤€浜炬俊顖氱毞閸嬫挾鎲撮崟顏冨閻炴碍绻堥弻娑樷枎閹邦剛浠ч梺? * 闂?5s 闁诲孩顔栭崰鏍磹瑜版帩鏁嗛柣鎰惈缁€鍡涙⒑閸噮鍎愰柣鎾亾闁诲海鍋ｉ崐鏍磿閼测晜宕叉繝濠傜墕杩濇繛杈剧到閸㈡煡鎯佸鍫熷€垫鐐茬仢閻忣亝绻濊閸嬫挻绻涢幋鐐村皑闁稿鎹囬弻銈夋煥鐎ｎ剚娈梺? *
 * - use闂備焦瀵х粙鎺楁偤閹款垶姊婚崒姘殭闁诲繑绻冮幈銊╊敃閳舵寧甯″顕€鍩€椤掑嫬钃熷鑸靛姇缁犮儵鎮楅敐搴′簻闁伙富鍣ｉ弻娑㈠箣濠垫劕鍓跺┑鐐存綑閸婂潡骞冮幎绛嬫晝闁靛牆妫楀▓?闂?setCurrentHand 闂佽崵濮嶉崶銊︽殸婵犫拃鍕煁婵炶壈顕ч埥澶愬閳哄偊绱欓梻浣瑰缁嬫帞绮欓幋锕€鐭楅柛鎾椻偓閸嬫捇鎮烽弶鎸庢闂佸ジ鏅查崡鎶界嵁閳ь剙霉閿濆牜娼愰柡澶婄秺閹泛鈽夊Ο鑲╁姽缂備浇椴搁悷锔剧矚閸楃偐鏀介柛顐ゅ枑閻ｎ剟姊洪崨濠庣劷闁哥姵鎸婚弲璺何旈崨顓犲摋闂佸憡鎸嗛崨顔锯偓顓㈡⒑閼测晛鍔嬮柛鐐跺吹濡叉劕顫濈捄浣? * - getMaxUseTime / getUseAction闂備焦瀵х粙鎺楁嚌妤ｅ啫闂柣銏犳啞閳锋牠鏌涢幘鑼槮缂傚倹宀搁弻?闂?32t + EAT闂備焦瀵х粙鎴︽偋閸涱垰鍨濋幖娣灪娴溿倖绻涢幋娆忕労婵☆偁鍔戦弻锟犲醇閵忕姵鐏堝┑鈽嗗亯濞夋洟骞冮鈧、鏇㈠Ψ椤斿吋顔戦梻浣告啞閻燂箓鎮ч鐘愁潟闂侇剙绉甸ˉ鍫ユ煟閹邦喖鍔嬮柍璇层偢閺屾盯寮幋婊呭嚬闂佷紮瀵岄崳锝咁嚕椤掑嫷鏁嶆繛鍡樺灩閹茶绻涢弶鎴濇倯濞ｅ洦妞介獮鎴︽晲婢跺﹦顓奸柣搴到婢у海绮堟径鎰厸濞达絽鎲￠崳鍝ョ磼濡も偓椤﹂潧鐣烽敐鍛傛棃鍩€椤掍胶闄勯柡宥庡幖娴肩姷鈧箍鍎辩换鎺旂矆閸℃稒鐓? * - finishUsing闂備焦瀵х粙鎺楁偤閹款垶姊婚崒姘殭闁诲繑绻冮幈銊╊敃閳舵寧甯″顕€鍩€椤掑嫬钃熷鑸靛姇鐎氬鈧箍鍎遍幊搴綖閵堝洨纾?闂?闂備礁鎼崐钘壩涢崟顒佸弿闁绘劗鍎ら埛鎾绘煠閸涘鍟忔繛闂寸矙閺屾盯骞嬪┑鎰壎濠电偞娼欓崐鍧楀极瀹ュ閱囬柕澶樺枟缁傚啫鈹戦悙鍙夘棏闁逞屽墮閸熷灝鈻嶉悢鍏尖拻闁糕剝鐟〒?15s闂備焦瀵х粙鎺楁儎椤栫偛闂柛娑橈工缁剁偟鈧箍鍎遍幊蹇涘磹閺嶎偆纾兼繛鎴烇供濡插搫螖?1濠电偞鍨堕幐鎼佹晝椤忓嫷鐎舵繛宸簻缁€鍕倵閿濆骸鏋熺紒浣靛АII闂備焦瀵х粙鎴︽嚐椤栫偛鏄ユ繛鎴欏灩濡?0s闂? 婵犵數鍋為崹鐢告偋閹版澘鐒?闂? *
 * 闂備焦妞挎禍婊堫敄閸涙潙绠栨繝濠傜墕濡﹢鏌ｉ悢鍝勵暭闁稿鍎甸弻锟犲礃閵娿儺鏆㈠┑鐐茬墛閸ㄥ爼鈥旈埀顒佷繆椤栨侗鍎ラ柛姘喘閺岋繝鍩€椤掑嫬绀堢憸蹇涘几閸岀偞鐓?SscAddon 闂?UseBlockCallback 闂佸搫顦弲婊堝蓟閵娿儍?FAIL 闂備礁鎲￠悷锕傛偋濡ゅ啰鐭撻柣鎴ｅГ閺咁剟鎮橀悙闈涘付闁宠绻愰湁婵犙冪仢閳ь剚鎸鹃懞杈ㄥ鐎涙ê顎忛梺鑺ッˇ顖炴偂?use()闂? */
@Mixin(Item.class)
public abstract class AnubisEatWitherRoseItemMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$anubisWitherRoseUse(Level world, Player user, InteractionHand hand,
			CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		ItemStack stack = user.getItemInHand(hand);
		if (!stack.is(Items.WITHER_ROSE)) return;
		if (!FormUtils.isForm(user, FormIdentifiers.ANUBIS_WOLF_SP)) return;
		user.startUsingItem(hand);
		cir.setReturnValue(InteractionResultHolder.consume(stack));
	}

	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$anubisWitherRoseMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (stack.is(Items.WITHER_ROSE)) {
			cir.setReturnValue(32);
		}
	}

	@Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$anubisWitherRoseUseAction(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
		if (stack.is(Items.WITHER_ROSE)) {
			cir.setReturnValue(UseAnim.EAT);
		}
	}

	@Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$anubisWitherRoseFinish(ItemStack stack, Level world, LivingEntity user,
			CallbackInfoReturnable<ItemStack> cir) {
		if (!stack.is(Items.WITHER_ROSE)) return;
		if (!(user instanceof ServerPlayer sp)) return;
		if (!FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP)) return;
		MobEffectInstance current = sp.getEffect(MobEffects.WITHER);
		int amplifier;
		int duration;
		if (current == null) {
			// 濠碘槅鍋撶徊楣冩偋濡ゅ拋鏁冨┑鍌滎焾鐟?闂?闂備礁鎲￠崹鐢割敄婢跺⊕?I闂?5 缂?			
			amplifier = 0;
			duration = 300;
		} else {
			// 闂傚倷鐒﹁ぐ鍐矓閹绢啟鍥蓟閵夈儴袝?闂?缂傚倷鐒︾粙鎴λ囨潏顭戞?+1闂備焦瀵х粙鎴︽偋閸涱垳绠旈柛宀€鍋為埛鏃堟煙闁箑澧繛鍛枛濮?III = amplifier 2闂備焦瀵х粙鎴βㄩ埀顒傜磼鏉堛劎绠炵€规洘宀搁弻銊р偓锝庡亞閸?duration 闂?20 缂?			
			amplifier = Math.min(current.getAmplifier() + 1, 2);
			duration = 400;
		}
		sp.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
		if (!sp.getAbilities().instabuild) {
			stack.shrink(1);
		}
		cir.setReturnValue(stack);
	}
}
