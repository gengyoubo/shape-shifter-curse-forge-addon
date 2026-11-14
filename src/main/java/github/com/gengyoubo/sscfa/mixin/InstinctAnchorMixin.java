package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.power.InstinctService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 闁插秴鍟撻敍鍫㈢暆閸栨牭绱氶敍姘拱閼充粙鏁嬬€规碍妞傞崘鑽ょ波閺堫剝鍏橀惃鍕缁?闁插秶鐤嗛敍鍫㈡窗閺嶅洦鏁兼稉?Forge InstinctService閿涘鈧?*/
@Mixin(value = InstinctService.class, remap = false)
public abstract class InstinctAnchorMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$freezeTick(ServerPlayer player, CallbackInfo ci) {
        if (InstinctAnchors.locked(player)) ci.cancel();
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$freezeAdd(Player player, String id, float value, int ticks, boolean immediate, CallbackInfo ci) {
        if (immediate && InstinctAnchors.locked(player)) ci.cancel();
    }

    @Inject(method = "synchronizeHud", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$syncLock(ServerPlayer player, CallbackInfo ci) {
        if (!InstinctAnchors.locked(player)) return;
        net.onixary.shapeShifterCurseForge.api.SscApi.currentForm(player).ifPresent(data ->
                net.onixary.shapeShifterCurseForge.network.ModNetwork.sendInstinctSync(player, data.getInstinctValue(), 0, true, true));
        ci.cancel();
    }
    @Inject(method = "reset", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$freezeReset(Player player, CallbackInfo ci) {
        if (InstinctAnchors.locked(player)) ci.cancel();
    }
}
