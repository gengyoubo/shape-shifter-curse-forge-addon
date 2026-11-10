package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 闁插秴鍟撻敍姘拱閼充粙鏁嬬€规碍妞傞弳鍌滅处閺堝牆顧侀崣妯鸿埌閿涘牏娲伴弽鍥ㄦ暭娑?Forge CursedMoonService閿涘鈧?*/
@Mixin(value = CursedMoonService.class, remap = false)
public abstract class AnchoredCursedMoonMixin {
    @Inject(method = {"applyStartCursedMoonEffect", "applyEndCursedMoonEffect"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void ssca$deferMoonTransform(ServerPlayer player, CallbackInfo ci) {
        if (InstinctAnchors.locked(player)) ci.cancel();
    }
}
