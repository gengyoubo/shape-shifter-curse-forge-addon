package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP雪狐近战主要技?- 雪刺冲刺
 * 向准星方向冲?格，碰撞敌人造成6点魔法伤害并施加霜凝效果3?
 */
public class SnowFoxSpMeleeAbility {

	private static final ConcurrentHashMap<UUID, DashingPlayerData> DASHING_PLAYERS = new ConcurrentHashMap<>();
	private static final double DASH_DISTANCE = 8.0;   // 默认；balance 可覆盖（abilities.snow_fox_sp_melee?
	private static final double DASH_SPEED = 1.5;
	private static final float DAMAGE = 8.0f;
	private static final int FROST_FREEZE_DURATION = 60;
	private static final int MANA_COST = 15;
	private static final int MELEE_REGEN_LOCK_TICKS = 100; // 回能锁默认；balance regen_lock_ticks 可覆?
	private static final float DAMAGE_REDUCTION = 1.0f;    // 冲刺期间免伤?00%）；balance damage_reduction 可覆?

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.snow_fox_sp_melee");
	// ==== NEW CODE: 使用FormIdentifiers ====
	private static final ResourceLocation REGEN_COOLDOWN_ID = FormIdentifiers.SNOW_FOX_REGEN_COOLDOWN;

	private SnowFoxSpMeleeAbility() {
	}

	/**
	 * 执行雪刺冲刺
	 * 门禁：power JSON（my_addon:fail_aware_active_self）原?cooldown 字段管理?
	 * 本方法只负责效果与失败标记：蓝不?冲刺??markFail()（按 fail_cooldown ?CD）?
	 */
	private static final String SKILL_ID = "my_addon:form_snow_fox_sp_melee_primary";

	public static boolean execute(ServerPlayer player) {
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return false;
		if (github.com.gengyoubo.sscfa.resource.ResourceBars.get(player,
                github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX) < BAL.i("mana_cost", MANA_COST)) {
			// 法力不足提示音仅施法者自己听（player.playSound 在服务端会排除自己，故改为定向发包）
			github.com.gengyoubo.sscfa.ability.MancianimaMarkManager.playSoundToPlayer(
					player, SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.0f);
			FailAwareActiveSelfPower.markFail();
			return false;
		}

		if (DASHING_PLAYERS.containsKey(player.getUUID())) {
			FailAwareActiveSelfPower.markFail();
			return false;
		}

        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
        github.com.gengyoubo.sscfa.resource.ResourceBars.consume(player,
                github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX, BAL.i("mana_cost", MANA_COST));
		PowerUtils.setResourceValueAndSync(player, REGEN_COOLDOWN_ID, BAL.i("regen_lock_ticks", MELEE_REGEN_LOCK_TICKS));

		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID);

		Vec3 lookDir = player.getLookAngle().normalize();

		DashingPlayerData data = new DashingPlayerData(lookDir, 0);
        data.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		DASHING_PLAYERS.put(player.getUUID(), data);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0f, 1.2f);

		return true;
	}

	/**
	 * 每tick更新冲刺状?
	 */
	public static void tick(ServerPlayer player) {
		DashingPlayerData data = DASHING_PLAYERS.get(player.getUUID());
		if (data == null) return;
        if (!player.isAlive()) {
            DASHING_PLAYERS.remove(player.getUUID());
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
            return;
        }

		double distanceMoved = data.ticksElapsed * BAL.d("dash_speed", DASH_SPEED);

		// 不要检?verticalCollision：踩在地面时 vanilla Entity.move 会基?重力?movement.y=-0.08
		// 但被地面阻挡?0，movement.y != vec3d.y"恒置 verticalCollision=true，导致站立触发的 dash
		// 在第一 tick 就被这里 return 掉，setVelocity 一次都没执行，玩家原地不动也碰不到敌人?
		// 水平撞墙才需要终?dash，所以只?horizontalCollision?
		if (distanceMoved >= BAL.d("dash_distance", DASH_DISTANCE) || player.horizontalCollision) {
			DASHING_PLAYERS.remove(player.getUUID());
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, data.castId);
			return;
		}

		Vec3 velocity = data.direction.scale(BAL.d("dash_speed", DASH_SPEED));
		player.setDeltaMovement(velocity);
		player.hurtMarked = true;
		// 关键（多?客机修复）：玩家移动是客户端权威，单?velocityModified 不保证把"自身速度"
		// 下发给控制端，远端客机会原地不动、冲刺无位移、也碰不到沿途实体导致无伤害?
		// 这里?tick 显式给该玩家连接补发速度包，强制客机应用冲刺速度（主机本地玩家不受影响）?
		if (player.connection != null) {
			player.connection.send(
					new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
		}

		AABB hitbox = player.getBoundingBox().inflate(0.5);
		List<Entity> nearbyEntities = player.level().getEntities(player, hitbox,
				entity -> entity instanceof LivingEntity && !data.hitEntities.contains(entity.getUUID()));

		for (Entity entity : nearbyEntities) {
			if (entity instanceof LivingEntity target) {
				if (WhitelistUtils.isProtected(player, target)) {
					data.hitEntities.add(entity.getUUID());
					continue;
				}
				data.hitEntities.add(entity.getUUID());

				DamageSource source = player.damageSources().playerAttack(player);
				boolean damaged=target.hurt(source, (float) BAL.d("damage", DAMAGE));

				boolean frozen=target.addEffect(new MobEffectInstance(
						github.com.gengyoubo.sscfa.init.AddonEffects.FROST_FREEZE.get(),
						BAL.i("freeze_ticks", FROST_FREEZE_DURATION),
						0,
						false,
						true,
						true
				), player);
				if(damaged||frozen)github.com.gengyoubo.sscfa.spell.FrostSpellLink.onNativeHit(player,target);

				// 使用ParticleUtils
				if (player.level() instanceof ServerLevel serverWorld) {
					ParticleUtils.spawnHitParticles(serverWorld, new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ()));
				}

				player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
						SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.5f);
			}
		}

		// 使用ParticleUtils
		if (player.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnSnowflakeParticles(serverWorld, new Vec3(player.getX(), player.getY() + 0.5, player.getZ()));
		}

		data.ticksElapsed++;
	}

	/**
	 * 玩家断线/死亡时清理所有状态，防止内存泄漏和重连后状态错?
	 */
	public static void clearPlayer(java.util.UUID uuid) {
		DASHING_PLAYERS.remove(uuid);
	}

	/**
	 * 清理所有冲刺状态，用于服务器重置或全局清理
	 */
	public static void clearAll() {
		DASHING_PLAYERS.clear();
	}

	/**
	 * 检查玩家是否正在雪刺冲刺（?mixin 免伤查询）?
	 */
	public static boolean isDashing(ServerPlayer player) {
		return DASHING_PLAYERS.containsKey(player.getUUID());
	}

	/**
	 * 雪刺冲刺期间的伤害减免系数（?mixin；默?1.0 = 100% 免伤，balance damage_reduction 可调）?
	 */
	public static float getDashDamageReduction(ServerPlayer player) {
		if (isDashing(player)) {
			return (float) BAL.d("damage_reduction", DAMAGE_REDUCTION);
		}
		return 0.0f;
	}

	/**
	 * 冲刺中玩家数?
	 */
	private static class DashingPlayerData {
        long castId = -1;
		final Vec3 direction;
		final Set<UUID> hitEntities;
		int ticksElapsed;

		DashingPlayerData(Vec3 direction, int ticksElapsed) {
			this.direction = direction;
			this.hitEntities = new HashSet<>();
			this.ticksElapsed = ticksElapsed;
		}
	}
}
