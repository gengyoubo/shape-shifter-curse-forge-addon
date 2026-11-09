package github.com.gengyoubo.sscfa.ability;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Active;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import github.com.gengyoubo.sscfa.action.SscAddonActions;
import github.com.gengyoubo.sscfa.cooldown.SkillCastManager;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Cancels live casting state before a queued release can execute. Repeated calls are harmless. */
public final class ControlledAbilityInterruption {
    private ControlledAbilityInterruption() { }

    public static void interrupt(ServerPlayer player) {
        if (!AbilityUseGuard.fullyControlled(player)) return;
        cancelTransient(player);
        // SSC charge/grapple/dive powers expose their normal cleanup through onRemoved().
        for (var power : PowerHolderComponent.KEY.get(player).getPowers()) {
            if (power instanceof Active && power.getClass().getName().startsWith(
                    "net.onixary.shapeShifterCurseFabric.additional_power.")) power.onRemoved();
        }
    }

    /** Cancel owned controls before reload/death, preserving autonomous released effects. */
    public static void cancelTransient(ServerPlayer player) {
        var manager = SkillCastManager.get(player.serverLevel());
        var casts = manager.activeCasts().stream().filter(c -> c.playerId.equals(player.getUUID())
                && !SkillCastManager.isAutonomousEffect(c)
                // This instant skill applies the control effect before completing its own cast.
                && !SscAddonActions.PLAY_DEAD_SKILL_ID.equals(c.skillId)).toList();
        SpellChannelManager.onFullyControlled(player);
        VortexChargeManager.cancel(player);
        SpiderMoonWeaverWebManager.cancel(player);
        SpiderMoonWeaverSwingManager.interruptControl(player);
        WaterSpearLeapManager.cancel(player);
        VortexGuideManager.cancel(player);
        JumpKillManager.interruptControl(player);
        VenomSkillManager.interruptControl(player);
        AnubisWolfSpSummonWolves.interruptControl(player);
        WindDashManager.clear(player);
        WindSpiritClawManager.clear(player);
        SnowFoxSpFrostStorm.cancelCharging(player);
        SnowFoxSpTeleportAttack.clearPlayer(player.getUUID());
        SnowFoxSpMeleeAbility.clearPlayer(player.getUUID());
        GoldenSandstormWitherSand.clearPlayer(player);
        NovaSkillManager.interruptControl(player);
        FluorescentLaserManager.interruptControl(player);
        FluorescentTidalManager.interruptControl(player);
        FrostSpikeManager.interruptControl(player);
        AllaySPGroupHeal.interruptControl(player);
        
        
        for (var cast : casts) {
            clearCastResources(player, cast.skillId);
            manager.interrupt(cast.castId, SkillCastManager.now(player));
        }

    }

    private static void clearCastResources(ServerPlayer player, String skill) {
        String root = skill.endsWith("_key_activation")
                ? skill.substring(0, skill.length() - "_key_activation".length()) : skill;
        for (String suffix : new String[]{"_charging_state", "_charge_timer", "_heal_execute", "_toggle_state"}) {
            ResourceLocation id = ResourceLocation.tryParse(root + suffix);
            if (id != null && PowerTypeRegistry.contains(id)
                    && PowerHolderComponent.KEY.get(player).hasPower(PowerTypeRegistry.get(id))
                    && PowerUtils.getResourceValue(player, id) != 0) PowerUtils.setResourceValueAndSync(player, id, 0);
        }
        if (root.startsWith("my_addon:form_familiar_fox_")
                && (root.endsWith("_blue_fire_ring") || root.endsWith("_blue_fire_ring_amulet"))) {
            player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
        }
    }
}
