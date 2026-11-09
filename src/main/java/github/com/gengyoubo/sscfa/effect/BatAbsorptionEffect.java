/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 寄生果蝠主技能专属「伤害吸收」buff（纯显示）??
 * 黄心预算和绝对结束时间随目标存档，??ParasiticAbsorptionMixin 到期撤销；本效果提供状态栏图标与名字??
 * 图??/ 名字复用原版伤害吸收???
 */
public class BatAbsorptionEffect extends MobEffect {
    public BatAbsorptionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2552A5);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
