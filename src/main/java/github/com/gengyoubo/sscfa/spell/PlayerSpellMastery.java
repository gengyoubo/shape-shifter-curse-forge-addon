package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.neomagic.NeoRuntime;
import github.com.gengyoubo.sscfa.neomagic.NeoState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家施法熟练度按 UUID 存于 {@link NeoState}；书本等级与升级经验独立保留，不设承载上限。
 * 经验以十分之一为单位存储，每 600 点经验提升 50 安全值。
 */
public final class PlayerSpellMastery {
    public static final int EXPERIENCE_PER_LEVEL_TEN = 6000;
    private static int clientExpTen;

    private PlayerSpellMastery() {}

    public static int clientExpTen() { return clientExpTen; }

    /** 客户端快照：由 HUD 同步写入，退出世界时清零。 */
    public static void acceptClientExperience(int experience) { clientExpTen = Math.max(0, experience); }

    public static int tier(NeoState.PlayerData data) { return tier(data.spellbookMasteryExpTen); }

    public static int tier(int experience) {
        experience = Math.max(0, experience);
        return experience / EXPERIENCE_PER_LEVEL_TEN;
    }

    /** 每级所需经验固定，与上游规则一致（等级参数仅保留接口形状）。 */
    public static int thresholdTen(int level) { return EXPERIENCE_PER_LEVEL_TEN; }

    public static int progressTen(int experience) {
        experience = Math.max(0, experience);
        return experience % EXPERIENCE_PER_LEVEL_TEN;
    }

    public static int bonus(NeoState.PlayerData data) { return bonus(data.spellbookMasteryExpTen); }

    public static int bonus(int experience) { return tier(experience) * 50; }

    /** 满级旧书的书内经验取最大值迁入玩家档案（不叠加复制品），随后清空书内经验，交易后不会再次转移。 */
    public static void migrate(ServerPlayer player, ItemStack book) {
        if (player == null || book == null || book.isEmpty()
                || SpellbookData.getLevel(book) < SpellbookData.MAX_LEVEL) return;
        int legacy = SpellbookData.getExpTen(book);
        if (legacy <= 0) return;
        var data = NeoRuntime.data(player);
        data.spellbookMasteryExpTen = Math.max(data.spellbookMasteryExpTen, legacy);
        SpellbookData.setExpTen(book, 0);
        NeoRuntime.dirty(player);
    }

    /** 成功施法总会给玩家累加熟练度；未满级书另外保留原有的装备升级进度。 */
    public static void grant(ServerPlayer player, ItemStack book, int amountTen) {
        if (player == null || amountTen <= 0) return;
        if (book != null && !book.isEmpty() && SpellbookData.getLevel(book) < SpellbookData.MAX_LEVEL) {
            SpellbookData.addExpTen(book, amountTen);
        }
        migrate(player, book);
        var data = NeoRuntime.data(player);
        data.spellbookMasteryExpTen = (int) Math.min(Integer.MAX_VALUE,
                (long) data.spellbookMasteryExpTen + SpellbookData.boostedExpTen(amountTen));
        NeoRuntime.dirty(player);
    }
}
