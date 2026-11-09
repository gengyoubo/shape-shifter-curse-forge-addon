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
 * 閺堝牆鐨规鏃€纭惰矾閸愪即鏁熼幎鏇炵殸閻椻晪绱檍ackcooper閿涘鈧?
 *
 * <p>閺堟繂鍣弰鐔烘纯缁惧灝瀵戦柅鐔碱棧鐞涘矉绱欓弮鐘诲櫢閸旀冻绱氶敍灞芥嚒娑擃厾鏁撻悧鈺呪偓鐘冲灇姒勬梹纭舵导銈咁唺閸氬孩绉锋径鎲嬬幢鐡掑懓绐?鐡掑懏妞傞懛顏呯槈閵?
 * 娑撳酣娲╅悪?SP 閻?{@link FrostBallEntity} 閺冪姴鍙ч敍鍫滅瑝閺傝棄濮為棁婊堟閵嗕焦妫ら幎銈囶儊閸掑棜顥囬妴浣规￥鏉╁€熼嚋閿涘绱?
 * 娓氭盯鐡熷▔鏇氬姛閵嗗苯鍟洪柨銉ｂ偓宥夌摕濞夋洜瀚粩瀣╁▏閻㈩煉绱濇穱婵囧瘮娴ｅ氦鈧箑鎮庨妴鍌炵帛鐠併倗娅ч崥宥呭礋閿涙矮瀵屾禍鍝勬躬缁惧じ绗栭惄顔界垼閸欐ぞ绻氶幎銈呭灟娑撳秳婵€鐎圭偨鈧?/p>
 */
public class SpellFrostSpikeEntity extends Projectile implements ItemSupplier {

	private static final double SPEED = 0.75;        // 15 閺?缁?
	private static final double MAX_DISTANCE = 50.0; // 閺堚偓婢堆囶棧鐞涘矁绐涚粋?

	public static double maxDistance() { return BAL.d("max_distance", MAX_DISTANCE); }

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("spells.frost_spike_entity");

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘绱滵ataTracker 閸氬本顒炴笟娑欒閺屾挾顏崚鍥ㄥ床 L4+ 3D 閸愪即鏁熷Ο鈥崇€烽妴?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellFrostSpikeEntity.class, EntityDataSerializers.INT);

	private Vec3 startPos;
	private int ticksAlive = 0;
	private float damage = 6.0f;

	/** 閸涙垝鑵戦崣鎴炴杹閻ㄥ嫮绮℃宀冪セ闁叉埊绱欒劤10 閺佸瓨鏆熼敍娌瀤p_mode 1/2 閹稿倽鎹ｉ柈銊ュ瀻閻㈣鲸鏌﹀▔鏇熸鐟佸懎鍙嗛敍瀛˙T 閹镐椒绠欓崠鏍硶 tick閿涙盯缍堢亸鍕綆閸掑棗鎮楀В蹇旂亣閹镐焦婀佹禒浠嬵杺閿涘鈧?*/
	private int expBountyTen = 0;

	/** 閸撯晙缍戦崣顖氭嚒娑擃厽鏅禍鐑樻殶閿涘牆鎯堣ぐ鎾冲閿? = 閸涙垝鑵戦崡宕囶暢閿涘苯宕熼崣鎴濆暫闁裤儵绮拋銈忕幢姒绘劕鐨犻幐澶岀搼缁狙勬暈閸?2+缁涘楠?1閿涘鈧?*/
	private int pierceRemaining = 0;
	/** 瀹告彃鎳℃稉顓＄箖閻ㄥ嫬鐤勬担?UUID閿涘牏鈹涢崚鐑樻埂闂冩彃鎮撴稉鈧惄顔界垼鐞氼偄顦?tick 闁插秴顦查崚銈呯暰閿涘鈧?*/
	private final java.util.List<java.util.UUID> piercedTargets = new java.util.ArrayList<>();
	/** 閺?tick 閸涙垝鑵戞稉鎭掆偓宀€鈹涢崚铏光敍鏉╁洢鈧秶娈戞穱鈥冲娇閺冩绱檕nEntityHit 缂冾喕缍?閳?onCollision 鐠哄疇绻冮柨鈧В渚婄礆閵?*/
	private boolean passThroughHit = false;

	/** 鐠佸墽鐤嗛崡鏇熺亣閺堚偓婢舵艾褰查崨鎴掕厬閺佸奔姹夐弫甯礄缁屽灝鍩￠弫甯幢閳? 缁涘鎮撴稉宥団敍閸掔尨绱濋崨鎴掕厬閸楀磭顣抽敍澶堚偓?*/
	public void setPierceCount(int count) {
		this.pierceRemaining = Math.max(0, count);
	}

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
		// 娑撳秷鐨?super閿涘湕ntity 閻ㄥ嫭濞婄挒鈩冩煙濞夋洘妫ゅ▔鏇℃硶娑撱倗楠囩拋鍧楁６閿涙矟ataTracker 閻?Entity 閺嬪嫰鈧姴娅掗崚婵嗩潗閸栨牭绱濋崥?ThrownWaterSpearEntity 閸愭瑦纭堕敍?
		this.entityData.define(LEVEL, 1);
	}

	/** 鐠佸墽鐤嗛崘浼存暉閸涙垝鑵戞导銈咁唺閵?*/
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涙北4+ 濞撳弶鐓嬬粩顖涘床 3D 閸愪即鏁熷Ο鈥崇€烽敍澶堚偓?*/
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

	/** 閹稿鈧喎瀹抽懛顏嗙暬閺堟繂鎮滈敍鍫滅瑢鐎垫帗顥濋悪鎰暫闁裤儱鎮撳▎鎯у彆瀵骏绱扮亸鏍ㄦ篂闁喎瀹抽弬鐟版倻閿涘绱濇笟?3D 濡€崇€峰〒鍙夌厠鐎佃顒滈妴?*/
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

		// 鐡掑懓绐?/ 鐡掑懏妞傞懛顏呯槈閿涘牅绮庨張宥呭缁旑垱娼堟繛渚婄礆閿涙艾顓归幋椋庮伂鐎圭偘缍嬬紒蹇曠秹缂佹粌瀵橀弸鍕偓鐙呯礉startPos 閹帊璐?(0,0,0)閿?
		// 閼汇儱寮荤粩顖涘⒔鐞涘奔绱伴崷銊ь瀲閸樼喓鍋?50 閺嶇厧顦荤粭顑跨 tick 鐏忚精顕ら崚鐘差吂閹撮顏€圭偘缍?閳?濡€崇€峰闀愮瑝閺勫墽銇氶敍鍫濆涧閸撯晜婀囬崝锛勵伂缁帒鐡欓敍澶堚偓?
		// 鐎广垺鍩涚粩顖氱杽娴ｆ挾娈戝☉鍫濄亼閻㈣鲸婀囬崝锛勵伂 discard 閸氬海娈?tracker 缁夊娅庨崠鍛粹攳閸旑煉绱濇径鈺冨姧婢舵矮姹夐崥灞绢劄閵?
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

		// 妞嬬偠顢戦幏鏍х啲缁帒鐡欓敍鍫熸箛閸旓紕顏幘鎺炵礉婢垛晝鍔ф径姘眽閸氬本顒為敍?
		if (this.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
					this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		this.passThroughHit = false; // 濮ｅ繑顐肩喊鐗堟寬閸忓牓鍣哥純顔锯敍閸掔儤妫楅弽?
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 缁屽灝鍩＄粚鑳箖閿涙俺浜ら柌蹇涙穿閼哄崬寮芥＃鍫濇倵缂佈呯敾妞嬬偠顢戦敍灞肩瑝绾板氦顥?
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
			// 閸忣剙鍙￠崨鎴掕厬缂佹挾鐣婚敍鍫㈡閸氬秴宕熺挒浣稿帳 閳?濞夋洘婀虫导銈咁唺 閳?缂佸繘鐛欑悰銉ュ絺 閳?濞翠焦娣抽柦鈺佺摍閿涘绱扮憴?SpellHitHelper
			github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult hit =
					github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
							this, livingTarget, damage,
							github.com.gengyoubo.sscfa.spell.FormationElement.ICE, refundCastId, expBountyTen);
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
				expBountyTen = 0; // 缂佸繘鐛欏鎻掑絺閺€鎾呯礉濞撳懘娴傞梼鏌ュ櫢婢?
			}
			if (hit == github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.PROTECTED) {
				return; // 閻ц棄鎮曢崡鏇＄湲閸忓稄绱版稉宥囩波缁犳鈹涢崚?
			}
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.2f);
			// 缁屽灝鍩＄紒鎾剁暬閿涘牓缍堢亸鍕降濠?pierceRemaining>0閿涘绱扮拋鏉垮弳瀹告彃鎳℃稉顓°€冮獮鍫曗偓鎺戝櫤閿?
			// 闁帒鍣洪崥搴濈矝 >0 閳?缂冾喗妫楃粚鑳箖缂佈呯敾妞嬬儑绱濋崥锕€鍨張顒侇偧娑撶儤娓堕崥搴濈濞嗏€虫嚒娑擃厹鈧胶鍙庣敮鍝ヮ暢鐟佸倶鈧?
			// 姒涙顓婚崡鏇炲絺閸愪即鏁?pierceRemaining=0 閳?閸戝繑鍨?-1 娑?>0 閳?鐞涘奔璐熸稉宥呭綁閿涘牆鎳℃稉顓炲祮绾板函绱氶妴?
			this.pierceRemaining--;
			this.piercedTargets.add(livingTarget.getUUID());
			if (this.pierceRemaining > 0) {
				this.passThroughHit = true;
			}
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 閹烘帡娅庨惄鏃傛暢閺嬭绱欐稉搴㈡箑閻忛潧鍘滃鐟版倱閸欙絽绶為敍澶涚窗閸嬪洣姹夋稉宥勯獓鏉╂棁绻?缂佸繘鐛?
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
