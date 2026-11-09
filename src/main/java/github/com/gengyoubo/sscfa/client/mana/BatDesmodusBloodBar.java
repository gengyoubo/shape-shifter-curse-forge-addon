package github.com.gengyoubo.sscfa.client.mana;

import net.minecraft.resources.ResourceLocation;

/**
 * 吸血蝙蝠形?- 雾血资源?HUD?
 * 仅在玩家拥有雾血资源（即处于吸血蝙蝠形态）时渲染，复用暗色调灵魂条贴图契合吸血主题?
 * 位置复用魔力条配置，吸血蝙蝠形态无魔力条，两者不冲突?
 */
public class BatDesmodusBloodBar extends SimpleResourceBarRenderer {
	private static final ResourceLocation TEX_FULL = new ResourceLocation("my_addon", "textures/gui/bat_desmodus_blood_bar_full.png");
	private static final ResourceLocation TEX_EMPTY = new ResourceLocation("my_addon", "textures/gui/bat_desmodus_blood_bar_empty.png");
	private static final ResourceLocation RESOURCE_ID = new ResourceLocation("my_addon", "form_bat_desmodus_blood_resource");

	@Override
	protected ResourceLocation resourceId() {
		return RESOURCE_ID;
	}

	@Override
	protected ResourceLocation texFull() {
		return TEX_FULL;
	}

	@Override
	protected ResourceLocation texEmpty() {
		return TEX_EMPTY;
	}
}
