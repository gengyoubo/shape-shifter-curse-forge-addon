package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.renderer.DomainRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 濡澘妫楅悡娆愮珶娴煎鍙鹃梻鍛⒒椤洭鏁嶉崸鏈糲kcooper闁?026-09-21 闂傚洠鍋撴慨鐟板亰缁辨岸鏁嶅棰濇殭闁糕晝鍠庨ˇ濠氭儍閸曨亝宕插ù锝嗘礀閿涙劙妫呴搹顐ｏ骏婵炲娲滈埞娑欐交閸ヮ剛鎷ㄩ柤纭呭皺閹棙绔? * 閻炴凹鍋勯敍鎾诲礃閸涱垰璐熼悗纭呮硾閹宕氱敮顔剧闁告瑥绉崇粻锝嗙閿旀儳濮ч柍銉︽煛閳ь剚姊瑰﹢浼村捶閹殿喖璐熼悗瑙勬构缁楀本绔熸导瀵稿従婵犙勫姇閸ㄥ海浠﹂悙璇х处闁告劕鎳庨ˇ濠氬础閹惰姤楗梻濠勵劜缁辨岸濡? *
 * <p>婵炲鍔岄崣鍡涙倷?{@code SoundSystem.play(SoundInstance)} HEAD闁挎稒淇洪姘跺棘鐟欏嫮銆婄€殿喒鍋撳鑸垫綑瀹撳棝骞愭径濠勬澖濞撴艾顑呰ぐ? * WeightedSoundSet 鐎?Channel闁挎稑鐣盿ncel 闁瑰搫顦銊︾▔瀹ュ嫮绐楅柛鎺撶☉缂傛挻绔熸导缈犲闁挎稑鑻悾顒勫礂閵娾晜楗梻濠勵劜缁辨瑥顔忛幓鎺戝唨缂傚倹鐗為惁褔寮界粙璺ㄦ澖闁挎稑顦埀?/p>
 *
 * <p>閻犵偘绀侀崢銈夋晬閸喐锟ユ繛澶嬫礀閻ｇ偓鎷呭鍥ㄧ暠闁活潿鍔戦埀顒佹閻撳爼鏁嶇仦鎯ь仭闁规惌浜欑槐鎵嫚椤栨瑦绺洪柨娑橆檧缁?/p>
 * <ul>
 *   <li>{@code isRelative()}=true闁挎稑鐗忓ù澶屸偓鐢垫嚀閹鎳撻崨顖涚暠 UI/闁绘粠鍨伴。銊╂缁涘湱绀夐柡鍐С缁楁﹢鎮剧仦鑺ョ稄闁哄秴娴勭槐姘舵晬?/li>
 *   <li>MUSIC / WEATHER / AMBIENT 缂侇偉顕ч崺鍡涙晬閸︽摜M闁靛棔绶氬ù鍕⒖閵娧呮惣闁稿繈鍔岄惇顒€顫濆☉妯荤函闁挎稑鏈Λ銈団偓瑙勭煯缂嶅懐鎷犻婵堢枀闁挎稑顦埀?/li>
 * </ul>
 *
 * <p>闁告帇鍊曢悾楣冨极閻楀牆绁︽繝褎鍔х槐鐨桜link DomainRenderer} 鐎圭寮堕弫褰掑礆閹殿喗鐣遍柡鍫濈Т婵喓绮╅鐑嗘殭闁糕晝鍠庨幃鎾愁潰閵夈劊鈧啴鏁嶉崼婵囧創闁圭鏅涚槐鍫曞嫉閻斿嘲纾圭€垫澘瀚哥槐? * 濠㈢娅曢崹姘跺垂鐎ｎ亜绁梻鍛⒒椤洭鏁嶆径娑氬耿闁哄牜鍓欏﹢鎾偝閳轰緡鍟€濞戞挸鎼幆澶愭嚀閸涱偀鍋撻崒娆戠煂閻庡箍鍨洪崺娑氱博椤栨縿鈧啴鎮抽弶璺ㄦ勾闁挎稑鑻ˇ鎸庣鏉炴壆顏遍柤鐤彧缁辨瑩宕ラ崟顓т紓闁告艾瀚崵婊堝箰婢跺﹥鍊卞☉鎾亾闁告艾鏈鐐垫偘閵娿儱鐏查悗瑙勭啲缁辨岸濡?/p>
 */
@Mixin(SoundEngine.class)
public abstract class DomainSoundBarrierMixin {
	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
	private void ssca$domainMuffleSound(SoundInstance sound, CallbackInfo ci) {
		if (sound.isRelative()) return;
		SoundSource category = sound.getSource();
		if (category == SoundSource.MUSIC || category == SoundSource.WEATHER
				|| category == SoundSource.AMBIENT) return;
		if (DomainRenderer.blocksSoundForListener(sound.getX(), sound.getY(), sound.getZ())) ci.cancel();
	}
}
