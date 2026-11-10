package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 闂備焦褰冪粔鎾疮閹剧粯鏅慨姗嗗幗閹烽亶鏌ら崗鍛煓闁轰礁顑囬埀顒冾潐瀹€绋课熼崱妤婃桨闁靛繒濮鹃崺宀勬煙椤戣法顦︾憸鏉款樀瀵瓨娼忛崜褏顦╅梺鐑╂櫓閸犳鎮ラ敐澶婄哗闂侇偅绋栫粈?Forge FormManager.setForm闂佹寧绋戦ˇ顓㈠焵?*/
@Mixin(value = FormManager.class, remap = false)
public abstract class AnchoredFormMixin {
    @Inject(method = "setForm(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$keepForm(Player player, ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if (InstinctAnchors.blocksChange(player, FormRegistry.get(id))) cir.setReturnValue(false);
    }

    @Inject(method = "setForm(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;Z)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$keepFormForced(Player player, ResourceLocation id, boolean force,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (InstinctAnchors.blocksChange(player, FormRegistry.get(id))) cir.setReturnValue(false);
    }
}
