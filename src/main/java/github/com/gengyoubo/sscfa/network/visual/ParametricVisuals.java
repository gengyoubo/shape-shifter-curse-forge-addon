package github.com.gengyoubo.sscfa.network.visual;

import java.util.*;
import java.util.function.Predicate;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.DecorationParticleScope;
import github.com.gengyoubo.sscfa.network.DecorationParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Server-authorized cosmetic scenes. No damage, resources or collision are delegated to clients. */
public final class ParametricVisuals {
    public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "visual_scenes_v1");
    public static final ResourceLocation DELTA = new ResourceLocation("ssc_addon", "visual_delta_v1");
    public static final ResourceLocation MOTION = new ResourceLocation("ssc_addon", "visual_motion_v1");
    public static final ResourceLocation CIRCLE = new ResourceLocation("ssc_addon", "visual_circle_v1");
    /** Receiver registration is the capability; no periodic capability packet is sent. */
    public static final ResourceLocation MIST_FORM_CAPABILITY = new ResourceLocation("ssc_addon", "mist_form_visuals_v1");
    public static final int PAGE_SIZE = 128, MAX_PAGES = 128, MAX_PARAMETERS = 16;
    public static final int MAX_SCENES = PAGE_SIZE * MAX_PAGES;
    public enum Kind {
        CORRUPT_MIST(32, false), METEOR_WARNING(32, false), DOMAIN_EDGE(512, true),
        DOMAIN_AMBIENT(512, true), BLUE_FIRE_RING(512, true), SSC_WEB_TRAIL(32, false),
        FIRE_BOLT_TRAIL(512, true), LUNAR_BOLT_TRAIL(32, false),
        MIST_FORM_AMBIENT(512, true), MIST_FORM_CHARGE(512, true);
        public final double range;
        public final boolean force;
        Kind(double range, boolean force) { this.range = range; this.force = force; }
    }
    public record Key(UUID id, Kind kind) {
        public void write(FriendlyByteBuf b) { b.writeUUID(id); b.writeEnum(kind); }
        public static Key read(FriendlyByteBuf b) { return new Key(b.readUUID(), b.readEnum(Kind.class)); }
    }
    public record View(Key key, UUID anchorUuid, int entityId, UUID decorationOwner, Vec3 position,
                       Vec3 velocity, long epoch, int duration, boolean follows, float width,
                       float eyeHeight, double extent, double[] parameters) {
        public View { parameters = parameters.clone(); }
        @Override public double[] parameters() { return parameters.clone(); }
        public double parameter(int index) { return parameters[index]; }
        public double parameter(int index, double fallback) { return index < parameters.length ? parameters[index] : fallback; }
        public void write(FriendlyByteBuf b) {
            key.write(b); b.writeUUID(anchorUuid); b.writeVarInt(entityId);
            b.writeBoolean(decorationOwner != null); if (decorationOwner != null) b.writeUUID(decorationOwner);
            vector(b, position); motion(b, velocity); b.writeLong(epoch); b.writeVarInt(duration);
            b.writeBoolean(follows); b.writeFloat(width); b.writeFloat(eyeHeight); b.writeDouble(extent);
            b.writeVarInt(parameters.length); for (double v : parameters) b.writeDouble(v);
        }
        public static View read(FriendlyByteBuf b) {
            Key key = Key.read(b); UUID anchor = b.readUUID(); int id = b.readVarInt();
            UUID owner = b.readBoolean() ? b.readUUID() : null;
            Vec3 pos = vector(b), velocity = motion(b); long epoch = b.readLong();
            int duration = bounded(b.readVarInt(), 0, 1_000_000);
            boolean follows = b.readBoolean(); float width = b.readFloat(), eye = b.readFloat();
            double extent = finite(b.readDouble());
            if (!Float.isFinite(width) || !Float.isFinite(eye) || width < 0 || eye < 0 || extent < 0 || extent > 65536)
                throw new IllegalArgumentException("Invalid visual dimensions");
            int n = bounded(b.readVarInt(), 0, MAX_PARAMETERS); double[] p = new double[n];
            for (int i = 0; i < n; i++) p[i] = finite(b.readDouble());
            validateParameters(key.kind, p);
            return new View(key, anchor, id, owner, pos, velocity, epoch, duration, follows, width, eye, extent, p);
        }
    }
    public record Circle(long seed, UUID owner, ParticleOptions particle, boolean force, boolean projectile,
                         Vec3 center, int count, double speed, Vec3 spread, double radius, int samples) {
        public void write(FriendlyByteBuf b) {
            b.writeLong(seed); b.writeBoolean(owner != null); if (owner != null) b.writeUUID(owner);
            b.writeVarInt(BuiltInRegistries.PARTICLE_TYPE.getId(particle.getType())); particle.writeToNetwork(b);
            b.writeBoolean(force); b.writeBoolean(projectile); vector(b, center);
            b.writeVarInt(count); b.writeFloat((float) speed); motion(b, spread); b.writeDouble(radius); b.writeVarInt(samples);
        }
        public static Circle read(FriendlyByteBuf b) {
            long seed = b.readLong(); UUID owner = b.readBoolean() ? b.readUUID() : null;
            int raw = bounded(b.readVarInt(), 0, BuiltInRegistries.PARTICLE_TYPE.size() - 1);
            ParticleOptions particle = readParticle(BuiltInRegistries.PARTICLE_TYPE.byId(raw), b);
            boolean force = b.readBoolean(), projectile = b.readBoolean(); Vec3 center = vector(b);
            int count = bounded(b.readVarInt(), 0, 65536); double speed = finite(b.readFloat());
            Vec3 spread = motion(b); double radius = finite(b.readDouble());
            if (Math.abs(radius) > 65536) throw new IllegalArgumentException("Invalid visual circle radius");
            int samples = bounded(b.readVarInt(), 1, 65536);
            return new Circle(seed, owner, particle, force, projectile, center, count, speed, spread, radius, samples);
        }
    }
    private static final class Entry {
        Entity anchor; ServerLevel world; View view; int touched;
        final Random random = new Random();
    }
    private record Motion(Vec3 position, Vec3 velocity) {}
    private record Audience(ServerPlayer player, ServerLevel world, Object connection,
                            int lastFullTick, int sequence, List<View> views, Map<Key, Motion> motion) {}
    private static final Map<Key, Entry> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, Audience> AUDIENCES = new HashMap<>();
    private static boolean initialized;
    private static int sequence;
    private static long circleSequence, statePackets, deltaPackets, motionPackets, circlePackets, legacyPackets;
    public record Metrics(int scenes, int audiences, long statePackets, long deltaPackets,
                          long motionPackets, long circlePackets, long legacyPackets) {}
    private ParametricVisuals() {}

    public static void init() {
        if (initialized) return; initialized = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) flush(event.getServer());
        });
        PlayerSessionCleanup.register((handler, server) -> {
            Audience audience = AUDIENCES.get(handler.player.getUUID());
            if (audience != null && audience.connection == handler) AUDIENCES.remove(handler.player.getUUID());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            ACTIVE.clear(); AUDIENCES.clear(); sequence = 0;
        });
    }
    public static Metrics metrics() { return new Metrics(ACTIVE.size(), AUDIENCES.size(), statePackets, deltaPackets, motionPackets, circlePackets, legacyPackets); }
    public static void resetMetrics() { statePackets = deltaPackets = motionPackets = circlePackets = legacyPackets = 0; }
    public static void touch(Entity anchor, UUID eventId, Kind kind, UUID decorationOwner, Vec3 origin,
                             boolean follows, long epoch, int duration, double extent, double... parameters) {
        if (!(anchor.level() instanceof ServerLevel world)) return;
        if (parameters.length > MAX_PARAMETERS || duration < 0) throw new IllegalArgumentException("Invalid visual scene");
        Key key = new Key(eventId, kind); Entry e = ACTIVE.computeIfAbsent(key, ignored -> new Entry());
        int tick = world.getServer().getTickCount();
        Vec3 velocity = Vec3.ZERO;
        if (follows) {
            if (e.view != null && e.world == world && tick > e.touched && tick - e.touched <= 5) {
                Vec3 delta = origin.subtract(e.view.position);
                if (delta.lengthSqr() <= 64) velocity = delta.scale(1.0 / (tick - e.touched));
            } else velocity = anchor.getDeltaMovement();
        }
        e.anchor = anchor; e.world = world; e.touched = tick;
        e.view = new View(key, anchor.getUUID(), anchor.getId(), decorationOwner, origin, velocity, epoch,
                duration, follows, anchor.getBbWidth(), anchor.getEyeHeight(anchor.getPose()), extent, parameters);
    }
    public static void stop(UUID eventId, Kind kind) { ACTIVE.remove(new Key(eventId, kind)); }

    private static void flush(MinecraftServer server) {
        int tick = server.getTickCount();
        ACTIVE.values().removeIf(e -> e.touched < tick || e.anchor.isRemoved() || e.anchor.level() != e.world);
        if (ACTIVE.isEmpty() && AUDIENCES.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean modern = AddonNetwork.canSend(player, STATE) && AddonNetwork.canSend(player, CIRCLE);
            boolean mistForm = modern && AddonNetwork.canSend(player, MIST_FORM_CAPABILITY);
            List<View> visible = new ArrayList<>();
            for (Entry e : ACTIVE.values()) {
                View v = e.view; double range = v.key.kind.range + v.extent + 4;
                if (e.world != player.serverLevel() || (!player.getUUID().equals(v.decorationOwner)
                        && player.distanceToSqr(v.position) > range * range)) continue;
                // The protocol is bounded. Extreme cosmetic overload must not abort a server tick.
                if (modern && supportsKind(v.key.kind, mistForm)) { if (visible.size() < MAX_SCENES) visible.add(v); }
                else legacy(e, player);
            }
            if (!modern) { AUDIENCES.remove(player.getUUID()); continue; }
            Audience previous = AUDIENCES.get(player.getUUID());
            boolean sameSession = previous != null && previous.player == player && previous.world == player.serverLevel()
                    && previous.connection == player.connection;
            if (visible.isEmpty() && (previous == null || previous.views.isEmpty())) continue;
            boolean full = !sameSession || tick - previous.lastFullTick < 0 || tick - previous.lastFullTick >= 20;
            boolean changed = !sameSession || !sameState(previous.views, visible);
            if (full || changed && !AddonNetwork.canSend(player, DELTA)) {
                int sentSequence = sendSnapshot(player, visible);
                if (visible.isEmpty()) AUDIENCES.remove(player.getUUID());
                else {
                    Map<Key, Motion> motion = new HashMap<>();
                    for (View v : visible) motion.put(v.key, new Motion(v.position, v.velocity));
                    AUDIENCES.put(player.getUUID(), new Audience(player, player.serverLevel(), player.connection,
                            tick, sentSequence, List.copyOf(visible), motion));
                }
                continue;
            }
            if (changed) {
                Delta delta = diff(previous.views, visible);
                int sentSequence = delta.isEmpty() ? previous.sequence : sendDelta(player, delta, previous.sequence);
                // Retain existing motion history. Churn in one projectile must not allocate a new
                // history for every persistent scene or postpone the periodic full repair.
                for (Key removed : delta.removed) previous.motion.remove(removed);
                for (View v : delta.changed) previous.motion.put(v.key, new Motion(v.position, v.velocity));
                if (visible.isEmpty()) { AUDIENCES.remove(player.getUUID()); continue; }
                previous = new Audience(player, player.serverLevel(), player.connection,
                        previous.lastFullTick, sentSequence, List.copyOf(visible), previous.motion);
                AUDIENCES.put(player.getUUID(), previous);
            }
            // Untracked persistent origins still need movement while unrelated projectiles churn.
            if (tick % 5 == 0 && AddonNetwork.canSend(player, MOTION)) {
                List<View> moving = new ArrayList<>();
                for (View v : visible) {
                    Entry e = ACTIVE.get(v.key);
                    if (v.follows && e.anchor != player && !tracked(player, e.anchor)) {
                        Motion sent = previous.motion.get(v.key);
                        if (sent == null || !sent.position.equals(v.position) || !sent.velocity.equals(v.velocity)) {
                            moving.add(v);
                            previous.motion.put(v.key, new Motion(v.position, v.velocity));
                        }
                    }
                }
                if (!moving.isEmpty()) sendMotion(player, moving);
            }
        }
    }
    private static boolean tracked(ServerPlayer viewer, Entity anchor) {
        return anchor.level() == viewer.serverLevel() && viewer.distanceToSqr(anchor) <= 128.0 * 128.0;
    }
    private static int sendSnapshot(ServerPlayer player, List<View> views) {
        int size = Math.min(views.size(), MAX_SCENES);
        int pages = Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE), seq = ++sequence;
        for (int page = 0; page < pages; page++) {
            FriendlyByteBuf b = AddonNetwork.buffer(); b.writeResourceLocation(player.serverLevel().dimension().location());
            b.writeLong(player.serverLevel().getGameTime()); b.writeInt(seq); b.writeVarInt(page); b.writeVarInt(pages);
            int from = page * PAGE_SIZE, to = Math.min(from + PAGE_SIZE, size);
            b.writeVarInt(to - from); for (int i = from; i < to; i++) views.get(i).write(b);
            AddonNetwork.sendToPlayer(player, STATE, b); statePackets++;
        }
        return seq;
    }
    private static int sendDelta(ServerPlayer player, Delta delta, int base) {
        int pages = Math.max(1, Math.max((delta.changed.size() + PAGE_SIZE - 1) / PAGE_SIZE,
                (delta.removed.size() + PAGE_SIZE - 1) / PAGE_SIZE)), seq = ++sequence;
        for (int page = 0; page < pages; page++) {
            FriendlyByteBuf b = AddonNetwork.buffer(); b.writeResourceLocation(player.serverLevel().dimension().location());
            b.writeLong(player.serverLevel().getGameTime()); b.writeInt(seq); b.writeInt(base);
            b.writeVarInt(page); b.writeVarInt(pages);
            int from = Math.min(page * PAGE_SIZE, delta.changed.size());
            int to = Math.min(from + PAGE_SIZE, delta.changed.size());
            b.writeVarInt(to - from); for (int i = from; i < to; i++) delta.changed.get(i).write(b);
            from = Math.min(page * PAGE_SIZE, delta.removed.size());
            to = Math.min(from + PAGE_SIZE, delta.removed.size());
            b.writeVarInt(to - from); for (int i = from; i < to; i++) delta.removed.get(i).write(b);
            AddonNetwork.sendToPlayer(player, DELTA, b); deltaPackets++;
        }
        return seq;
    }
    private static void sendMotion(ServerPlayer player, List<View> views) {
        for (int from = 0; from < views.size(); from += PAGE_SIZE) {
            FriendlyByteBuf b = AddonNetwork.buffer(); b.writeResourceLocation(player.serverLevel().dimension().location());
            b.writeLong(player.serverLevel().getGameTime()); int to = Math.min(from + PAGE_SIZE, views.size());
            b.writeVarInt(to - from);
            for (int i = from; i < to; i++) { View v = views.get(i); v.key.write(b); vector(b, v.position); motion(b, v.velocity); }
            AddonNetwork.sendToPlayer(player, MOTION, b); motionPackets++;
        }
    }
    public static boolean sameState(List<View> a, List<View> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!sameView(a.get(i), b.get(i))) return false;
        }
        return true;
    }
    public static boolean sameView(View x, View y) {
        return x.key.equals(y.key) && x.anchorUuid.equals(y.anchorUuid) && x.entityId == y.entityId
                && Objects.equals(x.decorationOwner, y.decorationOwner) && x.epoch == y.epoch
                && x.duration == y.duration && x.follows == y.follows && x.width == y.width
                && x.eyeHeight == y.eyeHeight && x.extent == y.extent && Arrays.equals(x.parameters, y.parameters)
                && (x.follows || x.position.equals(y.position));
    }
    public record Delta(List<View> changed, List<Key> removed) {
        public Delta { changed = List.copyOf(changed); removed = List.copyOf(removed); }
        public boolean isEmpty() { return changed.isEmpty() && removed.isEmpty(); }
    }
    /** Identity-based metadata diff; motion uses its own bounded, low-frequency path. */
    public static Delta diff(List<View> before, List<View> after) {
        Map<Key, View> remaining = new LinkedHashMap<>();
        for (View v : before) remaining.put(v.key, v);
        List<View> changed = new ArrayList<>();
        for (View v : after) {
            View old = remaining.remove(v.key);
            if (old == null || !sameView(old, v)) changed.add(v);
        }
        return new Delta(changed, new ArrayList<>(remaining.keySet()));
    }
    private static void legacy(Entry e, ServerPlayer viewer) {
        View v = e.view; long now = e.world.getGameTime(); int age = (int) Math.max(0, now - v.epoch);
        if (!SceneRecipes.active(v, age)) return;
        e.random.setSeed(seed(v, now));
        SceneRecipes.emit(v, age, now, e.random, (effect,x,y,z,count,dx,dy,dz,speed,decoration,projectile) -> {
            Vec3 p = v.position.add(x,y,z);
            if (!visible(viewer, p, v.key.kind, decoration ? v.decorationOwner : null)) return;
            var packet = new ClientboundLevelParticlesPacket(effect, v.key.kind.force, p.x,p.y,p.z,
                    (float)dx,(float)dy,(float)dz,(float)speed,count);
            DecorationParticles.sendVisualFallback(e.world, viewer, packet, decoration ? v.decorationOwner : null, projectile);
            legacyPackets++;
        });
    }
    public static boolean visible(ServerPlayer player, Vec3 pos, Kind kind, UUID owner) {
        if (kind == Kind.CORRUPT_MIST && player.getUUID().equals(owner)) return true;
        return exactRange(kind)
                ? player.distanceToSqr(pos) <= kind.range * kind.range
                : player.blockPosition().closerToCenterThan(pos, kind.range);
    }
    public static boolean supportsKind(Kind kind, boolean mistFormCapability) {
        return mistFormCapability || kind != Kind.MIST_FORM_AMBIENT && kind != Kind.MIST_FORM_CHARGE;
    }
    public static boolean exactRange(Kind kind) {
        return kind == Kind.FIRE_BOLT_TRAIL || kind == Kind.DOMAIN_EDGE || kind == Kind.DOMAIN_AMBIENT
                || kind == Kind.MIST_FORM_AMBIENT || kind == Kind.MIST_FORM_CHARGE;
    }
    public static long seed(View v, long sampleTick) {
        return mix(v.key.id.getMostSignificantBits() ^ Long.rotateLeft(v.key.id.getLeastSignificantBits(), 17)
                ^ v.epoch ^ (sampleTick * 0x9e3779b97f4a7c15L) ^ v.key.kind.ordinal());
    }
    public static long mix(long value) {
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }
    public static void circle(Entity actor, ParticleOptions particle, boolean force, int count, double speed,
                              Vec3 spread, Vec3 offset, double radius, int samples,
                              Predicate<ServerPlayer> audience, UUID decorationOwner) {
        if (!(actor.level() instanceof ServerLevel world)) return;
        boolean projectile = DecorationParticleScope.isProjectileScope();
        if (DecorationParticleScope.isProtected()) decorationOwner = null;
        Circle circle = new Circle(mix(actor.getUUID().getLeastSignificantBits() ^ world.getGameTime() ^ ++circleSequence),
                decorationOwner, particle, force, projectile, actor.position().add(offset), count, speed, spread, radius, samples);
        double range = (force ? 512 : 32) + Math.abs(radius) + 4;
        for (ServerPlayer viewer : world.players()) {
            if (viewer.distanceToSqr(circle.center) > range * range || audience != null && !audience.test(viewer)) continue;
            if (AddonNetwork.canSend(viewer, CIRCLE)) {
                FriendlyByteBuf b = AddonNetwork.buffer(); b.writeResourceLocation(world.dimension().location()); circle.write(b);
                AddonNetwork.sendToPlayer(viewer, CIRCLE, b); circlePackets++;
            } else {
                for (int i = 0; i < samples; i++) {
                    double angle = Math.PI * 2 * i / samples;
                    Vec3 p = circle.center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                    if (!viewer.blockPosition().closerToCenterThan(p, force ? 512 : 32)) continue;
                    var packet = new ClientboundLevelParticlesPacket(particle, force, p.x,p.y,p.z,(float)spread.x,(float)spread.y,
                            (float)spread.z,(float)speed,count);
                    DecorationParticles.sendVisualFallback(world, viewer, packet, decorationOwner, projectile); legacyPackets++;
                }
            }
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ParticleOptions readParticle(ParticleType<?> type, FriendlyByteBuf b) {
        return ((ParticleOptions.Deserializer) type.getDeserializer()).fromNetwork((ParticleType) type, b);
    }
    public static void vector(FriendlyByteBuf b, Vec3 p) { b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); }
    public static Vec3 vector(FriendlyByteBuf b) { return new Vec3(finite(b.readDouble()),finite(b.readDouble()),finite(b.readDouble())); }
    public static void motion(FriendlyByteBuf b, Vec3 p) { b.writeFloat((float)p.x); b.writeFloat((float)p.y); b.writeFloat((float)p.z); }
    public static Vec3 motion(FriendlyByteBuf b) { return new Vec3(finite(b.readFloat()),finite(b.readFloat()),finite(b.readFloat())); }
    public static int bounded(int n, int min, int max) { if(n < min || n > max)throw new IllegalArgumentException("Invalid scene count");return n; }
    public static double finite(double n) { if(!Double.isFinite(n))throw new IllegalArgumentException("Invalid visual number");return n; }
    private static void validateParameters(Kind kind, double[] p) {
        int minimum = switch (kind) {
            case CORRUPT_MIST, METEOR_WARNING, BLUE_FIRE_RING -> 2;
            case DOMAIN_EDGE -> 3;
            case DOMAIN_AMBIENT -> 4;
            case MIST_FORM_AMBIENT -> 2;
            case MIST_FORM_CHARGE -> 4;
            case SSC_WEB_TRAIL -> 1;
            default -> 0;
        };
        if (p.length < minimum) throw new IllegalArgumentException("Incomplete visual recipe: " + kind);
        // Decoding a finite radius must not authorize an unbounded loop on the client tick.
        if (kind == Kind.CORRUPT_MIST && Math.max(10, p[0] * p[0] * 4) > 65536
                || kind == Kind.METEOR_WARNING && Math.max(12, p[0] * 10) > 65536)
            throw new IllegalArgumentException("Visual recipe exceeds sample capacity: " + kind);
        if (kind == Kind.MIST_FORM_AMBIENT || kind == Kind.MIST_FORM_CHARGE) {
            int heightIndex = kind == Kind.MIST_FORM_AMBIENT ? 0 : 1;
            double height = p[heightIndex], flags = p[heightIndex + 1];
            if (height < 0 || height > 65532 || flags != Math.rint(flags) || flags < 0 || flags > 3)
                throw new IllegalArgumentException("Invalid mist form dimensions or scope");
            if (kind == Kind.MIST_FORM_CHARGE && (p[0] < 1 || p[0] > 1_000_000
                    || p[0] != Math.rint(p[0]) || p[3] < 0 || p[3] > p[0] || p[3] != Math.rint(p[3])))
                throw new IllegalArgumentException("Invalid mist form charge timeline");
        }
    }
}
