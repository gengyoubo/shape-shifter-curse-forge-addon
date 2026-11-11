/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 寄生果蝠形态吃种子机制?
 *   1. 复用原版 SSC ?{@code shape-shifter-curse:custom_edible} power（form_bat_parasitic_fruit_seed_eat.json?
 *      把种子物品标记成可吃食物，原版吃食物动画接管"持续?1.6 秒进?的视觉表现；
 *   2. 玩家面对方块?BlockItem.useOnBlock 默认会触发种植；本类?UseBlockCallback 在玩家潜?+ 持种?+
 *      寄生果蝠形态时直接转调 ItemStack.use(...)，绕过种植走食物分支?
 *   3. 实际"加能?+ 推配??{@link #onSeedEaten(ServerPlayer, ItemStack)} 中完成，?
 *      LivingEntity.eatFood ?mixin 在吃食物完成时调用；
 *   4. 配额：每 3 分钟最多吃 8 颗，超额时红?actionBar 提示且不进入吃食流程?
 */
public final class SeedEnergyEatingHandler {

    /** 视为??的原版种子物品集合（?form_bat_parasitic_fruit_seed_eat.json 列表对齐）?*/
    public static final Set<net.minecraft.world.item.Item> EDIBLE_SEEDS = Set.of(
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.WHEAT_SEEDS,
            Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD
    );

    /** 配额窗口? 分钟（未登记，不开放）?*/
    private static final long QUOTA_WINDOW_TICKS = 60L * 20L * 3L;
    /** 配额上限? 分钟内最?8 次；默认值，运行时从 balance 快照读取（abilities.parasitic_seed_system）?*/
    private static final int QUOTA_LIMIT = 8;
    /** 超额提示节流（未登记，不开放）?*/
    private static final int OVER_QUOTA_HINT_COOLDOWN_TICKS = 40;

    /** balance 快照读取（快照未初始化回退默认常量?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");

    /** 每个玩家近期完成时间戳队列（3 分钟滑动窗口配额）?*/
    private static final Map<UUID, Deque<Long>> RECENT_EATS = new HashMap<>();
    /** 上次发?吃不下了"提示?tick，避免每 tick 刷屏?*/
    private static final Map<UUID, Long> LAST_OVER_QUOTA_HINT_TICK = new HashMap<>();

    private SeedEnergyEatingHandler() {
    }

    public static void register() {
        // 玩家面对方块持种子时，原版会优先触发 BlockItem.useOnBlock 种植?
        // 在寄生果蝠潜行场景下，主动转?ItemStack.use 进入食物分支
        github.com.gengyoubo.sscfa.util.SscEvents.useBlock((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !EDIBLE_SEEDS.contains(stack.getItem())) return InteractionResult.PASS;
            if (!hasSeedEnergyPower(player)) return InteractionResult.PASS;

            // 服务端进行配额预检查；超额则红字提示并阻断
            if (player instanceof ServerPlayer sp) {
                long now = sp.level().getGameTime();
                int quotaLimit = BAL.i("quota_limit", QUOTA_LIMIT);
                pruneQuota(sp.getUUID(), now);
                Deque<Long> recent = RECENT_EATS.get(sp.getUUID());
                if (recent != null && recent.size() >= quotaLimit) {
                    sendOverQuotaHint(sp, now);
                    return InteractionResult.FAIL;
                }
                // 能量已满则不进入吃食流程，让玩家保留种子
                int current = github.com.gengyoubo.sscfa.resource.ResourceBars.get(sp,
                        github.com.gengyoubo.sscfa.resource.BarKeys.SEED);
                int max = github.com.gengyoubo.sscfa.resource.ResourceBars.maxOf(sp,
                        github.com.gengyoubo.sscfa.resource.BarKeys.SEED);
                if (max <= 0) max = 10;
                if (current >= max) return InteractionResult.FAIL;
            }
            // 转调 Item.use（基类）?SSC ?custom_edible mixin ?isFood 改成 true，进入吃食物动画
            InteractionResultHolder<ItemStack> result = stack.use(world, player, hand);
            return result.getResult();
        });

        // 玩家面对空气直接右键时也会进?Item.use ?SSC custom_edible 食物分支?
        // 此处统一?UseItemCallback 做配额预检查，超额时直?FAIL，阻止进入吃食物动画
        github.com.gengyoubo.sscfa.util.SscEvents.useItem((player, world, hand) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !EDIBLE_SEEDS.contains(stack.getItem())) return InteractionResultHolder.pass(stack);
            if (!hasSeedEnergyPower(player)) return InteractionResultHolder.pass(stack);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

            long now = sp.level().getGameTime();
            pruneQuota(sp.getUUID(), now);
            int quotaLimit = BAL.i("quota_limit", QUOTA_LIMIT);
            Deque<Long> recent = RECENT_EATS.get(sp.getUUID());
            if (recent != null && recent.size() >= quotaLimit) {
                sendOverQuotaHint(sp, now);
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
    }

    /**
     * ?LivingEntity.eatFood ?mixin 在吃种子完成时调用?
     * @return true 表示该次吃食物属于本机制（mixin 不需要再做额外处理）?
     */
    public static boolean onSeedEaten(ServerPlayer sp, ItemStack stack) {
        if (stack.isEmpty() || !EDIBLE_SEEDS.contains(stack.getItem())) return false;
        if (!hasSeedEnergyPower(sp)) return false;

        long now = sp.level().getGameTime();
        pruneQuota(sp.getUUID(), now);

        Deque<Long> recent = RECENT_EATS.computeIfAbsent(sp.getUUID(), k -> new ArrayDeque<>());
        if (recent.size() >= BAL.i("quota_limit", QUOTA_LIMIT)) {
            sendOverQuotaHint(sp, now);
            return true;
        }

        github.com.gengyoubo.sscfa.resource.ResourceBars.gain(sp,
                github.com.gengyoubo.sscfa.resource.BarKeys.SEED, 1);

        recent.addLast(now);
        return true;
    }

    private static void pruneQuota(UUID uuid, long now) {
        Deque<Long> recent = RECENT_EATS.get(uuid);
        if (recent == null) return;
        while (!recent.isEmpty() && now - recent.peekFirst() >= QUOTA_WINDOW_TICKS) {
            recent.pollFirst();
        }
    }

    private static void sendOverQuotaHint(ServerPlayer sp, long now) {
        Long last = LAST_OVER_QUOTA_HINT_TICK.get(sp.getUUID());
        if (last != null && now - last < OVER_QUOTA_HINT_COOLDOWN_TICKS) return;
        LAST_OVER_QUOTA_HINT_TICK.put(sp.getUUID(), now);
        sp.displayClientMessage(Component.translatable("msg.my_addon.seed_eat_too_much")
                .withStyle(ChatFormatting.RED), true);
    }

    private static boolean hasSeedEnergyPower(Player player) {
        return FormPowerRegistry.has(player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY);
    }
    /** Keep unexpired quotas across reconnects; discard only entries whose window has elapsed. */
    public static void pruneAll(long now) {
        for (UUID uuid : java.util.List.copyOf(RECENT_EATS.keySet())) {
            pruneQuota(uuid, now);
            Deque<Long> recent = RECENT_EATS.get(uuid);
            if (recent != null && recent.isEmpty()) RECENT_EATS.remove(uuid);
        }
        LAST_OVER_QUOTA_HINT_TICK.values().removeIf(t -> now - t >= OVER_QUOTA_HINT_COOLDOWN_TICKS);
    }

    public static void clearAll() {
        RECENT_EATS.clear();
        LAST_OVER_QUOTA_HINT_TICK.clear();
    }

}
