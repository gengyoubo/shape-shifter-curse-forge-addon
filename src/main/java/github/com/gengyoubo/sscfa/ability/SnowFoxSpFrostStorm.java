package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.FrostStormEntity;
import github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP雪狐远程次要技??- 冰风?
 * 1.5秒蓄力后在准星位置释放冰风??
 * 蓄力期间减??0%准星移动速??
 */
public class SnowFoxSpFrostStorm {

    private SnowFoxSpFrostStorm() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }
    
    private static final ConcurrentHashMap<UUID, ChargingData> CHARGING_PLAYERS = new ConcurrentHashMap<>();
    
    private static final int CHARGE_TICKS = 30; // 默??1.5秒蓄力；balance 可覆盖（abilities.snow_fox_sp_frost_storm??
    private static final double MAX_RANGE = 30.0; // 默认最大释放距??
    private static final int MANA_COST = 30; // 默认霜寒值消?
    private static final int REGEN_LOCK_TICKS = 100; // 默??5秒回能锁；balance regen_lock_ticks 可覆?

    // 阶??5：服务端权威快照读??
    private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
            new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.snow_fox_sp_frost_storm");
    //未使? private static final int COOLDOWN = 600;  30秒CD = 600tick（CD 已回?power JSON 原生管理??
    
    private static final ResourceLocation RESOURCE_ID = new ResourceLocation("my_addon", "form_snow_fox_sp_resource");
    public static final String SKILL_ID = "my_addon:form_snow_fox_sp_ranged_secondary";
    private static final ResourceLocation REGEN_COOLDOWN_ID = new ResourceLocation("my_addon", "form_snow_fox_sp_frost_regen_cooldown_resource");
    
    /**
     * 开始蓄力（点按技能键时调用）
     */
    public static boolean startCharging(ServerPlayer player) {
        // 检查是否已经在蓄??
        if (CHARGING_PLAYERS.containsKey(player.getUUID())) {
            FailAwareActiveSelfPower.markFail();
            return false;
        }

        // 检查霜寒??
        int currentMana = getResourceValue(player);
        if (currentMana < BAL.i("mana_cost", MANA_COST)) {
            player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
            FailAwareActiveSelfPower.markFail();
            return false;
        }

        // 消耗霜寒值（在蓄力开始时就消耗）
        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
        changeResourceValue(player, -BAL.i("mana_cost", MANA_COST));
        // 设置回复冷却（默??秒；balance regen_lock_ticks 可覆盖）
        setRegenCooldown(player, BAL.i("regen_lock_ticks", REGEN_LOCK_TICKS));

        CHARGING_PLAYERS.put(player.getUUID(), new ChargingData(0, github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID)));
        
        // 播放蓄力开始音?
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
        
        return true;
    }
    
    /**
     * 取消蓄力（被净化时调用?
     */
    public static void cancelCharging(ServerPlayer player) {
        ChargingData data = CHARGING_PLAYERS.remove(player.getUUID());
        if (data != null) {
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(player, SKILL_ID, data.castId);
            // 播放打断音??
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }

    /**
     * 玩家断线时清理所有状态，防止内存泄??
     */
    public static void clearPlayer(java.util.UUID uuid) {
        CHARGING_PLAYERS.remove(uuid);
    }

    /**
     * Cancel pending charges on reload. Spawned storms own their lifetime/NBT;
     * active cooldowns must not be reset by a data-pack reload.
     */
    public static void clearAll() {
        CHARGING_PLAYERS.clear();
    }

    /** A new server has a new tick clock; discard states left by the previous world. */
    public static void resetForServerStart() {
        clearAll();
    }
    
    /**
     * 每tick更新蓄力状??
     */
    public static void tick(ServerPlayer player) {
        ChargingData data = CHARGING_PLAYERS.get(player.getUUID());
        if (data == null) return;
        if (!player.isAlive()) {
            cancelCharging(player);
            return;
        }
        
        // 检查是否被净??- 如果有purified效果则取消蓄?
        if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
            cancelCharging(player);
            return;
        }
        
        data.chargeTicks++;
        
        // 生成蓄力粒子效果
        if (player.level() instanceof ServerLevel serverWorld) {
            Vec3 pos = player.position();
            double angle = (data.chargeTicks * 0.3) % (Math.PI * 2);
            double radius = 0.8;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            // 充能态每 tick 单发：只广??64 格（?512 ??ParticleUtils.spawnParticles），减少远处玩家高频网络包；充能主体特效不受影响
            github.com.gengyoubo.sscfa.network.DecorationParticles.forced(serverWorld, player,
                    ParticleTypes.SNOWFLAKE, x, pos.y + 1, z, 1, 0, 0.1, 0, 0, 64);
        }
        
        // 蓄力完??
        if (data.chargeTicks >= BAL.i("charge_ticks", CHARGE_TICKS)) {
            releaseStorm(player, data.castId);
            CHARGING_PLAYERS.remove(player.getUUID());
        }
    }
    
    /**
     * 释放冰风??
     */
    private static void releaseStorm(ServerPlayer player, long castId) {
        if (castId < 0 || github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != castId) return;
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID, castId);

        // 计算准星位置（射线检测）
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = start.add(look.scale(BAL.d("max_range", MAX_RANGE)));
        
        BlockHitResult hitResult = player.level().clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        
        Vec3 targetPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            targetPos = hitResult.getLocation();
        } else {
            targetPos = end;
        }
        
        // 创建冰风暴实体（绑定本次 cast：旧风暴/重登后不误伤新施放）
        FrostStormEntity storm = new FrostStormEntity(
            player.level(),
            targetPos.x, targetPos.y, targetPos.z,
            player
        );
        storm.bindCast(castId);
        player.level().addFreshEntity(storm);
        
        // 播放释放音??
        player.level().playSound(null, targetPos.x, targetPos.y, targetPos.z,
            SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0f, 0.8f);
        
        // 生成释放粒??
        if (player.level() instanceof ServerLevel serverWorld) {
            ParticleUtils.spawnParticles(serverWorld, ParticleTypes.CLOUD,
                targetPos.x, targetPos.y + 1, targetPos.z,
                30, 1.5, 1.0, 1.5, 0.05);
        }
    }

    /**
     * 获取霜寒??
     */
    private static int getResourceValue(ServerPlayer player) {
        return PowerUtils.getResourceValue(player, RESOURCE_ID);
    }

    /**
     * 修改霜寒??
     */
    private static void changeResourceValue(ServerPlayer player, int change) {
        PowerUtils.changeResourceValueAndSync(player, RESOURCE_ID, change);
    }

    /**
     * 设置回复冷却（使用??秒内无法自然回复霜寒值）
     */
    private static void setRegenCooldown(ServerPlayer player, int value) {
        PowerUtils.setResourceValueAndSync(player, REGEN_COOLDOWN_ID, value);
    }

    /**
     * 蓄力数据
     */
    private static class ChargingData {
        int chargeTicks;
        final long castId;

        ChargingData(int chargeTicks, long castId) {
            this.castId = castId;
            this.chargeTicks = chargeTicks;
        }
    }
}
