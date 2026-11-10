package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import github.com.gengyoubo.sscfa.item.PotionBagItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 缂佹洝绻冮張宥呭缁旑垰鍠庨崡瀛樺閹搭亷绱濈拋鈺勫祩濮樼顣伴崷?ItemCooldownManager 閸愬嘲宓堥張鐔兼？娴犲秴褰插婊嗩攽閸欐娊鏁鈧崠鍛偓? * <p>
 * 閺嶇懓娲滈敍姘礁闁款喕姘︽禍鎺旀畱閸愬嘲宓堥幏锔藉焻閸?b>閺堝秴濮熺粩?/b> {@code ServerPlayerInteractionManager.interactItem}
 * 娑擃厸鈧柡鈧柨鐣犻崷銊ㄧ殶閻?{@code stack.use()} <b>娑斿澧?/b>濡偓閺? * {@code player.getItemCooldownManager().isCoolingDown(stack.getItem())} 楠炲墎娲块幒銉ㄧ箲閸?PASS閿? * 鐎佃壈鍤?{@code use()} 娑撳秷顫︾拫鍐暏閵嗕礁绱戦崠鍛粹偓鏄忕帆閺冪姵纭剁憴锕€褰傞敍鍫濐吂閹撮顏?interactItem / doItemUse 閸у洦妫ゅ銈嗩梾閺屻儻绱? * 娑斿澧犵拠顖涘Ω mixin 閹垫挸鍩岀€广垺鍩涚粩顖滆鐎佃壈鍤?"Scanned 0 target(s)" 濞夈劌鍙嗘径杈Е閼板苯绌垮┃鍐跨礆閵? * <p>
 * 娣囶喖顦查敍姘秼閻椻晛鎼ф稉?{@link PotionBagItem} 娑撴梻甯虹€硅埖缍旂悰灞炬閿涘矁顔€鐠囥儱鍠庨崡瀛橆梾閺屻儴绻戦崶?false閿涘牊鏂佺悰宀嬬礆閿? * 閺堝秴濮熺粩?{@code use()} 閸忓牆鍨介弬顓熺稊鐞?閳?瀵偓閸栧懌鈧倻娅ч懝鏌ヤ紕缂冣晙绮涢悽?ItemCooldownManager 濮濓絽鐖跺〒鍙夌厠閵? * <p>
 * 濞夈劍鍓伴敍姘箑妞ょ粯鏁為崘灞藉煂 mixins.json 閻?<b>common</b>閿?mixins"閿涘鍨悰顭掔礉娴ｅ灝鍙鹃崷銊ょ瑩閻劍婀囬崝鈥虫珤娑撳骸宕熸禍? * 闂嗗棙鍨氶張宥呭閸ｃ劋绗傞柈鐣屾晸閺佸牞绱濇穱婵婄槈娑撹顓归張杞扮閼疯揪绱欓張顒傝閾忚棄婀?client 鐎涙劕瀵橀敍灞肩矌娑撹櫣娲拌ぐ鏇犵矋缂佸浄绱濇稉搴ょ箥鐞涘瞼骞嗘晶鍐╂￥閸忕绱氶妴? */
@Mixin(ServerPlayerGameMode.class)
public class PotionBagCooldownBypassMixin {

    /**
     * 閹凤附鍩?{@code interactItem} 閸愬懐娈?{@code isCoolingDown(Item)} 鐠嬪啰鏁ら敍鍫㈡暏 @WrapOperation 閸忕厧顔愰崗璺虹暊 mod 閻ㄥ嫬鎮撴担宥嗘暈閸忋儻绱氶妴?     * 閹恒儲鏁归懓?(manager, item) 娑斿鎮楅悽?{@code Operation} 閹垫寧甯撮崢鐔荤殶閻㈩煉绱漿@code @Local} 閸欐牜娲伴弽鍥ㄦ煙濞夋洜娈戦張宥呭缁旑垳甯虹€硅泛寮弫鎷岊嚢閸欐牗缍旂悰宀€濮搁幀浣碘偓?     */
    @WrapOperation(
            method = "useItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;isOnCooldown(Lnet/minecraft/world/item/Item;)Z"),
            require = 0
    )
    private boolean ssc_addon$bypassCooldownForSneaking(ItemCooldowns manager, Item item,
            Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        if (item instanceof PotionBagItem && player.isShiftKeyDown()) {
            // 濞兼粏顢戝鈧崠鍜冪窗缂佹洝绻冮崘宄板祱濡偓閺屻儻绱濈拋鈺傛箛閸旓紕顏?use() 濮濓絽鐖剁憴锕€褰?            
            return false;
        }
        // 闂堢偞缍旂悰灞惧灗闂堢偠宓傚纾嬵暟閿涙矮绻氶幐浣稿斧閻楀牆鍠庨崡纾嬵攽娑?        
        return original.call(manager, item);
    }
}