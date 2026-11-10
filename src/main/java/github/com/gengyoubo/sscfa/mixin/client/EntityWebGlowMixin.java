package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.SpellcastClient;
import github.com.gengyoubo.sscfa.client.WebHighlightClient;
import github.com.gengyoubo.sscfa.client.renderer.DomainRenderer;
import github.com.gengyoubo.sscfa.spell.spells.LunarPhaseSpell;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 客户端专属：让高亮表内的实体在本机描边（仅施法者可见）。 */
@Mixin(Entity.class)
public class EntityWebGlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true, require = 0)
    private void ssca$webHighlightGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (DomainRenderer.blocksTargetingClient(self)) {
            cir.setReturnValue(false);
            return;
        }
        if (!cir.getReturnValueZ() && WebHighlightClient.isHighlighted(self.getId())) {
            cir.setReturnValue(true);
        }
        if (!cir.getReturnValueZ() && github.com.gengyoubo.sscfa.client.MancianimaCrosshairTracker.isCurrent(self.getUUID())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void ssca$webHighlightColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (SpellcastClient.isLunarTarget(self)) {
            cir.setReturnValue(LunarPhaseSpell.HIGHLIGHT_COLOR);
            return;
        }
        if (github.com.gengyoubo.sscfa.client.MancianimaCrosshairTracker.isCurrent(self.getUUID())) {
            cir.setReturnValue(0x55FF55);
            return;
        }
        int id = self.getId();
        if (WebHighlightClient.isHighlighted(id)) {
            cir.setReturnValue(WebHighlightClient.getHighlightColor(id));
        }
    }
}
