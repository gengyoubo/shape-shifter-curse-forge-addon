package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 閺夆晛鈧喖鍤嬬€殿喒鍋撳┑顔碱儐濡炲倻鎮伴妷銉ョ岛閻庡湱鍋樼紞瀣偐閼哥鍋撴担瑙勬珡闁哄绮岀€垫﹢鏁嶉崼銏㈢瓘濞存粌妫旈柌婊呯礃婵犲洤甯冲┑鍌涱殣缁?026-09-30 闁哄秶鎳撳ú婊勭┍椤旂⒈妲婚柨娑橆槶閳?
 *
 * <p><b>闁哄秶鎳撳ú婊堟晬閸繂鍐€缂傚倹鐗為惁?class_3231 閻庡湱鍋犻惁澶愭晬?/b>闁?.20.1 闁?{@code EntityTrackerEntry.sendPackets}
 * 闁告瑯浜滆ぐ?spawn / 闁稿繐鍟弳鐔煎箲?/ 閻忕偟鍋為埀?/ 閻熶礁鎳庨ˇ?/ 濞戞梹锚椤撳綊宕犻崪鍐<b>濞戞挸绉撮幆鍫熺鐠佸磭绉块柡浣哥墛閻忓宕?/b>闁炽儲鏌￠埀顒佹⒒鐢櫣鈧娉涚槐鎴炴叏鐎ｎ厽瀚归棅顏嶄簷缁旀挳宕?
 * 閻㈩垽闄勯弲銉╁几濠婂懏鐣?mob 闁哄啳顔愮槐娆撴焻閳ь剟宕欏ú顏勬閺夆晜绋忛埀顑挎祰閾斿娼诲鍐ㄦ櫃閻犙勬緲濞叉牠濡存担绋块殬闁秆勵殜閸ｅ憡娼弬銈囩闁挎稑鑻褰掑箣妞嬪寒浼傚☉鎾崇Ф閻擄繝鏌嗛幘宕囨殜閻㈩垽濡囧鍐极閸喓浜柕鍡楀€歌ぐ澶岀礃?NPC
 * 闁汇劌瀚憰鍡涘蓟?mixin 闁?{@code hasStatusEffect(SHEEP_FORM)}闁挎稑鑻褰掑箣妞嬪寒浼傚〒姘€鍕埅 false 闁?缂佹柨顑呴崺銏ゆ偨鐠囧弶绀€闁告鍠愯啯闁?
 * 闁挎稑鐗勯埀顒€鐬肩敮铏光偓鐟扮埣閳ь兘鍋撻柛鎴濇惈閸熲偓闁搞儳鍋炲鐢电礃婵犲嫮褰岄柛鎺曨嚙瑜板宕堕悙鎻掓枾闁搞劌顑冮埀顒€绉跺▓鎴︽偨鏉堛劍闄嶉柨娑橆槶閳?.20.3+ 闁告鍠撴晶妤€顔忛幓鎺撹含閺夆晛鈧喖鍤嬮柛鏍ф嚇閸ｉ鎮伴妷銈囩憪闁轰礁鐗婇悘澶愬触鐏炵虎鍔勯柨?
 * 闁?mixin 閻庨潧缍婄紞鍫㈡嫚閵夘煈鏀藉☉鎾翠航閳?/p>
 *
 * <p><b>鐟滄澘宕幖鐑芥?/b>闁挎稒鑹鹃崣蹇涙煂韫囨澶愬矗閹达綆鍤夐悗鍦仒缂嶅銇愰幘鍐差枀闁圭鍋撻柡鍫濐槹閺呫儵寮稿鎰濞?1.20.3+ 闁告鍠撴晶妤佺▔閳ь剟鎳涙潏鍓х闁炽儲鏌￠埀顒佹閵嗗海鏁敂鎸庡弿濠㈣泛绉撮崣鍓р偓?
 * SSCA 闁轰礁鐗婇悘澶愭晬閸絿妲掗柛婊勫笒瀹撳啰鎷?婵炵⒈鍠楅惈鍫ュ礄韫囨稈鍋?閻炴稈鍋撻梻鍡樺劤閼镐即骞€娴ｈ櫣鎼奸柨娑橆槸濠€顏堟煂瀹ュ牊瀚归棅顏嶄簻閹鈧箍鍨洪崺娑氱博椤栨瑦涓㈠鎯伴哺濡绮堥搹瑙勭暠闁告艾鐬肩悮顐︽⒒椤曗偓椤ｄ粙濡?
 * 閻庡箍鍨洪崺娑氱博椤栨稑鐦婚柡浣哥墛閻?id 閻熸洖妫涘ú濠傤嚕韫囨柨娼戦柛鏃傚缁辨繈鏌屽鍜佹Щ閻炴稏鍎辫ぐ鍌炵嵁閸屾粎鎼奸柨娑欑⊕閺呫儵寮稿鍐ㄧ厒闁哄牏鍠愬鍌炲储閻斿搫顣奸柣鎾楀啰鍩楅柛娆愬灩浜涢梻鍕╁€曠€垫﹢鏁嶇仦鐐粯缂備礁鐗呯粩鎾嚊濞ｎ兘鍋?/p>
 */
@Mixin(ServerEntity.class)
public abstract class EntityTrackerEntryEffectSyncMixin {

	@Shadow
	private Entity entity;

	@Inject(method = "addPairing", at = @At("TAIL"))
	private void ssca$syncStatusEffects(ServerPlayer player, CallbackInfo ci) {
		if (entity instanceof LivingEntity living) {
			for (MobEffectInstance effect : living.getActiveEffects()) {
				player.connection.send(new ClientboundUpdateMobEffectPacket(living.getId(), effect));
			}
		}
	}
}
