package github.com.gengyoubo.sscfa.effect;

import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 定身（ROOTED）效果：荧光幼灵法阵激光蓄???释放期使用??
 *
 * <p>??STUN 的区别：ROOTED <b>只锁移动</b>??100% 移??+ ??{@code StunnedKeyBindingMixin}
 * 屏蔽移??跳跃键）??b>不锁视???/b>（不触??STUN 的相?鼠标锁）。释放期的视角限速??
 * 专门?{@code ViewRateLimitMixin} ??laser_state 资源单独处理?
 */
public class RootedEffect extends MobEffect {
	/** 固??UUID，供孤儿清理??UUID 精确移除??*/
	public static final UUID SPEED_MODIFIER_UUID = UUID.fromString("3F2A6C11-9D74-4B8E-A1C3-77E5D2B0A9F1");

	public RootedEffect() {
		super(MobEffectCategory.NEUTRAL, 0x33CCFF);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
				SPEED_MODIFIER_UUID.toString(), -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return false;
	}
}
