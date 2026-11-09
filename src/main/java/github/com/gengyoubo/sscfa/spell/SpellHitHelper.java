package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

/**
 * 法术命中结算辅助：白名单豁免 → 法术伤害 → 异常通知。
 * 形态链接（悦灵/王庭）与经验发放待能力层移植后补回。
 */
public final class SpellHitHelper {

	private SpellHitHelper() {}

	public enum HitResult { HIT, PROTECTED, IMMUNE }

	public static HitResult projectileHit(Entity owner, LivingEntity target, float damage,
	                                      FormationElement element, UUID refundCastId, int expBounty) {
		return projectileHit(owner, target, damage, element, refundCastId, expBounty,
                github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.context(owner));
    }

    public static HitResult projectileHit(Entity owner, LivingEntity target, float damage,
            FormationElement element, UUID refundCastId, int expBounty,
            github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly) {
		return hitRaw(owner, target, damage, expBounty, anomaly);
	}

	public static HitResult hitRaw(Entity owner, LivingEntity target, float damage, int expBounty) {
        return hitRaw(owner, target, damage, expBounty,
                github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.context(owner));
    }

    public static HitResult hitRaw(Entity owner, LivingEntity target, float damage, int expBounty,
            github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly) {
		if (owner instanceof ServerPlayer ownerPlayer && WhitelistUtils.isProtected(ownerPlayer, target)) {
			return HitResult.PROTECTED;
		}
		boolean damaged;
		if (owner instanceof LivingEntity livingOwner) {
			damaged = target.hurt(SpellDamageSource.of(target.level().damageSources(), livingOwner), damage);
		} else {
			damaged = target.hurt(SpellDamageSource.of(target.level().damageSources()), damage);
		}
		if (damaged) github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(anomaly, target);
		return damaged ? HitResult.HIT : HitResult.IMMUNE;
	}
}
