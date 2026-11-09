package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.util.EntityDisplayClock;

/**
 * 寒棘狐「凝棘」蓄力法阵实体（纯视觉，无碰撞无伤害）?
 *
 * <p>蓄力期间跟随施法者眼部；渲染?{@code FrostArrayRenderer} 按施法者准星在眼前 0.2 格画青蓝雪花法阵?
 * 并在法阵?0.08 格中央画一根随蓄力等级放大的冰锥。由 {@link github.com.gengyoubo.sscfa.ability.FrostSpikeManager}
 * 蓄力开?spawn、蓄力结?/ 发射?discard；{@value #MAX_TICKS} tick 超时为双保险?/p>
 *
 * <p>生命周期全在服务端，?EntityTracker 天然多人同步（所有人可见施法者面前的法阵）?
 * 蓄力等级 {@code LEVEL} ?DataTracker 同步，供渲染器决定中央冰锥大小?/p>
 */
public class FrostArrayEntity extends Entity {

	private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FrostArrayEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(FrostArrayEntity.class, EntityDataSerializers.INT);
	/** 凝棘蓄力总进度（0-100 tick = 等级×20 + 级内 ticks）：HUD 副槽侧边内置条从 0 涨到满的数据源?*/
	private static final EntityDataAccessor<Integer> PROGRESS_BASE = SynchedEntityData.defineId(FrostArrayEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> PROGRESS_EPOCH = SynchedEntityData.defineId(FrostArrayEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> PROGRESS_CYCLE = SynchedEntityData.defineId(FrostArrayEntity.class, EntityDataSerializers.INT);
	private int authoritativeProgress;
	private int progressBase, progressCycleTick, progressCycleLength;
	private static final int MAX_TICKS = 400; // 20s 超时双保险（正常由管理器 discard?

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_array");

	public FrostArrayEntity(EntityType<? extends FrostArrayEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	public FrostArrayEntity(Level world, Player owner) {
		this(github.com.gengyoubo.sscfa.init.AddonEntities.FROST_ARRAY.get(), world);
		this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
		this.entityData.set(OWNER_ID, owner.getId());
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER_ID, -1);
		this.entityData.define(LEVEL, 0);
		this.entityData.define(PROGRESS_BASE, 0);
		this.entityData.define(PROGRESS_EPOCH, EntityDisplayClock.UNSET);
		this.entityData.define(PROGRESS_CYCLE, 0);
	}

	/** 施法者实?id（客户端渲染器据此取施法者准星算法阵位置）?*/
	public int getTrackedOwnerId() { return this.entityData.get(OWNER_ID); }

	public int getLevel() { return this.entityData.get(LEVEL); }

	public void setLevel(int level) { if (getLevel() != level) this.entityData.set(LEVEL, level); }

	/** 凝棘总进度：服务端读真实进度，客户端从同步的周期起点与世界时间计算?*/
	public int getProgress() {
		return this.level().isClientSide ? EntityDisplayClock.cyclicProgress(this.entityData.get(PROGRESS_BASE),
				this.entityData.get(PROGRESS_EPOCH), this.entityData.get(PROGRESS_CYCLE), this.level().getGameTime())
				: authoritativeProgress;
	}

	public void setProgress(int progress) { setProgressCycle(progress, 0, 0); }

	/** A cycle resets even if no thorn was consumed, so it cannot be represented by one linear counter. */
	public void setProgressCycle(int base, int cycleTick, int cycleLength) {
		authoritativeProgress = base + cycleTick;
		progressBase = base; progressCycleTick = cycleTick; progressCycleLength = cycleLength;
	}

	private void syncProgressClock() {
		// The manager runs at START_WORLD_TICK, before world time advances. Publish after that advance.
		long epoch = progressCycleLength > 0 ? this.level().getGameTime() - progressCycleTick : EntityDisplayClock.UNSET;
		int base = progressCycleLength > 0 ? progressBase : authoritativeProgress;
		if (this.entityData.get(PROGRESS_BASE) != base) this.entityData.set(PROGRESS_BASE, base);
		if (this.entityData.get(PROGRESS_EPOCH) != epoch) this.entityData.set(PROGRESS_EPOCH, epoch);
		if (this.entityData.get(PROGRESS_CYCLE) != progressCycleLength) this.entityData.set(PROGRESS_CYCLE, progressCycleLength);
	}

	/** 上一次读到的等级（客户端检?LEVEL 跳变 = 服务端刚消耗一根冰???burst 密集汇聚）?*/
	private int clientPrevLevel = -1;

	@Override
	public void tick() {
		super.tick();
		// 客户端：次技能蓄力粒子本地自算（零网络包）——中?= 头顶法阵中心（与服务?secondaryFocus 严格同点）?
		// 服务端原逻辑：按??4t 持续汇聚 + 每秒消耗时 24 ?burst；此处等价复现（burst ?LEVEL 跳变锚定，天然同步）?
		if (this.level().isClientSide) {
			Entity ownerC = this.level().getEntity(getTrackedOwnerId());
			if (ownerC instanceof Player p && FormUtils.isForm(p, FormIdentifiers.SNOW_FOX_FROSTSPINE)) {
				Vec3 center = FrostThornEntity.hoverTarget(p, 0);
				// 每秒消?burst：LEVEL 跳变?1? 刚消耗一??密集波（24 颗，与服务端原版同密度）
				int lv = getLevel();
				if (clientPrevLevel >= 0 && lv > clientPrevLevel) {
					spawnInwardIceClient(center, 24);
				}
				clientPrevLevel = lv;
				// 持续汇聚（每 4t 一?18 颗；服务端原条件 countThorns>0 的近似：等级未满 5 即还有冰锥可消耗）
				if (lv < 5 && this.tickCount % 4 == 0) {
					spawnInwardIceClient(center, 18);
				}
			}
			return;
		}
		Entity owner = this.level().getEntity(getTrackedOwnerId());
		if (!(owner instanceof ServerPlayer p) || p.isRemoved() || p.isDeadOrDying()
				|| !FormUtils.isForm(p, FormIdentifiers.SNOW_FOX_FROSTSPINE)) {
			this.discard();
			return;
		}
		// 跟随施法者眼部（法阵前方位置由渲染器按准星实时算?
		syncProgressClock();
		this.setPos(p.getX(), p.getEyeY(), p.getZ());
		if (this.tickCount > BAL.i("max_ticks", MAX_TICKS)) this.discard(); // 双保险超?
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		// 法阵在眼?0.2 ?+ 中央冰锥延伸出锚点，扩大可见盒避免视锥剔除整帧剔?
		return this.getBoundingBox().inflate(4.0);
	}

	@Override protected void readAdditionalSaveData(CompoundTag nbt) {}

	@Override protected void addAdditionalSaveData(CompoundTag nbt) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override public boolean canBeCollidedWith() { return false; }

	@Override public boolean isPickable() { return false; }

	/** 客户端本地生成向内汇聚冰晶（与服务端?spawnInwardIceParticles 同几何：球面均匀、初?1?20t 向心）?*/
	private void spawnInwardIceClient(Vec3 center, int count) {
		for (int i = 0; i < count; i++) {
			double u = this.random.nextDouble() * 2 - 1;
			double theta = this.random.nextDouble() * Math.PI * 2;
			double r = Math.sqrt(1 - u * u);
			double dx = r * Math.cos(theta), dy = u, dz = r * Math.sin(theta);
			double speed = 1.0 / 20.0;
			this.level().addParticle(github.com.gengyoubo.sscfa.init.AddonParticles.INWARD_ICE.get(),
					center.x + dx, center.y + dy, center.z + dz,
					-dx * speed, -dy * speed, -dz * speed);
		}
	}
}
