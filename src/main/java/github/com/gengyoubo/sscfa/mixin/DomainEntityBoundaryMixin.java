package github.com.gengyoubo.sscfa.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class DomainEntityBoundaryMixin {

	@ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
	private Vec3 ssca$domainMovement(Vec3 movement) {
		return DomainManager.limitMovement((Entity) (Object) this, movement);
	}

	@ModifyExpressionValue(method = "move", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 ssca$domainFinalMovement(Vec3 movement) {
		return DomainManager.finishMovement((Entity) (Object) this, movement);
	}

	@Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
	private void ssca$domainPosition(double x, double y, double z, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (entity.level() instanceof ServerLevel world && world.getEntity(entity.getId()) == entity
				&& DomainManager.blocksTeleport(entity, world, new Vec3(x, y, z))) {
			if (entity instanceof net.minecraft.world.entity.projectile.Projectile) entity.discard();
			ci.cancel();
		}
	}

	@Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"), cancellable = true)
	private void ssca$domainTeleport(ServerLevel world, double x, double y, double z,
	                                 java.util.Set<net.minecraft.world.entity.RelativeMovement> flags,
	                                 float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
		if (DomainManager.blocksTeleport((Entity) (Object) this, world, new Vec3(x, y, z))) cir.setReturnValue(false);
	}}
