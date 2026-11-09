package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/**
 * SP雪狐远程次要技?- 冰风暴实?
 * 持续10秒，伤害半径3.5格，每秒2点魔法伤?
 * 吸附速度2b/s?格内），6-10格吸附减?
 */
public class FrostStormEntity extends Entity {

	private static final int DURATION = 200; // 10绉?
	private static final double DAMAGE_RADIUS = 3.5;
	private static final double PULL_RADIUS_STRONG = 6.0;
	private static final double PULL_RADIUS_WEAK = 10.0;
	private static final float DAMAGE_PER_SECOND = 2.0f;
	private static final double PULL_SPEED = 0.1; // 2鏍?绉?= 0.1鏍?tick

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_storm");

    private int stormDuration = BAL.i("duration", DURATION);
    private double stormDamageRadius = BAL.d("damage_radius", DAMAGE_RADIUS);
    private double stormPullStrong = BAL.d("pull_radius_strong", PULL_RADIUS_STRONG);
    private double stormPullWeak = BAL.d("pull_radius_weak", PULL_RADIUS_WEAK);
    private double stormDamage = BAL.d("damage_per_second", DAMAGE_PER_SECOND);
    private double stormPullSpeed = BAL.d("pull_speed", PULL_SPEED);

	private int ticksAlive = 0;
	private UUID ownerUuid;
	/** 本次施放?castId（修审查#3：旧风暴不能误伤新施放的记录；NBT 持久化）?*/
	private long boundCastId = 0;

	/** 绑定本次施放（生成时?SnowFoxSpFrostStorm 调用）?*/
	public void bindCast(long castId) {
		this.boundCastId = castId;
        if (level() instanceof ServerLevel sw) {
            github.com.gengyoubo.sscfa.cooldown.SkillCastManager.get(sw).bindEntity(castId, getUUID());
        }
	}

	public UUID getDecorationOwner() { return ownerUuid; }

	public FrostStormEntity(EntityType<?> entityType, Level world) {
		super(entityType, world);
		this.noPhysics = true;
	}

	public FrostStormEntity(Level world, double x, double y, double z, Player owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.FROST_STORM.get(), world);
		this.setPos(x, y, z);
		this.ownerUuid = owner.getUUID();
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		// 暂时不需要初始化数据跟踪?
	}

	/** 风暴消失即冰风暴效果结束：凭 castId 精确结束本次施放，旧风暴不误伤新施放?*/
	private void settleOwnerEffectEnd() {
		if (this.level().isClientSide || !(this.level() instanceof ServerLevel sw) || ownerUuid == null) return;
        var manager = github.com.gengyoubo.sscfa.cooldown.SkillCastManager.get(sw);
        var cast = manager.control(ownerUuid, github.com.gengyoubo.sscfa.ability.SnowFoxSpFrostStorm.SKILL_ID);
        if (cast != null && cast.castId == boundCastId && (cast.persistentEntity == null || getUUID().equals(cast.persistentEntity))) {
            manager.finish(boundCastId, sw.getServer().overworld().getGameTime());
        }
	}

	@Override
    public void remove(RemovalReason reason) {
        // 区块卸载也结算：风暴远离玩家?CD 照常起算，不会因实体未加载而锁死技?
        if (!isRemoved() && reason.shouldDestroy()) settleOwnerEffectEnd();
        super.remove(reason);
    }

	@Override
	public void tick() {
		super.tick();
		ticksAlive++;

		if (!this.level().isClientSide && ticksAlive > stormDuration) {
			// 自然到期：结算（discard() 内统一处理，见下）
			this.discard();
			return;
		}

		if (!this.level().isClientSide && this.level() instanceof ServerLevel serverWorld) {
			// 同函数多次读取局部变量化
			int duration = stormDuration;
			double damageRadius = stormDamageRadius;

			// damage_per_second is one full hit per 20 server ticks, phased by persisted lifetime.
			if (ticksAlive % 20 == 0) {
				dealDamage(serverWorld);
			}

			// 每tick吸附敌人
			pullEntities();

			// 同步时间轴，雪花与旋转云由客户端按原密度生成?
			github.com.gengyoubo.sscfa.network.SustainedVisuals.touch(this,
					github.com.gengyoubo.sscfa.network.VisualRecipe.Kind.FROST_STORM,
					ticksAlive, duration, damageRadius, 0);

			// 播放环境音效
			if (ticksAlive % 40 == 0) {
				this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
						SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.HOSTILE, 0.5f, 0.5f);
			}
		}
	}

	private void dealDamage(ServerLevel world) {
		// 同函数多次读取局部变量化
		double damageRadius = stormDamageRadius;
		AABB damageBox = new AABB(
				this.getX() - damageRadius, this.getY() - 1, this.getZ() - damageRadius,
				this.getX() + damageRadius, this.getY() + 3, this.getZ() + damageRadius
		);

		List<LivingEntity> targets = world.getEntitiesOfClass(
				LivingEntity.class, damageBox,
				entity -> entity.getUUID() != ownerUuid && entity.isAlive()
		);

		Player owner = ownerUuid != null ? world.getPlayerByUUID(ownerUuid) : null;

		for (LivingEntity target : targets) {
			double dist = this.distanceToSqr(target.getX(), this.getY(), target.getZ());
			if (dist <= damageRadius * damageRadius) {
				if (WhitelistUtils.isProtected(ownerUuid, world, target)) continue;
				DamageSource source = owner != null
						? target.damageSources().playerAttack(owner)
						: target.damageSources().magic();
				// Online caster damage is scaled centrally. The surviving offline storm has no attacker attribution.
				float damage = (float) stormDamage;
				if (owner == null && !(target instanceof Player)) damage *= (float) github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements.value("snow_fox_damage", 1.5);
				if(target.hurt(source, damage)&&owner instanceof net.minecraft.server.level.ServerPlayer caster)
					github.com.gengyoubo.sscfa.spell.FrostSpellLink.onNativeHit(caster,target);
			}
		}
	}

	private void pullEntities() {
		// 同函数多次读取局部变量化
		double pullRadiusWeak = stormPullWeak;
		double pullRadiusStrong = stormPullStrong;
		double pullSpeed = stormPullSpeed;
		AABB pullBox = new AABB(
				this.getX() - pullRadiusWeak, this.getY() - 2, this.getZ() - pullRadiusWeak,
				this.getX() + pullRadiusWeak, this.getY() + 4, this.getZ() + pullRadiusWeak
		);

		List<LivingEntity> targets = this.level().getEntitiesOfClass(
				LivingEntity.class, pullBox,
				entity -> entity.getUUID() != ownerUuid && entity.isAlive()
		);

		Vec3 center = new Vec3(this.getX(), this.getY(), this.getZ());
		ServerLevel pullWorld = this.level() instanceof ServerLevel sw ? sw : null;

		for (LivingEntity target : targets) {
			Vec3 targetPos = target.position();
			double dist = Math.sqrt(target.distanceToSqr(this.getX(), this.getY(), this.getZ()));

			if (dist > pullRadiusWeak || dist < 0.5) continue;
			if (pullWorld != null && WhitelistUtils.isProtected(ownerUuid, pullWorld, target)) continue;

			// 计算吸附速度
			double pullStrength;
			if (dist <= pullRadiusStrong) {
				pullStrength = pullSpeed; // 正常吸附速度
			} else {
				// 6-10格，吸附减弱
				double factor = 1.0 - ((dist - pullRadiusStrong) / (pullRadiusWeak - pullRadiusStrong));
				pullStrength = pullSpeed * factor * 0.3; // 楠ゅ噺鍚搁檮
			}

			// 计算吸附方向
			Vec3 direction = center.subtract(targetPos).normalize();
			Vec3 pullVelocity = direction.scale(pullStrength);

			// 应用吸附
			Vec3 newVelocity = target.getDeltaMovement().add(pullVelocity);
			target.setDeltaMovement(newVelocity);
			target.hurtMarked = true;
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("Balance", 10)) {
            CompoundTag values = nbt.getCompound("Balance");
            if (values.contains("duration")) stormDuration = values.getInt("duration");
            if (values.contains("damage_radius")) stormDamageRadius = values.getDouble("damage_radius");
            if (values.contains("pull_radius_strong")) stormPullStrong = values.getDouble("pull_radius_strong");
            if (values.contains("pull_radius_weak")) stormPullWeak = values.getDouble("pull_radius_weak");
            if (values.contains("damage_per_second")) stormDamage = values.getDouble("damage_per_second");
            if (values.contains("pull_speed")) stormPullSpeed = values.getDouble("pull_speed");
        }
		this.ticksAlive = nbt.getInt("TicksAlive");
		if (nbt.hasUUID("Owner")) {
			this.ownerUuid = nbt.getUUID("Owner");
		}
		this.boundCastId = nbt.getLong("BoundCastId");
        if (boundCastId > 0 && level() instanceof ServerLevel sw) {
            var manager = github.com.gengyoubo.sscfa.cooldown.SkillCastManager.get(sw);
            var cast = manager.control(ownerUuid, github.com.gengyoubo.sscfa.ability.SnowFoxSpFrostStorm.SKILL_ID);
            if (cast != null && cast.castId == boundCastId) manager.bindEntity(boundCastId, getUUID());
        }
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
        CompoundTag values = new CompoundTag();
        values.putInt("duration", stormDuration);
        values.putDouble("damage_radius", stormDamageRadius);
        values.putDouble("pull_radius_strong", stormPullStrong);
        values.putDouble("pull_radius_weak", stormPullWeak);
        values.putDouble("damage_per_second", stormDamage);
        values.putDouble("pull_speed", stormPullSpeed);
        nbt.put("Balance", values);
		nbt.putInt("TicksAlive", this.ticksAlive);
		nbt.putLong("BoundCastId", this.boundCastId);
		if (ownerUuid != null) {
			nbt.putUUID("Owner", ownerUuid);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return super.getAddEntityPacket();
	}

	@Override
	public boolean canBeCollidedWith() {
		return super.canBeCollidedWith();
	}
}
