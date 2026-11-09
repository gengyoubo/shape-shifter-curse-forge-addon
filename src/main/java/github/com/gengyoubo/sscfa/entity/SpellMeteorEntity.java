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
 * 閺堝牆鐨规鏃€纭惰矾闂勩劎浼€鐎圭偘缍嬮敍鍧朼ckcooper閿涘鈧倷琚遍梼鑸殿唽閿?
 * <ol>
	 *   <li><b>閽€鐣屽仯閺嶅洩顔囬梼鑸殿唽</b>閿涘牆澧?{@link #DELAY_TICKS} tick閿涘绱伴幃顒€浠犻崷銊ф窗閺嶅洩鎯ら悙閫涚瑐缁岀尨绱濋張宥呭缁旑垰鎮撳?
	 *       妫板嫯顒熼崣鍌涙殶閿涘苯顓归幋椋庮伂閹稿鎮撴稉鈧弮鍫曟？缁捐法鏁撻幋鎰紑閻?閻戠喎婀€缁帒鐡欓敍灞惧絹缁€楦挎儰閻愮瀵栭崶杈剧幢</li>
 *   <li><b>閽€鎴掔瑓闂冭埖顔?/b>閿涙矮绮犻拃鐣屽仯娑撳﹦鈹?20 閺嶅吋鈧儵鈧喎娼拃钘夊煂閽€鐣屽仯閿涘本鎸掗崙璇叉倵 AOE 缂佹挾鐣?
 *       閿涘牅婵€鐎?+ 閻愬湱鍣?3s + 閸戝鈧偓閿涘瞼娅ч崥宥呭礋娣囨繃濮㈤妴浣风瑝閸氼偅鏌﹀▔鏇♀偓鍜冪礆閿涘瞼浼€閸?閻栧棛鍋㈤棅铏櫏/缁帒鐡欓敍宀勬閸楄櫕绉锋径渚库偓?/li>
 * </ol>
 * 娴笺倕顔婃稉搴″磹瀵板嫮鏁遍弬鑺ョ《闁槒绶崘娆忓弳閵嗗倸褰查崨鎴掕厬閸掋倕鐣鹃敍姘垼鐠佷即妯佸▓鍏哥瑢娑撳鎯ゆ稉顓犳畱鐎圭偘缍嬮張顒冮煩娑撳秴褰茬悮顐ｆ暰閸戜紮绱檆anHit 閹?false閿涘鈧?
 */
public class SpellMeteorEntity extends Projectile implements ItemSupplier {

	/** 閽€鐣屽仯妫板嫯顒熷鎯扮箿閿涘澅ick閿涘鈧?0t = 0.5 缁夋帇鈧?*/
	private static final int DELAY_TICKS = 10;
	/** 娑撳鎯ょ挧宄邦潗妤傛ê瀹抽敍鍫ｆ儰閻愰€涚瑐缁岀尨绱氶妴?*/
	private static final double FALL_HEIGHT = 20.0;
	/** 娑撳鎯ら柅鐔峰閿涘牊鐗?tick閿涘鈧?*/
	private static final double FALL_SPEED = 2.5;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("spells.meteor_entity");

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘绱滵ataTracker 閸氬本顒為妴?*/
	private static final EntityDataAccessor<Integer> LEVEL =
			SynchedEntityData.defineId(SpellMeteorEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RESUME_TICKS =
			SynchedEntityData.defineId(SpellMeteorEntity.class, EntityDataSerializers.INT);

	private int ticksAlive = 0;
	private float damage = 8.0f;
	/** AOE 閸楀﹤绶為敍鍫熺壐閿涘鈧?*/
	private double radius = 3.0;
	private boolean falling = false;
	private boolean legacyFallingResume;
	private double[] warningVisualParams;
    private int runeFireTicks=60;
    public void setRemainingFireTicks(int ticks){runeFireTicks=Math.max(0,ticks);}

	/** 閸涙垝鑵戦崣鎴炴杹閻ㄥ嫮绮℃宀冪セ闁叉埊绱欒劤10 閺佸瓨鏆熼敍娌瀤p_mode 1/2 閹稿倽鎹ｉ柈銊ュ瀻閻㈣鲸鏌﹀▔鏇熸鐟佸懎鍙嗛敍瀛˙T 閹镐椒绠欓崠鏍硶 tick閿涘鈧?*/
	private int expBountyTen = 0;

	public void setExpBountyTen(int expTen) {
		this.expBountyTen = Math.max(0, expTen);
	}

	/** 閺堫剚顐奸弬鑺ョ《鐎圭偤妾懓妤勬憫閿涘牆鎳℃稉顓＄箲鏉╂琚ù浣规烦閻㈩煉绱辨稉搴ｇ病妤犲矁绁婚柌鎴濇倱濡€崇础鐠?tick 鐎?NBT閿涘鈧?*/
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
		// 娑撳秷鐨?super閿涘牆鎮撻崘浼存暉閸愭瑦纭堕敍姝瀉taTracker 閻?Entity 閺嬪嫰鈧姴娅掗崚婵嗩潗閸栨牭绱?
		this.entityData.define(LEVEL, 1);
		this.entityData.define(RESUME_TICKS, 0);
	}

	/** 鐠佸墽鐤嗛梽銊т紑 AOE 娴笺倕顔婇妴?*/
	public void setDamage(float damage) {
		this.damage = damage;
	}

	/** 鐠佸墽鐤?AOE 閸楀﹤绶為敍鍫熺壐閿涘鈧?*/
	public void setRadius(double radius) {
		this.radius = radius;
	}

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘鈧?*/
	public int getSpellLevel() {
		return this.entityData.get(LEVEL);
	}

	/** 鐠佸墽鐤嗘鏃€纭剁粵澶岄獓閿涘湒ataTracker 閼奉亜濮╅崥灞绢劄鐎广垺鍩涚粩顖ょ礆閵?*/
	public void setLevel(int level) {
		this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
	}

	/** 閸掓繂顫愰崠鏍儰閻愮櫢绱伴幃顒€浠犳担宥囩枂 = 閽€鐣屽仯娑撳﹦鈹?FALL_HEIGHT閿涘牊鏌﹀▔鏇熸鐠嬪啰鏁ら敍澶堚偓?*/
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
			// 闂冭埖顔?5閿涙艾鎮撻崙鑺ユ殶婢舵碍顐肩拠璇插絿鐏炩偓闁劌褰夐柌蹇撳
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
			// 瀵ゆ儼绻滈張鐔稿姬閿涙艾鍨忛崗銉ょ瑓閽€浠嬫▉濞堢绱欓崣宀€顏柈钘夊瀼閿涘苯顓归幋椋庮伂鐎圭偘缍嬫稊鐔活洣瀵偓婵绗呴拃钘夊З閻紮绱?
			if (ticksAlive >= BAL.i("delay_ticks", DELAY_TICKS)) {
				falling = true;
				if (!this.level().isClientSide) {
					this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
							SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
				}
			}
			return;
		}

		// 娑撳鎯ら梼鑸殿唽閿涙艾寮荤粩顖氭倱濮濄儰绗呴崸鐙呯礄鐎广垺鍩涚粩顖涘瘻闁喎瀹抽幓鎺戔偓鐓庡祮閸欘垳婀呴崚鎵紑閻炲啰鐗虫稉瀣剁礆
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

		// 閺堝秴濮熺粩顖ょ窗娑撳鎯ら梼鑸殿唽濮?tick 闂?FALL_SPEED閿涘瞼鐤拋?ceil(FALL_HEIGHT / FALL_SPEED) tick 閸氬骸鍩屾潏鎹愭儰閻愮櫢绱濆鏇犲瀻
		if (!this.level().isClientSide && this.level() instanceof ServerLevel impactWorld) {
			// 闂冭埖顔?5閿涙艾鎮撻崙鑺ユ殶婢舵碍顐肩拠璇插絿鐏炩偓闁劌褰夐柌蹇撳閿涘牅绗岄弽鍥唶闂冭埖顔岄妴浣风瑓閽€浠嬧偓鐔峰娣囨繃瀵旈崥灞肩 tick 韫囶偆鍙庢稉鈧懛瀛樷偓褝绱?
			double fallHeight2 = BAL.d("fall_height", FALL_HEIGHT);
			double fallSpeed2 = BAL.d("fall_speed", FALL_SPEED)*speedFactor;
			int delayTicks2 = BAL.i("delay_ticks", DELAY_TICKS);
			if (!guided&&ticksAlive >= delayTicks2 + Math.ceil(fallHeight2 / fallSpeed2)) {
                // 20/2.5缁涘鏆ｉ梽銈堟儰閻愯浼嗘總鍊熸儰閸︺劍鏌熼崸妞剧瑐鐞涖劑娼伴敍灞界殸缁捐法绮撻悙鐟板讲閼虫垝绗夋潻娑樺弳閺傜懓娼￠敍娑樺涧閹恒垺绁撮懘姘瑓閽栧嫬鐪伴妴?
                var floor=weaving?impactWorld.clip(new net.minecraft.world.level.ClipContext(
                        this.position().add(0,.01,0),this.position().add(0,-.05,0),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this)):null;
                explode(impactWorld,null,floor!=null&&floor.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?floor:null);
			}
            if(guided&&ticksAlive>160)this.discard();
		}
	}

	/** 瀵洜鍨庨敍娆癘E 娴笺倕顔?+ 閻愬湱鍣?3s + 閸戝鈧偓閿涘瞼娅ч崥宥呭礋娣囨繃濮㈤妴浣风瑝閸氼偅鏌﹀▔鏇♀偓鍜冪幢閻忣偄鍘滈悥鍡欏仮濠曟柨鍤崥搴ゅ殰濮ｄ降鈧?*/
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
		LivingEntity lastHitTarget = null;   // 閺堚偓閸氬簼绔存稉顏勭杽闂勫懎褰堟导銈囨窗閺嶅浄绱欓悤搴″斧閻曞啰鍎虫潻鏃囩箷閸掋倕鐣鹃悽顭掔礆
		LivingEntity killedTarget = null;    // 閺堫剚顐奸悥鍡欏仮閸戠粯娼冮惃鍕窗閺嶅浄绱欐导妯哄帥娴肩姷绮伴崨鎴掕厬闁解晛鐡欓敍灞藉毊閺夆偓鏉?50% 閸掋倕鐣鹃悽顭掔礆
		boolean hitBurningTarget = false;
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(radius), e -> e != this.getOwner() && e.isAlive());
		for (LivingEntity target : targets) {
			if (target.distanceTo(this) > radius) {
				continue;
			}
			// 鐠烘繄顬囩悰鏉垮櫤閿涙矮鑵戣箛鍐╁姬娴?閳?鏉堝湱绱?40%
			double dist = target.distanceTo(this);
			float dmg = damage * (float) (1.0 - 0.6 * (dist / radius));
			if (dmg <= 0) {
				continue;
			}
			// 閸忣剙鍙￠崨鎴掕厬缂佹挾鐣婚敍鍫㈡閸氬秴宕熺挒浣稿帳 閳?濞夋洘婀虫导銈咁唺 閳?缂佸繘鐛欑悰銉ュ絺閿涙稒绁﹀ú楣冩尙鐎涙劒璐熼懕姘値鐠囶厺绠熼敍灞芥儕閻滎垰顦荤拫鍐跨礆閿涙俺顫?SpellHitHelper
			var hit = github.com.gengyoubo.sscfa.spell.SpellHitHelper.hitRaw(
					this, target, dmg, hitAnyTarget ? 0 : expBountyTen);
			if (hit != github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				continue;
			}
			hitBurningTarget |= target.getRemainingFireTicks() > 0;
            if(target==direct)github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,target);
			hitAnyTarget = true;
			lastHitTarget = target;
			// 閸戠粯娼冮崚銈呯暰韫囶偆鍙庨敍鍧塧mage 閸氬海鐝涢崡铏叀閳ユ柡鈧柧鎱ㄦ径宥忕窗閸樼喎鍘涙导鐘电舶闁解晛鐡欓惃鍕窗閺嶅洤鍘?filter(isAlive) 鐎佃壈鍤ч崙缁樻絻閸掑棙鏁闀愮瑝閸欘垵鎻敍?
			if (!target.isAlive() || target.getHealth() <= 0f) {
				killedTarget = target;
			}
			// 閻愬湱鍣?3s + 鏉炶浜曢崙濠氣偓鈧敍鍫㈩瀲瀵偓閻栧棗绺鹃弬鐟版倻閿?
			target.setRemainingFireTicks(runeFireTicks);
			Vec3 knock = new Vec3(target.getX() - ix, 0.1, target.getZ() - iz).normalize().scale(0.6);
			target.push(knock.x, knock.y, knock.z);
			target.hurtMarked = true;
		}
		// 濞翠焦娣抽崨鎴掕厬闁解晛鐡欓敍?026-09-17閿涘绱伴悤搴″斧閹稿婀板▎陇鈧鎽戞潻鏃囩箷閿涘牆鎯堥崙缁樻絻閸掋倕鐣鹃敍澶涚幢閸ュ搫鐣鹃崐鑲╄娑撳秳绶风挧鏍偓妤勬憫閵?
		// 闁解晛鐡欓惄顔界垼娴兼ê鍘涢崣鏍潶閸戠粯娼冮懓鍜冪礄閸戠粯娼冩潻?50% 閸掑棙鏁敍澶涚礉閺冪姴鍤弶鈧崣鏍ㄦ付閸氬骸褰堟导銈囨窗閺嶅浄绱欓悤鍐劤 20% 閸掑棙鏁敍?
		if (hitAnyTarget && this.getOwner() instanceof ServerPlayer styleOwner) {
			LivingEntity hookTarget = killedTarget != null ? killedTarget : lastHitTarget;
			github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(
					styleOwner, hookTarget,
					github.com.gengyoubo.sscfa.spell.FormationElement.FIRE, refundCastId, hitBurningTarget);
		}
		// 濠曟柨鍤敍姘卞瀻閻愬摜鐭戠€?+ 閻忣偄鍘?+ 閸欏苯鐪伴棅铏櫏
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
		return false; // 缁绢垰灏崺鐔肩摕濞夋洝娴囨担鎿勭窗娑撳秴褰茬悮顐ｆ暰閸戣鎳℃稉?
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
		// 濞撳弶鐓嬮悽顭掔窗鐏忓繗顫夊Ο锛勪紑閻掓澘鑴婇悧鈺佹惂閿涘湗lyingItemEntityRenderer 缁墽浼掗敍灞筋吂閹撮顏稉宥呯箑閻儵浜鹃惇鐔风杽娴笺倕顔婇敍?
		return new ItemStack(Items.FIRE_CHARGE);
		// 闂勩劎浼€閺堫兛缍嬫稉鏄忣洣闂堢姷鐭戠€涙劖瀚嬬亸?+ 閻忣偆鍔欏鍦翱閻忕數绮嶉崥鍫ｃ€冮悳?
	}
}
