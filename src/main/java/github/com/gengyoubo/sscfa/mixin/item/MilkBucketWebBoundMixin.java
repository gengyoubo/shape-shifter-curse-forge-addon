package github.com.gengyoubo.sscfa.mixin.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MilkBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 闂備浇宕垫慨宕囨媼閺屻儱妫橀柛宀€鍋涢悞鍨亜閹哄棗浜鹃梺娲诲幒缁€浣哄弲闂佽鍎虫晶搴ｅ妤ｅ啯鐓欓悗鐢殿焾鍟搁梺缁樺姇閿曨亜螞閸涙惌鏁嗗璺猴梗缁ㄦ挳姊洪柅鐐茶嫰婢ь喚绱掓径灞惧殌閸楀崬鈹戦悩宕囶暡闁稿顑嗙换娑㈠箣閻愬啯绮撳畷鎴︽偐缂佹鍙嗗┑鐐村灦閻燂箓鎯冮幋婵愮唵鐟滃繑鏅跺Δ鍐处濞寸姴顑嗛崵瀣煟閵忊槅鍟忓ù鍏兼礋濮婅櫣绱掑鍡欏姼闂佺硶鏅滈悧鐘诲春閳ь剚銇勯幋锝嗙《闁告柨绉归弻锟犲川椤愩垹濮﹂悗娈垮枤椤牓鈥﹂妸鈺佺妞ゆ帒顦拌ⅶ闂傚倷鑳堕、濠傗枖濞戙垺鏅濋柕濞炬櫓閺?debuff 婵犵數鍎戠徊钘壝洪敂鐐床闁告洦鍨板Ч鏌ユ煃瑜滈崜娆撳煡婢舵劕绠婚柟棰佺閸撳崬鈹戦悙璺轰汗闁轰礁顭峰顐㈩吋婢跺﹪鍞跺┑鐘绘涧濡娆㈤鐔虹閺夊牆澧界粙濠氭煟閻曞倻鐣电€殿喗鐓℃俊鐑藉煛娴ｇ娈ゆ繝鐢靛Т閿曘倝骞婃惔鈽嗗殨濡わ絽鍟崑銊︺亜閺嶃劎顣查柛鐔哄仧缁辨帡寮▎鍓у悑濠殿喖锕ｇ划娆愪繆閹壆鐤€闁哄洨濯Σ鎾⒒? *
 * <p>闂?{@code @WrapOperation} 闂傚倷绀侀幉锟犳偡閿曞倹鍋嬮柡鍐ㄧ墕閼歌銇勯幒鎴濐仼闁稿被鍔戦弻锝夊棘閸喗些闁汇埄鍨崕鐢稿蓟?{@code @Redirect}闂傚倷鑳堕崑銊╁磿閺屻儱钃熼柨鐔哄Т閻掑灚銇勯幒宥囧妽闁哄懐顦瞔ode @Redirect} 婵犵數鍋為崹鍫曞箰閸涘娈介柟闂寸閻鏌涢幘妞诲亾闁?mod /
 * SSC 婵犵數鍋為崹鍫曞箲娓氣偓椤㈡俺顦撮柍褜鍓欓悘姘垛€﹂崶顒€鐒垫い鎺戝€归弳鈺呮煙閾忣偅宕屾鐐插暣楠炴鈧稒锚椤庢捇鎮楅獮鍨姎婵炲眰鍔戝?{@code clearStatusEffects()} 闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸欏啫顕ｉ锔藉€烽柛婵嗗椤ρ囨⒑闂堟侗妲堕柛濠冾殘缁絽螖閸愵亝锛忓銈嗘寙閸愵煈娼婚梻浣告憸閸犳洖鈻旈弴鐘插灊闁挎繂妫涢悷鐟扳攽閻樻彃顏い锔肩瘋@code InjectionError} 闂佽姘﹂～澶娒哄澶婄柧婵犲﹤鎳撴慨铏節闂堟侗鍎愰柡鍜佸墮椤潡鎳滈悽鐐光偓鎺旂磼閳? * 闂?{@code @WrapOperation} 闂傚倷娴囬妴鈧柛瀣尰閵囧嫰寮介妸褉妲堥梺浼欏瘜閸犳牠鈥?mod 闂傚倸鍊烽懗鍫曞磻閹捐纾跨€规洖娲﹂～鏇㈡煥閻斿搫孝闁活厽顨婇弻锝呂熺拠鑼シ濡炪値鍋撶紞渚€寮婚妸銉㈡婵炲棙甯掗ˉ婵嬫⒑閸涘鎴犵矙閹捐绠悗锝庡亾缁憋絽霉閿濆牊顏犻柟宸櫍濮婃椽鎮欓浣叉嫻缂備礁顑嗛幐鍓ф?debuff闂傚倷绶氬褍螞閺冨牊鍊块柨鏇炲€稿Λ妯好归敐鍥у妺闁稿锕㈤幃姗€鎮欓懜娈挎闂?{@code clearStatusEffects}闂? * 闂傚倷绀侀幉锟犲礉閺囩姷鐭撻柛顭戝亗缁诲棝鏌熼梻瀵稿妽闁瑰弶蓱缁绘盯骞嬮悙瀵告缂?debuff 闂傚倸鍊烽悞锕併亹閸愵亞鐭撻柣銏㈩焾閽冪喎鈹戦悩鍙夋悙缂佺姳鍗抽弻娑氫沪閸撗屾殹缂備讲鍋撻柍褜鍓熷?/p>
 */
@Mixin(MilkBucketItem.class)
public class MilkBucketWebBoundMixin {

	@WrapOperation(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"), require = 0)
	private boolean ssca$keepWebBound(LivingEntity entity, Operation<Boolean> original) {
		MobEffectInstance web = entity.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get());
		// 闂傚倷绀侀幉锟犳偡閿曞倵鈧箓宕堕埡鍌ゅ仺闂侀潧鐗嗛ˇ浼村疾?026-09-29 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾閹寸偛鐒归柛瀣尵閹叉挳宕熼鍌涱吇婵＄偑鍊ら崢濂告偋閹剧繝绻嗛柡澶嬪灩閺嗗棝鏌嶈閸撴岸寮查崼鏇熷亱闁割偁鍨藉顕€姊洪懖鈺婄劸闁诲繑宀搁弫鎾诲Ψ閿斿墽顔曟繝銏ｆ硾椤戝棛绮堥埀顒勬⒑闂堚晝绉剁紓宥勭閻ｇ兘鎮╃拠鎻掑敤闂侀潧顦崹瑙勭閸撗€鍋撻獮鍨姎缂佺粯甯炵划鍫ュ幢濞戞瑧鍙嗗┑鐐村灦閻熴劍绔熷鈧弻鐔碱敊閼恒儱纾抽梺纭呮閸婃悂鍩ユ径濠庢僵閺夊牃鏁╄缁辨捇宕掑顑藉亾缂佹ɑ鍙忓Δ锝呭暞椤ュ﹪鏌嶉崫鍕偓濠氬煝閺冨牊鐓冪憸婊堝礈閻斿吋鍋樻い鏇楀亾鐎殿喚鏁婚幃浠嬫偨濞堝灝鏅梻?		
		MobEffectInstance sheep = entity.getEffect(github.com.gengyoubo.sscfa.SscAddon.SHEEP_FORM);
		boolean result = original.call(entity);
		if (web != null) {
			entity.addEffect(new MobEffectInstance(web));
		}
		if (sheep != null) {
			entity.addEffect(new MobEffectInstance(sheep));
		}
		return result;
	}
}
