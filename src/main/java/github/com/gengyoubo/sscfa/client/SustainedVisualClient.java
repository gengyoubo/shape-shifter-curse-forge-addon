package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.client.particle.FirstPersonParticles;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SustainedVisuals;
import github.com.gengyoubo.sscfa.network.VisualRecipe;
import github.com.gengyoubo.sscfa.network.VisualRecipe.Kind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** 持续视觉客户端：按同一配方在本地重放粒子。 */
public final class SustainedVisualClient {
    private static final DustParticleOptions GREEN =
            new DustParticleOptions(new Vector3f(0.30f, 0.85f, 0.30f), 1);
    private static final DustParticleOptions LETHAL =
            new DustParticleOptions(new Vector3f(0.72f, 0.12f, 0.12f), 0.9f);
    private static final DustParticleOptions EDGE =
            new DustParticleOptions(new Vector3f(0.68f, 0.68f, 0.70f), 0.8f);

    private static List<SustainedVisuals.View> views = List.of();
    private static ClientLevel world;
    private static long received;
    private static long lastTick = Long.MIN_VALUE;

    private SustainedVisualClient() {
    }

    public static void init() {
        AddonNetwork.registerClientReceiver(SustainedVisuals.STATE, buf -> {
            ResourceLocation dimension = buf.readResourceLocation();
            long serverTime = buf.readLong();
            int count = Math.max(0, Math.min(4096, buf.readVarInt()));
            List<SustainedVisuals.View> incoming = new ArrayList<>(count);
            for (int i = 0; i < count; i++) incoming.add(SustainedVisuals.View.read(buf));
            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            if (level == null || !level.dimension().location().equals(dimension)) return;
            if (world != level) clear();
            world = level;
            views = incoming;
            received = Math.max(serverTime, level.getGameTime());
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
    }

    private static void clear() {
        views = List.of();
        world = null;
        lastTick = Long.MIN_VALUE;
    }

    private static void tick(Minecraft client) {
        if (client.level != world) {
            clear();
            return;
        }
        if (world == null || client.isPaused() || views.isEmpty() || client.player == null) return;
        long now = world.getGameTime();
        if (now == lastTick) return;
        lastTick = now;
        if (now - received > 60) {
            views = List.of();
            return;
        }
        for (SustainedVisuals.View view : views) {
            int age = (int) Math.max(0, now - view.epoch());
            if (view.duration() > 0 && age > view.duration()) continue;
            Entity source = world.getEntity(view.entityId());
            if (source != null && !source.getUUID().equals(view.key().source())) source = null;
            Vec3 pos = source == null ? view.pos() : source.position();
            if (view.key().kind() == Kind.SEED_FIELD && source != null) {
                source.yRotO = (age - 1) * 5f;
                source.setYRot(age * 5f);
                if (source instanceof net.minecraft.world.entity.decoration.ArmorStand stand) {
                    stand.yBodyRotO = source.yRotO;
                    stand.yBodyRot = source.getYRot();
                    stand.yHeadRotO = source.yRotO;
                    stand.yHeadRot = source.getYRot();
                }
            }
            var random = new java.util.Random(
                    view.key().source().getLeastSignificantBits() ^ now ^ view.key().kind().ordinal());
            Vec3 base = pos;
            VisualRecipe.emit(view.key().kind(), age, now, view.duration(), view.width(), view.eyeHeight(),
                    view.radius(), view.outerRadius(), random, batch ->
                            FirstPersonParticles.emit(
                                    VisualRecipe.isDecoration(view.key().kind(), batch)
                                            ? view.decorationOwner() : null,
                                    () -> cloud(view, particle(batch.particle()),
                                            base.add(batch.x(), batch.y(), batch.z()), batch.count(),
                                            batch.dx(), batch.dy(), batch.dz(), batch.speed(), random)));
        }
    }

    private static ParticleOptions particle(VisualRecipe.Particle particle) {
        return switch (particle) {
            case SOUL_FIRE -> ParticleTypes.SOUL_FIRE_FLAME;
            case FLAME -> ParticleTypes.FLAME;
            case LAVA -> ParticleTypes.LAVA;
            case LARGE_SMOKE -> ParticleTypes.LARGE_SMOKE;
            case LETHAL_DUST -> LETHAL;
            case EDGE_DUST -> EDGE;
            case SNOWFLAKE -> ParticleTypes.SNOWFLAKE;
            case CLOUD -> ParticleTypes.CLOUD;
            case GREEN_DUST -> GREEN;
            case HAPPY_VILLAGER -> ParticleTypes.HAPPY_VILLAGER;
            case WARPED_SPORE -> ParticleTypes.WARPED_SPORE;
        };
    }

    private static void cloud(SustainedVisuals.View view, ParticleOptions particle, Vec3 position,
                              int count, double dx, double dy, double dz, double speed,
                              java.util.Random random) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        double range = view.key().kind().range;
        if (view.key().kind() == Kind.FROST_STORM) {
            if (player.distanceToSqr(position) > range * range) return;
        } else if (!player.blockPosition().closerToCenterThan(position, range)) {
            return;
        }
        float fx = (float) dx;
        float fy = (float) dy;
        float fz = (float) dz;
        float fs = (float) speed;
        if (count == 0) {
            world.addParticle(particle, view.key().kind().force, position.x, position.y, position.z,
                    fx * fs, fy * fs, fz * fs);
        } else {
            for (int i = 0; i < count; i++) {
                world.addParticle(particle, view.key().kind().force,
                        position.x + random.nextGaussian() * fx,
                        position.y + random.nextGaussian() * fy,
                        position.z + random.nextGaussian() * fz,
                        random.nextGaussian() * fs, random.nextGaussian() * fs, random.nextGaussian() * fs);
            }
        }
    }
}
