package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.util.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AxolotlShifterEntity extends PathfinderMob implements GeoEntity {

	private static final int BURST_COOLDOWN_MAX = 160;
	private static final int BURST_POWER = 2;
	private static final float BURST_RADIUS = BURST_POWER * 2.0f;
	private static final int BURST_MOISTURE_COST = 15;
	private static final TagKey<EntityType<?>> WATER_EXPLODE_IMMUNE =
			TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shape-shifter-curse", "water_explode_immune"));

	public static final int MAX_MOISTURE = 300;
	private static final int MOISTURE_DRAIN_PER_SEC = 3;
	private static final int MOISTURE_REGEN_PER_TICK = 4;
	private static final int DEHYDRATION_DAMAGE = 2;
	private static final int FIND_WATER_THRESHOLD = 180;
	private static final int SEEK_WATER_RANGE = 24;
	private static final int NEAR_WATER_RADIUS = 6;

	private static final int RETREAT_MOISTURE_THRESHOLD = 90;
	private static final int RETREAT_RECOVER_MOISTURE = 240;

	private static final int PANIC_DURATION = 200;

	private static final int EAT_SCAN_RANGE = 10;
	private static final int EAT_MOISTURE_GAIN = 60;
	private static final float EAT_HEAL = 4.0f;
	private static final int EAT_COOLDOWN_MIN = 600;
	private static final int EAT_COOLDOWN_MAX = 1200;

	private static final UUID COMBAT_SPEED_UUID = UUID.fromString("b7f3c2a1-4d5e-6f70-8192-a3b4c5d6e7f8");
	private static final double COMBAT_SPEED_BOOST = 0.3;

	private static final int POUNCE_MOISTURE_COST = 10;

	private static final UUID MOISTURE_HEALTH_UUID = UUID.fromString("8e6e6b3a-1c2d-4e5f-9a8b-7c6d5e4f3a2b");

	private static final ResourceKey<DamageType> ATTACK_DAMAGE_KEY =
			ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("ssc_addon", "axolotl_shifter_attack"));
	private static final ResourceKey<DamageType> THIRST_DAMAGE_KEY =
			ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("ssc_addon", "axolotl_shifter_thirst"));

	private static final DustParticleOptions DEEP_BLUE_DUST =
			new DustParticleOptions(new Vector3f(0.11f, 0.18f, 0.69f), 2.0f);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	private int moisture = MAX_MOISTURE;
	private int moistureBonus = Integer.MIN_VALUE;
	private int burstCooldown = 0;
	private int panicTicks = 0;
	private int eatCooldown = 0;
	private boolean combatSpeedApplied = false;
	private boolean recoveringMoisture;

	public AxolotlShifterEntity(EntityType<? extends AxolotlShifterEntity> type, Level level) {
		super(type, level);
		this.moveControl = new ShifterMoveControl(this);
		this.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new ShifterNavigation(this, level);
	}

	public static AttributeSupplier.Builder createAxolotlShifterAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ARMOR, 5.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.FOLLOW_RANGE, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.ATTACK_KNOCKBACK, 0.0);
	}

	public static boolean canSpawnInWater(EntityType<? extends AxolotlShifterEntity> type, ServerLevelAccessor level,
	                                      MobSpawnType reason, BlockPos pos, RandomSource random) {
		return level.getFluidState(pos).is(FluidTags.WATER)
				&& level.getBlockState(pos.below()).isSolidRender(level, pos.below());
	}

	public int getMoisture() {
		return this.moisture;
	}

	public boolean isPanicking() {
		return this.panicTicks > 0;
	}

	@Nullable
	private BlockPos findNearestWater(int rangeH, int rangeV) {
		BlockPos center = this.blockPosition();
		for (BlockPos pos : BlockPos.withinManhattan(center, rangeH, rangeV, rangeH)) {
			if (this.level().getFluidState(pos).is(FluidTags.WATER)) {
				return pos.immutable();
			}
		}
		return null;
	}

	@Nullable
	private Path findWaterPath(int rangeH, int rangeV, Set<BlockPos> rejected) {
		Set<BlockPos> candidates = new HashSet<>();
		for (BlockPos pos : BlockPos.withinManhattan(this.blockPosition(), rangeH, rangeV, rangeH)) {
			if (!this.level().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || rejected.contains(pos)
					|| !this.level().getFluidState(pos).is(FluidTags.WATER)) continue;
			Vec3 feet = Vec3.atBottomCenterOf(pos);
			if (this.level().noCollision(this, this.getBoundingBox().move(feet.subtract(this.position())))) {
				candidates.add(pos.immutable());
			}
		}
		Path path = this.getNavigation().createPath(candidates, 0);
		return path != null && path.canReach() ? path : null;
	}

	private boolean wanderTowardYaw(float yaw, double speed) {
		double rad = Math.toRadians(yaw);
		Vec3 prefer = this.position().add(-Math.sin(rad) * 12.0, 0.0, Math.cos(rad) * 12.0);
		Vec3 target = DefaultRandomPos.getPosTowards(this, 12, 7, prefer, (float) (Math.PI / 2));
		if (target == null) target = DefaultRandomPos.getPos(this, 10, 5);
		if (target == null) return false;
		Path path = this.getNavigation().createPath(BlockPos.containing(target), 0);
		return path != null && path.canReach() && this.getNavigation().moveTo(path, speed);
	}

	private void setCombatSpeedBoost(boolean active) {
		if (active == this.combatSpeedApplied) return;
		AttributeInstance attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr == null) return;
		attr.removeModifier(COMBAT_SPEED_UUID);
		if (active) {
			attr.addTransientModifier(new AttributeModifier(COMBAT_SPEED_UUID,
					"Axolotl shifter combat speed", COMBAT_SPEED_BOOST, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		this.combatSpeedApplied = active;
	}

	private void eatGlowBerryAt(BlockPos pos) {
		BlockState state = this.level().getBlockState(pos);
		if (!CaveVines.hasGlowBerries(state)) return;
		this.level().setBlock(pos, state.setValue(CaveVines.BERRIES, false), Block.UPDATE_CLIENTS);
		this.level().playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES,
				SoundSource.NEUTRAL, 1.0f, 0.8f + this.getRandom().nextFloat() * 0.4f);
		this.moisture = Math.min(MAX_MOISTURE, this.moisture + EAT_MOISTURE_GAIN);
		this.heal(EAT_HEAL);
		if (this.level() instanceof ServerLevel serverLevel) {
			ParticleUtils.spawnParticles(serverLevel, ParticleTypes.HAPPY_VILLAGER,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new PanicToWaterGoal(this));
		this.goalSelector.addGoal(1, new CombatRetreatToWaterGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(3, new SeekWaterGoal(this));
		this.goalSelector.addGoal(4, new EatGlowBerriesGoal(this));
		this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 10) {
			@Override public boolean canUse() {
				return isInWater() && getTarget() == null && !recoveringMoisture && super.canUse();
			}
			@Override public boolean canContinueToUse() {
				return isInWater() && getTarget() == null && !recoveringMoisture && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
			@Override public boolean canUse() {
				return !isInWater() && getTarget() == null && !recoveringMoisture && super.canUse();
			}
			@Override public boolean canContinueToUse() {
				return !isInWater() && getTarget() == null && !recoveringMoisture && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, this::shouldHuntTarget));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AxolotlShifterEntity.class).setAlertOthers(AxolotlShifterEntity.class));
	}

	private boolean shouldHuntTarget(LivingEntity target) {
		if (target instanceof AxolotlShifterEntity) return false;
		if (!target.isInWater()) return false;
		return target.isAlive();
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();

		if (this.burstCooldown > 0) this.burstCooldown--;
		if (this.panicTicks > 0) this.panicTicks--;
		if (this.eatCooldown > 0) this.eatCooldown--;

		LivingEntity target0 = this.getTarget();
		if (this.isPanicking()) {
			this.setSprinting(true);
			setCombatSpeedBoost(false);
		} else if (this.recoveringMoisture) {
			this.setSprinting(target0 != null && !this.isInWater());
			setCombatSpeedBoost(false);
		} else if (target0 == null) {
			this.setSprinting(false);
			setCombatSpeedBoost(false);
		} else {
			setCombatSpeedBoost(true);
			double distSq = this.distanceToSqr(target0);
			if (distSq >= 36.0) {
				this.setSprinting(true);
			} else if (distSq <= 9.0) {
				this.setSprinting(false);
			}
		}

		if (this.level().isClientSide()) return;
		if (!(this.level() instanceof ServerLevel serverLevel)) return;

		if (this.isOnFire() && !this.isInWater()) {
			this.panicTicks = Math.max(this.panicTicks, 60);
		}
		if (this.panicTicks > 0 && this.isInWater()) {
			this.panicTicks = 0;
		}

		boolean inWater = this.isInWater();
		boolean rained = this.level().isRaining() && this.level().isRainingAt(this.blockPosition());
		if (inWater || rained) {
			if (this.moisture < MAX_MOISTURE) {
				this.moisture = Math.min(MAX_MOISTURE, this.moisture + MOISTURE_REGEN_PER_TICK);
			}
		} else if (this.tickCount % 20 == 0) {
			boolean nearWater = findNearestWater(NEAR_WATER_RADIUS, 4) != null;
			if (!nearWater || this.getRandom().nextFloat() >= 0.8f) {
				this.moisture = Math.max(0, this.moisture - MOISTURE_DRAIN_PER_SEC);
				if (this.moisture <= 0) {
					DamageSource thirst = new DamageSource(serverLevel.registryAccess()
							.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(THIRST_DAMAGE_KEY));
					this.hurt(thirst, DEHYDRATION_DAMAGE);
				}
			}
		}
		if (this.tickCount % 20 == 0) {
			updateMoistureHealth();
		}

		if (this.tickCount % 5 == 0 && inWater && this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
			ParticleUtils.spawnParticles(serverLevel, ParticleTypes.NAUTILUS,
					this.getX(), this.getY() + 0.5, this.getZ(), 2, 0.5, 0.3, 0.5, 0.0);
			ParticleUtils.spawnParticles(serverLevel, ParticleTypes.SPLASH,
					this.getX(), this.getY() + 0.3, this.getZ(), 2, 0.5, 0.1, 0.5, 0.0);
		}

		if (this.burstCooldown <= 0 && this.panicTicks <= 0 && !this.recoveringMoisture && this.getTarget() != null
				&& this.hasLineOfSight(this.getTarget())
				&& this.distanceToSqr(this.getTarget()) <= BURST_RADIUS * BURST_RADIUS) {
			useWaterBurst();
			this.burstCooldown = BURST_COOLDOWN_MAX;
		}
	}

	private void updateMoistureHealth() {
		int m = this.moisture;
		int bonus = m >= 270 ? 6 : m >= 240 ? 4 : m >= 210 ? 2 : m >= 120 ? 0
				: m >= 90 ? -4 : m >= 60 ? -6 : m >= 30 ? -8 : -10;
		if (bonus == this.moistureBonus) return;
		int oldBonus = this.moistureBonus;
		this.moistureBonus = bonus;

		AttributeInstance attr = this.getAttribute(Attributes.MAX_HEALTH);
		if (attr == null) return;
		attr.removeModifier(MOISTURE_HEALTH_UUID);
		attr.addTransientModifier(new AttributeModifier(MOISTURE_HEALTH_UUID,
				"Axolotl shifter moisture health", bonus, AttributeModifier.Operation.ADDITION));
		if (this.getHealth() > this.getMaxHealth()) {
			this.setHealth(this.getMaxHealth());
		} else if (oldBonus == Integer.MIN_VALUE) {
			this.setHealth(this.getMaxHealth());
		} else if (bonus > oldBonus) {
			this.heal(bonus - oldBonus);
		}
	}

	private void useWaterBurst() {
		if (!(this.level() instanceof ServerLevel serverLevel)) return;

		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();

		serverLevel.playSound(null, x, y, z, SoundEvents.AXOLOTL_SPLASH, SoundSource.NEUTRAL, 0.5f, 0.8f);
		serverLevel.playSound(null, x, y, z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.NEUTRAL, 0.5f, 0.8f);

		ParticleUtils.spawnParticles(serverLevel, ParticleTypes.RAIN, x, y + 0.5, z, 32, 2.0, 0.5, 2.0, 0.0);
		ParticleUtils.spawnParticles(serverLevel, DEEP_BLUE_DUST, x, y + 0.5, z, 32, 2.0, 1.0, 2.0, 0.0);

		serverLevel.gameEvent(this, GameEvent.EXPLODE, this.position());

		Vec3 look = this.getLookAngle();
		double hl = Math.sqrt(look.x * look.x + look.z * look.z);
		if (hl > 0.001) {
			this.push(-look.x / hl * 0.3, 0.6, -look.z / hl * 0.3);
		} else {
			this.push(0, 0.6, 0);
		}
		this.hurtMarked = true;

		this.moisture = Math.max(0, this.moisture - BURST_MOISTURE_COST);

		Vec3 explosionPos = this.position();
		float q = BURST_RADIUS;

		int k = Mth.floor(explosionPos.x - q - 1.0);
		int l = Mth.floor(explosionPos.x + q + 1.0);
		int r = Mth.floor(explosionPos.y - q - 1.0);
		int s = Mth.floor(explosionPos.y + q + 1.0);
		int t = Mth.floor(explosionPos.z - q - 1.0);
		int u = Mth.floor(explosionPos.z + q + 1.0);

		List<Entity> entityList = serverLevel.getEntities(this, new AABB(k, r, t, l, s, u));
		DamageSource source = serverLevel.damageSources().explosion(this, this);

		for (Entity targetEntity : entityList) {
			if (!shouldBurstAffect(targetEntity)) continue;

			double w = Math.sqrt(targetEntity.distanceToSqr(explosionPos)) / (double) q;
			if (w > 1.0) continue;

			double dx = targetEntity.getX() - explosionPos.x;
			double dy = targetEntity.getEyeY() - explosionPos.y;
			double dz = targetEntity.getZ() - explosionPos.z;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dist == 0.0) continue;

			dx /= dist;
			dy /= dist;
			dz /= dist;

			double exposure = Explosion.getSeenPercent(explosionPos, targetEntity);
			double intensity = (1.0 - w) * exposure;

			float damage = (float) ((int) ((intensity * intensity + intensity) / 2.0 * 7.0 * (double) q + 1.0));
			targetEntity.hurt(source, damage);

			double knockback;
			if (targetEntity instanceof LivingEntity living) {
				knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, intensity);
			} else {
				knockback = intensity;
			}
			targetEntity.setDeltaMovement(targetEntity.getDeltaMovement().add(dx * knockback, dy * knockback, dz * knockback));
		}
	}

	private boolean shouldBurstAffect(Entity target) {
		if (target instanceof AxolotlShifterEntity) return false;
		if (target.ignoreExplosion()) return false;
		if (target.getType().is(WATER_EXPLODE_IMMUNE)) return false;
		return true;
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		DamageSource source = new DamageSource(this.level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ATTACK_DAMAGE_KEY), this, this);
		if (!target.hurt(source, 4.0f)) return false;

		if (this.isSprinting() && !this.isInWater() && this.moisture > 0) {
			this.playSound(SoundEvents.AXOLOTL_SPLASH, 0.5f, 0.8f);
			if (this.level() instanceof ServerLevel serverLevel) {
				ParticleUtils.spawnParticles(serverLevel, ParticleTypes.RAIN,
						target.getX(), target.getY(0.5), target.getZ(), 8, 0.5, 0.5, 0.5, 0.0);
				ParticleUtils.spawnParticles(serverLevel, DEEP_BLUE_DUST,
						target.getX(), target.getY(0.5), target.getZ(), 8, 0.5, 0.5, 0.5, 0.0);
			}
			this.moisture = Math.max(0, this.moisture - POUNCE_MOISTURE_COST);
		}
		return true;
	}

	@Override
	public void travel(Vec3 movementInput) {
		if (this.isControlledByLocalInstance() && this.isInWater()) {
			this.moveRelative(this.getSpeed(), movementInput);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().multiply(0.93, 0.85, 0.93));
			this.calculateEntityAnimation(false);
		} else {
			if (!this.isSprinting()) {
				movementInput = movementInput.multiply(0.7, 0.7, 0.7);
			}
			super.travel(movementInput);
		}
	}

	@Override
	public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource damageSource) {
		if (fallDistance <= 6.0f) return false;
		return super.causeFallDamage(fallDistance - 6.0f, multiplier, damageSource);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean result = super.hurt(source, amount);
		if (result && !this.level().isClientSide() && this.isAlive() && isEnvironmentalHazard(source)) {
			this.panicTicks = PANIC_DURATION;
		}
		return result;
	}

	private boolean isEnvironmentalHazard(DamageSource source) {
		if (source.is(THIRST_DAMAGE_KEY)) return false;
		if (source.is(DamageTypeTags.IS_FIRE)) return true;
		if (source.is(DamageTypeTags.IS_FREEZING)) return true;
		String name = source.getMsgId();
		return "sweetBerryBush".equals(name) || "cactus".equals(name);
	}

	@Override
	public MobType getMobType() {
		return MobType.WATER;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putInt("Moisture", this.moisture);
		nbt.putInt("BurstCooldown", this.burstCooldown);
		nbt.putInt("MoistureBonus", this.moistureBonus);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		this.moisture = nbt.contains("Moisture") ? Mth.clamp(nbt.getInt("Moisture"), 0, MAX_MOISTURE) : MAX_MOISTURE;
		this.burstCooldown = nbt.getInt("BurstCooldown");
		this.moistureBonus = Integer.MIN_VALUE;
		this.panicTicks = 0;
		this.combatSpeedApplied = false;
		this.recoveringMoisture = false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AXOLOTL_IDLE_AIR;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AXOLOTL_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AXOLOTL_DEATH;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 3, state -> {
			if (state.isMoving()) {
				state.setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
			} else {
				state.setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
			}
			return PlayState.CONTINUE;
		}));
		controllers.add(new AnimationController<>(this, "attack", 0, state -> {
			if (this.swinging) {
				return state.setAndContinue(RawAnimation.begin().thenPlay("attack"));
			}
			state.getController().forceAnimationReset();
			return PlayState.STOP;
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	static class ShifterNavigation extends AmphibiousPathNavigation {
		ShifterNavigation(Mob mob, Level level) { super(mob, level); }

		@Override
		protected PathFinder createPathFinder(int range) {
			this.nodeEvaluator = new AmphibiousNodeEvaluator(false) {
				@Override public void prepare(PathNavigationRegion region, Mob mob) {
					super.prepare(region, mob);
					mob.setPathfindingMalus(BlockPathTypes.WALKABLE, 0.0f);
					mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0f);
				}
			};
			this.nodeEvaluator.setCanOpenDoors(true);
			return new PathFinder(this.nodeEvaluator, range);
		}

		@Override protected Vec3 getTempMobPos() { return this.mob.position(); }
		@Override protected boolean canMoveDirectly(Vec3 from, Vec3 to) { return false; }
	}

	static class ShifterMoveControl extends MoveControl {
		private final SmoothSwimmingMoveControl swimming;

		ShifterMoveControl(Mob mob) {
			super(mob);
			this.swimming = new SmoothSwimmingMoveControl(mob, 85, 45, 0.2f, 1.0f, false);
		}

		@Override public void setWantedPosition(double x, double y, double z, double speed) {
			super.setWantedPosition(x, y, z, speed);
			this.swimming.setWantedPosition(x, y, z, speed);
		}

		@Override public void tick() {
			if (this.mob.isInWater()) {
				this.swimming.tick();
				if (this.operation == Operation.MOVE_TO && !this.mob.getNavigation().isDone()
						&& this.mob.horizontalCollision && this.wantedY > this.mob.getY() + 0.1
						&& this.wantedY <= this.mob.getY() + 1.25
						&& !this.mob.level().getFluidState(BlockPos.containing(this.wantedX, this.wantedY, this.wantedZ)).is(FluidTags.WATER)
						&& this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(0.0, 0.6, 0.0))) {
					Vec3 velocity = this.mob.getDeltaMovement();
					this.mob.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.42), velocity.z);
				}
			} else {
				this.mob.setYya(0.0f);
				this.mob.setXRot(this.rotlerp(this.mob.getXRot(), 0.0f, 10.0f));
				super.tick();
			}
		}
	}

	static abstract class WaterSeekingGoal extends Goal {
		protected final AxolotlShifterEntity shifter;
		private final int range;
		private final double speed;
		private final boolean recovery;
		@Nullable private BlockPos waterPos;
		private final Set<BlockPos> rejected = new HashSet<>();
		private int nextSearchAge;
		private int rejectedUntil;
		private int lastProgressAge;
		private Vec3 lastProgressPos = Vec3.ZERO;
		private float exploreYaw;

		WaterSeekingGoal(AxolotlShifterEntity shifter, int range, double speed, boolean recovery) {
			this.shifter = shifter;
			this.range = range;
			this.speed = speed;
			this.recovery = recovery;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override public void start() {
			this.shifter.recoveringMoisture = this.recovery;
			this.nextSearchAge = this.shifter.tickCount;
			this.lastProgressAge = this.shifter.tickCount;
			this.lastProgressPos = this.shifter.position();
			this.exploreYaw = this.shifter.getYRot();
			this.waterPos = null;
			this.rejected.clear();
		}

		@Override public void tick() {
			if (this.shifter.isInWater()) {
				this.shifter.getNavigation().stop();
				this.shifter.setDeltaMovement(this.shifter.getDeltaMovement().scale(0.6));
				return;
			}
			boolean moving = !this.shifter.getNavigation().isDone();
			if (this.shifter.position().distanceToSqr(this.lastProgressPos) >= 0.0625) {
				this.lastProgressPos = this.shifter.position();
				this.lastProgressAge = this.shifter.tickCount;
			}
			boolean stuck = moving && this.shifter.tickCount - this.lastProgressAge >= 40;
			boolean waterGone = this.waterPos != null
					&& !this.shifter.level().getFluidState(this.waterPos).is(FluidTags.WATER);
			if (stuck || waterGone || (!moving && this.waterPos != null)) {
				if (this.waterPos != null) {
					for (BlockPos pos : BlockPos.withinManhattan(this.waterPos, 1, 1, 1)) this.rejected.add(pos.immutable());
					this.rejectedUntil = this.shifter.tickCount + 200;
				}
				this.waterPos = null;
				this.shifter.getNavigation().stop();
				this.exploreYaw += 90 + this.shifter.getRandom().nextInt(90);
				this.lastProgressAge = this.shifter.tickCount;
				moving = false;
			}
			if (this.waterPos != null || this.shifter.tickCount < this.nextSearchAge) return;
			this.nextSearchAge = this.shifter.tickCount + 40;
			if (this.shifter.tickCount >= this.rejectedUntil) this.rejected.clear();
			Path path = this.shifter.findWaterPath(this.range, 8, this.rejected);
			if (path != null && this.shifter.getNavigation().moveTo(path, this.speed)) {
				this.waterPos = path.getTarget();
				this.lastProgressAge = this.shifter.tickCount;
			} else if (!moving) {
				if (!this.shifter.wanderTowardYaw(this.exploreYaw, this.recovery ? 0.8 : this.speed)) this.exploreYaw += 90;
				this.lastProgressAge = this.shifter.tickCount;
			}
		}

		@Override public void stop() {
			if (this.recovery) this.shifter.recoveringMoisture = false;
			this.waterPos = null;
			this.shifter.getNavigation().stop();
		}
	}

	static class SeekWaterGoal extends WaterSeekingGoal {
		private int nextScanAge;
		SeekWaterGoal(AxolotlShifterEntity shifter) { super(shifter, SEEK_WATER_RANGE, 1.1, true); }

		@Override public boolean canUse() {
			if (this.shifter.getTarget() != null || this.shifter.isPanicking() || this.shifter.tickCount < this.nextScanAge) return false;
			this.nextScanAge = this.shifter.tickCount + 20;
			if (this.shifter.getMoisture() < FIND_WATER_THRESHOLD) return true;
			return !this.shifter.isInWater() && this.shifter.findNearestWater(NEAR_WATER_RADIUS, 4) == null;
		}

		@Override public boolean canContinueToUse() {
			if (this.shifter.getTarget() != null || this.shifter.isPanicking()) return false;
			if (this.shifter.isInWater()) return this.shifter.getMoisture() < RETREAT_RECOVER_MOISTURE;
			return this.shifter.getMoisture() < RETREAT_RECOVER_MOISTURE
					|| this.shifter.findNearestWater(NEAR_WATER_RADIUS, 4) == null;
		}
	}

	static class CombatRetreatToWaterGoal extends WaterSeekingGoal {
		CombatRetreatToWaterGoal(AxolotlShifterEntity shifter) { super(shifter, 12, 1.15, true); }
		@Override public boolean canUse() {
			return this.shifter.getTarget() != null && !this.shifter.isPanicking()
					&& this.shifter.getMoisture() < RETREAT_MOISTURE_THRESHOLD
					&& (this.shifter.isInWater() || this.shifter.findNearestWater(12, 4) != null);
		}
		@Override public boolean canContinueToUse() {
			return !this.shifter.isPanicking() && this.shifter.getMoisture() < RETREAT_RECOVER_MOISTURE;
		}
	}

	static class PanicToWaterGoal extends WaterSeekingGoal {
		private int nextWaterScanAge;
		private boolean noWaterNearby;
		PanicToWaterGoal(AxolotlShifterEntity shifter) { super(shifter, SEEK_WATER_RANGE, 1.05, false); }
		private boolean shouldYieldToCombat() {
			if (this.shifter.getTarget() == null) return false;
			if (this.shifter.tickCount >= this.nextWaterScanAge) {
				this.nextWaterScanAge = this.shifter.tickCount + 40;
				this.noWaterNearby = this.shifter.findWaterPath(SEEK_WATER_RANGE, 8, Set.of()) == null;
			}
			return this.noWaterNearby;
		}
		@Override public boolean canUse() {
			return this.shifter.isPanicking() && !this.shifter.isInWater() && !shouldYieldToCombat();
		}
		@Override public boolean canContinueToUse() { return canUse(); }
	}

	static class EatGlowBerriesGoal extends Goal {
		private final AxolotlShifterEntity shifter;
		@Nullable private BlockPos berryPos;
		@Nullable private Path berryPath;
		private int nextScanAge;
		private int finishAge;

		EatGlowBerriesGoal(AxolotlShifterEntity shifter) {
			this.shifter = shifter;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}
		@Override public boolean canUse() {
			if (this.shifter.getTarget() != null || this.shifter.isPanicking() || this.shifter.eatCooldown > 0
					|| this.shifter.tickCount < this.nextScanAge) return false;
			this.nextScanAge = this.shifter.tickCount + 30;
			for (BlockPos pos : BlockPos.withinManhattan(this.shifter.blockPosition(), EAT_SCAN_RANGE, 6, EAT_SCAN_RANGE)) {
				if (!CaveVines.hasGlowBerries(this.shifter.level().getBlockState(pos))) continue;
				for (int below = 0; below <= 2; below++) {
					Path path = this.shifter.getNavigation().createPath(pos.below(below), 0);
					if (path != null && path.canReach()) {
						this.berryPos = pos.immutable();
						this.berryPath = path;
						return true;
					}
				}
			}
			return false;
		}
		@Override public boolean canContinueToUse() {
			return this.berryPos != null && this.shifter.getTarget() == null && !this.shifter.isPanicking()
					&& this.shifter.tickCount < this.finishAge && (!this.shifter.getNavigation().isDone() || canReachBerry())
					&& CaveVines.hasGlowBerries(this.shifter.level().getBlockState(this.berryPos));
		}
		@Override public void start() {
			this.finishAge = this.shifter.tickCount + 200;
			this.shifter.getNavigation().moveTo(this.berryPath, 1.0);
		}
		@Override public void tick() {
			if (this.berryPos == null) return;
			Vec3 berry = Vec3.atCenterOf(this.berryPos);
			this.shifter.getLookControl().setLookAt(berry.x, berry.y, berry.z);
			if (canReachBerry()) {
				this.shifter.eatGlowBerryAt(this.berryPos);
				this.berryPos = null;
			}
		}
		private boolean canReachBerry() {
			if (this.berryPos == null) return false;
			Vec3 berry = Vec3.atCenterOf(this.berryPos);
			return this.shifter.getEyePosition().distanceToSqr(berry) <= 4.0
					&& this.shifter.level().clip(new ClipContext(this.shifter.getEyePosition(), berry,
							ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.shifter)).getType() == HitResult.Type.MISS;
		}
		@Override public void stop() {
			this.berryPos = null;
			this.berryPath = null;
			this.shifter.getNavigation().stop();
			this.shifter.eatCooldown = EAT_COOLDOWN_MIN
					+ this.shifter.getRandom().nextInt(EAT_COOLDOWN_MAX - EAT_COOLDOWN_MIN + 1);
		}
	}
}
