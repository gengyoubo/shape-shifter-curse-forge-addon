package github.com.gengyoubo.sscfa.mixin;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SSC Forge 1.10.0.6 checks {@link SscJavaRegistries} for entity actions but not for bi-entity
 * actions: {@code executeBiEntity} only knows its built-in types and drops the rest. The addon's
 * {@code apoli:action_on_hit} powers (venom fang, passive wolf summon, erosion brand hit, ...) use
 * custom bi-entity types, so this dispatches registered Java actions first, with SSC's own
 * (actor, target) meaning.
 */
@Mixin(value = FormPowerRuntime.class, remap = false)
public abstract class FormPowerRuntimeBiEntityMixin {
    @Inject(method = "executeBiEntity(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lcom/google/gson/JsonObject;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void ssca$javaBiEntityAction(Player actor, LivingEntity target, JsonObject action, CallbackInfo ci) {
        if (action == null || target == null) return;
        ResourceLocation type = ResourceLocation.tryParse(FormPowerRegistry.typeOf(action));
        if (type != null && !"apoli".equals(type.getNamespace())
                && SscJavaRegistries.executeAction(type, actor, target, action)) ci.cancel();
    }
}
