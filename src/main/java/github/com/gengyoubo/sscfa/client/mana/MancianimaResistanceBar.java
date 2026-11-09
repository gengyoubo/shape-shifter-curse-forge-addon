package github.com.gengyoubo.sscfa.client.mana;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.ManaBarPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;

/**
 * 契灵 - 抗伤值条 HUD?
 * 设计?
 *  - ?mana 条中心对齐（mana 条宽?80）?
 *  - 长度 = FULL_WIDTH * current/max（线性），current=0 时不显示?
 *  - 位置紧贴 mana 条上?2 像素?
 *  - 仅在契灵形态显示（max>1 即代表持有该 power）?
 */
public class MancianimaResistanceBar extends SimpleResourceBarRenderer {
	private static final ResourceLocation BAR_FULL = new ResourceLocation("my_addon", "textures/gui/mancianima_resistance_full.png");
	private static final ResourceLocation BAR_EMPTY = new ResourceLocation("my_addon", "textures/gui/mancianima_resistance_empty.png");
	private static final int MANA_WIDTH = 80;
	private static final int FULL_WIDTH = 92;
	private static final int CENTER_OFFSET_X = -(FULL_WIDTH - MANA_WIDTH) / 2; // -6，在 mana 条上居中

	@Override
	protected ResourceLocation resourceId() {
		return FormIdentifiers.MANCIANIMA_RESISTANCE;
	}

	@Override
	protected ResourceLocation texFull() {
		return BAR_FULL;
	}

	@Override
	protected ResourceLocation texEmpty() {
		return BAR_EMPTY;
	}

	@Override
	protected int width() {
		return FULL_WIDTH;
	}

	/** ?mana 条同 y（复?offsetY=0 的布局参数），X 额外做居中偏移?*/
	@Override
	protected int[] manaBarPosArgs() {
		return ManaBarPos.get(8, 100, 0);
	}

	@Override
	protected int barX(Tuple<Integer, Integer> pos) {
		return pos.getA() + CENTER_OFFSET_X;
	}
}
