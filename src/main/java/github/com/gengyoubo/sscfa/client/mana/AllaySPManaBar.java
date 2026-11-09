package github.com.gengyoubo.sscfa.client.mana;

import net.minecraft.resources.ResourceLocation;

/** SP 悦灵魔力条（80×5，空/满双层贴图按百分比裁剪）?*/
public class AllaySPManaBar extends SimpleResourceBarRenderer {
	private static final ResourceLocation TEX_FULL = new ResourceLocation("my_addon", "textures/gui/allay_sp_mana_bar_full.png");
	private static final ResourceLocation TEX_EMPTY = new ResourceLocation("my_addon", "textures/gui/allay_sp_mana_bar_empty.png");
	private static final ResourceLocation RESOURCE_ID = new ResourceLocation("my_addon", "form_allay_sp_mana_resource");

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
