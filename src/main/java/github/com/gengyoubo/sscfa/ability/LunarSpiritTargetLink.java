package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.LunarSpiritBoltEntity;
import github.com.gengyoubo.sscfa.entity.LunarSpiritEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

/**
 * 月灵目标联动（jackcooper，服务端权威）。挂 {@code LivingHurtEvent}（伤害结算前记录，不取消事件）：
 * <ul>
 *   <li>主人（玩家）亲手对生物造成伤害 → 该玩家场上全部月灵以最高优先级集火受害者（「你打谁月灵就打谁」）。
 *       月灵光弹的伤害源是光弹本体，明确排除，避免多只月灵每次命中都互相抢目标；</li>
 *   <li>主人被生物攻击 → 没有更高优先级目标的月灵反击攻击者（护主）。</li>
 * </ul>
 * 合法性（白名单 / 同主人月灵 / 创造玩家 / 14 格内）由 {@link LunarSpiritEntity#offerTarget} 统一校验。
 * 月灵始终在主人 3 格内，故只需在主人周围 8 格找月灵。
 */
public final class LunarSpiritTargetLink {
	private LunarSpiritTargetLink() {
	}

	/** 在模组初始化阶段调用一次（FMLCommonSetupEvent / 模组构造器）。 */
	public static void init() {
		MinecraftForge.EVENT_BUS.addListener(LunarSpiritTargetLink::onLivingHurt);
	}

	private static void onLivingHurt(LivingHurtEvent event) {
		LivingEntity victim = event.getEntity();
		if (!(victim.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		Entity attackerEntity = event.getSource().getEntity();
		// 情形一：主人亲手造成伤害（近战 / 箭 / 法术，attacker 都是玩家）→ 集火；光弹命中排除
		if (attackerEntity instanceof ServerPlayer player
				&& !(event.getSource().getDirectEntity() instanceof LunarSpiritBoltEntity)) {
			if (victim instanceof LunarSpiritEntity spirit && player.getUUID().equals(spirit.getOwnerUuid())) {
				return; // 主人误击自己的月灵不算
			}
			for (LunarSpiritEntity spirit : spiritsOf(serverWorld, player)) {
				spirit.offerTarget(victim, LunarSpiritEntity.TIER_OWNER_ATTACKED);
			}
			return;
		}
		// 情形二：主人被攻击 → 月灵护主反击
		if (victim instanceof ServerPlayer owner
				&& attackerEntity instanceof LivingEntity attacker && attacker != owner) {
			for (LunarSpiritEntity spirit : spiritsOf(serverWorld, owner)) {
				spirit.offerTarget(attacker, LunarSpiritEntity.TIER_DEFENSE);
			}
		}
	}

	private static List<LunarSpiritEntity> spiritsOf(ServerLevel serverWorld, Player owner) {
		return serverWorld.getEntitiesOfClass(LunarSpiritEntity.class,
				owner.getBoundingBox().inflate(8.0),
				spirit -> owner.getUUID().equals(spirit.getOwnerUuid()));
	}
}
