package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.ability.AllaySPGroupHeal;
import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import java.util.ArrayList;
import java.util.List;

/**
 * 净化标记效??- SP悦灵净化技能施加的短暂标???
 * 用于通知Java端技能（如冰霜风暴蓄力、闪现攻击）被打??
 * 效果本身无任何属性修改，仅作为信号使?
 * 持续时间极短???秒），仅用于在下一个tick被Java代码检测???
 */
public class PurifiedEffect extends MobEffect {

	public PurifiedEffect() {
		super(MobEffectCategory.NEUTRAL, 0x99DDFF); // Light blue color
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);
		// Interrupt immediately, before a last-tick release or its cooldown can win the tick order.
		interruptCasting(entity);
	}

	private static void interruptCasting(LivingEntity entity) {
		if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) return;
		SpellChannelManager.onPurified(player);
		VortexChargeManager.onPurified(player);
		AllaySPGroupHeal.onPurified(player);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide) return;
		interruptCasting(entity);

		// Clear all harmful effects (or all effects except this one)
		// Since we are iterating while modifying, we need a copy
		List<MobEffectInstance> effects = new ArrayList<>(entity.getActiveEffects());

		for (MobEffectInstance instance : effects) {
			// Don't remove self
			if (instance.getEffect() == this) continue;

			// Remove the effect
			// Note: removeStatusEffect returns boolean, doesn't throw concurrent modification if we iterate over a copy
			entity.removeEffect(instance.getEffect());
		}

		// 净化同样清除“感染孢子”（由自定义管理器维护，非原版状态效果），并驱散身边的滞留毒雾云
		if (entity.level() instanceof ServerLevel sw) {
			InfectionSporeManager.cureInfection(entity.getUUID());
			InfectionSporeManager.dissipateCloudsNear(sw, entity.position(), InfectionSporeManager.cloudPurifyReach());
		}
	}
}
