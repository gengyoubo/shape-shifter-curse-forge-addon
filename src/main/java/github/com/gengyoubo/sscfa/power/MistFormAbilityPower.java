package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.BatDesmodusBloodThirst;
import github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.network.visual.recipes.MistFormRecipes;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.BatAttachService;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幽雾化形 {@code my_addon:mist_form}：吸血蝙蝠的主技能。
 *
 * <p>按下后化为一团雾（原版隐身让形态模型消失，碰撞箱缩成 0.2×0.1，见 {@link MistFormDimensions}），
 * 雾化期间除虚空外免疫伤害（数据驱动的 invulnerability 能力）。持续 {@code duration} tick，结束后按 JSON 冷却
 * （默认施放结束才起算）。化雾满 1 秒后再按可凝聚爆破：蓄力 1 秒后对 4 格内非白名单目标造成 12 点伤害并击退。
 * 75-100 血渴阶段，雾化中每秒对 2 格内目标造成 2 点伤害并回 2 点生命。被净化会立即中断并进入冷却。</p>
 *
 * <p>每个玩家的状态按 UUID 存在本类里；形态切换、离线时由 {@link #onRemoved} 清理。特效走服务端直接发原版粒子。</p>
 */
public final class MistFormAbilityPower implements SscPowerType {
	public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "mist_form");

	private static final String SCOPE = "abilities.bat_mist_burst";
	private static final int MIST_BURST_DELAY = 20;
	private static final int CHARGE_DURATION = 20;
	private static final double AOE_RADIUS = 4.0;
	private static final float AOE_DAMAGE = 12.0f;
	private static final double AOE_KNOCKBACK = 1.0;
	private static final int DEFAULT_DURATION = 90;

	private static int mistBurstDelay() { var s = BalanceIntegration.currentSnapshot(); return s != null ? (int) s.getInt(SCOPE, "min_delay_ticks") : MIST_BURST_DELAY; }
	private static int chargeDuration() { var s = BalanceIntegration.currentSnapshot(); return s != null ? (int) s.getInt(SCOPE, "charge_ticks") : CHARGE_DURATION; }
	private static double aoeRadius() { var s = BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble(SCOPE, "radius") : AOE_RADIUS; }
	private static float aoeDamage() { var s = BalanceIntegration.currentSnapshot(); return s != null ? (float) s.getDouble(SCOPE, "damage") : AOE_DAMAGE; }
	private static double aoeKnockback() { var s = BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble(SCOPE, "knockback") : AOE_KNOCKBACK; }

	/** 一个玩家的雾化状态。 */
	private static final class State {
		long castId = -1;
		boolean wasMist;
		long mistStartTime;
		boolean charging;
		long chargeStartTime;
		long lastAuraTime;
	}

	private final Map<UUID, State> states = new ConcurrentHashMap<>();

	private State state(ServerPlayer player) {
		return states.computeIfAbsent(player.getUUID(), ignored -> new State());
	}

	private static boolean isMist(ServerPlayer player) {
		return player.hasEffect(AddonEffects.MIST_FORM.get());
	}

	// ===== 按键 =====

	@Override
	public void onKeyPressed(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		if (!BalanceIntegration.isPlayerReady(player)) return;
		if (!FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) return;
		if (player.hasEffect(AddonEffects.PURIFIED.get())) return;
		State state = state(player);
		long now = player.level().getGameTime();
		if (isMist(player)) {
			// 化雾满最短间隔后再按：凝聚爆破蓄力；蓄力中再按无效
			if (now - state.mistStartTime >= mistBurstDelay() && !state.charging) startCharge(player, state, now);
		} else if (SkillCooldowns.ready(player, powerId.toString())) {
			enterMist(player, powerId, state, duration(data));
		}
	}

	private static int duration(JsonObject data) {
		return data.has("duration") ? data.get("duration").getAsInt() : DEFAULT_DURATION;
	}

	// ===== tick =====

	@Override
	public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		State state = states.get(player.getUUID());
		if (state == null) return;
		// 被净化：立即中断雾化（含蓄力）并进入冷却
		if (player.hasEffect(AddonEffects.PURIFIED.get())) {
			if (isMist(player)) {
				exitMist(player, true);
				applyCooldown(player, powerId, state);
			}
			state.charging = false;
			player.removeEffect(AddonEffects.MIST_CHARGING.get());
			state.wasMist = false;
			return;
		}
		boolean mist = isMist(player);
		ServerLevel world = player.serverLevel();

		if (state.charging) {
			long elapsed = world.getGameTime() - state.chargeStartTime;
			var origin = player.position();
			MistFormRecipes.charge(elapsed, chargeDuration(), player.getBbHeight(), 0, player.getRandom()::nextDouble,
					(particle, x, y, z, count, dx, dy, dz, speed, decoration, projectile) ->
							ParticleUtils.spawnParticles(world, particle, origin.x + x, origin.y + y, origin.z + z, count, dx, dy, dz, speed));
			if (elapsed >= chargeDuration()) {
				state.charging = false;
				player.removeEffect(AddonEffects.MIST_CHARGING.get());
				detonate(player);
				exitMist(player, false);
				applyCooldown(player, powerId, state);
				state.wasMist = false;
			}
			return;
		}

		// 自然结束：上一 tick 还在雾化、这一 tick 效果到期
		if (state.wasMist && !mist) {
			player.removeEffect(MobEffects.INVISIBILITY);
			applyCooldown(player, powerId, state);
			setFlightExempt(player, false);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
			state.wasMist = false;
			return;
		}

		if (mist) {
			if (world.getGameTime() - state.lastAuraTime >= 20) {
				state.lastAuraTime = world.getGameTime();
				bloodAura(player);
			}
			var origin = player.position();
			MistFormRecipes.ambient(player.getBbHeight(), 0, (particle, x, y, z, count, dx, dy, dz, speed, decoration, projectile) ->
					ParticleUtils.spawnParticles(world, particle, origin.x + x, origin.y + y, origin.z + z, count, dx, dy, dz, speed));
			state.wasMist = true;
		}
	}

	/** 75-100 血渴阶段：每秒对 2 格内非白名单生物造成 2 点伤害（不吃 75-100 阶段的伤害加成），每命中一个回 2 点生命。 */
	private static void bloodAura(ServerPlayer player) {
		if (BatDesmodusBloodThirst.getStage(player) != 3) return;
		DamageSource source = player.damageSources().magic();
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(2.0), t -> t != player && t.isAlive())) {
			if (player.distanceToSqr(target) > 4.0 || WhitelistUtils.isProtected(player, target)) continue;
			BatDesmodusBloodThirst.SUPPRESS_OUTGOING_BUFF.set(true);
			boolean dealt;
			try {
				dealt = target.hurt(source, NpcCombatEnhancements.scale(player, target, 2.0f, "desmodus_skill_damage", 2.0));
			} finally {
				BatDesmodusBloodThirst.SUPPRESS_OUTGOING_BUFF.set(false);
			}
			if (dealt) player.heal(2.0f);
		}
	}

	// ===== 移除 =====

	/** 形态切换、离线、数据重载：解除雾化、蓄力与飞行豁免，正在进行的施放按冷却结算。 */
	@Override
	public void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		State state = states.remove(player.getUUID());
		if (state != null && (state.charging || state.wasMist || isMist(player))) {
			state.charging = false;
			player.removeEffect(AddonEffects.MIST_CHARGING.get());
			exitMist(player, false);
			applyCooldown(player, powerId, state);
		}
		setFlightExempt(player, false);
	}

	// ===== 实现 =====

	private static void enterMist(ServerPlayer player, ResourceLocation powerId, State state, int duration) {
		String id = powerId.toString();
		if (!SkillCooldowns.begin(player, id)) return;
		state.castId = SkillCooldowns.currentCastId(player, id);
		SkillCooldowns.released(player, id, state.castId);
		// 解除贴墙附着，避免附着锁定与化雾飞行冲突
		if (BatAttachService.isAttached(player)) BatAttachService.detach(player, false);
		player.addEffect(new MobEffectInstance(AddonEffects.MIST_FORM.get(), duration, 0, false, false, true));
		// 原版隐身：SSC 在隐身时跳过形态模型渲染，从而「模型消失」
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
		player.refreshDimensions();
		state.wasMist = true;
		state.mistStartTime = player.level().getGameTime();
		state.lastAuraTime = state.mistStartTime;

		ServerLevel world = player.serverLevel();
		double cx = player.getX(), cy = player.getY() + player.getBbHeight() * 0.5, cz = player.getZ();
		ParticleUtils.spawnParticles(world, ParticleTypes.CLOUD, cx, cy, cz, 80, 0.5, 0.7, 0.5, 0.04);
		ParticleUtils.spawnParticles(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy, cz, 50, 0.45, 0.7, 0.45, 0.02);
		ParticleUtils.spawnParticles(world, MistFormRecipes.BLOOD_DUST, cx, cy, cz, 30, 0.5, 0.7, 0.5, 0.0);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
		setFlightExempt(player, true);
	}

	private static void exitMist(ServerPlayer player, boolean withEffect) {
		player.removeEffect(AddonEffects.MIST_FORM.get());
		player.removeEffect(MobEffects.INVISIBILITY);
		player.refreshDimensions();
		setFlightExempt(player, false);
		if (withEffect) {
			ServerLevel world = player.serverLevel();
			ParticleUtils.spawnParticles(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
					player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), 25, 0.4, 0.6, 0.4, 0.02);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
	}

	/** 雾化的三种结束路径都走这里：按 JSON 冷却起算。 */
	private static void applyCooldown(ServerPlayer player, ResourceLocation powerId, State state) {
		SkillCooldowns.completed(player, powerId.toString(), state.castId);
	}

	/** 蓄力：客户端据蓄力效果把化雾飞行减速 50%；雾化剩余不足时延长到爆破完成。 */
	private static void startCharge(ServerPlayer player, State state, long now) {
		state.charging = true;
		state.chargeStartTime = now;
		int ticks = chargeDuration() + 5;
		player.addEffect(new MobEffectInstance(AddonEffects.MIST_CHARGING.get(), ticks, 0, false, false, false));
		MobEffectInstance mist = player.getEffect(AddonEffects.MIST_FORM.get());
		if (mist == null || mist.getDuration() < ticks) {
			player.addEffect(new MobEffectInstance(AddonEffects.MIST_FORM.get(), ticks, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, false));
		}
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
	}

	/** 凝聚爆破：对范围内非白名单生物造成玩家攻击伤害（会拉仇恨）并向外击退，命中按 12/6/3 累积血渴。 */
	private static void detonate(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		DamageSource source = player.damageSources().playerAttack(player);
		double radius = aoeRadius();
		List<LivingEntity> hits = new ArrayList<>();
		for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
				t -> t != player && t.isAlive())) {
			if (player.distanceToSqr(target) > radius * radius || WhitelistUtils.isProtected(player, target)) continue;
			target.hurt(source, NpcCombatEnhancements.scale(player, target, aoeDamage(), "desmodus_skill_damage", 2.0));
			target.knockback(aoeKnockback(), player.getX() - target.getX(), player.getZ() - target.getZ());
			hits.add(target);
		}
		if (!hits.isEmpty()) BatDesmodusBloodThirst.onSkillHit(player, hits);
		double cy = player.getY() + player.getBbHeight() * 0.5;
		ParticleUtils.spawnParticles(world, ParticleTypes.EXPLOSION, player.getX(), cy, player.getZ(), 8, 1.0, 1.0, 1.0, 0.0);
		ParticleUtils.spawnParticles(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), cy, player.getZ(),
				60, radius * 0.5, 0.8, radius * 0.5, 0.05);
		ParticleUtils.spawnParticles(world, MistFormRecipes.BLOOD_DUST, player.getX(), cy, player.getZ(), 50, 0.1, 0.1, 0.1, 0.7);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
	}

	/** 雾化期间用无重力豁免原版悬空踢人检测（不用 allowFlying，避免误触创造飞行）；创造/旁观不改动。 */
	private static void setFlightExempt(ServerPlayer player, boolean exempt) {
		if (player.isCreative() || player.isSpectator()) return;
		player.setNoGravity(exempt);
	}
}
