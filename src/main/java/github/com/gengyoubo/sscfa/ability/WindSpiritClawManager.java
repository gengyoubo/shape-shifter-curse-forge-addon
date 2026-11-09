package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 风灵（月髓环豹猫??- 「疾风连爪」左键连击技能（服务端状态机）??
 *
 * 按死左键 ??连续爪击，越打越快（间隔 1.0s ??3 秒??0.34s）；每次爪击对前?2.5 格范围造成横扫伤害
 * （基础 8，随时长衰减，且叠??MC 原版攻击冷却减伤），前冲一小段、扣饱食度。准星条即「耐力」，连击开始时满??
 * 随每次爪击下降；松开或打空后从当前进度慢慢回满（回满?8 秒，期间左??弱普?0%??0%）??
 * 移速随时长线性衰减（-15%/秒，封??-45%）??
 *
 * 客户端准星攻击冷却条?{@code ClawCrosshairMixin} 读取?S2C（PACKET_CLAW_STATE）同步的
 * {@code ClawClientState} 绘制。本类只做服务端判定，保证多人一致?
 */
public final class WindSpiritClawManager {

    // 以下已登记常量均为默认值；运行时从 balance 快照读取（abilities.wind_spirit_claw??
    private static final float BASE_DAMAGE = 8.0f;
    private static final double RADIUS = 2.5;
    private static final double REACH = 2.0;
    private static final int MAX_INTERVAL = 20;   // 1.0s
    private static final int MIN_INTERVAL = 7;    // 閳?.34s
    private static final int RAMP_TICKS = 60;     // 3 秒达最?
    private static final int MAX_CLAW_TICKS = 200; // 连击最?10 秒兜?
    private static final int RECOVER_TICKS = 160;  // ??0 回??8 ??
    private static final int RECOVER_TICKS_NECKLACE = 110; // 戴风灵专属项链：耐力回满??5.5 ??
    private static final float DMG_DECAY_PER_SEC = 0.12f;
    private static final float DMG_DECAY_MAX = 0.36f;
    private static final float SPD_DECAY_PER_SEC = 0.15f;
    private static final float SPD_DECAY_MAX = 0.45f;
    private static final float EXHAUSTION_PER_HIT = 0.64f; // ??.16 饱食度点
    private static final float FORWARD_LUNGE = 0.28f;
    private static final float PROGRESS_PER_HIT = 1.0f / 25.0f; // 最快频率下??10 秒打?

    private static final UUID SPEED_MOD_UUID = UUID.fromString("6a1d3c9e-7b2f-4c8a-9e1d-0f5a2c7b4d33");
    private static final String SPEED_MOD_NAME = "wind_spirit_claw_slow";

    public static final int PHASE_IDLE = 0;
    public static final int PHASE_CLAW = 1;
    public static final int PHASE_OVERHEAT = 2;

    private static final Map<UUID, ClawState> STATES = new ConcurrentHashMap<>();

    // 副技能：+50% 徒??形态伤??buff（CD 2 秒??power JSON 配置，原 SECONDARY_CD_TICKS 常量已删??
    private static final int BUFF_DURATION = 10;      // 0.5 缁?
    private static final float BUFF_MULT = 1.5f;
    private static final Map<UUID, Integer> BUFF_TICKS = new ConcurrentHashMap<>();

    // 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
    private static final BalanceReader BAL = new BalanceReader("abilities.wind_spirit_claw");
    /** 统一冷却服务的稳定技?ID（网络包触发型）??*/
    private static final String SKILL_ID = "my_addon:form_ocelot_wind_spirit_claw_buff";

    public static final class ClawState {
        boolean holding = false;
        int phase = PHASE_IDLE;
        int holdTicks = 0;
        int sinceLastAttack = 0;
        float progress = 1.0f;
        float recovery = 0.0f;
        int sentPhase = -1;
        float sentProgress = Float.NaN;
        float sentStep = Float.NaN;
        long sentAt = Long.MIN_VALUE;
    }

    private WindSpiritClawManager() {
    }

    /** 客户端上报左键按住状态?*/
    public static void setHolding(ServerPlayer player, boolean holding) {
        if (!FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }
        ClawState s = STATES.computeIfAbsent(player.getUUID(), k -> new ClawState());
        s.holding = holding;
        if (holding && s.phase == PHASE_IDLE) {
            s.phase = PHASE_CLAW;
            s.holdTicks = 0;
            s.sinceLastAttack = BAL.i("min_interval", MIN_INTERVAL); // 按下稍缓冲，避免按下即命??
        }
    }

    /** 每服务端 tick 对每个在线玩家调用?*/
    public static void tick(ServerPlayer player) {
        // 副技?buff 计时（独立于连击状态??
        Integer buff = BUFF_TICKS.get(player.getUUID());
        if (buff != null) {
            if (buff <= 1 || !FormUtils.isOcelotSP(player)) BUFF_TICKS.remove(player.getUUID());
            else BUFF_TICKS.put(player.getUUID(), buff - 1);
        }

        ClawState s = STATES.get(player.getUUID());
        if (s == null) return;

        if (player.isDeadOrDying() || !FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }

        switch (s.phase) {
            case PHASE_CLAW -> tickClaw(player, s);
            case PHASE_OVERHEAT -> tickOverheat(player, s);
            default -> {
            }
        }

        // 爪击扣耐力按事件同步；过热条按服务端回复速度本地推进，每秒校准??
        if (STATES.containsKey(player.getUUID())) {
            float progress = crosshairProgress(s);
            float step = s.phase == PHASE_OVERHEAT ? recoveryStep(player) : 0;
            long now = player.level().getGameTime();
            boolean legacy = !github.com.gengyoubo.sscfa.network.AddonNetwork.canSend(player,
                    github.com.gengyoubo.sscfa.network.CountdownSync.STATE);
            if (legacy || s.sentPhase != s.phase || s.sentStep != step
                    || s.phase == PHASE_CLAW && s.sentProgress != progress || now - s.sentAt >= 20) {
                SscAddonNetworking.syncClawState(player, s.phase, progress, step);
                s.sentPhase = s.phase; s.sentProgress = progress; s.sentStep = step; s.sentAt = now;
            }
        }
    }

    private static void tickClaw(ServerPlayer player, ClawState s) {
        s.holdTicks++;
        applySpeedSlow(player, slowFactor(s.holdTicks));

        if (!s.holding) {
            enterOverheat(s);
            removeSpeedSlow(player);
            return;
        }

        s.sinceLastAttack++;
        int interval = currentInterval(s.holdTicks);
        if (s.sinceLastAttack >= interval) {
            performClawAttack(player, s);
            s.sinceLastAttack = 0;
            s.progress -= PROGRESS_PER_HIT;
            player.resetAttackStrengthTicker(); // 让原版减??准星充能反映爪击节??
        }

        if (s.progress <= 0.0f || s.holdTicks >= BAL.i("max_claw_ticks", MAX_CLAW_TICKS)) {
            s.progress = 0.0f;
            enterOverheat(s);
            removeSpeedSlow(player);
        }
    }

    private static void tickOverheat(ServerPlayer player, ClawState s) {
        removeSpeedSlow(player);

        // 停手后立即从当前进度慢慢回满（无延迟）；期间左键 = 弱普攻（getRecoveryMultiplier 缩??0??0%）??
        // 戴风灵专属项链「御风爪铃」时耐力回复更快?s ??5.5s）??
        s.recovery += recoveryStep(player);
        if (s.recovery >= 1.0f) {
            // 回满：仍按????无缝重启连击；否则结束、准星条消失
            if (s.holding) {
                s.phase = PHASE_CLAW;
                s.holdTicks = 0;
                s.sinceLastAttack = BAL.i("min_interval", MIN_INTERVAL);
                s.progress = 1.0f;
                s.recovery = 0.0f;
            } else {
                clear(player);
            }
        }
    }

    private static void enterOverheat(ClawState s) {
        s.phase = PHASE_OVERHEAT;
        s.recovery = Mth.clamp(s.progress, 0.0f, 1.0f); // 从当前剩余进度开始回满（打得越久剩越少、回越久??
    }

    private static float recoveryStep(ServerPlayer player) {
        int recoverTicks = TrinketUtils.isWearing(player,
                github.com.gengyoubo.sscfa.init.AddonItems.WIND_SPIRIT_STAMINA_NECKLACE.get())
                ? BAL.i("recover_ticks_necklace", RECOVER_TICKS_NECKLACE)
                : BAL.i("recover_ticks", RECOVER_TICKS);
        return 1.0f / recoverTicks;
    }

    private static void performClawAttack(ServerPlayer player, ClawState s) {
        ServerLevel sw = (ServerLevel) player.level();

        Vec3 look = player.getViewVector(1.0f);
        double randSide = (player.getRandom().nextDouble() - 0.5) * 1.2;
        double randUp = (player.getRandom().nextDouble() - 0.5) * 0.6;
        Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
        double reach = BAL.d("reach", REACH);
        Vec3 center = player.getEyePosition()
                .add(look.scale(reach))
                .add(side.scale(randSide))
                .add(0, randUp, 0);

        // MC 原版攻击冷却减伤系数（真实充能进度）× 爪击时长衰??
        float g = player.getAttackStrengthScale(0.5f);
        float vanillaFactor = 0.2f + g * g * 0.8f;
        float clawFactor = 1.0f - Math.min((float) BAL.d("dmg_decay_max", DMG_DECAY_MAX),
                (float) BAL.d("dmg_decay_per_sec", DMG_DECAY_PER_SEC) * (s.holdTicks / 20.0f));
        float dmg = (float) BAL.d("base_damage", BASE_DAMAGE) * vanillaFactor * clawFactor;

        double radius = BAL.d("radius", RADIUS);
        AABB box = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
        for (Entity e : sw.getEntities(player, box)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (WhitelistUtils.isProtected(player, living)) continue; // 默认白名??
            // 视线检查：墙后目标不命中（仿原?PR #523 豹猫冲刺穿墙修复??
            if (!github.com.gengyoubo.sscfa.util.LineOfSightUtils.hasLineOfSight(sw, player, living)) continue;
            living.hurt(player.damageSources().playerAttack(player), dmg);
            Vec3 push = living.position().subtract(player.position());
            if (push.lengthSqr() < 1.0e-4) push = look;
            push = push.normalize();
            living.knockback(0.35, -push.x, -push.z);
        }

        sw.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0, 0, 0, 0);

        Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale((float) BAL.d("forward_lunge", FORWARD_LUNGE));
        player.push(flat.x, 0.0, flat.z);
        player.hurtMarked = true;

        player.causeFoodExhaustion((float) BAL.d("exhaustion_per_hit", EXHAUSTION_PER_HIT));

        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    private static float slowFactor(int holdTicks) {
        return Math.min((float) BAL.d("spd_decay_max", SPD_DECAY_MAX),
                (float) BAL.d("spd_decay_per_sec", SPD_DECAY_PER_SEC) * (holdTicks / 20.0f));
    }

    private static void applySpeedSlow(ServerPlayer player, float factor) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        double target = -factor; // MULTIPLY_TOTAL??0.15 = 移速?.85
        AttributeModifier existing = attr.getModifier(SPEED_MOD_UUID);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - target) < 1.0e-4) return;
            attr.removeModifier(SPEED_MOD_UUID);
        }
        attr.addTransientModifier(new AttributeModifier(
                SPEED_MOD_UUID, SPEED_MOD_NAME, target,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeSpeedSlow(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(SPEED_MOD_UUID) != null) {
            attr.removeModifier(SPEED_MOD_UUID);
        }
    }

    /** 按住时长 ??当前攻击间隔（tick）??0 ??3 秒??7??*/
    private static int currentInterval(int holdTicks) {
        int rampTicks = BAL.i("ramp_ticks", RAMP_TICKS);
        float t = Math.min(holdTicks, rampTicks) / (float) rampTicks;
        int maxInterval = BAL.i("max_interval", MAX_INTERVAL);
        int minInterval = BAL.i("min_interval", MIN_INTERVAL);
        return Math.round(maxInterval - (maxInterval - minInterval) * t);
    }

    /** 准星?耐力：爪击??剩余进度(随每次爪击下?；过热期=从当前进度回?0??)；空?1??*/
    public static float crosshairProgress(ClawState s) {
        return switch (s.phase) {
            case PHASE_CLAW -> Mth.clamp(s.progress, 0.0f, 1.0f);
            case PHASE_OVERHEAT -> Mth.clamp(s.recovery, 0.0f, 1.0f);
            default -> 1.0f;
        };
    }

    /** 徒??非武器近战伤害倍??= 过热回复缩??0-0.9) ??副技?buff(1.5)。??ClawDamageBoostMixin 用??*/
    public static float getNormalMeleeMultiplier(ServerPlayer player) {
        return getNormalMeleeMultiplier(player, null);
    }

    public static float getNormalMeleeMultiplier(ServerPlayer player, LivingEntity target) {
        float recovery = 1.0f;
        ClawState s = STATES.get(player.getUUID());
        if (s != null && s.phase == PHASE_OVERHEAT) {
            recovery = s.recovery * 0.9f; // 回复进度 0?? 映射伤害 0??0%
        }
        float buff = BUFF_TICKS.containsKey(player.getUUID())
                ? (float) BAL.d("buff_mult", BUFF_MULT) : 1.0f;
        if (BUFF_TICKS.containsKey(player.getUUID()) && target != null && NpcCombatEnhancements.isEnemyNpc(player, target))
            buff *= (float) NpcCombatEnhancements.value("wind_secondary_mul", 2.0);
        return recovery * buff;
    }

    /** 是否手持武器（与主??is_weapon 一致：主手攻击力加?&gt; 1）。拿武器时不吃疾风连??副技能加伤（火把/食??方块等非武器仍吃）??*/
    public static boolean isHoldingWeapon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        double totalAdd = 0.0;
        for (AttributeModifier m : stack
                .getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) {
                totalAdd += m.getAmount();
            }
        }
        return totalAdd > 1.0;
    }

    /** 副技能（sp_secondary）??.5 秒内徒手/形态伤??+50%，cd 2 秒??*/
    public static void activateSecondaryBuff(ServerPlayer player) {
        if (!FormUtils.isOcelotSP(player)) return;
        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return; // CD 娑?
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);
        BUFF_TICKS.put(player.getUUID(), BAL.i("buff_duration", BUFF_DURATION));
        ServerLevel sw = (ServerLevel) player.level();
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 1.6f);
        // owner 打标：仅本人第一人称避让
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
    }

    /** 彻底清理：移除状?+ 移速修饰符，并通知客户端准星条消失??*/
    public static void clear(ServerPlayer player) {
        removeSpeedSlow(player);
        if (STATES.remove(player.getUUID()) != null) {
            SscAddonNetworking.syncClawState(player, PHASE_IDLE, 1.0f);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        removeSpeedSlow(player);
        STATES.remove(player.getUUID());
        BUFF_TICKS.remove(player.getUUID());
    }
}
