package github.com.gengyoubo.sscfa.mixin;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ItemGroup 濞达絽绉堕悿鍡欌偓娑欘殕椤斿矂宕樺▎蹇撳汲闁革綆鐓夌槐妾峚ckcooper闁挎稑顧€缁辩檵abric 闁汇劌瀚崹閬嶆焻閻樻眹鈧宕氶崱娑栤偓澶愭晬閸︽硟emGroupsMixin.paginateGroups闁?
 * 闁硅泛锕ヨ啯缂備礁瀚晶鍧楀传娴ｈ櫣鐭嬮柟绋款槸閻ｎ剟寮?id 閻庢稒顨呴崥鈧幖鏉戠箲鐢挻绺介挊澶嬪€甸柨娑樼焸閳ь剚淇虹换鍐嚊椤忓嫮绠掗柣?accessor 闁告劖鐟ラ崣?row / column闁?
 * 闁哄牜鍓熷顔句沪閻愮儤浠橀悷鏇氱濠€顏堝礆閸℃稏鈧鈧懓鏈崹姘跺触鎼粹€虫櫃闁哄秮鍓濋婊堟嚊椤忓嫮绠掑☉鎾卞€撻柌婊呯磼閸曨厽鐣卞ù锝呯Ф閻ゅ棝鏁嶉崼婊呯閻犲洣闄嶉埀顒€鏈涵鍫曞嫉椤栨ǚ鍋撳澶堚偓澶屾瑜庣亸鎰版⒔閸曨偆娼ｅ☉鎾愁煼閵嗗鏁嶅畝鍐炬綄
 * {@code client.CreativeTabNeighborMixin}闁挎稑顧€缁辨繈寮崨顓熷€遍柡宥囧厴濞撳墎鎲版担鐣岀濞戞挶鍊撻柌婊呪偓娑欘殕椤斿矂鎯?setter闁?
 * getter 闁活潿鍔岀敮顐︽偋閸繂褰嗙€殿喒鍋?API闁挎稑婢朄code row()} / {@code column()}闁挎稑顦畵鍡涘矗椤栥倗绀夐柡鍐█濞撳爼宕烽妸銊х闂佹彃鐭傞崳鍛婂緞瀹ュ拑绱ｉ柡鍕昂閳?
 */
@Mixin(CreativeModeTab.class)
public interface ItemGroupPositionAccessor {

	/** 闁告劖鐟ラ崣鍡欐偘瀹€瀣TOP / BOTTOM闁挎稑顦埀?*/
	@Accessor("row")
	void ssc_addon$setRow(CreativeModeTab.Row row);

	/** 闁告劖鐟ラ崣鍡涘礆濡ゅ绀?-4闁挎稑鏈惁锛勬偘?5 闁哄秶銆嬬槐姘跺Υ?*/
	@Accessor("column")
	void ssc_addon$setColumn(int column);
}
