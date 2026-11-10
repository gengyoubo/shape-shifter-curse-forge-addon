package github.com.gengyoubo.sscfa.spell;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 褰㈡€佹柦娉曞崗鍚岋紙Forge 绉绘鍗犱綅锛夛細淇濈暀娉曟湳涓庡疄浣撴墍闇€鐨勭ǔ瀹氭帴鍙ｏ紱
 * 褰㈡€佸姞閫?鍏遍福/鏂芥硶閽╁瓙鐨勫疄闄呯粨绠楀緟鑳藉姏灞傜Щ妞嶅悗琛ュ洖銆? */
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
    public static boolean trySeedSow(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack) { return false; }

    public static void register() {}
}
