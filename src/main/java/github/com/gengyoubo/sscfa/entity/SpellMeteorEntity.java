package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * 月尘魔法·陨火实体（jackcooper）。两阶段??
 * <ol>
	 *   <li><b>落点标记阶段</b>（??{@link #DELAY_TICKS} tick）：悬停在目标落点上空，服务端同?
	 *       预警参数，客户端按同一时间线生成火??烟圈粒子，提示落点范围；</li>
 *   <li><b>落下阶??/b>：从落点上??20 格急速坠落到落点，撞击后 AOE 结??
 *       （伤??+ 点??3s + 击退，白名单保护、不含施法者），火??爆炸音效/粒子，随即消失?/li>
 * </ol>
 * 伤害与半径由施法逻辑写入。可命中判定：标记阶段与下落中的实体本身不可被攻击（canHit ??false）??
 */
public class SpellMeteorEntity extends Projectile implements ItemSupplier {

	/** 钀界偣棰勮寤惰繜锛坱ick锛夈?0t = 0.5 绉掋?*/
	private static final int DELAY_TICKS = 10;
	/** 下落起始高度（落点上空）?*/
	private static final double FALL_HEIGHT = 20.0;
	/** 下落速度（??tick）??*/
	private static final double FALL_SPEED = 2.5;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("spells.meteor_entity");

	/** 魔法等级??-5），DataTracker 同步?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellMeteorEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RESUME_TICKS =
			SynchedEntityData.defineId(SpellMeteorEntity.class, EntityDataSerializers.INT);

	private int ticksAlive = 0;
	private float damage = 8.0f;
	/** AOE 半径（格）??*/
	private double radius = 3.0;
	private boolean falling = false;
	private boolean legacyFallingResume;
	private double[] warningVisualParams;
    private int runeFireTicks=60;
    public void setRemainingFireTicks(int ticks){runeFireTicks=Math.max(0,ticks);}

	/** 命中发放的经验赏金（×10 整数；exp_mode 1/2 挂起部分由施法时装入，NBT 持久化跨 tick）??*/
	private int expBountyTen = 0;

	public void setExpBountyTen(int expTen) {
		this.expBountyTen = Math.max(0, expTen);
	}

	/** 本次施法实际耗蓝（命中返还类流派用；与经验赏金同模式??tick ??NBT）??*/
	private java.util.UUID refundCastId;

	public void setRefundCastId(java.util.UUID castId) {
		this.refundCastId = castId;
	}

	public SpellMeteorEntity(EntityType<? extends SpellMeteorEntity> entityType, Level world) {
		super(entityType, world);
	}

	public SpellMeteorEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.SPELL_METEOR.get(), world);
		this.setOwner(owner);
	}

	@Override
	protected void defineSynchedData() {
		// 不??super（同冰锥写法：dataTracker ??Entity 构造器初始化??
		this.entityData.define(LEVEL, 1);
		this.entityData.define(RESUME_TICKS, 0);
	}

	/** 设置陨火 AOE 伤害?*/
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/** 设??AOE 半径（格）??*/
	public void setRadius(double radius) {
		this.radius = radius;
	}

	/** 魔法等级??-5）??*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 设置魔法等级（DataTracker 自动同步客户端）??*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 初始化落点：悬停位置 = 落点上??FALL_HEIGHT（施法时调用）?*/
	public void setImpactTarget(double x, double y, double z) {
		this.setPos(x, y + BAL.d("fall_height", FALL_HEIGHT), z);
	}

	@Override
	public void tick() {
		super.tick();
        if(this.isRemoved())return;
		if (level().isClientSide && ticksAlive == 0) {
			ticksAlive = entityData.get(RESUME_TICKS);
			falling = ticksAlive >= BAL.i("delay_ticks", DELAY_TICKS);
		}
		// Old saves lack the remaining fall time. Resolve at the saved position rather than
		// replaying a complete descent through the original impact point.
		if (legacyFallingResume && level() instanceof ServerLevel serverWorld) {
			legacyFallingResume = false;
			explode(serverWorld);
			return;
		}
		ticksAlive++;

		if (!falling) {
			// 阶??5：同函数多次读取局部变量化
			double fallHeight = BAL.d("fall_height", FALL_HEIGHT);
			// The fixed impact warning uses one shared timeline instead of one packet per ring point.
			if (!this.level().isClientSide && this.level() instanceof ServerLevel serverWorld) {
				int visualAgeOffset = this.tickCount - ticksAlive;
				if (warningVisualParams == null || warningVisualParams[0] != radius || warningVisualParams[1] != visualAgeOffset)
					warningVisualParams = new double[]{radius, visualAgeOffset};
				ParametricVisuals.touch(this, this.getUUID(), ParametricVisuals.Kind.METEOR_WARNING,
						null, this.position().add(0, -fallHeight, 0), false,
						serverWorld.getGameTime() - ticksAlive, BAL.i("delay_ticks", DELAY_TICKS),
						Math.hypot(radius, 0.5), warningVisualParams);
			}
			// 延迟期满：切入下落阶段（双端都切，客户端实体也要开始下落动画??
			if (ticksAlive >= BAL.i("delay_ticks", DELAY_TICKS)) {
				falling = true;
				if (!this.level().isClientSide) {
					this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
							SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
				}
			}
			return;
		}

		// 下落阶段：双端同步下坠（客户端按速度插值即可看到火球砸下）
        var homing=(github.com.gengyoubo.sscfa.spell.FormSpellStyle.HomingProjectile)this;
        boolean guided=homing.ssca$isHoming();
        double speedFactor=homing.ssca$flightSpeedMultiplier();
        double fallSpeed=BAL.d("fall_speed",FALL_SPEED)*speedFactor;
        if(!guided&&speedFactor!=1) {
            int fallAge=Math.max(0,ticksAlive-BAL.i("delay_ticks",DELAY_TICKS)-1);
            fallSpeed=Math.min(fallSpeed,Math.max(0,BAL.d("fall_height",FALL_HEIGHT)-fallAge*fallSpeed));
        }
		Vec3 velocity = new Vec3(0,-fallSpeed,0);
        velocity=homing.ssca$guidedVelocity(velocity);
        boolean weaving=((github.com.gengyoubo.sscfa.spell.SpiderSpellLink.WebProjectile)this).ssca$weave().enabled();
        if((guided||weaving)&&!this.level().isClientSide&&this.level() instanceof ServerLevel impactWorld) {
            Vec3 end=this.position().add(velocity);
            var wall=this.level().clip(new net.minecraft.world.level.ClipContext(this.position(),end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
            Vec3 clipped=wall.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?wall.getLocation():end;
            var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(this.level(),this,this.position(),end,
                    this.getBoundingBox().expandTowards(velocity).inflate(1),e->e!=this.getOwner()&&e instanceof LivingEntity&&e.isAlive());
            if(hit!=null&&hit.getLocation().distanceToSqr(this.position())<=clipped.distanceToSqr(this.position())){
                this.setPos(hit.getLocation());explode(impactWorld,(LivingEntity)hit.getEntity(),null);return;
            }
            if(wall.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){
                this.setPos(wall.getLocation());explode(impactWorld,null,wall);return;
            }
        }
		this.setPos(this.position().add(velocity));
		if (this.level() instanceof ServerLevel serverWorld) {
			ParametricVisuals.stop(this.getUUID(), ParametricVisuals.Kind.METEOR_WARNING);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLAME,
					this.getX(), this.getY(), this.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
		}

		// 服务端：下落阶段??tick ??FALL_SPEED，累?ceil(FALL_HEIGHT / FALL_SPEED) tick 后到达落点，引爆
		if (!this.level().isClientSide && this.level() instanceof ServerLevel impactWorld) {
			// 阶??5：同函数多次读取局部变量化（与标记阶段、下落速度保持同一 tick 快照一致性??
			double fallHeight2 = BAL.d("fall_height", FALL_HEIGHT);
			double fallSpeed2 = BAL.d("fall_speed", FALL_SPEED)*speedFactor;
			int delayTicks2 = BAL.i("delay_ticks", DELAY_TICKS);
			if (!guided&&ticksAlive >= delayTicks2 + Math.ceil(fallHeight2 / fallSpeed2)) {
                // 20/2.5等整除落点恰好落在方块上表面，射线终点可能不进入方块；只探测脚下薄层?
                var floor=weaving?impactWorld.clip(new net.minecraft.world.level.ClipContext(
                        this.position().add(0,.01,0),this.position().add(0,-.05,0),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this)):null;
                explode(impactWorld,null,floor!=null&&floor.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?floor:null);
			}
            if(guided&&ticksAlive>160)this.discard();
		}
	}

	/** 引爆：AOE 伤??+ 点??3s + 击退，白名单保护、不含施法者；火光爆炸演出后自毁??*/
	private void explode(ServerLevel serverWorld) {
        explode(serverWorld,null,null);
    }
    private void explode(ServerLevel serverWorld,LivingEntity direct,net.minecraft.world.phys.BlockHitResult ground) {
        var weave=((github.com.gengyoubo.sscfa.spell.SpiderSpellLink.WebProjectile)this).ssca$weave();
        if(direct!=null)weave.direct();
		double ix = this.getX();
		double iy = this.getY();
		double iz = this.getZ();
		boolean hitAnyTarget = false;
		LivingEntity lastHitTarget = null;   // 最后一个实际受伤目标（燎原燃烧返还判定用）
		LivingEntity killedTarget = null;    // 本次爆炸击杀的目标（优先传给命中钩子，击杀??50% 判定用）
		boolean hitBurningTarget = false;
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(radius), e -> e != this.getOwner() && e.isAlive());
		for (LivingEntity target : targets) {
			if (target.distanceTo(this) > radius) {
				continue;
			}
			// 距离衰减：中心满????边??40%
			double dist = target.distanceTo(this);
			float dmg = damage * (float) (1.0 - 0.6 * (dist / radius));
			if (dmg <= 0) {
				continue;
			}
			// 公共命中结算（白名单豁免 ??法术伤害 ??经验补发；流派钩子为聚合语义，循环外调）：??SpellHitHelper
			var hit = github.com.gengyoubo.sscfa.spell.SpellHitHelper.hitRaw(
					this, target, dmg, hitAnyTarget ? 0 : expBountyTen);
			if (hit != github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				continue;
			}
			hitBurningTarget |= target.getRemainingFireTicks() > 0;
            if(target==direct)github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,target);
			hitAnyTarget = true;
			lastHitTarget = target;
			// 击杀判定快照（damage 后立即查——修复：原先传给钩子的目标??filter(isAlive) 导致击杀分支永不可达?
			if (!target.isAlive() || target.getHealth() <= 0f) {
				killedTarget = target;
			}
			// 点??3s + 轻微击退（离开爆心方向??
			target.setRemainingFireTicks(runeFireTicks);
			Vec3 knock = new Vec3(target.getX() - ix, 0.1, target.getZ() - iz).normalize().scale(0.6);
			target.push(knock.x, knock.y, knock.z);
			target.hurtMarked = true;
		}
		// 流派命中钩子?026-09-17）：燎原按本次耗蓝返还（含击杀判定）；固定值类不依赖耗蓝??
		// 钩子目标优先取被击杀者（击杀?50% 分支），无击杀取最后受伤目标（燃烧 20% 分支?
		if (hitAnyTarget && this.getOwner() instanceof ServerPlayer styleOwner) {
			LivingEntity hookTarget = killedTarget != null ? killedTarget : lastHitTarget;
			github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(
					styleOwner, hookTarget,
					github.com.gengyoubo.sscfa.spell.FormationElement.FIRE, refundCastId, hitBurningTarget);
		}
		// 演出：爆炸粒??+ 火??+ 双层音效
		serverWorld.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ix, iy + 0.5, iz, 1, 0, 0, 0, 0);
		serverWorld.sendParticles(ParticleTypes.FLAME, ix, iy + 0.5, iz, 40, (double) (radius * 0.6), 0.4, (double) (radius * 0.6), 0.15);
		serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE, ix, iy + 0.5, iz, 20, (double) (radius * 0.5), 0.3, (double) (radius * 0.5), 0.1);
		this.level().playSound(null, ix, iy, iz,
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.1f);
		this.level().playSound(null, ix, iy, iz,
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.5f);
        if(ground!=null&&this.getOwner() instanceof ServerPlayer caster&&caster.level()==serverWorld)weave.ground(caster,ground);
		this.discard();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return false; // 纯区域魔法载体：不可被攻击命?
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
        runeFireTicks=nbt.contains("RuneFireTicks")?Math.max(0,nbt.getInt("RuneFireTicks")):60;
		if (nbt.contains("Damage")) {
			this.damage = nbt.getFloat("Damage");
		}
		if (nbt.contains("Radius")) {
			this.radius = nbt.getDouble("Radius");
		}
		if (nbt.contains("SpellLevel")) {
			setLevel(nbt.getInt("SpellLevel"));
		}
		if (nbt.contains("Falling")) {
			this.falling = nbt.getBoolean("Falling");
		}
		legacyFallingResume = this.falling && !nbt.contains("TicksAlive");
		this.ticksAlive = Math.max(0, nbt.contains("TicksAlive") ? nbt.getInt("TicksAlive")
				: this.falling ? BAL.i("delay_ticks", DELAY_TICKS) : 0);
		this.entityData.set(RESUME_TICKS, ticksAlive);
		if (nbt.contains("ExpBountyTen")) {
			this.expBountyTen = Math.max(0, nbt.getInt("ExpBountyTen"));
		}
		refundCastId = nbt.hasUUID("RefundCastId") ? nbt.getUUID("RefundCastId") : null;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
        nbt.putInt("RuneFireTicks",runeFireTicks);
		nbt.putFloat("Damage", this.damage);
		nbt.putDouble("Radius", this.radius);
		nbt.putInt("SpellLevel", getSpellLevel());
		nbt.putBoolean("Falling", this.falling);
		nbt.putInt("TicksAlive", this.ticksAlive);
		nbt.putInt("ExpBountyTen", this.expBountyTen);
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
	}

	@Override
	public ItemStack getItem() {
		// 渲染用：小规模火焰弹物品（FlyingItemEntityRenderer 精灵，客户端不必知道真实伤害?
		return new ItemStack(Items.FIRE_CHARGE);
		// 陨火本体主要靠粒子拖?+ 火焰弹精灵组合表?
	}
}
