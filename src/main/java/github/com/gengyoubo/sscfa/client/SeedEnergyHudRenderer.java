/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.client.mana.SimpleResourceBarRenderer;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.resources.ResourceLocation;

/**
 * 寄生果蝠形态种子量能量?HUD 渲染?
 * 复用主模组魔力条 UI 位置配置，与雪狐 / 悦灵 / 吸血蝙蝠等能量条对齐到同一高度?
 * 该形态无魔力条，位置不与其他能量条冲突?
 */
public final class SeedEnergyHudRenderer extends SimpleResourceBarRenderer {
    private static final ResourceLocation TEX_EMPTY = new ResourceLocation("my_addon", "textures/gui/bat_parasitic_fruit_seed_bar_empty.png");
    private static final ResourceLocation TEX_FULL = new ResourceLocation("my_addon", "textures/gui/bat_parasitic_fruit_seed_bar_full.png");
    /** 原图尺寸?6 像素?× 5 像素高，10 个圆点等距分布?*/
    private static final int TEX_WIDTH = 86;

    public static void register() {
        github.com.gengyoubo.sscfa.client.mana.SimpleResourceBarRenderer.register(new SeedEnergyHudRenderer());
    }

    @Override
    protected ResourceLocation resourceId() {
        return FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY;
    }

    @Override
    protected ResourceLocation texFull() {
        return TEX_FULL;
    }

    @Override
    protected ResourceLocation texEmpty() {
        return TEX_EMPTY;
    }

    @Override
    protected int width() {
        return TEX_WIDTH;
    }

    /** 必须真正拥有?power 才渲染，避免对其他形?未变形玩家误显示?*/
    @Override
    protected boolean requirePower() {
        return true;
    }
}
