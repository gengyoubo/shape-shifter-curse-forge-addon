package github.com.gengyoubo.sscfa.client.mana;

import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;

/** SP 雪狐霜寒条（80×5）：按近?远程切换状态选用两套贴图?*/
public class SnowFoxSPManaBar extends SimpleResourceBarRenderer {
	// 近战形态纹?
	private static final ResourceLocation MELEE_FULL = new ResourceLocation("my_addon", "textures/gui/sp_snow_fox_mana_bar_melee_full.png");
	private static final ResourceLocation MELEE_EMPTY = new ResourceLocation("my_addon", "textures/gui/sp_snow_fox_mana_bar_melee_empty.png");
	// 远程形态纹?
	private static final ResourceLocation RANGED_FULL = new ResourceLocation("my_addon", "textures/gui/sp_snow_fox_mana_bar_ranged_full.png");
	private static final ResourceLocation RANGED_EMPTY = new ResourceLocation("my_addon", "textures/gui/sp_snow_fox_mana_bar_ranged_empty.png");

	private static final ResourceLocation RESOURCE_ID = new ResourceLocation("my_addon", "form_snow_fox_sp_resource");
	// 近战/远程切换状态：0=近战?=远程
	private static final ResourceLocation SWITCH_STATE_ID = new ResourceLocation("my_addon", "form_snow_fox_sp_switch_state");

	@Override
	protected ResourceLocation resourceId() {
		return RESOURCE_ID;
	}

	@Override
	protected ResourceLocation texFull() {
		return isRanged() ? RANGED_FULL : MELEE_FULL;
	}

	@Override
	protected ResourceLocation texEmpty() {
		return isRanged() ? RANGED_EMPTY : MELEE_EMPTY;
	}

	private static boolean isRanged() {
		return PowerUtils.getClientResourceValue(
				net.minecraft.client.Minecraft.getInstance().player, SWITCH_STATE_ID) == 1;
	}
}
