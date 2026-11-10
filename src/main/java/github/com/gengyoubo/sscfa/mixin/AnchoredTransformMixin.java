package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 闁插秴鍟撻敍姘拱閼充粙鏁嬬€规碍瀚嗙紒婵嗙暚閺佹潙褰夐煬顐ヮ嚞濮瑰偊绱欓惄顔界垼閺€閫涜礋 Forge TransformManager.startTransform閿涘鈧?*/
@Mixin(value = TransformManager.class, remap = false)
public abstract class AnchoredTransformMixin {
    @Inject(method = "startTransform(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$rejectStart(ServerPlayer player, ResourceLocation id,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (InstinctAnchors.blocksChange(player, FormRegistry.get(id))) cir.setReturnValue(false);
    }
}
