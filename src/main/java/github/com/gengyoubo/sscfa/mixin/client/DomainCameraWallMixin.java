package github.com.gengyoubo.sscfa.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.client.renderer.DomainRenderer;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 妫板棗鐓欓惄鍛婃簚婢ф瑱绱檍ackcooper閿?026-09-21 閸欏秹顩敍澶涚窗缁楊兛绗佹禍铏剐為惄鍛婃簚閹峰绻欓弮鏈电瑝瀵版鈹涢崙娲暙閸╃喓鎮嗘竟鐐解偓? *
 * <p>瀹告彃寮界紓鏍槯閺嶇鐤?1.20.1 {@code Camera.update} 鐠嬪啰鏁ゆ惔蹇ョ窗
 * {@code setPosition(閻溾晛顔嶉惇鑲╂涧閹绘帒鈧棿缍? 閳?clipToSpace(4 閺嶅吋鏌熼崸妤勵梿閸? 閳?moveBy(閺傜懓鎮?鑴?閹峰绻欓柌?}閿? * {@code moveBy} 鐏忛箖鍎存禒?{@code setPosition(Vec3d 閻╊喗鐖ｇ紒婵嗩嚠閸ф劖鐖?} 閽€鎴掔秴閿涘牆鐡ч懞鍌滅垳 143 鐞涘矉绱氶妴? * 閺?mixin 閻?{@code @WrapOperation} 閸栧懓顥婃潻娆庣濞?{@code setPosition(Vec3d)} 鐠嬪啰鏁ら敍? * 閼汇儯鈧瞼娴夐張楦挎崳閻愮櫢绱欒ぐ鎾冲 pos閿涘苯宓嗛悳鈺侇啀閻偐娼ч敍澶嗗晪 閹峰绻欓惄顔界垼閵嗗秷娉曠搾濠囶暙閸╃喎锛撴潏鍦櫕閿涘奔绨╅崚鍡欑級閸ョ偛鍩岀拹鏉戯紦閸愬懍鏅堕敍? * 閻╁憡婧€鐞氼偆鏆€閸︺劌锛撻崘鍛偓鏂衡偓鏃傤儑娑撳姹夌粔鎷岊潒鐟欐帊绗岀粭顑跨娴滆櫣袨娑撯偓閺嶉婀呮稉宥堫潌妫板棗鐓欐径鏍劥閵?/p>
 *
 * <p>閸掋倕鐣炬稉搴ｇ級閺€鎯ь槻閻?{@link DomainRenderer#clampCamera(Vec3, Vec3)}閿涘牅绗岄張宥呭缁旑垰鎮撴稉鈧總? * DomainRules 閸戠姳缍?+ 娴滃苯鍨庣粻妤佺《閿涘绱遍悳鈺侇啀閻偐娼ф稉宥呮躬娴犺缍嶆竟鍐插敶閿涘牆鎯堥弮鐘诧紦/婢瑰啿顦婚敍澶嬫娑撳秴鍏辨０鍕┾偓? * 閹碘晛绱堕張鐔凤紦閸氬本鐗遍悽鐔告櫏閿涘牏娴夐張鍝勫毉娑撳秴骞撻敍宀€顑侀崥鍫濆礋閸氭垿姒鹃敍澶堚偓?/p>
 */
@Mixin(Camera.class)
public abstract class DomainCameraWallMixin {
	@Shadow protected abstract void setPosition(Vec3 pos);

	@WrapOperation(method = "move", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Camera;setPosition(Lnet/minecraft/world/phys/Vec3;)V"))
	private void ssca$domainCameraWall(Camera camera, Vec3 target, Operation<Void> original) {
		Vec3 from = camera.getPosition();
		Vec3 clamped = DomainRenderer.clampCamera(from, target);
		original.call(camera, clamped);
	}

	/**
	 * 缂佸牊顥呴崗婊冪俺閿?026-09-22 閸欏秹顩穱顔筋劀閿涘绱皍pdate 鐏忛箖鍎撮張鈧崥搴㈢墡妤犲奔绔村▎锛勬祲閺堝搫娼楅弽鍥ｂ偓鏂衡偓鏂剧瑝缁犫€冲闂堛垽鎽肩捄?	 * 閿涘牆鎯堢粭顑跨瑏閺傚湱娴夐張?鐟欏棜顫楀Ο锛勭矋閸?moveBy 娑斿鎮楅弨鐟板晸鏉?pos閿涘顩ф担鏇礉閸欘亣顩﹂悳鈺侇啀閻偐娼ч崷銊ワ紦閸愬懓鈧瞼娴夐張?	 * 缂佸牏鍋ｉ崷銊ワ紦婢舵牭绱濈亸鍗炲繁閸掕埖濡搁惄鍛婃簚閸樺娲栨竟鍐插敶閿涘牆绶為崥鎴炲瑜版唻绱氶妴鍌濈箹娣囨繆鐦夌粭顑跨瑏娴滆櫣袨鐟欏棜顫楅崙杞扮瑝閸樻眹鈧?	 */
	@Inject(method = "setup", at = @At("TAIL"))
	private void ssca$domainCameraFinalClamp(net.minecraft.world.level.BlockGetter area, net.minecraft.world.entity.Entity focused,
	                                         boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (!thirdPerson) return;
		Vec3 eye = focused == null ? null : focused.getPosition(tickDelta).add(0, focused.getEyeHeight(), 0);
		if (eye == null) return;
		Vec3 clamped = DomainRenderer.projectInside(eye, ((Camera) (Object) this).getPosition());
		if (clamped != null) setPosition(clamped);
	}
}
