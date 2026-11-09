package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 侵蚀烙印标记效果 - 无实际机制效???
 * 仅用于将烙印状态同步到客户端，以驱???entity_glow 条件检测??
 * 拆分?个独立效果以支持不同层数显示不同图标???
 * - erosion_brand_marker_1: 1閻?濮掓稑瀚竟?
 * - erosion_brand_marker_2: 2閻?婵鐟ㄦ竟?
 * - erosion_brand_marker_3: 3??红??
 * 绿色冷却状态不显示图标?
 */
public class ErosionBrandMarkerEffect extends MobEffect {

	public ErosionBrandMarkerEffect(int color) {
		super(MobEffectCategory.NEUTRAL, color);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return false; // 无任何tick效??
	}
}
