package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 杩涘寲缇庤タ铻堛€屾姇鎺锋按鐭涖€嶇殑鐩寸嚎姘寸煕鎶曞皠鐗┿€?
 *
 * <p>鏃犻噸鍔涖€佸寑閫熺洿绾块琛岋紙浠?{@link FrostBallEntity}锛夛紱鐩村嚮鐩爣 12 鐐圭墿鐞嗕激瀹筹紝
 * 鍛戒腑鐐?2 鏍煎崐寰勫唴棰濆 5 鐐硅寖鍥翠激瀹筹紱瀵规晫鏂?NPC 鐨勪袱绉嶄激瀹冲潎鎻愰珮 50%銆?
 * 榛樿鐧藉悕鍗曪細璞佸厤鐜╁ / 瀹犵墿 / 鐧藉悕鍗曚釜浣撱€?
 * 鎵€鏈夊垽瀹氬湪鏈嶅姟绔€?/p>
 */
public class ThrownWaterSpearEntity extends Projectile {

	private static final double SPEED = 8.5;          // 鏍?tick锛堥珮閫熺洿鍒猴級
	private static final double MAX_DISTANCE = 64.0;  // 鏈€澶ч琛岃窛绂?
	private static final float DIRECT_DAMAGE = 12.0f; // 鐩村嚮鐗╃悊浼ゅ
	private static final float AOE_DAMAGE = 5.0f;     // 鑼冨洿浼ゅ
	private static final double AOE_RADIUS = 2.0;     // 鑼冨洿鍗婂緞

	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final BalanceReader BAL = new BalanceReader("abilities.water_spear_entity");

	// 鍏ㄧ簿搴﹂€熷害鍚屾锛氱敓鎴愬寘 velocity 鐢?short 缂栫爜锛堢害 卤3.9 鏍?tick锛夛紝8.5 鏍?tick 浼氳閫愬垎閲?clamp
	// 鈫?瀹㈡埛绔?velocity 鏂瑰悜澶辩湡 + 鍙樻參锛岃瑙夋柟鍚?浣嶇疆涓庢湇鍔＄鐪熷疄鍛戒腑涓嶇銆傛敼鐢?DataTracker 鍚屾鍏ㄧ簿搴﹂€熷害銆?
	private static final EntityDataAccessor<Float> VEL_X = SynchedEntityData.defineId(ThrownWaterSpearEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> VEL_Y = SynchedEntityData.defineId(ThrownWaterSpearEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> VEL_Z = SynchedEntityData.defineId(ThrownWaterSpearEntity.class, EntityDataSerializers.FLOAT);

	private Vec3 startPos;
	private int ticksAlive = 0;

	public ThrownWaterSpearEntity(EntityType<? extends ThrownWaterSpearEntity> entityType, Level world) {
		super(entityType, world);
		this.startPos = this.position();
	}

	public ThrownWaterSpearEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.SscAddon.THROWN_WATER_SPEAR_ENTITY, world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.startPos = this.position();
	}

	/** 璁剧疆椋炶鏂瑰悜锛堝綊涓€鍖栧悗鎸?SPEED 璧嬮€燂級锛屽苟璁╁疄浣撴湞鍚戦琛屾柟鍚戙€?*/
	public void setDirection(Vec3 direction) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED));
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
		// 鍏ㄧ簿搴﹂€熷害鍐欏叆 DataTracker锛屼緵瀹㈡埛绔簿纭鐜伴琛屾柟鍚?閫熷害
		this.entityData.set(VEL_X, (float) velocity.x);
		this.entityData.set(VEL_Y, (float) velocity.y);
		this.entityData.set(VEL_Z, (float) velocity.z);
		double horiz = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		float yaw = (float) (net.minecraft.util.Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));
		float pitch = (float) (net.minecraft.util.Mth.atan2(velocity.y, horiz) * (180.0 / Math.PI));
		this.setYRot(yaw);
		this.yRotO = yaw;
		this.setXRot(pitch);
		this.xRotO = pitch;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(VEL_X, 0.0f);
		this.entityData.define(VEL_Y, 0.0f);
		this.entityData.define(VEL_Z, 0.0f);
	}

	@Override
	public void tick() {
		super.tick();
		ticksAlive++;
		// 瀹㈡埛绔敤 DataTracker 鍏ㄧ簿搴﹂€熷害瑕嗙洊琚敓鎴愬寘鎴柇/澶辩湡鐨?velocity锛屼繚璇佽瑙夋柟鍚?浣嶇疆涓庢湇鍔＄涓€鑷?
		if (this.level().isClientSide) {
			this.setDeltaMovement(this.entityData.get(VEL_X), this.entityData.get(VEL_Y), this.entityData.get(VEL_Z));
		}
		Vec3 velocity = this.getDeltaMovement();

		// 纰版挒鍒ゅ畾浠呮湇鍔＄锛屼笖鍦ㄧЩ鍔ㄥ墠鍋氾細getCollision 鎵帬 褰撳墠浣嶇疆鈫掑綋鍓?velocity锛堟湰 tick 灏嗙粡杩囩殑鏁存璺緞锛夛紝
		// 楂橀€燂紙8.5 鏍?tick锛変笅涔熶笉浼氭紡杩戣窛绂荤洰鏍囥€?
		// 锛堝彟锛氬垽瀹氫粎鏈嶅姟绔€斺€斿鎴风 startPos 鏈殢鐢熸垚鍖呭悓姝ャ€佹瀯閫犳椂涓?(0,0,0)锛岃嫢鍦ㄥ鎴风璺戣秴璺濆垽瀹氫細琚珛鍗?discard锛?
		// 瀵艰嚧鎶曞皠鐗╁湪瀹㈡埛绔灛闂存秷澶便€佺湅涓嶅埌妯″瀷銆傦級
		if (!this.level().isClientSide) {
			HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
			if (hitResult.getType() != HitResult.Type.MISS) {
				this.onHit(hitResult);
			}
			if (this.isRemoved()) return;
		}

		// 鏃犻噸鍔涘寑閫熺Щ鍔紙瀹㈡埛绔篃澶栨帹浣嶇疆锛屼繚璇侀琛屽钩婊戯級
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
		this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
		// 鏈濆悜椋炶鏂瑰悜锛堜緵 3D 娓叉煋鍣ㄦ憜姝ｆ按鐭涳級
		double horiz = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		if (horiz > 1.0e-6 || velocity.y != 0) {
			this.setYRot((float) (net.minecraft.util.Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI)));
			this.setXRot((float) (net.minecraft.util.Mth.atan2(velocity.y, horiz) * (180.0 / Math.PI)));
		}

		// 瓒呰窛 / 瓒呮椂閿€姣?+ 鎷栧熬 浠呮湇鍔＄
		if (!this.level().isClientSide) {
			double maxDistance = BAL.d("max_distance", MAX_DISTANCE);
			if (startPos != null && this.distanceToSqr(startPos) > maxDistance * maxDistance) {
				this.discard();
				return;
			}
			if (ticksAlive > 60) {
				this.discard();
				return;
			}
			// 姘寸煕椋炶姘磋姳鎷栧熬
			ParticleUtils.spawnParticles((ServerLevel) this.level(), ParticleTypes.BUBBLE,
					this.getX(), this.getY(), this.getZ(), 3, 0.08, 0.08, 0.08, 0.02);
			ParticleUtils.spawnParticles((ServerLevel) this.level(), ParticleTypes.SPLASH,
					this.getX(), this.getY(), this.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		if (this.level().isClientSide) return;
		Entity target = entityHitResult.getEntity();
		if (target instanceof LivingEntity living) {
			// 鐩村嚮 12 鐗╃悊锛堢櫧鍚嶅崟璞佸厤锛?
			if (!(this.getOwner() instanceof net.minecraft.server.level.ServerPlayer ownerP)
					|| !WhitelistUtils.isProtected(ownerP, living)) {
				living.hurt(this.damageSources().mobAttack(this.getOwner() instanceof LivingEntity l ? l : null),
						NpcCombatEnhancements.scale(this.getOwner(), living,
								(float) BAL.d("direct_damage", DIRECT_DAMAGE), "upgrade_axolotl_primary_damage", 1.5));
			}
		}
		// 鍛戒腑鐐硅寖鍥翠激瀹?
		explodeAoe(target);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level().isClientSide) return;
		if (hitResult.getType() == HitResult.Type.BLOCK) {
			explodeAoe(null); // 鎾炴柟鍧椾篃鍋氳寖鍥存按鑺变激瀹?
		}
		if (!this.isRemoved()) {
			this.discard();
		}
	}

	/** 鍛戒腑鐐?2 鏍煎崐寰勮寖鍥翠激瀹?+ 姘磋姳鐗规晥闊虫晥锛堟湇鍔＄锛夈€?*/
	private void explodeAoe(Entity directTarget) {
		if (!(this.level() instanceof ServerLevel sw)) return;
		sw.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.2f, 0.9f);
		ParticleUtils.spawnWaterBurst(sw, this.getX(), this.getY(), this.getZ(), 1.2);
		net.minecraft.server.level.ServerPlayer ownerP =
				this.getOwner() instanceof net.minecraft.server.level.ServerPlayer p ? p : null;
		LivingEntity ownerLiving = this.getOwner() instanceof LivingEntity l ? l : null;
		AABB box = this.getBoundingBox().inflate(BAL.d("aoe_radius", AOE_RADIUS));
		for (Entity e : sw.getEntities(this, box)) {
			if (e == directTarget) continue; // 鐩村嚮鐩爣涓嶉噸澶嶅彈 AOE
			if (e instanceof LivingEntity living) {
				if (ownerP != null && WhitelistUtils.isProtected(ownerP, living)) continue;
				living.hurt(this.damageSources().mobAttack(ownerLiving),
						NpcCombatEnhancements.scale(ownerP, living,
								(float) BAL.d("aoe_damage", AOE_DAMAGE), "upgrade_axolotl_primary_damage", 1.5));
			}
		}
		this.discard();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != this.getOwner() && entity instanceof LivingEntity;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (startPos != null) {
			nbt.putDouble("StartX", startPos.x);
			nbt.putDouble("StartY", startPos.y);
			nbt.putDouble("StartZ", startPos.z);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("StartX")) {
			this.startPos = new Vec3(nbt.getDouble("StartX"), nbt.getDouble("StartY"), nbt.getDouble("StartZ"));
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
