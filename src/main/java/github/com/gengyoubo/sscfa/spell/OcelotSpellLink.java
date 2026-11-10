package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;

/** 璞圭尗锛堥鐏碉級褰㈡€佸崗鍚屽崰浣?*/
public final class OcelotSpellLink {
    public static java.util.function.Predicate<net.minecraft.world.entity.player.Player> clientReleaseLocked = player -> false;
    private OcelotSpellLink() {}
    public static boolean wind(ServerPlayer player) { return false; }
    public static boolean windLocked(net.minecraft.world.entity.player.Player player) { return false; }
    public static boolean rushing(ServerPlayer player) { return false; }
    public static boolean quickCast(ServerPlayer player) { return false; }
    public static double walkingMultiplier() { return 1.0; }
    public static void onNineLivesRevive(net.minecraft.server.level.ServerPlayer player) {}
    public static void register() {}
}
