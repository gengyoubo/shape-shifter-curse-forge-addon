package github.com.gengyoubo.sscfa.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FoxFireBurnEffect extends MobEffect {
	public FoxFireBurnEffect() {
		super(MobEffectCategory.HARMFUL, 0x3366FF); // Blue color
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	private static DamageSource damageSource(LivingEntity entity) {
		FoxFireBurnOwnership origin = FoxFireBurnOwnership.resolve(entity);
		if (origin.recorded()) {
			Player owner = origin.uuid() == null ? null : entity.level().getPlayerByUUID(origin.uuid());
			// 已知归属离线或旧记录有歧义时，保留火焰伤害，不转嫁给旧施法者或最近攻击者???
			return owner == null ? entity.damageSources().inFire() : entity.damageSources().playerAttack(owner);
		}
		if (entity.getLastAttacker() instanceof Player player) return entity.damageSources().playerAttack(player);
		if (entity.getLastHurtByMob() instanceof Player player) return entity.damageSources().playerAttack(player);
		return entity.damageSources().inFire();
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			entity.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.FOX_FIRE_BURN.get());
			return;
		}

		if (entity.level().isClientSide) {
			// Client side particles can be handled here or via separate client tick handler
			// But applyUpdateEffect runs on both usually if registered correctly.
			// For simple visuals, we might rely on server spawning particles or client side implementation.
			// StatusEffect particles are usually handled by the game automatically if color is set,
			// but we want Soul Fire particles.
			entity.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, true,
					entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth(),
					entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight(),
					entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth(),
					0, 0, 0);
		} else {
			// Server side logic
			// Damage every second (20 ticks) like vanilla fire
			if (entity.tickCount % 20 == 0) {
				DamageSource source = damageSource(entity);

				net.minecraft.world.phys.Vec3 oldVelocity = entity.getDeltaMovement();
				DamageSource burnSource = source;
				github.com.gengyoubo.sscfa.spell.DomainManager.runAttachedEffect(burnSource.getEntity(), entity, () -> {
					if (github.com.gengyoubo.sscfa.ability.KillEmpowerManager.damageBurn(entity, burnSource, 1.0f)) {
						entity.setDeltaMovement(oldVelocity);
					}
				});

				// Spawn explicit particles on server for everyone to see
				if (entity.level() instanceof ServerLevel serverWorld) {
					github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
							entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
							2, 0.3, 0.3, 0.3, 0.05);
				}
			}
		}
	}
}
