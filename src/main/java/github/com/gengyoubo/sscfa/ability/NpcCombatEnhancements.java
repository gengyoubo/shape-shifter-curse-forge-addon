package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 服务端 PvE 加成：技能打敌对 NPC 时按平衡参数放大伤害/时长；每次加成都重新检查施法者白名单。
 *
 * <p>目前移植了纯数值缩放部分和野猫眩晕。Fabric 版里依附于各形态的其余部分（伤害修正 modifyDamage、
 * 净化/尖啸的附加效果、寄生果蝠减益增强）随对应形态一起移植。</p>
 */
public final class NpcCombatEnhancements {
    private static final BalanceReader BAL = new BalanceReader("abilities.npc_combat");

    private NpcCombatEnhancements() {}

    public static double value(String key, double fallback) {
        return BAL.d(key, fallback);
    }

    /** 目标是不在施法者白名单里的非玩家生物（同一世界、服务端）。 */
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
        return target instanceof LivingEntity living ? scale(caster, living, amount, "mancianima_fixed_mul", 2.0) : amount;
    }

    public static float primaryCap(Entity caster, LivingEntity target, float original) {
        return isEnemyNpc(caster, target) ? (float) value("mancianima_primary_cap", 40.0) : original;
    }

    public static float novaMaxDamage() {
        return (float) value("nova_explosion", 100.0);
    }

    /** 野猫震慑冲刺的眩晕：对敌对 NPC 时长按平衡参数放大，并在目标身上记下野猫眩晕标记。 */
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
        // 目标已有等长或更长的同效果时原版返回 false；这次施放仍然标记该 NPC。
        return applied || target.hasEffect(effect.getEffect()) && target.canBeAffected(effect)
                && !DomainManager.blocksEffect(target, caster);
    }

    public static NpcCombatState state(LivingEntity target) {
        return ((NpcCombatStateAccess) target).ssca$getNpcCombatState();
    }

    public static float modifyDamage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) { return amount; }
    public static MobEffectInstance enhanceFruitDebuff(LivingEntity target, MobEffectInstance effect, Entity source) { return effect; }
}
