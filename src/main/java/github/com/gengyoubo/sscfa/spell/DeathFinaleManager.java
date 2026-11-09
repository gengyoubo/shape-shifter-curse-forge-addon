package github.com.gengyoubo.sscfa.spell;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** 死亡终章蓄力管理器占位：正式结算待能力层/领域系统完整移植后补回。 */
public final class DeathFinaleManager {
    private DeathFinaleManager() {}
    public static void begin(ServerPlayer caster, int ticks) {}
    public static void advance(ServerPlayer caster, int ticks) {}
    public static boolean canContinue(ServerPlayer caster) { return true; }
    public static void cancel(ServerPlayer caster) {}
    public static void release(ServerPlayer caster, float power, UUID refundCastId, int expBounty) {}
}
