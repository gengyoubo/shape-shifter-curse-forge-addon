package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkManager;
import github.com.gengyoubo.sscfa.ability.ParasiticAbsorptionManager;
import github.com.gengyoubo.sscfa.ability.ParasiticCombatTracker;
import github.com.gengyoubo.sscfa.ability.fruit.FruitSelection;
import github.com.gengyoubo.sscfa.ability.fruit.FruitStatusOverlay;
import github.com.gengyoubo.sscfa.ability.fruit.ParasiticFruitAbility;
import github.com.gengyoubo.sscfa.ability.fruit.ParasiticFruitLayers;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.entity.ParasiticSeedProjectile;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.GlowMarker;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.joml.Vector3f;

/**
 * 寄生果蝠主技能 {@code my_addon:parasitic_fruit_seed}：灵果寄生。
 *
 * <p>按住技能键蓄力、松开投掷种子（最小蓄力 0.5 秒，满蓄力 1.5 秒决定速度），两次短按切换果实
 * （自动 / 缠藤 / 苦涩 / 腐熟 / 酸涩），手势见 {@link ParasiticFruitAbility}。命中生物在宿主身上种下一层果实，
 * 宿主最多三层（所有施法者共享，先进先出），由 {@link ParasiticFruitLayers} 按间隔结出果实：
 * 友方得到治疗与辅助，敌方按所选果实（自动时按宿主状态）受到削弱，层数决定等级。
 * 未命中落地时生成种子圈。全部判定在服务端执行。</p>
 */
public final class ParasiticFruitSeedPower implements SscPowerType {
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("my_addon", "parasitic_fruit_seed");

    // 以下均为默认值；运行时从 balance 快照读取（abilities.parasitic_seed_power）。
    private static final int MAX_SEEDS = 3;
    private static final int ROOTING_TICKS = 20;
    private static final int FRUIT_INTERVAL_TICKS = 25;
    private static final int ENERGY_COST = 1;

    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_power");

    private static final DustParticleOptions FRIEND_DUST = new DustParticleOptions(new Vector3f(0.35f, 0.95f, 0.30f), 1.1f);
    private static final DustParticleOptions ENEMY_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.10f, 0.75f), 1.1f);
    private static final DustParticleOptions SEED_DUST = new DustParticleOptions(new Vector3f(0.95f, 0.72f, 0.24f), 1.0f);

    /** 腐殖之戒：当前结果的时长系数（{@link #harvest} 临时设置，结束复位为 1）。服务端单线程使用。 */
    private static float currentHumusFactor = 1.0f;

    /** 玩家激活的本类型能力。 */
    public record Active(ResourceLocation id, JsonObject data) {}

    public static boolean isParasitizedByEnemyFruit(UUID hostUuid, long worldTime) {
        return ParasiticFruitLayers.isEnemyParasitized(hostUuid, worldTime);
    }

    /** 当前激活且条件成立的灵果寄生能力；没有时为 null。 */
    public static Active active(ServerPlayer player) {
        final Active[] found = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] == null && TYPE.toString().equals(FormPowerRegistry.typeOf(power))) {
                JsonObject condition = power.has("condition") && power.get("condition").isJsonObject()
                        ? power.getAsJsonObject("condition") : null;
                if (FormPowerRuntime.test(player, player, condition)) found[0] = new Active(id, power);
            }
        });
        return found[0];
    }

    // ===== 按键：交给服务端手势识别 =====

    @Override
    public void onKeyPressed(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        if (!BalanceIntegration.isPlayerReady(caster)) return;
        if (!FormPowerRuntime.test(caster, caster, data.has("condition") ? data.getAsJsonObject("condition") : null)) return;
        ParasiticFruitAbility.pressed(caster, powerId);
    }

    @Override
    public void onKeyReleased(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        ParasiticFruitAbility.released(caster);
    }

    @Override
    public void onRemoved(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        ParasiticFruitAbility.cancel(caster);
        ParasiticFruitLayers.removeOwner(caster);
    }

    /** 蓄力投掷：消耗种子量（双生种荚翻倍），按蓄力决定速度，起算冷却（双生种荚用 extra twin_pod）。 */
    public static boolean throwChargedSeed(ServerPlayer caster, ResourceLocation powerId, int ticks, FruitSelection selection) {
        String id = powerId.toString();
        if (!FruitSelection.canRelease(ticks) || !SkillCooldowns.ready(caster, id)
                || AbilityUseGuard.blocked(caster) || caster.hasEffect(AddonEffects.PURIFIED.get())
                || !FormUtils.isForm(caster, FormIdentifiers.BAT_PARASITIC_FRUIT)) return false;
        boolean twinPod = TrinketUtils.isWearing(caster, AddonItems.TWIN_POD.get());
        int cost = BAL.i("energy_cost", ENERGY_COST) * (twinPod ? 2 : 1);
        if (!PowerUtils.hasResource(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, cost)) {
            caster.serverLevel().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.7f);
            return false;
        }
        var seed = new ParasiticSeedProjectile(caster.level(), caster);
        seed.setItem(Items.WHEAT_SEEDS.getDefaultInstance());
        seed.setTwinPod(twinPod);
        seed.setSelection(selection);
        seed.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        // 原版 shootFromRotation 已叠加一次施法者速度。
        seed.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0, FruitSelection.speed(ticks), 0.4f);
        PowerUtils.changeResourceValueAndSync(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, -cost);
        boolean spawned = false;
        try {
            spawned = caster.level().addFreshEntity(seed);
        } finally {
            if (!spawned) PowerUtils.changeResourceValueAndSync(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, cost);
        }
        if (!spawned) return false;
        SkillCooldowns.force(caster, id, twinPod ? SkillCooldowns.extra(caster, id, "twin_pod") : SkillCooldowns.cooldown(caster, id));
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 1.5f);
        return true;
    }

    public static int fruitInterval() { return Math.max(1, BAL.i("fruit_interval_ticks", FRUIT_INTERVAL_TICKS)); }

    public static FruitSelection selection(ServerPlayer caster) { return ParasiticFruitAbility.selection(caster); }

    // ===== 种植 =====

    /** 种下一层：寿命未交战 15 秒 / 交战 5 秒（life_peace / life_combat）。 */
    public static void plantSeedFrom(ServerPlayer caster, LivingEntity host, FruitSelection selected) {
        if (caster == null || host == null) return;
        plantSeedFrom(caster, host, ParasiticCombatTracker.isInCombat(host)
                ? BAL.i("life_combat", 100) : BAL.i("life_peace", 300), selected);
    }

    public static void plantSeedFrom(ServerPlayer caster, LivingEntity host) { plantSeedFrom(caster, host, selection(caster)); }

    /** 带自定义寿命（种子圈拾取传剩余时长）。 */
    public static void plantSeedFrom(ServerPlayer caster, LivingEntity host, int life) {
        plantSeedFrom(caster, host, life, selection(caster));
    }

    public static void plantSeedFrom(ServerPlayer caster, LivingEntity host, int life, FruitSelection selected) {
        if (!isValidSeedHost(caster, host) || active(caster) == null) return;
        FruitSelection fruit = selected == null || selected == FruitSelection.AUTO ? autoFruit(host) : selected;
        int total = ParasiticFruitLayers.plant(caster, host, fruit, life,
                BAL.i("rooting_ticks", ROOTING_TICKS), BAL.i("max_seeds", MAX_SEEDS));
        if (host.level() instanceof ServerLevel world) {
            ParticleUtils.spawnParticles(world, SEED_DUST, host.getX(), host.getY() + host.getBbHeight() * 0.65,
                    host.getZ(), 18 + total * 6, 0.25, 0.35, 0.25, 0.02);
            // 叮声仅施法者本人听见
            MancianimaMarkManager.playSoundToPlayer(caster, SoundEvents.GRASS_PLACE, 0.9f, 1.5f);
        }
    }

    public static void plantSeedSpread(ServerPlayer caster, LivingEntity host, boolean twinPod) {
        plantSeedSpread(caster, host, twinPod, selection(caster));
    }

    /** 双生种荚：额外寄生宿主 4 格内最近的另一个目标；附近没有时给宿主再叠一层。 */
    public static void plantSeedSpread(ServerPlayer caster, LivingEntity host, boolean twinPod, FruitSelection selected) {
        if (!isValidSeedHost(caster, host)) return;
        plantSeedFrom(caster, host, selected);
        if (twinPod) {
            LivingEntity extra = findNearbyOther(caster, host);
            plantSeedFrom(caster, extra == null ? host : extra, selected);
        }
    }

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

    /** 盔甲架、旁观者、其它世界或被领域隔开的目标不能消耗种子。 */
    public static boolean isValidSeedHost(ServerPlayer caster, LivingEntity host) {
        return caster != null && host != null && host.isAlive() && !host.isSpectator()
                && !(host instanceof ArmorStand) && caster.level() == host.level()
                && !DomainManager.blocksTargeting(caster, host);
    }

    /** 自动选果：移动 → 缠藤，攻击/有目标 → 苦涩，高血或高护甲 → 腐熟，否则酸涩。 */
    public static FruitSelection autoFruit(LivingEntity host) {
        if (host.isSprinting() || host.getDeltaMovement().horizontalDistanceSqr() > 0.08) return FruitSelection.VINE;
        if (host.swinging || (host instanceof Mob mob && mob.getTarget() != null)) return FruitSelection.BITTER;
        if (host.getHealth() / host.getMaxHealth() > 0.7f || host.getArmorValue() >= 10) return FruitSelection.ROTTEN;
        return FruitSelection.SOUR;
    }

    // ===== 结果 =====

    /** 一次结果：友方按状态给支援（不随层数倍增），敌方按果实种类与层数施加削弱。 */
    public static void harvest(ServerPlayer caster, LivingEntity host, FruitSelection kind, int stack, boolean friend) {
        boolean humus = TrinketUtils.isWearing(caster, AddonItems.HUMUS_RING.get());
        // 腐殖之戒：敌方削弱果时长 ×1.5、友方增益果时长 ×0.7。
        currentHumusFactor = humus ? (friend ? 0.7f : 1.5f) : 1.0f;
        try {
            if (friend) applyFriendBuff(host, caster);
            else applyEnemyFruit(host, kind == FruitSelection.AUTO ? autoFruit(host) : kind, stack, caster);
            spawnFruitParticles(host, friend ? FRIEND_DUST : ENEMY_DUST, stack);
        } finally {
            currentHumusFactor = 1.0f;
        }
    }

    /** 友方果实：交战/未交战 × 血量分级。 */
    private static void applyFriendBuff(LivingEntity target, ServerPlayer caster) {
        boolean inCombat = ParasiticCombatTracker.isInCombat(target);
        float hpRatio = target.getHealth() / Math.max(1.0f, target.getMaxHealth());
        float humus = currentHumusFactor;
        // buff 时长 = 结果间隔 + 0.2s，再乘腐殖之戒系数
        int buffDur = Math.max(10, Math.round((fruitInterval() + 4) * humus));
        if (!inCombat) {
            if (hpRatio > 0.7f) addEffect(target, MobEffects.DIG_SPEED, buffDur, 0, caster);
            else healWithFx(target, caster, 1);
        } else if (hpRatio > 0.8f) {
            addEffect(target, MobEffects.MOVEMENT_SPEED, buffDur, 1, caster);
        } else if (hpRatio >= 0.5f) {
            ParasiticAbsorptionManager.addAbsorption(target, 0, humus);
            healWithFx(target, caster, 1);
        } else {
            ParasiticAbsorptionManager.addAbsorption(target, 1, humus);
            healWithFx(target, caster, 2);
        }
    }

    /** 立即回血（血条动画）；专属「生命恢复」buff 只显示图标，不周期回血。 */
    private static void healWithFx(LivingEntity target, ServerPlayer caster, int hearts) {
        target.heal(hearts * 2.0f);
        addEffect(target, AddonEffects.BAT_REGEN.get(), 60, hearts - 1, caster);
    }

    private static void applyEnemyFruit(LivingEntity target, FruitSelection fruit, int stack, ServerPlayer caster) {
        int amp = Mth.clamp(stack - 1, 0, 2);
        switch (fruit) {
            case VINE -> {
                addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(60), amp, caster);
                if (!target.onGround()) {
                    Vec3 velocity = target.getDeltaMovement();
                    target.setDeltaMovement(velocity.x * 0.75, Math.min(velocity.y, -0.08), velocity.z * 0.75);
                    target.hurtMarked = true;
                }
            }
            case BITTER -> {
                addEffect(target, MobEffects.WEAKNESS, scaleDuration(80), amp, caster);
                addEffect(target, MobEffects.GLOWING, scaleDuration(80), 0, caster);
            }
            case ROTTEN -> {
                if (target.getMobType() == MobType.UNDEAD) {
                    addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(70), amp, caster);
                    addEffect(target, MobEffects.WEAKNESS, scaleDuration(70), amp, caster);
                } else {
                    addEffect(target, MobEffects.POISON, scaleDuration(60), amp, caster);
                }
            }
            case SOUR -> {
                addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(40), amp, caster);
                addEffect(target, MobEffects.GLOWING, scaleDuration(60), 0, caster);
            }
            case AUTO -> { }
        }
    }

    private static void addEffect(LivingEntity target, MobEffect effect, int duration, int amplifier, ServerPlayer caster) {
        if (duration <= 0) return;
        // 带 caster source：供食梦魔「入梦」归因（友方目标 caster 也是合法 source）
        target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true), caster);
    }

    /** 腐殖之戒系数后，每次触发的药水效果统一额外延长 0.3 秒（+6 tick）。 */
    private static int scaleDuration(int duration) {
        return Math.max(10, Math.round(duration * currentHumusFactor) + 6);
    }

    // ===== 特效 =====

    /** 宿主附着表现：满三层且未被孢子感染时描边（通过叠加层登记为 outline 组），每 10 tick 冒附着粒子。 */
    public static void drawAttached(ServerPlayer caster, LivingEntity host, int total) {
        if (!(host.level() instanceof ServerLevel world)) return;
        boolean friend = WhitelistUtils.isBuffTarget(caster, host);
        if (total >= 3 && !InfectionSporeManager.isInfected(host.getUUID())) {
            FruitStatusOverlay.pulse(host, "outline", 3, () -> {
                if (friend) GlowMarker.markFriend(host, caster);
                else GlowMarker.markEnemy(host, caster);
            });
        } else {
            GlowMarker.unmark(host);
        }
        if (world.getGameTime() % 10 != 0) return;
        ParticleUtils.spawnParticles(world, friend ? FRIEND_DUST : ENEMY_DUST,
                host.getX(), host.getY() + host.getBbHeight() * 0.75, host.getZ(),
                2 * total, 0.18, 0.25, 0.18, 0.0);
    }

    private static void spawnFruitParticles(LivingEntity host, ParticleOptions particle, int stack) {
        if (!(host.level() instanceof ServerLevel world)) return;
        double y = host.getY() + host.getBbHeight() * 0.75;
        int extra = stack * 4;
        ParticleUtils.spawnParticles(world, particle, host.getX(), y, host.getZ(), 16 + extra, 0.35, 0.45, 0.35, 0.02);
        ParticleUtils.spawnParticles(world, SEED_DUST, host.getX(), y, host.getZ(), 8 + extra / 2, 0.25, 0.35, 0.25, 0.01);
        world.playSound(null, host.getX(), host.getY(), host.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.0875f, 1.8f);
    }
}
