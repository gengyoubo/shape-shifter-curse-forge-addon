package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.entity.TidalOrbEntity;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 荧光幼灵主要技能「潮汐波动」管理器（服务端状态机）?
 *
 * <p>按键语义（客户端边沿触发发网络包）：
 * <ul>
 *   <li>空闲(STATE=0) ?按键：开始蓄力（1.25s=25t），STATE=1?/li>
 *   <li>蓄力?STATE=1) ??25t：发射粒子球，STATE=2?/li>
 *   <li>球飞行中(STATE=2) ?再次按键：触发球减速（进入吸附流程），STATE 保持 2 直到球消失?/li>
 *   <li>球消??STATE=0，开?CD?s=160t）。CD 期间按键无效?/li>
 * </ul>
 * 蓄力期间可移动但减?50%；受伤不打断；被净化打断蓄力并清除球?
 * 技能不消耗任何资源（潮湿度）?
 */
public final class FluorescentTidalManager {

    /** 客户端同步状态：0=空闲?=蓄力中，2=球飞行中?*/
    public static final net.minecraft.resources.ResourceLocation TIDAL_STATE =
            new net.minecraft.resources.ResourceLocation("my_addon", "form_axolotl_fluorescent_tidal_state");

    private static final int CHARGE_TICKS = 25;       // 1.25 绉掕搫鍔?
    // CD? 秒，球消失后起算）已由统一冷却服务 + power JSON 配置管理，原 CD_TICKS 常量已删
    private static final double CHARGE_SPEED_PENALTY = -0.5;  // 蓄力期间移动 -50%

    // 阶段 5：运行时快照读取（abilities.fluorescent_tidal；快照未初始化回退默认常量?
    private static final BalanceReader BAL = new BalanceReader("abilities.fluorescent_tidal");
    /** 统一冷却服务的稳定技?ID（网络包触发型，用形?技能语?ID）?*/
    private static final String SKILL_ID = "my_addon:form_axolotl_fluorescent_tidal";

    private static final UUID CHARGE_SPEED_UUID = UUID.fromString("9d2b3c4d-5e6f-7081-92a3-b4c5d6e7f819");

    private enum State { IDLE, CHARGING, FLYING }

    private static final class Session {
        ServerPlayer owner;
        long castId = -1;
        State state = State.IDLE;
        int chargeTicks = 0;
        TidalOrbEntity orb = null;
        boolean pendingCd = false;   // 球消失后等待在主线程补设 CD（回调拿不到 player 引用?
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private FluorescentTidalManager() {
    }

    /** 客户端「按下主要技能键」时调用（由网络包触发）?*/
    public static void onKeyPress(ServerPlayer player) {
        Session s = SESSIONS.computeIfAbsent(player.getUUID(), k -> new Session());
        s.owner = player;
        if (!FormUtils.isAxolotlFluorescent(player)) {
            return;
        }
        switch (s.state) {
            case IDLE -> startCharge(player, s);
            case CHARGING -> { /* 蓄力中再按：忽略，必须等蓄力完成自动发射 */ }
            case FLYING -> triggerOrbDecel(player, s);
        }
    }

    private static void startCharge(ServerPlayer player, Session s) {
        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return;
        s.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
        s.pendingCd = false;
        s.state = State.CHARGING;
        s.chargeTicks = 0;
        PowerUtils.setResourceValueAndSync(player, TIDAL_STATE, 1);
        // 蓄力减?modifier
        applyChargeSpeed(player, true);
        ServerLevel sw = (ServerLevel) player.level();
        // 起手：多层音效（潮涌充能 + 海守诅咒 + 水花?
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, 1.0f, 0.8f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.7f, 1.4f);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.8, 0.5, 0.15);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
    }

    /** 飞行中再次按键：让球开始减速?*/
    private static void triggerOrbDecel(ServerPlayer player, Session s) {
        if (s.orb != null && s.orb.isAliveOrActive()) {
            s.orb.triggerDecelerate();
        }
    }

    /** 每服务端 tick 对每个在线玩家调用（?SscAddon tick 注册）?*/
    public static void tick(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) return;
        // 形态丢?/ 死亡 ?清理
        if (player.isDeadOrDying() || !FormUtils.isAxolotlFluorescent(player)) {
            cleanup(player, s);
            return;
        }
        // 蓄力阶段推进
        if (s.state == State.CHARGING) {
            // 被净化打?
            if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
                cancelCharge(player, s);
                return;
            }
            s.chargeTicks++;
            int chargeTicks = BAL.i("charge_ticks", CHARGE_TICKS);   // 同函数多次读 ?局部变?
            // 蓄力音效：音高随进度上升（每 5 tick?
            if (s.chargeTicks % 5 == 0) {
                ServerLevel sw = (ServerLevel) player.level();
                float prog = s.chargeTicks / (float) chargeTicks;
                sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.4f, 0.8f + prog * 0.8f);
                sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.5f, 0.8f + prog * 0.6f);
            }
            // 蓄力粒子（围绕玩家收缩的水环?
            if (s.chargeTicks % 2 == 0) {
                ServerLevel sw = (ServerLevel) player.level();
                double ang = s.chargeTicks * 0.5;
                for (int i = 0; i < 4; i++) {
                    double a = ang + i * (Math.PI / 2);
                    double r = 1.2 - (s.chargeTicks / (double) chargeTicks) * 0.8;
                    github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE,
                            player.getX() + Math.cos(a) * r, player.getY() + 1.0, player.getZ() + Math.sin(a) * r,
                            1, 0, 0.05, 0, 0.0);
                }
            }
            if (s.chargeTicks >= chargeTicks) {
                releaseOrb(player, s);
            }
        }
        // 飞行阶段：球实体自行 tick，管理器只等 onBallRemoved 回调
    }

    private static void releaseOrb(ServerPlayer player, Session s) {
        // 移除蓄力减?
        applyChargeSpeed(player, false);
        s.state = State.FLYING;
        PowerUtils.setResourceValueAndSync(player, TIDAL_STATE, 2);
        ServerLevel sw = (ServerLevel) player.level();
        // 生成粒子?
        TidalOrbEntity orb = new TidalOrbEntity(sw, player);
        sw.addFreshEntity(orb);
        s.orb = orb;
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID, s.castId);
        // 发射：清爽的水灵发射音（溺尸投掷 + 潮涌激活，两种不重叠）
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DROWNED_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.3f);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.8, 0.6, 0.3);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.25);
    }

    /** 球实体消失时回调：标?pendingCd，由 tickPendingCd 在主线程补设 CD（回调拿不到 player 引用）?*/
    public static void onBallRemoved(UUID ownerUuid, TidalOrbEntity removedOrb) {
        Session s = SESSIONS.get(ownerUuid);
        if (s == null || s.orb != removedOrb) return;
        s.state = State.IDLE;
        s.orb = null;
        s.chargeTicks = 0;
        s.pendingCd = true;
    }

    /** 取消蓄力（被净化打断）：不发射，按 fail_cooldown 结算?*/
    public static void interruptControl(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        if (s != null && s.state == State.CHARGING) cancelCharge(player, s);
    }

    private static void cancelCharge(ServerPlayer player, Session s) {
        applyChargeSpeed(player, false);
        s.state = State.IDLE;
        s.chargeTicks = 0;
        PowerUtils.setResourceValueAndSync(player, TIDAL_STATE, 0);
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(player, SKILL_ID, s.castId);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    private static void cleanup(ServerPlayer player, Session s) {
        applyChargeSpeed(player, false);
        // 蓄力中死?变形：无惩罚中止；飞行中的球被移除后?onBallRemoved 正常结算
        if (s.state == State.CHARGING) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
        if (s.orb != null && s.orb.isAliveOrActive()) {
            s.orb.discard();
        }
        s.state = State.IDLE;
        s.orb = null;
        s.chargeTicks = 0;
        PowerUtils.setResourceValueAndSync(player, TIDAL_STATE, 0);
    }

    /** 蓄力期间移动速度 -50%?*/
    private static void applyChargeSpeed(ServerPlayer player, boolean apply) {
        var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(CHARGE_SPEED_UUID);
        if (apply) {
            attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    CHARGE_SPEED_UUID, "Tidal Charge Slow", BAL.d("charge_speed_penalty", CHARGE_SPEED_PENALTY),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** ?SscAddon ?END_SERVER_TICK 中调用：为待 CD ?session 补设 CD（on_end 结算?= 球消失）?*/
    public static void tickPendingCd(Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            Session s = SESSIONS.get(p.getUUID());
            if (s == null || !s.pendingCd) continue;
            s.pendingCd = false;
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(p, SKILL_ID, s.castId);
            PowerUtils.setResourceValueAndSync(p, TIDAL_STATE, 0);
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        Session session = SESSIONS.remove(uuid);
        if (session == null) return;
        if (session.owner != null) {
            cleanup(session.owner, session);
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(session.owner, SKILL_ID, session.castId);
        } else if (session.orb != null) session.orb.discard();
    }

    public static void clearAll() {
        for (UUID uuid : java.util.List.copyOf(SESSIONS.keySet())) onPlayerDisconnect(uuid);
    }
}
