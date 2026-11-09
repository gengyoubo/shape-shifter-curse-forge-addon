package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 月尘魔法·冰锥投射物（jackcooper）??
 *
 * <p>朝准星直线匀速飞行（无重力），命中生物造成魔法伤害后消失；超??超时自毁??
 * 与雪?SP ??{@link FrostBallEntity} 无关（不施加霜降、无护符分裂、无追踪）??
 * 供魔法书「冰锥」魔法独立使用，保持低耦合。默认白名单：主人在线且目标受保护则不伤害??/p>
 */
public class SpellFrostSpikeEntity extends Projectile implements ItemSupplier {

	private static final double SPEED = 0.75;        // 15 閺?缁?
	private static final double MAX_DISTANCE = 50.0; // 最大飞行距?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("spells.frost_spike_entity");

	/** 魔法等级??-5），DataTracker 同步供渲染端切换 L4+ 3D 冰锥模型?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellFrostSpikeEntity.class, EntityDataSerializers.INT);

	private Vec3 startPos;
	private int ticksAlive = 0;
	private float damage = 6.0f;

	/** 命中发放的经验赏金（×10 整数；exp_mode 1/2 挂起部分由施法时装入，NBT 持久化跨 tick；齐射均分后每枚持有份额）??*/
	private int expBountyTen = 0;

	/** 剩余可命中敌人数（含当前?? = 命中即碎，单发冰锥默认；齐射按等级注??2+等??1）??*/
	private int pierceRemaining = 0;
	/** 已命中过的实?UUID（穿刺期防同一目标被??tick 重复判定）??*/
	private final java.util.List<java.util.UUID> piercedTargets = new java.util.ArrayList<>();
	/** ??tick 命中为「穿刺穿过」的信号旗（onEntityHit 置????onCollision 跳过销毁）??*/
	private boolean passThroughHit = false;

	/** 设置单枚最多可命中敌人数（穿刺数；?? 等同不穿刺，命中即碎）?*/
	public void setPierceCount(int count) {
		this.pierceRemaining = Math.max(0, count);
	}

	public void setExpBountyTen(int expTen) {
		this.expBountyTen = Math.max(0, expTen);
	}

	public int getExpBountyTen() {
		return expBountyTen;
	}

	/** 本次施法实际耗蓝（命中返还类流派用；与经验赏金同模式??tick ??NBT）??*/
	private java.util.UUID refundCastId;

	public void setRefundCastId(java.util.UUID castId) {
		this.refundCastId = castId;
	}

	public SpellFrostSpikeEntity(EntityType<? extends SpellFrostSpikeEntity> entityType, Level world) {
		super(entityType, world);
		this.startPos = this.position();
	}

	public SpellFrostSpikeEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.SPELL_FROST_SPIKE.get(), world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.startPos = this.position();
	}

	@Override
	protected void defineSynchedData() {
		// 不??super（Entity 的抽象方法无法跨两级访问；dataTracker ??Entity 构造器初始化，?ThrownWaterSpearEntity 写法?
		this.entityData.define(LEVEL, 1);
	}

	/** 设置冰锥命中伤害??*/
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/** 魔法等级??-5；L4+ 渲染端换 3D 冰锥模型）?*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 设置魔法等级（服务端施法时调用，DataTracker 自动同步客户端）??*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 设置飞行方向（朝准星），速度按等级倍率缩放?*/
	public void setDirection(Vec3 direction, float speedMultiplier) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED) * speedMultiplier);
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
		updateRotationFromVelocity(velocity);
	}

	/** 按速度自算朝向（与寒棘狐冰锥同款公式：尖朝速度方向），?3D 模型渲染对正?*/
	private void updateRotationFromVelocity(Vec3 v) {
		double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
		this.setYRot((float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Mth.atan2(-v.y, horiz) * (180.0 / Math.PI)));
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	@Override
	public void tick() {
		super.tick();
        if(this.isRemoved())return;
		ticksAlive++;

		// 无重力匀速移??
		Vec3 velocity = this.getDeltaMovement();
		boolean limited=((github.com.gengyoubo.sscfa.spell.FormSpellStyle.HomingProjectile)this).ssca$hasRangeLimit();
        if(!limited)this.setPos(this.position().add(velocity));

		// 绾扮増鎸掑Λ鈧ù?
		HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if(limited)this.setPos(this.position().add(velocity));
		if (hitResult.getType() != HitResult.Type.MISS) {
			this.onHit(hitResult);
		}
		if (this.isRemoved()) {
			return;
		}

		// 超??/ 超时自毁（仅服务端权威）：客户端实体经网络包构造，startPos 恒??(0,0,0)??
		// 若双端执行会在离原??50 格外第一 tick 就误删客户端实????模型永不显示（只剩服务端粒子）?
		// 客户端实体的消失由服务端 discard 后??tracker 移除包驱动，天然多人同步??
		if (!this.level().isClientSide) {
			double maxDistance = maxDistance();
			if (!limited && startPos != null && this.distanceToSqr(startPos) > maxDistance * maxDistance) {
				this.discard();
				return;
			}
			if (ticksAlive > 100) { // 5 缁夋帟绉撮弮?
				this.discard();
				return;
			}
		}

		// 飞行拖尾粒子（服务端撒，天然多人同步?
		if (this.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
					this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		this.passThroughHit = false; // 每次碰撞先重置穿刺旗?
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 穿刺穿过：轻量雪花反馈后继续飞行，不碎??
			if (this.passThroughHit) {
				if (this.level() instanceof ServerLevel serverWorld) {
					ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
							this.getX(), this.getY(), this.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
				}
				return;
			}
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);
			if (this.level() instanceof ServerLevel serverWorld) {
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
						this.getX(), this.getY(), this.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
			}
			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		Entity target = entityHitResult.getEntity();
		if (target instanceof LivingEntity livingTarget && !this.level().isClientSide) {
			// 公共命中结算（白名单豁免 ??法术伤害 ??经验补发 ??流派钩子）：?SpellHitHelper
			github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult hit =
					github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
							this, livingTarget, damage,
							github.com.gengyoubo.sscfa.spell.FormationElement.ICE, refundCastId, expBountyTen);
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
				expBountyTen = 0; // 经验已发放，清零防重??
			}
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.PROTECTED) {
				return; // 白名单豁免：不结算穿?
			}
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.2f);
			// 穿刺结算（齐射来??pierceRemaining>0）：记入已命中表并递减??
			// 递减后仍 >0 ??置旗穿过继续飞，否则本次为最后一次命中、照常碎裂??
			// 默认单发冰??pierceRemaining=0 ??减??-1 ??>0 ??行为不变（命中即碎）?
			this.pierceRemaining--;
			this.piercedTargets.add(livingTarget.getUUID());
			if (this.pierceRemaining > 0) {
				this.passThroughHit = true;
			}
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 排除盔甲架（与月灵光弹同口径）：假人不产返??经??
		return super.canHitEntity(entity) && entity != this.getOwner() && entity instanceof LivingEntity
				&& !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand)
				&& !this.piercedTargets.contains(entity.getUUID());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("StartX")) {
			this.startPos = new Vec3(nbt.getDouble("StartX"), nbt.getDouble("StartY"), nbt.getDouble("StartZ"));
		}
		if (nbt.contains("Damage")) {
			this.damage = nbt.getFloat("Damage");
		}
		if (nbt.contains("SpellLevel")) {
			setLevel(nbt.getInt("SpellLevel"));
		}
		if (nbt.contains("ExpBountyTen")) {
			this.expBountyTen = Math.max(0, nbt.getInt("ExpBountyTen"));
		}
		refundCastId = nbt.hasUUID("RefundCastId") ? nbt.getUUID("RefundCastId") : null;
		if (nbt.contains("PierceRemaining")) {
			this.pierceRemaining = nbt.getInt("PierceRemaining");
		}
		this.piercedTargets.clear();
		if (nbt.contains("PiercedTargets")) {
			net.minecraft.nbt.ListTag pierced = nbt.getList("PiercedTargets", 8);
			for (int i = 0; i < pierced.size(); i++) {
				try {
					this.piercedTargets.add(java.util.UUID.fromString(pierced.getString(i)));
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (startPos != null) {
			nbt.putDouble("StartX", startPos.x);
			nbt.putDouble("StartY", startPos.y);
			nbt.putDouble("StartZ", startPos.z);
		}
		nbt.putFloat("Damage", this.damage);
		nbt.putInt("SpellLevel", getSpellLevel());
		nbt.putInt("ExpBountyTen", this.expBountyTen);
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
		nbt.putInt("PierceRemaining", this.pierceRemaining);
		net.minecraft.nbt.ListTag pierced = new net.minecraft.nbt.ListTag();
		for (java.util.UUID uuid : this.piercedTargets) {
			pierced.add(net.minecraft.nbt.StringTag.valueOf(uuid.toString()));
		}
		nbt.put("PiercedTargets", pierced);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(Items.SNOWBALL);
	}
}
