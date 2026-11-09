package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import java.util.List;

/**
 * 烈焰新星（火系，绿色基底，jackcooper）：以自身为圆心爆发火环，范围内造成伤害 + 击退 + 点燃 2s。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/flame_nova.json}：
 * 基准 4 伤 / 半径 4 格 / cd 12s / 耗蓝 20 逐级 ×1.25（复利，20/25/31/39/49）；
 * 半径按 speed_multiplier 缩放（每级 +0.5 格），
 * 稀有度为蓝/橙时额外 +25%。
 * 白名单：主人在线且目标受保护 → 免伤；施法者本人不受影响。</p>
 */
public class FlameNovaSpell extends Spell {

	/** 基础半径（格）默认；实际半径 = 基础 × speed_multiplier(level)。运行时从 balance 快照读取。 */
	private static final double BASE_RADIUS = 4.0;
	/** 点燃时长（tick）默认。 */
	private static final int FIRE_TICKS = 40;

	// 阶段 4：服务端权威快照读取（spells.flame_nova；快照未初始化回退默认常量）
	private static double baseRadius() { var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble("spells.flame_nova", "base_radius") : BASE_RADIUS; }
	private static int fireTicks() { var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.currentSnapshot(); return s != null ? (int) s.getInt("spells.flame_nova", "fire_ticks") : FIRE_TICKS; }
	private static double knockback() { var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble("spells.flame_nova", "knockback") : 0.8; }
	private static double rarityRadiusMultiplier() { var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.currentSnapshot(); return s != null ? s.getDouble("spells.flame_nova", "rarity_radius_multiplier") : 1.25; }

	public FlameNovaSpell() {
		super(new ResourceLocation("ssc_addon", "flame_nova"), SpellRarity.GREEN);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		if (!(caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		double radius = baseRadius() * getSpeedMultiplier(level);
		// 稀有度为蓝/橙时，生效范围额外 +25%（独立于等级缩放，balance 可调倍率）
		SpellRarity rarity = getRarity(level);
		if (rarity == SpellRarity.BLUE || rarity == SpellRarity.ORANGE) {
			radius *= rarityRadiusMultiplier();
		}
		radius=RuneCastContext.current().scale(radius,RuneModifiers.Stat.AREA);
        List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
		LivingEntity lastHitTarget = null;
		LivingEntity killedTarget = null;
		boolean hitBurningTarget = false;
		int hitExpTen = ssc_addon$takePendingExp();
		for (LivingEntity target : targets) {
			if (target.distanceTo(caster) > radius) {
				continue;
			}
			// 公共命中结算（白名单豁免 → 法术伤害 → 经验补发；FIRE 系钩子为聚合语义，循环外调）：见 SpellHitHelper
			var hit = github.com.gengyoubo.sscfa.spell.SpellHitHelper.hitRaw(
					caster, target, power, hitExpTen);
			if (hit != github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				continue;
			}
			hitExpTen = 0; // Protected or immune targets keep the budget for later targets.
			lastHitTarget = target;
			hitBurningTarget |= target.getRemainingFireTicks() > 0;
			if (!target.isAlive()) killedTarget = target;
			target.setRemainingFireTicks(RuneCastContext.current().duration(fireTicks(),RuneModifiers.Stat.BURN));
			// 击退：远离施法者（强度 balance 可调）
			Vec3 knock = new Vec3(target.getX() - caster.getX(), 0.1, target.getZ() - caster.getZ())
					.normalize().scale(knockback());
			target.push(knock.x, knock.y, knock.z);
			target.hurtMarked = true;
		}
		if (lastHitTarget != null) {
			github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(
					caster, killedTarget != null ? killedTarget : lastHitTarget,
					github.com.gengyoubo.sscfa.spell.FormationElement.FIRE,
					solo ? null : ssc_addon$getRefundCastId(), hitBurningTarget);
		}
		// 演出：球形火焰粒子（双层球面 + 烟火）+ 音效
		github.com.gengyoubo.sscfa.util.SpellFxUtils.sphere(serverWorld, caster,
				caster.getX(), caster.getY() + 1.0, caster.getZ(), radius,
				ParticleTypes.FLAME, ParticleTypes.LAVA, ParticleTypes.CAMPFIRE_COSY_SMOKE, 6);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.7f);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.9f);
	}
}
