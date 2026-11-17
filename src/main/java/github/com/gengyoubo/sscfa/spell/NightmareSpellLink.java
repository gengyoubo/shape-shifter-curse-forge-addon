package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.ability.NightmareDreamManager;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.*;
// [forge-port] removed yarn wildcard import net.minecraft.entity.*
// [forge-port] removed yarn wildcard import net.minecraft.entity.effect.*
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Live target lookup, scoped only to spell effects; no extra tick scan or network channel. */
public final class NightmareSpellLink {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private static final ThreadLocal<Optional<ServerPlayer>> EFFECT=new ThreadLocal<>();
    private NightmareSpellLink(){}
    public static ServerPlayer current(){var p=EFFECT.get();return p==null?FormSpellStyle.currentCaster():p.orElse(null);}
    public static boolean with(Entity source,BooleanSupplier effect){
        var previous=EFFECT.get();EFFECT.set(Optional.ofNullable(source instanceof ServerPlayer p?p:null));
        try{return effect.getAsBoolean();}finally{if(previous==null)EFFECT.remove();else EFFECT.set(previous);}
    }
    public static double multiplier(ServerPlayer p,LivingEntity target){
        return p!=null&&!target.level().isClientSide&&FormUtils.isForm(p,FormIdentifiers.WILD_CAT_NIGHTMARE)
                &&FormSpellStyle.legalTarget(p,target)&&NightmareDreamManager.isDreaming(p,target)
                ?BAL.d("nightmare_spell_multiplier",1.35):1;
    }
    public static float damage(LivingEntity target,DamageSource source,float amount){
        if(!(source.getEntity() instanceof ServerPlayer p)
                ||!source.is(SpellDamageSource.SPELL_DAMAGE)&&current()!=p)return amount;
        return (float)(amount*multiplier(p,target));
    }
    public static MobEffectInstance debuff(LivingEntity target,MobEffectInstance effect){
        if(effect.getEffect().getCategory()!=MobEffectCategory.HARMFUL||effect.getDuration()<=0)return effect;
        double factor=multiplier(current(),target);if(factor==1)return effect;
        int duration=(int)Math.min(Integer.MAX_VALUE,Math.ceil(effect.getDuration()*factor-1e-9));
        return new MobEffectInstance(effect.getEffect(),duration,effect.getAmplifier(),effect.isAmbient(),
                effect.isVisible(),effect.showIcon(),null,effect.getFactorData());
    }
}
