package github.com.gengyoubo.sscfa.ability.fruit;

import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.power.ParasiticFruitSeedPower;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * 寄生果蝠主技能的手势：按住蓄力、松开投掷（≥0.5 秒），两次短按（各 &lt;0.5 秒、间隔 ≤0.3 秒）切换果实。
 *
 * <p>Fabric 版由客户端识别手势并发送空包；Forge 端 SSC 会把技能键的按下/松开分派给
 * {@code SscPowerType}，因此手势改在服务端按 tick 计时识别，经过时间、消耗与所选果实都由服务端决定。
 * 所选果实存于玩家持久数据（跨死亡保留），换形态时重置为自动；蓄力状态只在内存中。
 * 状态经 {@code ssc_addon:fruit_state} 同步给本人客户端，用于技能栏图标、蓄力条与提示音。</p>
 */
public final class ParasiticFruitAbility {
    public static final ResourceLocation STATE = ResourceLocation.fromNamespaceAndPath("ssc_addon", "fruit_state");
    private static final String SAVE_KEY = "ssca_fruit_selection";
    /** 双击窗口（≈300 ms）与切换防抖。 */
    private static final int DOUBLE_TAP_TICKS = 6, CYCLE_DEBOUNCE_TICKS = 4;

    private record Charge(ServerPlayer player, ServerLevel world, long start, ResourceLocation powerId) {}

    private static final Map<UUID, Charge> CHARGES = new HashMap<>();
    private static final Map<UUID, Long> PRESSED_AT = new HashMap<>();
    private static final Map<UUID, Long> LAST_TAP = new HashMap<>();
    private static final Map<UUID, Long> LAST_CYCLE = new HashMap<>();
    private static final Map<UUID, ResourceLocation> OBSERVED_FORM = new HashMap<>();
    private static boolean registered;

    private ParasiticFruitAbility() {}

    public static synchronized void init() {
        if (registered) return;
        registered = true;
        AddonNetwork.clientbound(STATE);
        AddonNetwork.clientbound(github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager.MODE_STATE);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) tick(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            sync(player);
            github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager.syncMode(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) sync(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                cancel(player);
                sync(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            cancel(player);
            UUID id = player.getUUID();
            PRESSED_AT.remove(id);
            LAST_TAP.remove(id);
            LAST_CYCLE.remove(id);
            OBSERVED_FORM.remove(id);
            ParasiticFruitLayers.removeOwner(player);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            CHARGES.clear();
            PRESSED_AT.clear();
            LAST_TAP.clear();
            LAST_CYCLE.clear();
            OBSERVED_FORM.clear();
        });
    }

    // ------------------------------------------------------------------ 选择

    public static FruitSelection selection(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return FruitSelection.fromId(persisted.getInt(SAVE_KEY));
    }

    public static void setSelection(ServerPlayer player, FruitSelection value) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(SAVE_KEY, (value == null ? FruitSelection.AUTO : value).ordinal());
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        sync(player);
    }

    public static boolean charging(ServerPlayer player) { return CHARGES.containsKey(player.getUUID()); }

    public static boolean allowed(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isRemoved()
                && FormUtils.isForm(player, FormIdentifiers.BAT_PARASITIC_FRUIT)
                && !AbilityUseGuard.blocked(player) && !player.hasEffect(AddonEffects.PURIFIED.get())
                && BalanceIntegration.isPlayerReady(player);
    }

    // ------------------------------------------------------------------ 手势（由能力类型的按键回调调用）

    public static void pressed(ServerPlayer player, ResourceLocation powerId) {
        long now = player.level().getGameTime();
        PRESSED_AT.put(player.getUUID(), now);
        // 松开事件丢失时（例如按住时打开界面），下一次按下重新开始。
        cancel(player);
        if (!allowed(player) || !SkillCooldowns.ready(player, powerId.toString())) return;
        if (!SpellChannelManager.onSkillAction(player)) return;
        CHARGES.put(player.getUUID(), new Charge(player, player.serverLevel(), now, powerId));
        chargeState(player, true, 0);
    }

    public static void released(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long pressedAt = PRESSED_AT.remove(player.getUUID());
        long held = pressedAt == null ? 0 : now - pressedAt;
        Charge charge = CHARGES.remove(player.getUUID());
        if (charge != null) chargeState(player, false, 0);
        if (held >= FruitSelection.MIN_CHARGE) {
            LAST_TAP.remove(player.getUUID());
            if (charge == null || charge.player() != player || charge.world() != player.level() || !allowed(player)) return;
            int ticks = (int) Math.min(FruitSelection.FULL_CHARGE, Math.max(0, now - charge.start()));
            if (!FruitSelection.canRelease(ticks) || !SpellChannelManager.onSkillAction(player)) return;
            ParasiticFruitSeedPower.throwChargedSeed(player, charge.powerId(), ticks, selection(player));
            return;
        }
        Long lastTap = LAST_TAP.get(player.getUUID());
        if (lastTap != null && now - lastTap <= DOUBLE_TAP_TICKS) {
            LAST_TAP.remove(player.getUUID());
            cycle(player);
        } else {
            LAST_TAP.put(player.getUUID(), now);
        }
    }

    public static void cancel(ServerPlayer player) {
        Charge charge = CHARGES.get(player.getUUID());
        if (charge != null && charge.player() == player) {
            CHARGES.remove(player.getUUID());
            chargeState(player, false, 0);
        }
    }

    public static void cycle(ServerPlayer player) {
        if (!allowed(player) || CHARGES.containsKey(player.getUUID()) || ParasiticFruitSeedPower.active(player) == null) return;
        long now = player.level().getGameTime();
        Long last = LAST_CYCLE.get(player.getUUID());
        if (last != null && now - last < CYCLE_DEBOUNCE_TICKS) return;
        LAST_CYCLE.put(player.getUUID(), now);
        FruitSelection next = selection(player).next();
        setSelection(player, next);
        player.displayClientMessage(Component.translatable("message.my_addon.fruit_selection",
                Component.translatable("fruit.my_addon." + next.key())), true);
    }

    /** 每 tick 驱动：换形态重置选择、校验与同步蓄力。由 PlayerTickEvent 调用（假玩家不会触发该事件）。 */
    public static void tick(ServerPlayer player) {
        if (player.isAlive()) {
            var current = FormUtils.currentFormId(player);
            UUID id = player.getUUID();
            boolean observed = OBSERVED_FORM.containsKey(id);
            ResourceLocation previous = OBSERVED_FORM.put(id, current);
            if (observed && !Objects.equals(previous, current)) {
                cancel(player);
                if (selection(player) != FruitSelection.AUTO) setSelection(player, FruitSelection.AUTO);
            }
        }
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null) return;
        if (charge.player() != player || charge.world() != player.level() || !allowed(player)
                || ParasiticFruitSeedPower.active(player) == null) {
            cancel(charge.player());
            return;
        }
        long elapsed = player.level().getGameTime() - charge.start();
        if (elapsed <= FruitSelection.FULL_CHARGE && elapsed % 5 == 0) chargeState(player, true, (int) elapsed);
    }

    // ------------------------------------------------------------------ 同步

    private static void chargeState(ServerPlayer player, boolean active, int ticks) {
        send(player, active, ticks);
    }

    public static void sync(ServerPlayer player) {
        Charge charge = CHARGES.get(player.getUUID());
        int ticks = charge == null ? 0 : (int) Math.min(FruitSelection.FULL_CHARGE,
                Math.max(0, player.level().getGameTime() - charge.start()));
        send(player, charge != null, ticks);
    }

    private static void send(ServerPlayer player, boolean charging, int ticks) {
        if (!AddonNetwork.canSend(player, STATE)) return;
        var buf = AddonNetwork.buffer();
        buf.writeVarInt(selection(player).ordinal());
        buf.writeBoolean(charging);
        buf.writeVarInt(Math.max(0, Math.min(FruitSelection.FULL_CHARGE, ticks)));
        AddonNetwork.sendToPlayer(player, STATE, buf);
    }
}
