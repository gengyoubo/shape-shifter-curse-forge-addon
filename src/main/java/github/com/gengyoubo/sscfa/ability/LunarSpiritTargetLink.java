package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.LunarSpiritBoltEntity;
import github.com.gengyoubo.sscfa.entity.LunarSpiritEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.List;

/**
 * 月灵目标联动（jackcooper，服务端权威）。挂 {@code ALLOW_DAMAGE}（伤害结算前记录，return true 不拦截）?
 * <ul>
 *   <li>主人（玩家）亲手对生物造成伤害 ?该玩家场上全部月灵以最高优先级集火受害者（「你打谁月灵就打谁」）?
 *       月灵光弹的伤害源是光弹本体，明确排除，避免多只月灵每次命中都互相抢目标；</li>
 *   <li>主人被生物攻??没有更高优先级目标的月灵反击攻击者（护主）?/li>
 * </ul>
 * 合法性（白名?/ 同主人月?/ 创造玩?/ 14 格内）由 {@link LunarSpiritEntity#offerTarget} 统一校验?
 * 月灵始终在主?3 格内，故只需在主人周?8 格找月灵?
 */
public final class LunarSpiritTargetLink {
	private LunarSpiritTargetLink() {
	}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((victim, source, amount) -> {
			if (!(victim.level() instanceof ServerLevel serverWorld)) {
				return true;
			}
			Entity attackerEntity = source.getEntity();
			// 情形一：主人亲手造成伤害（近?/ ?/ 法术，attacker 都是玩家）→ 集火；光弹命中排?
			if (attackerEntity instanceof ServerPlayer player
					&& !(source.getDirectEntity() instanceof LunarSpiritBoltEntity)) {
				if (victim instanceof LunarSpiritEntity spirit && player.getUUID().equals(spirit.getOwnerUuid())) {
					return true; // 主人误击自己的月灵不?
				}
				for (LunarSpiritEntity spirit : spiritsOf(serverWorld, player)) {
					spirit.offerTarget(victim, LunarSpiritEntity.TIER_OWNER_ATTACKED);
				}
				return true;
			}
			// 情形二：主人被攻??月灵护主反击
			if (victim instanceof ServerPlayer owner
					&& attackerEntity instanceof LivingEntity attacker && attacker != owner) {
				for (LunarSpiritEntity spirit : spiritsOf(serverWorld, owner)) {
					spirit.offerTarget(attacker, LunarSpiritEntity.TIER_DEFENSE);
				}
			}
			return true; // 不拦截伤害本?
		});
	}

	private static List<LunarSpiritEntity> spiritsOf(ServerLevel serverWorld, Player owner) {
		return serverWorld.getEntitiesOfClass(LunarSpiritEntity.class,
				owner.getBoundingBox().inflate(8.0),
				spirit -> owner.getUUID().equals(spirit.getOwnerUuid()));
	}
}
