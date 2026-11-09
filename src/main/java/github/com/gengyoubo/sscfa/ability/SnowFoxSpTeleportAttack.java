package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP雪狐近战次要技?- 瞬移攻击
 * 瞬移?0格范围内最?个敌人身后攻?
 */
public class SnowFoxSpTeleportAttack {

	private static final ConcurrentHashMap<UUID, TeleportAttackData> ATTACKING_PLAYERS = new ConcurrentHashMap<>();
	private static final double RANGE = 10.0;          // 以下常量均为默认锚点；balance 可覆盖（abilities.snow_fox_sp_teleport?
	private static final int MAX_TARGETS = 3;
	private static final float BASE_DAMAGE = 6.0f;
	private static final float BONUS_DAMAGE = 3.0f;
	private static final int MANA_COST_SUCCESS = 30;
	private static final int MANA_COST_FAIL = 20;
	private static final int TELEPORT_INTERVAL = 10;
	private static final float DAMAGE_REDUCTION = 1.0f;    // 攻击期间免伤?00%?026-10-20 ?0.65 上调）；balance damage_reduction 可覆?
	private static final int TELEPORT_REGEN_LOCK_TICKS = 100; // 回能锁默认；balance regen_lock_ticks 可覆?

	// 阶段 5：服务端权威快照读取
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.snow_fox_sp_teleport");
	// ==== NEW CODE: 使用FormIdentifiers（霜寒值读写已改走 ResourceBars，RESOURCE_ID 仅存于旧注释?===
	private static final ResourceLocation REGEN_COOLDOWN_ID = FormIdentifiers.SNOW_FOX_REGEN_COOLDOWN;

	private SnowFoxSpTeleportAttack() {
	}

	/**
	 * 执行瞬移攻击
	 * 门禁：power JSON（my_addon:fail_aware_active_self）原?cooldown / fail_cooldown 字段管理?
	 * 本方法只负责效果与失败标记：无目?蓝不??markFail()（按 fail_cooldown ?CD）?
	 */
	private static final String SKILL_ID = "my_addon:form_snow_fox_sp_melee_secondary";

	public static boolean execute(ServerPlayer player) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) return false;
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return false;
		if (ATTACKING_PLAYERS.containsKey(player.getUUID())) {
			FailAwareActiveSelfPower.markFail();
			return false;
		}

		int currentMana = github.com.gengyoubo.sscfa.resource.ResourceBars.get(player,
				github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX);
		if (currentMana < BAL.i("mana_cost_fail", MANA_COST_FAIL)) {
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			FailAwareActiveSelfPower.markFail();
			return false;
		}

		List<LivingEntity> targets = findTargets(player);

		if (targets.isEmpty()) {
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
			github.com.gengyoubo.sscfa.resource.ResourceBars.consume(player,
					github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX, BAL.i("mana_cost_fail", MANA_COST_FAIL));
			setRegenCooldown(player, BAL.i("regen_lock_ticks", TELEPORT_REGEN_LOCK_TICKS));
			FailAwareActiveSelfPower.markFail();
			return false;
		}

		if (currentMana < BAL.i("mana_cost_success", MANA_COST_SUCCESS)) {
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			FailAwareActiveSelfPower.markFail();
			return false;
		}

        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
		github.com.gengyoubo.sscfa.resource.ResourceBars.consume(player,
				github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX, BAL.i("mana_cost_success", MANA_COST_SUCCESS));
		setRegenCooldown(player, BAL.i("regen_lock_ticks", TELEPORT_REGEN_LOCK_TICKS));
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID);

		Vec3 originalPos = player.position();
		float originalYaw = player.getYRot();
		float originalPitch = player.getXRot();

		TeleportAttackData data = new TeleportAttackData(
				player.serverLevel(), originalPos, originalYaw, originalPitch,
				targets, 0, 0
		);
		data.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
        ATTACKING_PLAYERS.put(player.getUUID(), data);

		teleportToTarget(player, data);

		return true;
	}

	/**
	 * 每tick更新状?
	 */
	public static void tick(ServerPlayer player) {
		TeleportAttackData data = ATTACKING_PLAYERS.get(player.getUUID());
		if (data == null) return;
        if (player.serverLevel() != data.originalWorld
                || github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != data.castId
                || github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)) {
            ATTACKING_PLAYERS.remove(player.getUUID());
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
            return;
        }
        if (!player.isAlive()) {
            ATTACKING_PLAYERS.remove(player.getUUID());
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
            return;
        }
        if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) {
            // Cancelling a committed sequence still performs its safety return to the original position.
            returnToOrigin(player, data);
            ATTACKING_PLAYERS.remove(player.getUUID());
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, data.castId);
            return;
        }

		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			returnToOrigin(player, data);
			ATTACKING_PLAYERS.remove(player.getUUID());
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, data.castId);
			return;
		}

		data.ticksSinceLastTeleport++;

		player.setDeltaMovement(0, 0, 0);
		player.hurtMarked = true;

		if (data.ticksSinceLastTeleport >= BAL.i("teleport_interval", TELEPORT_INTERVAL)) {
			data.currentTargetIndex++;
			data.ticksSinceLastTeleport = 0;

			if (data.currentTargetIndex < data.targets.size()) {
				teleportToTarget(player, data);
			} else {
				returnToOrigin(player, data);
				ATTACKING_PLAYERS.remove(player.getUUID());
				github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, data.castId);
			}
		}
	}

	/**
	 * 瞬移到目标身后并攻击
	 */
	private static void teleportToTarget(ServerPlayer player, TeleportAttackData data) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(player)) return;
		if (data.currentTargetIndex >= data.targets.size()) return;

		LivingEntity target = data.targets.get(data.currentTargetIndex);

		if (!target.isAlive() || target.isRemoved() || target.isSpectator()
				|| target.level() != player.level() || player.serverLevel() != data.originalWorld
				|| WhitelistUtils.isProtected(player, target)
				|| github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)
				|| target.position().distanceToSqr(data.originalPos) > Math.pow(BAL.d("range", RANGE), 2)) {
			return;
		}

		Vec3 targetPos = target.position();
		Vec3 targetLookDir = target.getLookAngle().normalize();
		Vec3 behindPos = targetPos.subtract(targetLookDir.scale(1.5));
		behindPos = safeDestination(player, behindPos);
		if (behindPos == null) return;

		player.teleportToWithTicket(behindPos.x, behindPos.y, behindPos.z);

		Vec3 toTarget = targetPos.subtract(behindPos).normalize();
		float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
		player.setYRot(yaw);
		player.setXRot(0);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

		if (player.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnDecorationParticles(serverWorld, player, ParticleTypes.REVERSE_PORTAL,
					player.getX(), player.getY() + player.getBbHeight() / 2, player.getZ(),
					20, 0.3, 0.5, 0.3, 0.05);
		}

		player.swing(player.getUsedItemHand());

		float damage = (float) BAL.d("base_damage", BASE_DAMAGE);

		MobEffectInstance frostEffect = target.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.FROST_FREEZE.get());
		if (frostEffect != null) {
			damage += BAL.d("bonus_damage", BONUS_DAMAGE);
		}

		DamageSource source = player.damageSources().playerAttack(player);
		Vec3 oldVelocity = target.getDeltaMovement();
		if (target.hurt(source, damage)) {
			target.setDeltaMovement(oldVelocity);
			github.com.gengyoubo.sscfa.spell.FrostSpellLink.onNativeHit(player,target);
		}

		if (player.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE,
					target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
					15, 0.3, 0.3, 0.3, 0.1);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SWEEP_ATTACK,
					target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
					1, 0, 0, 0, 0);
		}

		player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
	}

	/**
	 * 返回原位
	 */
	private static void returnToOrigin(ServerPlayer player, TeleportAttackData data) {
		if (player.serverLevel() != data.originalWorld) return;
		Vec3 destination = safeDestination(player, data.originalPos);
		if (destination == null) return;
		player.teleportToWithTicket(destination.x, destination.y, destination.z);
		player.setYRot(data.originalYaw);
		player.setXRot(data.originalPitch);
		player.setDeltaMovement(0, 0, 0);
		player.hurtMarked = true;

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);

		if (player.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnDecorationParticles(serverWorld, player, ParticleTypes.REVERSE_PORTAL,
					player.getX(), player.getY() + player.getBbHeight() / 2, player.getZ(),
					30, 0.3, 0.5, 0.3, 0.05);
		}
	}

	private static Vec3 safeDestination(ServerPlayer player, Vec3 requested) {
		ServerLevel world = player.serverLevel();
		for (int dy = 0; dy <= 2; dy++) {
			for (int dx : new int[]{0, -1, 1}) for (int dz : new int[]{0, -1, 1}) {
				Vec3 candidate = requested.add(dx, dy, dz);
				AABB box = player.getBoundingBox().move(candidate.subtract(player.position()));
				if (world.hasChunkAt(net.minecraft.core.BlockPos.containing(candidate))
						&& box.minY >= world.getMinBuildHeight() && box.maxY <= world.getMaxBuildHeight()
						&& world.getWorldBorder().isWithinBounds(box) && world.noCollision(player, box)
						&& !world.containsAnyLiquid(box)) return candidate;
			}
		}
		return null;
	}

	/**
	 * 查找范围内的目标
	 */
	private static List<LivingEntity> findTargets(ServerPlayer player) {
		List<LivingEntity> result = new ArrayList<>();
		AABB searchBox = player.getBoundingBox().inflate(BAL.d("range", RANGE));

		List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
				LivingEntity.class, searchBox,
				entity -> entity != player &&
						!entity.isSpectator() &&
						entity.isAlive() &&
						!github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, entity) &&
						player.distanceToSqr(entity) <= BAL.d("range", RANGE) * BAL.d("range", RANGE) &&
						!WhitelistUtils.isProtected(player, entity)
		);

		nearbyEntities.sort(Comparator.comparingDouble(player::distanceToSqr));

		for (int i = 0; i < Math.min(BAL.i("max_targets", MAX_TARGETS), nearbyEntities.size()); i++) {
			result.add(nearbyEntities.get(i));
		}

		return result;
	}

	/**
	 * 检查玩家是否正在瞬移攻?
	 */
	public static boolean isAttacking(ServerPlayer player) {
		return ATTACKING_PLAYERS.containsKey(player.getUUID());
	}

	/**
	 * 玩家断线/死亡时清理所有状态，防止内存泄漏和重连后传送到错误位置
	 */
	public static void clearPlayer(java.util.UUID uuid) {
		ATTACKING_PLAYERS.remove(uuid);
	}

	/**
	 * 清除所有正在进行的传送攻击状?
	 */
	public static void clearAll() {
		ATTACKING_PLAYERS.clear();
	}

	/**
	 * 获取伤害减免系数（用于Mixin?
	 */
	public static float getDamageReduction(ServerPlayer player) {
		if (isAttacking(player)) {
			return (float) BAL.d("damage_reduction", DAMAGE_REDUCTION);
		}
		return 0.0f;
	}

	/**
	 * 设置回复冷却（使用后5秒内无法自然回复霜寒值）
	 * ==== NEW CODE: 使用PowerUtils ====
	 */
	public static void setRegenCooldown(ServerPlayer player, int value) {
		PowerUtils.setResourceValueAndSync(player, REGEN_COOLDOWN_ID, value);
	}

	/**
	 * 瞬移攻击数据
	 */
	private static class TeleportAttackData {
        long castId = -1;
		final ServerLevel originalWorld;
		final Vec3 originalPos;
		final float originalYaw;
		final float originalPitch;
		final List<LivingEntity> targets;
		int currentTargetIndex;
		int ticksSinceLastTeleport;

		TeleportAttackData(ServerLevel originalWorld, Vec3 originalPos,
		                   float originalYaw, float originalPitch,
		                   List<LivingEntity> targets,
		                   int currentTargetIndex, int ticksSinceLastTeleport) {
			this.originalWorld = originalWorld;
			this.originalPos = originalPos;
			this.originalYaw = originalYaw;
			this.originalPitch = originalPitch;
			this.targets = targets;
			this.currentTargetIndex = currentTargetIndex;
			this.ticksSinceLastTeleport = ticksSinceLastTeleport;
		}
	}
}

