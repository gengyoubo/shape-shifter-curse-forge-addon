package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.BatDesmodusBloodThirst;
import github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.init.AddonEffects;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * 吸血蝙蝠副技能 {@code my_addon:sonic_wave}：直线超声波（默认 3.5 格直径 × 8 格长的圆柱）。
 * 命中非白名单目标：6 点玩家攻击伤害（对敌对 NPC 按平衡参数放大）+ 反胃；玩家另加失聪，非玩家另加失明；
 * 命中后按 12/6/3 累积血渴值。冷却字段同统一技能（默认 160 tick，施放即起算），雾化与净化期间不能释放。
 */
public final class BatSonicWaveAbilityPower implements SscPowerType {
	public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "sonic_wave");

	private static final String SCOPE = "abilities.bat_sonic_wave";
	private static final double RANGE = 8.0;
	private static final double HALF_WIDTH = 1.75;
	private static final float DAMAGE = 6.0f;
	private static final int DEBUFF_TICKS = 60;

	private static double range() { var s = BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble(SCOPE, "range") : RANGE; }
	private static double halfWidth() { var s = BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble(SCOPE, "half_width") : HALF_WIDTH; }
	private static float damage() { var s = BalanceIntegration.currentSnapshot(); return s != null ? (float) s.getDouble(SCOPE, "damage") : DAMAGE; }
	private static int debuffTicks() { var s = BalanceIntegration.currentSnapshot(); return s != null ? (int) s.getInt(SCOPE, "debuff_ticks") : DEBUFF_TICKS; }

	@Override
	public void onKeyPressed(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		if (!BalanceIntegration.isPlayerReady(player)) return;
		if (!FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) return;
		if (player.hasEffect(AddonEffects.PURIFIED.get())) return;
		String id = powerId.toString();
		if (!SkillCooldowns.ready(player, id)) return;
		// 雾化期间禁止释放，避免与雾化爆破节奏冲突
		if (player.hasEffect(AddonEffects.MIST_FORM.get()) || player.hasEffect(AddonEffects.MIST_CHARGING.get())) return;
		fire(player);
		SkillCooldowns.instant(player, id);
	}

	private static void fire(ServerPlayer caster) {
		ServerLevel world = caster.serverLevel();
		Vec3 eye = caster.getEyePosition();
		Vec3 look = caster.getViewVector(1.0F).normalize();
		double range = range();
		double halfWidth = halfWidth();
		double halfWidthSq = halfWidth * halfWidth;

		AABB box = new AABB(eye.x - range, eye.y - range, eye.z - range, eye.x + range, eye.y + range, eye.z + range);
		List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, box,
				living -> living != caster && living.isAlive());
		DamageSource source = caster.damageSources().playerAttack(caster);

		List<LivingEntity> hits = new ArrayList<>();
		for (LivingEntity target : candidates) {
			Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
			// 沿视线的投影在 (0, range] 内（排除背后与超远目标），垂直视线的偏离在圆柱半径内
			double forward = toTarget.dot(look);
			if (forward <= 0.0 || forward > range) continue;
			if (toTarget.lengthSqr() - forward * forward > halfWidthSq) continue;
			if (WhitelistUtils.isProtected(caster, target)) continue;

			target.hurt(source, NpcCombatEnhancements.scale(caster, target, damage(), "desmodus_skill_damage", 2.0));
			target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, debuffTicks(), 0, false, true, true), caster);
			if (target instanceof Player) {
				target.addEffect(new MobEffectInstance(AddonEffects.DEAFEN.get(), debuffTicks(), 0, false, true, true), caster);
			} else {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, debuffTicks(), 0, false, true, true), caster);
			}
			hits.add(target);
		}

		spawnBeamParticles(world, eye, look, range, halfWidth);
		world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.6f);
		if (!hits.isEmpty()) BatDesmodusBloodThirst.onSkillHit(caster, hits);
	}

	/** 主轴每 0.7 格一发音爆，前方 2/4/6/8 格截面沿圆柱表面撒云雾。 */
	private static void spawnBeamParticles(ServerLevel world, Vec3 origin, Vec3 look, double range, double halfWidth) {
		for (double d = 0.5; d <= range; d += 0.7) {
			Vec3 p = origin.add(look.scale(d));
			ParticleUtils.spawnParticles(world, ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		Vec3 perp1 = new Vec3(-look.z, 0, look.x);
		if (perp1.lengthSqr() < 1e-6) perp1 = new Vec3(1, 0, 0);
		perp1 = perp1.normalize();
		Vec3 perp2 = look.cross(perp1).normalize();
		for (double d : new double[]{2.0, 4.0, 6.0, 8.0}) {
			if (d > range) break;
			Vec3 center = origin.add(look.scale(d));
			for (int i = 0; i < 12; i++) {
				double angle = 2 * Math.PI * i / 12;
				Vec3 p = center.add(perp1.scale(Math.cos(angle) * halfWidth)).add(perp2.scale(Math.sin(angle) * halfWidth));
				ParticleUtils.spawnParticles(world, ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
	}
}
