package github.com.gengyoubo.sscfa.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

/** Reads potion stack limits from the Forge SSC power ids (e.g. {@code modify_potion_stack_8}). */
public final class PotionStackLimit {
    private static final String PREFIX = "modify_potion_stack_";

    private PotionStackLimit() {}

    public static int of(Player player) {
        if (player == null) return 1;
        int limit = 1;
        for (ResourceLocation id : FormPowerRegistry.idsFor(player)) {
            if (!"shape-shifter-curse".equals(id.getNamespace())) continue;
            String path = id.getPath();
            if (!path.startsWith(PREFIX)) continue;
            try {
                limit = Math.max(limit, Integer.parseInt(path.substring(PREFIX.length())));
            } catch (NumberFormatException ignored) {
            }
        }
        return limit;
    }
}
