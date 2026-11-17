package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.renderer.DomainRenderer;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clamp third-person camera movement to the domain shell. */
@Mixin(Camera.class)
public abstract class DomainCameraWallMixin {
	@Shadow protected abstract void setPosition(Vec3 pos);

	@ModifyArg(method = "move", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Camera;setPosition(Lnet/minecraft/world/phys/Vec3;)V"), index = 0)
	private Vec3 ssca$domainCameraWall(Vec3 target) {
		return DomainRenderer.clampCamera(((Camera) (Object) this).getPosition(), target);
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
