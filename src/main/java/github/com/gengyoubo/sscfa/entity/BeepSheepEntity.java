package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 閺堝牆鐨规鏃€纭惰矾閸溾晛鑴婇敍鍫濆将閸炪倗閮撮妴宀€绶ゆ禍鍡曢嚋缂?Beep Sheep閵嗗稄绱漥ackcooper閿?026-09-29 閻劍鍩涚€规氨顭堥敍澶堚偓?
 *
 * <p><b>妞嬬偠顢戞潪銊ㄦ姉娑撳酣娴摂瀣╃閼?/b>閿涙氨鎴烽幍?{@link ThrowableItemProjectile} 娣囨繄鏆€姒涙顓婚柌宥呭閿?.03 闂冭濮?+ 0.05 闁插秴濮忛敍澶涚礉
 * 閸涘牊鐖ｉ崙鍡樺閻椻晝鍤庨妴鍌氭嚒娑擃厽妞挎担鎾虫倵鐏忓棗鍙?b>閸欐ɑ鍨氱紘?/b>閿涘牊鏌﹂崝?{@link SscAddon#SHEEP_FORM} 閺佸牊鐏夐敍?
 * 鐎广垺鍩涚粩顖滄暠 SheepFormClientHooks 閻劌甯悧鍫㈢筏濡€崇€烽弴鎸庡床濞撳弶鐓嬮敍澶涚礉閺堢喖妫块惄顔界垼閺冪姵纭堕弨璇插毊 / 閺冪姵纭堕惍鏉戞綎 / 閺冪姵纭堕弨鍓х枂閺傜懓娼￠妴?/p>
 *
 * <p><b>閻╊喗鐖ｉ梽鎰煑閿?026-09-29 閻劍鍩涚€规氨顭堥弨纭咁攨闁插繘妫敍?/b>閿涙oss 缁紮绱欓崙瀣紥/閻╂垵鐣ч懓鍜冪礆娑?b>閺堚偓婢堆呮晸閸涜棄鈧?
 * 鐡掑懓绻?50 閻?/b>閻ㄥ嫮娲伴弽鍥у帳閻ゎ偓绱欓崢鐔粹偓宀€顫幘鐐殿唸鐎?1.2 閹存牠鐝?2.0閵嗗秳缍嬮崹瀣，瀹歌尙些闂勩倐鈧柡鈧攷od NPC 閺咁噣浜堕煬顐︾彯
 * 鐡?2.0 鐞氼偉顕ら弶鈧敍灞筋嚤閼锋番鈧PC 閻㈢喓澧块弮鐘崇《閸欐ɑ鍨氱紘濞库偓宥忕幢鐞涒偓闁插繘妫径鈺冨姧鐟曞棛娲婇弶鎴炵毌 20 鐞涒偓/婢舵碍鏆?NPC閿涘苯鎮撻弮鏈电矝
 * 閹糕€茬秶闁句礁鍊嬮崕?100 鐞涒偓/閺堫偄濂栨Λ?200 鐞涒偓/閸戝浼?300 鐞涒偓缁涘銇囬崸妤€銇旈敍澶堚偓?/p>
 *
 * <p>姒涙顓婚惂钘夋倳閸楁洩绱版稉璁虫眽閸︺劎鍤庢稉鏃傛窗閺嶅洤褰堟穱婵囧Б閿涘牏甯虹€硅泛寮烽崗璺虹枃閻?閸欘剙鏁滈悧鈺嬬礆閸掓瑤绗夐弬钘夊閵嗗倸鎳℃稉?Boss 閸忓秶鏌呮担鎾存
 * 瀵€涚秼閸欏秷顫﹂妴宀€绶ら崠鏍у冀瀵箍鈧秵绱ㄩ崙鐚寸礄閸溾晛褰?+ 缁帒鐡欓敍澶涚礉娑撳秵鏌﹂崝鐘虫櫏閺嬫嚎鈧?/p>
 */
public class BeepSheepEntity extends ThrowableItemProjectile {

	/** 鐞涒偓闁插繘妫梼鍫濃偓纭风礄2026-09-29 閻劍鍩涚€规氨顭堥敍澶涚窗閺堚偓婢堆呮晸閸涜棄鈧?> 50 閻愬湱娈戦惄顔界垼閸忓秶鏌呴崣妯肩筏閵?*/
	private static final float MAX_TARGET_HEALTH = 50.0F;

	/** 閸欐绶ら弮鍫曟毐閿涘澅ick閿涘绱滵ataTracker 閺冪娀娓堕垾鏂衡偓鏂剧矌閺堝秴濮熺粩顖滅波缁犳鏁ら敍瀛˙T 閹镐椒绠欓崠鏍モ偓?*/
	private int sheepDuration = 120;
	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涘鈧?*/
	private int spellLevel = 1;
	/** 娑旓箑鍞撮弬鑺ョ《缂傛牕褰块敍鍧皁lo 娑?null閿涙稒绁﹀ú鎯ф嚒娑擃叀绻戞潻妯兼暏閿涘鈧?*/
	private java.util.UUID refundCastId;

	public void setRefundCastId(java.util.UUID castId) {
		refundCastId = castId;
	}

	public BeepSheepEntity(EntityType<? extends BeepSheepEntity> entityType, Level world) {
		super(entityType, world);
	}

	public BeepSheepEntity(Level world, LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.BEEP_SHEEP.get(), owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		// 濞撳弶鐓嬮悧鈺佹惂閿涙氨娅ч懝鍙夌厠閺傛瑦娓堕幒銉ㄧ箮閵嗗瞼绶ら妴宥嗗壈鐠炩槄绱欑亸蹇曟閼瑰弶鏌熼崸妤€鑴婇柆鎿勭礉FlyingItemEntityRenderer 閻╁瓨甯撮悽顭掔礆
		return Items.WHITE_DYE;
	}

	@Override
	public void tick() {
		super.tick();
        if(this.isRemoved())return;
		// 妞嬬偠顢戦幏鏍х啲閿涘牊婀囬崝锛勵伂閹炬帪绱濇径鈺冨姧婢舵矮姹夐崥灞绢劄閿涘绱伴惂鍊熷缂囧﹥鐦虹徊?+ 鏉炶浜曢崪鈺傤洤閻滃洭鐓舵稉宥呭煕閿涘牆鑴婇柆鎾崇暔闂堟瑱绱濋崨鎴掕厬閹靛秴褰ㄩ敍?
		if (this.level() instanceof ServerLevel serverWorld && this.tickCount % 2 == 0) {
			serverWorld.sendParticles(ParticleTypes.WHITE_ASH,
					this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
		// 鐡掑懏妞傞懛顏呯槈閿? 缁夋帪绱辨ウ陇娉插蹇斿閻椻晝鍤庨拃钘夋勾娴兼俺袝閸?onCollision閿涘本顒濇稉鍝勫幑鎼存洩绱?
		if (this.tickCount > 160) {
			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		Entity target = entityHitResult.getEntity();
		if (this.level().isClientSide || !(target instanceof LivingEntity livingTarget)) {
			return;
		}
		// 姒涙顓婚惂钘夋倳閸楁洩绱版稉璁虫眽閸︺劎鍤庢稉鏃傛窗閺嶅洤褰堟穱婵囧Б 閳?娑撳秵鏌﹂崝鐙呯礄瀵€涚秼濞戝牊鏆庨敍?
		if (this.getOwner() instanceof ServerPlayer ownerPlayer
				&& WhitelistUtils.isProtected(ownerPlayer, livingTarget)) {
			return;
		}
		// Boss / 婢堆冪€烽悽鐔哄⒖閸忓秶鏌呴敍鍫㈡暏閹村嘲鐣剧粙鍖＄窗娑撳秴鍘戠拋绋垮綁 boss 缁崵鏁撻悧鈺嬬礉閸欘亣鍏橀崣妯虹毈閸ㄥ鏁撻悧鈺嬬礆
		if (isImmuneTarget(livingTarget)) {
			// 閸忓秶鏌呭鏂垮毉閿涙艾鎸稉鈧竟?+ 缁帒鐡欏鐟扮磻閿涘牆鑿囨妯哄冀妫ｅ牞绱扮紘濠囩摕濞夋洖顕径褍娼℃径瀛樻￥閺佸牞绱?
			this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.SHEEP_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f);
			if (this.level() instanceof ServerLevel serverWorld) {
				serverWorld.sendParticles(ParticleTypes.SNEEZE,
						target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
			}
			return;
		}
		// 閺傝棄濮為崣妯肩筏閿涙瓫mplifier = 缁涘楠?1閿涘湢1=0 閳?L5=4閿涘奔绶?lang/閹碘晛鐫嶉弰鍓с仛閿?
		boolean hadHarmfulEffect = github.com.gengyoubo.sscfa.spell.FormCastingStyle.hasHarmfulEffect(livingTarget);
		boolean applied = livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get(), sheepDuration, spellLevel - 1, false, true, true), this.getOwner());
        if (github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(
                livingTarget,github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get(),sheepDuration,spellLevel-1,applied))
            github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(this,livingTarget);
		if (applied) {
			// 閻ф槒顔囬妴灞藉綀閹﹪鈧啰顬囧┃鎰┾偓宥忕礄2026-09-29 閻劍鍩涚€规氨顭堥敍姘綁缂?NPC 閻ㄥ嫮绶?AI 姒涙顓婚崣妤佸剷鏉╂粎顬囬弬鑺ョ《閼板拑绱?
			github.com.gengyoubo.sscfa.effect.SheepFormAiController.setFleeFrom(
					livingTarget.getUUID(), this.getOwner() != null ? this.getOwner().getUUID() : null);
		}
		if (applied && this.getOwner() instanceof ServerPlayer caster) {
			github.com.gengyoubo.sscfa.spell.SpiderSpellLink.directHit(this,livingTarget);
			github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(caster, livingTarget,
					github.com.gengyoubo.sscfa.spell.FormationElement.SUMMON, refundCastId, hadHarmfulEffect);
		}
		// 閸欐闊╁鏂垮毉閿涙艾鎸敍? 缂囧﹥鐦洪悥鍡樻殠
		this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.SHEEP_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);
		if (this.level() instanceof ServerLevel serverWorld) {
			serverWorld.sendParticles(ParticleTypes.POOF,
					target.getX(), target.getY(0.6), target.getZ(), 12, 0.35, 0.5, 0.35, 0.03);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			// 閽€钘夋勾濠曟柨鍤敍姘毈閸ャ垻绶ゅВ娑樼毠閿涘牅绗夐崪鈺佸建閳ユ柡鈧柨褰ч崨鎴掕厬濞茶缍嬮幍宥呭建閿?
			if (this.level() instanceof ServerLevel serverWorld) {
				serverWorld.sendParticles(ParticleTypes.POOF,
						this.getX(), this.getY(), this.getZ(), 5, 0.15, 0.15, 0.15, 0.02);
			}
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// 閹烘帡娅庨惄鏃傛暢閺嬫湹绗岄弬鑺ョ《閼板懏婀版禍鐚寸礄閸氬矁鐦忛崪鎺撶垼鐠佹澘鑴婇敍?
		return super.canHitEntity(entity) && entity != this.getOwner()
				&& entity instanceof LivingEntity && !(entity instanceof ArmorStand);
	}

	/** Boss / 妤傛顢呴柌蹇曟窗閺嶅洤鍘ら悿顐㈠灲鐎规熬绱欓張宥呭缁旑垱娼堟繛渚婄礉2026-09-29 閻劍鍩涚€规氨顭堢悰鈧柌蹇涙，閿涘绱伴崙瀣紥閵嗕胶娲冪€瑰牐鈧懌鈧?
	 *  閺堚偓婢堆呮晸閸涜棄鈧?> 50 閻愬箍鈧倷缍嬮崹瀣，瀹歌尙些闂勩倧绱檓od NPC 闊偊鐝弲顕€浜?>2.0 鐞氼偉顕ら幐鈽呯礉閺夋垶鐨?娴ｅ氦顢?NPC 閻滄澘婀崣顖氬綁缂囧绱氶妴?*/
	public static boolean isImmuneTarget(LivingEntity target) {
		if (target instanceof WitherBoss
				|| target instanceof net.minecraft.world.entity.monster.warden.Warden) {
			return true;
		}
		return target.getMaxHealth() > MAX_TARGET_HEALTH;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		refundCastId = nbt.hasUUID("RefundCastId") ? nbt.getUUID("RefundCastId") : null;
		if (nbt.contains("SheepDuration")) sheepDuration = nbt.getInt("SheepDuration");
		if (nbt.contains("SpellLevel")) spellLevel = nbt.getInt("SpellLevel");
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		if (refundCastId != null) nbt.putUUID("RefundCastId", refundCastId);
		nbt.putInt("SheepDuration", sheepDuration);
		nbt.putInt("SpellLevel", spellLevel);
	}

	/** 鐠佸墽鐤嗛崣妯肩筏閺冨爼鏆遍敍鍧眎ck閿涘本婀囬崝锛勵伂閺傝姤纭堕弮鎯扮殶閻㈩煉绱氶妴?*/
	public void setSheepDuration(int durationTicks) {
		this.sheepDuration = Math.max(20, durationTicks);
	}

	/** 鐠佸墽鐤嗘鏃€纭剁粵澶岄獓閿?-5閿涘鈧?*/
	public void setSpellLevel(int level) {
		this.spellLevel = Math.max(1, Math.min(5, level));
	}
}
