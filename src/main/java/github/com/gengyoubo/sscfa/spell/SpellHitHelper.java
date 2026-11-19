package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

/**
 * 法术命中结算辅助：白名单豁免 → 法术伤害（王庭联动）→ 命中经验 → 悦灵联动 → 异常通知。
 *
 * <p>流派钩子（{@link FormCastingStyle#onSpellHit}）有两种调用形态：逐目标系别用
 * {@link #projectileHit}（钩子 20t 防重窗挡掉 AOE 重复返还）；FIRE 系需要「击杀优先 + 燃烧聚合」，
 * 循环内用不带钩子的 {@link #hitRaw}，聚合钩子留在调用方循环外。</p>
 *
 * <p>经验预算：只在返回 {@code HIT} 时发放；调用方随后清零，保护/免疫目标不消耗预算。</p>
 */
public final class SpellHitHelper {

	private SpellHitHelper() {}

	/** {@code HIT} 已造成伤害 / {@code PROTECTED} 白名单豁免 / {@code IMMUNE} 伤害未生效。 */
	public enum HitResult { HIT, PROTECTED, IMMUNE }

	public static HitResult projectileHit(Entity owner, LivingEntity target, float damage,
	                                      FormationElement element, UUID refundCastId, int expBounty) {
		return projectileHit(owner, target, damage, element, refundCastId, expBounty,
				github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.context(owner));
	}

	/** 显式异常快照：用于施法作用域结束后才结算伤害的效果。 */
	public static HitResult projectileHit(Entity owner, LivingEntity target, float damage,
	                                      FormationElement element, UUID refundCastId, int expBounty,
	                                      github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly) {
		HitResult result = hitRaw(owner, target, damage, expBounty, anomaly);
		if (result != HitResult.HIT) return result;
		Entity resolved = AllaySpellLink.owner(owner);
		if (resolved instanceof ServerPlayer styleOwner) {
			FormCastingStyle.onSpellHit(styleOwner, target, element, refundCastId);
		}
		return result;
	}

	/** 基础命中结算（不调流派钩子）。 */
	public static HitResult hitRaw(Entity owner, LivingEntity target, float damage, int expBounty) {
		return hitRaw(owner, target, damage, expBounty,
				github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.context(owner));
	}

	public static HitResult hitRaw(Entity owner, LivingEntity target, float damage, int expBounty,
	                               github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly) {
		var link = AllaySpellLink.context(owner);
		var court = CourtSpellLink.of(owner);
		Entity source = AllaySpellLink.owner(owner);
		if (source instanceof ServerPlayer ownerPlayer && WhitelistUtils.isProtected(ownerPlayer, target)) {
			return HitResult.PROTECTED;
		}
		boolean damaged;
		float healthBefore = target.getHealth();
		if (source instanceof LivingEntity livingOwner) {
			damaged = CourtSpellLink.damage(court, source, target,
					() -> target.hurt(SpellDamageSource.of(target.level().damageSources(), livingOwner), damage));
		} else {
			damaged = target.hurt(SpellDamageSource.of(target.level().damageSources()), damage);
		}
		if (damaged && source instanceof ServerPlayer ownerPlayer) {
			SpellExpGrant.grant(ownerPlayer, expBounty);
			if (link != null) link.damage(ownerPlayer, target, Math.max(0, healthBefore - target.getHealth()));
		}
		if (damaged) github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(anomaly, target);
		return damaged ? HitResult.HIT : HitResult.IMMUNE;
	}
}
