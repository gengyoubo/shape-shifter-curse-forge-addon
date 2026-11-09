package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;

/** 鐠炲湱灏楅敍鍫ヮ棑閻忕绱氳ぐ銏♀偓浣稿礂閸氬苯宕版担宥冣偓?*/
public final class OcelotSpellLink {
    public static java.util.function.Predicate<net.minecraft.world.entity.player.Player> clientReleaseLocked = player -> false;
    private OcelotSpellLink() {}
    public static boolean wind(ServerPlayer player) { return false; }
    public static boolean windLocked(ServerPlayer player) { return false; }
    public static boolean rushing(ServerPlayer player) { return false; }
    public static boolean quickCast(ServerPlayer player) { return false; }
    public static double walkingMultiplier() { return 1.0; }
    public static void onNineLivesRevive(net.minecraft.server.level.ServerPlayer player) {}
    public static void register() {}
}
