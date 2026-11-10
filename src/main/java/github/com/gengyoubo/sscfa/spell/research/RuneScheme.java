package github.com.gengyoubo.sscfa.spell.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import github.com.gengyoubo.sscfa.spell.Spell;

/** 缂備焦顨愰梽鍕几閸愵喖妫橀悷娆忓閺€鎶芥煥濞戞ɑ绠簅rge 闂佸憡顨堟慨宕囩礊閸涘瓨鏅鑸电〒缁愭鏌″蹇曠瘈闁绘稒鐟╅弻鍛潩瀹曞洨鐣洪梺闈涙閻掞箑螞閵堝绀夐柣妯哄暱閻忓洭鏌曢崱妤€鈧悂鎮鹃鍕瀬閻庨潧澹婇崕濠囨煛閸屾碍宸濋柣鏍缁岸宕滄笟鈧崗鍥╃磽娴ｅ搫鏋庣紒鍓佸枛閹秹寮崒妤佹櫃闁诲繒鍋涢崐璁崇昂濠碉紕鍋涚粔鎾箖濡ゅ啯鍋橀柕澶堝劜缁€鈧梺?*/
public final class RuneScheme {
    public static final String KEY = "RuneScheme";
    private RuneScheme() {}
    public static boolean isModified(ItemStack stack) { return false; }
    public static boolean validScroll(ServerPlayer player, ItemStack scroll, Spell spell) { return true; }
    public static net.minecraft.nbt.CompoundTag authoritativeScroll(github.com.gengyoubo.sscfa.spell.research.WorldRuneState world, net.minecraft.world.item.ItemStack scroll) { return null; }

    public static net.minecraft.nbt.CompoundTag create(WorldRuneState world, int level, int[] slots, Object result) { return new net.minecraft.nbt.CompoundTag(); }

    public static int imprint(net.minecraft.world.Container table, WorldRuneState world) { return 0; }

    public static int imprint(net.minecraft.world.Container table, WorldRuneState world, int schemeSlot, int scrollSlot) { return 0; }

    public static boolean hasEnhancements(net.minecraft.nbt.CompoundTag saved) { return false; }

    public static void normalizeUnmodified(Object world, net.minecraft.world.item.ItemStack scroll) {}

    public static RuneModifiers modifiers(Player player, ItemStack scroll, int selectedLevel) {
        return RuneModifiers.NONE;
    }
}
