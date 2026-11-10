/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 閺堟梹婀滈妴宀冨灄闊偆鍨庨悙鎼炩偓宥堟惈閸旀稒婀￠梻缈犲紬閺嶈偐顩﹀銏㈡煣鐠烘埊绱欑€广垺鍩涚粩顖ょ礆閵? * <p>
 * 鐠囪褰囬崥灞绢劄閸掓澘顓归幋椋庮伂閻?apoli 鐠у嫭绨?form_ocelot_nova_charging閿涙矮璐?1 閺冩儼銆冪粈鐑橆劀閸︺劏鎼崝娑崇礉
 * 濮濄倖妞傞梼缁橆剾瀵偓婵鏌х捄鎴礉楠炶埖鐦?tick 瀵搫鍩楅崣鏍ㄧХ瑜版挸澧犻悿鎹愮獓閳ユ柡鈧柨鐤勯悳棰佸紬閺嶈偐顩﹂悿鎹愮獓閿涘矁鈧矂娼禒鍛存浆閸戝繘鈧喕绻庢导绗衡偓? */
@Mixin(LocalPlayer.class)
public abstract class NovaSprintLockMixin {

    private boolean ssca$novaCharging() {
        LocalPlayer self = (LocalPlayer) (Object) this;
        return PowerUtils.getClientResourceValue(self, FormIdentifiers.OCELOT_NOVA_CHARGING) > 0;
    }

    // 缁備焦顒涘鈧慨瀣煣鐠?    @Inject(method = "canStartSprinting", at = @At("HEAD"), cancellable = true, require = 0)
    private void ssca$novaBlockSprintStart(CallbackInfoReturnable<Boolean> cir) {
        if (ssca$novaCharging()||github.com.gengyoubo.sscfa.spell.OcelotSpellLink.windLocked((LocalPlayer)(Object)this)) {
            cir.setReturnValue(false);
        }
    }

    // 濮?tick 瀵搫鍩楅崣鏍ㄧХ瑜版挸澧犻悿鎹愮獓閿涘牐顩惄鏍惈閸旀稑绱戞慨瀣瀹告彃婀悿鎹愮獓閻ㄥ嫭鍎忛崘纰夌礆
    @Inject(method = "aiStep", at = @At("HEAD"), require = 0)
    private void ssca$novaStopSprint(CallbackInfo ci) {
        if (ssca$novaCharging()||github.com.gengyoubo.sscfa.spell.OcelotSpellLink.windLocked((LocalPlayer)(Object)this)) {
            ((LocalPlayer) (Object) this).setSprinting(false);
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private void ssca$spellCastingInput(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && !player.isPassenger()
                && player.getUseItem().getItem() instanceof github.com.gengyoubo.sscfa.item.MagicScrollItem) {
            player.input.forwardImpulse *= 5.0F;
            player.input.leftImpulse *= 5.0F;
        }
        if(github.com.gengyoubo.sscfa.spell.OcelotSpellLink.windLocked(player)) {
            player.input.shiftKeyDown=false;player.setShiftKeyDown(false);player.setSprinting(false);
        }
        if (!github.com.gengyoubo.sscfa.spell.SpellChannelManager.isImmobile(player)) return;
        // 閻楄鐣╁锝忕礄瑜版帡鈧?閸欙綀顣伴敍澶涚窗鐎瑰苯鍙忕粋浣诡剾缁夎濮╂稉搴ょ儲鐠哄喛绱?% 缁夊鈧喓鏁遍張宥呭缁旑垰鐫橀幀褍甯囧浼欑礉閻劍鍩?2026-09-18 鐎规氨顭堥敍澶涚幢
        // 閻╁憡婧€/鏉烆剝闊╂禒搴濈瑝閸欐妾洪妴鍌欑箽閻ｆ瑧些閸斻劏绶崗銉ょ瑝濞撳懘娴傞垾鏂衡偓鏃堜缉閸?prediction 閹峰澹囬敍宀勨偓鐔峰鐏炵偞鈧冨嚒娑?0 閺冪姴鐤勯梽鍛秴缁夋眹鈧?        player.input.jumping = false;
        player.input.shiftKeyDown = false;
        player.setSprinting(false);
    }
}
