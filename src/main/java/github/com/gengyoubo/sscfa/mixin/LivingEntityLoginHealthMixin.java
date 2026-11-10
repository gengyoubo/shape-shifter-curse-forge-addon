package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.event.LoginHealthRestoreHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 鐠囪褰囩€涙ɑ銆傞弮鎯邦唶瑜版洘婀囬崝锛勵伂閻溾晛顔嶉惃鍕斧婵顢呴柌蹇ョ礉娴?{@link LoginHealthRestoreHandler} 閸︺劍娓舵径褏鏁撻崨钘夆偓闂存叏妤楁壆顑侀幐鍌欑瑐閸氬氦藟閸ョ偑鈧?*/
@Mixin(LivingEntity.class)
public abstract class LivingEntityLoginHealthMixin {
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void ssca$snapshotLoginHealth(CompoundTag nbt, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && nbt.contains("Health", Tag.TAG_ANY_NUMERIC)) {
			LoginHealthRestoreHandler.recordSnapshot(player.getUUID(), nbt.getFloat("Health"));
		}
	}
}
