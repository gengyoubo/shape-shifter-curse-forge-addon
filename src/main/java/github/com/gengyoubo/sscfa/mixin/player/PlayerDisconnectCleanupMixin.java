package github.com.gengyoubo.sscfa.mixin.player;

import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerDisconnectCleanupMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void ssc_addon$cleanupBeforeSaving(ServerPlayer player, CallbackInfo ci) {
        PlayerSessionCleanup.disconnect(player.connection, player.getServer());
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void ssc_addon$forgetSavedAbsorption(ServerPlayer player, CallbackInfo ci) {
        var online = player.getServer().getPlayerList().getPlayer(player.getUUID());
        if (online == null || online.connection == player.connection) {
            github.com.gengyoubo.sscfa.ability.PlayDeadAbsorptionManager.forgetPlayer(player.getUUID());
        }
    }
}
