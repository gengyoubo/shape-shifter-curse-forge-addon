package github.com.gengyoubo.sscfa.spell.research;

import github.com.gengyoubo.sscfa.spell.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import static github.com.gengyoubo.sscfa.spell.research.RuneModifiers.Stat.*;

/** The server freezes effect comparisons; contextual cost estimates are sent only to the research screen. */
public final class RuneSummary {
    private RuneSummary() {}
    private static Component text(String key, Object... args) {
        return Component.translatable("research.ssc_addon.runes.summary." + key, args);
    }
    public static CompoundTag write(RuneBuildEvaluator.Result result) {
        CompoundTag n = new CompoundTag(); ListTag rows = new ListTag(), resonances = new ListTag();
        var base = result.baseModifiers(); var total = result.modifiers();
        var caps = result.recipe() == null ? EnumSet.noneOf(RuneModifiers.Stat.class) : RuneCapabilities.of(result.recipe().spell());
        if (caps.contains(DAMAGE)) addRow(rows,"damage_total",base.rawPower(true,false),total.rawPower(true,false),total.effectivePower(true,false));
        else if (caps.contains(HEAL)) addRow(rows,"heal_total",base.rawPower(false,true),total.rawPower(false,true),total.effectivePower(false,true));
        else if (caps.contains(POWER)) addRow(rows,"power",base.get(POWER),total.get(POWER),total.effective(POWER));
        boolean combinedMark = caps.contains(MARK) && caps.contains(NEGATIVE);
        if (combinedMark) addRow(rows,"mark_total",base.get(MARK)+base.get(NEGATIVE),total.get(MARK)+total.get(NEGATIVE),
                RuneModifiers.clamp(total.get(MARK)+total.get(NEGATIVE),-50,60));
        for (var stat : RuneModifiers.Stat.values()) {
            if (stat.ordinal() >= MANA.ordinal() || stat == POWER || stat == DAMAGE || stat == HEAL || !caps.contains(stat)
                    || combinedMark && (stat == MARK || stat == NEGATIVE)) continue;
            addRow(rows,stat.name().toLowerCase(Locale.ROOT),base.get(stat),total.get(stat),total.effective(stat));
        }
        n.put("Rows",rows); n.put("Modifiers",total.write());
        n.putInt("Stability",result.stability()); n.putInt("InteractionStability",result.interactionStability());
        for (var resonance : result.resonances()) {
            CompoundTag r = new CompoundTag(); r.putString("Rule",resonance.rule().id()); r.putBoolean("Active",resonance.active());
            r.putInt("Load",resonance.rule().load()); r.putInt("Protection",resonance.protection());
            r.putInt("Edges",resonance.edges()); r.put("Modifiers",resonance.rule().modifiers().write()); resonances.add(r);
        }
        n.put("Resonances",resonances); return n;
    }
    private static void addRow(ListTag rows, String key, int base, int total, int effective) {
        if (base == 0 && total == 0) return;
        CompoundTag row = new CompoundTag(); row.putString("Key",key); row.putInt("Base",base);
        row.putInt("Interaction",total-base); row.putInt("Total",total); row.putInt("Effective",effective); rows.add(row);
    }
    public static CompoundTag preview(RuneBuildEvaluator.Result result, int level, ServerPlayer player) {
        CompoundTag n = write(result);
        Spell spell = result.recipe() == null ? null : SpellRegistry.get(result.recipe().spell());
        if (spell == null) return n;
        ItemStack book = player == null ? ItemStack.EMPTY : SpellCastManager.getEquippedBook(player);
        if (book == null) book = ItemStack.EMPTY;
        int effectiveLevel = FormAffinity.bonusSpellLevel(player,spell.getElement(),level);
        int mana = player == null ? SpellNumbers.manaCost(spell.getConfig(),FormAffinity.manaCostLevel(null,spell.getElement(),level),
                FormationData.sumManaCostMultiplier(book,spell.getElement()),1) : SpellNumbers.finalManaCost(spell,book,player,level);
        int cooldown = SpellNumbers.finalCooldownTicks(spell,effectiveLevel,1,
                FormationData.sumCooldownMultiplier(book,spell.getElement()),FormAffinity.cooldownMultiplier(player,spell.getElement()));
        return withCosts(n,!book.isEmpty(),mana,spell.getCastingProfile(player,effectiveLevel,false).ticks(),cooldown,
                spell.getRarity(level).canUseSolo(),spell.getCastingProfile(player,level,true).ticks(),SpellNumbers.finalSoloCooldownTicks(spell,level));
    }
    public static CompoundTag withCosts(CompoundTag summary, boolean equipped, int mana, int time, int cooldown,
                                       boolean solo, int soloTime, int soloCooldown) {
        CompoundTag n = summary.copy(), costs = new CompoundTag();
        var modifiers = RuneModifiers.read(n.getCompound("Modifiers"));
        costs.putBoolean("Equipped",equipped); costs.putBoolean("Solo",solo);
        costs.putInt("BaseMana",mana); costs.putInt("Mana",mana<0?mana:modifiers.mana(mana));
        costs.putInt("BaseTime",time); costs.putInt("Time",modifiers.time(time));
        costs.putInt("BaseCd",cooldown); costs.putInt("Cd",modifiers.cooldown(cooldown));
        costs.putInt("BaseSoloTime",soloTime); costs.putInt("SoloTime",modifiers.time(soloTime));
        costs.putInt("BaseSoloCd",soloCooldown); costs.putInt("SoloCd",modifiers.cooldown(soloCooldown));
        n.put("Costs",costs); return n;
    }
    public static List<Component> effects(CompoundTag n, boolean detailed) {
        List<Component> lines = new ArrayList<>();
        if (detailed) lines.add(text("effects").copy().withStyle(ChatFormatting.GOLD));
        for (var entry : n.getList("Rows",Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)entry; Component name = text("attribute."+row.getString("Key"));
            lines.add(text(detailed?"row":"effective",name,signed(row.getInt("Effective"))).copy()
                    .withStyle(row.getInt("Effective")<0?ChatFormatting.RED:ChatFormatting.GREEN));
            if (detailed) lines.add(text("comparison",signed(row.getInt("Base")),signed(row.getInt("Interaction")),signed(row.getInt("Total"))).copy().withStyle(ChatFormatting.GRAY));
            if (row.getInt("Total") != row.getInt("Effective"))
                lines.add(text("capped",signed(row.getInt("Total")),signed(row.getInt("Effective")),Math.abs(row.getInt("Total")-row.getInt("Effective"))).copy().withStyle(ChatFormatting.YELLOW));
        }
        for (var entry : n.getList("Resonances",Tag.TAG_COMPOUND)) {
            var r = (CompoundTag)entry;
            lines.add(text(r.getBoolean("Active")?"active":"inactive",text("resonance."+r.getString("Rule"))).copy()
                    .withStyle(r.getBoolean("Active")?ChatFormatting.AQUA:ChatFormatting.YELLOW));
            if (detailed && r.getBoolean("Active")) {
                lines.add(text("load",r.getInt("Load")-r.getInt("Protection"),r.getInt("Protection")).copy().withStyle(ChatFormatting.GRAY));
                var changes = RuneModifiers.read(r.getCompound("Modifiers"));
                for (var stat : RuneModifiers.Stat.values()) if (changes.get(stat)!=0)
                    lines.add(RuneTooltips.stat(stat,changes.get(stat)).copy().withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }
    public static List<Component> lines(CompoundTag n) {
        List<Component> lines = effects(n,true);
        lines.add(text("stability",n.getInt("Stability"),signed(n.getInt("InteractionStability"))).copy().withStyle(ChatFormatting.GRAY));
        if (!n.contains("Costs",Tag.TAG_COMPOUND)) return lines;
        var c = n.getCompound("Costs");
        lines.add(text(c.getBoolean("Equipped")?"equipped":"unequipped").copy().withStyle(ChatFormatting.GOLD));
        if (c.getInt("Mana")>=0) lines.add(text("mana",c.getInt("BaseMana"),c.getInt("Mana"),signed(c.getInt("Mana")-c.getInt("BaseMana"))));
        else lines.add(text("mana_unavailable"));
        lines.add(text("time",c.getInt("BaseTime"),c.getInt("Time"),signed(c.getInt("Time")-c.getInt("BaseTime"))));
        lines.add(text("cooldown",c.getInt("BaseCd"),c.getInt("Cd"),signed(c.getInt("Cd")-c.getInt("BaseCd"))));
        if (c.getBoolean("Solo")) {
            lines.add(text("solo").copy().withStyle(ChatFormatting.GOLD));
            lines.add(text("time",c.getInt("BaseSoloTime"),c.getInt("SoloTime"),signed(c.getInt("SoloTime")-c.getInt("BaseSoloTime"))));
            lines.add(text("cooldown",c.getInt("BaseSoloCd"),c.getInt("SoloCd"),signed(c.getInt("SoloCd")-c.getInt("BaseSoloCd"))));
        }
        lines.add(text("context").copy().withStyle(ChatFormatting.DARK_GRAY)); return lines;
    }
    private static String signed(int value) { return value>0?"+"+value:Integer.toString(value); }
}
