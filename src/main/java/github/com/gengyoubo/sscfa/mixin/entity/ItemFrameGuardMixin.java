package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 閸楃姳缍呴幎鈧懗鐣屽⒖閸濅線妲绘径鏍ㄧウ閿涙氨顩﹀銏犵殺 Red 閼筋垱鎸夌悮瀣ㄢ偓涓糚閹妇浼?濞岀粯鍓ゅ▔鏇熸綘娑撳孩鍋婇悘闈涙暠閻楀洦婧€閺€鎯у弳閻椻晛鎼х仦鏇犮仛濡楀棴绱欓懡褍鍘滅仦鏇犮仛濡楀棛鎴烽幍鑳殰閺堫剛琚敍灞肩楠炴儼顩惄鏍电礆閵? * <p>
 * 閼冲本娅欓敍姘崇箹娑撳娆㈤崡鐘辩秴閹垛偓閼崇晫澧块崫渚€鏀ｇ€规艾婀悳鈺侇啀閼冲苯瀵橀崶鍝勭暰濡叉垝缍呴敍鍫ｅ祩濮樼顣?8閵嗕焦涓嶉幇鍫熺《閺?0閵嗕礁鏁遍悧鍥ㄦ簚=1閿涘绱? * 鐎圭懓娅掗悾宀勬桨閸愬懐娈戦悙鐟板毊瀹告彃婀?ScreenHandlerMixin 娴?onSlotClick HEAD 缂佺喍绔撮幏锔藉焻閿涘牆鎯堢粻鍗炵摍閻ｅ矂娼伴惃? * Shift 韫囶偊鈧喓些閸斻劊鈧讣WAP閵嗕箑HROW閿涘鈧倷绲鹃妴灞惧閹镐礁褰搁柨顔肩潔缁€鐑橆攱閵嗗秷铔?ItemFrameEntity#interact閿? * 閸樼喓澧楅惄瀛樺复閹跺﹥澧滄稉顓犲⒖閸濅焦鏂佹潻娑樼潔缁€鐑橆攱楠?decrement(1)閿涘苯鐣崗銊ょ瑝缂佸繗绻?PlayerInventory 閻ㄥ嫪鎹㈡担鏇熸暈閸忋儳鍋ｉ敍灞炬Ц閸烆垯绔撮惃鍕础缂冩垼鐭惧鍕┾偓? * <p>
 * 娑撳秴灏崚鍡楄埌閹?濡€崇础閺冪姵娼禒鑸靛閹搭亷绱伴崡鐘辩秴閹垛偓閼崇晫澧块崫浣规拱鐏忓彉绗夋惔鏃€绁﹂柅姘剧礉闂冨弶顒涢幐鍥︽姢閼惧嘲褰囬幋鏍ц埌閹礁鍨忛幑銏ゆ？闂呮瑨顫︾€涙绻樼仦鏇犮仛濡楀棎鈧? */
@Mixin(ItemFrame.class)
public abstract class ItemFrameGuardMixin {

    @Inject(
            method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ssc_addon$blockLockedSkillInItemFrame(Player player, InteractionHand hand,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        // 娑撶粯澧滈幋鏍у閹靛鎹㈡稉鈧幐浣规箒閸楃姳缍呴幎鈧懗鐣屽⒖閸濅礁宓嗛幏鎺旂卜娴溿倓绨伴敍鍫濆閹靛鐭惧鍕倱閺嶈渹绱板☉鍫ｂ偓妤佸娑擃厾澧块崫渚婄礆
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(h);
            if (stack.is(SscAddon.POTION_BAG)
                    || stack.is(SscAddon.ALLAY_HEAL_WAND)
                    || stack.is(SscAddon.ALLAY_JUKEBOX)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }
    }
}
