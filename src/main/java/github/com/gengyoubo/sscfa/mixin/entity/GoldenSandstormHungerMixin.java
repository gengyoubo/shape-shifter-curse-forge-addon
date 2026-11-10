package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 濡ゆ浜堕ˉ銈嗘償閿曚礁娈伴柣鎺曟硾濞叉牜鎮伴埀顒勫箯閿旇棄鐒婚梻鍡楁閸ㄦ岸鎮欓惂鍝ョ闁告艾绉堕弫閬嶅储閸℃钑夐柛妯煎枎濞叉粍绌卞┑鍫熸畬闁挎稑鑻悿鍕⒔閸涱収妲遍柣鐐叉椤﹁法绮斿鍛皻闁哄拋鍨界槐姘跺Υ? *
 * 闂侇偅淇虹换?Redirect 闁瑰嚖闄勯崺?HungerManager#update 闁告劕鎳橀崕鎾儍?{@code player.heal(amount)} 閻犲鍟伴弫銈夋晬? *   - 闂佸弶鍨堕惌娆忕暆濮濆└鐟滆埇鍨洪埀顑跨筏缁辨壆鎹勭€圭姷绠栭柛銉у仩椤㈠懘鏁嶉崼銏℃殸 GoldenSandstormRegen 闁规亽鍎抽鎼佹晬? *   - 閻庨潧瀚弫鎾诲几濠婂棙娴嗛柕鍡楁湰閸斿懘寮婚幘鎰佸壘閻庢稒鍔戦埀顒€绉舵慨鎼佸箑娴ｅ摜鏉藉ù锝嗘惈缁辨壆鎹勭€圭姷绠栭柛銉у仩椤㈠懘鏁嶉崼锝庢蕉閻庨潧瀚弫鎾绘偝閳轰緡鍟€闁哄啰濮电涵鑸电閵夆斂鍋欏瀣枎鐎规娊骞侀姀鐙€妲婚悶娑掑亾闂佹彃楠忕槐? *
 * Mixin 闁告瑯浜濈€氥倝骞?heal 閻犲鍟伴弫銈夊嫉椤掑啴鐓╅柨娑樺缁楀銇愰崡鐐存儥濡ゆ浜堕ˉ?闁肩増顨嗛弳搴㈡償?闁告瑯浜妶杈槹閻旀椿鍚€闁哄啳娉涘▍鎺楀Υ? */
@Mixin(FoodData.class)
public abstract class GoldenSandstormHungerMixin {

	@WrapOperation(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"),
			require = 0
	)
	private void ssc_addon$blockNaturalRegen(Player player, float amount, Operation<Void> original) {
		// 闂佸弶鍨堕惌娆忕暆濮濆└缂佸倷鑳堕弫銈嗩殰闁稁妫ㄩ幖杈剧畳閸ゆ粓鎮炵捄鐑樼閻炴稈鍋?		if (FormUtils.isForm(player, FormIdentifiers.GOLDEN_SANDSTORM_SP)) return;
		// 閻炴凹鍋呴崝鍛村蓟閹炬剚鍓鹃悗娑欏姉婵悂骞€娴ｇ儤鐣遍悗鍦仒缂嶅绮嬫担鐑樻殢濡ゆ浜堕ˉ銈嗘償閿曚礁娈伴柣鎺曟硾濞叉牜鎮伴埀?		if (InfectionSporeManager.isInfected(player.getUUID())) return;
		original.call(player, amount);
	}
}
