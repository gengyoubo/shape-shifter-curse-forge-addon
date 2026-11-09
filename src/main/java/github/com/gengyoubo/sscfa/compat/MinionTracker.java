package github.com.gengyoubo.sscfa.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import net.onixary.shapeShifterCurseForge.power.AnubisMinionService;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 冥狼召唤物本地追踪：桥接 Fabric 的 IPlayerEntityMinion 与 Forge 实体。 */
public final class MinionTracker {
    public static final String ANUBIS_WOLF = "anubis_wolf";

    private static final Map<UUID, Map<String, Set<UUID>>> MINIONS = new ConcurrentHashMap<>();

    private MinionTracker() {}

    public static Set<UUID> get(Player player, String id) {
        return MINIONS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(id, ignored -> ConcurrentHashMap.newKeySet());
    }

    public static int count(Player player, String id) {
        return get(player, id).size();
    }

    public static void add(Player player, String id, UUID uuid) {
        get(player, id).add(uuid);
    }

    public static void remove(Player player, String id, UUID uuid) {
        get(player, id).remove(uuid);
    }

    public static void clear(Player player, String id) {
        get(player, id).clear();
    }

    public static AnubisWolfMinionEntity spawn(ServerLevel world, BlockPos pos, Player owner) {
        AnubisWolfMinionEntity wolf = ModEntities.ANUBIS_WOLF_MINION.get().create(world);
        if (wolf == null) return null;
        wolf.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, owner.getYRot(), 0.0f);
        wolf.setTame(true);
        wolf.setOwnerUUID(owner.getUUID());
        world.addFreshEntity(wolf);
        AnubisMinionService.summon(owner, wolf, new com.google.gson.JsonObject());
        add(owner, ANUBIS_WOLF, wolf.getUUID());
        return wolf;
    }

    public static BlockPos nearbyEmptySpace(ServerLevel world, RandomSource random, BlockPos center,
                                            int radius, int minDy, int maxDy, int attempts) {
        for (int i = 0; i < Math.max(1, attempts) * 8; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = minDy + random.nextInt(maxDy - minDy + 1);
            BlockPos candidate = center.offset(dx, dy, dz);
            if (world.isEmptyBlock(candidate) && world.isEmptyBlock(candidate.above())) return candidate;
        }
        return center.above();
    }
}
