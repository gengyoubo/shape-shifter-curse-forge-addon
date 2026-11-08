package github.com.gengyoubo.sscfa.client.screen;

import github.com.gengyoubo.sscfa.screen.PotionStorageBoxScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 药品存储箱界面（jackcooper）：8 个存储槽（单排）+ 玩家背包，背景为正式 GUI 贴图
 * （textures/gui/container/potion_storage_box.png，176x133，与 ScreenHandler 槽位坐标逐像素对齐）。
 */
public class PotionStorageBoxScreen extends AbstractContainerScreen<PotionStorageBoxScreenHandler> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("ssc_addon", "textures/gui/container/potion_storage_box.png");

	public PotionStorageBoxScreen(PotionStorageBoxScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 133;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		// 直接绘制正式 GUI 贴图（必须用 9 参版显式传贴图真实尺寸 176x133；6 参版默认 256x256 会导致 UV 错位）
		context.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 176, 133);
	}

	@Override
	protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
		context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
		context.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		this.renderTooltip(context, mouseX, mouseY);
	}
}
