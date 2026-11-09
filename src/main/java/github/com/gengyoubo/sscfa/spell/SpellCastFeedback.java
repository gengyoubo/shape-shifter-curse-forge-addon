package github.com.gengyoubo.sscfa.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** Shared spell names and wording for local preflight and authoritative rejection. */
public final class SpellCastFeedback {
    private SpellCastFeedback() {}

    public static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.1f", Math.ceil(Math.max(0, ticks) / 2.0) / 10.0);
    }

    public static Component spellName(Spell spell, ItemStack scroll) {
        var name = Component.translatable(spell.getNameKey());
        return name;
    }

    public static Component noMana(ItemStack book, int cost) {
        if (book == null || book.isEmpty()) return Component.translatable("message.ssc_addon.spellbook.no_book");
        return manaFailure(SpellbookData.getMana(book), SpellbookData.getMaxMana(book), cost);
    }

    public static Component manaFailure(int available, int capacity, int cost) {
        if (cost < 0) return Component.translatable("message.ssc_addon.spellbook.cost_unavailable");
        if (cost > capacity) return Component.translatable("message.ssc_addon.spellbook.capacity_insufficient", cost, capacity, cost - capacity);
        return Component.translatable("message.ssc_addon.spellbook.no_mana_details", cost, available, Math.max(0, cost - available));
    }

    public static Component cooldown(long remaining) {
        return Component.translatable("message.ssc_addon.spellbook.cooldown", seconds(remaining));
    }

    public static Component stabilizing(long remaining) {
        return Component.translatable("message.ssc_addon.spellbook.swap_wait", seconds(remaining));
    }
}
