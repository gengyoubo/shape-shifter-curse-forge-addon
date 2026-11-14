package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.compat.ManaComponent;

/**
 * 鏈堢粐铔涖€岃洓涓濊崱婕俱€嶆鎶€鑳?- 铔涗笣椋炲脊锛堟姏鐗╃嚎鎶曞皠鐗╋紝澶嶇敤涓绘妧鑳借洓涓濆脊澶栬锛夈€?
 *
 * <p>鎸夋閿粠鐜╁鐪煎墠鍙戝皠锛屽甫杞诲井涓嬪潬鎶涚墿绾块鍚戝噯鏄熸柟鍚戙€傞琛屼腑鎸夊凡椋為暱搴︽墸 mana锛堟瘡 2 鏍?1 鐐癸級銆?
 * <ul>
 *   <li><b>鍛戒腑鏂瑰潡</b> 鈫?閽╀綇杩涘叆鎽嗚崱銆?/li>
 *   <li><b>鍛戒腑鐢熺墿</b> 鈫?杩炴帴杩涘叆 tether 鎷栨嫿銆?/li>
 *   <li><b>椋炲埌鏈€澶х怀闀匡紙32 鏍硷級浠嶆湭鍛戒腑 / mana 鑰楀敖</b> 鈫?鐞冧笌涓濈嚎<b>鍚屾椂绔嬪嵆娑堝け</b>锛坢iss锛? 绉?CD锛夈€?/li>
 * </ul>
 * 瀹㈡埛绔覆鏌撱€岀帺瀹垛啋椋炲脊銆嶇殑铔涗笣锛氬脊瀛樺湪鍗崇敾锛屽脊娑堝け锛堝懡涓?miss锛変笣绾块殢涔嬫秷澶便€?
 */
public class SpiderSwingBullet extends WebBulletBase {

	private double traveled = 0.0;
	private double lastManaCharge = 0.0;
	private int life = 0;

	/** 瀹㈡埛绔渶杩戜竴娆＄湅鍒拌洓涓濆脊瀛樻椿鐨?world tick锛堝疄浣?tick 蹇呰窇锛屽ぉ鐒跺彲闈狅紱娓叉煋鍣ㄦ嵁姝ら棬鎺э紝閬垮厤姣忓抚鍏ㄥ疄浣撴壂鎻忥級銆?*/
	private static volatile long sscAddon$lastClientBulletSeenTick = -1L;

	public SpiderSwingBullet(EntityType<? extends SpiderSwingBullet> type, Level world) {
		super(type, world);
	}

	public SpiderSwingBullet(LivingEntity owner) {
		super(github.com.gengyoubo.sscfa.init.AddonEntities.SPIDER_SWING_BULLET.get(), owner.level());
		this.owner = owner;
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
	}

	/** 娓叉煋鍣ㄩ棬鎺э細鏈€杩?3 涓?world tick 鍐呮槸鍚︽湁铔涗笣寮瑰瓨娲伙紙寮规秷澶卞悗鏍囪鏈€澶氫繚鐣?3t 鍗宠繃鏈燂級銆?*/
	public static boolean isBulletRecentlySeen(long currentWorldTick) {
		return sscAddon$lastClientBulletSeenTick >= 0L && currentWorldTick - sscAddon$lastClientBulletSeenTick <= 3L;
	}

	/** 瀹㈡埛绔柇绾挎椂閲嶇疆瀛樻椿鏍囪锛堟柇绾挎椂瀹炰綋涓嶉€愪釜璧?remove锛夈€?*/
	public static void resetClientState() {
		sscAddon$lastClientBulletSeenTick = -1L;
	}

	@Override
	protected float getGravity() {
		return 0.025f; // 杞诲井涓嬪潬鎶涚墿绾?
	}

	@Override
	public void tick() {
		// 瀹㈡埛绔瓨娲绘爣璁帮細鍙鏈脊杩樺湪 tick 灏卞埛鏂帮紙娓叉煋鍣ㄦ嵁姝ゅ垽鏂槸鍚﹂渶瑕佹壂瀹炰綋鐢讳笣锛?
		if (this.level().isClientSide) {
			sscAddon$lastClientBulletSeenTick = this.level().getGameTime();
		}
		if (this.level() instanceof ServerLevel sw && this.owner instanceof Player player) {
			traveled += this.getDeltaMovement().length();
			ManaComponent m = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
			while (traveled - lastManaCharge >= 2.0) {
				if (m.getMana() < 1.0) {
					fadeOut(sw); // mana 鑰楀敖 鈫?鐞冧笌涓濈嚎绔嬪嵆娑堝け
					this.discard();
					return;
				}
				m.consumeMana(1.0);
				lastManaCharge += 2.0;
			}
			if (traveled > SpiderMoonWeaverSwingManager.MAX_ROPE_REACH) {
				fadeOut(sw); // 椋炲埌鏈€澶х怀闀夸粛鏈懡涓?鈫?鐞冧笌涓濈嚎鍚屾椂绔嬪嵆娑堝け
				this.discard();
				return;
			}
		}
		life++;
		super.tick(); // WebBullet.tick锛氬彂灏勯煶鏁?+ 杞ㄨ抗绮掑瓙 + 绉诲姩 + 纰版挒妫€娴?
		if (life > 140 && !this.isRemoved()) {
			if (this.level() instanceof ServerLevel sw) fadeOut(sw);
			this.discard();
		}
	}

	@Override
	public void onHitBlock(BlockHitResult hit) {
		if (this.level() instanceof ServerLevel sw && this.owner instanceof ServerPlayer sp) {
			Vec3 anchor = hit.getLocation();
			SpiderMoonWeaverSwingManager.onBulletHitBlock(sp, anchor, this);
			sw.playSound(null, anchor.x, anchor.y, anchor.z,
					SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.9f, 1.4f);
		}
		this.discard();
	}

	@Override
	public void onHitEntity(EntityHitResult hit) {
		Entity e = hit.getEntity();
		if (this.level() instanceof ServerLevel sw && this.owner instanceof ServerPlayer sp
				&& e instanceof LivingEntity living && living != sp) {
			SpiderMoonWeaverSwingManager.onBulletHitEntity(sp, living, this);
			sw.playSound(null, living.getX(), living.getY(), living.getZ(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.9f, 1.6f); // 鍙紒
			this.discard();
		}
		// 鍛戒腑鏃犳晥鐩爣锛堝鑷繁锛夛細涓?discard锛岀户缁
	}

	private void fadeOut(ServerLevel sw) {
		sw.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(),
				12, 0.2, 0.2, 0.2, 0.02);
		sw.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 0.5f, 1.2f);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		// 缁熶竴 miss 澶勭悊锛堣秴绋?瓒呮椂/娑蹭綋/鏈懡涓級锛歰nBulletMiss 骞傜瓑锛屼粎浠嶅湪椋烇紙鏈懡涓級鏃剁粰 5 绉?CD
		if (!this.level().isClientSide && this.owner instanceof ServerPlayer sp) {
			SpiderMoonWeaverSwingManager.onBulletMiss(sp, this);
		}
		super.remove(reason);
	}
}
