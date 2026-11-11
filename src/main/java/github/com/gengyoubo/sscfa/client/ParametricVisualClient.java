package github.com.gengyoubo.sscfa.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import github.com.gengyoubo.sscfa.client.particle.FirstPersonParticles;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.Circle;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.Key;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.Kind;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.View;
import github.com.gengyoubo.sscfa.network.visual.SceneRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Cosmetic sampling only. The server retains every gameplay decision and scene lifetime. */
public final class ParametricVisualClient {
    private static final int CIRCLE_BUDGET = 4096, MAX_QUEUED_PARTICLES = 262144, MAX_QUEUED_CIRCLES = 4096;
    private static final Map<Key, Runtime> scenes = new LinkedHashMap<>();
    private static final VisualSampleHistory<Key> expiredSamples = new VisualSampleHistory<>(ParametricVisuals.MAX_SCENES);
    private static final ArrayDeque<CircleJob> circles = new ArrayDeque<>();
    private static ClientLevel world;
    private static Pending pending;
    private static PendingDelta pendingDelta;
    private static final Map<Key, TimedMotion> pendingMotions = new LinkedHashMap<>();
    private static boolean initialized, hasSequence;
    private static int sequence, budget = CIRCLE_BUDGET, ticks, receivedAt;
    private static long referenceServer, referenceLocal, lastWorldTick = Long.MIN_VALUE;
    private static long statePackets, deltaPackets, motionPackets, circlePackets, sampledBatches;
    private static long emittedParticles, deferredParticles, droppedParticles, malformedPackets, queuedParticles;
    private static Observer observer;

    @FunctionalInterface
    public interface Observer {
        void observe(View view, int sampleAge, long sampleTick, int batchIndex, ParticleOptions particle,
                     double x, double y, double z, int count, double dx, double dy, double dz,
                     double speed, boolean decoration, boolean projectile);
    }
    public record Metrics(int scenes, int pendingCircles, long statePackets, long motionPackets,
                          long circlePackets, long sampledBatches, long emittedParticles,
                          long deferredParticles, long droppedParticles, long malformedPackets) {}
    private record StatePage(ResourceLocation dimension, long time, int sequence, int page, int pages, List<View> views) {}
    private record DeltaPage(long time, int sequence, int base, int page, int pages, List<View> views, List<Key> removed) {}
    private record Motion(Key key, Vec3 position, Vec3 velocity) {}
    private record TimedMotion(long time, int receivedAt, Motion motion) {}
    private static final class Pending {
        final int sequence, createdAt;
        final long time;
        final List<View>[] pages;
        int count;
        @SuppressWarnings("unchecked")
        Pending(StatePage page) {
            sequence = page.sequence; time = page.time; createdAt = ticks;
            pages = (List<View>[]) new List<?>[page.pages];
        }
    }
    private static final class PendingDelta {
        final int sequence, base, createdAt;
        final long time;
        final DeltaPage[] pages;
        int count;
        PendingDelta(DeltaPage page) {
            sequence=page.sequence; base=page.base; time=page.time; createdAt=ticks;
            pages=new DeltaPage[page.pages];
        }
    }
    private static final class Runtime {
        View view;
        Vec3 position, velocity;
        long motionTime;
        int lastSample = -1;
        final Random recipeRandom = new Random(), jitterRandom = new Random();
        Runtime(View view, long time) {
            update(view, time);
            lastSample = expiredSamples.resume(view.key(), view.anchorUuid(), view.entityId(), view.epoch());
        }
        void update(View next, long time) {
            boolean sameSource=view!=null && view.epoch()==next.epoch()
                    && view.entityId()==next.entityId() && view.anchorUuid().equals(next.anchorUuid());
            if (view != null && view.epoch() != next.epoch()) lastSample = -1;
            view = next;
            // A newer motion packet may arrive between the pages of this metadata update.
            if(!sameSource || time>=motionTime) { position=next.position(); velocity=next.velocity(); motionTime=time; }
        }
    }
    private static final class CircleJob {
        final Circle circle;
        final Random random = new Random();
        int sample, particle;
        boolean initialized;
        CircleJob(Circle circle) { this.circle = circle; }
        long remaining() { return (long)(circle.samples() - sample) * Math.max(1, circle.count()) - particle; }
    }
    /** Bounded numeric checkpoints for a temporarily expired visual lease; never retains world/entity objects. */
    private static final class VisualSampleHistory<K> {
        private record Stamp(UUID anchor, int entityId, long epoch, int sample) {
            boolean matches(UUID nextAnchor, int nextEntityId, long nextEpoch) {
                return anchor.equals(nextAnchor) && entityId == nextEntityId && epoch == nextEpoch;
            }
        }
        private final int capacity;
        private final LinkedHashMap<K, Stamp> stamps = new LinkedHashMap<>();
        VisualSampleHistory(int capacity) {
            if (capacity < 1) throw new IllegalArgumentException("A visual history needs a positive capacity");
            this.capacity = capacity;
        }
        void retain(K key, UUID anchor, int entityId, long epoch, int sample) {
            if (sample < 0) return;
            Stamp previous = stamps.remove(key);
            if (previous != null && previous.matches(anchor, entityId, epoch)) sample = Math.max(sample, previous.sample);
            stamps.put(key, new Stamp(anchor, entityId, epoch, sample));
            if (stamps.size() > capacity) stamps.remove(stamps.keySet().iterator().next());
        }
        int resume(K key, UUID anchor, int entityId, long epoch) {
            Stamp previous = stamps.remove(key);
            return previous != null && previous.matches(anchor, entityId, epoch) ? previous.sample : -1;
        }
        void clear() { stamps.clear(); }
    }
    private ParametricVisualClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        AddonNetwork.registerClientReceiver(ParametricVisuals.MIST_FORM_CAPABILITY, buf -> { });
        AddonNetwork.registerClientReceiver(ParametricVisuals.STATE, buf -> {
            Minecraft client = Minecraft.getInstance();
            try {
                ResourceLocation dimension = buf.readResourceLocation(); long time = buf.readLong(); int seq = buf.readInt();
                int page = ParametricVisuals.bounded(buf.readVarInt(), 0, ParametricVisuals.MAX_PAGES - 1);
                int pages = ParametricVisuals.bounded(buf.readVarInt(), 1, ParametricVisuals.MAX_PAGES);
                if (page >= pages) throw new IllegalArgumentException("Invalid visual page");
                int count = ParametricVisuals.bounded(buf.readVarInt(), 0, ParametricVisuals.PAGE_SIZE);
                List<View> incoming = new ArrayList<>(count);
                for (int i = 0; i < count; i++) incoming.add(View.read(buf));
                StatePage decoded = new StatePage(dimension, time, seq, page, pages, incoming);
                if (!acceptWorld(client, dimension)) return;
                statePackets++;
                apply(decoded);
            } catch (RuntimeException invalid) { malformedPackets++; }
        });
        AddonNetwork.registerClientReceiver(ParametricVisuals.MOTION, buf -> {
            Minecraft client = Minecraft.getInstance();
            try {
                ResourceLocation dimension = buf.readResourceLocation(); long time = buf.readLong();
                int count = ParametricVisuals.bounded(buf.readVarInt(), 0, ParametricVisuals.PAGE_SIZE);
                List<Motion> incoming = new ArrayList<>(count);
                for (int i = 0; i < count; i++) incoming.add(new Motion(Key.read(buf), ParametricVisuals.vector(buf), ParametricVisuals.motion(buf)));
                if (!acceptWorld(client, dimension)) return;
                motionPackets++;
                for (Motion motion : incoming) {
                    Runtime runtime = scenes.get(motion.key);
                    if(runtime==null) {
                        TimedMotion previous=pendingMotions.get(motion.key);
                        if((previous==null && pendingMotions.size()<ParametricVisuals.MAX_SCENES)
                                || previous!=null && time>=previous.time)
                            pendingMotions.put(motion.key,new TimedMotion(time,ticks,motion));
                    } else applyMotion(runtime,motion,time);
                }
            } catch (RuntimeException invalid) { malformedPackets++; }
        });
        AddonNetwork.registerClientReceiver(ParametricVisuals.DELTA, buf -> {
            Minecraft client = Minecraft.getInstance();
            try {
                ResourceLocation dimension=buf.readResourceLocation(); long time=buf.readLong();
                int seq=buf.readInt(), base=buf.readInt();
                int page=ParametricVisuals.bounded(buf.readVarInt(),0,ParametricVisuals.MAX_PAGES-1);
                int pages=ParametricVisuals.bounded(buf.readVarInt(),1,ParametricVisuals.MAX_PAGES);
                if(page>=pages) throw new IllegalArgumentException("Invalid visual delta page");
                int changed=ParametricVisuals.bounded(buf.readVarInt(),0,ParametricVisuals.PAGE_SIZE);
                List<View> incoming=new ArrayList<>(changed);
                for(int i=0;i<changed;i++) incoming.add(View.read(buf));
                int removed=ParametricVisuals.bounded(buf.readVarInt(),0,ParametricVisuals.PAGE_SIZE);
                List<Key> removals=new ArrayList<>(removed);
                for(int i=0;i<removed;i++) removals.add(Key.read(buf));
                DeltaPage decoded=new DeltaPage(time,seq,base,page,pages,incoming,removals);
                if(!acceptWorld(client,dimension)) return;
                deltaPackets++; apply(decoded);
            } catch(RuntimeException invalid) {malformedPackets++;}
        });
        AddonNetwork.registerClientReceiver(ParametricVisuals.CIRCLE, buf -> {
            Minecraft client = Minecraft.getInstance();
            try {
                ResourceLocation dimension = buf.readResourceLocation(); Circle circle = Circle.read(buf);
                if (!acceptWorld(client, dimension) || client.player == null) return;
                circlePackets++;
                CircleJob job = new CircleJob(circle);
                if (!client.isPaused() && circles.isEmpty()) renderCircle(client, job);
                long left = job.remaining();
                if (left == 0) return;
                if (left > MAX_QUEUED_PARTICLES - queuedParticles || circles.size() >= MAX_QUEUED_CIRCLES) {
                    droppedParticles += left; return;
                }
                circles.add(job); queuedParticles += left; deferredParticles += left;
            } catch (RuntimeException invalid) { malformedPackets++; }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) budget = CIRCLE_BUDGET;
            else tick(Minecraft.getInstance());
        });
    }
    public static void setObserver(Observer value) { observer = value; }
    public static int activeSceneCount() { return scenes.size(); }
    public static long deltaPackets() { return deltaPackets; }
    public static Metrics metrics() {
        return new Metrics(scenes.size(), circles.size(), statePackets, motionPackets, circlePackets,
                sampledBatches, emittedParticles, deferredParticles, droppedParticles, malformedPackets);
    }
    public static void resetMetrics() {
        statePackets = deltaPackets = motionPackets = circlePackets = sampledBatches = emittedParticles = deferredParticles = droppedParticles = malformedPackets = 0;
    }
    private static boolean acceptWorld(Minecraft client, ResourceLocation dimension) {
        if (client.level == null || !client.level.dimension().location().equals(dimension)) return false;
        if (world != client.level) { clear(); world = client.level; }
        return true;
    }
    private static void clear() {
        scenes.clear(); circles.clear(); pendingMotions.clear(); queuedParticles = 0; world = null; pending = null; pendingDelta=null;
        expiredSamples.clear();
        hasSequence = false; ticks = receivedAt = 0; lastWorldTick = Long.MIN_VALUE;
        referenceServer = referenceLocal = 0; budget = CIRCLE_BUDGET;
    }
    private static void apply(StatePage page) {
        if (hasSequence && page.sequence != sequence && page.sequence - sequence <= 0) return;
        if (hasSequence && page.sequence == sequence) return;
        if (pending != null && page.sequence != pending.sequence && page.sequence - pending.sequence <= 0) return;
        if (pending == null || pending.sequence != page.sequence) pending = new Pending(page);
        if (pending.pages.length != page.pages || pending.time != page.time) { pending = null; malformedPackets++; return; }
        if (pending.pages[page.page] == null) { pending.pages[page.page] = page.views; pending.count++; }
        if (pending.count != pending.pages.length) return;
        Map<Key, Runtime> next = new LinkedHashMap<>();
        for (List<View> views : pending.pages) for (View view : views) {
            Runtime runtime = scenes.get(view.key());
            if (runtime == null) runtime = new Runtime(view, page.time); else runtime.update(view, page.time);
            applyPendingMotion(runtime);
            next.put(view.key(), runtime);
        }
        scenes.clear(); scenes.putAll(next);
        sequence = page.sequence; hasSequence = true; pending = null; pendingDelta=null;
        receivedAt = ticks; referenceServer = page.time; referenceLocal = world.getGameTime();
    }
    private static void apply(DeltaPage page) {
        // A delta is meaningful only against the exact complete baseline it was generated from.
        // Missing/incomplete baselines recover through the existing one-second full heartbeat.
        if(!hasSequence || page.base!=sequence || page.sequence-sequence<=0) return;
        if(pendingDelta!=null && page.sequence!=pendingDelta.sequence
                && page.sequence-pendingDelta.sequence<=0) return;
        if(pendingDelta==null || pendingDelta.sequence!=page.sequence) pendingDelta=new PendingDelta(page);
        if(pendingDelta.pages.length!=page.pages || pendingDelta.time!=page.time || pendingDelta.base!=page.base) {
            pendingDelta=null; malformedPackets++; return;
        }
        if(pendingDelta.pages[page.page]==null) {pendingDelta.pages[page.page]=page;pendingDelta.count++;}
        if(pendingDelta.count!=pendingDelta.pages.length) return;
        // No intermediate page changes a scene or its sampling phase.
        for(DeltaPage part:pendingDelta.pages) for(Key key:part.removed) {
            scenes.remove(key); pendingMotions.remove(key);
        }
        for(DeltaPage part:pendingDelta.pages) for(View view:part.views) {
            Runtime runtime=scenes.get(view.key());
            if(runtime==null) {
                if(scenes.size()>=ParametricVisuals.MAX_SCENES) {malformedPackets++;continue;}
                runtime=new Runtime(view,page.time); scenes.put(view.key(),runtime);
            } else runtime.update(view,page.time);
            applyPendingMotion(runtime);
        }
        sequence=page.sequence; pendingDelta=null;
        if(pending!=null && pending.sequence-sequence<=0) pending=null;
        receivedAt=ticks; referenceServer=page.time; referenceLocal=world.getGameTime();
    }
    private static void applyMotion(Runtime runtime, Motion motion, long time) {
        if(time<runtime.motionTime) return;
        runtime.position=motion.position; runtime.velocity=motion.velocity; runtime.motionTime=time;
    }
    private static void applyPendingMotion(Runtime runtime) {
        TimedMotion motion=pendingMotions.remove(runtime.view.key());
        if(motion!=null) applyMotion(runtime,motion.motion,motion.time);
    }
    private static void tick(Minecraft client) {
        if (client.level != world) { clear(); return; }
        if (world == null || client.player == null || client.isPaused()) return;
        long local = world.getGameTime();
        if (local == lastWorldTick) return;
        lastWorldTick = local; ticks++;
        if (pending != null && ticks - pending.createdAt > 40) pending = null;
        if(pendingDelta!=null && ticks-pendingDelta.createdAt>40) pendingDelta=null;
        pendingMotions.values().removeIf(motion -> ticks-motion.receivedAt>40);
        if (ticks - receivedAt > 60) {
            // Stop drawing an expired lease, but do not replay its already sampled ages after a
            // stalled server resumes with a clock behind the client's former projection.
            for (Runtime runtime : scenes.values()) {
                View view = runtime.view;
                expiredSamples.retain(view.key(), view.anchorUuid(), view.entityId(), view.epoch(), runtime.lastSample);
            }
            scenes.clear();hasSequence=false;pendingDelta=null;
        }
        long now = referenceServer + Math.max(0, local - referenceLocal);
        for (Runtime runtime : scenes.values()) {
            View view = runtime.view;
            long ageLong = now - view.epoch();
            if (ageLong < 0 || ageLong > Integer.MAX_VALUE) continue;
            int age = (int)ageLong;
            if (!SceneRecipes.active(view, age)) continue;
            int sample = SceneRecipes.latestSampleAge(view, age);
            if (sample < 0 || sample <= runtime.lastSample) continue;
            runtime.lastSample = sample;
            long sampleTick = view.epoch() + sample, seed = ParametricVisuals.seed(view, sampleTick);
            runtime.recipeRandom.setSeed(seed);
            Vec3 origin = origin(runtime, now);
            int[] batchIndex = { 0 };
            SceneRecipes.emit(view, sample, sampleTick, runtime.recipeRandom,
                    (particle,x,y,z,count,dx,dy,dz,speed,decoration,projectile) -> {
                        int index = batchIndex[0]++; sampledBatches++;
                        Observer listening = observer;
                        if (listening != null) listening.observe(view,sample,sampleTick,index,particle,x,y,z,count,dx,dy,dz,speed,decoration,projectile);
                        UUID owner = decoration ? view.decorationOwner() : null;
                        double px = origin.x+x, py = origin.y+y, pz = origin.z+z;
                        if (!visible(client,view.key().kind(),owner,px,py,pz)) return;
                        // Recipe and jitter never share a random stream. Local culling cannot move another point.
                        runtime.jitterRandom.setSeed(ParametricVisuals.mix(seed ^ ((long)index * 0xd1342543de82ef95L)));
                        Runnable emission = () -> cloud(particle,view.key().kind().force,px,py,pz,count,dx,dy,dz,speed,runtime.jitterRandom);
                        if (projectile) FirstPersonParticles.emitProjectile(owner,emission); else FirstPersonParticles.emit(owner,emission);
                    });
        }
        while (budget > 0 && !circles.isEmpty()) {
            CircleJob job = circles.peek(); long before = job.remaining();
            renderCircle(client,job); queuedParticles -= before - job.remaining();
            if (job.remaining() == 0) circles.remove(); else break;
        }
    }
    private static Vec3 origin(Runtime runtime, long now) {
        if (!runtime.view.follows()) return runtime.view.position();
        Entity source = world.getEntity(runtime.view.entityId());
        if (source != null && source.getUUID().equals(runtime.view.anchorUuid())) return source.position();
        return runtime.position.add(runtime.velocity.scale(Math.max(0, Math.min(5, now-runtime.motionTime))));
    }
    private static boolean visible(Minecraft client, Kind kind, UUID owner, double x,double y,double z) {
        if (kind == Kind.CORRUPT_MIST && client.player.getUUID().equals(owner)) return true;
        if (ParametricVisuals.exactRange(kind))
            return client.player.distanceToSqr(x,y,z) <= kind.range*kind.range;
        return client.player.blockPosition().closerToCenterThan(new Vec3(x,y,z),kind.range);
    }
    private static void cloud(ParticleOptions particle, boolean force, double x,double y,double z,int count,
                              double dx,double dy,double dz,double speed,Random random) {
        dx = (float)dx; dy = (float)dy; dz = (float)dz; speed = (float)speed;
        if (count == 0) { world.addParticle(particle,force,x,y,z,dx*speed,dy*speed,dz*speed); emittedParticles++; }
        else for (int i=0;i<count;i++) {
            world.addParticle(particle,force,x+random.nextGaussian()*dx,y+random.nextGaussian()*dy,z+random.nextGaussian()*dz,
                    random.nextGaussian()*speed,random.nextGaussian()*speed,random.nextGaussian()*speed); emittedParticles++;
        }
    }
    private static void renderCircle(Minecraft client, CircleJob job) {
        Circle c = job.circle;
        while (budget > 0 && job.sample < c.samples()) {
            double angle = Math.PI*2*job.sample/c.samples();
            double x = c.center().x+Math.cos(angle)*c.radius(), y=c.center().y, z=c.center().z+Math.sin(angle)*c.radius();
            if (!client.player.blockPosition().closerToCenterThan(new Vec3(x,y,z), c.force()?512:32)) {
                // Range rejection is also bounded work: a huge invisible ring cannot monopolize this tick.
                budget--; job.sample++; job.particle=0; job.initialized=false; continue;
            }
            if (!job.initialized) {
                job.random.setSeed(ParametricVisuals.mix(c.seed() ^ ((long)job.sample*0xd1342543de82ef95L)));
                job.initialized = true;
            }
            int amount = Math.min(budget,Math.max(1,c.count())-job.particle);
            Runnable emission = () -> cloud(c.particle(),c.force(),x,y,z,c.count()==0?0:amount,
                    c.spread().x,c.spread().y,c.spread().z,c.speed(),job.random);
            if (c.projectile()) FirstPersonParticles.emitProjectile(c.owner(),emission); else FirstPersonParticles.emit(c.owner(),emission);
            budget-=amount; job.particle+=amount;
            if (job.particle>=Math.max(1,c.count())) { job.sample++; job.particle=0; job.initialized=false; }
        }
    }
}
