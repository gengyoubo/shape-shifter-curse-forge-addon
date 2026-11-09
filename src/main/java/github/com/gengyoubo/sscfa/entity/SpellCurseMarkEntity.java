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
 * 閺堝牆鐨规鏃€纭惰矾鐠囧懎鎷婚弽鍥唶閹舵洖鐨犻悧鈺嬬礄鐠囧懎鎷荤化浼欑礉jackcooper閿涘鈧倻绮ㄩ弸鍕瑢 {@link SpellFireBoltEntity} 閸氬矁瀵栧蹇ョ窗
 * 閺堟繂鍣弰鐔烘纯缁惧灝瀵戦柅鐔碱棧鐞涘矉绱欓弮鐘诲櫢閸旀冻绱氶敍灞芥嚒娑擃厾娲伴弽鍥ф倵閺傝棄濮為妴宀冪槒閸滄帗鐖ｇ拋鑸偓宥囧Ц閹緤绱欓崣妞炬縺 +20%閿涘本妞傞梹璺ㄦ暠閺傝姤纭剁€规熬绱氶崥搴㈢Х婢舵唻绱?
 * 鐡掑懓绐?鐡掑懏妞傞懛顏呯槈閵嗗倿绮拋銈囨閸氬秴宕熼敍姘瘜娴滃搫婀痪澶哥瑬閻╊喗鐖ｉ崣妞剧箽閹躲倕鍨稉宥嗘煢閸旂姰鈧?
 */
public class SpellCurseMarkEntity extends Projectile implements ItemSupplier {

	private static final double SPEED = 0.7;         // 14 閺?缁夋帪绱欓崪鎺戝祪缂傛挻鍙冮幇鐕傜礆
	private static final double MAX_DISTANCE = 30.0; // 閺堚偓婢堆囶棧鐞涘矁绐涚粋?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("spells.curse_mark_entity");

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘绱滵ataTracker 閸氬本顒為妴?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellCurseMarkEntity.class, EntityDataSerializers.INT);
	/** 閺嶅洩顔囬弮鍫曟毐閿涘澅ick閿涘绱滵ataTracker 閸氬本顒為敍鍫熻閺屾挾顏０鍕殌閿涘鈧?*/
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
		// 娑撳秷鐨?super閿涘湕ntity 閻ㄥ嫭濞婄挒鈩冩煙濞夋洘妫ゅ▔鏇℃硶娑撱倗楠囩拋鍧楁６閿涙矟ataTracker 閻?Entity 閺嬪嫰鈧姴娅掗崚婵嗩潗閸栨牭绱濋崥灞藉暫闁裤儱鍟撳▔鏇礆
		this.entityData.define(LEVEL, 1);
		this.entityData.define(DURATION, 160);
	}

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘鈧?*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 鐠佸墽鐤嗘鏃€纭剁粵澶岄獓閿涘牊婀囬崝锛勵伂閺傝姤纭堕弮鎯扮殶閻㈩煉绱滵ataTracker 閼奉亜濮╅崥灞绢劄鐎广垺鍩涚粩顖ょ礆閵?*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 閺嶅洩顔囬弮鍫曟毐閿涘澅ick閿涘鈧?*/
	public int getDuration() {
		return this.entityData.get(DURATION);
	}

	/** 鐠佸墽鐤嗛弽鍥唶閺冨爼鏆遍敍鍧眎ck閿涘鈧?*/
	public void setDuration(int durationTicks) {
		this.entityData.set(DURATION, Math.max(20, durationTicks));
	}

	/** 鐠佸墽鐤嗘鐐额攽閺傜懓鎮滈敍鍫熸篂閸戝棙妲﹂敍澶涚礉闁喎瀹抽幐澶岀搼缁狙冣偓宥囧芳缂傗晜鏂侀妴?*/
	public void setDirection(Vec3 direction, float speedMultiplier) {
		Vec3 velocity = direction.normalize().scale(BAL.d("speed", SPEED) * speedMultiplier);
		this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
		updateRotationFromVelocity(velocity);
	}

	/** 閹稿鈧喎瀹抽懛顏嗙暬閺堟繂鎮滈敍鍫濇倱閸愪即鏁熼崗顒€绱￠敍澶涚礉娓氭稒瑕嗛弻鎾愁嚠濮濓絻鈧?*/
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

		// 妞嬬偠顢戦幏鏍х啲缁帒鐡欓敍鍫熸箛閸旓紕顏幘鎺炵礉婢垛晝鍔ф径姘眽閸氬本顒為敍澶涚窗閺嗘浼犻崪鎺戝祪
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
			// 姒涙顓婚惂钘夋倳閸楁洩绱版稉璁虫眽閸︺劎鍤庢稉鏃傛窗閺嶅洤褰堟穱婵囧Б 閳?娑撳秵鏌﹂崝鐘崇垼鐠?
			if (this.getOwner() instanceof ServerPlayer ownerPlayer
					&& WhitelistUtils.isProtected(ownerPlayer, livingTarget)) {
				return;
			}
			// amplifier = 缁涘楠?1閿涘湢1=0 閳?L5=4閿涘绱癿ixin 閹稿鍙剧拋锛勭暬閸欐ぞ婵€閸旂姵绻?1.2+0.1鑴砤mp
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
			// 閸涙垝鑵戝鏂垮毉閿涙碍娈槐顐㈡嫽閸楁壆鍨庨弫?
			if (this.level() instanceof ServerLevel serverWorld) {
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.WITCH,
						this.getX(), this.getY(), this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
			}
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 閹烘帡娅庨惄鏃傛暢閺嬭绱伴崑鍥︽眽娑撳秴鎮嗙拠鍛嫽閺嶅洩顔囬敍宀勪缉閸忓秵姘拹鐟拌剨娴?
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
		// 閻劍浼撴鍌欑濞夘亝瑕嗛弻鎿勭礄閸滄帒宓冮幇蹇氳杽閿?
		return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GHAST_TEAR);
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}
}
