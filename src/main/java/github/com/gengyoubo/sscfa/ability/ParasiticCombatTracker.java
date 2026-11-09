/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 寄生果蝠主技能「交战状态」追踪器?
 * <p>
 * 判定一个玩家是否处于交战：
 * - 造成伤害?10s?00t）内?
 * - 受伤?10s 内；
 * - 10s 后若「对其造成伤害的来源」仍存活且在?10 格内，仍视为交战?
 * <p>
 * 服务端权威，仅追踪玩家；非玩家用简单判定（受击中或有攻击目标）?
 */
public final class ParasiticCombatTracker {
    /** 交战判定时长（tick）：10s；默认值，运行时从 balance 快照读取（abilities.parasitic_seed_system?*/
    private static final int COMBAT_TICKS = 200;
    /** 来源在场判定半径平方?0 格；未登记派生值，不开放） */
    private static final double SOURCE_RANGE_SQ = 10.0 * 10.0;

    /** balance 快照读取（快照未初始化回退默认常量?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");

    private static final Map<UUID, CombatData> DATA = new ConcurrentHashMap<>();

    private ParasiticCombatTracker() {
    }

    private static final class CombatData {
        long lastDamageDealt = Long.MIN_VALUE;
        long lastHurt = Long.MIN_VALUE;
        UUID lastHurtSource;
    }

    public static void init() {
        // ?ALLOW_DAMAGE（伤害结算前）记录交战时间戳，return true 不拦截伤?
        github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((entity, source, amount) -> {
            if (entity.level() instanceof ServerLevel) {
                long now = entity.level().getGameTime();
                // 受伤方是玩家 ?记录受伤时间与来?
                if (entity instanceof ServerPlayer victim) {
                    CombatData d = DATA.computeIfAbsent(victim.getUUID(), k -> new CombatData());
                    d.lastHurt = now;
                    Entity attacker = source.getEntity();
                    d.lastHurtSource = attacker != null ? attacker.getUUID() : null;
                }
                // 攻击方是玩家 ?记录造成伤害时间
                if (source.getEntity() instanceof ServerPlayer dealer) {
                    CombatData d = DATA.computeIfAbsent(dealer.getUUID(), k -> new CombatData());
                    d.lastDamageDealt = now;
                }
            }
            return true;
        });
    }

    /** 判定目标是否处于交战。非玩家用简单判定（受击中或有攻击目标）?*/
    public static boolean isInCombat(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return entity.hurtTime > 0 || (entity instanceof Mob mob && mob.getTarget() != null);
        }
        CombatData d = DATA.get(player.getUUID());
        if (d == null) return false;
        long now = player.level().getGameTime();
        int combatTicks = BAL.i("combat_ticks", COMBAT_TICKS);
        // 哨兵判断：未记录的一侧不参与判定，防?now - Long.MIN_VALUE 下溢为负导致永久交战
        if (d.lastDamageDealt != Long.MIN_VALUE && now - d.lastDamageDealt < combatTicks) return true;
        if (d.lastHurt != Long.MIN_VALUE && now - d.lastHurt < combatTicks) return true;
        // 10s 后：对其造成伤害的来源仍存活且在 10 格内 ?仍交?
        if (d.lastHurtSource != null && player.level() instanceof ServerLevel sw) {
            Entity src = sw.getEntity(d.lastHurtSource);
            if (src != null && src.isAlive() && src.distanceToSqr(player) <= SOURCE_RANGE_SQ) {
                return true;
            }
        }
        return false;
    }
    public static void clearPlayer(UUID uuid) { DATA.remove(uuid); }
    public static void clearAll() { DATA.clear(); }

}
