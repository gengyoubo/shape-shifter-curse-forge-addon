package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class WaterSpearEntity extends ThrownTrident {

	private ItemStack waterSpearStack = new ItemStack(github.com.gengyoubo.sscfa.SscAddon.WATER_SPEAR);

	public WaterSpearEntity(EntityType<? extends ThrownTrident> entityType, Level world) {
		super(github.com.gengyoubo.sscfa.SscAddon.WATER_SPEAR_ENTITY, world);
		// 涓嶅彲鎷惧彇锛氭按鐭涘彧鑳藉悎鎴愯幏寰楋紙璧?5 绉?CD + 鏈€澶氫竴鎶婏級锛屾墧鍑哄嵆娑堣€?
		this.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
	}

	public WaterSpearEntity(Level world, LivingEntity owner, ItemStack stack) {
		super(github.com.gengyoubo.sscfa.SscAddon.WATER_SPEAR_ENTITY, world);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.waterSpearStack = stack.copy();
		// 涓嶅彲鎷惧彇锛氭按鐭涘彧鑳藉悎鎴愯幏寰楋紙璧?5 绉?CD + 鏈€澶氫竴鎶婏級锛屾墧鍑哄嵆娑堣€?
		this.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
	}

	public ItemStack getWeaponStack() {
		return this.waterSpearStack;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("WaterSpear", 10)) {
			this.waterSpearStack = ItemStack.of(nbt.getCompound("WaterSpear"));
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.put("WaterSpear", this.waterSpearStack.save(new CompoundTag()));
	}


	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		Entity entity = entityHitResult.getEntity();
		Level world = this.level();

		if (!world.isClientSide && entity instanceof LivingEntity target) {
			// Direct damage
			float damage = 10.0f;
			target.hurt(this.damageSources().trident(this, this.getOwner()), damage);

			// Apply slowness锛堝甫鎶曟幏鑰?source 渚涘叆姊︽嫤鎴綊鍥狅級
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this.getOwner());

			// Area damage
			doAreaDamage(target.position().add(0, target.getBbHeight() / 2, 0), target);
		}

		// Remove the spear after hitting
		this.discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		Level world = this.level();

		if (!world.isClientSide) {
			doAreaDamage(this.position(), null);
		}

		// Remove the spear after hitting block
		this.discard();
	}

	private void doAreaDamage(Vec3 pos, Entity directTarget) {
		Level world = this.level();
		double x = pos.x;
		double y = pos.y;
		double z = pos.z;

		// Get entities within 1.5 block radius (3 block diameter)
		List<Entity> nearbyEntities = world.getEntities(this.getOwner(), new AABB(x - 1.5, y - 1.5, z - 1.5, x + 1.5, y + 1.5, z + 1.5));
		for (Entity nearEntity : nearbyEntities) {
			if (nearEntity instanceof LivingEntity living && nearEntity != this.getOwner() && nearEntity != directTarget) {
				living.hurt(this.damageSources().trident(this, this.getOwner()), 4.0f);
			}
		}

		// Play splash sound and particles
		world.playSound(null, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 0.8F);

		// Spawn particles on server - 钀藉湴姘磋姳鐖嗗紑锛堜豢 RC-4 鑽按鐮寸鐗规晥锛?
		if (world instanceof net.minecraft.server.level.ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnWaterBurst(serverWorld, x, y, z, 1.0);
		}
	}

	@Override
	public void tick() {
		super.tick();

		// Spawn water trail particles
		Level world = this.level();
		if (world.isClientSide && !this.inGround) {
			for (int i = 0; i < 2; i++) {
				world.addParticle(ParticleTypes.DRIPPING_WATER, true,
						this.getX() + (world.random.nextDouble() - 0.5) * 0.3,
						this.getY() + (world.random.nextDouble() - 0.5) * 0.3,
						this.getZ() + (world.random.nextDouble() - 0.5) * 0.3,
						0, 0, 0);
			}
		}
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.GENERIC_SPLASH;
	}

	@Override
	public boolean isNoGravity() {
		return false;
	}

	@Override
	public void playerTouch(Player player) {
		// 姘寸煕鎵斿嚭鍗虫秷鑰椼€佹案杩滀笉鍙嬀鍙栵紙鍚噸鍚墠鏃?ALLOWED 瀹炰綋锛夛細纰版挒涓嶅仛浠讳綍鎷惧彇澶勭悊銆?
		// 鍞竴鑾峰彇閫斿緞=鍚堟垚锛? 绉?CD + 鏈€澶氫竴鎶婏級銆?
	}

}
