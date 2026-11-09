/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 寄生果蝠种子量能量回复管理器?
 * <p>
 * 按交战状态决定回复间隔：未交战??5s??00t）??1 点，交战??8s??60t）??1 点??
 * ??10 点时不再回复。交战判定与种子寿命一致（{@link ParasiticCombatTracker}）??
 * 服务端权威，替代??JSON action_over_time（其无法判定交战）??
 */
public final class ParasiticSeedEnergyRegen {
    // 以下均为默认值；运行时从 balance 快照读取（abilities.parasitic_seed_system??
    /** 未交战回复间隔（tick）??s */
    private static final int INTERVAL_PEACE = 100;
    /** 交战回复间隔（tick）??s */
    private static final int INTERVAL_COMBAT = 160;
    /** 能量上限 */
    private static final int MAX_ENERGY = 10;

    /** balance 快照读取（快照未初始化回退默认常量?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");

    /** 玩??UUID -> 距上次回复已累计 tick ??*/
    private static final Map<UUID, Integer> ACCUM = new ConcurrentHashMap<>();

    private ParasiticSeedEnergyRegen() {
    }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        // 仅果蝠形态持有种子量能量资源；非该形态直接清理累计并跳过?
        // 注意：必须??hasPowerId（是否持??power），不能??hasResource(...,0)—??
        // 后者对?power 玩??= 0>=0 恒真，守卫完全失效，会导致全玩家??tick 白跑
        // 且周期性白?PowerHolderComponent.sync 全量同步包??
        // 先用便宜的形态组件读（单次）挡掉绝大多数非果蝠玩家??
        // 再??O(power?? ??power 检测；果蝠形态才可能挂??power，两层判定结果等价??
        if (!github.com.gengyoubo.sscfa.util.FormUtils.isBatParasiticFruit(player)
                || !github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(
                        player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY)) {
            ACCUM.remove(player.getUUID());
            return;
        }
        int current = PowerUtils.getResourceValue(player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY);
        if (current >= BAL.i("max_energy", MAX_ENERGY)) {
            ACCUM.put(player.getUUID(), 0);
            return;
        }
        int interval = ParasiticCombatTracker.isInCombat(player)
                ? BAL.i("regen_interval_combat", INTERVAL_COMBAT)
                : BAL.i("regen_interval_peace", INTERVAL_PEACE);
        int acc = ACCUM.getOrDefault(player.getUUID(), 0) + 1;
        if (acc >= interval) {
            PowerUtils.changeResourceValueAndSync(player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, 1);
            acc = 0;
        }
        ACCUM.put(player.getUUID(), acc);
    }
}
