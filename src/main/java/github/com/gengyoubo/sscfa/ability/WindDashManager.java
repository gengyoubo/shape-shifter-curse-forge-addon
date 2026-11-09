package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.WindSpiritPerks;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 风灵「风之冲刺」主技能（sp_primary）—?四阶段状态机?
 *
 * <p>?1 次按主技能键?
 * <ul>
 *   <li><b>RISE 起飞</b>：垂直上升至 5 格高?-3 格保持较快速度（≈0.45 ?tick），
 *       3-5 格线性减速到 0。脚下灰烟（CAMPFIRE_COSY_SMOKE? 烟花上升粒子（FIREWORK）?
 *       上方有方块则顶头停在方块下方（不穿墙）?/li>
 *   <li><b>HOVER 悬浮</b>：到顶后定住 3 秒，脚下持续灰烟；同时客户端显示绿色落点指示?
 *       （准星水平方向最?16 格，半径 3 格）?/li>
 * </ul>
 * <p>悬浮中第 2 次按主技能键?
 * <ul>
 *   <li><b>DASH 冲刺</b>：直线快速冲到落点中心。起点反方向冒白粒子（CLOUD），
 *       路径白粒子拖尾。冲刺过程中不产生灰烟?/li>
 *   <li><b>LAND 落地</b>：落?3 格半径内 12 点物理伤?+ 方块视觉浮动（粒子模拟）?
 *       进入 12 ?CD?/li>
 * </ul>
 * <p>悬浮 3 秒超时未按键?b>FALL 缓慢落下</b>? ?秒），落地无伤害但进?CD?
 *
 * <p>所有判定在服务端，粒子通过 ServerWorld 广播，落点预览纯客户端各自计算?
 * 伤害 AOE 默认白名单（{@code my_addon:not_actor_whitelisted} 对应 WhitelistUtils）?
 */
public final class WindDashManager {

    // ===== 常量（均为默认值；运行时从 balance 快照读取 abilities.wind_dash?=====
    private static final float TARGET_HEIGHT = 5.0f;
    private static final double RISE_FAST_SPEED = 0.45;      // 0-3 格阶段速度（格/tick?
    private static final int RISE_FAST_TICKS = 7;            // 0-3 格约 7 tick
    private static final int RISE_SLOW_TICKS = 10;           // 3-5 格减速阶段约 10 tick
    private static final int HOVER_TICKS = 60;               // 鎮诞 3 绉?
    private static final double DASH_SPEED = 1.5;            // 冲刺速度（格/tick，极快）
    private static final double MAX_DASH_RANGE = 16.0;
    private static final double LANDING_RADIUS = 3.0;
    private static final float LANDING_DAMAGE = 12.0f;
    // CD?2 秒）由统一冷却服务 + power JSON 配置管理，原 COOLDOWN_TICKS 常量已删
    private static final double FALL_SPEED = 0.15;           // 3 鏍?绉?鈮?0.15 鏍?tick

    // 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
    private static final BalanceReader BAL = new BalanceReader("abilities.wind_dash");
    /** 统一冷却服务的稳定技?ID（网络包触发型）?*/
    private static final String SKILL_ID = "my_addon:form_ocelot_wind_spirit_wind_dash";

    // ===== 阶段 =====
    public static final int PHASE_NONE = 0;
    public static final int PHASE_RISE = 1;
    public static final int PHASE_HOVER = 2;
    public static final int PHASE_DASH = 3;
    public static final int PHASE_FALL = 4;

    private static final Map<UUID, DashState> STATES = new ConcurrentHashMap<>();

    public static final class DashState {
        boolean enhanced;
        double maxRange, landingRadius;
        float landingDamage;
        long castId = -1;
        int phase = PHASE_NONE;
        double startY;          // 起飞前脚?Y
        double targetY;         // 目标悬浮 Y（可能因顶头而低?startY+5?
        int riseTick = 0;       // 起飞阶段 tick 计数
        int hoverTick = 0;      // 悬浮已持?tick
        Vec3 dashTarget = null; // 鍐插埡钀界偣
        double dashTraveled = 0.0;
        double dashTotal = 0.0;
        Vec3 dashStart = null;
        Vec3 dashDir = null;
        boolean dashed = false; // 是否已进入过冲刺（区?冲刺落地"?超时落下落地"?
    }

    private WindDashManager() {
    }

    /** 客户端按主技能键（无 payload）。服务端根据当前阶段分支?*/
    public static void onKeyPress(ServerPlayer player) {
        if (!FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }
        DashState s = STATES.get(player.getUUID());
        if (s == null || s.phase == PHASE_NONE) {
            if (!SkillCooldowns.begin(player, SKILL_ID)) return;
            startRise(player);
            SkillCooldowns.released(player, SKILL_ID);
        } else if (s.phase == PHASE_HOVER) {
            // 悬浮中第 2 次按??冲刺
            startDash(player, s);
        }
        // RISE/DASH/FALL 期间按键忽略
    }

    /** 开始起飞?*/
    private static void startRise(ServerPlayer player) {
        DashState s = new DashState();
        s.castId = SkillCooldowns.currentCastId(player, SKILL_ID);
        s.enhanced = WindSpiritPerks.hasLongPounce(player);
        s.maxRange = WindSpiritPerks.dashRange(BAL.d("max_dash_range", MAX_DASH_RANGE), s.enhanced);
        s.landingRadius = WindSpiritPerks.landingRadius(BAL.d("landing_radius", LANDING_RADIUS), s.enhanced);
        s.landingDamage = WindSpiritPerks.landingDamage((float) BAL.d("landing_damage", LANDING_DAMAGE), s.enhanced);
        s.phase = PHASE_RISE;
        double targetHeight = BAL.d("target_height", TARGET_HEIGHT);
        s.startY = player.getY();
        s.targetY = s.startY + targetHeight;
        // 顶头检测：从脚部向?raycast（目标高 +0.5 格），碰到的方块下方即为可达最高点
        double headClear = checkVerticalClearance(player, targetHeight + 0.5);
        if (headClear < targetHeight) {
            // 上方有方块阻挡，目标降为碰顶处（方块下方?
            s.targetY = s.startY + Math.max(0.5, headClear - 0.05);
        }
        STATES.put(player.getUUID(), s);
        ServerLevel sw = (ServerLevel) player.level();
        // 起飞音效（全员可听）：用短促上升音，避免循环/长音在落地后残留 1-2 ?
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9f, 0.8f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.5f, 1.8f);
        // 同步阶段给客户端（驱动落点预?音效?
        SscAddonNetworking.syncDashState(player, s.phase, s.targetY, s.maxRange, s.landingRadius);
    }

    /** 悬浮中触发冲刺：3D 准星射线找落点（瞄近处地?近落点，瞄远?远落点，看天空退?6格）?*/
    private static void startDash(ServerPlayer player, DashState s) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        ServerLevel sw = (ServerLevel) player.level();
        // 3D 准星射线找落点（与客户端预览 computeHorizontalLanding 完全一致，保证所见即所得）
        s.dashTarget = computeDashLanding(sw, eye, look, player, s.maxRange);
        s.dashStart = new Vec3(player.getX(), player.getY(), player.getZ());
        s.dashDir = s.dashTarget.subtract(s.dashStart).normalize();
        s.dashTotal = Math.max(1.0, s.dashStart.distanceTo(s.dashTarget));
        s.dashTraveled = 0.0;
        s.phase = PHASE_DASH;
        s.dashed = true;

        // 起点反方向白粒子（向冲刺反方向喷出）
        Vec3 back = s.dashDir.scale(-1);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                25, 0.2, 0.3, 0.2, 0.02);
        // 给反方向粒子一个速度（用 spawnParticles 的速度分量近似?
        for (int i = 0; i < 15; i++) {
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                    player.getX() + back.x * 0.5, player.getY() + 1.0, player.getZ() + back.z * 0.5,
                    1, 0.1, 0.1, 0.1, 0.0);
        }
        // 冲刺音效
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6f, 1.4f);
        SscAddonNetworking.syncDashState(player, s.phase, 0, s.maxRange, s.landingRadius);
    }

    /** 每服务端 tick 对每个在线玩家调用?*/
    public static void tick(ServerPlayer player) {
        DashState s = STATES.get(player.getUUID());
        if (s == null) return;

        // 死亡 / 非风??清理并让玩家自然落下
        if (player.isDeadOrDying() || !FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }

        switch (s.phase) {
            case PHASE_RISE -> tickRise(player, s);
            case PHASE_HOVER -> tickHover(player, s);
            case PHASE_DASH -> tickDash(player, s);
            case PHASE_FALL -> tickFall(player, s);
            default -> {}
        }
    }

    private static void tickRise(ServerPlayer player, DashState s) {
        s.riseTick++;
        ServerLevel sw = (ServerLevel) player.level();

        // 计算?tick 应到达的 Y
        double curY = player.getY();
        double dy = s.targetY - curY;
        double stepY;
        if (dy <= 0) {
            // 已到?
            enterHover(player, s);
            return;
        }
        double traveledFromStart = curY - s.startY;
        double riseFastSpeed = BAL.d("rise_fast_speed", RISE_FAST_SPEED);
        if (traveledFromStart < 3.0) {
            // 0-3 格快?
            stepY = Math.min(riseFastSpeed, dy);
        } else {
            // 3-5 格线性减速：?rise_fast_speed 线性到 0
            float t = (float) Mth.clamp((traveledFromStart - 3.0) / 2.0, 0.0, 1.0);
            stepY = riseFastSpeed * (1.0f - t);
            stepY = Math.min(stepY, dy);
            if (stepY < 0.02) stepY = dy; // 收尾，避免抖?
        }

        // 应用位移（垂直，保持 x/z?
        player.setDeltaMovement(0, stepY, 0);
        player.hurtMarked = true;
        player.setPos(player.getX(), curY + stepY, player.getZ());

        // 脚下灰烟 + 烟花上升粒子
        spawnRiseParticles(sw, player);

        if (curY + stepY >= s.targetY - 0.05
                || s.riseTick > BAL.i("rise_fast_ticks", RISE_FAST_TICKS) + BAL.i("rise_slow_ticks", RISE_SLOW_TICKS) + 5) {
            enterHover(player, s);
        }
    }

    private static void enterHover(ServerPlayer player, DashState s) {
        // 精确落到 targetY
        player.setPos(player.getX(), s.targetY, player.getZ());
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        s.phase = PHASE_HOVER;
        s.hoverTick = 0;
        SscAddonNetworking.syncDashState(player, s.phase, s.targetY, s.maxRange, s.landingRadius);
    }

    private static void tickHover(ServerPlayer player, DashState s) {
        s.hoverTick++;
        ServerLevel sw = (ServerLevel) player.level();
        // 定住位置（对抗重力）
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        // 脚下持续灰烟
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() - 0.1, player.getZ(),
                3, 0.25, 0.0, 0.25, 0.005);

        if (s.hoverTick >= BAL.i("hover_ticks", HOVER_TICKS)) {
            // 超时 ?缓慢落下
            s.phase = PHASE_FALL;
            SscAddonNetworking.syncDashState(player, s.phase, 0, s.maxRange, s.landingRadius);
        }
    }

    private static void tickDash(ServerPlayer player, DashState s) {
        ServerLevel sw = (ServerLevel) player.level();
        double dashSpeed = BAL.d("dash_speed", DASH_SPEED);
        double step = Math.min(dashSpeed, s.dashTotal - s.dashTraveled);
        if (step <= 0) {
            enterLand(player, s, true);
            return;
        }
        s.dashTraveled += step;
        Vec3 next = s.dashStart.add(s.dashDir.scale(s.dashTraveled));
        // 直线冲刺中途碰撞检测：若中途遇墙，提前落地
        if (!isClearPath(sw, new Vec3(player.getX(), player.getY(), player.getZ()), next)) {
            enterLand(player, s, true);
            return;
        }
        player.setDeltaMovement(s.dashDir.x * dashSpeed, s.dashDir.y * dashSpeed, s.dashDir.z * dashSpeed);
        player.hurtMarked = true;
        player.setPos(next.x, next.y, next.z);

        // 路径白粒子拖尾（冲刺中无灰烟?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5, player.getZ(),
                4, 0.15, 0.15, 0.15, 0.0);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                player.getX(), player.getY() + 1.2, player.getZ(),
                3, 0.1, 0.1, 0.1, 0.0);

        if (s.dashTraveled >= s.dashTotal - 0.05) {
            enterLand(player, s, true);
        }
    }

    private static void tickFall(ServerPlayer player, DashState s) {
        ServerLevel sw = (ServerLevel) player.level();
        // 3 ??= 0.15 ?tick 缓慢下落
        double stepY = BAL.d("fall_speed", FALL_SPEED);
        double curY = player.getY();
        // 检测脚下方块，落地则结?
        double groundY = findGroundY(sw, player.getX(), player.getZ(), curY);
        if (curY - stepY <= groundY + 0.01) {
            // 落地（超时落下：无伤害但?CD?
            player.setPos(player.getX(), groundY, player.getZ());
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;
            // 落地小粒?
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                    10, 0.3, 0.0, 0.3, 0.02);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.5f, 1.2f);
            enterCooldown(player);
            clear(player);
            return;
        }
        player.setDeltaMovement(0, -stepY, 0);
        player.hurtMarked = true;
        player.setPos(player.getX(), curY - stepY, player.getZ());
        // 下落时脚下灰?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() - 0.1, player.getZ(),
                2, 0.2, 0.0, 0.2, 0.005);
    }

    /** 冲刺落地：造成伤害 + 方块视觉浮动 + ?CD?*/
    private static void enterLand(ServerPlayer player, DashState s, boolean withDamage) {
        ServerLevel sw = (ServerLevel) player.level();
        Vec3 land = s.dashTarget != null ? s.dashTarget : player.position();
        player.setPos(land.x, land.y, land.z);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;

        // 落地音效（全员可听）
        sw.playSound(null, land.x, land.y, land.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.4f);
        sw.playSound(null, land.x, land.y, land.z,
                SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 0.8f, 0.8f);

        if (withDamage) {
            // AOE 伤害（默认白名单?
            double landingRadius = s.landingRadius;
            AABB box = new AABB(land.subtract(landingRadius, landingRadius, landingRadius),
                    land.add(landingRadius, landingRadius, landingRadius));
            for (Entity e : sw.getEntities(player, box)) {
                if (!(e instanceof LivingEntity living)) continue;
                if (WhitelistUtils.isProtected(player, living)) continue; // 默认白名?
                // 视线检查：墙后目标不命中（仿原?PR #523 豹猫冲刺穿墙修复?
                if (!github.com.gengyoubo.sscfa.util.LineOfSightUtils.hasLineOfSight(sw, player, living)) continue;
                boolean hit = living.hurt(player.damageSources().playerAttack(player),
                        NpcCombatEnhancements.scale(player, living, s.landingDamage, "wind_primary_mul", 1.5));
                if (hit && s.enhanced) living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), player);
                Vec3 push = living.position().subtract(land);
                if (push.lengthSqr() < 1.0e-4) push = new Vec3(0, 1, 0);
                push = push.normalize();
                living.knockback(0.6, -push.x, -push.z);
            }
            // 落地爆发粒子（用快消/瞬消粒子，避免白雾长时间残留?
            sw.sendParticles(ParticleTypes.POOF, land.x, land.y + 0.2, land.z,
                    25, landingRadius * 0.5, 0.1, landingRadius * 0.5, 0.08);
            sw.sendParticles(ParticleTypes.SWEEP_ATTACK, land.x, land.y + 0.5, land.z,
                    10, landingRadius * 0.4, 0.3, landingRadius * 0.4, 0.0);
            sw.sendParticles(ParticleTypes.EXPLOSION, land.x, land.y + 0.3, land.z,
                    2, landingRadius * 0.3, 0.1, landingRadius * 0.3, 0.0);
            // 方块视觉浮动（粒子模拟）：落点半径内方块按远近冒破坏粒子
            spawnBlockFloatParticles(sw, land, landingRadius);
        }

        enterCooldown(player);
        clear(player);
    }

    /** 进入 CD?2 秒，on_end 结算?= 落地）。超时落下也?CD?*/
    private static void enterCooldown(ServerPlayer player) {
        DashState s = STATES.get(player.getUUID());
        if (s != null) SkillCooldowns.ended(player, SKILL_ID, s.castId);
    }

    /** 起飞阶段脚下粒子：灰?+ 烟花上升?*/
    private static void spawnRiseParticles(ServerLevel sw, ServerPlayer player) {
        // 灰烟（脚下）
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() - 0.1, player.getZ(),
                4, 0.3, 0.0, 0.3, 0.01);
        // 烟花同款上升粒子（FIREWORK，带向上速度?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.FIREWORK,
                player.getX(), player.getY() - 0.2, player.getZ(),
                3, 0.25, 0.0, 0.25, 0.05);
    }

    /**
     * 方块视觉浮动（粒子模拟）：落地半?3 格内方块，按距落点远近冒方块破坏粒子?
     * 近处密集、远处稀疏，呈现"被冲击波震起"的视觉。不动真实方块，避免兼容问题?
     */
    private static void spawnBlockFloatParticles(ServerLevel sw, Vec3 land, double radius) {
        BlockPos center = BlockPos.containing(land.x, land.y, land.z);
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                // 找该列地表方块（从落点高度向下找第一个实心）
                for (int dy = 0; dy >= -2; dy--) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState st = sw.getBlockState(pos);
                    if (st.isAir()) continue;
                    // 近处粒子多、远处少
                    float intensity = (float) (1.0 - dist / radius);
                    int count = Math.max(1, (int) (4 * intensity));
                    sw.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK, st),
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            count, 0.3, 0.05, 0.3, 0.1);
                    break;
                }
            }
        }
    }

    // ===== 工具方法 =====

    /** 垂直向上射线，返回从脚部到上方第一个实心方块底面的距离（无阻挡返回 maxCheck）?*/
    private static double checkVerticalClearance(ServerPlayer player, double maxCheck) {
        ServerLevel sw = (ServerLevel) player.level();
        Vec3 feet = new Vec3(player.getX(), player.getY(), player.getZ());
        Vec3 top = feet.add(0, maxCheck, 0);
        BlockHitResult bhr = sw.clip(new ClipContext(feet, top,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (bhr.getType() == HitResult.Type.MISS) return maxCheck;
        return bhr.getLocation().y - feet.y;
    }

    /**
     * 计算冲刺落点：用完整 3D 准星射线（含俯仰角），命中方块则取该处地面（水平距离≤MAX_DASH_RANGE）；
     * 打空（看天空）才退到水平方向最?MAX_DASH_RANGE 处的地面。这样玩家瞄近处地面=近落点，瞄远?远落点?
     */
    private static Vec3 computeDashLanding(ServerLevel world, Vec3 eye, Vec3 look, ServerPlayer player, double maxDashRange) {
        Vec3 flatLook = new Vec3(look.x, 0, look.z);
        double flatLen = flatLook.length();
        // 3D 射线长度上限：保证水平投影不超过 max_dash_range
        double max3D = flatLen > 1.0e-4 ? maxDashRange / flatLen : maxDashRange;
        Vec3 end3D = eye.add(look.scale(max3D));
        BlockHitResult bhr = world.clip(new ClipContext(eye, end3D,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (bhr.getType() != HitResult.Type.MISS) {
            Vec3 hit = bhr.getLocation();
            double groundY = findGroundY(world, hit.x, hit.z, Math.min(hit.y, player.getY()));
            return new Vec3(hit.x, groundY, hit.z);
        }
        // 打空：退到水平方向最?MAX_DASH_RANGE 处的地面
        Vec3 flatDir;
        if (flatLen > 1.0e-4) {
            flatDir = flatLook.normalize();
        } else {
            float yaw = player.getYRot();
            flatDir = new Vec3(-Mth.sin(yaw * 0.017453292f), 0, Mth.cos(yaw * 0.017453292f));
        }
        double hx = player.getX() + flatDir.x * maxDashRange;
        double hz = player.getZ() + flatDir.z * maxDashRange;
        double gy = findGroundY(world, hx, hz, player.getY());
        return new Vec3(hx, gy, hz);
    }

    /** 从空中水平点 (x,z) 向下找地面站立面 Y。fromY 为起始高度?*/
    private static double findGroundY(ServerLevel world, double x, double z, double fromY) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(fromY) + 1;
        for (int y = startY; y >= startY - 20 && y >= world.getMinBuildHeight(); y--) {
            mpos.set(x, y, z);
            BlockState st = world.getBlockState(mpos);
            if (!st.isAir() && !st.getCollisionShape(world, mpos).isEmpty()) {
                return y + 1; // 方块顶面
            }
        }
        return fromY; // 找不到地面，维持原高?
    }

    /** 直线路径是否畅通（无实心方块阻挡）?*/
    private static boolean isClearPath(ServerLevel world, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(dist / 0.5));
        Vec3 step = to.subtract(from).scale(1.0 / steps);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < steps; i++) {
            Vec3 p = from.add(step.scale(i));
            mpos.set(p.x, p.y, p.z);
            BlockState st = world.getBlockState(mpos);
            if (!st.isAir() && !st.getCollisionShape(world, mpos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 彻底清理状态并通知客户端?*/
    public static void clear(ServerPlayer player) {
        // 死亡/变形按实际施放阶段结算；落地路径已先 ended，此处为空操作?
        DashState s = STATES.get(player.getUUID());
        if (s != null) SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
        if (STATES.remove(player.getUUID()) != null) {
            SscAddonNetworking.syncDashState(player, PHASE_NONE, 0);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        clear(player);
    }
}
