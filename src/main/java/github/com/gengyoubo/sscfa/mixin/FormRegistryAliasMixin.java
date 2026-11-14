package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Forge 闁插秴鍟撻敍姘愁嚢閸欐牗妫ぐ銏♀偓?ID 閺冩儼顫夐懠鍐ㄥ閿涘矂浼╅崗宥夆偓澶嬪閻ｅ矂娼伴崙铏瑰箛闁插秴顦茶ぐ銏♀偓浣碘偓?*/
@Mixin(value = FormRegistry.class, remap = false)
public abstract class FormRegistryAliasMixin {
    @ModifyVariable(
            method = "get(Lnet/minecraft/resources/ResourceLocation;)Lnet/onixary/shapeShifterCurseForge/form/FormDefinition;",
            at = @At("HEAD"), argsOnly = true, remap = false)
    private static ResourceLocation ssca$registeredFormId(ResourceLocation id) {
        return FormIdMigration.legacy(id);
    }
}
