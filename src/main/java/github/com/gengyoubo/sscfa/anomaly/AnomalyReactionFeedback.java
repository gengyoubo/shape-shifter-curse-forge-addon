package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 异常反应反馈（Forge 占位）：粒子/音效/提示待客户端层移植后接入。 */
public final class AnomalyReactionFeedback {
    private AnomalyReactionFeedback() {}
    public static void showBase(String element, LivingEntity target) {}
    public static void show(String first, String second, LivingEntity target, SpellAnomalyContext context) {}
    public static void fireZonePulse(LivingEntity anchor, Vec3 center) {}
    public static void clear(ServerLevel world) {}
    public static void clearAll() {}
}
