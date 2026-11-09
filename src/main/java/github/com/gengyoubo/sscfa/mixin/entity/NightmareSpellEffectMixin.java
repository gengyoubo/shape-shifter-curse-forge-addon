package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.spell.NightmareSpellLink;
// [forge-port] removed yarn wildcard import net.minecraft.entity.*
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntity.class)
public class NightmareSpellEffectMixin {
    @ModifyVariable(method="hurt",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float ssca$dreamSpellDamage(float amount,DamageSource source,float original){
        return NightmareSpellLink.damage((LivingEntity)(Object)this,source,amount);
    }
    @ModifyVariable(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private MobEffectInstance ssca$dreamSpellDuration(MobEffectInstance effect,MobEffectInstance original,Entity source){
        return NightmareSpellLink.debuff((LivingEntity)(Object)this,effect);
    }
}
