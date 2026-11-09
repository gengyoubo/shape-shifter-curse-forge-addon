package github.com.gengyoubo.sscfa.client.screen;

import github.com.gengyoubo.sscfa.screen.SpellbookScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 月尘魔法书配置界面（jackcooper）。书页与玩家信息侧栏使用独立背景贴图，
 * 玩家施法数据的初始快照由 {@link SpellbookScreenHandler} 从服务端携带。
 */
public class SpellbookScreen extends AbstractContainerScreen<SpellbookScreenHandler> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("ssc_addon", "textures/gui/moon_dust_spellbook.png");
	private static final ResourceLocation PLAYER_INFORMATION = new ResourceLocation("ssc_addon", "textures/gui/spellbook_player_information.png");
	private static final ResourceLocation SLOT_CELL = new ResourceLocation("ssc_addon", "textures/gui/spellbook_slot.png");
	private static final int BOOK_WIDTH=176, SIDE_X=182, SIDE_WIDTH=136;

	public SpellbookScreen(SpellbookScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = SIDE_X + SIDE_WIDTH;
		this.imageHeight = 166;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		context.blit(TEXTURE, x, y, 0, 0, BOOK_WIDTH, this.imageHeight, 256, 256);
		// 独立侧栏贴图沿用主书页的边框像素，按原始尺寸绘制，避免描边被拉伸。
		int sx=x+SIDE_X;
		context.blit(PLAYER_INFORMATION, sx, y, 0, 0, SIDE_WIDTH, this.imageHeight,
				SIDE_WIDTH, this.imageHeight);
		// 动态卷轴槽凹槽（数量随书等级 3/5/7，居中；用 18×18 格子材质，背包槽已在贴图内）
		for (int i = 0; i < this.menu.slotCount; i++) {
			Slot slot = this.menu.slots.get(i);
			context.blit(SLOT_CELL, x + slot.x - 1, y + slot.y - 1, 0, 0, 18, 18, 18, 18);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
		// 标题改纯黑：背景贴图为浅色书页，浅色文字看不清
		context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x000000, false);
        Component info = Component.translatable("text.ssc_addon.book.contents",this.menu.bookLevel,this.menu.slotCount);
		context.drawString(this.font, info, 8, 20, 0x404040, false);
		context.drawString(this.font, this.playerInventoryTitle,
				this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
		drawPlayerInformation(context);
	}

	private void drawPlayerInformation(GuiGraphics context) {
		var live=github.com.gengyoubo.sscfa.neomagic.client.NeoClient.hud;
		var state=live.contains("S")&&live.getDouble("S")>0?live:this.menu.playerState;
		int experience=Math.max(0,state.getInt("MasteryExpTen"));
		int level=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.tier(experience);
		int progress=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.progressTen(experience);
		int required=github.com.gengyoubo.sscfa.spell.PlayerSpellMastery.thresholdTen(level);
		double safety=Math.max(0,state.getDouble("S"));
		row(context,9,"title",0x303030);
		row(context,25,"level",0x303030,level);
		row(context,39,"experience",0x404040,number(progress/10.0),number(required/10.0));
		context.fill(SIDE_X+8,52,SIDE_X+128,58,0xFF373737);
		context.fill(SIDE_X+9,53,SIDE_X+9+(int)(118.0*progress/required),57,0xFF609637);
		row(context,68,"safe",0x235A40,number(safety));
		row(context,82,"danger",0x854C1D,number(safety),number(safety*2));
		row(context,96,"critical",0x832D2D,number(safety*2));
		row(context,110,"current",0x303030,number(state.getDouble("C")));
		row(context,128,"growth",0x404040);
		row(context,142,"recovery",0x404040,number((state.contains("RecoveryDelay")?state.getInt("RecoveryDelay"):120)/20.0));
		row(context,154,"ownership",0x555555);
	}
	private static String number(double value){
		if(value>=1e6)return String.format(java.util.Locale.ROOT,"%.1fm",value/1e6);
		if(value>=1e3)return String.format(java.util.Locale.ROOT,"%.1fk",value/1e3);
		return value==Math.rint(value)?Long.toString((long)value):String.format(java.util.Locale.ROOT,"%.1f",value);
	}
	private void row(GuiGraphics context,int y,String key,int color,Object... arguments){
		Component text=Component.translatable("text.ssc_addon.book.player."+key,arguments);
		float scale=Math.min(1,120f/Math.max(1,font.width(text)));
		context.pose().pushPose();context.pose().translate(SIDE_X+8,y,0);context.pose().scale(scale,scale,1);
		context.drawString(font,text,0,0,color,false);context.pose().popPose();
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		this.renderTooltip(context, mouseX, mouseY);
	}
}
