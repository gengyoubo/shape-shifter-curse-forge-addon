package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.spell.BatSpellLink;
// [forge-port] removed yarn wildcard import net.minecraft.entity.*
// [forge-port] removed yarn wildcard import net.minecraft.entity.effect.*
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the accepted spell-effect context may seed a contagion. */
@Mixin(LivingEntity.class)
public class BatSpellDebuffMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("RETURN"))
    private void ssca$spellDebuff(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ()){
            BatSpellLink.applied(BatSpellLink.current(),(LivingEntity)(Object)this,effect,source);
            github.com.gengyoubo.sscfa.spell.CourtSpellLink.status(github.com.gengyoubo.sscfa.spell.CourtSpellLink.current(),(LivingEntity)(Object)this,effect);
        }
    }
    @Inject(method="removeEffectNoUpdate",at=@At("RETURN"))
    private void ssca$cleansed(MobEffect effect,CallbackInfoReturnable<MobEffectInstance> cir){
        if(cir.getReturnValue()!=null)BatSpellLink.cured((LivingEntity)(Object)this,effect);
    }
    @Inject(method="removeAllEffects",at=@At("RETURN"))
    private void ssca$allCleansed(CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValueZ())BatSpellLink.cured((LivingEntity)(Object)this,null);}
}
