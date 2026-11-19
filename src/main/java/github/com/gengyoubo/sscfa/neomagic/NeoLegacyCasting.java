package github.com.gengyoubo.sscfa.neomagic;

import github.com.gengyoubo.sscfa.spell.SpellChannelManager;

import github.com.gengyoubo.sscfa.ability.BeepSheepFormManager;
import github.com.gengyoubo.sscfa.spell.FormAffinity;
import github.com.gengyoubo.sscfa.spell.FormCastingStyle;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.FrostSpellLink;
import github.com.gengyoubo.sscfa.spell.FormSpellStyle;
import github.com.gengyoubo.sscfa.spell.PlayerSpellMastery;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellCastManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Blueprint;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Carrier;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Control;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Core;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Profile;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Quote;

/**
 * 旧魔法与新魔法运行时的桥接。旧法术仍然复用原有的施法通道、冷却与数值，
 * 这里只负责把它们折算成新运行时的形态档案、蓝图报价和协同倍率。
 */
public final class NeoLegacyCasting {
    private static final ThreadLocal<Outcome> OUTCOME = ThreadLocal.withInitial(Outcome::new);

    private NeoLegacyCasting() {}

    private static final class Outcome {
        boolean reversed, shifted, wrongTarget, missed;
    }

    /** 旧版施法会话视图；本 Forge 移植没有独立的旧研究会话，因此按需返回 null。 */
    public static final class LegacyActive {
        public final NeoSession session;
        public final NeoState.Experiment record;
        public final int duration;
        public int age;

        public LegacyActive(NeoSession session, NeoState.Experiment record, int duration, int age) {
            this.session = session;
            this.record = record;
            this.duration = duration;
            this.age = age;
        }
    }

    public static boolean reversed() { return OUTCOME.get().reversed; }
    public static boolean shifted() { return OUTCOME.get().shifted; }
    public static boolean wrongTarget() { return OUTCOME.get().wrongTarget; }

    /** 命中/施放失败标记只消费一次，避免影响下一次无关施法。 */
    public static boolean missed() {
        Outcome outcome = OUTCOME.get();
        boolean missed = outcome.missed;
        outcome.missed = false;
        return missed;
    }

    public static void miss() { OUTCOME.get().missed = true; }

    public static int actualCost(int cost) { return Math.max(0, cost); }

    public static float power(float damage) { return Math.max(0, damage); }

    public static int summonCount(int base) { return Math.max(0, Math.min(6, base)); }

    public static int formationSafetyBonus(int level) {
        return (int) Math.round(FormationData.universalManaBonusPct(level) * 100);
    }

    public static double safetyBonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int best = FormationData.getBestUniversalVariantLevel(stack, FormationData.VARIANT_MANA);
        return best > 0 ? formationSafetyBonus(best) : 0;
    }

    public static double recoveryMultiplier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1;
        return FormationData.universalRecoveryMultiplier(stack);
    }

    public static Profile profile(ServerPlayer player) {
        return profile(player, SpellCastManager.getEquippedBook(player));
    }

    /** 身体档案（临时羊形态继承原身体容量）+ 玩家熟练度安全值 + 增能法阵安全值。 */
    public static Profile profile(ServerPlayer player, ItemStack book) {
        Profile base = NeoRuntime.bodyProfile(player);
        PlayerSpellMastery.migrate(player, book);
        double bonus = PlayerSpellMastery.bonus(NeoRuntime.data(player)) + safetyBonus(book);
        return bonus <= 0 ? base
                : new Profile(base.safety() + bonus, base.failure(), base.offset(),
                base.merge(), base.split(), base.reference());
    }

    public static Blueprint descriptor(Spell spell, int level, int preparation) {
        Core core = core(spell.getElement());
        Carrier carrier = carrier(spell, level);
        Core conversion = switch (core) {
            case THERMAL -> Core.FROST;
            case FROST -> Core.THERMAL;
            case LUNAR -> Core.VOID;
            case CURSE -> Core.LUNAR;
            case SUMMON -> Core.VOID;
            case VOID -> Core.LUNAR;
            case SPACE -> Core.LUNAR;
        };
        double range = Math.max(1, spell.getAimMaxRange(level));
        return new Blueprint(spell.getId().getPath(), core, carrier, List.<Control>of(), conversion,
                1, Math.max(0, preparation), range);
    }

    public static Quote quote(ServerPlayer player, Spell spell, int level, int preparation, int normal) {
        Quote base = blueprintQuote(player, descriptor(spell, level, preparation));
        double actual = normal;
        if (base.normal() > 0) actual = normal * (base.actual() / base.normal());
        return new Quote(normal, Math.max(0, actual), base.floor(), base.plan());
    }

    public static Quote blueprintQuote(ServerPlayer player, Blueprint blueprint) {
        return blueprintQuote(player, blueprint, SpellCastManager.getEquippedBook(player));
    }

    /**
     * 服务端结算与 HUD 预览共用的报价：书内法阵按输入核心折算费用，潮汐与形态崩解倍率随后叠乘。
     * 无书时只保留形态崩解倍率。
     */
    public static Quote blueprintQuote(ServerPlayer player, Blueprint blueprint, ItemStack book) {
        Quote quote = NeoRules.current().quote(blueprint, NeoRuntime.data(player).mastery(blueprint));
        boolean hasBook = book != null && !book.isEmpty();
        double multiplier = blueprintCostMultiplier(player, blueprint.core(), book)
                * (hasBook ? FormCastingStyle.tidalCostMultiplier(player) : 1)
                * FormSpellStyle.collapseMultiplier(player, blueprint.carrier() == Carrier.PROJECTILE,
                blueprint.output() == Core.FROST,
                blueprint.output() == Core.FROST || blueprint.output() == Core.CURSE);
        return new Quote(quote.normal() * multiplier, quote.actual() * multiplier, quote.floor(), quote.plan());
    }

    public static double blueprintCostMultiplier(ServerPlayer player, Core core) {
        return blueprintCostMultiplier(player, core, SpellCastManager.getEquippedBook(player));
    }

    private static double blueprintCostMultiplier(ServerPlayer player, Core core, ItemStack book) {
        return book == null || book.isEmpty() ? 1
                : FormationData.sumManaCostMultiplier(book, element(core)) * FormAffinity.manaCostMultiplier(player);
    }

    /** 冷却按输入核心的法阵、输出核心的形态亲和折算；最低为基础冷却的 20%（且不少于 10 tick）。 */
    public static int blueprintCooldown(ServerPlayer player, Blueprint blueprint) {
        int base = NeoRules.current().cooldown(blueprint);
        ItemStack book = SpellCastManager.getEquippedBook(player);
        if (book == null || book.isEmpty()) return base;
        return Math.max(Math.max(10, (int) Math.ceil(base * .2)),
                Math.round(base * FormationData.sumCooldownMultiplier(book, element(blueprint.core()))
                        * FormAffinity.cooldownMultiplier(player, element(blueprint.output()))));
    }

    /** 威力跟随最终输出核心：书内伤害法阵 × 形态亲和；无书为 1。 */
    public static double blueprintPower(ServerPlayer player, Blueprint blueprint) {
        return blueprintPower(player, blueprint.output());
    }

    public static double blueprintPower(ServerPlayer player, Core core) {
        ItemStack book = SpellCastManager.getEquippedBook(player);
        if (book == null || book.isEmpty()) return 1;
        return FormationData.sumDamageMultiplier(book, element(core)) * FormAffinity.damageMultiplier(player, element(core));
    }

    /** 空间法阵射程倍率；形态弹体射程由 {@link FormSpellStyle#projectileRange} 在运行时另行叠乘。 */
    public static double blueprintRangeMultiplier(ServerPlayer player, Blueprint blueprint) {
        ItemStack book = SpellCastManager.getEquippedBook(player);
        return book == null || book.isEmpty() ? 1
                : Math.max(0.1, FormationData.sumSpaceRangeMultiplier(book, element(blueprint.output())));
    }

    /**
     * 经验法阵倍率（只取书内最高等级）。全局获取倍率由 {@link PlayerSpellMastery#grant} 统一施加，
     * 此处不得再乘一次。
     */
    public static double blueprintExperienceMultiplier(ServerPlayer player) {
        ItemStack book = SpellCastManager.getEquippedBook(player);
        int level = book == null || book.isEmpty() ? 0
                : FormationData.getBestUniversalVariantLevel(book, FormationData.VARIANT_EXP);
        return level == 0 ? 1 : FormationData.universalExpMultiplier(level);
    }

    public static FormationElement element(Core core) {
        return switch (core) {
            case THERMAL -> FormationElement.FIRE;
            case FROST -> FormationElement.ICE;
            case LUNAR -> FormationElement.LUNAR;
            case CURSE -> FormationElement.CURSE;
            case SUMMON -> FormationElement.SUMMON;
            case VOID -> FormationElement.VOID;
            case SPACE -> FormationElement.SPACE;
        };
    }

    public static LegacyActive active(ServerPlayer player) { return null; }

    /** Any cast in progress: an old-style spell channel or a blueprint cast. */
    public static boolean isCasting(ServerPlayer player) {
        return SpellChannelManager.isCasting(player) || active(player) != null
                || NeoRuntime.ACTIVE.containsKey(player.getUUID());
    }

    /**
     * Form refunds ("restore mana") lower the caster's collapse burden; returns the burden actually
     * removed, rounded up.
     */
    public static int restore(ServerPlayer player, double amount) {
        double before = NeoRuntime.data(player).burden;
        if (amount > 0 && before > 0) NeoRuntime.relieve(player, amount);
        return (int) Math.ceil(before - NeoRuntime.data(player).burden);
    }

    public static void beginBlueprintEffect(ServerPlayer player, NeoRuntime.Active active) {
        if (active.record.spell == null || active.record.spell.isEmpty()) active.record.spell = "blueprint";
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.15f);
    }

    public static void blueprintReleased(ServerPlayer player, NeoRuntime.Active active) {
        active.legacyReleased = true;
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.0f);
    }

    public static void blueprintHit(ServerPlayer player, NeoRuntime.Active active, LivingEntity target,
                                    boolean conditionBeforeHit) {
        if (target == null || target == player) return;
        UUID castId = parse(active.record.id);
        FormCastingStyle.onSpellHit(player, target, element(active.session.blueprint.output()), castId, conditionBeforeHit);
        if (active.session.blueprint.output() == Core.FROST) FrostSpellLink.onNativeHit(player, target);
    }

    public static void snapshotBlueprintAffinity(ServerPlayer player, NeoRuntime.Active active) {
        float curse = FormAffinity.curseDurationTicks(player, 100) / 100f;
        active.curseDurationMultiplier = curse > 0 ? curse : 1;
        if (active.record.form == null) active.record.form = NeoRuntime.form(player);
        if (active.record.form.equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.SHEEP_FORM.toString())) {
            String original = BeepSheepFormManager.originalFormId(player);
            if (original != null) active.record.form = original;
        }
    }

    private static Core core(FormationElement element) {
        if (element == null) return Core.THERMAL;
        return switch (element) {
            case FIRE -> Core.THERMAL;
            case ICE -> Core.FROST;
            case LUNAR -> Core.LUNAR;
            case CURSE -> Core.CURSE;
            case SUMMON -> Core.SUMMON;
            case VOID -> Core.VOID;
            case SPACE -> Core.SPACE;
            case UNIVERSAL -> Core.LUNAR;
        };
    }

    private static Carrier carrier(Spell spell, int level) {
        if (spell.getConfig().projectile) return Carrier.PROJECTILE;
        if (spell.getAimMaxRange(level) > 0) return Carrier.LOCK;
        if (spell.getBaseDamage() <= 0 && core(spell.getElement()) == Core.LUNAR) return Carrier.SELF;
        return Carrier.CONTACT;
    }

    private static UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return UUID.randomUUID();
        }
    }
}
