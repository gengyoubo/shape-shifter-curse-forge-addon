package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.FluorescentAxolotlPerks;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.EntityDisplayClock;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/**
 * 荧光幼灵主要技能「法阵激光」实体（自驱状态机）?
 *
 * <p>阶段?
 * <ul>
 *   <li>CHARGE?~7s=140t）：玩家定身（ROOTED，仅锁移?跳跃、视角自由）。前方放法阵?
 *       0~2.5s 四条 END_ROD 白线从法阵四角延伸到 24 格；2.5~6.5s 四线随法阵旋?+ 加速向中心聚拢?
 *       末尾聚到较小间隔并慢慢停转；6.5~7s 停稳。可被净化打断（取消、无 CD）?/li>
 *   <li>RELEASE?~10s=60t）：视角限?5°/s（LASER_STATE=2 ?ViewRateLimitMixin 处理）?
 *       沿朝向发射直?5 格、长 24 格的穿墙光柱，每 4t 造成 6 点魔法伤害（默认白名单）?
 *       周围?青螺旋粒子沿光柱前进。不可被打断?/li>
 *   <li>FADE?0~11.5s=30t）：光柱半径 1.5s 内缩小后随法阵消失；结束后设 20s CD?/li>
 * </ul>
 * 渲染（法?+ 光柱）由 {@code FluorescentLaserRenderer} 负责；四?螺旋/爆裂粒子服务端生成（所有人可见）?
 */
public class LaserBeamEntity extends Entity {
    private long cooldownCastId = -1;
    public void bindCast(long id) { cooldownCastId = id; }


	// ===== 时序 =====
	private static final int CHARGE_TICKS = 140;      // 7 绉掕搫鍔?
	private static final int RELEASE_TICKS = 60;      // 3 绉掓縺鍏?
	private static final int FADE_TICKS = 30;         // 1.5 秒消退

	// ===== 几何 =====
	private static final double ARRAY_DIST = 3.0;     // 法阵在玩家前方距?
	private static final double BEAM_LENGTH = 32.0;    // 光柱/四线长度
	private static final double BEAM_RADIUS = 2.5;     // 光柱半径（直?5 格）

	// ===== 海晶荧光坠增强（三连发单道视觉）=====
	private static final float ENH_BEAM_RADIUS = 0.75f;   // 增强光柱半径（原 2.5 ?30%?
	private static final float ENH_ARRAY_SCALE = 0.3f;    // 增强法阵缩到?30%
	private static final int ENH_SHOT_TICKS = 8;          // 增强单道存活 8t（纯视觉?
	// 注：增强激光射程由 Manager.ENH_BEAM_LENGTH 管（resolveHitPoint 射线长度），实体不再单独?ENH_LENGTH（渲染改用实时距离）

	// ===== 浼ゅ =====
	private static final int DAMAGE_INTERVAL = 10;      // ?10t 结算
	private static final float DAMAGE = 20.0f;          // 每次 20 魔法伤害（释?60t ?6 ?= 120?

	// 阶段 5：运行时快照读取（abilities.laser_beam；快照未初始化回退默认常量?
	// 几何 ARRAY_DIST 与装?ENH_ARRAY_SCALE 不迁移）
	private static final BalanceReader BAL = new BalanceReader("abilities.laser_beam");

	// ===== 同步数据（供渲染器）=====
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);       // 0 CHARGE / 1 RELEASE / 2 FADE
	private static final EntityDataAccessor<Long> PHASE_EPOCH = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> CHARGE_DURATION = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
	// 海晶荧光坠增强：单道视觉标记 + 发射方向（Vec3d ?3 ?float 同步给渲染器?
	private static final EntityDataAccessor<Boolean> ENHANCED = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> FIRING = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ARRAYS_LEFT = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FIRING_IDX = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.INT);
	// 海晶荧光坠增强：实时锁定点（准星落点，服务端?tick 同步）——供渲染器让剩余待发射法阵预瞑转?
	private static final EntityDataAccessor<Float> LOCK_X = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOCK_Y = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOCK_Z = SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);

	private enum Phase { CHARGE, RELEASE, FADE }

	private Phase phase = Phase.CHARGE;
	private int phaseTicks = 0;
	private UUID ownerUuid;
	private ResourceLocation casterForm;
	private float damage = (float) BAL.d("damage", DAMAGE);
	private int releaseTicks = BAL.i("release_ticks", RELEASE_TICKS);
	private int enhFiringTicks = 0;   // 增强发射态剩?tick

	/** 客户端渲染器粒子门控：记录本阶段 tick 已发过粒子（render 每帧调用，同 tick 多帧只放行一次，
	 *  保证粒子频率与服务端?tick 一致；非同步字段，仅客户端使用）?*/
	public int clientParticleGate = -1;

	public LaserBeamEntity(EntityType<?> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	public LaserBeamEntity(Level world, ServerPlayer owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.LASER_BEAM.get(), world);
		this.ownerUuid = owner.getUUID();
		this.casterForm = github.com.gengyoubo.sscfa.util.FormUtils.currentFormId(owner);
		this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
		this.entityData.set(OWNER_ID, owner.getId());
		this.entityData.set(CHARGE_DURATION, FluorescentAxolotlPerks.laserChargeTicks(
				BAL.i("charge_ticks", CHARGE_TICKS), FluorescentAxolotlPerks.hasWaterMagicEfficiency(owner)));
		syncPhaseClock();
		this.noPhysics = true;
	}

	/** 海晶荧光坠增强：持久「待机法阵」实体——combo 期间跟随玩家、在其前方显示旋转法阵（像原激光）?
	 *  发射?startFiring ?8t 发射态额外画光柱。不定身/不伤?不设 CD（由 Manager 管，combo 结束 discard）?*/
	public LaserBeamEntity(Level world, ServerPlayer owner, boolean enhancedStandby) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.LASER_BEAM.get(), world);
		this.ownerUuid = owner.getUUID();
		this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
		this.entityData.set(OWNER_ID, owner.getId());
		this.entityData.set(ENHANCED, true);
		this.entityData.set(PHASE, 1);
		syncPhaseClock();
		this.noPhysics = true;
	}

	/** 发射一道：进入 8t 发射态，记录发射法阵索引 + 发射瞬间定格的世界锁定点?
	 *  激光落点固定为此世界点，法阵随玩家移动时激光始终指向它（追踪锁定，方向由渲染器/伤害按当前法阵位置实时算）?*/
	public void startFiring(int arrayIdx, Vec3 fireLock) {
		this.entityData.set(FIRING, true);
		this.entityData.set(FIRING_IDX, arrayIdx);
		this.entityData.set(DIR_X, (float) fireLock.x);
		this.entityData.set(DIR_Y, (float) fireLock.y);
		this.entityData.set(DIR_Z, (float) fireLock.z);
		this.enhFiringTicks = BAL.i("enh_shot_ticks", ENH_SHOT_TICKS);
	}

	public boolean isFiring() {
		return this.entityData.get(FIRING);
	}

	/** combo 当前剩余法阵数（渲染器据此画斜后方剩余法阵）?*/
	public void setArraysLeft(int n) {
		this.entityData.set(ARRAYS_LEFT, n);
	}

	public int getArraysLeft() {
		return this.entityData.get(ARRAYS_LEFT);
	}

	public int getFiringIdx() {
		return this.entityData.get(FIRING_IDX);
	}

	/** 服务端每 tick 同步的实时锁定点（准星落点），供渲染器让剩余待发射法阵预瞑转向?*/
	public void setLockPoint(Vec3 p) {
		this.entityData.set(LOCK_X, (float) p.x);
		this.entityData.set(LOCK_Y, (float) p.y);
		this.entityData.set(LOCK_Z, (float) p.z);
	}

	/** 锁定点（未同步时返回 null，渲染器回退朝准星）?*/
	public Vec3 getLockPoint() {
		float x = this.entityData.get(LOCK_X);
		float y = this.entityData.get(LOCK_Y);
		float z = this.entityData.get(LOCK_Z);
		if (x == 0f && y == 0f && z == 0f) return null;
		return new Vec3(x, y, z);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(PHASE, 0);
		this.entityData.define(PHASE_EPOCH, EntityDisplayClock.UNSET);
		this.entityData.define(CHARGE_DURATION, CHARGE_TICKS);
		this.entityData.define(OWNER_ID, 0);
		this.entityData.define(ENHANCED, false);
		this.entityData.define(DIR_X, 0f);
		this.entityData.define(DIR_Y, 0f);
		this.entityData.define(DIR_Z, 0f);
		this.entityData.define(FIRING, false);
		this.entityData.define(ARRAYS_LEFT, 3);
		this.entityData.define(FIRING_IDX, 0);
		this.entityData.define(LOCK_X, 0f);
		this.entityData.define(LOCK_Y, 0f);
		this.entityData.define(LOCK_Z, 0f);
	}

	// ===== 渲染器读?=====
	public int getPhaseId() {
		return this.entityData.get(PHASE);
	}

	public int getPhaseTick() {
		return this.level().isClientSide
				? EntityDisplayClock.elapsed(this.level().getGameTime(), this.entityData.get(PHASE_EPOCH))
				: phaseTicks;
	}

	/** The server counter remains authoritative; a stable phase only needs its start marker. */
	private void syncPhaseClock() {
		long epoch = this.level().getGameTime() - phaseTicks;
		if (this.entityData.get(PHASE_EPOCH) != epoch) this.entityData.set(PHASE_EPOCH, epoch);
	}

	/** Per-cast duration, synchronized to clients and retained through entity NBT. */
	public int getChargeTicks() {
		return this.entityData.get(CHARGE_DURATION);
	}

	public int getTrackedOwnerId() {
		return this.entityData.get(OWNER_ID);
	}

	public boolean isEnhanced() {
		return this.entityData.get(ENHANCED);
	}

	/** 增强单道发射瞬间定格的世界锁定点（渲染器/伤害据当前法阵位置算方向与长度，实现落点固定的追踪锁定）?*/
	public Vec3 getFireLock() {
		return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z));
	}

	public float enhArrayScale() {
		return ENH_ARRAY_SCALE;
	}

	public float enhBeamRadius() {
		// 渲染器（客户端）与服务端 shotDamage（Manager 侧独立读）共用：双端一致读?
		if (github.com.gengyoubo.sscfa.balance.BalanceIntegration.isClientThread()) {
			var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.clientSnapshot();
			if (s != null) return (float) s.getDouble("abilities.laser_beam", "enh_beam_radius");
		}
		return (float) BAL.d("enh_beam_radius", ENH_BEAM_RADIUS);
	}

	public static double arrayDist() {
		return ARRAY_DIST;
	}

	public double beamLength() {
		// 渲染器（客户端）与服务端 beamDamage 共用：双端一致读取—?
		// 客户端读客户端镜像快照，服务端读权威快照（同 MeteorSpell.maxRange 模式?
		if (github.com.gengyoubo.sscfa.balance.BalanceIntegration.isClientThread()) {
			var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.clientSnapshot();
			if (s != null) return s.getDouble("abilities.laser_beam", "beam_length");
		}
		return BAL.d("beam_length", BEAM_LENGTH);
	}

	/** 增强单道光柱?24 格、碰撞盒?0.5，放宽视锥剔除避免离屏被 cull?*/
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return true;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		// 增强单道光柱最?24 格、起点在玩家斜后方，扩大可见盒避免视锥剔除导致法?光柱整体不渲?
		return this.entityData.get(ENHANCED) ? this.getBoundingBox().inflate(48.0) : super.getBoundingBoxForCulling();
	}

	@Override
	public void tick() {
		super.tick();
		// 海晶荧光坠增强：持久「待机法阵」——combo 期间跟随玩家、前方显示旋转法阵；发射态额外画光柱 8t；由 Manager combo 结束 discard
		if (this.entityData.get(ENHANCED)) {
			if (this.level().isClientSide) return;
			if (!(this.level() instanceof ServerLevel sw2)) return;
			ServerPlayer ownerE = getOwner(sw2);
			if (ownerE == null || ownerE.isRemoved() || ownerE.isDeadOrDying() || !FormUtils.isAxolotlFluorescent(ownerE)) {
				this.discard();
				return;
			}
			this.setPos(ownerE.getX(), ownerE.getEyeY(), ownerE.getZ());   // 跟随玩家
			phaseTicks++;
			syncPhaseClock();
			if (this.entityData.get(FIRING)) {
				enhFiringTicks--;
				if (enhFiringTicks <= 0) this.entityData.set(FIRING, false);
			}
			return;
		}
		if (this.level().isClientSide) {
			return;   // 渲染?FluorescentLaserRenderer 负责；粒子由服务端生?
		}
		if (!(this.level() instanceof ServerLevel sw)) return;

		ServerPlayer owner = getOwner(sw);
		if (owner == null || owner.isRemoved() || owner.isDeadOrDying()
				|| !FormUtils.isAxolotlFluorescent(owner)
				|| (casterForm != null && !casterForm.equals(
					github.com.gengyoubo.sscfa.util.FormUtils.currentFormId(owner)))) {
			cancelNoCd(owner);
			this.discard();
			return;
		}

		// 跟随玩家眼部（法阵在前方?aim 计算?
		this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());

		// 定身：每 tick 刷新 ROOTED（仅锁移动，视角自由；释放期视角限速由 mixin 处理?
		owner.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.ROOTED.get(), 8, 0, false, false, false));

		Vec3 aim = owner.getViewVector(1.0f).normalize();
		Vec3 arrayPos = new Vec3(owner.getX(), owner.getEyeY(), owner.getZ()).add(aim.scale(ARRAY_DIST));

		phaseTicks++;
		switch (phase) {
			case CHARGE -> tickCharge(sw, owner, aim, arrayPos);
			case RELEASE -> tickRelease(sw, owner, aim, arrayPos);
			case FADE -> tickFade(sw, owner, aim, arrayPos);
		}
		syncPhaseClock();
	}

	// ==================== LASER_STATE 去重同步 ====================
	/** 上次已同步给客户端的 laser_state 值（-1 = 尚未同步）；值不变时不重发，阶段内每 tick 重复同步是纯带宽浪费?*/
	private int lastSyncedLaserState = -1;

	private void syncLaserStateDedup(ServerPlayer owner, int value) {
		if (lastSyncedLaserState == value) return;
		lastSyncedLaserState = value;
		PowerUtils.setResourceValueAndSync(owner, LASER_STATE, value);
	}

	// ==================== CHARGE ====================
	private void tickCharge(ServerLevel sw, ServerPlayer owner, Vec3 aim, Vec3 arrayPos) {
		// 净化打断：取消，返?40% CD（进 60% CD?
		if (owner.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			cancelWithInterruptCd(owner);
			sw.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
					SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.2f);
			this.discard();
			return;
		}
		syncLaserStateDedup(owner, 1);
		int chargeTicks = getChargeTicks();

		// 蓄力音效（volume=3.0：反编译实证客户?clamp ?1，近处响度不变不震耳；
		// 可闻半径 16×3=48 ?光柱射程 32 ?16 格富余，OpenAL 距离线性衰减到 48 格归零）
		if (phaseTicks == 1) {
			sw.playSound(null, arrayPos.x, arrayPos.y, arrayPos.z,
					SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0f, 0.8f);
		}
		if (phaseTicks % 12 == 0) {
			float p = phaseTicks / (float) chargeTicks;
			sw.playSound(null, arrayPos.x, arrayPos.y, arrayPos.z,
					SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 3.0f, 0.8f + p * 0.8f);
		}

		// 四条白线由渲染器绘制（客户端，无粒子残留）；法阵核心发光粒子改为客户端渲染器按视角生?
		// （第一人称不生成、第三人称生成），避免服务端粒子无法区分视角导致第一人称被遮?
		if (phaseTicks >= chargeTicks) {
			phase = Phase.RELEASE;
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(owner, github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.SKILL_ID, cooldownCastId);
			phaseTicks = 0;
			this.entityData.set(PHASE, 1);
			syncLaserStateDedup(owner, 2);
				// 发射音（volume=3.0?8 格可?射程+16，距离衰减归零；近处 clamp ?1 不震耳）
				sw.playSound(null, arrayPos.x, arrayPos.y, arrayPos.z,
						SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0f, 1.0f);
				sw.playSound(null, arrayPos.x, arrayPos.y, arrayPos.z,
						SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 3.0f, 0.7f);
		}
	}

	// ==================== RELEASE ====================
	private void tickRelease(ServerLevel sw, ServerPlayer owner, Vec3 aim, Vec3 arrayPos) {
		syncLaserStateDedup(owner, 2);
		// 螺旋粒子已移到客户端渲染器自绘（逐行照抄未改参数，网络包归零?
		// 伤害：每 4t 一次，5 格直径穿墙圆?
		if (phaseTicks % BAL.i("damage_interval", DAMAGE_INTERVAL) == 0) {
			beamDamage(sw, owner, arrayPos, aim, BAL.d("beam_radius", BEAM_RADIUS));
		}
		if (phaseTicks % 8 == 0) {
			sw.playSound(null, arrayPos.x, arrayPos.y, arrayPos.z,
					SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 3.0f, 1.4f);
		}
		if (phaseTicks >= releaseTicks) {
			phase = Phase.FADE;
			phaseTicks = 0;
			this.entityData.set(PHASE, 2);
			syncLaserStateDedup(owner, 3);
		}
	}

	// ==================== FADE ====================
	private void tickFade(ServerLevel sw, ServerPlayer owner, Vec3 aim, Vec3 arrayPos) {
		// 螺旋粒子已移到客户端渲染器自绘（含消退期半径缩小，网络包归零）
		if (phaseTicks >= BAL.i("fade_ticks", FADE_TICKS)) {
			// 完全消失 ??CD、解除定身、清状?
			owner.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.ROOTED.get());
			syncLaserStateDedup(owner, 0);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(owner,
					github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.SKILL_ID, cooldownCastId);
			sw.playSound(null, arrayPos.x, arrayPos.y, arrayPos.z,
					SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 3.0f, 1.0f);
			this.discard();
		}
	}

	// ==================== 四线粒子 ====================
	// ==================== 光柱螺旋粒子 ====================
        // 已移到客户端 FluorescentLaserRenderer.spawnBeamSpiralClient 自绘（逐行照抄未改参数?
        // phase/phaseTick ?DataTracker 同步、螺旋角纯时间函数），服务端不再广播——持续粒子网络包归零。方法删除?
	// ==================== 浼ゅ ====================
	private void beamDamage(ServerLevel sw, ServerPlayer owner, Vec3 arrayPos, Vec3 aim, double radius) {
		double beamLen = beamLength();
		Vec3 end = arrayPos.add(aim.scale(beamLen));
		AABB box = new AABB(arrayPos, end).inflate(radius);
		List<LivingEntity> targets = sw.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && !e.isSpectator() && !e.getUUID().equals(ownerUuid));
		ResourceKey<net.minecraft.world.damagesource.DamageType> key =
				ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "magic"));
		for (LivingEntity t : targets) {
			// 默认白名单豁?
			if (WhitelistUtils.isProtected(owner, t)) continue;
			// 到光柱轴线的距离（圆柱判定，穿墙 ?不检查方块遮挡）
			Vec3 toT = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(arrayPos);
			double proj = toT.dot(aim);
			if (proj < 0 || proj > beamLen) continue;
			Vec3 closest = arrayPos.add(aim.scale(proj));
			double distSq = t.position().add(0, t.getBbHeight() * 0.5, 0).distanceToSqr(closest);
			if (distSq > radius * radius) continue;
			var holder = t.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key);
			if (t.hurt(new net.minecraft.world.damagesource.DamageSource(holder, owner, owner), damage)) {
				FluorescentAxolotlPerks.onLaserHit(owner);
			}
		}
	}

    @Override
    public void remove(RemovalReason reason) {
        if (!isRemoved() && level() instanceof ServerLevel world) {
            ServerPlayer owner = getOwner(world);
            var manager = github.com.gengyoubo.sscfa.cooldown.SkillCastManager.get(world);
            var cast = ownerUuid == null ? null : manager.control(ownerUuid,
                    github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.SKILL_ID);
            if (!isEnhanced() && cast != null && cast.castId == cooldownCastId) {
                manager.interrupt(cooldownCastId, world.getServer().overworld().getGameTime());
                if (owner != null) {
                    owner.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.ROOTED.get());
                    syncLaserStateDedup(owner, 0);
                }
            }
            super.remove(reason);
            github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.onBeamRemoved(this, owner);
            return;
        }
        super.remove(reason);
    }

	// ==================== 取消/清理 ====================
	public void interruptControl(ServerPlayer owner) {
		cancelNoCd(owner);
		discard();
	}

	private void cancelNoCd(ServerPlayer owner) {
		if (owner != null) {
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(owner, github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.SKILL_ID, cooldownCastId);
			owner.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.ROOTED.get());
			syncLaserStateDedup(owner, 0);
		}
	}

	/** 被净化打断时取消：按 power JSON fail_cooldown ?CD?*/
	private void cancelWithInterruptCd(ServerPlayer owner) {
		if (owner != null) {
			owner.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.ROOTED.get());
			syncLaserStateDedup(owner, 0);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(owner,
					github.com.gengyoubo.sscfa.ability.FluorescentLaserManager.SKILL_ID, cooldownCastId);
		}
	}

	private ServerPlayer getOwner(ServerLevel sw) {
		if (ownerUuid == null) return null;
		var server = sw.getServer();
		ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(ownerUuid);
		if (owner != null && this.entityData.get(OWNER_ID) != owner.getId()) this.entityData.set(OWNER_ID, owner.getId());
		return owner;
	}

	/** laser_state 资源 id? 空闲 /1 蓄力 /2 释放 /3 消退）?*/
	public static final ResourceLocation LASER_STATE =
			new ResourceLocation("my_addon", "form_axolotl_fluorescent_laser_state");

	/** 客户端查询某玩家的活跃激光实体（供视角限速读?phase/phaseTick）?*/
	public static LaserBeamEntity getActiveForClient(net.minecraft.client.player.LocalPlayer player) {
		net.minecraft.client.multiplayer.ClientLevel w = net.minecraft.client.Minecraft.getInstance().level;
		if (w == null) return null;
		for (Entity e : w.entitiesForRendering()) {
			if (e instanceof LaserBeamEntity laser && !laser.isEnhanced()) {
				if (player != null && player.getId() == laser.getTrackedOwnerId()) return laser;
			}
		}
		return null;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		nbt.putInt("Phase", phase.ordinal());
		nbt.putInt("PhaseTicks", phaseTicks);
		nbt.putInt("ChargeTicks", getChargeTicks());
		nbt.putLong("CooldownCastId", cooldownCastId);
		if (casterForm != null) nbt.putString("CasterForm", casterForm.toString());
		if (ownerUuid != null) nbt.putUUID("Owner", ownerUuid);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		int p = nbt.contains("Phase") ? nbt.getInt("Phase") : 0;
		phase = Phase.values()[Mth.clamp(p, 0, Phase.values().length - 1)];
		phaseTicks = nbt.contains("PhaseTicks") ? nbt.getInt("PhaseTicks") : 0;
		this.entityData.set(PHASE, phase.ordinal());
		syncPhaseClock();
		this.entityData.set(CHARGE_DURATION, Math.max(1, nbt.contains("ChargeTicks")
				? nbt.getInt("ChargeTicks") : BAL.i("charge_ticks", CHARGE_TICKS)));
		if (nbt.contains("CooldownCastId")) cooldownCastId = nbt.getLong("CooldownCastId");
		if (nbt.contains("CasterForm")) casterForm = ResourceLocation.tryParse(nbt.getString("CasterForm"));
		if (nbt.hasUUID("Owner")) ownerUuid = nbt.getUUID("Owner");
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
