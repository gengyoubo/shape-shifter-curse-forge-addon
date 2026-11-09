package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 腐蚀之雾持续区域结算（jackcooper，服务端权威）。每个施法者至多一团雾
 * （重复施放刷新覆盖），雾随施法者移动（以施法者为圆心）??
 *
 * <p>??INTERVAL tick 对雾内敌人：中毒 I（POISON_TICKS?? 缓??I??s）??
 * 白名单受保护目标免受。断线清理?/p>
 */
public final class CorruptMistManager {
	private static final List<Mist> MISTS = new ArrayList<>();

	private static final class Mist {
		final UUID casterId;
		final ServerPlayer caster;
		final double radius;
		final UUID visualId = UUID.randomUUID();
		final int durationTicks;
		final double visualExtent;
		final double[] visualParams;
		int ticksRemaining;
		final int intervalTicks;
		int ticksToNextPulse;
		final int level;
		final UUID castId;
		final int poisonTicks;
		final int slownessTicks;
        final github.com.gengyoubo.sscfa.spell.BatSpellLink.Cast bat;
        final github.com.gengyoubo.sscfa.spell.CourtSpellLink.Cast court;
        final github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly;

		Mist(ServerPlayer caster, double radius, int durationTicks, int intervalTicks, int level, UUID castId) {
			this.casterId = caster.getUUID();
            this.bat=github.com.gengyoubo.sscfa.spell.BatSpellLink.current();
            this.court=github.com.gengyoubo.sscfa.spell.CourtSpellLink.current();
            this.anomaly=github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.current();
			this.caster = caster;
			this.radius = radius;
			this.durationTicks = durationTicks;
			this.visualExtent = Math.hypot(radius, 1);
			this.visualParams = new double[]{radius, durationTicks};
			this.ticksRemaining = durationTicks;
			this.intervalTicks = intervalTicks;
			this.ticksToNextPulse = intervalTicks;
			this.level = level;
			this.castId = castId;
			int basePoisonTicks = github.com.gengyoubo.sscfa.spell.spells.CorruptMistSpell.poisonTicks(); // 运行时快照读取（spells.corrupt_mist.poison_ticks??
			boolean venomAffinity = castId != null && github.com.gengyoubo.sscfa.util.FormUtils.isForm(
					caster, github.com.gengyoubo.sscfa.util.FormIdentifiers.SPIDER_SALTICIDAE);
			this.poisonTicks = github.com.gengyoubo.sscfa.spell.research.RuneCastContext.current().duration((castId == null ? basePoisonTicks
					: github.com.gengyoubo.sscfa.spell.FormAffinity.curseDurationTicks(caster, basePoisonTicks))
					+ (venomAffinity ? 60 : 0),github.com.gengyoubo.sscfa.spell.research.RuneModifiers.Stat.NEGATIVE);
			this.slownessTicks = github.com.gengyoubo.sscfa.spell.research.RuneCastContext.current().duration(castId == null ? 40
					: github.com.gengyoubo.sscfa.spell.FormAffinity.curseDurationTicks(caster, 40),github.com.gengyoubo.sscfa.spell.research.RuneModifiers.Stat.NEGATIVE);
		}
	}

	private CorruptMistManager() {
	}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
			Iterator<Mist> it = MISTS.iterator();
			while (it.hasNext()) {
				Mist mist = it.next();
				// 施法者离??被移????雾消散（DISCONNECT 已兜底，这里防意外引用）
				if (mist.caster.isRemoved() || !mist.caster.isAlive()) {
					ParametricVisuals.stop(mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST);
					it.remove();
					continue;
				}
				if (--mist.ticksRemaining <= 0) {
					ParametricVisuals.stop(mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST);
					it.remove();
					continue;
				}
				// Send the disk recipe once; every viewer samples the same event/tick on its client.
				ParametricVisuals.touch(mist.caster, mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST,
						mist.casterId, mist.caster.position(), true,
						mist.caster.level().getGameTime() - (mist.durationTicks - mist.ticksRemaining),
						mist.durationTicks, mist.visualExtent, mist.visualParams);
				// 每跳结算
				if (--mist.ticksToNextPulse <= 0) {
					mist.ticksToNextPulse = mist.intervalTicks;
					pulse(mist);
				}
			}
		});
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) ->
				removeMist(handler.player.getUUID()));
	}

	/** 施??刷新一团雾（同施法者旧雾被覆盖）?*/
	public static void start(ServerPlayer caster, double radius, int durationTicks,
	                         int intervalTicks, int level, UUID castId) {
		removeMist(caster.getUUID());
		MISTS.add(new Mist(caster, radius, durationTicks, intervalTicks, level, castId));
	}

	private static void removeMist(UUID casterId) {
		MISTS.removeIf(mist -> {
			if (!mist.casterId.equals(casterId)) return false;
			ParametricVisuals.stop(mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST);
			return true;
		});
	}

	/** 单跳结算：雾内敌人中?+ 缓速（白名单免受；L4+ 中??II）??*/
	private static void pulse(Mist mist) {
		if (!(mist.caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		// 高等级毒更深：L4+ 中??II（??橙卷轴）
		int poisonAmplifier = mist.level >= 4 ? 1 : 0;
		// 雾跳缓速：L5 ??II 级（2026-09-17 用户定稿?
		int slownessAmplifier = mist.level >= 5 ? 1 : 0;
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				mist.caster.getBoundingBox().inflate(mist.radius), e -> e != mist.caster && e.isAlive());
		for (LivingEntity target : targets) {
			if (target.distanceTo(mist.caster) > mist.radius) {
				continue;
			}
			// 领域隔离：目标被任一领域壳隔开（与施法者分属内外）??不中毒不缓??
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(mist.caster, target)) {
				continue;
			}
			if (WhitelistUtils.isProtected(mist.caster, target)) {
				continue;
			}
			boolean hadHarmfulEffect = github.com.gengyoubo.sscfa.spell.FormCastingStyle.hasHarmfulEffect(target);
            boolean applied=github.com.gengyoubo.sscfa.spell.CourtSpellLink.with(mist.court,()->github.com.gengyoubo.sscfa.spell.NightmareSpellLink.with(mist.caster,()->github.com.gengyoubo.sscfa.spell.BatSpellLink.with(mist.bat,()->target.addEffect(new MobEffectInstance(MobEffects.POISON,mist.poisonTicks,poisonAmplifier)))));
            boolean slowed=github.com.gengyoubo.sscfa.spell.CourtSpellLink.with(mist.court,()->github.com.gengyoubo.sscfa.spell.NightmareSpellLink.with(mist.caster,()->github.com.gengyoubo.sscfa.spell.BatSpellLink.with(mist.bat,()->target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,mist.slownessTicks,slownessAmplifier)))));
            applied|=slowed;
            if (applied || github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(target,MobEffects.POISON,mist.poisonTicks,poisonAmplifier,false)
                    || github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(target,MobEffects.MOVEMENT_SLOWDOWN,mist.slownessTicks,slownessAmplifier,false))
                github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(mist.anomaly,target);
			if (applied) {
				github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(mist.caster, target,
						github.com.gengyoubo.sscfa.spell.FormationElement.CURSE, mist.castId, hadHarmfulEffect);
			}
		}
	}
}
