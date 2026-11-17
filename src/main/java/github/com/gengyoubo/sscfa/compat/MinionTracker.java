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
    /** Key of SSC Forge's owner registry in the player's persistent data (AnubisMinionService). */
    private static final String SSC_MINIONS_KEY = "SscAnubisWolfMinions";

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
        if (!world.addFreshEntity(wolf)) return null;
        registerWithSsc(owner, wolf);
        add(owner, ANUBIS_WOLF, wolf.getUUID());
        return wolf;
    }

    /**
     * Adds an already spawned wolf to SSC Forge's owner registry (the player's
     * {@code SscAnubisWolfMinions} list, which SSC's recall and minion-death hooks read) and applies
     * the owner's {@code wolf_minion_bonus} powers. {@link AnubisMinionService#summon} cannot be used
     * for this: it always spawns a new wolf of its own.
     */
    public static void registerWithSsc(Player owner, AnubisWolfMinionEntity wolf) {
        var data = owner.getPersistentData();
        var list = data.getList(SSC_MINIONS_KEY, net.minecraft.nbt.Tag.TAG_STRING);
        var entry = net.minecraft.nbt.StringTag.valueOf(wolf.getUUID().toString());
        if (!list.contains(entry)) list.add(entry);
        data.put(SSC_MINIONS_KEY, list);
        AnubisMinionService.applySpawnBonuses(owner, wolf);
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
