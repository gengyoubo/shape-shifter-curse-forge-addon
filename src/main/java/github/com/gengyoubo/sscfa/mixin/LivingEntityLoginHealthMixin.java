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

/** 读取存档时记录服务端玩家的原始血量，交 {@link LoginHealthRestoreHandler} 在最大生命值修饰符挂上后补回。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLoginHealthMixin {
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void ssca$snapshotLoginHealth(CompoundTag nbt, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && nbt.contains("Health", Tag.TAG_ANY_NUMERIC)) {
			LoginHealthRestoreHandler.recordSnapshot(player.getUUID(), nbt.getFloat("Health"));
		}
	}
}
