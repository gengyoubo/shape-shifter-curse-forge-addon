package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.resource.*;
import github.com.gengyoubo.sscfa.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import github.com.gengyoubo.sscfa.neomagic.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Per-cast blood payment and bounded, server-owned spell-debuff transmission. */
public final class BatSpellLink {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private static final ThreadLocal<Boolean> PROPAGATING=new ThreadLocal<>();
    private static final ThreadLocal<Optional<Cast>> EFFECT=new ThreadLocal<>();
    public static boolean with(Cast cast,java.util.function.BooleanSupplier action){var previous=EFFECT.get();EFFECT.set(Optional.ofNullable(cast));try{return action.getAsBoolean();}finally{if(previous==null)EFFECT.remove();else EFFECT.set(previous);}}
    public static boolean transmitting(){return Boolean.TRUE.equals(PROPAGATING.get());}
    private static boolean copy(LivingEntity target,MobEffectInstance effect,ServerPlayer owner){
        Boolean previous=PROPAGATING.get();PROPAGATING.set(true);
        try{return with(null,()->target.addEffect(effect,owner));}finally{if(previous==null)PROPAGATING.remove();else PROPAGATING.set(previous);}
    }
    public static long queries,attempts;
    private static final boolean PROFILE=Boolean.getBoolean("neomagic.batProfile");
    public static long profileNanos,profileTicks,profileMaxNanos;
    private BatSpellLink() {}
    public interface ProjectileData { Cast ssca$bat(); void ssca$bat(Cast cast); }
    public static Cast current(){var value=EFFECT.get();return value==null?FormSpellStyle.currentBat():value.orElse(null);}
    public static Cast of(Entity source){return source instanceof ProjectileData data?data.ssca$bat():current();}
    public static double bloodMultiplier(net.minecraft.world.entity.player.Player p) {
        return FormUtils.isForm(p,FormIdentifiers.BAT_DESMODUS)&&ResourceBars.get(p,BarKeys.BAT_BLOOD)>=BAL.i("bat_blood_cost",15)
                ?1-BAL.d("bat_blood_reduction",.35):1;
    }
    public static boolean spreading(net.minecraft.world.entity.player.Player p,boolean eligible){return eligible&&FormUtils.isForm(p,FormIdentifiers.BAT_PARASITIC_FRUIT);}
    public static double spreadMultiplier(net.minecraft.world.entity.player.Player p,boolean eligible){return spreading(p,eligible)?1+BAL.d("fruit_debuff_extra_collapse",.25):1;}
    public static Blood planBlood(ServerPlayer p){return new Blood(p,bloodMultiplier(p)<1?BAL.i("bat_blood_cost",15):0);}
    public static final class Blood {
        private final ServerPlayer player;private final int price;private boolean paid;
        public final double multiplier;
        private Blood(ServerPlayer p,int price){player=p;this.price=price;multiplier=price>0?1-BAL.d("bat_blood_reduction",.35):1;}
        public boolean pay(){if(paid)return true;if(price>0&&!ResourceBars.consume(player,BarKeys.BAT_BLOOD,price))return false;paid=true;return true;}
        public int spent(){return paid?price:0;}
    }
    public static final class Cast {
        public UUID id,owner;
        public double radius;
        public int dwell,children,generations,npcBonus;
        public Cast() {}
        public Cast(ServerPlayer p,NeoState.Experiment record){id=UUID.fromString(record.id);owner=p.getUUID();radius=BAL.d("fruit_debuff_radius",3);dwell=BAL.i("fruit_debuff_dwell_ticks",50);children=BAL.i("fruit_debuff_children",2);generations=BAL.i("fruit_debuff_generations",3);npcBonus=github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements.fruitDebuffLevelBonus();}
        public CompoundTag write(){CompoundTag n=new CompoundTag();n.putUUID("Id",id);n.putUUID("Owner",owner);n.putDouble("Radius",radius);n.putInt("Dwell",dwell);n.putInt("Children",children);n.putInt("Generations",generations);n.putInt("NpcBonus",npcBonus);return n;}
        private boolean valid(){return id!=null&&owner!=null&&Double.isFinite(radius)&&radius>0&&radius<=16&&dwell>=1&&dwell<=1200&&children>=1&&children<=2&&generations>=1&&generations<=3&&npcBonus>=0&&npcBonus<=255;}
        public static Cast read(CompoundTag n){if(!n.hasUUID("Id")||!n.hasUUID("Owner"))return null;Cast c=new Cast();c.id=n.getUUID("Id");c.owner=n.getUUID("Owner");c.radius=n.getDouble("Radius");c.dwell=n.getInt("Dwell");c.children=n.getInt("Children");c.generations=n.getInt("Generations");c.npcBonus=n.getInt("NpcBonus");return c.valid()?c:null;}
    }
    private static final class Debuff {
        String type;int amplifier,npcBonus;long until;boolean ambient,particles,icon;
        Debuff() {}
        Debuff(MobEffectInstance e,long now){type=BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect()).toString();amplifier=e.getAmplifier();until=now+e.getDuration();ambient=e.isAmbient();particles=e.isVisible();icon=e.showIcon();}
        MobEffect effect(){return BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(type));}
    }
    private static final class Node {
        UUID target;int generation=1,children;
        Map<String,Debuff> effects=new LinkedHashMap<>();
        transient Map<UUID,Long> near=new HashMap<>();
    }
    private static final class Group {
        Cast cast;String world;long expires;
        transient ResourceKey<net.minecraft.world.level.Level> worldKey;
        Map<UUID,Node> nodes=new LinkedHashMap<>();
    }
    private static final class Saved {Map<UUID,Group> groups=new LinkedHashMap<>();}
    private static final class Store extends SavedData {
        Saved saved=new Saved();
        final Map<UUID,Set<Node>> index=new HashMap<>();
        void index(Node node){index.computeIfAbsent(node.target,id->Collections.newSetFromMap(new IdentityHashMap<>())).add(node);}
        void unindex(Node node){var nodes=index.get(node.target);if(nodes!=null){nodes.remove(node);if(nodes.isEmpty())index.remove(node.target);}}
        static Store get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(Store::read,Store::new,"ssca_bat_spell_links");}
        static Store read(CompoundTag n){
            Store s=new Store();if(!n.contains("Data",Tag.TAG_BYTE_ARRAY))return s;
            Saved saved=NeoState.JSON.fromJson(new String(n.getByteArray("Data"),StandardCharsets.UTF_8),Saved.class);
            if(saved==null||saved.groups==null)throw new IllegalArgumentException("Invalid bat spell state");
            for(var entry:saved.groups.entrySet()){
                Group g=entry.getValue();if(g==null||g.cast==null||!g.cast.valid()||!entry.getKey().equals(g.cast.id)||ResourceLocation.tryParse(g.world)==null||g.nodes==null)throw new IllegalArgumentException("Invalid bat cast");
                for(var node:g.nodes.entrySet()){
                    Node v=node.getValue();if(v==null||!node.getKey().equals(v.target)||v.generation<1||v.generation>g.cast.generations||v.children<0||v.children>g.cast.children||v.effects==null)throw new IllegalArgumentException("Invalid bat node");
                    for(var d:v.effects.values())if(d==null||d.type==null||ResourceLocation.tryParse(d.type)==null||d.amplifier<0||d.amplifier>255||d.npcBonus<0||d.npcBonus>d.amplifier)throw new IllegalArgumentException("Invalid bat effect");
                }
            }s.saved=saved;saved.groups.values().forEach(g->g.nodes.values().forEach(s::index));return s;
        }
        @Override public CompoundTag save(CompoundTag n){n.putInt("Version",1);n.putByteArray("Data",NeoState.JSON.toJson(saved).getBytes(StandardCharsets.UTF_8));return n;}
    }
    /** Called only after a spell successfully adds a harmful status; fire and native powers never enter here. */
    public static void applied(Cast cast,LivingEntity target,MobEffectInstance applied,Entity source){
        if(cast==null||applied==null||applied.getDuration()<=0||applied.getEffect().getCategory()!=MobEffectCategory.HARMFUL||!(target.level() instanceof ServerLevel world))return;
        ServerPlayer owner=world.getServer().getPlayerList().getPlayer(cast.owner);
        if(owner==null||!FormSpellStyle.legalTarget(owner,target))return;
        MobEffectInstance active=target.getEffect(applied.getEffect());if(active==null)return;
        Store s=Store.get(world.getServer());Group g=s.saved.groups.computeIfAbsent(cast.id,id->{Group v=new Group();v.cast=cast;v.world=world.dimension().location().toString();return v;});
        if(!g.world.equals(world.dimension().location().toString()))return;
        Node node=g.nodes.computeIfAbsent(target.getUUID(),id->{Node v=new Node();v.target=id;s.index(v);return v;});
        Debuff d=new Debuff(applied,world.getGameTime());d.amplifier=Math.min(d.amplifier,active.getAmplifier());
        boolean enhanced=github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements.isEnemyNpc(source,target)
                &&source instanceof LivingEntity living&&FormUtils.isForm(living,FormIdentifiers.BAT_PARASITIC_FRUIT);
        d.npcBonus=enhanced?Math.min(d.amplifier,cast.npcBonus):0;d.until=Math.min(d.until,world.getGameTime()+active.getDuration());
        node.effects.put(d.type,d);g.expires=Math.max(g.expires,d.until+200);s.setDirty();
    }
    private static List<Debuff> active(Node n,LivingEntity source,long now){
        List<Debuff> active=new ArrayList<>();
        Iterator<Debuff> it=n.effects.values().iterator();
        while(it.hasNext()){
            var d=it.next();MobEffect type=d.effect();if(type==null){it.remove();continue;}
            MobEffectInstance effect=source.getEffect(type);
            if(effect==null||d.until<=now||effect.getDuration()<=0||effect.getAmplifier()<d.amplifier){it.remove();continue;}
            Debuff copy=new Debuff();copy.type=d.type;copy.amplifier=d.amplifier;copy.npcBonus=d.npcBonus;copy.until=Math.min(d.until,now+effect.getDuration());copy.ambient=d.ambient;copy.particles=d.particles;copy.icon=d.icon;active.add(copy);
        }return active;
    }
    private static void tick(MinecraftServer server){
        Store s=Store.get(server);Iterator<Group> groups=s.saved.groups.values().iterator();
        while(groups.hasNext()){
            Group g=groups.next();if(g.worldKey==null)g.worldKey=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(g.world));ServerLevel world=server.getLevel(g.worldKey);
            if(world==null)continue;long now=world.getGameTime();
            if(now>=g.expires){g.nodes.values().forEach(s::unindex);groups.remove();s.setDirty();continue;}
            ServerPlayer owner=server.getPlayerList().getPlayer(g.cast.owner);
            if(owner==null||owner.level()!=world||!owner.isAlive())continue;
            // Existing contacts get cheap distance checks each tick: a brief exit must reset exposure.
            // New neighbors are found only by the staggered local query below, once per five ticks.
            for(Node n:g.nodes.values())if(n.near!=null&&!n.near.isEmpty()){
                Entity source=world.getEntity(n.target);
                if(!(source instanceof LivingEntity living)||!living.isAlive()){n.near.clear();continue;}
                n.near.keySet().removeIf(id->{Entity e=world.getEntity(id);return !(e instanceof LivingEntity target)||!target.isAlive()||g.nodes.containsKey(id)||living.distanceToSqr(target)>g.cast.radius*g.cast.radius;});
            }
            List<Node> batch=new ArrayList<>();
            for(Node n:g.nodes.values())if(n.generation<g.cast.generations&&n.children<g.cast.children&&Math.floorMod(n.target.hashCode(),5)==Math.floorMod(server.getTickCount(),5))batch.add(n);
            for(Node n:batch){
                if(n.near==null)n.near=new HashMap<>();
                Entity entity=world.getEntity(n.target);
                if(!(entity instanceof LivingEntity source)){n.near.clear();continue;}
                if(!source.isAlive()||source.isRemoved()){n.near.clear();n.effects.clear();s.setDirty();continue;}
                List<Debuff> effects=active(n,source,now);if(effects.isEmpty()){n.near.clear();continue;}
                queries++;
                List<LivingEntity> nearby=world.getEntitiesOfClass(LivingEntity.class,source.getBoundingBox().inflate(g.cast.radius),e->!g.nodes.containsKey(e.getUUID())&&FormSpellStyle.legalTarget(owner,e)&&source.distanceToSqr(e)<=g.cast.radius*g.cast.radius);
                Set<UUID> present=new HashSet<>();nearby.forEach(e->present.add(e.getUUID()));n.near.keySet().retainAll(present);
                nearby.sort(Comparator.<LivingEntity>comparingDouble(source::distanceToSqr).thenComparing(e->e.getUUID().toString()));
                for(LivingEntity target:nearby){
                    long since=n.near.computeIfAbsent(target.getUUID(),id->now);if(now-since<=g.cast.dwell)continue;
                    attempts++;Node child=new Node();child.target=target.getUUID();child.generation=n.generation+1;
                    for(Debuff d:effects){int duration=(int)Math.min(Integer.MAX_VALUE,d.until-now);int base=Math.max(0,d.amplifier-d.npcBonus),bonus=target instanceof net.minecraft.world.entity.player.Player?0:g.cast.npcBonus;int amplifier=base+bonus;
                        if(duration>0&&copy(target,new MobEffectInstance(d.effect(),duration,amplifier,d.ambient,d.particles,d.icon),owner)){
                        MobEffectInstance received=target.getEffect(d.effect());
                        if(received!=null){Debuff actual=new Debuff(received,now);actual.amplifier=Math.min(amplifier,received.getAmplifier());actual.npcBonus=Math.min(bonus,Math.max(0,actual.amplifier-base));actual.until=Math.min(d.until,actual.until);child.effects.put(actual.type,actual);}
                    }}
                    if(child.effects.isEmpty())continue;
                    g.nodes.put(child.target,child);s.index(child);n.children++;n.near.remove(child.target);s.setDirty();
                    var record=NeoRuntime.data(owner).records.stream().filter(r->r.id.equals(g.cast.id.toString())).findFirst().orElse(null);if(record!=null){record.sporeInfections++;NeoRuntime.dirty(owner);}
                    if(n.children>=g.cast.children){n.near.clear();break;}
                }
            }
        }
    }
    public static void cured(LivingEntity target,MobEffect effect){
        if(!(target.level() instanceof ServerLevel world)||world.getServer()==null||world.getServer().overworld()==null)return;
        Store s=Store.get(world.getServer());Set<Node> nodes=s.index.get(target.getUUID());if(nodes==null)return;
        String key=effect==null?null:BuiltInRegistries.MOB_EFFECT.getKey(effect).toString();
        for(Node node:nodes){if(key==null)node.effects.clear();else node.effects.remove(key);node.near.clear();}s.setDirty();
    }
    public static void register(){github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server->{
        if(!PROFILE){tick(server);return;}
        long before=System.nanoTime();tick(server);long elapsed=System.nanoTime()-before;
        profileNanos+=elapsed;profileTicks++;profileMaxNanos=Math.max(profileMaxNanos,elapsed);
    });}
}
