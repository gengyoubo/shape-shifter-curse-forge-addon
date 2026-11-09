package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.ability.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Lifecycle cleanup for transient skill data. Unexpired quotas and persistent resources are retained. */
public final class SkillStateMaintenance {
    private SkillStateMaintenance() {}

    public static void init() {
        PlayerSessionCleanup.register((handler, server) -> {
            BatDesmodusBloodThirst.clearPlayer(handler.player.getUUID());
            ParasiticCombatTracker.clearPlayer(handler.player.getUUID());
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onLivingDeath((entity, source) -> {
            MancianimaAggroTracker.forget(entity.getUUID());
            if (entity instanceof ServerPlayer player) {
                ControlledAbilityInterruption.cancelTransient(player);
                AnubisWolfSpDeathDomain.clearPlayer(player);
            }
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityUnload((entity, world) ->
                MancianimaAggroTracker.forget(entity.getUUID()));
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityLoad((entity, world) -> {
            if (entity instanceof LivingEntity living) {
                AnubisWolfSpDeathDomain.removeLegacyHealthReduction(living);
                CompanionResonanceManager.removeLegacyModifier(living);
            }
            if ((entity.getTags().contains("ssca_ghost_creeper")
                    || entity.getTags().contains("ssca_ghost_cat"))
                    && !NightmareSpookManager.ownsGhost(entity.getUUID())) entity.discard();
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
            if (server.getTickCount() % 20 != 0 || server.overworld() == null) return;
            long now = server.overworld().getGameTime();
            SeedEnergyEatingHandler.pruneAll(now);
            AllaySPRangedHitPassive.prune(now);
            AnubisWolfSpSoulEnergy.pruneSources(now);
            GoldenSandstormRegen.pruneSources(now);
            NightmareFearManager.pruneImmunity(now);
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> {
            SeedEnergyEatingHandler.clearAll();
            ParasiticCombatTracker.clearAll();
            AllaySPRangedHitPassive.clearAll();
            AnubisWolfSpSoulEnergy.clearSources();
            NightmareFearManager.clearAll();
            MancianimaAggroTracker.clearAll();
            WindSpiritWindPressureManager.clearAll();
            CompanionResonanceManager.clearAll();
            PlayDeadAbsorptionManager.clearAll();
        });
    }
}
