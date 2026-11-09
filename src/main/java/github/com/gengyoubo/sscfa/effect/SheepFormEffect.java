package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 变羊状态（召唤系法术「羊了个??Beep Sheep」，jackcooper??026-09-29 用户定稿）：
 * 中招实体被「变成一只羊」——客户端??SheepFormClientHooks 用原版羊模型替换其渲???
 * （效果经原??DataTracker 自动同步，无需自建网络）；期间实??b>失去战斗能??/b>??
 * <ul>
 *   <li>攻击伤????（属性修正??? 玩家端禁攻击/禁用物???禁破坏输入（客户?mixin）??/li>
 *   <li>服务?UseBlockCallback/AttackBlockCallback 兜底禁止放置与破坏方块（防假客户端）??/li>
 *   <li>mob ??AI 停机（isImmobile ??整体跳???tickNewAi，??STUN 拦截模式）??/li>
 * </ul>
 *
 * <p><b>三项实体属性换血???026-09-29 用户定稿???/b>——变羊期间目标临时成为「标准羊」：
 * 移速统??0.23（羊基础值，快慢都拉平）、最大生?8 心??6.0，按变羊瞬间血量百分??
 * 换算当前血）；效果结束按变羊期间血量百分比逆向还原。换血与恢复的完整状态机??
 * {@link SheepFormRestoreHandler}（按实??UUID 记录，正常到期???onRemoved 恢复?
 * 死??断??清效果等非正常路径??sweep 兜底）???/p>
 */
public class SheepFormEffect extends MobEffect {
	// 固??UUID：攻击力归零（???StunEffect 命名规范，供孤儿修正清理?UUID 精确移除??
	public static final java.util.UUID ATTACK_MODIFIER_UUID =
			java.util.UUID.fromString("A51E7C2D-9B34-4F5E-8D6A-3C0B7E1F9A24");

	public SheepFormEffect() {
		super(MobEffectCategory.HARMFUL, 0xE8D8C0);
		// 攻击伤??-100%（变羊后无法造成任何攻击伤害；击退仍存在，符合「羊乱撞」的直觉）??
		// 移??血量上限不??addAttributeModifier（需要动态系数与换血顺序控制），???Handler 管理?
		this.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
				ATTACK_MODIFIER_UUID.toString(), -1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// mob 侧「??AI」??026-09-29 用户定稿）：变羊期???AI 暂时变成羊——清仇???+
		// 受惊远离施法者（SheepFormAiController ??10t 节流寻??+ isImmobile 已改为不冻??mob）??
		if (!entity.level().isClientSide) {
			SheepFormAiController.tickSheepAi(entity);
		}
		// 变羊期间随机咩叫（演出）：约??6~9 秒一声，客户端听到的是服务端广播的实体???
		if (!entity.level().isClientSide && entity.tickCount % 140 == 0 && entity.getRandom().nextFloat() < 0.5f) {
			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT,
					net.minecraft.sounds.SoundSource.NEUTRAL, 0.9f,
					0.9f + entity.getRandom().nextFloat() * 0.2f);
		}
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);
		if (entity.level().isClientSide) return;
		SheepFormRestoreHandler.onSheepStart(entity);
		// 玩家：切空壳羊形态（技???被??魔法天然失效???026-09-29 用户定稿；mob 维持效果路线?
		if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			github.com.gengyoubo.sscfa.ability.BeepSheepFormManager.onSheepStart(serverPlayer);
		}
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);
		if (entity.level().isClientSide) return;
		SheepFormRestoreHandler.onSheepEnd(entity);
		// 玩家：切回原形???+ 回??Apoli CD 快照（CD 正常续走不重算，用户定稿?
		if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			github.com.gengyoubo.sscfa.ability.BeepSheepFormManager.onSheepEnd(serverPlayer);
		}
	}
}
