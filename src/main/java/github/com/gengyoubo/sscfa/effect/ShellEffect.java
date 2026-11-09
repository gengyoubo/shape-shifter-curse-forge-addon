package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 迁移占位效果：保留注??ID、分类、颜色和固定属性修饰，行为待依赖系统（能力、平衡、网络等）迁移后替换为原类? */
public class ShellEffect extends MobEffect {
	public ShellEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	public ShellEffect modifier(Attribute attribute, String uuid, double amount, AttributeModifier.Operation operation) {
		addAttributeModifier(attribute, uuid, amount, operation);
		return this;
	}

	/** 瞬时效果占位（原版药水实体可正常命中，但不产生作用）?*/
	public static class Instant extends ShellEffect {
		public Instant(MobEffectCategory category, int color) {
			super(category, color);
		}

		@Override
		public boolean isInstantenous() {
			return true;
		}

		@Override
		public boolean isDurationEffectTick(int duration, int amplifier) {
			return false;
		}
	}
}
