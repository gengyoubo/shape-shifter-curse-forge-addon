package github.com.gengyoubo.sscfa.mixin;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TargetingConditions.class)
public class SscAddonTargetPredicateMixin {

	// 缂傛挸鐡?Identifier閿涘矂浼╅崗?AI 缁便垺鏅姗€顣剁捄顖氱窞濮ｅ繑顐?new Identifier 閸掑棝鍘ら妴?
	private static final ResourceLocation SSCA_FOX_SP_VISIBILITY = github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_VISIBILITY;

	@ModifyVariable(method = "test", at = @At("STORE"), ordinal = 0)
	private double modifyMaxDistance(double d, @Nullable LivingEntity baseEntity, LivingEntity targetEntity) {
		// 濞夈劍鍓伴敍姘愁嚉 power 閸氬本妞傞幒鍫滅埃娴ｅ潡鐡?SP 娑撳海瀛╅崼鏇℃儰娴ｅ潡鐡熸稉銈勯嚋瑜般垺鈧緤绱濇稉宥堝厴閹稿宕熸稉鈧ぐ銏♀偓浣虹煐缁涙稏鈧?
		// 閸欏秶绱拠?Apoli 2.9.2 鐠囦礁鐤勯敍姝卭werTypeRegistry.get 鐎佃婀▔銊ュ斀 id 閻╁瓨甯撮幎?IllegalArgumentException
		// 閿涘牊妗堟稉宥堢箲閸?null閿涘本妫禒锝囩垳閻?!= null 閸掋倖鏌囬弰顖涙￥閺佸牓妲诲鈽呯礆閵嗗倸婀?/reload 濞撳懐鈹栭柌宥咃綖缁愭褰涢幋?power 閺傚洣娆㈢紓鍝勩亼閺冭绱?
		// 鐠囥儱绱撶敮闀愮窗閸?AI 缁便垺鏅?tick 閸愬懏鐦?tick 閹舵稑鍤?閳?閸忋劑鍎撮幀顏嗗⒖ AI 瀹曗晜绨濆顏嗗箚閵嗗倸绻€妞よ鍘?contains 閸掋倕鐡ㄩ崷銊ュ晙 get閵?
		// form_familiar_fox_sp_visibility 娴犲懏宸挎禍鍫滆⒈娑擃亞甯虹€硅泛鑸伴幀渚婄礄origins 瀹稿弶鐗抽敍澶涚礉
		// 閸?instanceof PlayerEntity 缁鐡垾鏂衡偓鏃傜卜婢堆冾樋閺?targetEntity 閺勵垱鈧亞澧块敍宀€娲块幒銉ㄧ箲閸ョ偛甯崐纭风礉
		// 閸忓秴骞撻悜顓＄熅瀵板嫪绗傞惃鍕暈閸愬矁銆冮弻銉嚄娑?O(power閺? 閹殿偅寮块妴?
		if (!(targetEntity instanceof net.minecraft.world.entity.player.Player)
				|| !PowerTypeRegistry.contains(SSCA_FOX_SP_VISIBILITY)) {
			return d;
		}
		PowerType<?> powerType = PowerTypeRegistry.get(SSCA_FOX_SP_VISIBILITY);
		if (PowerHolderComponent.KEY.get(targetEntity).hasPower(powerType)) {
			return d * 0.67D;
		}
		return d;
	}
}
