package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.util.SscIgnitedEntityAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class SscAddonEntityIgnitionMixin implements SscIgnitedEntityAccessor {

	@Unique
	private UUID sscAddon$igniterUuid;

	@Override
	public void sscAddon$setIgniterUuid(UUID uuid) {
		this.sscAddon$igniterUuid = uuid;
	}

	@Override
	public UUID sscAddon$getIgniterUuid() {
		return this.sscAddon$igniterUuid;
	}

	@Inject(method = "saveWithoutId", at = @At("HEAD"))
	private void injectWriteNbt(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
		if (this.sscAddon$igniterUuid != null) {
			nbt.putUUID("SscAddonIgniter", this.sscAddon$igniterUuid);
		}
	}

	@Inject(method = "load", at = @At("HEAD"))
	private void injectReadNbt(CompoundTag nbt, CallbackInfo ci) {
		if (nbt.contains("SscAddonIgniter")) {
			this.sscAddon$igniterUuid = nbt.getUUID("SscAddonIgniter");
		}
	}

	@ModifyArg(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 0)
	private DamageSource modifyFireDamageSource(DamageSource source) {
		if (source.is(DamageTypes.ON_FIRE)) {
			UUID igniter = this.sscAddon$getIgniterUuid();
			if (igniter != null) {
				Entity entity = (Entity) (Object) this;
				if (!entity.level().isClientSide) {
					Player player = entity.level().getPlayerByUUID(igniter);
					if (player != null) {
						return new DamageSource(source.typeHolder(), null, player);
					}
				}
			}
		}
		return source;
	}

	/**
	 * 閻熶礁鎳忛鎾晬閸︻檼AYING_DEAD闁挎稑顦板﹢锟犳⒒閹绢喗鏁氶悗瑙勪亢椤鎲撮幒鐐电闁告瑦鐗楃粔椋庢喆閸℃凹娼￠弶鍫熸尭閸欏棝濡撮崒锔剧闁?SscAddonEntityMixin 闁告艾鐗嗛懟鐔兼嚊閾忣偒鍔冮柨娑欑椤㈡垶绋夋潪鎵憹闁告瑦菧閳ь剙鍋婄槐?	 */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$onChangeLookDirectionPlayDead(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
		if ((Object) this instanceof LivingEntity entity && entity.hasEffect(SscAddon.PLAYING_DEAD)) {
			ci.cancel();
		}
	}
}
