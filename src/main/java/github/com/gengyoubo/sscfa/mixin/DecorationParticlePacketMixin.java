package github.com.gengyoubo.sscfa.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.network.DecorationParticles;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Covers vanilla /particle, Apoli spawn_particles and SSC circles without changing their spawning logic. */
@Mixin(ServerLevel.class)
public abstract class DecorationParticlePacketMixin {
    @WrapOperation(method = "sendParticles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void ssca$sendDecoration(ServerGamePacketListenerImpl handler, Packet<?> packet, Operation<Void> original) {
        if (!DecorationParticles.trySendScoped((ServerLevel) (Object) this, handler.player, packet))
            original.call(handler, packet);
    }
}
