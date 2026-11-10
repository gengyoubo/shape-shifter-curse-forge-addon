package github.com.gengyoubo.sscfa.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.sscfa.client.SpellcastClient;
import github.com.gengyoubo.sscfa.spell.spells.LunarPhaseSpell;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LunarTargetColorMixin {
    // Set the final buffer color after Apoli's glow powers have chosen their colors.
    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void ssca$lunarColor(Entity entity, double x, double y, double z, float tickDelta,
            PoseStack matrices, MultiBufferSource consumers, CallbackInfo ci) {
        if (SpellcastClient.isLunarTarget(entity) && consumers instanceof OutlineBufferSource outline) {
            int color = LunarPhaseSpell.HIGHLIGHT_COLOR;
            outline.setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, 255);
        }
    }
}
