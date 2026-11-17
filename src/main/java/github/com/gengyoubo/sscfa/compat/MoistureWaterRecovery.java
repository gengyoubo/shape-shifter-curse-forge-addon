package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerEvents;

/**
 * SSC's moisture drain treats only fully submerged players as "in water".
 * A player standing or crawling in shallow water is touching water but not
 * submerged, so SSC keeps applying the land drain and the moisture bar sticks
 * at zero / one bubble. Restore the pre-tick air plus normal water recovery
 * when any water contact is present.
 */
public final class MoistureWaterRecovery {
    private static final int WATER_RECOVERY = 4;
    // The integrated server and local client share a UUID but are different entities.
    // Per-entity storage also avoids boxed values and map-node allocation every tick.
    private static volatile long epoch;

    private MoistureWaterRecovery() { }

    public static void capture(Player player) {
        if (player != null) ((MoistureTickSnapshot) player).ssca$moistureBefore(epoch, player.getAirSupply());
    }

    /** 显式支付湿润度，同时校正浅水恢复的tick起点，避免恢复逻辑退回这笔消耗。 */
    public static boolean consume(Player player,int amount) {
        if(player==null||player.level().isClientSide||amount<0||player.getAirSupply()<amount)return false;
        if(amount==0)return true;
        player.setAirSupply(player.getAirSupply()-amount);
        var snapshot = (MoistureTickSnapshot) player;
        int before = snapshot.ssca$moistureBefore(epoch);
        if (before != Integer.MIN_VALUE) snapshot.ssca$moistureBefore(epoch, Math.max(0, before - amount));
        return true;
    }

    public static void restore(Player player) {
        if (player == null) return;
        var snapshot = (MoistureTickSnapshot) player;
        int before = snapshot.ssca$moistureBefore(epoch);
        snapshot.ssca$moistureBefore(epoch, Integer.MIN_VALUE);
        if (before == Integer.MIN_VALUE || player.getAirSupply() >= player.getMaxAirSupply()) return;
        if (!player.isInWater() && !player.isInWaterOrBubble()) return;
        // Fully submerged players already take SSC's water branch.
        if (player.isUnderWater()) return;
        if (!hasActiveWaterBreathing(player)) return;
        int target = Math.min(player.getMaxAirSupply(), before + WATER_RECOVERY);
        if (target > player.getAirSupply()) player.setAirSupply(target);
    }

    public static void clear() {
        epoch++;
    }

    private static boolean hasActiveWaterBreathing(Player player) {
        // Active shape-shifter-curse:custom_water_breathing power whose condition holds.
        return FormPowerEvents.hasCustomWaterBreathing(player);
    }
}
