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
 * 朔望「九命」剩余命?HUD 渲染? 格圆点条，仿寄生果蝠种子条分层贴图）?
 * 素材由种子条裁剪得到。横纵坐标均复用主模组魔力条布局，支持锚点和 X/Y 偏移?
 */
public final class NineLivesHudRenderer extends SimpleResourceBarRenderer {
    private static final ResourceLocation TEX_EMPTY = new ResourceLocation("my_addon", "textures/gui/form_ocelot_nova_nine_lives_empty.png");
    private static final ResourceLocation TEX_FULL = new ResourceLocation("my_addon", "textures/gui/form_ocelot_nova_nine_lives_full.png");
    /** 命数条尺寸：76 像素?× 5 像素高，与实际贴图一致?*/
    private static final int TEX_WIDTH = 76;

    public static void register() {
        github.com.gengyoubo.sscfa.client.mana.SimpleResourceBarRenderer.register(new NineLivesHudRenderer());
    }

    @Override
    protected ResourceLocation resourceId() {
        return FormIdentifiers.OCELOT_NOVA_NINE_LIVES;
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

    /** 必须真正拥有?power 才渲染?*/
    @Override
    protected boolean requirePower() {
        return true;
    }

}
