package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkManager;
import github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionManager;
import github.com.gengyoubo.sscfa.ability.ParasiticCombatTracker;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.entity.ParasiticSeedProjectile;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.GlowMarker;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 寄生果蝠主技能 {@code my_addon:parasitic_fruit_seed}：灵果寄生。
 *
 * <p>按键抛出灵果种子投掷物；命中生物后在目标身上种下种子，种子根据宿主的友敌关系与状态周期性结出
 * 自适应果实。全部判定与状态效果在服务端执行。冷却字段同统一技能（施放即起算，装备双生种荚用
 * {@code extra_cooldowns.twin_pod}）。</p>
 *
 * <p>每个施法者的种子表按 UUID 存在本类里，投掷物命中、种子圈拾取通过静态入口 {@link #plantSeedSpread} 回调；
 * 形态切换、离线时由 {@link #onRemoved} 清理。</p>
 */
public final class ParasiticFruitSeedPower implements SscPowerType {
    public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "parasitic_fruit_seed");

    // 以下均为默认值；运行时从 balance 快照读取（abilities.parasitic_seed_power）。
    private static final int MAX_SEEDS = 3;
    private static final int ROOTING_TICKS = 20;
    private static final int FRUIT_INTERVAL_TICKS = 25;
    private static final int ENERGY_COST = 1;

    /** balance 快照读取（快照未初始化回退默认常量） */
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_power");

    private static final DustParticleOptions FRIEND_DUST = new DustParticleOptions(new Vector3f(0.35f, 0.95f, 0.30f), 1.1f);
    private static final DustParticleOptions ENEMY_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.10f, 0.75f), 1.1f);
    private static final DustParticleOptions SEED_DUST = new DustParticleOptions(new Vector3f(0.95f, 0.72f, 0.24f), 1.0f);

    /** 一个施法者的种子表：宿主 UUID -> 种子，按种下顺序排列（超上限时移除最早的）。 */
    private static final class Holder {
        final LinkedHashMap<UUID, SeedData> seeds = new LinkedHashMap<>();
        /** 腐殖之戒：当前结果的时长系数（bearFruit 临时设置，scaleDuration 读取，结束复位 1.0） */
        float currentHumusFactor = 1.0f;
    }

    /** 当前拥有本能力的施法者。 */
    private static final Map<UUID, Holder> HOLDERS = new ConcurrentHashMap<>();

    /**
     * 全局表：当前被「敌方削弱果」寄生的宿主 UUID -> 失效世界 tick。
     * 由所有果蝠玩家的 tick 实时维护（宿主当前判定为敌方时写入，种子失效/转友方时清理）。
     * 供 LivingEntity.heal 拦截查询，实现「被削弱果寄生的敌人回血 -50%」被动。
     * 用绝对 endTick 兜底：即使某果蝠玩家下线导致 tick 停摆，过期后查询也会判定失效。
     */
    private static final ConcurrentHashMap<UUID, Long> ENEMY_PARASITIZED_HOSTS = new ConcurrentHashMap<>();

    /** 查询某生物当前是否被任意果蝠的敌方削弱果寄生；需传入世界当前 tick 以判定是否已过期。 */
    public static boolean isParasitizedByEnemyFruit(UUID hostUuid, long worldTime) {
        Long end = ENEMY_PARASITIZED_HOSTS.get(hostUuid);
        if (end == null) return false;
        if (worldTime >= end) {
            ENEMY_PARASITIZED_HOSTS.remove(hostUuid);
            return false;
        }
        return true;
    }

    // ===== 按键 =====

    @Override
    public void onKeyPressed(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        if (!BalanceIntegration.isPlayerReady(caster)) return;
        if (!FormPowerRuntime.test(caster, caster, data.getAsJsonObject("condition"))) return;
        if (caster.hasEffect(AddonEffects.PURIFIED.get())) return;
        String id = powerId.toString();
        if (!SkillCooldowns.ready(caster, id)) return;

        // 双生种荚：一次播种额外寄生最近的第二目标，但能量消耗翻倍、冷却 +1 秒
        boolean twinPod = TrinketUtils.isWearing(caster, AddonItems.TWIN_POD.get());
        int energyCostBase = BAL.i("energy_cost", ENERGY_COST);
        int energyCost = twinPod ? energyCostBase * 2 : energyCostBase;

        // 能量检查：不足则释放失败
        if (!PowerUtils.hasResource(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, energyCost)) {
            caster.serverLevel().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.7f);
            return;
        }

        // 抛物线投掷：命中生物由投掷物回调 plantSeedSpread；未命中落地后生成种子圈。
        PowerUtils.changeResourceValueAndSync(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, -energyCost);
        HOLDERS.computeIfAbsent(caster.getUUID(), ignored -> new Holder());
        launchSeedProjectile(caster, twinPod);
        SkillCooldowns.force(caster, id, twinPod ? SkillCooldowns.extra(caster, id, "twin_pod") : SkillCooldowns.cooldown(caster, id));
    }

    /** 发射一颗灵果种子投掷物（抛物线，速度同孢子炸弹）。 */
    private static void launchSeedProjectile(ServerPlayer caster, boolean twinPod) {
        ParasiticSeedProjectile seed = new ParasiticSeedProjectile(caster.level(), caster);
        seed.setItem(Items.WHEAT_SEEDS.getDefaultInstance());
        seed.setOwner(caster);
        seed.setTwinPod(twinPod);
        seed.setPosRaw(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        seed.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0f, 1.4f, 0.4f);
        Vec3 ownerVel = caster.getDeltaMovement();
        seed.setDeltaMovement(seed.getDeltaMovement().add(ownerVel.x, caster.onGround() ? 0.0 : ownerVel.y, ownerVel.z));
        caster.level().addFreshEntity(seed);
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    // ===== tick =====

    @Override
    public void tick(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        Holder holder = HOLDERS.computeIfAbsent(caster.getUUID(), ignored -> new Holder());
        if (holder.seeds.isEmpty()) return;

        long now = caster.level().getGameTime();
        Iterator<Map.Entry<UUID, SeedData>> iterator = holder.seeds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SeedData> entry = iterator.next();
            SeedData seed = entry.getValue();
            LivingEntity host = findLiving(caster.getServer(), entry.getKey());
            // 跨维度宿主视为脱离作用范围，主动失效
            if (host == null || !host.isAlive() || now >= seed.endTick || host.level() != caster.level()) {
                if (host != null) GlowMarker.unmark(host);
                ENEMY_PARASITIZED_HOSTS.remove(entry.getKey());
                iterator.remove();
                continue;
            }
            // 维护全局敌方寄生表：宿主当前为敌方（非友方且非白名单保护）时登记其失效 tick；转为友方/受保护则立即解除。
            if (!WhitelistUtils.isBuffTarget(caster, host) && !WhitelistUtils.isProtected(caster, host)) {
                ENEMY_PARASITIZED_HOSTS.put(entry.getKey(), seed.endTick);
            } else {
                ENEMY_PARASITIZED_HOSTS.remove(entry.getKey());
            }
            DomainManager.runAttachedEffect(caster, host, () -> spawnAttachedSeedParticles(caster, host, seed, now));
            if (now >= seed.nextFruitTick) {
                seed.nextFruitTick = now + BAL.i("fruit_interval_ticks", FRUIT_INTERVAL_TICKS);
                DomainManager.runAttachedEffect(caster, host, () -> bearFruit(holder, caster, host, seed));
            }
        }
    }

    // ===== 移除 =====

    /** 形态切换、离线：清理 outline 高光与敌方寄生登记，避免残留导致目标永久 -50% 回血。 */
    @Override
    public void onRemoved(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        Holder holder = HOLDERS.remove(caster.getUUID());
        if (holder == null) return;
        for (UUID uuid : holder.seeds.keySet()) {
            LivingEntity host = findLiving(caster.getServer(), uuid);
            if (host != null) GlowMarker.unmark(host);
            ENEMY_PARASITIZED_HOSTS.remove(uuid);
        }
        holder.seeds.clear();
    }

    // ===== 种植 =====

    /** 在宿主身上种下灵果种子：种子寿命未交战 15 秒 / 交战 5 秒（balance 可调 life_peace / life_combat）。 */
    private static void plantSeed(Holder holder, ServerPlayer caster, LivingEntity host) {
        plantSeed(holder, caster, host,
                ParasiticCombatTracker.isInCombat(host) ? BAL.i("life_combat", 100) : BAL.i("life_peace", 300));
    }

    /** 带自定义基础时长的种植（种子圈拾取传固定时长）。 */
    private static void plantSeed(Holder holder, ServerPlayer caster, LivingEntity host, int baseLife) {
        if (!isValidSeedHost(caster, host)) return;
        long now = caster.level().getGameTime();
        int maxSeeds = BAL.i("max_seeds", MAX_SEEDS);
        int rootingTicks = BAL.i("rooting_ticks", ROOTING_TICKS);
        LinkedHashMap<UUID, SeedData> seeds = holder.seeds;
        SeedData seed = seeds.get(host.getUUID());
        if (seed != null) {
            // 同一宿主：堆叠 1 层（封顶 max_seeds，默认 3）；重复命中在剩余时长基础上叠加（无论敌友）
            seed.stack = Math.min(maxSeeds, seed.stack + 1);
            seed.endTick = Math.max(seed.endTick, now) + baseLife;
            seed.nextFruitTick = now + rootingTicks;
        } else {
            cleanupExpiredSeeds(holder, caster, now);
            // 超过同时宿主上限时移除最早一个（FIFO）。max_seeds 同时作为「同一宿主堆叠上限」与「独立宿主上限」。
            if (seeds.size() >= maxSeeds) {
                Iterator<UUID> iterator = seeds.keySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
            seed = new SeedData(now + baseLife, now + rootingTicks);
            seeds.put(host.getUUID(), seed);
        }

        if (host.level() instanceof ServerLevel world) {
            ParticleUtils.spawnParticles(world, SEED_DUST,
                    host.getX(), host.getY() + host.getBbHeight() * 0.65, host.getZ(),
                    18 + seed.stack * 6, 0.25, 0.35, 0.25, 0.02);
            // 叮声仅施法者本人听见（不广播给其他玩家）
            MancianimaMarkManager.playSoundToPlayer(caster, SoundEvents.GRASS_PLACE, 0.9f, 1.5f);
        }
    }

    /** 静态回调入口：施法者当前拥有灵果寄生时在 host 身上种下种子（投掷物命中、种子圈拾取用）。 */
    public static void plantSeedFrom(ServerPlayer caster, LivingEntity host) {
        if (caster == null || host == null || !host.isAlive()) return;
        Holder holder = HOLDERS.get(caster.getUUID());
        if (holder != null) plantSeed(holder, caster, host);
    }

    /** 带自定义时长的静态回调（种子圈拾取用，时长 = 剩余）。 */
    public static void plantSeedFrom(ServerPlayer caster, LivingEntity host, int baseLife) {
        if (caster == null || host == null || !host.isAlive()) return;
        Holder holder = HOLDERS.get(caster.getUUID());
        if (holder != null) plantSeed(holder, caster, host, baseLife);
    }

    /**
     * 双生种荚扩散：命中宿主后，向其 4 格内额外寄生最近 1 个目标；
     * 若附近无其它可寄生目标，则给命中宿主叠 2 层。非双生种荚时退化为普通单层寄生。
     */
    public static void plantSeedSpread(ServerPlayer caster, LivingEntity host, boolean twinPod) {
        if (!isValidSeedHost(caster, host)) return;
        if (!twinPod) {
            plantSeedFrom(caster, host);
            return;
        }
        LivingEntity extra = findNearbyOther(caster, host);
        plantSeedFrom(caster, host);
        // 附近无额外目标：给命中宿主叠 2 层
        plantSeedFrom(caster, extra != null ? extra : host);
    }

    /** 在 primary 周围 4 格内查找最近的另一个可寄生生物（排除施法者与 primary 本身）。 */
    private static LivingEntity findNearbyOther(ServerPlayer caster, LivingEntity primary) {
        AABB box = primary.getBoundingBox().inflate(4.0D);
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity e : caster.level().getEntitiesOfClass(LivingEntity.class, box,
                living -> living != caster && living != primary && isValidSeedHost(caster, living))) {
            double d = e.distanceToSqr(primary);
            if (d <= 16.0 && d < bestDistSq) {
                bestDistSq = d;
                best = e;
            }
        }
        return best;
    }

    /** 盔甲架（视觉标记）、旁观者、其它世界或被领域隔开的目标不能消耗种子。 */
    public static boolean isValidSeedHost(ServerPlayer caster, LivingEntity host) {
        return caster != null && host != null && host.isAlive() && !host.isSpectator()
                && !(host instanceof ArmorStand)
                && caster.level() == host.level()
                && !DomainManager.blocksTargeting(caster, host);
    }

    private static void cleanupExpiredSeeds(Holder holder, ServerPlayer caster, long now) {
        holder.seeds.entrySet().removeIf(entry -> {
            LivingEntity host = findLiving(caster.getServer(), entry.getKey());
            return host == null || !host.isAlive() || now >= entry.getValue().endTick;
        });
    }

    // ===== 结果 =====

    private static void bearFruit(Holder holder, ServerPlayer caster, LivingEntity host, SeedData seed) {
        boolean friend = WhitelistUtils.isBuffTarget(caster, host);
        // 腐殖之戒：装备时敌方削弱果时长 ×1.5、友方增益果时长 ×0.7；未装备 ×1.0。
        boolean humus = TrinketUtils.isWearing(caster, AddonItems.HUMUS_RING.get());
        if (friend) {
            holder.currentHumusFactor = humus ? 0.7f : 1.0f;
            applyFriendBuff(holder, host, caster);
            spawnFruitParticles(host, FRIEND_DUST, seed.stack);
        } else if (!WhitelistUtils.isProtected(caster, host)) {
            holder.currentHumusFactor = humus ? 1.5f : 1.0f;
            applyEnemyFruit(holder, host, selectEnemyFruit(host), seed.stack, caster);
            spawnFruitParticles(host, ENEMY_DUST, seed.stack);
        }
        holder.currentHumusFactor = 1.0f;
    }

    private static EnemyFruit selectEnemyFruit(LivingEntity host) {
        if (host.isSprinting() || host.getDeltaMovement().horizontalDistanceSqr() > 0.08) return EnemyFruit.VINE;
        if (host.swinging || (host instanceof Mob mob && mob.getTarget() != null)) return EnemyFruit.BITTER;
        if (host.getHealth() / host.getMaxHealth() > 0.7f || host.getArmorValue() >= 10) return EnemyFruit.ROTTEN;
        return EnemyFruit.SOUR;
    }

    /** 友方果实效果：交战/未交战 × 血量分级。 */
    private static void applyFriendBuff(Holder holder, LivingEntity target, ServerPlayer caster) {
        boolean inCombat = ParasiticCombatTracker.isInCombat(target);
        float hpRatio = target.getHealth() / Math.max(1.0f, target.getMaxHealth());
        float humus = holder.currentHumusFactor;
        // buff 时长 = 叮声间隔 + 0.2s，再乘腐殖之戒系数（装备时友军增益 ×0.7）
        int buffDur = Math.max(10, Math.round((BAL.i("fruit_interval_ticks", FRUIT_INTERVAL_TICKS) + 4) * humus));
        if (!inCombat) {
            // 未交战：高血急迫 I，低血回血 1 心
            if (hpRatio > 0.7f) addEffect(target, MobEffects.DIG_SPEED, buffDur, 0, caster);
            else healWithFx(target, caster, 1);
        } else if (hpRatio > 0.8f) {
            // 交战 + 高血：迅捷 II
            addEffect(target, MobEffects.MOVEMENT_SPEED, buffDur, 1, caster);
        } else if (hpRatio >= 0.5f) {
            // 交战 + 中血：伤害吸收 I（自定义累加黄心，持续受腐殖之戒系数）+ 回血 1 心
            ParasiticAbsorptionManager.addAbsorption(target, 0, humus);
            healWithFx(target, caster, 1);
        } else {
            // 交战 + 低血：伤害吸收 II + 回血 2 心
            ParasiticAbsorptionManager.addAbsorption(target, 1, humus);
            healWithFx(target, caster, 2);
        }
    }

    /** 立即回血（触发血条回血动画）；专属「生命恢复」buff 仅显示图标，不周期回血。 */
    private static void healWithFx(LivingEntity target, ServerPlayer caster, int hearts) {
        target.heal(hearts * 2.0f);
        addEffect(target, AddonEffects.BAT_REGEN.get(), 60, hearts - 1, caster);
    }

    private static void applyEnemyFruit(Holder holder, LivingEntity target, EnemyFruit fruit, int stack, ServerPlayer caster) {
        int amp = Mth.clamp(stack - 1, 0, 2);
        float humus = holder.currentHumusFactor;
        switch (fruit) {
            case VINE -> {
                addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(60, humus), amp, caster);
                if (!target.onGround()) {
                    Vec3 velocity = target.getDeltaMovement();
                    target.setDeltaMovement(velocity.x * 0.75, Math.min(velocity.y, -0.08), velocity.z * 0.75);
                    target.hurtMarked = true;
                }
            }
            case BITTER -> {
                addEffect(target, MobEffects.WEAKNESS, scaleDuration(80, humus), amp, caster);
                addEffect(target, MobEffects.GLOWING, scaleDuration(80, humus), 0, caster);
            }
            case ROTTEN -> {
                if (target.getMobType() == MobType.UNDEAD) {
                    addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(70, humus), amp, caster);
                    addEffect(target, MobEffects.WEAKNESS, scaleDuration(70, humus), amp, caster);
                } else {
                    addEffect(target, MobEffects.POISON, scaleDuration(60, humus), amp, caster);
                }
            }
            case SOUR -> {
                addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(40, humus), amp, caster);
                addEffect(target, MobEffects.GLOWING, scaleDuration(60, humus), 0, caster);
            }
        }
    }

    private static void addEffect(LivingEntity target, MobEffect effect, int duration, int amplifier, ServerPlayer caster) {
        if (duration <= 0) return;
        // 带 caster source：供食梦魔「入梦」debuff/高光拦截归因（友方目标 caster 也是合法 source，不影响判定）
        target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true), caster);
    }

    /** 腐殖之戒系数（敌方果 +50%）后，每次触发的药水效果统一额外延长 0.3 秒（+6 tick）。 */
    private static int scaleDuration(int duration, float humusFactor) {
        return Math.max(10, Math.round(duration * humusFactor) + 6);
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return null;
        for (ServerLevel world : server.getAllLevels()) {
            Entity found = world.getEntity(uuid);
            if (found instanceof LivingEntity living) return living;
        }
        return null;
    }

    // ===== 特效 =====

    private static void spawnAttachedSeedParticles(ServerPlayer caster, LivingEntity host, SeedData seed, long now) {
        if (!(host.level() instanceof ServerLevel world)) return;
        boolean friend = WhitelistUtils.isBuffTarget(caster, host);
        // 满层 outline 高光：通过 scoreboard team 染色 + 短时长 GLOWING 状态实现客户端描边。
        // 优先级低于次要技能：被次要技能感染的目标让位（不画绿/红 outline）。
        boolean infected = InfectionSporeManager.isInfected(host.getUUID());
        if (seed.stack >= BAL.i("max_seeds", MAX_SEEDS) && !infected) {
            if (friend) GlowMarker.markFriend(host, caster);
            else GlowMarker.markEnemy(host, caster);
        } else {
            GlowMarker.unmark(host);
        }
        if (now % 10 != 0) return;
        // 粒子量随堆叠层数增加，仅作为附着指示
        ParticleUtils.spawnParticles(world, friend ? FRIEND_DUST : ENEMY_DUST,
                host.getX(), host.getY() + host.getBbHeight() * 0.75, host.getZ(),
                2 * seed.stack, 0.18, 0.25, 0.18, 0.0);
    }

    private static void spawnFruitParticles(LivingEntity host, ParticleOptions particle, int stack) {
        if (!(host.level() instanceof ServerLevel world)) return;
        double y = host.getY() + host.getBbHeight() * 0.75;
        int extra = stack * 4;
        ParticleUtils.spawnParticles(world, particle, host.getX(), y, host.getZ(), 16 + extra, 0.35, 0.45, 0.35, 0.02);
        ParticleUtils.spawnParticles(world, SEED_DUST, host.getX(), y, host.getZ(), 8 + extra / 2, 0.25, 0.35, 0.25, 0.01);
        world.playSound(null, host.getX(), host.getY(), host.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.175f, 1.8f);
    }

    private enum EnemyFruit {
        VINE,
        BITTER,
        ROTTEN,
        SOUR
    }

    private static final class SeedData {
        long endTick;
        long nextFruitTick;
        /** 堆叠层数 1～3，决定减益等级与粒子密度 */
        int stack = 1;

        SeedData(long endTick, long nextFruitTick) {
            this.endTick = endTick;
            this.nextFruitTick = nextFruitTick;
        }
    }
}
