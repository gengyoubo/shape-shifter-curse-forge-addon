package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.power.WebEntanglementService;

/**
 * Fabric SSC's {@code EntangledEffectUtils}: web binding accumulates in steps of
 * {@link #ENTANGLED_DURATION_PER_LEVEL}; reaching {@code (MAX_LEVEL + 1)} steps cocoons the target
 * (ENTANGLED_FULL). SSC Forge implements the same accumulation in {@link WebEntanglementService}.
 */
public final class EntangledEffectUtils {
    public static final int ENTANGLED_DURATION_PER_LEVEL = 20 * 5;
    public static final int ENTANGLED_MAX_LEVEL = 4;

    private EntangledEffectUtils() {}

    public static void applyEntangledEffect(Entity owner, LivingEntity target, int ticks) {
        if (target != null) WebEntanglementService.apply(owner, target, Math.max(1, ticks));
    }
}
