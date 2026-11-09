package github.com.gengyoubo.sscfa.spell;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 形态施法协同（Forge 移植占位）：保留法术与实体所需的稳定接口；
 * 形态加速/共鸣/施法钩子的实际结算待能力层移植后补回。
 */
public final class FormCastingStyle {
    private FormCastingStyle() {}

    public static boolean hasHarmfulEffect(LivingEntity target) { return false; }

    public static void onSpellHit(ServerPlayer caster, LivingEntity target, FormationElement element,
                                  UUID refundCastId, boolean hadHarmfulEffect) {}
    public static void onSpellHit(ServerPlayer caster, LivingEntity target, FormationElement element,
                                  UUID refundCastId) {}

    public static float lunarMendBonus(ServerPlayer caster, float power) { return power; }
    public static int applyTidalDiscount(Player player, FormationElement element, int cost) { return cost; }

    public static boolean isSwapStabilizing(ServerPlayer player) { return false; }
    public static long getSwapReadyAt(ServerPlayer player) { return 0L; }
    public static long getNaturalRegenAt(ServerPlayer player) { return 0L; }
    public static int naturalRegenPerSecond(net.minecraft.world.item.ItemStack book) { return 1; }
    public static UUID openRefundBudget(ServerPlayer player, int manaCost) { return null; }
    public static void onSpellCast(ServerPlayer player, FormationElement element, int manaCost) {}
    public static void register() {}
}
