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
 * 瀵勭敓鏋滆潬涓绘妧鑳姐€屼氦鎴樼姸鎬併€嶈拷韪櫒銆?
 * <p>
 * 鍒ゅ畾涓€涓帺瀹舵槸鍚﹀浜庝氦鎴橈細
 * - 閫犳垚浼ゅ鍚?10s锛?00t锛夊唴锛?
 * - 鍙椾激鍚?10s 鍐咃紱
 * - 10s 鍚庤嫢銆屽鍏堕€犳垚浼ゅ鐨勬潵婧愩€嶄粛瀛樻椿涓斿湪鍏?10 鏍煎唴锛屼粛瑙嗕负浜ゆ垬銆?
 * <p>
 * 鏈嶅姟绔潈濞侊紝浠呰拷韪帺瀹讹紱闈炵帺瀹剁敤绠€鍗曞垽瀹氾紙鍙楀嚮涓垨鏈夋敾鍑荤洰鏍囷級銆?
 */
public final class ParasiticCombatTracker {
    /** 浜ゆ垬鍒ゅ畾鏃堕暱锛坱ick锛夛細10s锛涢粯璁ゅ€硷紝杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.parasitic_seed_system锛?*/
    private static final int COMBAT_TICKS = 200;
    /** 鏉ユ簮鍦ㄥ満鍒ゅ畾鍗婂緞骞虫柟锛?0 鏍硷紱鏈櫥璁版淳鐢熷€硷紝涓嶅紑鏀撅級 */
    private static final double SOURCE_RANGE_SQ = 10.0 * 10.0;

    /** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
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
        // 鐢?ALLOW_DAMAGE锛堜激瀹崇粨绠楀墠锛夎褰曚氦鎴樻椂闂存埑锛宺eturn true 涓嶆嫤鎴激瀹?
        github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((entity, source, amount) -> {
            if (entity.level() instanceof ServerLevel) {
                long now = entity.level().getGameTime();
                // 鍙椾激鏂规槸鐜╁ 鈫?璁板綍鍙椾激鏃堕棿涓庢潵婧?
                if (entity instanceof ServerPlayer victim) {
                    CombatData d = DATA.computeIfAbsent(victim.getUUID(), k -> new CombatData());
                    d.lastHurt = now;
                    Entity attacker = source.getEntity();
                    d.lastHurtSource = attacker != null ? attacker.getUUID() : null;
                }
                // 鏀诲嚮鏂规槸鐜╁ 鈫?璁板綍閫犳垚浼ゅ鏃堕棿
                if (source.getEntity() instanceof ServerPlayer dealer) {
                    CombatData d = DATA.computeIfAbsent(dealer.getUUID(), k -> new CombatData());
                    d.lastDamageDealt = now;
                }
            }
            return true;
        });
    }

    /** 鍒ゅ畾鐩爣鏄惁澶勪簬浜ゆ垬銆傞潪鐜╁鐢ㄧ畝鍗曞垽瀹氾紙鍙楀嚮涓垨鏈夋敾鍑荤洰鏍囷級銆?*/
    public static boolean isInCombat(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return entity.hurtTime > 0 || (entity instanceof Mob mob && mob.getTarget() != null);
        }
        CombatData d = DATA.get(player.getUUID());
        if (d == null) return false;
        long now = player.level().getGameTime();
        int combatTicks = BAL.i("combat_ticks", COMBAT_TICKS);
        // 鍝ㄥ叺鍒ゆ柇锛氭湭璁板綍鐨勪竴渚т笉鍙備笌鍒ゅ畾锛岄槻姝?now - Long.MIN_VALUE 涓嬫孩涓鸿礋瀵艰嚧姘镐箙浜ゆ垬
        if (d.lastDamageDealt != Long.MIN_VALUE && now - d.lastDamageDealt < combatTicks) return true;
        if (d.lastHurt != Long.MIN_VALUE && now - d.lastHurt < combatTicks) return true;
        // 10s 鍚庯細瀵瑰叾閫犳垚浼ゅ鐨勬潵婧愪粛瀛樻椿涓斿湪 10 鏍煎唴 鈫?浠嶄氦鎴?
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
