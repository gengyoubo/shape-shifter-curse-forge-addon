package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.DomainManager;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DomainNetworkBoundaryMixin {
	@Shadow public ServerPlayer player;
	@Shadow private Vec3 awaitingPositionFromClient;
	@Shadow private Entity lastVehicle;
	@Shadow public abstract void teleport(double x, double y, double z, float yaw, float pitch);

	@Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;containsInvalidValues(DDDFF)Z"), cancellable = true)
	private void ssca$domainWalk(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
		if (awaitingPositionFromClient != null || player.wonGame || player.isPassenger()) return;
		Vec3 next = new Vec3(packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ()));
		if (!Double.isFinite(next.x) || !Double.isFinite(next.y) || !Double.isFinite(next.z)
				|| !Float.isFinite(packet.getYRot(player.getYRot())) || !Float.isFinite(packet.getXRot(player.getXRot()))) return;
		if (DomainManager.blocksTeleport(player, player.level(), next)) {
			// 上抬候选点也必须留在边界内；无安全落点时纠正回原位，确保纠正包能发出。
			Vec3 clamped = DomainManager.liftOutOfBlocks(player, DomainManager.clampToBoundary(player, next));
			teleport(clamped.x, clamped.y, clamped.z, packet.getYRot(player.getYRot()), packet.getXRot(player.getXRot()));
			ci.cancel();
		}
	}

	@Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;containsInvalidValues(DDDFF)Z"), cancellable = true)
	private void ssca$domainRide(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
		var vehicle = player.getRootVehicle();
		if (vehicle == player || vehicle != lastVehicle || vehicle.getControllingPassenger() != player) return;
		Vec3 next = new Vec3(packet.getX(), packet.getY(), packet.getZ());
		if (!Double.isFinite(next.x) || !Double.isFinite(next.y) || !Double.isFinite(next.z)
				|| !Float.isFinite(packet.getYRot()) || !Float.isFinite(packet.getXRot())) return;
		if (DomainManager.blocksTeleport(vehicle, player.level(), next)) {
			// 载具同款钳制 + 方块安全化（防陷边角方块/地底）
			Vec3 clamped = DomainManager.liftOutOfBlocks(vehicle, DomainManager.clampToBoundary(vehicle, next));
			vehicle.teleportTo(clamped.x, clamped.y, clamped.z);
			player.connection.send(new ClientboundMoveVehiclePacket(vehicle));
			ci.cancel();
		}
	}

	@Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
	private void ssca$domainTeleport(double x, double y, double z, float yaw, float pitch,
	                                 java.util.Set<net.minecraft.world.entity.RelativeMovement> flags, CallbackInfo ci) {
		if (DomainManager.blocksTeleport(player, player.level(), new Vec3(x, y, z))) ci.cancel();
	}
}
