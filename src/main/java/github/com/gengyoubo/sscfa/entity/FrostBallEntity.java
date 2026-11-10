package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.List;

/**
 * SP闆嫄杩滅▼涓昏鎶€鑳?- 娉曟湳鍐扮悆
 * 鏃犱笅鍧狅紝椋炶閫熷害10b/s锛屾渶澶氶琛?5鏍?
 * 鍛戒腑鏂藉姞闇滈檷鏁堟灉4绉?
 */
public class FrostBallEntity extends Projectile implements ItemSupplier {

	private static final double SPEED = 0.75; // 15鏍?绉?= 0.75鏍?tick锛堝師10鏍?绉掑鍔?.5鍊嶏級
	private static final double MAX_DISTANCE = 50.0; // 鏈€澶ч琛岃窛绂伙紙鍘?5鏍煎鍔?鍊嶏級
	private static final int FROST_FALL_DURATION = 80; // 闇滈檷鎸佺画4绉?
	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_ball");
	private Vec3 startPos;
	private int ticksAlive = 0;

	// 杩借釜鐩稿叧
	private LivingEntity trackingTarget;
	private boolean isChild = false;

	public FrostBallEntity(EntityType<? extends FrostBallEntity> entityType, Level world) {
		super(entityType, world);
		this.startPos = this.position();
	}

	public FrostBallEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.SscAddon.FROST_BALL_ENTITY, world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.startPos = this.position();
	}

	public void setTrackingTarget(LivingEntity target) {
		this.trackingTarget = target;
	}

	public void setIsChild(boolean isChild) {
		this.isChild = isChild;
	}

	/**
	 * 璁剧疆鍐扮悆鐨勯琛屾柟鍚?
	 */
	public void setDirection(Vec3 direction) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED));
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
	}

	@Override
	protected void defineSynchedData() {
		// 涓嶉渶瑕侀澶栫殑鏁版嵁杩借釜鍣?
	}

	@Override
	public void tick() {
		super.tick();
		ticksAlive++;

		// 杩借釜閫昏緫
		if (this.trackingTarget != null && this.trackingTarget.isAlive()) {
			Vec3 targetPos = this.trackingTarget.getEyePosition();
			Vec3 currentPos = this.position();
			Vec3 direction = targetPos.subtract(currentPos).normalize();

			// 绠€鍗曠殑杩借釜杞悜
			Vec3 currentVel = this.getDeltaMovement().normalize();
			// 0.2f 鐨勮浆鍚戠郴鏁?
			Vec3 newVel = currentVel.add(direction.scale(0.2)).normalize().scale(BAL.d("speed", SPEED));

			this.setDeltaMovement(newVel.x, newVel.y, newVel.z);
		}

		// 鏃犻噸鍔涚Щ鍔?
		Vec3 velocity = this.getDeltaMovement();
		this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);

		// 妫€娴嬬鎾?
		HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hitResult.getType() != HitResult.Type.MISS) {
			this.onHit(hitResult);
		}

		// 濡傛灉鍦ㄧ鎾炲鐞嗕腑瀹炰綋琚Щ闄や簡锛堟瘮濡傛挒鍒颁簡浠€涔堬級锛屽氨涓嶅啀绉诲姩
		if (this.isRemoved()) return;

		// 妫€鏌ユ槸鍚﹁秴杩囨渶澶ч琛岃窛绂伙紙灞€閮ㄥ彉閲忓寲锛氭湰鍑芥暟鍐呭彧璇讳竴娆″揩鐓э級
		double maxDistance = BAL.d("max_distance", MAX_DISTANCE);
		if (startPos != null && this.distanceToSqr(startPos) > maxDistance * maxDistance) {
			this.discard();
			return;
		}

		// 瓒呮椂妫€鏌ワ紙闃叉鏃犻檺椋炶锛?
		if (ticksAlive > 100) { // 5绉掕秴鏃?
			this.discard();
			return;
		}

		// 鐢熸垚绮掑瓙鏁堟灉
		if (this.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
					this.getX(), this.getY(), this.getZ(),
					2, 0.1, 0.1, 0.1, 0.02);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);

		if (!this.level().isClientSide) {
			// 鎾斁鍑讳腑闊虫晥
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);

			// 鐢熸垚鐖嗙偢绮掑瓙
			if (this.level() instanceof ServerLevel serverWorld) {
				github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
						this.getX(), this.getY(), this.getZ(),
						15, 0.3, 0.3, 0.3, 0.1);
			}

			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);

		Entity target = entityHitResult.getEntity();
		if (target instanceof LivingEntity livingTarget && !this.level().isClientSide) {
			// 鐧藉悕鍗曟鏌ワ細濡傛灉涓讳汉鍦ㄧ嚎涓旂洰鏍囧彈淇濇姢锛屽垯璺宠繃鏁堟灉涓庤拷韪鐗?
			if (this.getOwner() instanceof net.minecraft.server.level.ServerPlayer ownerPlayer
					&& WhitelistUtils.isProtected(ownerPlayer, livingTarget)) {
				return;
			}
			// 鏂藉姞闇滈檷鏁堟灉锛堝甫涓讳汉 source 渚涘叆姊︽嫤鎴綊鍥狅級
			boolean applied=livingTarget.addEffect(new MobEffectInstance(
					github.com.gengyoubo.sscfa.SscAddon.FROST_FALL,
					BAL.i("frost_fall_duration", FROST_FALL_DURATION),
					0,
					false,
					true,
					true
			), this.getOwner());
			if(applied&&getOwner() instanceof net.minecraft.server.level.ServerPlayer caster)
				github.com.gengyoubo.sscfa.spell.FrostSpellLink.onNativeHit(caster,livingTarget);

			// 鎾斁鍑讳腑闊虫晥
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.2f);

			// 闇滀箣鎶ょ閫昏緫锛氬鏋滄槸鐜╁鍙戝皠鐨勶紝涓斾笉鏄垎瑁傜殑瀛愬脊锛屼笖瑁呭浜嗘姢绗?
			if (this.getOwner() instanceof LivingEntity owner && !this.isChild) {
				boolean hasAmulet = TrinketUtils.isWearing(owner, github.com.gengyoubo.sscfa.SscAddon.FROST_AMULET);

				if (hasAmulet) {
					spawnTrackingShards(owner, livingTarget);
				}
			}
		}
	}

	private void spawnTrackingShards(LivingEntity owner, LivingEntity hitTarget) {
		// 鐩村緞10鏍?= 鍗婂緞5鏍?
		double radius = 5.0;
		AABB box = hitTarget.getBoundingBox().inflate(radius);
		List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, box,
				e -> e != owner && e != hitTarget && e.isAlive() && !e.isSpectator()
						&& !(owner instanceof net.minecraft.server.level.ServerPlayer sp && WhitelistUtils.isProtected(sp, e)));

		if (nearby.isEmpty()) return;

		// 鎸夎窛绂绘帓搴?
		nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(hitTarget)));

		int count = 0;
		int maxShards = 2; // 鍙戝皠涓や釜

		for (LivingEntity target : nearby) {
			if (count >= maxShards) break;

			FrostBallEntity shard = new FrostBallEntity(this.level(), owner);
			// 浠庤鍛戒腑鑰呬綅缃彂灏?
			shard.setPos(hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ());
			shard.setTrackingTarget(target);
			shard.setIsChild(true); // 鏍囪涓哄瓙寮癸紝闃叉閫掑綊鐖嗙偢

			// 鍒濆鏈濆悜鐩爣
			Vec3 direction = target.getEyePosition().subtract(hitTarget.getEyePosition()).normalize();
			shard.setDirection(direction);

			this.level().addFreshEntity(shard);
			count++;
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != this.getOwner() && entity instanceof LivingEntity;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("StartX")) {
			this.startPos = new Vec3(
					nbt.getDouble("StartX"),
					nbt.getDouble("StartY"),
					nbt.getDouble("StartZ")
			);
		}
		if (nbt.contains("IsChild")) {
			this.setIsChild(nbt.getBoolean("IsChild"));
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
		nbt.putBoolean("IsChild", this.isChild);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	/**
	 * FlyingItemEntity鎺ュ彛瀹炵幇 - 杩斿洖闆悆浣滀负鏄剧ず鐗╁搧
	 */
	@Override
	public ItemStack getItem() {
		return new ItemStack(Items.SNOWBALL);
	}
}
