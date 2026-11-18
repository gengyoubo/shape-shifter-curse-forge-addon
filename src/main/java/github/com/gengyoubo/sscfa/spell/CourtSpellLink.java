package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.ability.*;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.resource.*;
import github.com.gengyoubo.sscfa.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
// [forge-port] removed yarn wildcard import net.minecraft.entity.*
// [forge-port] removed yarn wildcard import net.minecraft.entity.effect.*
import net.minecraft.nbt.*;
// [forge-port] removed yarn wildcard import net.minecraft.registry.*
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
// [forge-port] removed yarn wildcard import net.minecraft.util.math.*
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.compat.MinionTracker;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Per-cast commands and brand hit deduplication. Only active commands are ticked. */
public final class CourtSpellLink {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private static final ThreadLocal<Optional<Cast>> EFFECT=new ThreadLocal<>();
    private static final ThreadLocal<Integer> DAMAGE=new ThreadLocal<>();
    private CourtSpellLink(){}
    public interface ProjectileData {Cast ssca$court();void ssca$court(Cast cast);}
    public static Cast current(){var c=EFFECT.get();return c==null?FormSpellStyle.currentCourt():c.orElse(null);}
    public static Cast of(Entity e){return e instanceof ProjectileData d?d.ssca$court():current();}
    public static boolean suppressLegacy(){return DAMAGE.get()!=null||current()!=null;}
    public static boolean with(Cast cast,BooleanSupplier action){var prior=EFFECT.get();EFFECT.set(Optional.ofNullable(cast));try{return action.getAsBoolean();}finally{if(prior==null)EFFECT.remove();else EFFECT.set(prior);}}
    public static boolean damage(Cast cast,Entity owner,LivingEntity target,BooleanSupplier action){
        var prior=DAMAGE.get();DAMAGE.set(prior==null?1:prior+1);boolean hit;
        try{hit=action.getAsBoolean();}finally{if(prior==null)DAMAGE.remove();else DAMAGE.set(prior);}
        if(hit&&owner instanceof ServerPlayer p)hit(cast,p,target);return hit;
    }
    public static void status(Cast cast,LivingEntity target,MobEffectInstance effect){
        if(cast==null||effect.getDuration()<=0||effect.getEffect().getCategory()!=MobEffectCategory.HARMFUL||!(target.level() instanceof ServerLevel w))return;
        var p=w.getServer().getPlayerList().getPlayer(cast.owner);if(p!=null)hit(cast,p,target);
    }
    public static Cast create(ServerPlayer p,github.com.gengyoubo.sscfa.neomagic.NeoState.Experiment record){
        int kind=FormUtils.isForm(p,FormIdentifiers.ANUBIS_WOLF_SP)?1:FormUtils.isGoldenSandstormSP(p)?2:0;
        if(kind==0)return null;var c=new Cast();c.id=UUID.fromString(record.id);c.owner=p.getUUID();c.kind=kind;c.price=BAL.i("court_soul_cost",5);return c;
    }
    public static final class Cast {
        public UUID id,owner;public int kind,price;public boolean ordered;long expires;
        final Set<UUID> hits=new HashSet<>(),members=new HashSet<>();
        public CompoundTag write(){var n=new CompoundTag();n.putUUID("Id",id);n.putUUID("Owner",owner);n.putInt("Kind",kind);n.putInt("Price",price);n.putBoolean("Ordered",ordered);n.putLong("Expires",expires);putIds(n,"Hits",hits);putIds(n,"Members",members);return n;}
        public static Cast read(CompoundTag n){if(!n.hasUUID("Id")||!n.hasUUID("Owner"))return null;var c=new Cast();c.id=n.getUUID("Id");c.owner=n.getUUID("Owner");c.kind=n.getInt("Kind");c.price=n.getInt("Price");if(c.kind<1||c.kind>2||c.price<0||c.price>1000)return null;c.ordered=n.getBoolean("Ordered");c.expires=n.getLong("Expires");readIds(n,"Hits",c.hits);readIds(n,"Members",c.members);return c;}
    }
    private static void putIds(CompoundTag n,String key,Set<UUID> ids){var list=new ListTag();ids.forEach(id->list.add(StringTag.valueOf(id.toString())));n.put(key,list);}
    private static void readIds(CompoundTag n,String key,Set<UUID> ids){var list=n.getList(key,Tag.TAG_STRING);for(int i=0;i<list.size();i++)try{ids.add(UUID.fromString(list.getString(i)));}catch(IllegalArgumentException ignored){}}
    private static final class Order {
        UUID owner,wolf,target;String dimension;long until,life;int previousAttack;boolean gone;
        CompoundTag write(){var n=new CompoundTag();n.putUUID("Owner",owner);n.putUUID("Wolf",wolf);if(target!=null)n.putUUID("Target",target);n.putString("World",dimension);n.putLong("Until",until);n.putLong("Life",life);n.putInt("Attack",previousAttack);n.putBoolean("Gone",gone);return n;}
        static Order read(CompoundTag n){if(!n.hasUUID("Owner")||!n.hasUUID("Wolf")||ResourceLocation.tryParse(n.getString("World"))==null)return null;var o=new Order();o.owner=n.getUUID("Owner");o.wolf=n.getUUID("Wolf");o.target=n.hasUUID("Target")?n.getUUID("Target"):null;o.dimension=n.getString("World");o.until=n.getLong("Until");o.life=n.getLong("Life");o.previousAttack=n.getInt("Attack");o.gone=n.getBoolean("Gone");return o;}
    }
    private static final class Store extends SavedData {
        final Map<UUID,Cast> casts=new HashMap<>();final Map<UUID,Order> orders=new HashMap<>();
        static Store get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(Store::read,Store::new,"ssca_court_spell_links");}
        static Store read(CompoundTag n){var s=new Store();var casts=n.getList("Casts",Tag.TAG_COMPOUND);for(int i=0;i<casts.size();i++){var c=Cast.read(casts.getCompound(i));if(c!=null)s.casts.put(c.id,c);}var orders=n.getList("Orders",Tag.TAG_COMPOUND);for(int i=0;i<orders.size();i++){var o=Order.read(orders.getCompound(i));if(o!=null)s.orders.put(o.wolf,o);}return s;}
        @Override public CompoundTag save(CompoundTag n){var cs=new ListTag();casts.values().forEach(c->cs.add(c.write()));n.put("Casts",cs);var os=new ListTag();orders.values().forEach(o->os.add(o.write()));n.put("Orders",os);return n;}
        Cast join(Cast proposed,long now){var c=casts.computeIfAbsent(proposed.id,id->proposed);if(!c.owner.equals(proposed.owner)||c.kind!=proposed.kind)return null;c.ordered|=proposed.ordered;c.hits.addAll(proposed.hits);c.members.addAll(proposed.members);c.expires=Math.max(c.expires,now+1200);setDirty();return c;}
    }
    public static void hit(Cast proposed,ServerPlayer p,LivingEntity target){
        if(proposed==null||!p.getUUID().equals(proposed.owner)||!FormSpellStyle.legalTarget(p,target)
                ||proposed.kind==1&&!FormUtils.isForm(p,FormIdentifiers.ANUBIS_WOLF_SP)||proposed.kind==2&&!FormUtils.isGoldenSandstormSP(p))return;
        var s=Store.get(p.getServer());var c=s.join(proposed,p.getServer().overworld().getGameTime());if(c==null)return;
        if(c.kind==2){if(c.hits.add(target.getUUID())){proposed.hits.add(target.getUUID());GoldenSandstormErosionBrand.onSpellHit(p,target);s.setDirty();}return;}
        if(c.ordered||AnubisWolfSpSoulEnergy.getEnergy(p)<c.price)return;
        var world=p.serverLevel();AnubisWolfMinionEntity wolf=null;boolean hasWolf=false;
        for(UUID id:MinionTracker.get(p,MinionTracker.ANUBIS_WOLF)){
            if(world.getEntity(id) instanceof AnubisWolfMinionEntity w&&w.isAlive()&&p.getUUID().equals(w.getOwnerUUID())){
                hasWolf=true;if(!DomainManager.blocksTargeting(w,target)&&(wolf==null||w.distanceToSqr(target)<wolf.distanceToSqr(target)))wolf=w;
            }
        }
        boolean summoned=false;
        if(wolf==null&&!hasWolf){var pos=safePosition(p,target);if(pos==null)return;wolf=AnubisWolfSpSummonWolves.spawnSpellWolf(p,pos,AnubisWolfSpDeathDomain.hasActiveDomain(p.getUUID()),false);summoned=wolf!=null;}
        if(wolf==null)return;
        if(!ResourceBars.consume(p,BarKeys.ANUBIS_SOUL,c.price)){if(summoned)remove(p,wolf);return;}
        long now=world.getGameTime();var order=s.orders.get(wolf.getUUID());if(order==null){order=new Order();order.owner=p.getUUID();order.wolf=wolf.getUUID();order.dimension=world.dimension().location().toString();}
        if(summoned)order.life=now+AnubisWolfSpSummonWolves.spellWolfDuration();
        order.target=target.getUUID();order.until=now+BAL.i("court_order_ticks",100);order.previousAttack=wolf.getLastHurtMobTimestamp();s.orders.put(order.wolf,order);
        wolf.setOrderedToSit(false);wolf.setTarget(target);wolf.getNavigation().moveTo(target,BAL.d("court_rush_speed",1.5));
        c.ordered=true;proposed.ordered=true;s.setDirty();
    }
    /** Finite loaded-chunk search, real support/collision/fluid/hazard checks; never teleport a wolf. */
    public static BlockPos safePosition(ServerPlayer p,LivingEntity target){
        var world=p.serverLevel();double start=p.getRandom().nextDouble()*Math.PI*2;
        double minimum=BAL.d("court_spawn_min_distance",5);
        for(int ring=0;ring<3;ring++)for(int i=0;i<16;i++){
            double angle=start+i*Math.PI/8,radius=minimum+.75+ring;int x=(int)Math.floor(target.getX()+Math.cos(angle)*radius),z=(int)Math.floor(target.getZ()+Math.sin(angle)*radius);
            for(int dy=2;dy>=-3;dy--){var at=new BlockPos(x,(int)Math.floor(target.getY())+dy,z);var center=Vec3.atBottomCenterOf(at);
                if(center.distanceToSqr(target.position())<=minimum*minimum||p.distanceToSqr(center)>900||!world.getChunkSource().hasChunk(at.getX()>>4,at.getZ()>>4)||!world.getWorldBorder().isWithinBounds(at)||!world.isInWorldBounds(at))continue;
                var ground=at.below();var below=world.getBlockState(ground);if(!below.isFaceSturdy(world,ground,Direction.UP)||!world.getFluidState(at).isEmpty()||!world.getFluidState(ground).isEmpty())continue;
                if(below.is(Blocks.MAGMA_BLOCK)||below.is(Blocks.CACTUS)||below.is(BlockTags.CAMPFIRES))continue;
                var box=net.onixary.shapeShifterCurseForge.registry.ModEntities.ANUBIS_WOLF_MINION.get().getDimensions().makeBoundingBox(center);if(!world.noCollision(box)||world.getBlockState(at).is(BlockTags.FIRE)||world.getBlockState(at).is(Blocks.SWEET_BERRY_BUSH))continue;
                if(DomainManager.blocksCrossBoundary(world,p.position(),center)||DomainManager.blocksCrossBoundary(world,target.position(),center))continue;
                return at;
            }
        }return null;
    }
    private static void remove(ServerPlayer owner,AnubisWolfMinionEntity wolf){MinionTracker.remove(owner,MinionTracker.ANUBIS_WOLF,wolf.getUUID());net.onixary.shapeShifterCurseForge.power.AnubisMinionService.unregister(owner,wolf.getUUID());wolf.discard();}
    private static void tick(MinecraftServer server){
        if(server.getTickCount()%5!=0)return;var s=Store.get(server);long global=server.overworld().getGameTime();
        if(server.getTickCount()%20==0&&s.casts.values().removeIf(c->c.members.isEmpty()&&c.expires<global))s.setDirty();
        var it=s.orders.values().iterator();while(it.hasNext()){
            var o=it.next();var world=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(o.dimension)));if(world==null)continue;
            var owner=server.getPlayerList().getPlayer(o.owner);var entity=world.getEntity(o.wolf);if(o.gone){it.remove();s.setDirty();continue;}if(!(entity instanceof AnubisWolfMinionEntity wolf)){if(world.getGameTime()>o.until&&o.life==0){it.remove();s.setDirty();}continue;}
            boolean valid=owner!=null&&owner.isAlive()&&owner.level()==world&&FormUtils.isForm(owner,FormIdentifiers.ANUBIS_WOLF_SP)&&o.owner.equals(wolf.getOwnerUUID());
            if(!wolf.isAlive()||!valid||o.life>0&&world.getGameTime()>=o.life){if(o.life>0)remove(owner,wolf);else {wolf.setTarget(null);wolf.getNavigation().stop();}it.remove();s.setDirty();continue;}
            if(o.target!=null){var e=world.getEntity(o.target);if(world.getGameTime()>=o.until||wolf.getLastHurtMobTimestamp()!=o.previousAttack||!(e instanceof LivingEntity target)||!FormSpellStyle.legalTarget(owner,target)||DomainManager.blocksTargeting(wolf,target)){
                if(wolf.getTarget()!=null&&wolf.getTarget().getUUID().equals(o.target)){wolf.setTarget(null);wolf.getNavigation().stop();}o.target=null;if(o.life==0)it.remove();s.setDirty();
            }else{wolf.setTarget(target);wolf.getNavigation().moveTo(target,BAL.d("court_rush_speed",1.5));}}
        }
    }
    public static void register(){
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(CourtSpellLink::tick);
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityLoad((e,w)->{if(e instanceof ProjectileData d){var proposed=current();if(d.ssca$court()==null&&proposed!=null&&e instanceof net.minecraft.world.entity.projectile.Projectile projectile&&projectile.getOwner()!=null&&projectile.getOwner().getUUID().equals(proposed.owner))d.ssca$court(proposed);if(d.ssca$court()!=null){var s=Store.get(w.getServer());var c=s.join(d.ssca$court(),w.getServer().overworld().getGameTime());if(c!=null){c.members.add(e.getUUID());d.ssca$court(c);s.setDirty();}}}});
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityUnload((e,w)->{if(e.getRemovalReason()!=Entity.RemovalReason.DISCARDED&&e.getRemovalReason()!=Entity.RemovalReason.KILLED)return;if(e instanceof ProjectileData d&&d.ssca$court()!=null){var s=Store.get(w.getServer());var c=s.casts.get(d.ssca$court().id);if(c!=null&&c.members.remove(e.getUUID()))s.setDirty();}if(e instanceof AnubisWolfMinionEntity){var s=Store.get(w.getServer());var order=s.orders.get(e.getUUID());if(order!=null){order.gone=true;s.setDirty();}}});
    }
}
