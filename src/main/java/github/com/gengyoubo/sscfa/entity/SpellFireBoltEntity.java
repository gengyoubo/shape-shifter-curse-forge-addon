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
 * 閺堝牆鐨规鏃€纭惰矾閻忣偆鎮嗛幎鏇炵殸閻椻晪绱檍ackcooper閿涘鈧倻绮ㄩ弸鍕瑢 {@link SpellFrostSpikeEntity} 閸氬矁瀵栧蹇ョ窗
 * 閺堟繂鍣弰鐔烘纯缁惧灝瀵戦柅鐔碱棧鐞涘矉绱欓弮鐘诲櫢閸旀冻绱氶敍灞芥嚒娑擃參鈧姵鍨氭鏃€纭舵导銈咁唺楠?b>閻愬湱鍣?/b>閻╊喗鐖ｉ崥搴㈢Х婢舵唻绱辩搾鍛扮獩/鐡掑懏妞傞懛顏呯槈閵?
 * 姒涙顓婚惂钘夋倳閸楁洩绱版稉璁虫眽閸︺劎鍤庢稉鏃傛窗閺嶅洤褰堟穱婵囧Б閸掓瑤绗夋导銈咁唺閵嗕椒绗夐悙鍦櫑閵?
 */
public class SpellFireBoltEntity extends Projectile implements ItemSupplier {

	private static final double SPEED = 0.8;         // 16 閺?缁?
	private static final double MAX_DISTANCE = 50.0; // 閺堚偓婢堆囶棧鐞涘矁绐涚粋?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("spells.fire_bolt_entity");

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘绱滵ataTracker 閸氬本顒為敍鍫ヮ暕閻ｆ瑦瑕嗛弻鎾额伂閹广垺膩閸?缂傗晜鏂侀敍灞界秼閸撳秳绮庣€涙ɑ銆傞悽顭掔礆閵?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellFireBoltEntity.class, EntityDataSerializers.INT);

	private Vec3 startPos;
	private int ticksAlive = 0;
	private float damage = 5.0f;
	/** 閸涙垝鑵戦悙鍦櫑閺冨爼鏆遍敍鍧眎ck閿涘鈧?*/
	private int fireTicks = 60;

	public SpellFireBoltEntity(EntityType<? extends SpellFireBoltEntity> entityType, Level world) {
		super(entityType, world);
		this.startPos = this.position();
	}

	public SpellFireBoltEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.SPELL_FIRE_BOLT.get(), world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.startPos = this.position();
	}

	@Override
	protected void defineSynchedData() {
		// 娑撳秷鐨?super閿涘湕ntity 閻ㄥ嫭濞婄挒鈩冩煙濞夋洘妫ゅ▔鏇℃硶娑撱倗楠囩拋鍧楁６閿涙矟ataTracker 閻?Entity 閺嬪嫰鈧姴娅掗崚婵嗩潗閸栨牭绱濋崥灞藉暫闁裤儱鍟撳▔鏇礆
		this.entityData.define(LEVEL, 1);
	}

	/** 鐠佸墽鐤嗛悘顐ゆ倖閸涙垝鑵戞导銈咁唺閵?*/
	public void setDamage(float damage) {
		this.damage = damage;
	}
	/** 閸涙垝鑵戦崣鎴炴杹閻ㄥ嫮绮℃宀冪セ闁叉埊绱欒劤10 閺佸瓨鏆熼敍娌瀤p_mode 1/2 閹稿倽鎹ｉ柈銊ュ瀻閻㈣鲸鏌﹀▔鏇熸鐟佸懎鍙嗛敍瀛˙T 閹镐椒绠欓崠鏍硶 tick閿涘鈧?*/
	private int expBountyTen = 0;

	public void setExpBountyTen(int expTen) {
		this.expBountyTen = Math.max(0, expTen);
	}

	public int getExpBountyTen() {
		return expBountyTen;
	}

	/** 閺堫剚顐奸弬鑺ョ《鐎圭偤妾懓妤勬憫閿涘牆鎳℃稉顓＄箲鏉╂琚ù浣规烦閻㈩煉绱辨稉搴ｇ病妤犲矁绁婚柌鎴濇倱濡€崇础鐠?tick 鐎?NBT閿涘鈧?*/
	private java.util.UUID refundCastId;

	public void setRefundCastId(java.util.UUID castId) {
		this.refundCastId = castId;
	}
	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘鈧?*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 鐠佸墽鐤嗘鏃€纭剁粵澶岄獓閿涘牊婀囬崝锛勵伂閺傝姤纭堕弮鎯扮殶閻㈩煉绱滵ataTracker 閼奉亜濮╅崥灞绢劄鐎广垺鍩涚粩顖ょ礆閵?*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 鐠佸墽鐤嗛崨鎴掕厬閻愬湱鍣ч弮鍫曟毐閿涘澅ick閿涘鈧?*/
	public void setRemainingFireTicks(int fireTicks) {
		this.fireTicks = fireTicks;
	}

	/** 鐠佸墽鐤嗘鐐额攽閺傜懓鎮滈敍鍫熸篂閸戝棙妲﹂敍澶涚礉闁喎瀹抽幐澶岀搼缁狙冣偓宥囧芳缂傗晜鏂侀妴?*/
	public void setDirection(Vec3 direction, float speedMultiplier) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED) * speedMultiplier);
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
		updateRotationFromVelocity(velocity);
	}

	/** 閹稿鈧喎瀹抽懛顏嗙暬閺堟繂鎮滈敍鍫濇倱鐎垫帗顥濋悪鎰暫闁裤儱鍙曞蹇ョ礆閿涘奔绶靛〒鍙夌厠鐎佃顒滈妴?*/
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

		// 閺冪娀鍣搁崝娑樺瘧闁喓些閸?
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

		// 鐡掑懓绐?/ 鐡掑懏妞傞懛顏呯槈閿涘牅绮庨張宥呭缁旑垱娼堟繛渚婄礉閻炲棛鏁遍崥灞藉暫闁裤儻绱扮€广垺鍩涚粩顖氱杽娴?startPos 閹帒甯悙鐧哥礉閸欏瞼顏崚銈勭窗鐠囶垰鍨归敍?
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

		// 閺堝秴濮熺粩顖氬涧閸忣剙绔峰褰掍壕鐟欏棜顫庨悩鑸碘偓渚婄幢閸氬嫬顓归幋椋庮伂閹稿鎮撴稉鈧弮鍫曟？缁捐法鏁撻幋鎰斧鐎靛棗瀹抽幏鏍х啲閵?
		if (this.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.touch(
					this, getUUID(), github.com.gengyoubo.sscfa.network.visual.ParametricVisuals.Kind.FIRE_BOLT_TRAIL,
					null, position(), true, serverWorld.getGameTime() - tickCount, 0, 0);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.4f);
			if (this.level() instanceof ServerLevel serverWorld) {
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLAME,
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
			// 閸忣剙鍙￠崨鎴掕厬缂佹挾鐣婚敍鍫㈡閸氬秴宕熺挒浣稿帳 閳?濞夋洘婀虫导銈咁唺 閳?缂佸繘鐛欑悰銉ュ絺 閳?濞翠焦娣抽柦鈺佺摍閿涘绱扮憴?SpellHitHelper
			github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult hit =
					github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
							this, livingTarget, damage,
							github.com.gengyoubo.sscfa.spell.FormationElement.FIRE, refundCastId, expBountyTen);
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
				expBountyTen = 0; // 缂佸繘鐛欏鎻掑絺閺€鎾呯礉濞撳懘娴傞梼鏌ュ櫢婢?
			}
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.PROTECTED) {
				return; // 閻ц棄鎮曢崡鏇＄湲閸忓稄绱版稉宥囧仯閻曞啨鈧椒绗夐幘顓㈢叾
			}
			// 閻愬湱鍣ч惄顔界垼閿涘牅婵€鐎硅櫕鏆熼崐鐓庮樆閻ㄥ嫬娴愮€规岸妾崝鐘虫櫏閺嬫粣绱遍弮鍫曟毐閻㈣鲸纭堕張顖涘瘻缁涘楠囬崘娆忓弳閿?
			livingTarget.setRemainingFireTicks(fireTicks);
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 閹烘帡娅庨惄鏃傛暢閺嬭绱欐稉搴㈡箑閻忛潧鍘滃鐟版倱閸欙絽绶為敍澶涚窗閸嬪洣姹夋稉宥勯獓鏉╂棁绻?缂佸繘鐛欓敍宀勬Щ閸ｎ剚鈪电粵澶嬬ウ濞茬偓澧﹂崑鍥︽眽闊厼娲栭拑?
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
		if (nbt.contains("Damage")) {
			this.damage = nbt.getFloat("Damage");
		}
		if (nbt.contains("SpellLevel")) {
			setLevel(nbt.getInt("SpellLevel"));
		}
		if (nbt.contains("FireTicks")) {
			this.fireTicks = nbt.getInt("FireTicks");
		}
		if (nbt.contains("ExpBountyTen")) {
			this.expBountyTen = Math.max(0, nbt.getInt("ExpBountyTen"));
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
		nbt.putInt("FireTicks", this.fireTicks);
		nbt.putInt("ExpBountyTen", this.expBountyTen);
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(Items.FIRE_CHARGE);
	}
}
