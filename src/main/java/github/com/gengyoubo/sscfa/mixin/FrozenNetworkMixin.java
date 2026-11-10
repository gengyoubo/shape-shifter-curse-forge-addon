package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.anomaly.FrozenControl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks vanilla gameplay actions; mod protocol/configuration payloads keep their normal receivers. */
@Mixin(value = ServerGamePacketListenerImpl.class, priority = 2000)
public abstract class FrozenNetworkMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Shadow private int aboveGroundTickCount;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ssca$allowFrozenSuspension(CallbackInfo ci) {
        if (FrozenControl.blocked(player)) {
            clientIsFloating = false;
            aboveGroundTickCount = 0;
        }
    }

    @ModifyVariable(method = "handleMovePlayer", at = @At("HEAD"), argsOnly = true)
    private ServerboundMovePlayerPacket ssca$restrictMove(ServerboundMovePlayerPacket packet) {
        if (!player.getServer().isSameThread()) return packet;
        if (FrozenControl.blocked(player)) {
            // Keep vanilla position/collision validation: falling, entity pushes and piston motion must be accepted.
            return FrozenControl.lockLook(player,packet);
        }
        return packet;
    }

    @Inject(method = {"handleInteract", "handleUseItemOn", "handleUseItem", "handlePlayerAction",
            "handlePlayerCommand", "handleAnimate", "handleContainerClick", "handleSetCreativeModeSlot", "handleContainerButtonClick",
            "handleSetCarriedItem", "handlePlayerInput", "handleMoveVehicle", "handleRecipeBookSeenRecipePacket", "handlePlaceRecipe"},
            at = @At("HEAD"), cancellable = true)
    private void ssca$blockActions(CallbackInfo ci) {
        // The off-thread invocation is queued by vanilla; its main-thread replay executes this guard.
        if (player.getServer().isSameThread() && FrozenControl.blocked(player)) ci.cancel();
    }
}
