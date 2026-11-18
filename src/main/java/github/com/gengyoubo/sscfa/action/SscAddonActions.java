package github.com.gengyoubo.sscfa.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpDeathDomain;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpSummonWolves;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormCounterBurst;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormDetonate;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormErosionBrand;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormWitherSand;
import github.com.gengyoubo.sscfa.ability.KillEmpowerManager;
import github.com.gengyoubo.sscfa.ability.NovaSkillManager;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpFrostStorm;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpMeleeAbility;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpTeleportAttack;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.entity.FrostBallEntity;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.network.VisualRecipe;
import github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import github.com.gengyoubo.sscfa.util.SscIgnitedEntityAccessor;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.onixary.shapeShifterCurseForge.api.registry.SscAction;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The addon's custom JSON action types, registered with SSC Forge by their {@code "type"} id.
 *
 * <p>Port of the Fabric {@code SscAddonActions}. Apoli kept entity and bi-entity actions in
 * separate registries; SSC Forge has one, called with {@code (actor, target)}. For an entity
 * action {@code target} is the entity the action runs on (the actor itself unless the JSON
 * targets someone else). Bi-entity actions reach this registry through
 * {@code mixin.FormPowerRuntimeBiEntityMixin}.</p>
 */
public final class SscAddonActions {

	/** Play-dead length (6 s); the HUD's remaining-time bar uses the same constant. */
	public static final int PLAY_DEAD_DURATION_TICKS = 120;
	/** Play-dead skill id: the key-bound child power ({@code <parent>_<childKey>}). */
	public static final String PLAY_DEAD_SKILL_ID = "my_addon:form_axolotl_sp_play_dead_activate";

	private static final BalanceReader FROST_BALL_BAL = new BalanceReader("abilities.frost_ball");
	/** Default 5 s frost regen lock after a frost ball; balance {@code regen_lock_ticks} overrides it. */
	public static final int FROST_BALL_REGEN_LOCK_TICKS = 100;
	private static final int FROST_BALL_MANA_COST = 15;

	private static boolean registered;

	private SscAddonActions() {}

	public static synchronized void register() {
		if (registered) return;
		registered = true;

		bi("ssc_addon", "bat_echo_feedback", (actor, target, data) ->
				github.com.gengyoubo.sscfa.compat.BatEchoFeedback.play(actor, target,
						intOf(data, "max_targets", 8), floatOf(data, "volume", .35f)));
		github.com.gengyoubo.sscfa.compat.BatEchoFeedback.registerLifecycle();

		skillLifecycle("skill_begin", (player, id) -> SkillCooldowns.begin(player, id));
		skillLifecycle("skill_released", SkillCooldowns::released);
		skillLifecycle("skill_completed", SkillCooldowns::completed);
		skillLifecycle("skill_ended", SkillCooldowns::ended);
		skillLifecycle("skill_failed", SkillCooldowns::failed);
		skillLifecycle("skill_cancelled", SkillCooldowns::cancelled);

		entity("my_addon", "self_decoration", (actor, target, data) -> {
			JsonObject inner = objectOf(data, "entity_action");
			if (inner != null) github.com.gengyoubo.sscfa.network.DecorationParticleScope.run(target,
					() -> net.onixary.shapeShifterCurseForge.power.FormPowerRuntime.execute(actor, target, inner));
		});

		entity("my_addon", "fallen_allay_scream", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) fallenAllayScream(player);
		});
		entity("my_addon", "summon_fallen_allay_vex", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) summonFallenAllayVex(player);
		});
		entity("my_addon", "fallen_allay_vex_watchdog", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) github.com.gengyoubo.sscfa.ability.FallenAllayVexSkill.watchdog(player);
		});
		entity("ssc_addon", "phantom_bell_teleport", (actor, target, data) -> PhantomBellTeleportAction.execute(target));
		// Item cooldown (phantom bell, lifesaving cat tail, ...): read back by ssc_addon:item_on_cooldown.
		entity("ssc_addon", "item_cooldown", (actor, target, data) -> {
			Item item = itemOf(data, "item");
			if (target instanceof Player player && item != null) player.getCooldowns().addCooldown(item, intOf(data, "duration", 0));
		});

		entity("ssc_addon", "nightmare_fear", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) github.com.gengyoubo.sscfa.ability.NightmareFearManager.execute(player);
		});
		entity("ssc_addon", "nightmare_spook", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) github.com.gengyoubo.sscfa.ability.NightmareSpookManager.execute(player);
		});

		entity("my_addon", "fire_breath", (actor, target, data) -> fireBreath(target, data));

		bi("ssc_addon", "salticidae_venom_fang", (actor, target, data) -> {
			// Jumping spider passive: hits poison for 5 s; the venom gland raises it to level II for 3 s.
			if (!(actor instanceof ServerPlayer player) || target == actor) return;
			boolean gland = github.com.gengyoubo.sscfa.item.VenomGlandItem.isWearingBy(player);
			target.addEffect(new MobEffectInstance(MobEffects.POISON, gland ? 60 : 100, gland ? 1 : 0,
					false, true, true), player);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.6f);
		});
		bi("my_addon", "npc_purification_debuffs", (actor, target, data) ->
				github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements.applyPurification(actor, target));
		bi("my_addon", "damage_target_from_actor", (actor, target, data) -> damageFromActor(actor, target, data));

		entity("ssc_addon", "snow_fox_sp_dash", (actor, target, data) -> {
			if (!(target instanceof ServerPlayer player)) return;
			if (SkillBlocker.isSkillBlocked(player, "snow_fox", "melee_primary")) { FailAwareActiveSelfPower.markFail(); return; }
			SnowFoxSpMeleeAbility.execute(player);
		});
		entity("ssc_addon", "snow_fox_sp_teleport_attack", (actor, target, data) -> {
			if (!(target instanceof ServerPlayer player)) return;
			if (SkillBlocker.isSkillBlocked(player, "snow_fox", "melee_secondary")) { FailAwareActiveSelfPower.markFail(); return; }
			SnowFoxSpTeleportAttack.execute(player);
		});
		entity("ssc_addon", "snow_fox_sp_frost_ball", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) frostBall(player);
		});
		entity("ssc_addon", "snow_fox_sp_frost_storm", (actor, target, data) -> {
			if (!(target instanceof ServerPlayer player)) return;
			if (SkillBlocker.isSkillBlocked(player, "snow_fox", "ranged_secondary")) { FailAwareActiveSelfPower.markFail(); return; }
			SnowFoxSpFrostStorm.startCharging(player);
		});

		entity("ssc_addon", "nova_charge", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) NovaSkillManager.startCharge(player);
		});
		entity("ssc_addon", "nova_leap", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) NovaSkillManager.tryLeap(player);
		});

		entity("ssc_addon", "trigger_play_dead", (actor, target, data) -> playDead(target));
		entity("my_addon", "swim_jump_out", (actor, target, data) -> swimJumpOut(target, floatOf(data, "multiplier", 2.0f)));

		entity("ssc_addon", "anubis_wolf_sp_death_domain", (actor, target, data) -> {
			if (target instanceof ServerPlayer player && !SkillBlocker.isSkillBlocked(player, "anubis_wolf", "death_domain"))
				AnubisWolfSpDeathDomain.execute(player);
		});
		entity("ssc_addon", "anubis_wolf_sp_summon_wolves", (actor, target, data) -> {
			if (target instanceof ServerPlayer player && !SkillBlocker.isSkillBlocked(player, "anubis_wolf", "summon_wolves"))
				AnubisWolfSpSummonWolves.execute(player);
		});
		bi("ssc_addon", "anubis_wolf_sp_passive_summon", (actor, target, data) ->
				github.com.gengyoubo.sscfa.compat.AnubisWolfSpPerks.summonPassive(actor, target, boolOf(data, "reverse", false)));
		entity("ssc_addon", "anubis_wolf_sp_soul_recall", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) github.com.gengyoubo.sscfa.compat.AnubisWolfSpPerks.recall(player);
		});

		entity("ssc_addon", "golden_sandstorm_brand_recall", (actor, target, data) -> {
			if (target instanceof ServerPlayer player && !github.com.gengyoubo.sscfa.compat.GoldenSandstormPerks.recall(player))
				FailAwareActiveSelfPower.markFail();
		});
		bi("ssc_addon", "golden_sandstorm_erosion_brand_hit", (actor, target, data) -> {
			if (actor instanceof ServerPlayer player && target != actor) GoldenSandstormErosionBrand.onPlayerAttack(player, target);
		});
		entity("ssc_addon", "golden_sandstorm_wither_sand", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) GoldenSandstormWitherSand.execute(player);
		});
		entity("ssc_addon", "golden_sandstorm_detonate", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) GoldenSandstormDetonate.execute(player);
		});
		entity("ssc_addon", "golden_sandstorm_counter_burst", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) GoldenSandstormCounterBurst.execute(player);
		});

		entity("my_addon", "red_fire_ring_visual", (actor, target, data) -> {
			var kind = !boolOf(data, "amulet", false) ? VisualRecipe.Kind.RED_RING
					: boolOf(data, "empowered", false) ? VisualRecipe.Kind.EMPOWERED_AMULET_RING
					: VisualRecipe.Kind.AMULET_RING;
			github.com.gengyoubo.sscfa.network.SustainedVisuals.touch(target, kind, target.tickCount, 0,
					doubleOf(data, "radius", 6.0), 0);
		});

		// Red fox fireball: launches the projectile plus a 60° x 4-block point-blank cone (5 magic damage).
		entity("my_addon", "fox_fireball", (actor, target, data) -> {
			if (target instanceof ServerPlayer player) launchFoxFireball(player, false);
		});
	}

	public static void launchFoxFireball(ServerPlayer player, boolean empowered) {
		ServerLevel world = player.serverLevel();
		Vec3 look = player.getViewVector(1.0F);
		var ball = new github.com.gengyoubo.sscfa.entity.FoxFireballEntity(world, player);
		ball.setEmpowered(empowered);
		ball.setDirection(look);
		world.addFreshEntity(ball);
		Vec3 eye = player.getEyePosition();
		AABB box = player.getBoundingBox().inflate(4.0);
		world.getEntitiesOfClass(LivingEntity.class, box,
				target -> target != player && target.isAlive() && !target.isSpectator()).forEach(target -> {
			if (WhitelistUtils.isProtected(player, target)) return;
			Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2.0, 0).subtract(eye).normalize();
			if (look.dot(toTarget) > 0.5 && player.distanceToSqr(target) < 16.0) {
				target.invulnerableTime = 0;
				float damage = target instanceof Player ? 5.0f : 5.0f * 1.5f;
				KillEmpowerManager.damage(target, damageSource(target, "minecraft:magic", player, player),
						damage, !empowered);
			}
		});
	}

	// ---- action bodies ----

	private static void fallenAllayScream(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		// Owner-tagged particles so the caster's first-person view can skip them.
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 1.2f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.forced(world, player, net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
				player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.5, 0.5, 0.5, 0.1, 512);
		AABB box = player.getBoundingBox().inflate(25.0);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
			// Always skip: own pets, own vexes, raiders.
			if (e instanceof net.minecraft.world.entity.TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID())) continue;
			if (e instanceof net.minecraft.world.entity.monster.Vex vex && vex.getTags().contains("owner:" + player.getStringUUID())) continue;
			if (e instanceof net.minecraft.world.entity.raid.Raider) continue;
			if (WhitelistUtils.isProtected(player, e)) continue;
			github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements.applyFallenScream(player, e, 160);
		}
		for (Entity p : world.getEntities(player, box, e -> e instanceof net.minecraft.world.entity.projectile.Projectile)) {
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(world, net.minecraft.core.particles.ParticleTypes.SMOKE,
					p.position(), 5, 0.1, 0.1, 0.1, 0.05);
			p.discard();
		}
	}

	private static void summonFallenAllayVex(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		boolean hasVex = false;
		for (Entity e : world.getEntitiesOfClass(net.minecraft.world.entity.monster.Vex.class, player.getBoundingBox().inflate(128.0), v -> true)) {
			if (e.getTags().contains("owner:" + player.getStringUUID()) && e.getTags().contains("ssc_fallen_allay_vex")) {
				hasVex = true;
				break;
			}
		}
		if (!hasVex) {
			for (int i = 0; i < 2; i++) {
				var vex = net.minecraft.world.entity.EntityType.VEX.create(world);
				if (vex == null) continue;
				vex.moveTo(player.getX(), player.getEyeY(), player.getZ(), player.getYRot(), player.getXRot());
				// Two vexes 180° apart with a small push so they do not overlap.
				double angle = i * Math.PI;
				vex.setDeltaMovement(Math.cos(angle) * 0.3, 0.3, Math.sin(angle) * 0.3);
				vex.addTag("ssc_fallen_allay_vex");
				vex.addTag("owner:" + player.getStringUUID());
				vex.setLimitedLife(700); // 35 s
				world.addFreshEntity(vex);
			}
		}
		github.com.gengyoubo.sscfa.ability.FallenAllayVexSkill.onSummoned(player);
	}

	private static void fireBreath(LivingEntity living, JsonObject data) {
		float distance = floatOf(data, "distance", 0);
		float damageAmount = floatOf(data, "damage", 0);
		// Multiplier keyed on whether the TARGET is a player.
		float nonPlayerMult = floatOf(data, "non_player_multiplier", 1.0f);
		int duration = intOf(data, "duration", 100);
		boolean bypassIframes = boolOf(data, "bypass_iframes", false);
		boolean grantsEmpower = boolOf(data, "grants_empower", false);

		Vec3 eyePos = living.getEyePosition();
		Vec3 lookVec = living.getViewVector(1.0F);
		AABB box = living.getBoundingBox().inflate(distance).expandTowards(lookVec.scale(distance));
		living.level().getEntitiesOfClass(LivingEntity.class, box, t -> t != living).forEach(target -> {
			if (living instanceof ServerPlayer sPlayer && WhitelistUtils.isProtected(sPlayer, target)) return;
			Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eyePos).normalize();
			if (lookVec.dot(targetVec) <= 0.8 || living.distanceToSqr(target) >= distance * distance) return;
			float finalDamage = target instanceof Player ? damageAmount : damageAmount * nonPlayerMult;
			Vec3 oldVelocity = target.getDeltaMovement();
			if (bypassIframes) target.invulnerableTime = 0; // before: not eaten by melee i-frames
			if (KillEmpowerManager.damage(target, damageSource(target, "minecraft:magic", living, living),
					finalDamage, grantsEmpower)) {
				target.setDeltaMovement(oldVelocity);
				if (bypassIframes) target.invulnerableTime = 0; // after: leave no i-frames behind
			}
			// Source = caster so dream blocking can attribute it.
			target.addEffect(new MobEffectInstance(AddonEffects.FOX_FIRE_BURN.get(), duration, 0), living);
			if (living instanceof ServerPlayer player && grantsEmpower) KillEmpowerManager.trackBurn(player, target, duration, true);
			if (living instanceof Player player && target instanceof SscIgnitedEntityAccessor accessor)
				accessor.sscAddon$setIgniterUuid(player.getUUID());
		});
		// Water along the breath has a 15% chance per block to become frosted ice.
		if (living.level() instanceof ServerLevel serverWorld) freezeWaterInCone(serverWorld, eyePos, lookVec, distance, 0.15f);
	}

	private static void damageFromActor(LivingEntity actor, LivingEntity target, JsonObject data) {
		if (actor == null || target == null) return;
		float amount = floatOf(data, "amount", 0);
		if (!(target instanceof Player)) amount *= floatOf(data, "non_player_multiplier", 1.0f);
		String type = stringOf(data, "damage_type", "minecraft:generic");
		boolean grantsEmpower = boolOf(data, "grants_empower", false);
		Vec3 oldVelocity = target.getDeltaMovement();
		if (KillEmpowerManager.damage(target, damageSource(target, type, null, actor), amount, grantsEmpower))
			target.setDeltaMovement(oldVelocity);
		int burn = intOf(data, "empower_burn_ticks", 0);
		if (grantsEmpower && actor instanceof ServerPlayer player && burn > 0) KillEmpowerManager.trackBurn(player, target, burn, true);
	}

	private static void frostBall(ServerPlayer player) {
		if (SkillBlocker.isSkillBlocked(player, "snow_fox", "ranged_primary")) { FailAwareActiveSelfPower.markFail(); return; }
		// The power JSON's fail_aware cooldown gates the cast; frost (mana) is checked here.
		int currentMana = PowerUtils.getResourceValue(player, FormIdentifiers.SNOW_FOX_RESOURCE);
		if (currentMana < FROST_BALL_MANA_COST) {
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			FailAwareActiveSelfPower.markFail();
			return;
		}
		PowerUtils.changeResourceValueAndSync(player, FormIdentifiers.SNOW_FOX_RESOURCE, -FROST_BALL_MANA_COST);
		PowerUtils.setResourceValueAndSync(player, FormIdentifiers.SNOW_FOX_REGEN_COOLDOWN,
				FROST_BALL_BAL.i("regen_lock_ticks", FROST_BALL_REGEN_LOCK_TICKS));
		FrostBallEntity frostBall = new FrostBallEntity(player.level(), player);
		frostBall.setDirection(player.getViewVector(1.0F));
		player.level().addFreshEntity(frostBall);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0f, 0.8f);
	}

	private static void playDead(LivingEntity living) {
		int duration = PLAY_DEAD_DURATION_TICKS;
		// Hidden icon; healing and the amulet's absorption are settled by PlayingDeadEffect every 10 ticks.
		if (!living.addEffect(new MobEffectInstance(AddonEffects.PLAYING_DEAD.get(), duration, 0, false, false, false))) {
			FailAwareActiveSelfPower.markFail();
			return;
		}
		living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
		living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 10, false, false));
		AABB box = living.getBoundingBox().inflate(64.0);
		living.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, mob -> mob.getTarget() == living).forEach(mob -> {
			mob.setTarget(null);
			mob.setLastHurtByMob(null);
		});
		living.setPose(Pose.SWIMMING);
		living.setSwimming(true);
	}

	private static void swimJumpOut(LivingEntity entity, float multiplier) {
		// Only while sprint-swimming and moving upward.
		if (!entity.isSwimming()) return;
		Vec3 velocity = entity.getDeltaMovement();
		double vy = velocity.y;
		if (vy <= 0) return;
		double newVy;
		if (entity.getXRot() < -20.0f) {
			// Looking up: explosive jump out of the water, clamped to roughly 7..12 blocks of height.
			newVy = Mth.clamp(vy * multiplier, 1.1, 1.7);
		} else {
			newVy = vy * 1.5;
		}
		entity.setDeltaMovement(velocity.x * 1.5, newVy, velocity.z * 1.5);
		entity.hurtMarked = true;
	}

	/** Water source blocks in the breath cone turn into frosted ice with the given chance. */
	private static void freezeWaterInCone(ServerLevel world, Vec3 origin, Vec3 direction, float distance, float chance) {
		for (float d = 1.0f; d <= distance; d += 1.0f) {
			Vec3 center = origin.add(direction.scale(d));
			int radius = Math.max(1, (int) (d * 0.375f));
			int cx = Mth.floor(center.x), cy = Mth.floor(center.y), cz = Mth.floor(center.z);
			for (int x = -radius; x <= radius; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -radius; z <= radius; z++) {
						if (x * x + z * z > radius * radius) continue;
						BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
						if (world.getBlockState(pos).is(Blocks.WATER) && world.getFluidState(pos).isSource()
								&& world.getBlockState(pos.above()).isAir() && world.getRandom().nextFloat() < chance) {
							world.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
							world.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(world.getRandom(), 60, 120));
						}
					}
				}
			}
		}
	}

	// ---- registration helpers ----

	/** Entity action: runs on {@code target} (the actor unless the JSON redirects it). */
	private static void entity(String namespace, String path, SscAction action) {
		SscJavaRegistries.registerAction(ResourceLocation.fromNamespaceAndPath(namespace, path), action);
	}

	/** Bi-entity action: {@code actor} is the power holder, {@code target} the other entity. */
	private static void bi(String namespace, String path, SscAction action) {
		SscJavaRegistries.registerAction(ResourceLocation.fromNamespaceAndPath(namespace, path), action);
	}

	/** Cooldown lifecycle step for pure-JSON skills: {"type":"my_addon:skill_xxx","power":"<skill id>"}. */
	private static void skillLifecycle(String name, BiConsumer<ServerPlayer, String> step) {
		entity("my_addon", name, (actor, target, data) -> {
			String power = stringOf(data, "power", null);
			if (target instanceof ServerPlayer player && power != null) step.accept(player, power);
		});
	}

	/** {@code DamageSources.source} is private on Forge; build the source from the registry holder. */
	private static net.minecraft.world.damagesource.DamageSource damageSource(Entity victim, String type, Entity direct, Entity causing) {
		var registry = victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var holder = registry.getHolder(damageKey(type))
				.orElseGet(() -> registry.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC));
		return new net.minecraft.world.damagesource.DamageSource(holder, direct, causing);
	}

	private static ResourceKey<DamageType> damageKey(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		return ResourceKey.create(Registries.DAMAGE_TYPE, location == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "generic") : location);
	}

	private static <T> T get(
			JsonObject data,
			String key,
			T fallback,
			Function<JsonElement, T> converter
	) {
		if (data == null) return fallback;

		JsonElement element = data.get(key);
		if (element == null || element.isJsonNull()) {
			return fallback;
		}

		try {
			return converter.apply(element);
		} catch (IllegalStateException | NumberFormatException e) {
			return fallback;
		}
	}
	static JsonObject objectOf(JsonObject data, String key) {
		return get(data, key, null,
				e -> e.isJsonObject() ? e.getAsJsonObject() : null);
	}

	static String stringOf(JsonObject data, String key, String fallback) {
		return get(data, key, fallback,
				e -> e.isJsonPrimitive() ? e.getAsString() : fallback);
	}

	static int intOf(JsonObject data, String key, int fallback) {
		return get(data, key, fallback,
				e -> e.isJsonPrimitive() ? e.getAsInt() : fallback);
	}

	static float floatOf(JsonObject data, String key, float fallback) {
		return get(data, key, fallback,
				e -> e.isJsonPrimitive() ? e.getAsFloat() : fallback);
	}

	static double doubleOf(JsonObject data, String key, double fallback) {
		return get(data, key, fallback,
				e -> e.isJsonPrimitive() ? e.getAsDouble() : fallback);
	}

	static boolean boolOf(JsonObject data, String key, boolean fallback) {
		return get(data, key, fallback,
				e -> e.isJsonPrimitive() ? e.getAsBoolean() : fallback);
	}

	/** Item field, either {@code "id"} or Apoli's {@code {"item": "id"}} form. */
	static Item itemOf(JsonObject data, String key) {
		return get(data, key, null, e -> {
			if (e.isJsonObject()) {
				e = e.getAsJsonObject().get("item");
			}

			if (e == null || !e.isJsonPrimitive()) {
				return null;
			}

			ResourceLocation id =
					ResourceLocation.tryParse(e.getAsString());

			return id == null
					? null
					: ForgeRegistries.ITEMS.getValue(id);
		});
	}
}
