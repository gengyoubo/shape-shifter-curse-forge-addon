package github.com.gengyoubo.sscfa.client.mana;

import net.minecraft.resources.ResourceLocation;

/** 阿努比斯之狼 SP 灵魂能量条（80×5）?*/
public class AnubisWolfSPSoulBar extends SimpleResourceBarRenderer {
	private static final ResourceLocation TEX_FULL = new ResourceLocation("my_addon", "textures/gui/anubis_wolf_sp_soul_bar_full.png");
	private static final ResourceLocation TEX_EMPTY = new ResourceLocation("my_addon", "textures/gui/anubis_wolf_sp_soul_bar_empty.png");
	private static final ResourceLocation RESOURCE_ID = new ResourceLocation("my_addon", "form_anubis_wolf_sp_soul_energy");

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
