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
			// 娑撳﹥濮崐娆撯偓澶屽仯娑旂喎绻€妞よ崵鏆€閸︺劏绔熼悾灞藉敶閿涙稒妫ょ€瑰鍙忛拃鐣屽仯閺冨墎绫傚锝呮礀閸樼喍缍呴敍宀€鈥樻穱婵堢眰濮濓絽瀵橀懗钘夊絺閸戞亽鈧?
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
			// 鏉炶棄鍙块崥灞绢儥闁藉啿鍩?+ 閺傜懓娼＄€瑰鍙忛崠鏍电礄闂冩煡娅℃潏纭咁潡閺傜懓娼?閸︽澘绨抽敍?
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
