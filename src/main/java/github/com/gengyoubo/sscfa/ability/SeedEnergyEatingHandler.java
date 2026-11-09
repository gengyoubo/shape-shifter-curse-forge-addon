/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import io.github.apace100.apoli.component.PowerHolderComponent;
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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 瀵勭敓鏋滆潬褰㈡€佸悆绉嶅瓙鏈哄埗锛?
 *   1. 澶嶇敤鍘熺増 SSC 鐨?{@code shape-shifter-curse:custom_edible} power锛坒orm_bat_parasitic_fruit_seed_eat.json锛?
 *      鎶婄瀛愮墿鍝佹爣璁版垚鍙悆椋熺墿锛屽師鐗堝悆椋熺墿鍔ㄧ敾鎺ョ"鎸佺画绾?1.6 绉掕繘搴?鐨勮瑙夎〃鐜帮紱
 *   2. 鐜╁闈㈠鏂瑰潡鏃?BlockItem.useOnBlock 榛樿浼氳Е鍙戠妞嶏紱鏈被鐢?UseBlockCallback 鍦ㄧ帺瀹舵綔琛?+ 鎸佺瀛?+
 *      瀵勭敓鏋滆潬褰㈡€佹椂鐩存帴杞皟 ItemStack.use(...)锛岀粫杩囩妞嶈蛋椋熺墿鍒嗘敮锛?
 *   3. 瀹為檯"鍔犺兘閲?+ 鎺ㄩ厤棰?鍦?{@link #onSeedEaten(ServerPlayer, ItemStack)} 涓畬鎴愶紝鐢?
 *      LivingEntity.eatFood 鐨?mixin 鍦ㄥ悆椋熺墿瀹屾垚鏃惰皟鐢紱
 *   4. 閰嶉锛氭瘡 3 鍒嗛挓鏈€澶氬悆 8 棰楋紝瓒呴鏃剁孩瀛?actionBar 鎻愮ず涓斾笉杩涘叆鍚冮娴佺▼銆?
 */
public final class SeedEnergyEatingHandler {

    /** 瑙嗕负鍙?鍚?鐨勫師鐗堢瀛愮墿鍝侀泦鍚堬紙涓?form_bat_parasitic_fruit_seed_eat.json 鍒楄〃瀵归綈锛夈€?*/
    public static final Set<net.minecraft.world.item.Item> EDIBLE_SEEDS = Set.of(
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.WHEAT_SEEDS,
            Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD
    );

    /** 閰嶉绐楀彛锛? 鍒嗛挓锛堟湭鐧昏锛屼笉寮€鏀撅級銆?*/
    private static final long QUOTA_WINDOW_TICKS = 60L * 20L * 3L;
    /** 閰嶉涓婇檺锛? 鍒嗛挓鍐呮渶澶?8 娆★紱榛樿鍊硷紝杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.parasitic_seed_system锛夈€?*/
    private static final int QUOTA_LIMIT = 8;
    /** 瓒呴鎻愮ず鑺傛祦锛堟湭鐧昏锛屼笉寮€鏀撅級銆?*/
    private static final int OVER_QUOTA_HINT_COOLDOWN_TICKS = 40;

    /** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");

    /** 姣忎釜鐜╁杩戞湡瀹屾垚鏃堕棿鎴抽槦鍒楋紙3 鍒嗛挓婊戝姩绐楀彛閰嶉锛夈€?*/
    private static final Map<UUID, Deque<Long>> RECENT_EATS = new HashMap<>();
    /** 涓婃鍙戦€?鍚冧笉涓嬩簡"鎻愮ず鐨?tick锛岄伩鍏嶆瘡 tick 鍒峰睆銆?*/
    private static final Map<UUID, Long> LAST_OVER_QUOTA_HINT_TICK = new HashMap<>();

    private SeedEnergyEatingHandler() {
    }

    public static void register() {
        // 鐜╁闈㈠鏂瑰潡鎸佺瀛愭椂锛屽師鐗堜細浼樺厛瑙﹀彂 BlockItem.useOnBlock 绉嶆锛?
        // 鍦ㄥ瘎鐢熸灉铦犳綔琛屽満鏅笅锛屼富鍔ㄨ浆璋?ItemStack.use 杩涘叆椋熺墿鍒嗘敮
        github.com.gengyoubo.sscfa.util.SscEvents.useBlock((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !EDIBLE_SEEDS.contains(stack.getItem())) return InteractionResult.PASS;
            if (!hasSeedEnergyPower(player)) return InteractionResult.PASS;

            // 鏈嶅姟绔繘琛岄厤棰濋妫€鏌ワ紱瓒呴鍒欑孩瀛楁彁绀哄苟闃绘柇
            if (player instanceof ServerPlayer sp) {
                long now = sp.level().getGameTime();
                int quotaLimit = BAL.i("quota_limit", QUOTA_LIMIT);
                pruneQuota(sp.getUUID(), now);
                Deque<Long> recent = RECENT_EATS.get(sp.getUUID());
                if (recent != null && recent.size() >= quotaLimit) {
                    sendOverQuotaHint(sp, now);
                    return InteractionResult.FAIL;
                }
                // 鑳介噺宸叉弧鍒欎笉杩涘叆鍚冮娴佺▼锛岃鐜╁淇濈暀绉嶅瓙
                int current = github.com.gengyoubo.sscfa.resource.ResourceBars.get(sp,
                        github.com.gengyoubo.sscfa.resource.BarKeys.SEED);
                int max = github.com.gengyoubo.sscfa.resource.ResourceBars.maxOf(sp,
                        github.com.gengyoubo.sscfa.resource.BarKeys.SEED);
                if (max <= 0) max = 10;
                if (current >= max) return InteractionResult.FAIL;
            }
            // 杞皟 Item.use锛堝熀绫伙級璁?SSC 鐨?custom_edible mixin 鎶?isFood 鏀规垚 true锛岃繘鍏ュ悆椋熺墿鍔ㄧ敾
            InteractionResultHolder<ItemStack> result = stack.use(world, player, hand);
            return result.getResult();
        });

        // 鐜╁闈㈠绌烘皵鐩存帴鍙抽敭鏃朵篃浼氳繘鍏?Item.use 鈫?SSC custom_edible 椋熺墿鍒嗘敮锛?
        // 姝ゅ缁熶竴鍦?UseItemCallback 鍋氶厤棰濋妫€鏌ワ紝瓒呴鏃剁洿鎺?FAIL锛岄樆姝㈣繘鍏ュ悆椋熺墿鍔ㄧ敾
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
     * 鐢?LivingEntity.eatFood 鐨?mixin 鍦ㄥ悆绉嶅瓙瀹屾垚鏃惰皟鐢ㄣ€?
     * @return true 琛ㄧず璇ユ鍚冮鐗╁睘浜庢湰鏈哄埗锛坢ixin 涓嶉渶瑕佸啀鍋氶澶栧鐞嗭級銆?
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
        try {
            return PowerHolderComponent.KEY.get(player).getPowers().stream()
                    .anyMatch(p -> p.getType().getIdentifier().equals(FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY));
        } catch (Exception e) {
            return false;
        }
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
