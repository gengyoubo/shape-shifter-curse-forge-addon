package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 月尘魔法·诅咒标记投射物（诅咒系，jackcooper）。结构与 {@link SpellFireBoltEntity} 同范式：
 * 朝准星直线匀速飞行（无重力），命中目标后施加「诅咒标记」状态（受伤 +20%，时长由施法定）后消失??
 * 超??超时自毁。默认白名单：主人在线且目标受保护则不施加??
 */
public class SpellCurseMarkEntity extends Projectile implements ItemSupplier {

	private static final double SPEED = 0.7;         // 14 ??秒（咒印缓慢感）
	private static final double MAX_DISTANCE = 30.0; // 最大飞行距?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("spells.curse_mark_entity");

	/** 魔法等级??-5），DataTracker 同步?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellCurseMarkEntity.class, EntityDataSerializers.INT);
	/** 标记时长（tick），DataTracker 同步（渲染端预留）??*/
	private static final EntityDataAccessor<Integer> DURATION =
			SynchedEntityData.defineId(SpellCurseMarkEntity.class, EntityDataSerializers.INT);

	private Vec3 startPos;
	private int ticksAlive = 0;
	private java.util.UUID refundCastId;

	public void setRefundCastId(java.util.UUID castId) {
		refundCastId = castId;
	}

	public SpellCurseMarkEntity(EntityType<? extends SpellCurseMarkEntity> entityType, Level world) {
		super(entityType, world);
		this.startPos = this.position();
	}

	public SpellCurseMarkEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.SPELL_CURSE_MARK.get(), world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.startPos = this.position();
	}

	@Override
	protected void defineSynchedData() {
		// 不??super（Entity 的抽象方法无法跨两级访问；dataTracker ??Entity 构造器初始化，同冰锥写法）
		this.entityData.define(LEVEL, 1);
		this.entityData.define(DURATION, 160);
	}

	/** 魔法等级??-5）??*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 设置魔法等级（服务端施法时调用，DataTracker 自动同步客户端）??*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 标记时长（tick）??*/
	public int getDuration() {
		return this.entityData.get(DURATION);
	}

	/** 设置标记时长（tick）??*/
	public void setDuration(int durationTicks) {
		this.entityData.set(DURATION, Math.max(20, durationTicks));
	}

	/** 设置飞行方向（朝准星），速度按等级倍率缩放?*/
	public void setDirection(Vec3 direction, float speedMultiplier) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED) * speedMultiplier);
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
		updateRotationFromVelocity(velocity);
	}

	/** 按速度自算朝向（同冰锥公式），供渲染对正??*/
	private void updateRotationFromVelocity(Vec3 v) {
		double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
		this.setYRot((float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Mth.atan2(v.y, horiz) * (180.0 / Math.PI)));
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

		// 飞行拖尾粒子（服务端撒，天然多人同步）：暗紫咒印
		if (this.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.WITCH,
					this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		Entity target = entityHitResult.getEntity();
		if (target instanceof LivingEntity livingTarget && !this.level().isClientSide) {
			// 默认白名单：主人在线且目标受保护 ??不施加标??
			if (this.getOwner() instanceof ServerPlayer ownerPlayer
					&& WhitelistUtils.isProtected(ownerPlayer, livingTarget)) {
				return;
			}
			// amplifier = 等??1（L1=0 ??L5=4）：mixin 按其计算受伤加??1.2+0.1×amp
			boolean hadHarmfulEffect = github.com.gengyoubo.sscfa.spell.FormCastingStyle.hasHarmfulEffect(livingTarget);
            boolean applied=github.com.gengyoubo.sscfa.spell.CourtSpellLink.with(github.com.gengyoubo.sscfa.spell.CourtSpellLink.of(this),()->github.com.gengyoubo.sscfa.spell.NightmareSpellLink.with(this.getOwner(),()->github.com.gengyoubo.sscfa.spell.BatSpellLink.with(github.com.gengyoubo.sscfa.spell.BatSpellLink.of(this),()->livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.CURSE_MARK.get(),getDuration(),getSpellLevel()-1)))));
			if (github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(
                    livingTarget, github.com.gengyoubo.sscfa.init.AddonEffects.CURSE_MARK.get(), getDuration(), getSpellLevel()-1, applied))
                github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(this,livingTarget);
			github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
			if (applied && this.getOwner() instanceof ServerPlayer caster) {
				github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(caster, livingTarget,
						github.com.gengyoubo.sscfa.spell.FormationElement.CURSE, refundCastId, hadHarmfulEffect);
			}
            var link=github.com.gengyoubo.sscfa.spell.AllaySpellLink.context(this);
            if(link!=null&&this.getOwner() instanceof ServerPlayer caster)link.chain(caster,livingTarget,java.util.Set.of(livingTarget.getUUID()),other->{
                boolean before=github.com.gengyoubo.sscfa.spell.FormCastingStyle.hasHarmfulEffect(other);
                boolean changed=other.addEffect(new net.minecraft.world.effect.MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.CURSE_MARK.get(),getDuration(),getSpellLevel()-1),caster);
                if(github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(
                        other,github.com.gengyoubo.sscfa.init.AddonEffects.CURSE_MARK.get(),getDuration(),getSpellLevel()-1,changed))
                    github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(this,other);
                if(changed)github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(caster,other,
                        github.com.gengyoubo.sscfa.spell.FormationElement.CURSE,refundCastId,before);
                return changed;
            });
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0f, 1.2f);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 命中演出：暗紫咒印爆?
			if (this.level() instanceof ServerLevel serverWorld) {
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.WITCH,
						this.getX(), this.getY(), this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
			}
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 排除盔甲架：假人不吃诅咒标记，避免浪费弹??
		return super.canHitEntity(entity) && entity != this.getOwner()
				&& entity instanceof LivingEntity && !(entity instanceof ArmorStand);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		refundCastId = nbt.hasUUID("RefundCastId") ? nbt.getUUID("RefundCastId") : null;
		if (nbt.contains("StartX")) {
			this.startPos = new Vec3(nbt.getDouble("StartX"), nbt.getDouble("StartY"), nbt.getDouble("StartZ"));
		}
		if (nbt.contains("SpellLevel")) {
			setLevel(nbt.getInt("SpellLevel"));
		}
		if (nbt.contains("MarkDuration")) {
			setDuration(nbt.getInt("MarkDuration"));
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
		if (startPos != null) {
			nbt.putDouble("StartX", startPos.x);
			nbt.putDouble("StartY", startPos.y);
			nbt.putDouble("StartZ", startPos.z);
		}
		nbt.putInt("SpellLevel", getSpellLevel());
		nbt.putInt("MarkDuration", getDuration());
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override
	public net.minecraft.world.item.ItemStack getItem() {
		// 用恶魂之泪渲染（咒印意象??
		return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GHAST_TEAR);
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}
}
