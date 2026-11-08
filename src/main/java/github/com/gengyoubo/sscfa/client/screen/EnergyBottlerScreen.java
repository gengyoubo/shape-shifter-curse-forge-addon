package github.com.gengyoubo.sscfa.client.screen;

import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.screen.EnergyBottlerScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 能量装瓶器界面（jackcooper）。
 * <p>纯 fill 自绘占位（不依赖容器贴图）：竖直能量条 + 三行「空瓶输入 → 进度箭头 → 能量瓶输出」+ 手动/自动按钮。
 * 后续替换正式贴图时只需把 {@link #renderBg} 改为 drawTexture，服务端逻辑不受影响。
 */
public class EnergyBottlerScreen extends AbstractContainerScreen<EnergyBottlerScreenHandler> {

	/** 背景图资源（176x184：含边框/面板/全部槽位，坐标与槽位一一对应）。 */
	private static final ResourceLocation BACKGROUND = new ResourceLocation("ssc_addon", "textures/gui/container/energy_bottler.png");

	private static final int ENERGY_BAR_X = 8;
	private static final int ENERGY_BAR_Y = 17;
	private static final int ENERGY_BAR_W = 14;
	private static final int ENERGY_BAR_H = 71;

	private Button modeButton;
	private Button craftButton;

	public EnergyBottlerScreen(EnergyBottlerScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 184;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		// 按钮放到输出槽右侧的空白区（x+128 起，避免压住 x108 的输出槽与进度条）
		modeButton = Button.builder(Component.empty(), b -> clickButton(EnergyBottlerScreenHandler.BUTTON_TOGGLE_MODE))
				.bounds(this.leftPos + 128, this.topPos + 20, 42, 18).build();
		craftButton = Button.builder(Component.translatable("gui.ssc_addon.energy_bottler.craft"),
						b -> clickButton(EnergyBottlerScreenHandler.BUTTON_MANUAL_CRAFT))
				.bounds(this.leftPos + 128, this.topPos + 44, 42, 18).build();
		this.addRenderableWidget(modeButton);
		this.addRenderableWidget(craftButton);
	}

	private void clickButton(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
		}
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		// 整张背景图（176x184 容器图：含边框/面板/全部槽位），与槽位坐标一一对应
		// 背景图整体往左上偏移 1px（用户校准）；槽位 Y 也已上移 1px（ScreenHandler 17+i*24）
		context.blit(BACKGROUND, x - 1, y - 1, 0, 0, this.imageWidth, this.imageHeight, 176, 184);
		// 三行进度箭头（输入 → 输出）
		for (int i = 0; i < EnergyBottlerBlockEntity.LINES; i++) {
			int ay = y + 22 + i * 24;
			int ax = x + 70;
			int aw = 36;
			context.fill(ax, ay, ax + aw, ay + 10, 0xFF3B3B3B);
			int prog = this.menu.getProgress(i);
			int filled = prog <= 0 ? 0 : Math.max(1, aw * prog / EnergyBottlerBlockEntity.CRAFT_TIME);
			context.fill(ax, ay, ax + filled, ay + 10, 0xFFFFC24B);
		}
		// 竖直能量条（自底向上填充）
		int bx = x + 8;
		int by = y + 17;
		context.fill(bx - 1, by - 1, bx + 15, by + 72, 0xFF2B2B2B);
		context.fill(bx, by, bx + 14, by + 71, 0xFF101820);
		int cap = Math.max(1, this.menu.getCapacity());
		int energy = Math.min(cap, this.menu.getEnergy());
		int fh = ENERGY_BAR_H * energy / cap;
		if (fh > 0) {
			context.fill(bx, by + ENERGY_BAR_H - fh, bx + ENERGY_BAR_W, by + ENERGY_BAR_H, 0xFF35D6FF);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
		context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
		context.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		boolean auto = this.menu.isAutoMode();
		modeButton.setMessage(Component.translatable(auto
				? "gui.ssc_addon.energy_bottler.mode.auto"
				: "gui.ssc_addon.energy_bottler.mode.manual"));
		craftButton.visible = !auto;
		super.render(context, mouseX, mouseY, delta);
		// 能量条悬停显示数值
		int bx = this.leftPos + ENERGY_BAR_X;
		int by = this.topPos + ENERGY_BAR_Y;
		if (mouseX >= bx && mouseX < bx + ENERGY_BAR_W && mouseY >= by && mouseY < by + ENERGY_BAR_H) {
			context.renderTooltip(this.font,
					Component.translatable("gui.ssc_addon.energy_bottler.energy", this.menu.getEnergy(), this.menu.getCapacity()),
					mouseX, mouseY);
		}
		this.renderTooltip(context, mouseX, mouseY);
	}
}
