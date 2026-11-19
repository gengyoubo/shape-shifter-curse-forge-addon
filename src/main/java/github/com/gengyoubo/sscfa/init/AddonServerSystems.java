package github.com.gengyoubo.sscfa.init;

/**
 * Server-side ability managers and damage hooks that the Fabric entry point
 * ({@code SscAddon.onInitialize}) started and the Forge constructor did not.
 *
 * <p>Each of these only adds Forge event listeners (through {@code SscEvents} or
 * {@code PlayerSessionCleanup}). They run from common setup rather than the mod
 * constructor because several of the classes read {@code RegistryObject}s in their
 * static initialisers. Classes annotated {@code @Mod.EventBusSubscriber} are
 * registered by Forge itself and are not listed here.</p>
 */
public final class AddonServerSystems {
    private static boolean started;

    private AddonServerSystems() {}

    public static synchronized void init() {
        if (started) return;
        started = true;

        // Custom JSON action / condition types used by the form powers, and the per-tick drivers of
        // the skills they start.
        github.com.gengyoubo.sscfa.action.SscAddonActions.register();
        github.com.gengyoubo.sscfa.condition.SscAddonConditions.register();
        github.com.gengyoubo.sscfa.event.SscAddonServerEvents.registerAll();
        github.com.gengyoubo.sscfa.event.SscAddonPlayerEvents.register();
        github.com.gengyoubo.sscfa.event.SscAddonInteractionEvents.register();
        // /ssc_addon admin and debug commands.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) ->
                github.com.gengyoubo.sscfa.command.SscAddonCommands.register(event.getDispatcher()));
        github.com.gengyoubo.sscfa.spell.FormCastingStyle.init();
        github.com.gengyoubo.sscfa.loot.AccessoryLoot.register();
        github.com.gengyoubo.sscfa.loot.EvolutionItemsLoot.register();
        github.com.gengyoubo.sscfa.loot.MagicScrollLoot.register();
        github.com.gengyoubo.sscfa.loot.FormationLoot.register();
        github.com.gengyoubo.sscfa.loot.StoryBookLoot.init();

        // Fabric order: login restore, landing surge, form passives.
        github.com.gengyoubo.sscfa.event.LoginResourceRestoreHandler.register();
        github.com.gengyoubo.sscfa.ability.WindSpiritLandingSurgeManager.register();
        github.com.gengyoubo.sscfa.ability.AnubisWolfSpSoulEnergy.registerEvents();
        github.com.gengyoubo.sscfa.ability.GoldenSandstormRegen.init();
        github.com.gengyoubo.sscfa.ability.MancianimaMarkManager.register();
        github.com.gengyoubo.sscfa.ability.KillEmpowerManager.register();

        // ALLOW_DAMAGE vetoes (dodge, invulnerability windows, leap immunity).
        github.com.gengyoubo.sscfa.event.FluorescentDodgeHandler.register();
        github.com.gengyoubo.sscfa.event.SscaDamageVetoHandler.register();
        github.com.gengyoubo.sscfa.event.StorySleepTimeGuardHandler.register();

        // Item use and per-tick skill managers.
        github.com.gengyoubo.sscfa.ability.AllaySPTotem.init();
        github.com.gengyoubo.sscfa.ability.AllaySPPortableBeacon.init();
        github.com.gengyoubo.sscfa.ability.BeepSheepFormManager.init();
        github.com.gengyoubo.sscfa.ability.InfectionSporeManager.init();
        github.com.gengyoubo.sscfa.ability.ParasiticSeedFieldManager.init();
        github.com.gengyoubo.sscfa.ability.ParasiticCombatTracker.init();
        github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionManager.init();
        github.com.gengyoubo.sscfa.ability.ParasiticSeedEnergyRegen.init();
        github.com.gengyoubo.sscfa.ability.NineLivesManager.init();
        github.com.gengyoubo.sscfa.ability.NovaSkillManager.init();
        github.com.gengyoubo.sscfa.ability.CorruptMistManager.init();
        github.com.gengyoubo.sscfa.spell.ExplosionManager.init();
        github.com.gengyoubo.sscfa.spell.UniversalFormationManager.init();
        github.com.gengyoubo.sscfa.ability.CompanionResonanceManager.init();
        github.com.gengyoubo.sscfa.ability.SeedEnergyEatingHandler.register();
    }
}
