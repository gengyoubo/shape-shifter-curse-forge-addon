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
 * 閺堝牆鐨规鏃€纭惰矾閺堝牆鍘滅粻顓熷鐏忓嫮澧块敍鍫熸箑鏉堝閮撮敍瀹゛ckcooper閿涘鈧倻绮ㄩ弸鍕瑢 {@link SpellFireBoltEntity} 閸氬矁瀵栧蹇ョ窗
 * 閺堟繂鍣弰鐔烘纯缁惧灝瀵戦柅鐔碱棧鐞涘矉绱欓弮鐘诲櫢閸旀冻绱氶敍灞芥嚒娑擃參鈧姵鍨氭鏃€纭舵导銈咁唺閿?b>鐎甸€涢閻忕數鏁撻悧鈺咁杺婢?+50% 娴笺倕顔?/b>閸氬孩绉锋径鎲嬬幢
 * 鐡掑懓绐?鐡掑懏妞傞懛顏呯槈閵嗗倿绮拋銈囨閸氬秴宕熼敍姘瘜娴滃搫婀痪澶哥瑬閻╊喗鐖ｉ崣妞剧箽閹躲倕鍨稉宥勬縺鐎圭偨鈧?
 */
public class SpellMoonlightArrowEntity extends Projectile {

	private static final double SPEED = 1.0;         // 20 閺?缁夋帪绱欏В鏃備紑閻炲啫鎻╅敍宀€顔勯惌銏″妳閿?
	private static final double MAX_DISTANCE = 50.0; // 閺堚偓婢堆囶棧鐞涘矁绐涚粋?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("spells.moonlight_arrow_entity");

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘绱滵ataTracker 閸氬本顒為敍鍫ヮ暕閻ｆ瑦瑕嗛弻鎾额伂閹广垺膩閸?缂傗晜鏂侀敍灞界秼閸撳秳绮庣€涙ɑ銆傞悽顭掔礆閵?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellMoonlightArrowEntity.class, EntityDataSerializers.INT);

	private Vec3 startPos;
	private int ticksAlive = 0;
	private float damage = 4.0f;

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
		// 娑撳秷鐨?super閿涘湕ntity 閻ㄥ嫭濞婄挒鈩冩煙濞夋洘妫ゅ▔鏇℃硶娑撱倗楠囩拋鍧楁６閿涙矟ataTracker 閻?Entity 閺嬪嫰鈧姴娅掗崚婵嗩潗閸栨牭绱濋崥灞藉暫闁裤儱鍟撳▔鏇礆
		this.entityData.define(LEVEL, 1);
	}

	/** 鐠佸墽鐤嗛崨鎴掕厬娴笺倕顔婇妴?*/
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘鈧?*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 鐠佸墽鐤嗘鏃€纭剁粵澶岄獓閿涘牊婀囬崝锛勵伂閺傝姤纭堕弮鎯扮殶閻㈩煉绱滵ataTracker 閼奉亜濮╅崥灞绢劄鐎广垺鍩涚粩顖ょ礆閵?*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 鐠佸墽鐤嗘鐐额攽閺傜懓鎮滈敍鍫熸篂閸戝棙妲﹂敍澶涚礉闁喎瀹抽幐澶岀搼缁狙冣偓宥囧芳缂傗晜鏂侀妴?*/
	public void setDirection(Vec3 direction, float speedMultiplier) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED) * speedMultiplier);
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
		updateRotationFromVelocity(velocity);
	}

	/** 閹稿鈧喎瀹抽懛顏嗙暬閺堟繂鎮滈敍鍫滅瑢鐎垫帗顥濋悪鎰暫闁裤儱鎮撳▎鎯у彆瀵骏绱扮亸鏍ㄦ篂闁喎瀹抽弬鐟版倻閿涘绱濇笟娑欒閺屾挸顕锝冣偓?*/
	private void updateRotationFromVelocity(Vec3 v) {
		double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
		this.setYRot((float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Mth.atan2(-v.y, horiz) * (180.0 / Math.PI))); // 閸欐牞绀嬮敍姘瑢鐎垫帗顥濋悪?閸愪即鏁熼幆顖欑伐娑撯偓閼疯揪绱濋柊宥呮値濞撳弶鐓嬮崳?-pitch 閺冨娴?
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

		// 妞嬬偠顢戦幏鏍х啲缁帒鐡欓敍鍫熸箛閸旓紕顏幘鎺炵礉婢垛晝鍔ф径姘眽閸氬本顒為敍澶涚窗閺堝牆鍘滅亸鎹愭姉
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
			// 鐎甸€涢閻忕數鏁撻悧鈺嬬礄閸嶉潧妗?妤犵兘鐝?楠炵晫浼掔粵澶涚礆妫版繂顦绘晶鐐版縺閿涙碍鐦＄痪?+10%閿涘湢1=+10% 閳?L5=+50%閿?
			float finalDamage = livingTarget.isInvertedHealAndHarm() ? damage * (1.0f + 0.1f * getSpellLevel()) : damage;
			// 閸忣剙鍙￠崨鎴掕厬缂佹挾鐣婚敍鍫㈡閸氬秴宕熺挒浣稿帳 閳?濞夋洘婀虫导銈咁唺 閳?缂佸繘鐛欑悰銉ュ絺 閳?濞翠焦娣抽柦鈺佺摍閿涘绱扮憴?SpellHitHelper
			github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult hit =
					github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
							this, livingTarget, finalDamage,
							github.com.gengyoubo.sscfa.spell.FormationElement.LUNAR, refundCastId, expBountyTen);
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
				expBountyTen = 0; // 缂佸繘鐛欏鎻掑絺閺€鎾呯礉濞撳懘娴傞梼鏌ュ櫢婢?
			}
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.PROTECTED) {
				return; // 閻ц棄鎮曢崡鏇＄湲閸忓稄绱版稉宥嗘尡闂?
			}
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 閸涙垝鑵戝鏂垮毉閿涙碍婀€鏉堝鍨庨梻顏呯Х閺侊綇绱欑€圭偘缍嬮崨鎴掕厬閻ㄥ嫪婵€鐎瑰啿婀?onEntityHit閿涘矁绻栭柌宀€绮烘稉鈧划鎺戠摍 + 閺€璺虹啲閿?
			if (this.level() instanceof ServerLevel serverWorld) {
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.END_ROD,
						this.getX(), this.getY(), this.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
			}
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 閹烘帡娅庨惄鏃傛暢閺嬭绱欐稉搴㈡箑閻忛潧鍘滃鐟版倱閸欙絽绶為敍澶涚窗閸嬪洣姹夋稉宥勯獓鏉╂棁绻?缂佸繘鐛?
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
