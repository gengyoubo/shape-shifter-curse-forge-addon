package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;
import java.util.UUID;

/** Server-only PvE additions. Every offensive addition rechecks the caster's whitelist. */
public final class NpcCombatEnhancements {
    private static final BalanceReader BAL = new BalanceReader("abilities.npc_combat");
    private NpcCombatEnhancements() { }

    public static double value(String key, double fallback) { return BAL.d(key, fallback); }
    public static boolean isEnemyNpc(Entity caster, LivingEntity target) {
        return !target.level().isClientSide && !(target instanceof Player)
                && caster instanceof ServerPlayer player && caster.level() == target.level()
                && !WhitelistUtils.isProtected(player, target);
    }
    public static float scale(Entity caster, LivingEntity target, float amount, String key, double fallback) {
        return isEnemyNpc(caster, target) ? amount * (float) value(key, fallback) : amount;
    }
    public static int duration(Entity caster, LivingEntity target, int ticks, String key, double fallback) {
        return isEnemyNpc(caster, target) ? Math.max(1, (int) Math.round(ticks * value(key, fallback))) : ticks;
    }
    public static float fixedDamage(Entity caster, Entity target, float amount) {
        return target instanceof LivingEntity living
                ? scale(caster, living, amount, "mancianima_fixed_mul", 2.0) : amount;
    }
    public static float primaryCap(Entity caster, LivingEntity target, float original) {
        return isEnemyNpc(caster, target) ? (float) value("mancianima_primary_cap", 40.0) : original;
    }
    public static float novaMaxDamage() { return (float) value("nova_explosion", 100.0); }

    public static void applyPurification(Entity caster, LivingEntity target) {
        if (!isEnemyNpc(caster, target) || !FormUtils.isAllaySP((LivingEntity) caster)) return;
        int ticks = BAL.i("allay_purify_duration", 300);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1), caster);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0), caster);
    }
    public static void applyFallenScream(ServerPlayer caster, LivingEntity target, int ticks) {
        if (WhitelistUtils.isProtected(caster, target)) return;
        int actualTicks = duration(caster, target, ticks, "fallen_glow_duration_mul", 2.0);
        MobEffectInstance glow = new MobEffectInstance(MobEffects.GLOWING, actualTicks, 0);
        boolean applied = target.addEffect(glow, caster);
        if (isEnemyNpc(caster, target) && markApplies(caster, target, glow, applied)) {
            state(target).markFallen(caster.getUUID(), target.level().getGameTime(), actualTicks);
        }
    }
    public static void applyWildCatStun(Entity caster, LivingEntity target, int ticks) {
        if (caster instanceof ServerPlayer player && WhitelistUtils.isProtected(player, target)) return;
        boolean enhanced = isEnemyNpc(caster, target) && FormUtils.isWildCatSP((LivingEntity) caster);
        int actualTicks = enhanced ? duration(caster, target, ticks, "wild_cat_stun_duration_mul", 2.0) : ticks;
        MobEffectInstance stun = new MobEffectInstance(AddonEffects.STUN.get(), actualTicks, 0, false, false, true);
        boolean applied = target.addEffect(stun, caster);
        if (enhanced && markApplies(caster, target, stun, applied)) {
            state(target).markWildCat(caster.getUUID(), target.level().getGameTime(), actualTicks);
        }
    }

    private static boolean markApplies(Entity caster, LivingEntity target, MobEffectInstance effect, boolean applied) {
        // Vanilla returns false when an equal/longer effect already exists; the cast still marks that NPC.
        return applied || target.hasEffect(effect.getEffect()) && target.canBeAffected(effect)
                && !DomainManager.blocksEffect(target, caster);
    }
    public static MobEffectInstance enhanceFruitDebuff(LivingEntity target, MobEffectInstance effect, Entity source) {
        // 扩散传递的效果已包含原施法的 NPC 加成，不再叠加。
        if (github.com.gengyoubo.sscfa.spell.BatSpellLink.transmitting()) return effect;
        if (!isEnemyNpc(source, target) || !FormUtils.isForm((LivingEntity) source, FormIdentifiers.BAT_PARASITIC_FRUIT)
                || effect.getEffect().getCategory() != MobEffectCategory.HARMFUL) return effect;
        // 施法联动开启时取该次施法快照中的 NPC 加成，否则用默认加成。
        var cast = github.com.gengyoubo.sscfa.spell.BatSpellLink.current();
        return new MobEffectInstance(effect.getEffect(), effect.getDuration(),
                effect.getAmplifier() + (cast == null ? fruitDebuffLevelBonus() : cast.npcBonus), effect.isAmbient(),
                effect.isVisible(), effect.showIcon(), null, effect.getFactorData());
    }

    public static int fruitDebuffLevelBonus(){return BAL.i("fruit_debuff_level_bonus",1);}

    public static NpcCombatState state(LivingEntity target) {
        return ((NpcCombatStateAccess) target).ssca$getNpcCombatState();
    }
    /** Pets, SSC minions and tagged summons all resolve to the actual online owner. */
    public static ServerPlayer owner(Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity == null || entity.getServer() == null) return null;
        // SSC Forge minions (Anubis wolves, ...) are tamable; other summons carry an owner tag.
        UUID uuid = entity instanceof TamableAnimal tame ? tame.getOwnerUUID() : null;
        if (uuid == null) {
            for (String tag : entity.getTags()) {
                String raw = tag.startsWith("ssc_owner:") ? tag.substring(10)
                        : tag.startsWith("owner:") ? tag.substring(6) : null;
                if (raw == null) continue;
                try { uuid = UUID.fromString(raw); break; }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return uuid == null ? null : entity.getServer().getPlayerList().getPlayer(uuid);
    }

    private static boolean isFallenSummon(Entity attacker, ServerPlayer owner) {
        if (attacker instanceof Vex && attacker.getTags().contains("ssc_fallen_allay_vex")) return true;
        if (!FormUtils.isForm(owner, FormIdentifiers.FALLEN_ALLAY_SP)) return false;
        return attacker instanceof net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity || !(attacker instanceof TamableAnimal)
                && attacker.getTags().stream().anyMatch(tag -> tag.startsWith("owner:") || tag.startsWith("ssc_owner:"));
    }

    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (target.level().isClientSide || amount <= 0 || source == null) return amount;
        Entity attacker = source.getEntity();
        ServerPlayer dealer = owner(attacker);
        boolean enemyNpc = isEnemyNpc(dealer, target);
        if (enemyNpc) {
            if (attacker == dealer) {
                if (FormUtils.isAxolotlFluorescent(dealer)) amount *= (float) value("fluorescent_damage", 1.5);
                if (FormUtils.isAnyForm(dealer, FormIdentifiers.SNOW_FOX_SP, FormIdentifiers.SNOW_FOX_FROSTSPINE))
                    amount *= (float) value("snow_fox_damage", 1.5);
                if (FormUtils.isForm(dealer, FormIdentifiers.FALLEN_ALLAY_SP)) {
                    amount *= (float) value("fallen_damage", 1.5);
                    if (target.hasEffect(MobEffects.GLOWING)
                            && state(target).hasFallen(dealer.getUUID(), target.level().getGameTime()))
                        amount *= (float) value("fallen_mark_dealt", 1.25);
                }
                if (FormUtils.isAnubisWolfSP(dealer) && AnubisWolfSpDeathDomain.containsForNpcCombat(dealer, target))
                    amount *= (float) value("anubis_domain_dealt", 1.2);
                if (FormUtils.isForm(dealer, FormIdentifiers.SPIDER_MOON_WEAVER)
                        && target.hasEffect(ModEffects.ENTANGLED_FULL.get()))
                    amount *= (float) value("moon_cocoon_dealt", 1.25);
            } else if (isFallenSummon(attacker, dealer)) {
                amount *= (float) value("fallen_damage", 1.5);
            }
        }
        // Wild-cat exposure doubles all sources, including environment, once (multiple cats do not stack).
        if (!(target instanceof Player) && target.hasEffect(AddonEffects.STUN.get())
                && (dealer == null || !WhitelistUtils.isProtected(dealer, target))) {
            for (UUID casterId : state(target).wildCats(target.level().getGameTime())) {
                if (!WhitelistUtils.isProtected(casterId, (ServerLevel) target.level(), target)) {
                    amount *= (float) value("wild_cat_stun_taken", 2.0);
                    break;
                }
            }
        }
        if (attacker instanceof LivingEntity npc && !(npc instanceof Player)) {
            ServerPlayer defender = owner(target);
            if (defender != null && isEnemyNpc(defender, npc)) {
                if (FormUtils.isAnubisWolfSP(defender)) amount *= (float) value("anubis_taken", 0.5);
                if (target == defender) {
                    if (FormUtils.isForm(defender, FormIdentifiers.OCELOT_NOVA)) amount *= (float) value("nova_taken", 0.75);
                    if (FormUtils.isForm(defender, FormIdentifiers.FALLEN_ALLAY_SP) && npc.hasEffect(MobEffects.GLOWING)
                            && state(npc).hasFallen(defender.getUUID(), npc.level().getGameTime()))
                        amount *= (float) value("fallen_mark_taken", 0.75);
                }
            }
        }
        return amount;
    }
}
