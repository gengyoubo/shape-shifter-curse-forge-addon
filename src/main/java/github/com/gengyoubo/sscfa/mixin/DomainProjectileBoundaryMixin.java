package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.DomainManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class DomainProjectileBoundaryMixin {
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void ssca$domainTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
		Projectile projectile = (Projectile) (Object) this;
		if (projectile.isRemoved() || DomainManager.blocksPath(projectile.level(), projectile.position(),
				target.getBoundingBox().getCenter(), 0)) cir.setReturnValue(false);
	}

	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void ssca$domainCollision(HitResult hit, CallbackInfo ci) {
		Projectile projectile = (Projectile) (Object) this;
		if (projectile.isRemoved() || DomainManager.blocksPath(projectile.level(), projectile.position(), hit.getLocation(), 0)) {
			if (!projectile.level().isClientSide) projectile.discard();
			ci.cancel();
		}
	}
}