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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 月尘魔法·月光箭投射物（月辉系，jackcooper）。结构与 {@link SpellFireBoltEntity} 同范式：
 * 朝准星直线匀速飞行（无重力），命中造成魔法伤害??b>对亡灵生物额??+50% 伤??/b>后消失；
 * 超??超时自毁。默认白名单：主人在线且目标受保护则不伤害??
 */
public class SpellMoonlightArrowEntity extends Projectile {

	private static final double SPEED = 1.0;         // 20 ??秒（比火球快，箭矢感??
	private static final double MAX_DISTANCE = 50.0; // 最大飞行距?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("spells.moonlight_arrow_entity");

	/** 魔法等级??-5），DataTracker 同步（预留渲染端换模??缩放，当前仅存档用）??*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellMoonlightArrowEntity.class, EntityDataSerializers.INT);

	private Vec3 startPos;
	private int ticksAlive = 0;
	private float damage = 4.0f;

	/** 命中发放的经验赏金（×10 整数；exp_mode 1/2 挂起部分由施法时装入，NBT 持久化跨 tick）??*/
	private int expBountyTen = 0;

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

	public SpellMoonlightArrowEntity(EntityType<? extends SpellMoonlightArrowEntity> entityType, Level world) {
		super(entityType, world);
		this.startPos = this.position();
	}

	public SpellMoonlightArrowEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.SPELL_MOONLIGHT_ARROW.get(), world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.startPos = this.position();
	}

	@Override
	protected void defineSynchedData() {
		// 不??super（Entity 的抽象方法无法跨两级访问；dataTracker ??Entity 构造器初始化，同冰锥写法）
		this.entityData.define(LEVEL, 1);
	}

	/** 设置命中伤害?*/
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/** 魔法等级??-5）??*/
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

	/** 按速度自算朝向（与寒棘狐冰锥同款公式：尖朝速度方向），供渲染对正?*/
	private void updateRotationFromVelocity(Vec3 v) {
		double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
		this.setYRot((float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Mth.atan2(-v.y, horiz) * (180.0 / Math.PI))); // 取负：与寒棘?冰锥惯例一致，配合渲染?-pitch 旋??
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

		// 超??/ 超时自毁（仅服务端权威，理由同冰锥：客户端实??startPos 恒原点，双端判会误删?
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

		// 飞行拖尾粒子（服务端撒，天然多人同步）：月光尾迹
		if (this.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.END_ROD,
					this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		Entity target = entityHitResult.getEntity();
		if (target instanceof LivingEntity livingTarget && !this.level().isClientSide) {
			// 对亡灵生物（僵??骷??幽灵等）额外增伤：每?+10%（L1=+10% ??L5=+50%??
			float finalDamage = livingTarget.isInvertedHealAndHarm() ? damage * (1.0f + 0.1f * getSpellLevel()) : damage;
			// 公共命中结算（白名单豁免 ??法术伤害 ??经验补发 ??流派钩子）：?SpellHitHelper
			github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult hit =
					github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
							this, livingTarget, finalDamage,
							github.com.gengyoubo.sscfa.spell.FormationElement.LUNAR, refundCastId, expBountyTen);
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
				expBountyTen = 0; // 经验已发放，清零防重??
			}
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.PROTECTED) {
				return; // 白名单豁免：不播??
			}
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 命中演出：月辉爆闪消散（实体命中的伤害??onEntityHit，这里统一粒子 + 收尾??
			if (this.level() instanceof ServerLevel serverWorld) {
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.END_ROD,
						this.getX(), this.getY(), this.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
			}
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 排除盔甲架（与月灵光弹同口径）：假人不产返??经??
		return super.canHitEntity(entity) && entity != this.getOwner()
				&& entity instanceof LivingEntity && !(entity instanceof ArmorStand);
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
		this.expBountyTen = Math.max(0, nbt.getInt("ExpBountyTen"));
		refundCastId = nbt.hasUUID("RefundCastId") ? nbt.getUUID("RefundCastId") : null;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (startPos != null) {
			nbt.putDouble("StartX", startPos.x);
			nbt.putDouble("StartY", startPos.y);
			nbt.putDouble("StartZ", startPos.z);
		}
		nbt.putInt("ExpBountyTen", this.expBountyTen);
		nbt.putFloat("Damage", this.damage);
		nbt.putInt("SpellLevel", getSpellLevel());
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
