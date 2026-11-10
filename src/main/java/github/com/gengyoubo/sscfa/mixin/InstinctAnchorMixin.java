package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.InstinctService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 闁插秴鍟撻敍鍫㈢暆閸栨牭绱氶敍姘拱閼充粙鏁嬬€规碍妞傞崘鑽ょ波閺堫剝鍏橀惃鍕缁?闁插秶鐤嗛敍鍫㈡窗閺嶅洦鏁兼稉?Forge InstinctService閿涘鈧?*/
@Mixin(value = InstinctService.class, remap = false)
public abstract class InstinctAnchorMixin {
    @Inject(method = "reset", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$freezeReset(Player player, CallbackInfo ci) {
        if (InstinctAnchors.locked(player)) ci.cancel();
    }
}
