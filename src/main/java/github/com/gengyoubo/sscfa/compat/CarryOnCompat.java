package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** Optional client bridge, also usable with SSC builds predating its Carry On integration. */
public final class CarryOnCompat {
    private CarryOnCompat() {}

    public static boolean isInCarryingAnimation(Player player) {
        // Replaced by the client mixin only when Carry On is installed.
        return false;
    }
}
