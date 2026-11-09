package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.DomainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class DomainPlayerBoundaryMixin {

	@Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("HEAD"), cancellable = true)
	private void ssca$domainTeleport(ServerLevel world, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
		if (DomainManager.blocksTeleport((Entity) (Object) this, world, new Vec3(x, y, z))) ci.cancel();
	}

	@Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"), cancellable = true)
	private void ssca$domainTeleportFlags(ServerLevel world, double x, double y, double z,
	                                      java.util.Set<net.minecraft.world.entity.RelativeMovement> flags,
	                                      float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
		if (DomainManager.blocksTeleport((Entity) (Object) this, world, new Vec3(x, y, z))) cir.setReturnValue(false);
	}}