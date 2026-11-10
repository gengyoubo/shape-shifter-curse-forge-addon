package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.item.InfiniteEnergyPotionItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * 闂佽崵濮崇拋鏌ュ棘閸岀偛鐒垫い鎺嗗亾闁哄牜鍓欒灋闁靛牆顦伴埛鏃堟煙闁箑澧紒鏂垮⒔缁辨捇宕掑Δ浣圭彈闂佺绻戦敃銏ゅ蓟瀹€鍐杸婵﹩鍓涢妶鈺佲攽椤旂即鎴︽偩椤忓牆鐒垫い鎺嗗亾缂佸瀚Σ鎰枎閹炬潙娈滃銈呯箰鐎氼噣寮抽弶搴撴?Item闂備焦瀵х粙鎴﹀嫉椤掆偓閿曘垽顢橀悩鐢碉紲閻熸粌瀛╃粩?PotionItem闂備焦瀵х粙鎴λ囬幎钘夌婵せ鍋撶€殿喓鍔戦獮鎴﹀箛椤旂虎鍟嬮梻鍌欑劍濠㈡ê顭囬敓鐘茬劦妞ゆ帒鍊稿瓭閻熸粍婢橀悺銊╁箚閺冨牆鍗抽柕蹇曞Т婵?3 濠电偞鍨堕幖鈺傜閿濆懐鏆ら柛灞惧嚬濞尖晠鎮楀☉娆樼労妞ゆ劧绠戣繚? * 闂備礁鎲￠…鍥窗閹惧瓨娅?{@link BrewingStandBlockEntity#canPlaceItem} 闂佽娴烽弫鎼併€佹繝鍌滄殼闁稿本鍑瑰鈺呮倵濞戞瑯鐒烘い鎰╁€栭崗婊勩亜閹烘垵鈧顪冩禒瀣厽妞ゆ棁宕甸ˇ锔炬喐閹跺﹤娲ょ粻鎶芥煏婢跺牆鍔氱紓宥佸亾 vanilla 闂備浇鍋愰悺鏃堝垂闁秴绠?闂備胶绮划蹇撶暦閻㈢绠柟杈鹃檮閸嬨倝鎮规担鍛婅础缂佲偓? * 闂佸搫顦弲婊堟偡閳哄懎闂柣鎴ｆ閹?HEAD 闂備浇銆€閸嬫捇鏌熺憴鍕Е闁衡偓娴犲鐓欓悗娑櫭悘锝嗙節婵犲啫鐏撮柟顖氬暣瀹曠喖顢欓崗纰卞敹闂佽姘﹂～澶愭儗椤斿墽涓嶉柣鏂垮悑閸ゆ帗銇勯弽銊р姇閻庡灚绮撻弻銊モ槈濡厧顣圭紓浣哥埣濞佳団€︾€圭姷鐤€闁哄啠鍋撻柣蹇撶Ч閺屻倖绺芥径澶岀泿婵犳鍠掗弲鐘茬暦?/ SidedInventory 闂備礁鎼粙鍕崲濠靛鍋樻繛鍡樺姈婵挳鎮归幁鎺戝闁哄棗绻橀弻銊モ槈濮楀棙肖闂? * {@link BrewingPotionSlotMixin} 闂佽崵濮甸崝妤呭窗閺囥垺鍎?GUI 闂備礁缍婂褔顢栭崨顔藉弿闁冲搫鎳庣紒鈺呮煙椤栧棗鍟Ч鏌ユ⒑濮瑰洤濡奸悗姘煎弮閹勬償閵忊晜顔旈悗鍏夊亾閻庯綆鍓涢ˇ? * {@link BrewingRegistryInfiniteMixin} 闂佽崵濮甸崝妤呭窗閺囥垺鍎楁俊銈呭暞婵ジ鏌嶉崫鍕櫣闁诡噮浜娲敃閵堝憛銏ゆ煕閵娧勬珪闁?濠电偛鐡ㄧ猾鍌炲礃閵婏妇妲橀梻浣规た娴滅偤宕伴幇閭︽晢婵犲﹤鐗婇崑銊╂煟閺傛寧鍟炴繛鍛箻閺屻劌鈽夊Ο鐓庮暪缂備焦顨呴ˇ鎵箔閻斿皝鏀藉┑鐘插濞?ITEM_RECIPES 闂備礁鎲＄敮妤呫€冩径鎰ラ柛鎰╁妷閺嬫牠鏌曟繛鍨姎闁哄倵鍋撻梻浣瑰缁嬫垿藝椤撱垹鐒? */

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandInfinitePotionMixin {

    @Shadow
    public abstract ItemStack getItem(int slot);

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$allowInfinitePotion(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // 闂佸湱鍘ч悺銊ッ洪悢鐓庣？?3 濠电偞鍨堕幖鈺傜閿濆懐鏆ら柛灞惧嚬濞尖晠鎮楀☉娆樼労妞ゆ劧闄勯弲?/1/2闂備焦瀵х粙鎴βㄩ埀顒傜磼鏉堚晛顣奸柨娑欏姇閳规垿宕遍埡鍌楁瀻闂備浇顫夋禍浠嬪磿鏉堫偁浜归柛顐犲劚缁秹鏌ｅΟ鑲╁笡妞ゅ孩鐟╅弻娑樜熸笟顖氬壋缂傚倸鍊搁ˇ鐢稿箠濡や胶顩烽悗锝庝簼椤忥繝姊洪懖鈺冩憘闁搞劑浜堕獮?/ 闂備礁鎲￠崹鐢割敄婢跺⊕褰掑磼閻愭潙娈ュ銈嗙墬缁嬫帞鈧?/ 闂傚倷绶￠崑鍛潩閵娾晛鏋侀柕鍫濐槹閸ゅ嫬霉閻樺樊鍎忔繛鍛€濋弻銈囩驳鐎ｎ亞浠梺鍦焾椤︾敻寮鍥︽勃闁稿﹦鍠庨悵顖涚箾閿濆懏澶勭紒璇插暟閳ь剙鐏氬Λ鍐ㄧ暦閵忋倖鍋勯柛婵嗗閺嗩參姊洪棃娑欘棞闁活厺绶氶、姘潩椤戔晪缍侀幐濠冨緞瀹€鈧悾杈ㄤ繆閻愵亜鈧盯顢欐繝鍕闁绘棁娅ｉ惌姘舵煙椤撶喎鍧婇柛瀣尭椤撳ジ宕堕妸褎娈搁梻鍌氬€哥€氼參宕濋弴銏犳槬?        
        if (slot >= 0 && slot < 3
                && (stack.getItem() instanceof InfiniteEnergyPotionItem
                    || stack.getItem() instanceof github.com.gengyoubo.sscfa.item.WitherPotionItem
                    || stack.getItem() instanceof github.com.gengyoubo.sscfa.item.UniversalEnergyPotionItem)
                && this.getItem(slot).isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}

