package github.com.gengyoubo.sscfa.anomaly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.spell.SpellDamageSource;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.registries.RegistryObject;

/** Registered elemental effects, local reactions and bounded summon-hit budgets. */
public final class AnomalyEffects {
    public static final RegistryObject<MobEffect> LUNAR_ECLIPSE = AddonEffects.ANOMALY_LUNAR_ECLIPSE;
    public static final RegistryObject<MobEffect> CURSE_WEAKNESS = AddonEffects.ANOMALY_CURSE_WEAKNESS;
    public static final RegistryObject<MobEffect> SUMMON_MARK = AddonEffects.ANOMALY_SUMMON_MARK;
    public static final RegistryObject<MobEffect> VOID_EROSION = AddonEffects.ANOMALY_VOID_EROSION;
    public static final RegistryObject<MobEffect> SPACE_BIND = AddonEffects.ANOMALY_SPACE_BIND;
    public static final RegistryObject<MobEffect> FROST_SLOW = AddonEffects.ANOMALY_FROST_SLOW;
    public static final RegistryObject<MobEffect> NO_SPRINT = AddonEffects.ANOMALY_NO_SPRINT;
    public static final RegistryObject<MobEffect> FLAME_SPIRIT = AddonEffects.ANOMALY_FLAME_SPIRIT;
    public static final RegistryObject<MobEffect> FROST_HUNT = AddonEffects.ANOMALY_FROST_HUNT;
    public static final RegistryObject<MobEffect> SPIRIT_MOON = AddonEffects.ANOMALY_SPIRIT_MOON;
    public static final RegistryObject<MobEffect> SUMMON_WEAKNESS = AddonEffects.ANOMALY_SUMMON_WEAKNESS;
    public static final RegistryObject<MobEffect> FIRE_ZONE = AddonEffects.ANOMALY_FIRE_ZONE;
    public static final ResourceKey<DamageType> FIRE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("ssc_addon", "anomaly_fire"));
    public static final ResourceKey<DamageType> MAGIC_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("ssc_addon", "anomaly_magic"));

    @FunctionalInterface public interface BaseExecutor { void apply(LivingEntity target, SpellAnomalyContext context); }
    @FunctionalInterface public interface ReactionExecutor { void apply(LivingEntity target, SpellAnomalyContext context); }

    private static final UUID SLOW_MODIFIER = UUID.fromString("362902c5-ebc9-4efe-8096-31bb6f2e3573");
    private static final Map<String, MobEffect> EFFECTS = new LinkedHashMap<>();
    private static final Map<String, BaseExecutor> BASES = new LinkedHashMap<>();
    private static final Map<String, ReactionExecutor> REACTIONS = new LinkedHashMap<>();
    private static final Map<LivingEntity, ServerLevel> ACTIVE = new IdentityHashMap<>();
    private static final Map<ZoneKey, Zone> ZONES = new LinkedHashMap<>();
    private static final ArrayDeque<Proc> PROCS = new ArrayDeque<>();
    private static final ThreadLocal<int[]> REACTION_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static boolean initialized;

    static {
        registerBase("fire", (target, context) -> reactionDamage(target, context, 8, true));
        registerBase("ice", (target, context) -> FrozenControl.freeze(target, FrozenControl.DEFAULT_DURATION_TICKS));
        registerBase("lunar", (target, context) -> apply(target, LUNAR_ECLIPSE.get(), 120, 0));
        registerBase("curse", (target, context) -> apply(target, CURSE_WEAKNESS.get(), 120, 0));
        registerBase("summon", (target, context) -> apply(target, SUMMON_MARK.get(), 160, 0));
        registerBase("void", (target, context) -> apply(target, VOID_EROSION.get(), 120, 0));
        registerBase("space", (target, context) -> apply(target, SPACE_BIND.get(), 100, 0));

        registerReaction("fire", "ice", (target, context) -> area(target, context, 5, other -> {
            reactionDamage(other, context, 8, false);
            if (other.isAlive()) apply(other, FROST_SLOW.get(), 100, 0);
        }));
        registerReaction("fire", "lunar", (target, context) -> apply(target, LUNAR_ECLIPSE.get(), 120, 1));
        registerReaction("fire", "curse", (target, context) -> area(target, context, 3,
                other -> apply(other, CURSE_WEAKNESS.get(), 100, 0)));
        registerReaction("fire", "summon", (target, context) -> budget(target, FLAME_SPIRIT.get()));
        registerReaction("fire", "void", (target, context) -> reactionDamage(target, context, 4, false));
        registerReaction("fire", "space", AnomalyEffects::fireZone);
        registerReaction("ice", "lunar", (target, context) -> extend(target, LUNAR_ECLIPSE.get(), 180, 0));
        registerReaction("ice", "curse", (target, context) -> apply(target, CURSE_WEAKNESS.get(), 120, 2));
        registerReaction("ice", "summon", (target, context) -> budget(target, FROST_HUNT.get()));
        registerReaction("ice", "void", (target, context) -> reactionDamage(target, context, 6, false));
        registerReaction("ice", "space", (target, context) -> apply(target, NO_SPRINT.get(), 100, 0));
        registerReaction("lunar", "curse", (target, context) -> {
            extend(target, LUNAR_ECLIPSE.get(), 180, 0); extend(target, CURSE_WEAKNESS.get(), 180, 0);
        });
        registerReaction("lunar", "summon", (target, context) -> budget(target, SPIRIT_MOON.get()));
        registerReaction("lunar", "void", (target, context) -> apply(target, VOID_EROSION.get(), 120, 1));
        registerReaction("lunar", "space", (target, context) -> area(target, context, 3,
                other -> apply(other, LUNAR_ECLIPSE.get(), 100, 0)));
        registerReaction("curse", "summon", (target, context) -> apply(target, SUMMON_WEAKNESS.get(), 120, 0));
        registerReaction("curse", "void", (target, context) -> {
            apply(target, CURSE_WEAKNESS.get(), 120, 1); apply(target, VOID_EROSION.get(), 120, 1);
        });
        registerReaction("curse", "space", (target, context) -> area(target, context, 3, other -> {
            apply(other, CURSE_WEAKNESS.get(), 100, 0); apply(other, NO_SPRINT.get(), 100, 0);
        }));
        registerReaction("summon", "void", (target, context) -> apply(target, SUMMON_MARK.get(), 160, 1));
        registerReaction("summon", "space", (target, context) -> area(target, context, 3,
                other -> apply(other, SUMMON_MARK.get(), 160, 0)));
        registerReaction("void", "space", (target, context) -> area(target, context, 3,
                other -> apply(other, VOID_EROSION.get(), 100, 0)));
    }

    private AnomalyEffects() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EFFECTS.put("lunar_eclipse", LUNAR_ECLIPSE.get());
        EFFECTS.put("curse_weakness", CURSE_WEAKNESS.get());
        EFFECTS.put("summon_mark", SUMMON_MARK.get());
        EFFECTS.put("void_erosion", VOID_EROSION.get());
        EFFECTS.put("space_bind", SPACE_BIND.get());
        EFFECTS.put("frost_slow", FROST_SLOW.get());
        EFFECTS.put("no_sprint", NO_SPRINT.get());
        EFFECTS.put("flame_spirit", FLAME_SPIRIT.get());
        EFFECTS.put("frost_hunt", FROST_HUNT.get());
        EFFECTS.put("spirit_moon", SPIRIT_MOON.get());
        EFFECTS.put("summon_weakness", SUMMON_WEAKNESS.get());
        EFFECTS.put("fire_zone", FIRE_ZONE.get());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) { drain(); tickZones(); }
        });
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            clear(event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getEntity() instanceof LivingEntity living) clear(living);
        });
        MinecraftForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getEntity() instanceof LivingEntity living) clear(living);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) clear(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) clear(player);
        });
        PlayerSessionCleanup.register((connection, server) -> clear(connection.player));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> clearAll());
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel world) AnomalyReactionFeedback.clear(world);
        });
    }

    public static void registerBase(String element, BaseExecutor executor) {
        Objects.requireNonNull(element); Objects.requireNonNull(executor);
        if (element.isBlank() || BASES.putIfAbsent(element, executor) != null)
            throw new IllegalArgumentException("Duplicate or empty anomaly type: " + element);
    }

    public static void registerReaction(String first, String second, ReactionExecutor executor) {
        Objects.requireNonNull(executor);
        if (first == null || second == null || first.isBlank() || second.isBlank() || first.equals(second)
                || REACTIONS.putIfAbsent(pair(first, second), executor) != null)
            throw new IllegalArgumentException("Invalid or duplicate anomaly reaction");
    }

    public static boolean hasBase(String element) { return BASES.containsKey(element); }
    public static Map<String, BaseExecutor> registeredBases() { return Collections.unmodifiableMap(BASES); }
    public static Map<String, ReactionExecutor> registeredReactions() { return Collections.unmodifiableMap(REACTIONS); }

    public static void activate(String element, LivingEntity target, SpellAnomalyContext context) {
        if (!live(target)) return;
        var executor = BASES.get(element);
        if (executor != null) {
            AnomalyReactionFeedback.showBase(element, target);
            executor.apply(target, context);
        }
    }

    public static void react(String first, String second, LivingEntity target, SpellAnomalyContext context) {
        if (!live(target) || first == null || second == null || first.equals(second)) return;
        var executor = REACTIONS.get(pair(first, second));
        if (executor != null) {
            AnomalyReactionFeedback.show(first, second, target, context);
            executor.apply(target, context);
        }
    }

    static String pair(String first, String second) {
        return first.compareTo(second) < 0 ? first + "+" + second : second + "+" + first;
    }

    public static final class Cache {
        double healingSuppression, dealtReduction, summonVulnerability, spellVulnerability, summonReduction, slow;
        boolean teleportBlocked, sprintBlocked;
        Budget fire, frost, healing;
    }

    private static final class Budget {
        double remaining;
        Budget(int remaining) { this.remaining = remaining; }
    }

    private static Cache cache(LivingEntity target) { return ((AnomalyEffectAccess) target).ssca$anomalyEffects(); }

    public static boolean hasState(LivingEntity target) {
        return target != null && (cache(target) != null || target.hasEffect(AddonEffects.FROZEN.get()));
    }

    public record EffectView(String id, int ticks, double strength) {}

    /** Client-display mirror of the currently managed anomaly effects. */
    public static List<EffectView> effectSnapshot(LivingEntity target) {
        if (target == null) return List.of();
        List<EffectView> views = new ArrayList<>();
        for (Map.Entry<String, MobEffect> entry : EFFECTS.entrySet()) {
            MobEffectInstance instance = target.getEffect(entry.getValue());
            if (instance != null) {
                views.add(new EffectView(entry.getKey(), instance.getDuration(), instance.getAmplifier() + 1));
            }
        }
        MobEffectInstance frozen = target.getEffect(AddonEffects.FROZEN.get());
        if (frozen != null) views.add(new EffectView("frozen", frozen.getDuration(), frozen.getAmplifier() + 1));
        return List.copyOf(views);
    }

    public static boolean isManaged(MobEffect effect) {
        return effect instanceof github.com.gengyoubo.sscfa.effect.AnomalyMarkerEffect
                || effect == AddonEffects.FROZEN_RECOVERY.get();
    }

    public static void refresh(LivingEntity target) {
        Cache prior = cache(target);
        double healing = strength(target, LUNAR_ECLIPSE.get()), dealt = strength(target, CURSE_WEAKNESS.get());
        double summon = strength(target, SUMMON_MARK.get()), spell = strength(target, VOID_EROSION.get());
        boolean space = target.hasEffect(SPACE_BIND.get()), sprint = target.hasEffect(NO_SPRINT.get());
        double slow = Math.max(target.hasEffect(AddonEffects.FROZEN_RECOVERY.get()) ? .25 : 0,
                Math.max(space ? .30 : 0, target.hasEffect(FROST_SLOW.get()) ? .50 : 0));
        boolean fireBudget = target.hasEffect(FLAME_SPIRIT.get()), frostBudget = target.hasEffect(FROST_HUNT.get());
        boolean healBudget = target.hasEffect(SPIRIT_MOON.get()), summonWeak = target.hasEffect(SUMMON_WEAKNESS.get());
        boolean zone = target.hasEffect(FIRE_ZONE.get());
        boolean any = healing > 0 || dealt > 0 || summon > 0 || spell > 0 || space || sprint
                || slow > 0 || fireBudget || frostBudget || healBudget || summonWeak || zone;
        Cache next = any ? prior == null ? new Cache() : prior : null;
        if (next != null) {
            next.healingSuppression = healing; next.dealtReduction = dealt;
            next.summonVulnerability = summon; next.spellVulnerability = spell;
            next.summonReduction = summonWeak ? .50 : 0;
            next.teleportBlocked = space; next.sprintBlocked = sprint; next.slow = slow;
            if (!fireBudget) next.fire = null;
            if (!frostBudget) next.frost = null;
            if (!healBudget) next.healing = null;
        }
        ((AnomalyEffectAccess) target).ssca$anomalyEffects(next);
        if (target.level() instanceof ServerLevel world) {
            if (any) ACTIVE.put(target, world); else ACTIVE.remove(target);
            if (!zone && !ZONES.isEmpty()) {
                for (Zone current : ZONES.values())
                    if (current.anchor == target && now(current.world) < current.until) current.cancelled = true;
            }
            var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                var modifier = speed.getModifier(SLOW_MODIFIER);
                if (slow <= 0) { if (modifier != null) speed.removeModifier(SLOW_MODIFIER); }
                else if (modifier == null || Math.abs(modifier.getAmount() + slow) > 1e-9) {
                    speed.removeModifier(SLOW_MODIFIER);
                    speed.addTransientModifier(new AttributeModifier(SLOW_MODIFIER, "SSCA anomaly slow",
                            -slow, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            AnomalyManager.effectsChanged(target);
        }
        if (sprint && target.isSprinting()) target.setSprinting(false);
    }

    private static double strength(LivingEntity target, MobEffect type) {
        MobEffectInstance effect = target.getEffect(type);
        if (effect == null) return 0;
        int amp = effect.getAmplifier();
        if (type == LUNAR_ECLIPSE.get()) return amp >= 1 ? .75 : .50;
        if (type == CURSE_WEAKNESS.get()) return amp >= 2 ? .35 : amp == 1 ? .30 : .20;
        if (type == SUMMON_MARK.get()) return amp >= 1 ? .40 : .25;
        if (type == VOID_EROSION.get()) return amp >= 1 ? .25 : .15;
        return type == SPACE_BIND.get() ? .30 : type == FROST_SLOW.get() ? .50 : 0;
    }

    public static float modifyHealing(LivingEntity target, float amount) {
        Cache state = cache(target);
        return state == null || amount <= 0 ? amount : (float)(amount * (1 - state.healingSuppression));
    }

    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0 || target.level().isClientSide) return amount;
        Cache received = cache(target);
        Cache dealt = source.getEntity() instanceof LivingEntity attacker ? cache(attacker) : null;
        if (received == null && dealt == null) return amount;
        boolean summon = isSummonDamage(source);
        if (dealt != null) amount *= 1 - Math.max(dealt.dealtReduction,
                isSummoned(target) ? dealt.summonReduction : 0);
        if (received != null) {
            double vulnerability = Math.max(summon ? received.summonVulnerability : 0,
                    isSpellDamage(source) ? received.spellVulnerability : 0);
            amount *= 1 + vulnerability;
        }
        return amount;
    }

    public static boolean teleportBlocked(LivingEntity target) {
        Cache state = cache(target); return state != null && state.teleportBlocked;
    }
    public static boolean sprintBlocked(LivingEntity target) {
        Cache state = cache(target); return state != null && state.sprintBlocked;
    }

    public static boolean isReaction(DamageSource source) { return source.is(FIRE_DAMAGE) || source.is(MAGIC_DAMAGE); }
    public static boolean isSpellDamage(DamageSource source) {
        return source.is(SpellDamageSource.SPELL_DAMAGE) || isReaction(source)
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private static boolean isSummoned(Entity entity) {
        return entity instanceof SpellAnomalyHits.SummonedData summon && summon.ssca$summonAnomaly() != null;
    }
    private static SpellAnomalyContext summonContext(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof SpellAnomalyHits.SummonedData summon) return summon.ssca$summonAnomaly();
        if (direct instanceof SpellAnomalyHits.ProjectileData projectile) {
            var context = projectile.ssca$anomaly();
            if (context != null && context.element().equals("summon")) return context;
        }
        return source.getEntity() instanceof SpellAnomalyHits.SummonedData summon ? summon.ssca$summonAnomaly() : null;
    }
    private static boolean isSummonDamage(DamageSource source) { return !isReaction(source) && summonContext(source) != null; }

    private record Proc(LivingEntity target, Cache state, Budget fire, Budget frost, Budget healing,
                        SpellAnomalyContext summoner, ServerLevel world) {}

    public static void afterDamage(LivingEntity target, DamageSource source, boolean accepted) {
        if (!accepted || target.level().isClientSide) return;
        Cache state = cache(target);
        if (state == null || state.fire == null && state.frost == null && state.healing == null) return;
        if (isReaction(source) || REACTION_DEPTH.get()[0] > 0) return;
        SpellAnomalyContext summoner = summonContext(source);
        if (summoner == null || !(target.level() instanceof ServerLevel world)) return;
        PROCS.addLast(new Proc(target, state, state.fire, state.frost, state.healing, summoner, world));
    }

    public static void drain() {
        while (!PROCS.isEmpty()) {
            Proc proc = PROCS.removeFirst();
            LivingEntity target = proc.target(); Cache state = cache(target);
            if (!live(target) || target.level() != proc.world() || state != proc.state()) continue;
            if (proc.fire() != null && state.fire == proc.fire() && proc.fire().remaining > 0
                    && target.hasEffect(FLAME_SPIRIT.get())) {
                proc.fire().remaining--;
                reactionDamage(target, proc.summoner(), 2, true);
                if (proc.fire().remaining <= 0) target.removeEffect(FLAME_SPIRIT.get());
            }
            if (live(target) && proc.frost() != null && state.frost == proc.frost() && proc.frost().remaining > 0
                    && target.hasEffect(FROST_HUNT.get())) {
                proc.frost().remaining--;
                reactionDamage(target, proc.summoner(), 6, false);
                if (proc.frost().remaining <= 0) target.removeEffect(FROST_HUNT.get());
            }
            if (proc.healing() != null && state.healing == proc.healing() && proc.healing().remaining > 0
                    && target.hasEffect(SPIRIT_MOON.get())) {
                ServerPlayer owner = proc.world().getServer().getPlayerList().getPlayer(proc.summoner().casterUUID());
                if (owner != null && owner.isAlive() && owner.level() == proc.world()) {
                    float before = owner.getHealth(); owner.heal((float)Math.min(1, proc.healing().remaining));
                    proc.healing().remaining = Math.max(0, proc.healing().remaining - Math.max(0, owner.getHealth() - before));
                    if (proc.healing().remaining <= 0) target.removeEffect(SPIRIT_MOON.get());
                }
            }
        }
    }

    private static void budget(LivingEntity target, MobEffect marker) {
        apply(target, marker, 120, 0);
        if (!target.hasEffect(marker)) return;
        Cache state = cache(target);
        if (marker == FLAME_SPIRIT.get()) state.fire = new Budget(3);
        else if (marker == FROST_HUNT.get()) state.frost = new Budget(1);
        else if (marker == SPIRIT_MOON.get()) state.healing = new Budget(4);
    }

    private static void apply(LivingEntity target, MobEffect type, int ticks, int amplifier) {
        if (live(target)) target.addEffect(new MobEffectInstance(type, ticks, amplifier, false, false, true));
    }
    private static void extend(LivingEntity target, MobEffect type, int ticks, int minimumAmplifier) {
        MobEffectInstance old = target.getEffect(type);
        apply(target, type, Math.max(ticks, old == null ? 0 : old.getDuration()),
                Math.max(minimumAmplifier, old == null ? 0 : old.getAmplifier()));
    }

    public static boolean reactionDamage(LivingEntity target, SpellAnomalyContext context, float amount, boolean fire) {
        if (!live(target) || amount <= 0) return false;
        ServerLevel world = (ServerLevel)target.level();
        ServerPlayer owner = context == null ? null : world.getServer().getPlayerList().getPlayer(context.casterUUID());
        Entity attacker = owner != null && owner.level() == world ? owner : null;
        Holder<DamageType> holder = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(fire ? FIRE_DAMAGE : MAGIC_DAMAGE);
        DamageSource source = new DamageSource(holder, attacker, attacker);
        boolean[] accepted = {false};
        int[] depth = REACTION_DEPTH.get(); depth[0]++;
        try {
            DomainManager.runAttachedEffect(attacker, target, () -> accepted[0] = target.hurt(source, amount));
        } finally {
            depth[0]--;
        }
        return accepted[0];
    }

    private static boolean live(LivingEntity target) {
        return target != null && target.level() instanceof ServerLevel && target.isAlive()
                && !target.isRemoved() && !target.isSpectator();
    }

    private static void area(LivingEntity origin, SpellAnomalyContext context, double radius, Consumer<LivingEntity> action) {
        ServerLevel world = (ServerLevel)origin.level();
        localArea(world, origin.position(), context, radius, action);
    }
    private static void localArea(ServerLevel world, Vec3 center, SpellAnomalyContext context,
                                  double radius, Consumer<LivingEntity> action) {
        AnomalyManager.recordLocalQuery();
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), target -> eligible(world, center, radius, context, target)))
            action.accept(other);
    }
    private static boolean eligible(ServerLevel world, Vec3 center, double radius,
                                    SpellAnomalyContext context, LivingEntity target) {
        if (!live(target) || target instanceof ArmorStand || target.position().distanceToSqr(center) > radius * radius
                || DomainManager.blocksPath(world, center, target.position(), 0)) return false;
        if (context == null) return !(target instanceof Player);
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(context.casterUUID());
        if (owner == null || owner.level() != world)
            return !(target instanceof Player)
                    && (owner == null || !owner.isAlliedTo(target))
                    && !WhitelistUtils.isProtected(context.casterUUID(), world, target);
        return target != owner && !owner.isAlliedTo(target) && !WhitelistUtils.isProtected(owner, target)
                && (!(target instanceof Player player) || owner.canHarmPlayer(player));
    }

    private record ZoneKey(UUID target, UUID owner) {}
    private record ZoneCell(ServerLevel world, int x, int y, int z) {
        static ZoneCell at(ServerLevel world, Vec3 center) {
            return new ZoneCell(world, (int)Math.floor(center.x / 6),
                    (int)Math.floor(center.y / 6), (int)Math.floor(center.z / 6));
        }
        AABB bounds() { return new AABB(x * 6.0 - 3, y * 6.0 - 3, z * 6.0 - 3,
                (x + 1) * 6.0 + 3, (y + 1) * 6.0 + 3, (z + 1) * 6.0 + 3); }
    }
    private static final class Zone {
        final LivingEntity anchor; final ServerLevel world; final Vec3 center; final SpellAnomalyContext context;
        final ZoneCell cell;
        final long until; long next; boolean cancelled;
        Zone(LivingEntity target, SpellAnomalyContext context) {
            anchor = target; world = (ServerLevel)target.level(); center = target.position(); this.context = context;
            cell = ZoneCell.at(world, center);
            next = now(world) + 20; until = now(world) + 100;
        }
    }

    private static void fireZone(LivingEntity target, SpellAnomalyContext context) {
        if (context == null) return;
        apply(target, FIRE_ZONE.get(), 100, 0);
        if (!target.hasEffect(FIRE_ZONE.get())) return;
        ZONES.put(new ZoneKey(target.getUUID(), context.casterUUID()), new Zone(target, context));
    }
    private static void tickZones() {
        if (ZONES.isEmpty()) return;
        Map<ZoneCell, List<LivingEntity>> candidates = null;
        var iterator = ZONES.values().iterator();
        while (iterator.hasNext()) {
            Zone zone = iterator.next(); long now = now(zone.world);
            if (zone.cancelled || !live(zone.anchor) || zone.anchor.level() != zone.world
                    || now > zone.until || !zone.anchor.hasEffect(FIRE_ZONE.get()) && now < zone.until) {
                iterator.remove(); continue;
            }
            if (now >= zone.next && now <= zone.until) {
                zone.next += 20;
                AnomalyReactionFeedback.fireZonePulse(zone.anchor, zone.center);
                if (candidates == null) candidates = new HashMap<>();
                List<LivingEntity> nearby = candidates.get(zone.cell);
                if (nearby == null) {
                    AnomalyManager.recordLocalQuery();
                    nearby = zone.world.getEntitiesOfClass(LivingEntity.class, zone.cell.bounds(), LivingEntity::isAlive);
                    candidates.put(zone.cell, nearby);
                }
                for (LivingEntity other : nearby) {
                    if (!eligible(zone.world, zone.center, 3, zone.context, other)) continue;
                    var access = (AnomalyEffectAccess)other;
                    long last = access.ssca$lastAnomalyZoneHitTick();
                    if (last != Long.MIN_VALUE && now >= last && now - last < 20) continue;
                    if (reactionDamage(other, zone.context, 1, true)) access.ssca$lastAnomalyZoneHitTick(now);
                }
            }
            if (zone.cancelled || now >= zone.until) iterator.remove();
        }
        if (candidates != null) candidates.clear();
    }
    private static long now(ServerLevel world) { return AnomalyManager.now(world.getServer()); }
    private static void cancelZones(LivingEntity target) {
        for (Zone zone : ZONES.values()) if (zone.anchor == target) zone.cancelled = true;
    }

    public static void clear(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel)) return;
        ((AnomalyEffectAccess)target).ssca$lastAnomalyZoneHitTick(Long.MIN_VALUE);
        if (!hasState(target)) {
            boolean loadedMarker = false;
            for (MobEffectInstance effect : target.getActiveEffects())
                if (isManaged(effect.getEffect())) { loadedMarker = true; break; }
            if (!loadedMarker) return;
        }
        for (MobEffect effect : EFFECTS.values()) target.removeEffect(effect);
        target.removeEffect(AddonEffects.FROZEN_RECOVERY.get());
        FrozenControl.clear(target);
        ((AnomalyEffectAccess)target).ssca$anomalyEffects(null);
        var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW_MODIFIER);
        ACTIVE.remove(target); cancelZones(target);
    }

    public static void clearAll() {
        for (LivingEntity target : ACTIVE.keySet().toArray(LivingEntity[]::new)) clear(target);
        ACTIVE.clear(); ZONES.clear(); PROCS.clear(); REACTION_DEPTH.remove();
        AnomalyReactionFeedback.clearAll();
    }
}
