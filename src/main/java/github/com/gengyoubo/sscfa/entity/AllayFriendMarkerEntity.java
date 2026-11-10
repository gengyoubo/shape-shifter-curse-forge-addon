package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.AllaySPGroupHeal;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AllayFriendMarkerEntity extends ThrowableItemProjectile {

	private static final Logger LOGGER = LoggerFactory.getLogger("SscAddon-FriendMarker");

	public AllayFriendMarkerEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
		super(entityType, world);
	}

	public AllayFriendMarkerEntity(Level world, LivingEntity owner) {
		super(SscAddon.FRIEND_MARKER_ENTITY_TYPE, owner, world);
	}

	public AllayFriendMarkerEntity(Level world, double x, double y, double z) {
		super(SscAddon.FRIEND_MARKER_ENTITY_TYPE, x, y, z, world);
	}

	@Override
	protected Item getDefaultItem() {
		return SscAddon.FRIEND_MARKER;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.tickCount > 40) {
			this.discard();
		}
	}

	@Override
	public void handleEntityEvent(byte status) {
		if (status == 3) {
			for (int i = 0; i < 8; ++i) {
				this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
						this.getX(), this.getY(), this.getZ(),
						((double) this.random.nextFloat() - 0.5D) * 0.08D,
						((double) this.random.nextFloat() - 0.5D) * 0.08D,
						((double) this.random.nextFloat() - 0.5D) * 0.08D);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		if (this.level().isClientSide) return;
		Entity entity = entityHitResult.getEntity();
		if (!(entity instanceof LivingEntity target)) return;

		// 瑙ｆ瀽鎶曟幏鑰呬负銆屽綋鍓嶆椿璺冪殑鏈嶅姟绔帺瀹跺疄渚嬨€嶏細
		// getOwner() 鍦ㄥ浜?/ 瀹㈡満鐜涓嬪彲鑳借繑鍥炶繃鏈熸垨闈炴椿璺冪殑鐜╁闀滃儚锛?
		// 瀵艰嚧 addToWhitelist 鍐欏埌浜嗛敊璇璞°€佹椿璺冪帺瀹剁殑 commandTags 浠嶄负绌猴紙瀹㈡満鐧藉悕鍗曞け鏁堢殑鏍瑰洜涔嬩竴锛夈€?
		ServerPlayer writeTarget = resolveActiveOwner();
		if (writeTarget == null) {
			LOGGER.warn("[FriendMarker] 鏍囪澶辫触锛氭棤娉曡В鏋愭姇鎺疯€呮椿璺冨疄渚?owner={}",
					this.getOwner() == null ? "null" : this.getOwner().getClass().getSimpleName());
			return;
		}

		// 鍔犲叆鐧藉悕鍗曪紙vex 涓嶆敾鍑汇€佸皷鍚撲笉鏍囪锛? ssc_raid_friend锛堣鍑荤敓鐗╀笉鏀诲嚮鐩爣锛?
		AllaySPGroupHeal.addToWhitelist(writeTarget, target);
		target.addTag("ssc_raid_friend");

		long wlCount = writeTarget.getTags().stream()
				.filter(t -> t.startsWith(AllaySPGroupHeal.WHITELIST_TAG_PREFIX)).count();
		LOGGER.info("[FriendMarker] {} 鏍囪鎴愬姛锛屽綋鍓嶇櫧鍚嶅崟鏉＄洰鏁?{}",
				writeTarget.getName().getString(), wlCount);

		// 绔嬪嵆鎶婃渶鏂扮櫧鍚嶅崟鎺ㄩ€佸埌璇ョ帺瀹跺鎴风锛岀‘淇?GUI 涓庢湇鍔＄ commandTags 瀹炴椂涓€鑷达紙淇澶氫汉涓?GUI 缂撳瓨涓庢湇鍔＄涓嶅悓姝ワ級
		SscAddonNetworking.sendWhitelistSync(writeTarget);
	}

	/** 閫氳繃 ownerUuid 浠?PlayerManager 鍙栧綋鍓嶆椿璺冩湇鍔＄鐜╁瀹炰緥锛岄伩鍏嶆妸鐧藉悕鍗曞啓鍒拌繃鏈?/ 閲嶅闀滃儚涓娿€?*/
	private ServerPlayer resolveActiveOwner() {
		Entity owner = this.getOwner();
		UUID uuid = (owner instanceof ServerPlayer sp) ? sp.getUUID() : null;
		if (uuid == null) return null;
		if (this.getServer() != null) {
			ServerPlayer active = this.getServer().getPlayerList().getPlayer(uuid);
			if (active != null) return active;
		}
		return (owner instanceof ServerPlayer sp2) ? sp2 : null;
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			this.level().broadcastEntityEvent(this, (byte) 3);
			this.discard();
		}
	}
}
