package github.com.gengyoubo.sscfa.mixin.effect;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SscAddonTravelMixin {

	// aliases 鍏煎 intermediary(field_6362)/yarn(jumping) 杩愯鏃舵槧灏勶紝闃层€宩umping was not located銆嶅穿婧?
	@Shadow(aliases = {"field_6362", "jumping"})
	protected boolean jumping;

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void adaptiveSwimming(Vec3 movementInput, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;

		// 骞介浘鍖栧舰椋炶锛堟偊鐏靛悓娆撅級锛氫粎瀹㈡埛绔湰鍦扮帺瀹舵帴绠＄Щ鍔紝鏈嶅姟绔潬浣嶇疆鍚屾 + setNoGravity 璞佸厤鎮┖韪汉銆?
		// WASD 鎺у埗姘村钩绉诲姩骞朵弗鏍奸攣閫?4.5 鏍?s锛?.225 鏍?tick锛夛紱璺宠穬閿笂娴€佹綔琛岄敭涓嬮檷銆佹棤杈撳叆缂撻檷銆?
		if (entity.level().isClientSide && entity instanceof Player mistPlayer
				&& entity.hasEffect(SscAddon.MIST_FORM)) {
			// 鍑濊仛鐖嗙牬钃勫姏鏈熼棿鏁翠綋鍑忛€?50%锛堝甫 MIST_CHARGING 鏍囪鏃讹級
			double maxH = entity.hasEffect(SscAddon.MIST_CHARGING) ? 0.1125 : 0.225; // 0.225 鏍?tick = 4.5 鏍?s

			// 姘村钩锛氭寜鏈濆悜(yaw)灏?WASD 杈撳叆杞负涓栫晫鏂瑰悜锛屽綊涓€鍖栧悗涓ユ牸閿佸畾涓?maxH锛堝墠鍚庡乏鍙虫枩鍚戝悓閫燂級
			float yaw = entity.getYRot() * 0.017453292F;
			Vec3 fwd = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
			Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
			Vec3 horiz = fwd.scale(movementInput.z).add(right.scale(movementInput.x));
			if (horiz.lengthSqr() > 1.0E-6) {
				horiz = horiz.normalize().scale(maxH);
			} else {
				horiz = Vec3.ZERO;
			}

			// 鍨傜洿锛氳烦璺冮敭涓婃诞锛屾綔琛岄敭涓嬮檷锛屽惁鍒欑紦闄嶏紙钀ラ€犳偊鐏垫紓娴笅娌夋劅锛?
			double vy;
			if (this.jumping) {
				vy = maxH;
			} else if (mistPlayer.isShiftKeyDown()) {
				vy = -maxH;
			} else {
				vy = Math.max(entity.getDeltaMovement().y - 0.02, -0.10);
			}

			Vec3 v = new Vec3(horiz.x, vy, horiz.z);
			entity.setDeltaMovement(v);
			entity.move(MoverType.SELF, v);
			entity.fallDistance = 0f;
			ci.cancel();
			return;
		}
	}
}
