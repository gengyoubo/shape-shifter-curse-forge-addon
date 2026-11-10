package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.com.gengyoubo.sscfa.client.ClawClientState;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 妞嬪海浼掗妴宀€鏌ф搴ょ箾閻栴亗鈧秴鍣弰鐔告暰閸戣鍠庨崡瀛樻蒋閹恒儳顓搁妴? *
 * 娴犲懘鍣哥€规艾鎮?{@code InGameHud.renderCrosshair} 闁插矂鍋呮稉鈧▎?{@code getAttackCooldownProgress} 鐠嬪啰鏁ら敍?濞撳弶鐓嬮崙鍡樻Е
 * 閺€璇插毊閸忓懓鍏橀幐鍥┿仛閸ｃ劎鏁ら惃鍕箻鎼达讣绱氶敍姘额棑閻忕數鍩呴崙?鏉╁洨鍎归弮鎯扮箲閸ョ偠鍤滅€规矮绠熸潻娑樺閿涘牏鍩呴崙濠氭？闂呮柨鍘栭懗?/ 鏉╁洨鍎归崶鐐差槻閿涘绱濋崥锕€鍨潻鏂挎礀閸樼喎鈧鈧? *
 * 閳?閸忔娊鏁敍姘瑝閺€?{@code PlayerEntity.attack()} 闁插矂鍋呮径?getAttackCooldownProgress 閳?MC 閸樼喓澧楅弨璇插毊閸愬嘲宓堥崙蹇庢縺閻撗冪埗
 * 閻㈢喐鏅ラ敍鍫熻閺屾挷绗岄崙蹇庢縺閸掑棛顬囬敍澶堚偓鍌氬涧閻劏绻栨稉鈧稉顏勫斧閻楀牆鍣弰鐔告蒋閿涘奔绗夋０婵嗩樆閻㈡槒鍤滅€规矮绠熷Ο顏呮蒋閵? *
 * 濞夘煉绱皗@code renderCrosshair} 闁插矁鐨熼悽銊ф畱閹恒儲鏁归懓鍛Ц {@code this.client.player}閿涘牓娼ら幀浣鸿閸?ClientPlayerEntity閿涘绱? * 閺?INVOKE owner 韫囧懘銆忛弰?ClientPlayerEntity 閼板矂娼?PlayerEntity閿涘牆鎯侀崚娆愬娑撳秴鍩岄惄顔界垼閵嗕礁绌垮┃鍐跨礆閵? * {@code require = 0}閿涙艾宓嗘笟鑳潶閸忚泛鐣?mod 閹躲垺鏁為幋鏍х摟閼哄倻鐖滃顔肩磽鐎佃壈鍤ч張顏勫爱闁板稄绱濇稊鐔峰涧閺勵垰鍣弰鐔告蒋娑撳秵妯夌粈鎭掆偓浣风瑝瀹曗晜绨濋敍鍫滅喘闂嗗懘妾风痪褝绱氶妴? */
@Mixin(Gui.class)
public class ClawCrosshairMixin {

    @ModifyExpressionValue(
            method = "renderCrosshair",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"),
            require = 0)
    private float ssc_addon$clawCrosshairProgress(float original) {
        if (ClawClientState.isActive()) {
            return ClawClientState.getCrosshairProgress();
        }
        return original;
    }
}
