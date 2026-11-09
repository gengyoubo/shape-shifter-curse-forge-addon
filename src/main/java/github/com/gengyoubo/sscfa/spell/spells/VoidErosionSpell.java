package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/**
 * 虚空侵蚀（虚无系，绿色基底，jackcooper）：以自身为圆心爆发虚无波，
 * 范围内敌人挖掘疲劳 + 虚弱 8s（对怪物对玩家均生效，走白名单）。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/void_erosion.json}：
 * 基准半径 3 格 / 8s / cd 20s（L5 12s，每级 -2s）/ 耗蓝 20；半径按 speed_multiplier 缩放。</p>
 */
public class VoidErosionSpell extends Spell {

	/** 基础半径（格）默认，实际半径 = 基础 × speed_multiplier(level)；运行时从 balance 快照读取。 */
	private static final double BASE_RADIUS = 3.0;
	/** 减益时长（tick）：8s。默认值；运行时从 balance 快照读取。 */
	private static final int DURATION_TICKS = 160;

	// 阶段 5：运行时快照读取（spells.void_erosion；快照未初始化回退上方默认常量）
	private static final BalanceReader BAL = new BalanceReader("spells.void_erosion");

	public VoidErosionSpell() {
		super(new ResourceLocation("ssc_addon", "void_erosion"), SpellRarity.GREEN);
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
		double radius = RuneCastContext.current().scale(BAL.d("base_radius", BASE_RADIUS) * getSpeedMultiplier(level),RuneModifiers.Stat.AREA);
		// 减益时长运行时读取（下方两种减益共用局部变量，快照未初始化回退默认常量）
		int durationTicks = RuneCastContext.current().duration(BAL.i("duration_ticks", DURATION_TICKS),RuneModifiers.Stat.NEGATIVE);
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
		for (LivingEntity target : targets) {
			if (target.distanceTo(caster) > radius) {
				continue;
			}
			// 领域隔离：目标被任一领域壳隔开（与施法者分属内外）→ 不变减益
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(caster, target)) {
				continue;
			}
			// 默认白名单：受保护目标免受减益
			if (WhitelistUtils.isProtected(caster, target)) {
				continue;
			}
			// 减益每两级 +1 级：L1/L2=疲劳 I + 虚弱 II、L3/L4=II + III、L5=III + IV
			int debuffAmplifier = (level - 1) / 2;
			boolean applied = target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, debuffAmplifier));
			applied |= target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1 + debuffAmplifier));
            if (applied || github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(target,MobEffects.DIG_SLOWDOWN,durationTicks,debuffAmplifier,false)
                    || github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(target,MobEffects.WEAKNESS,durationTicks,1+debuffAmplifier,false))
                github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(caster,target);
			if (applied) {
				github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(caster, target,
						github.com.gengyoubo.sscfa.spell.FormationElement.VOID,
						solo ? null : ssc_addon$getRefundCastId());
			}
		}
		// 演出：暗紫侵蚀波纹（双圈）+ 低沉音效
		github.com.gengyoubo.sscfa.util.SpellFxUtils.ring(serverWorld, caster, ParticleTypes.PORTAL,
				caster.getX(), caster.getY() + 0.2, caster.getZ(), radius * 0.6, 20);
		github.com.gengyoubo.sscfa.util.SpellFxUtils.ring(serverWorld, caster, ParticleTypes.PORTAL,
				caster.getX(), caster.getY() + 0.4, caster.getZ(), radius, 28);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.WARDEN_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.6f);
	}
}
