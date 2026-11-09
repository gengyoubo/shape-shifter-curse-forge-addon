package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 独立计时。清理由 LivingEntity 的实际移除钩子负责，避免刷新效果时的 onRemoved 重算误清护盾。 */
public final class FrostArmorEffect extends MobEffect {
    public FrostArmorEffect() { super(MobEffectCategory.BENEFICIAL, 0x80d8ff); }
}
