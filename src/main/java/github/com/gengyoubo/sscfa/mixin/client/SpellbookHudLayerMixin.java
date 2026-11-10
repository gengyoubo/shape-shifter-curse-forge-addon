package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.hud.SpellbookHudRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class SpellbookHudLayerMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
    private void ssca$spellbookBeforeChat(GuiGraphics context, float tickDelta, CallbackInfo ci) {
        SpellbookHudRenderer.renderBeforeChat(context, tickDelta);
        github.com.gengyoubo.sscfa.neomagic.client.NeoClient.renderPixelBarBeforeChat(context,tickDelta);
    }
}
