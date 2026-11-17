package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
// [forge-port] removed yarn wildcard import net.minecraft.entity.*
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.saveddata.SavedData;
import github.com.gengyoubo.sscfa.neomagic.*;
import java.util.*;
import java.util.function.Predicate;

/** Accepted cast snapshot; extra collapse is charged once, only after a real benefit. */
public final class AllaySpellLink {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private AllaySpellLink() {}
    public interface ProjectileData {
        Cast ssca$allay();
        void ssca$allay(Cast cast);
    }
    public static Cast create(ServerPlayer p,double cost,NeoState.Experiment record,boolean directCurse) {
        int kind=FormUtils.isForm(p,FormIdentifiers.ALLAY_SP)?1:
                directCurse&&FormUtils.isForm(p,FormIdentifiers.FALLEN_ALLAY_SP)?2:0;
        return kind==0?null:new Cast(p,kind,cost,record);
    }
    public static Entity owner(Entity source) {return source instanceof Projectile projectile?projectile.getOwner():source;}
    public static Cast context(Entity source) {
        return source instanceof ProjectileData projectile?projectile.ssca$allay():FormSpellStyle.currentAllay();
    }
    public static boolean hostile(ServerPlayer p,LivingEntity target) {
        return target!=p&&target.level()==p.level()&&!target.isSpectator()&&!(target instanceof net.minecraft.world.entity.decoration.ArmorStand)
                &&!WhitelistUtils.isProtected(p,target)&&!p.isAlliedTo(target)
                &&(!(target instanceof ServerPlayer other)||p.canHarmPlayer(other))&&!DomainManager.blocksTargeting(p,target);
    }
    public static final class Cast {
        public final UUID id,owner;
        public final int kind;
        public final double cost,extraRate,healRate,healMinimum,healRange,chainRange;
        private final Set<UUID> members=new HashSet<>();
        private boolean charged,chained,processing;
        private Cast(ServerPlayer p,int kind,double cost,NeoState.Experiment record) {
            this(UUID.fromString(record.id),p.getUUID(),kind,cost,BAL.d("allay_spell_extra_collapse",.15),
                    BAL.d("allay_damage_heal_ratio",.25),BAL.d("allay_damage_heal_minimum",.25),BAL.d("allay_damage_heal_range",16),BAL.d("fallen_curse_chain_range",5));
        }
        private Cast(UUID id,UUID owner,int kind,double cost,double extraRate,double healRate,double healMinimum,double healRange,double chainRange) {
            this.id=id;this.owner=owner;this.kind=kind;this.cost=cost;this.extraRate=extraRate;
            this.healRate=healRate;this.healMinimum=healMinimum;this.healRange=healRange;this.chainRange=chainRange;
        }
        private NeoState.Experiment record(ServerPlayer p) {
            return NeoRuntime.data(p).records.stream().filter(e->e.id.equals(id.toString())).findFirst().orElse(null);
        }
        private boolean actor(ServerPlayer p) {return p.getUUID().equals(owner)&&p.isAlive()&&!p.isRemoved()&&!p.hasDisconnected();}
        private void changed(ServerPlayer p) {
            Store.get(p.getServer()).setDirty();NeoRuntime.dirty(p);
        }
        private void charge(ServerPlayer p) {
            if(charged)return;charged=true;
            double extra=cost*extraRate;
            if(extra>0){var data=NeoRuntime.data(p);data.burden+=extra;data.recovery=NeoRules.current().recoveryDelay();data.lastPaymentTick=p.getServer().getTickCount();}
            var record=record(p);if(record!=null)record.allaySurcharge+=extra;
            changed(p);NeoNetwork.hud(p);
        }
        public void damage(ServerPlayer p,LivingEntity target,double actualHealthLost) {
            if(kind!=1||processing||!actor(p)||actualHealthLost<=0||!Double.isFinite(actualHealthLost)||!hostile(p,target))return;
            List<ServerPlayer> allies=p.serverLevel().getPlayers(other->other.isAlive()&&!other.isSpectator()
                    &&other.getHealth()<other.getMaxHealth()&&p.distanceToSqr(other)<=healRange*healRange
                    &&WhitelistUtils.isBuffTarget(p,other)&&!DomainManager.blocksTargeting(p,other));
            if(allies.isEmpty())return;
            double share=Math.max(healMinimum,actualHealthLost*healRate/allies.size()),healed=0;
            processing=true;
            try{for(var ally:allies){float before=ally.getHealth();ally.heal((float)share);healed+=Math.max(0,ally.getHealth()-before);}}
            finally{processing=false;}
            if(healed>0){var record=record(p);if(record!=null)record.allayHealing+=healed;charge(p);changed(p);}
        }
        /** One successful secondary effect per whole cast; no recursive chain or primary-target overlap. */
        public boolean chain(ServerPlayer p,LivingEntity primary,Set<UUID> excluded,Predicate<LivingEntity> effect) {
            if(kind!=2||chained||processing||!actor(p)||!hostile(p,primary))return false;
            LivingEntity best=null;double distance=chainRange*chainRange;
            for(var candidate:p.serverLevel().getEntitiesOfClass(LivingEntity.class,primary.getBoundingBox().inflate(chainRange),
                    e->e!=primary&&!excluded.contains(e.getUUID())&&FormSpellStyle.legalTarget(p,e))) {
                double d=primary.distanceToSqr(candidate);
                if(d<=distance&&primary.hasLineOfSight(candidate)&&(best==null||d<distance)){best=candidate;distance=d;}
            }
            if(best==null)return false;
            processing=true;boolean applied;
            try{applied=effect.test(best);}finally{processing=false;}
            if(!applied)return false;
            chained=true;var record=record(p);if(record!=null)record.fallenChains++;
            p.serverLevel().sendParticles(ParticleTypes.WITCH,primary.getX(),primary.getY(.5),primary.getZ(),4,.15,.2,.15,0);
            p.serverLevel().sendParticles(ParticleTypes.WITCH,best.getX(),best.getY(.5),best.getZ(),6,.15,.2,.15,0);
            charge(p);changed(p);return true;
        }
        public CompoundTag write() {
            CompoundTag n=new CompoundTag();n.putUUID("Id",id);n.putUUID("Owner",owner);n.putInt("Kind",kind);
            n.putDouble("Cost",cost);n.putDouble("Extra",extraRate);n.putDouble("Heal",healRate);n.putDouble("HealMinimum",healMinimum);n.putDouble("Radius",healRange);n.putDouble("Chain",chainRange);
            n.putBoolean("Charged",charged);n.putBoolean("Chained",chained);return n;
        }
        public static Cast read(CompoundTag n) {
            if(!n.hasUUID("Id")||!n.hasUUID("Owner"))return null;
            int kind=n.getInt("Kind");double cost=n.getDouble("Cost"),extra=n.getDouble("Extra"),heal=n.getDouble("Heal"),radius=n.getDouble("Radius"),chain=n.getDouble("Chain");
            double minimum=n.contains("HealMinimum",Tag.TAG_ANY_NUMERIC)?n.getDouble("HealMinimum"):BAL.d("allay_damage_heal_minimum",.25);
            if((kind!=1&&kind!=2)||!Double.isFinite(cost+extra+heal+minimum+radius+chain)||cost<0||extra<0||extra>1||heal<0||heal>1||minimum<0||minimum>20||radius<0||radius>128||chain<0||chain>128)return null;
            Cast cast=new Cast(n.getUUID("Id"),n.getUUID("Owner"),kind,cost,extra,heal,minimum,radius,chain);
            cast.charged=n.getBoolean("Charged");cast.chained=n.getBoolean("Chained");return cast;
        }
    }
    /** Kept outside player sync data; unloaded projectiles retain membership across restarts. */
    private static final class Store extends SavedData {
        private final Map<UUID,Cast> casts=new HashMap<>();
        static Store get(MinecraftServer server) {return server.overworld().getDataStorage().computeIfAbsent(Store::read,Store::new,"ssca_allay_spell_links");}
        static Store read(CompoundTag n) {
            Store store=new Store();for(String key:n.getAllKeys()){
                CompoundTag entry=n.getCompound(key);Cast cast=Cast.read(entry);if(cast==null)continue;
                ListTag members=entry.getList("Members",Tag.TAG_STRING);
                for(int i=0;i<members.size();i++)try{cast.members.add(UUID.fromString(members.getString(i)));}catch(IllegalArgumentException ignored){}
                if(!cast.members.isEmpty())store.casts.put(cast.id,cast);
            }return store;
        }
        @Override public CompoundTag save(CompoundTag n) {
            casts.forEach((id,cast)->{CompoundTag entry=cast.write();ListTag list=new ListTag();cast.members.forEach(p->list.add(StringTag.valueOf(p.toString())));entry.put("Members",list);n.put(id.toString(),entry);});return n;
        }
        Cast join(Cast proposed,UUID projectile) {
            Cast cast=casts.computeIfAbsent(proposed.id,id->proposed);
            boolean changed=cast.charged!= (cast.charged||proposed.charged)||cast.chained!=(cast.chained||proposed.chained);
            cast.charged|=proposed.charged;cast.chained|=proposed.chained;
            if(cast.members.add(projectile)||changed)setDirty();return cast;
        }
        void leave(Cast cast,UUID projectile) {
            if(cast==null)return;Cast actual=casts.get(cast.id);
            if(actual!=null&&actual.members.remove(projectile)){if(actual.members.isEmpty())casts.remove(cast.id);setDirty();}
        }
    }
    public static void register() {
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityLoad((entity,world)->{
            if(!(entity instanceof ProjectileData data))return;
            Cast cast=FormSpellStyle.currentAllay();
            if(cast!=null&&entity instanceof Projectile projectile&&projectile.getOwner()!=null&&projectile.getOwner().getUUID().equals(cast.owner))data.ssca$allay(cast);
            if(data.ssca$allay()!=null)data.ssca$allay(Store.get(world.getServer()).join(data.ssca$allay(),entity.getUUID()));
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityUnload((entity,world)->{
            var reason=entity.getRemovalReason();
            if(entity instanceof ProjectileData data&&data.ssca$allay()!=null&&(reason==Entity.RemovalReason.DISCARDED||reason==Entity.RemovalReason.KILLED))Store.get(world.getServer()).leave(data.ssca$allay(),entity.getUUID());
        });
    }
}
