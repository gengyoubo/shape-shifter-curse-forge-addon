package github.com.gengyoubo.sscfa.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 閹劖鍎崇憴鍡涘櫣闂勬劕鍩楅悽顭掔窗閻╁瓨甯撮弳鎾苟 {@code LivingEntity.activeStatusEffects}閿涘牊鏅ラ弸婊冪杽娓?Map閿涘鈧? *
 * <p>閼冲本娅欓敍姘汗閹呮畱闂嗛箖娓剁憰浣碘偓灞姐亼閺勫簺鈧秳缍旀稉楦挎祰娴ｆ搫绱檝anilla 闂嗗墽顓哥痪?+ 閸忓濂?blindness uniform 闁€燁吇鐎瑰喛绱氶敍? * 娴ｅ棗褰堢€瑰疇鈧懎鑸伴幀浣烘畱 Apoli 閺佸牊鐏夐崗宥囨焻閹凤附顒?addStatusEffect 閸忋劑鍎寸捄顖氱窞閿涘本鏅犻惄瀛樺复閸?Map 缂佹洝绻冮弬钘夊闁? * 閿涘牆鍘ら悿顐ゎ吀娑撳秴鍩?Map 閸愭瑥鍙嗛敍宀冣偓宀勬禈缁狅紕鍤庡В蹇撴姎閸欘亝鐓?Map閿涘鈧倸鍘滆ぐ鍙樼瑓婢惰鲸妲戦梿鎹愵潶 IrisShaderPackMixin
 * 閸愬懎鐡ㄩ弴鎸庡床娑撴亽鈧瞼鐭囬懝?+ 12閳?6 閺嶇鈧秶澧楅張顒婄幢閺冪姴鍘滆ぐ杈ㄦ閻?FearFogMixin 鐟曞棛娲婇梿鎹愮獩/闂嗘崘澹婇妴?/p>
 */
@Mixin(LivingEntity.class)
public interface LivingEntityStatusEffectsAccessor {

	@Accessor("activeEffects")
	Map<MobEffect, MobEffectInstance> sscAddon$getActiveStatusEffects();
}
