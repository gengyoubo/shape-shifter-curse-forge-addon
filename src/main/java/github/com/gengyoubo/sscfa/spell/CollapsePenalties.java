package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.ability.NineLivesManager;
import github.com.gengyoubo.sscfa.mixin.entity.LivingEntityTotemInvoker;
import github.com.gengyoubo.sscfa.neomagic.NeoMagic;
import github.com.gengyoubo.sscfa.neomagic.NeoNetwork;
import github.com.gengyoubo.sscfa.neomagic.NeoRuntime;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 服务端权威的崩解阶段惩罚：按负荷/安全值分 5 档减速，3 档起周期扣血。
 * 不提供药水免疫、伤害转换或随机残留施法锁。由 {@link NeoRuntime} 每 tick 驱动，
 * 变形、死亡、退出与关服时清理。
 */
public final class CollapsePenalties {
    private static final UUID SLOW = UUID.fromString("587f5108-6c90-464f-9226-937f876c195b");
    private static final double[] SLOWS = {0, .15, .30, .40, .60, .80};
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("ssc_addon", "collapse"));
    private static final Map<UUID, State> PLAYERS = new HashMap<>();

    private static final class State {
        final ServerPlayer player;
        int stage, damagingTicks;
        State(ServerPlayer player) { this.player = player; }
    }

    private CollapsePenalties() {}

    /** 恰好落在边界时归入较低档；负荷等于安全值始终安全。 */
    public static int stage(double collapse, double safety) {
        if (!(collapse > safety) || !(safety > 0) || !Double.isFinite(safety)) return 0;
        for (int i = 1; i < 5; i++) if (collapse <= safety * (1 + i * .2)) return i;
        return 5;
    }

    public static double slow(int stage) { return SLOWS[Math.max(0, Math.min(5, stage))]; }

    /** 原版身体转向的平方距离阈值需跟随两端同步的减速修饰符。 */
    public static float bodyYawMovementThreshold(LivingEntity entity, float threshold) {
        if (!(entity instanceof Player)) return threshold;
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var modifier = speed == null ? null : speed.getModifier(SLOW);
        if (modifier == null || modifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) return threshold;
        double remaining = 1 + modifier.getAmount();
        return remaining > 0 && remaining < 1 ? (float) (threshold * remaining * remaining) : threshold;
    }

    public static int currentStage(ServerPlayer player) {
        State state = PLAYERS.get(player.getUUID());
        return state == null || state.player != player ? 0 : state.stage;
    }

    public static void tick(ServerPlayer player) {
        State state = PLAYERS.get(player.getUUID());
        if (state == null || state.player != player) {
            if (state != null) clear(state.player);
            state = new State(player);
            PLAYERS.put(player.getUUID(), state);
            // 迁移旧存档中的崩解药水效果，不动普通失明/反胃药水。
            player.removeEffect(NeoMagic.DISORDER.get());
            player.removeEffect(NeoMagic.DISINTEGRATION.get());
            player.removeEffect(NeoMagic.BREAKDOWN.get());
        }
        double collapse = NeoRuntime.data(player).burden;
        int next = player.isAlive() && !player.isSpectator() && collapse > 0
                ? stage(collapse, NeoRuntime.profile(player).safety()) : 0;
        boolean changed = next != state.stage;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && (changed || next > 0 && speed.getModifier(SLOW) == null)) {
            speed.removeModifier(SLOW);
            if (next > 0) speed.addTransientModifier(new AttributeModifier(SLOW, "SSCA collapse slowdown",
                    -slow(next), AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        state.stage = next;
        if (next < 3 || player.isCreative()) state.damagingTicks = 0;
        else {
            state.damagingTicks++;
            int interval = next == 3 ? 20 : 10;
            if (state.damagingTicks >= interval) {
                state.damagingTicks = 0;
                drain(player, next == 5 ? 2 : 1);
            }
        }
        if (changed) NeoNetwork.hud(player);
    }

    private static void drain(ServerPlayer player, float amount) {
        // 直接扣除生命：不经护甲、吸收与能力伤害修正；保留真实死亡、图腾及九命复活。
        if (!player.isAlive() || player.isCreative() || player.isSpectator()
                || NineLivesManager.isInvulnerable(player)) return;
        var source = new net.minecraft.world.damagesource.DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
        if (player.getHealth() <= amount) {
            if (FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA) && NineLivesManager.tryRevive(player)) return;
            if (((LivingEntityTotemInvoker) player).sscAddon$tryUseTotem(source)) return;
        }
        player.setHealth(Math.max(0, player.getHealth() - amount));
        if (player.getHealth() <= 0) player.die(source);
    }

    public static void clear(ServerPlayer player) {
        State state = PLAYERS.get(player.getUUID());
        if (state != null && state.player == player) PLAYERS.remove(player.getUUID());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW);
    }

    public static void clearAll() { PLAYERS.clear(); }
}
