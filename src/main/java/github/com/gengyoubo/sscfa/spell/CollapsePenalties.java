package github.com.gengyoubo.sscfa.spell;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 崩解惩罚（Forge 简化）：仅保留原版身体转向阈值修正；完整崩解阶段/扣血待 NeoMagic 运行时移植。
 */
public final class CollapsePenalties {
    private static final UUID SLOW = UUID.fromString("587f5108-6c90-464f-9226-937f876c195b");
    private static final double[] SLOWS = {0, .15, .30, .40, .60, .80};
    private CollapsePenalties() {}

    public static int stage(double collapse, double safety) {
        if (!(collapse > safety) || !(safety > 0) || !Double.isFinite(safety)) return 0;
        for (int i = 1; i < 5; i++) if (collapse <= safety * (1 + i * .2)) return i;
        return 5;
    }
    public static double slow(int stage) { return SLOWS[Math.max(0, Math.min(5, stage))]; }

    public static float bodyYawMovementThreshold(LivingEntity entity, float threshold) {
        if (!(entity instanceof Player)) return threshold;
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var modifier = speed == null ? null : speed.getModifier(SLOW);
        if (modifier == null || modifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) return threshold;
        double remaining = 1 + modifier.getAmount();
        return remaining > 0 && remaining < 1 ? (float) (threshold * remaining * remaining) : threshold;
    }
}
