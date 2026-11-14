package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import github.com.gengyoubo.sscfa.spell.Spell;

/** Guards active, mod-owned teleports without intercepting safety evacuation. */
public final class AnomalyTeleportGuard {
    private AnomalyTeleportGuard() {}
    public static boolean reject(ServerPlayer player) {
        if (!AnomalyEffects.teleportBlocked(player)) return false;
        player.displayClientMessage(Component.translatable("message.ssc_addon.anomaly.teleport_blocked"), true);
        return true;
    }

    public static boolean isTeleportSpell(Spell spell) {
        if (spell == null || !spell.getId().getNamespace().equals("ssc_addon")) return false;
        return switch (spell.getId().getPath()) {
            case "space_blink", "space_recall", "pocket_space" -> true;
            default -> false;
        };
    }
}
