/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem;
import github.com.gengyoubo.sscfa.item.WitherPotionItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 闂佽偐鐡旈崹閬嶅箰婢跺瞼灏甸柤濮愬€曢埛鏍煕濠靛懓鍏岀紒顔惧劋缁嬪鍩€椤掆偓椤曪綁宕崟顐ゅ幀闁荤喐鐟ュΛ鏃傛嫻閻旂厧违閻忕偠妫勭拋鏌ユ煕椤愩垺顥滅憸鐗堟婵℃潙顓艰箛鏇犵崶婵烇絽娴傞崰鏍?shift 闂婎偄娴傞崑濠囧焵椤掑倸鏋旀禍娑㈡煕閺傝濡虹紒妤€顦靛畷銉╁醇濠靛﹤鐝梺姹囧妼鐎氼參鎮ч幘顔肩倞闁绘瑥鎳愮粈鍕偡閺囩偞顥犳繛鎻掞躬楠炲秹鍩€椤掑嫬瀚夊璺洪缁佲晜鎱ㄥΟ鎸庡涧缂佽鲸宀告俊?
 *
 * <p>闂佸憡顭囬崰鎾存櫠?{@code ScreenHandler.insertItem} 闂佹眹鍔岀€氼剟骞冩惔銊у祦閻犺櫣鍎ら崕鏇㈡煟濠婂骸鐏犻柛娆忔瀹曪綁鏁愰崱妯肩暢 {@code isStackable()} 闁诲海鎳撻悧鍡楃暦?
 * 闂佹寧绋戦悧鍡涙偪閸曨垱鍋濆Λ棰佹祰缁€?{@code getMaxCount() > 1}闂佹寧绋戦¨鈧紒杈ㄧ箞閹筹繝顢涘☉娆忕樆缂備緡鍋夊畷鍨櫠閸ф浼?maxCount=1 闂佸搫鍟抽崺鏍汲閿濆棛鈻旀い蹇撳閸娿倖顨ラ悙璺虹厫妞ゆ柨鐬奸幃顕€顢曢妶鍡欏嚱闂佽浜介崕鎾倻閿旇姤浜ら柛銉╊棑缁€?
 * {@code Slot.getMaxItemCount} / {@code insertItem} 闂佸憡鍔曢幊鎰枔閹寸偟鈻斿┑鐘叉处椤庢瑦绻涙径鍫濆闁告瑥妫濋弻鍫ユ嚍閵夛讣楠忓┑鐐差槹濞叉粓寮幘璇叉瀬?
 * 闂佹寧绋戦悧鍡欐暜椤愶附鍋?SSC 闂佹眹鍔岀€氼垰鐣甸崒姘兼僵闁哄鍨电紞宀勬煛閳ь剟骞嗚閸婇亶鏌″鍫熻吂缂佹顦遍幉浼村幢濞嗘劗锛?shift 闁荤姳璀﹂崹鎵閻愮儤鏅悘鐐舵缁侇喗鎱ㄥ┑鍕偓鏇犳暜椤愶箑鐐婇柣娆忔噽缁€鍡涙煏?
 *
 * <p>闁哄鏅滈悷鈺呭闯妞嬪簶鍋撻悽鍨殌闁告瑥绻橀弻鍫ュΩ閵娿儳銈╁┑顔筋焾閹活亞妲愬▎鎾冲偍闁绘柨鎼。?PotionItem 缂?+ 闂傚倸瀚€氼剟鎯堝鈧弻鍛潩鏉堛劍娈㈤梺鐓庡帠缁瑩宕冲ú顏呭殥妞ゆ牗绋掔€?/ 闂佸憡鍨甸澶娒归崒鐐村殥妞ゆ牗绋掔€垫粓鏌ㄥ☉姗嗘Ц鐎规悂浜跺畷姘跺箚閹殿喚顔夐梺?true
 * 闂佽　鍋撻柟瑙勫姦閺€浠嬪级閳哄倻鈽夐柛娆忔瀹曘儵宕煎┑濠傜彲閻庣敻鍋婃禍鐐虹嵁閸℃稒鏅繛鎴烆焽閺変粙姊婚崟顐ｅ櫣鐟滅増妫冨銊╁矗閵壯呮啰闂傚倸瀚崝鎺斿垝濞戙垺鍋?WitherPotionStackMixin闂佹寧绋戝﹢姗碔 濠碘€冲级閸ㄦ繄绱為崨瀛樻櫖濠㈣泛顦遍悷?ScreenHandlerMixin
 * 闂佹寧绋戝鐒爏ertItem / 闂佸憡鐟ラ懟顖炲吹椤曗偓瀹曘儵宕煎┑濠傜彲 / 闂佸憡甯楅惄顖炲焵椤掑倸啸闁哄拋鍋婂Λ鍐綖椤斿墽顦? 闂佸憡顭囬崰鎾存櫠?PotionStackMixin 闂佸湱顭堥ˇ顖炴嚋娴兼潙绠?/ power 闂佸綊顤傞崜娆忕暦瀹€鍕伋闁哄倽銆€閸?
 * 闂佸搫鍟版慨鍓х矈椤愶箑鍐€闁兼祴鏅涚拹鐔兼倵绾懏纭炬繝鈧鍛氦婵炲棗娴烽弰?wrap 婵犮垼娉涚€氼厾鈧絼绶氬畷姘跺箥椤旂晫鏆犳繛鎴炴尭閿曨亜顬婇悜妯碱浄鐎广儱妫欑瑧 1闂佹寧绋戦懟顖炲箖鎼淬劎宓侀悹鍝勬惈閻忔煡鏌?{@code j+k > 1} 婵炴垶鎸哥粔褰掑焵椤掍椒浜㈢紒璇插暣閺佸秴鐣濋崘銊︻唶闂佺粯甯楀﹢鍦箔婢舵劕鐭楅柣姗嗗枓閸嬫挻鎷呯憴鍕堕獜婵犮垼娉涚粔鎾春濡や緡妲归幖娣妽閻濓箓鏌?
 * 闂侀潻缂氶崡鍐差焽娴兼潙绠冲璺哄閸庝即鏌ｅ缁樻珔闁诡喖閰ｉ悰顕€鐓ù瀣壕濞达絽婀辩涵鈧梺鍝勫€归〃鍫ユ偤閹达箑绀傞悹楦挎閺?isStackable 闁荤姴顑呴崯浼村极閵堝鍊烽柣鐔哄缁€?{@code getMaxCount()=1} 闂佸憡鑹鹃張顒勬偋鏉堛劎鈻旂€广儱瀚粣妤呮倵閸︻厼浠辨俊缁㈠灦瀹曪綁鎮╅搹顐ｆ緬闂?
 * 闂佸搫鍟版繛鈧俊鐐そ閹虫绂掔€ｎ亝顏￠梺鑲╃摂閸ㄩ亶骞愭径鎰櫖闁割偆鍠撳Σ銊╂煟閳ュ啿顣抽悗姘煎幘缁晝鈧綆鍘煎▓浼存煕韫囨挻鍤€闁?NBT闂佹寧绋戦ˇ顔剧箔婢舵劕鎹堕柕濠忓瘜閸斿啴鏌涢幒鎿冩缂佽鲸绻冪粚鍗炩攽閸パ呮Х婵炴垶鎸哥粔纾嬨亹閺屻儱鐭楅柣姗嗗枓閸?
 */
@Mixin(ItemStack.class)
public abstract class AddonPotionStackableMixin {

	@Inject(method = "isStackable", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$potionAlwaysStackable(CallbackInfoReturnable<Boolean> cir) {
		ItemStack self = (ItemStack) (Object) this;
		if (self.getItem() instanceof PotionItem
				|| self.getItem() instanceof UniversalEnergyPotionItem
				|| self.getItem() instanceof WitherPotionItem) {
			cir.setReturnValue(true);
		}
	}
}
