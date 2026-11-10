package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.NightmareDreamManager;
import github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月影野猫副技能 {@code my_addon:true_invisibility_dash}：震慑冲刺。
 *
 * <p>只能在真隐身中使用：破隐并向前冲刺，自身减速 1 秒；1 秒后震慑周围 5 格内的非野猫、非白名单目标 5 秒。
 * 没有独立冷却，破隐时与主技能一起结算（{@link TrueInvisibilityAbilityPower#applyUniversalCooldown}）。</p>
 */
public final class TrueInvisibilityDashAbilityPower implements SscPowerType {
	public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "true_invisibility_dash");

	private static final int STUN_DELAY_TICKS = 20;
	private static final int STUN_TICKS = 100;
	private static final double STUN_RADIUS = 5.0;
	/** 尚未冲刺。 */
	private static final int IDLE = -1;

	/** 当前拥有本能力的玩家 -> 冲刺后经过的 tick（{@link #IDLE} = 没有待结算的震慑）。 */
	private static final Map<UUID, Integer> TICKS_SINCE_DASH = new ConcurrentHashMap<>();

	/** 该实体当前是否拥有震慑冲刺（即是否是野猫）。 */
	public static boolean holds(Entity entity) {
		return TICKS_SINCE_DASH.containsKey(entity.getUUID());
	}

	@Override
	public void onKeyPressed(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		if (!BalanceIntegration.isPlayerReady(player)) return;
		if (!FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) return;
		// 只能在隐身中使用
		if (!player.hasEffect(AddonEffects.TRUE_INVISIBILITY.get())) return;

		player.removeEffect(AddonEffects.TRUE_INVISIBILITY.get());
		player.removeEffect(MobEffects.INVISIBILITY);
		// 缓慢 III，持续 1 秒
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, false));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CAT_HISS, SoundSource.PLAYERS, 1.0f, 1.0f);

		// 沿水平朝向前冲
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		player.push(-Mth.sin(yaw) * 1.5, 0.5, Mth.cos(yaw) * 1.5);
		player.hurtMarked = true;

		TICKS_SINCE_DASH.put(player.getUUID(), 0);
		TrueInvisibilityAbilityPower.applyUniversalCooldown(player);
	}

	@Override
	public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		int ticks = TICKS_SINCE_DASH.computeIfAbsent(player.getUUID(), ignored -> IDLE);
		if (ticks == IDLE) return;
		if (++ticks < STUN_DELAY_TICKS) {
			TICKS_SINCE_DASH.put(player.getUUID(), ticks);
			return;
		}
		TICKS_SINCE_DASH.put(player.getUUID(), IDLE);
		performStunEffect(player);
	}

	@Override
	public void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		TICKS_SINCE_DASH.remove(player.getUUID());
	}

	private static void performStunEffect(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(STUN_RADIUS),
				// 不震慑自己和其他野猫（无论对方是否隐身）
				e -> e != player && !TrueInvisibilityAbilityPower.holds(e) && !holds(e)
						&& e.distanceTo(player) <= STUN_RADIUS)) {
			if (WhitelistUtils.isProtected(player, target)) continue;
			// 食梦魔「入梦」：冲刺的野猫已入梦且目标正是把它打入梦的食梦魔 → 震慑无效
			if (NightmareDreamManager.isBlocked(player, target)) continue;
			NpcCombatEnhancements.applyWildCatStun(player, target, STUN_TICKS);
		}

		ParticleUtils.spawnParticles(world, ParticleTypes.POOF, player.getX(), player.getY(), player.getZ(), 15, 0.8, 0.2, 0.8, 0.1);
		ParticleUtils.spawnParticles(world, ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.5, 0.1, 0.5, 0.05);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.5f);
	}
}
