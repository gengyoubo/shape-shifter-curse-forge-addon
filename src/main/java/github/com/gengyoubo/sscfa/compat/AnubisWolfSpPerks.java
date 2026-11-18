package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.ability.AnubisWolfSpSummonWolves;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netherjudge (Anubis wolf SP) summoning rules: passive wolves on hit / when hit, a perk-granted
 * extra slot, and the soul-recall skill.
 *
 * <p>Each perk rank is an addon power and counts while the player is in this form and holds it.
 * The Fabric version remapped SSC's wolf perk purchases to these powers; SSC Forge applies perk
 * powers per perk tree and no addon tree is registered for this form yet, so until then the
 * rank-0 values apply.</p>
 */
public final class AnubisWolfSpPerks {
    public static final ResourceLocation SUMMONING_1 = addon("perk_anubis_wolf_sp_summoning_1");
    public static final ResourceLocation SUMMONING_2 = addon("perk_anubis_wolf_sp_summoning_2");
    public static final ResourceLocation SOUL_EXCITATION = addon("perk_anubis_wolf_sp_soul_excitation");
    public static final ResourceLocation SOUL_RECALL = addon("perk_anubis_wolf_sp_soul_recall");

    /** Last passive summon per owner (game time), standing in for Fabric MinionRegister's cooldown. */
    private static final Map<UUID, Long> LAST_PASSIVE = new ConcurrentHashMap<>();

    private AnubisWolfSpPerks() {}

    public static boolean hasExtraCapacity(Player player) {
        return player != null && FormUtils.isAnubisWolfSP(player) && has(player, SOUL_EXCITATION);
    }

    public static int capacity(Player player, int base) {
        return base + (hasExtraCapacity(player) ? 1 : 0);
    }

    public record PassiveSummon(int count, int maximum, int cooldown) {}

    public static PassiveSummon passiveSummon(int rank, boolean crystal, boolean extraCapacity, boolean whenHit) {
        int accessory = crystal ? 1 : 0;
        int count = (!whenHit && rank >= 2 ? 3 : 2) + accessory;
        int maximum = (rank >= 2 ? 3 : 2) + accessory + (extraCapacity ? 1 : 0);
        return new PassiveSummon(count, maximum, rank > 0 ? 420 : 600);
    }

    /**
     * Bi-entity action {@code ssc_addon:anubis_wolf_sp_passive_summon}. Without {@code reverse} the
     * actor is the owner (on hit); with it the target is (when hit).
     */
    public static void summonPassive(LivingEntity actor, LivingEntity target, boolean reverse) {
        LivingEntity owner = reverse ? target : actor;
        LivingEntity nearby = reverse ? actor : target;
        if (!(owner instanceof ServerPlayer player) || nearby == null || !(nearby.level() instanceof ServerLevel world)
                || !FormUtils.isAnubisWolfSP(player)) return;
        int rank = has(player, SUMMONING_2) ? 2 : has(player, SUMMONING_1) ? 1 : 0;
        PassiveSummon spec = passiveSummon(rank, AnubisWolfSpSummonWolves.hasTrinketEquipped(player),
                hasExtraCapacity(player), reverse);
        long now = world.getGameTime();
        Long last = LAST_PASSIVE.get(player.getUUID());
        if (last != null && last <= now && now - last < spec.cooldown()) return;
        if (aliveWolves(player, world) >= spec.maximum()) return;
        boolean summoned = false;
        for (int i = 0; i < spec.count() && aliveWolves(player, world) < spec.maximum(); i++) {
            AnubisWolfMinionEntity wolf = MinionTracker.spawn(world,
                    AnubisWolfSpSummonWolves.summonPosition(world, player, nearby), player);
            if (wolf == null) continue;
            wolf.setMinionLevel(3);
            summoned = true;
        }
        if (summoned) {
            LAST_PASSIVE.put(player.getUUID(), now);
            world.playSound(null, player.blockPosition(), SoundEvents.WOLF_GROWL, player.getSoundSource(), 1.0F, 1.5F);
            world.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true,
                    player.getX(), player.getY() + 0.5, player.getZ(), 8, 0, 0, 0, 0);
        }
    }

    public static void recall(ServerPlayer player) {
        if (!FormUtils.isAnubisWolfSP(player) || !has(player, SOUL_RECALL) || AbilityUseGuard.blocked(player)) return;
        AnubisWolfSpSummonWolves.recallAll(player);
    }

    public static void clearPlayer(UUID uuid) {
        LAST_PASSIVE.remove(uuid);
    }

    /** Tracked wolves still alive in this world; drops stale entries as it counts. */
    private static int aliveWolves(ServerPlayer player, ServerLevel world) {
        var tracked = MinionTracker.get(player, MinionTracker.ANUBIS_WOLF);
        tracked.removeIf(id -> !(world.getEntity(id) instanceof AnubisWolfMinionEntity wolf) || !wolf.isAlive());
        return tracked.size();
    }

    private static boolean has(Player player, ResourceLocation id) {
        return ResourceBars.hasPowerId(player, id);
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
