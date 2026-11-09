package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;

/** 豹猫（风灵）形态协同占位。 */
public final class OcelotSpellLink {
    private OcelotSpellLink() {}
    public static boolean wind(ServerPlayer player) { return false; }
    public static boolean windLocked(ServerPlayer player) { return false; }
    public static boolean rushing(ServerPlayer player) { return false; }
    public static boolean quickCast(ServerPlayer player) { return false; }
    public static double walkingMultiplier() { return 1.0; }
    public static void register() {}
}
