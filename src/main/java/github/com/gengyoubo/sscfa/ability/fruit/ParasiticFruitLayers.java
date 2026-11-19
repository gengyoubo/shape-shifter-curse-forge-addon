package github.com.gengyoubo.sscfa.ability.fruit;

import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.power.ParasiticFruitSeedPower;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.GlowMarker;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/**
 * 世界范围共享的宿主果实层。每种果实对同一宿主每个间隔只结算一次（层数决定等级），
 * 友方支援不因多层而倍增治疗；满三层时宿主描边。施法者离线、换形态或离开世界时其层失效。
 */
public final class ParasiticFruitLayers {
    private static final class Host {
        final LivingEntity entity;
        final Map<String, Long> nextPulse = new HashMap<>();
        boolean enemy;

        Host(LivingEntity entity) { this.entity = entity; }
    }

    private static final class WorldState {
        final FruitLayerLedger ledger = new FruitLayerLedger();
        final Map<UUID, Host> hosts = new LinkedHashMap<>();
    }

    private record Group(ServerPlayer caster, FruitSelection fruit, int count, boolean friend) {}

    private static final Map<ServerLevel, WorldState> WORLDS = new IdentityHashMap<>();
    /** 不在玩家列表中的施法者（如其他模组或测试的假玩家）按种植时的实例解析。 */
    private static final Map<UUID, java.lang.ref.WeakReference<ServerPlayer>> DETACHED = new HashMap<>();
    private static boolean registered;

    private ParasiticFruitLayers() {}

    public static synchronized void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) tick(level);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
            for (var state : WORLDS.values()) for (var host : state.hosts.values()) cleanup(host.entity);
            WORLDS.clear();
            DETACHED.clear();
        });
    }

    /** 种下一层并返回宿主当前总层数。 */
    public static int plant(ServerPlayer caster, LivingEntity host, FruitSelection fruit, int life, int rooting, int maxHosts) {
        ServerLevel world = caster.serverLevel();
        var state = WORLDS.computeIfAbsent(world, ignored -> new WorldState());
        state.hosts.computeIfAbsent(host.getUUID(), ignored -> new Host(host));
        if (caster instanceof net.minecraftforge.common.util.FakePlayer) DETACHED.put(caster.getUUID(), new java.lang.ref.WeakReference<>(caster));
        Set<UUID> changed = state.ledger.add(host.getUUID(), caster.getUUID(), fruit, world.getGameTime(), life, rooting, maxHosts);
        for (UUID uuid : changed) reconcile(world, state, uuid, false);
        return state.ledger.layers(host.getUUID()).size();
    }

    public static void removeOwner(ServerPlayer caster) {
        for (var entry : WORLDS.entrySet()) {
            for (UUID uuid : entry.getValue().ledger.removeOwner(caster.getUUID())) {
                reconcile(entry.getKey(), entry.getValue(), uuid, false);
            }
        }
    }

    /** 宿主是否被敌方果实寄生（供「被削弱果寄生的敌人回血减半」被动查询）。 */
    public static boolean isEnemyParasitized(UUID host, long now) {
        for (var state : WORLDS.values()) {
            Host value = state.hosts.get(host);
            if (value != null && value.enemy && state.ledger.layers(host).stream().anyMatch(l -> l.expiresAt() > now)) return true;
        }
        return false;
    }

    public static List<FruitLayerLedger.Layer> layers(ServerLevel world, UUID host) {
        var state = WORLDS.get(world);
        return state == null ? List.of() : state.ledger.layers(host);
    }

    private static boolean validOwner(ServerLevel world, FruitLayerLedger.Layer layer) {
        var caster = caster(world, layer.owner());
        return caster != null && caster.isAlive() && caster.level() == world
                && FormUtils.isForm(caster, FormIdentifiers.BAT_PARASITIC_FRUIT)
                && ParasiticFruitSeedPower.active(caster) != null;
    }

    public static void tick(ServerLevel world) {
        var state = WORLDS.get(world);
        if (state == null) return;
        state.ledger.prune(world.getGameTime(), layer -> validOwner(world, layer));
        for (UUID uuid : List.copyOf(state.hosts.keySet())) reconcile(world, state, uuid, true);
        if (state.hosts.isEmpty()) WORLDS.remove(world);
    }

    private static void reconcile(ServerLevel world, WorldState state, UUID uuid, boolean pulse) {
        Host runtime = state.hosts.get(uuid);
        if (runtime == null) return;
        LivingEntity host = runtime.entity;
        var layers = state.ledger.layers(uuid);
        if (layers.isEmpty() || !host.isAlive() || host.isRemoved() || host.level() != world) {
            cleanup(host);
            state.ledger.removeHost(uuid);
            state.hosts.remove(uuid);
            return;
        }
        long now = world.getGameTime();
        Map<String, Group> groups = new LinkedHashMap<>();
        ServerPlayer visualOwner = null;
        runtime.enemy = false;
        for (var layer : layers) {
            var caster = caster(world, layer.owner());
            if (caster == null || caster.level() != world) continue;
            boolean friend = WhitelistUtils.isBuffTarget(caster, host);
            if ((!friend && WhitelistUtils.isProtected(caster, host)) || DomainManager.blocksTargeting(caster, host)) continue;
            if (visualOwner == null) visualOwner = caster;
            if (!friend) runtime.enemy = true;
            if (!layer.rooted(now)) continue;
            String key = friend ? "support" : layer.fruit().key();
            Group old = groups.get(key);
            groups.put(key, new Group(old == null ? caster : old.caster(), layer.fruit(),
                    old == null ? 1 : old.count() + 1, friend));
        }
        Map<String, Integer> active = new HashMap<>();
        groups.forEach((key, value) -> active.put(key, value.count()));
        if (visualOwner != null && layers.size() == FruitLayerLedger.MAX_LAYERS && !InfectionSporeManager.isInfected(uuid)) {
            active.put("outline", FruitLayerLedger.MAX_LAYERS);
        }
        FruitStatusOverlay.update(host, active);
        runtime.nextPulse.keySet().retainAll(groups.keySet());
        if (pulse) {
            for (var entry : groups.entrySet()) {
                String key = entry.getKey();
                Group group = entry.getValue();
                if (now < runtime.nextPulse.getOrDefault(key, Long.MIN_VALUE)) continue;
                if (ParasiticFruitSeedPower.active(group.caster()) == null) continue;
                runtime.nextPulse.put(key, now + ParasiticFruitSeedPower.fruitInterval());
                DomainManager.runAttachedEffect(group.caster(), host, () -> FruitStatusOverlay.pulse(host, key, group.count(),
                        () -> ParasiticFruitSeedPower.harvest(group.caster(), host, group.fruit(), group.count(), group.friend())));
            }
        }
        if (visualOwner != null) {
            ServerPlayer caster = visualOwner;
            if (ParasiticFruitSeedPower.active(caster) != null) {
                DomainManager.runAttachedEffect(caster, host, () -> ParasiticFruitSeedPower.drawAttached(caster, host, layers.size()));
            }
        } else {
            GlowMarker.unmark(host);
        }
    }

    private static ServerPlayer caster(ServerLevel world, UUID owner) {
        ServerPlayer online = world.getServer().getPlayerList().getPlayer(owner);
        if (online != null) return online;
        var reference = DETACHED.get(owner);
        ServerPlayer detached = reference == null ? null : reference.get();
        if (detached == null || detached.isRemoved()) {
            DETACHED.remove(owner);
            return null;
        }
        return detached;
    }

    private static void cleanup(LivingEntity host) {
        FruitStatusOverlay.clear(host);
        GlowMarker.unmark(host);
    }
}
