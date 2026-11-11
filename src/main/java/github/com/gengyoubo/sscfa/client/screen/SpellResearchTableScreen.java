package github.com.gengyoubo.sscfa.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.client.tutorial.SpellTutorialClient;
import github.com.gengyoubo.sscfa.compat.RegCustomItem;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.spell.research.SlottedSpellRecipes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpellResearchTableScreen extends AbstractContainerScreen<SpellResearchTableScreenHandler> {

	private static final ResourceLocation BACKGROUND = new ResourceLocation("ssc_addon", "textures/gui/spell_research_background.png");
	private static final ResourceLocation ANALYSIS_PROGRESS = new ResourceLocation("ssc_addon", "textures/gui/formation_research/analysis_progress.png");
	private static final ResourceLocation SLOT_CELL = new ResourceLocation("ssc_addon", "textures/gui/spellbook_slot.png");
	private static final ResourceLocation LIST_PANEL = new ResourceLocation("ssc_addon", "textures/gui/magic_slot_panel.png");
	private static final ResourceLocation FRAME = new ResourceLocation("ssc_addon", "textures/gui/formation_frame.png");
	private static final ResourceLocation SCROLL_THUMB = new ResourceLocation("ssc_addon", "textures/gui/scroll_thumb.png");
	private static final int TAB_HEIGHT = 28;
	private static final int PANEL_X = 8, PANEL_Y = 42, PANEL_W = 131, PANEL_H = 72;
	private static final int CELL = 22, COLUMNS = 5, VISIBLE_ROWS = 3;
	private static final int TRACK_X = 117, THUMB_W = 8, THUMB_H = 13;
	private static final int THUMB_Y_MIN = 6, THUMB_Y_MAX = 53;
	private static final int DETAIL_X = 152, DETAIL_W = 152;

	private Button projectionButton;
	private EditBox searchField;
	private Button levelDownButton;
	private Button levelUpButton;
	private Button formationButton;
	private List<Entry> entries = List.of();
	private int tab;
	private int scroll;
	private int selected = -1;
	private int referenceLevel = 1;
	private int thumbY = THUMB_Y_MIN;
	private boolean draggingThumb;
	private boolean researchHandoff;

	private record Entry(String id, String variant, int level, Spell spell) {
		ItemStack stack(int referenceLevel) {
			return spell == null ? FormationData.create(FormationElement.byId(id), level, variant)
					: ScrollData.create(id, referenceLevel);
		}

		FormationElement element() {
			return spell == null ? FormationElement.byId(id) : spell.getElement();
		}

		Component name() {
			return spell == null ? stack(1).getHoverName() : Component.translatable(spell.getNameKey());
		}
	}

	public SpellResearchTableScreen(SpellResearchTableScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = SpellResearchTableScreenHandler.GUI_WIDTH;
		this.imageHeight = SpellResearchTableScreenHandler.GUI_HEIGHT;
		this.inventoryLabelX = SpellResearchTableScreenHandler.INVENTORY_X;
		this.inventoryLabelY = 118;
	}

	@Override
	protected void init() {
		this.researchHandoff = false;
		String query = this.searchField == null ? "" : this.searchField.getValue();
		super.init();
		this.topPos = (this.height - this.imageHeight - TAB_HEIGHT) / 2 + TAB_HEIGHT;
		this.menu.setActivePage(this.tab);
		if (this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.tab);
		this.draggingThumb = false;
		for (int index = 0; index < ResearchTableTabs.COUNT; index++) {
			final int targetTab = index;
			this.addRenderableWidget(ResearchTableTabs.create(this.leftPos, this.topPos, index, () -> this.tab == targetTab,
					() -> selectTab(targetTab)));
		}
		this.addRenderableWidget(ResearchTableTabs.tutorial(this.leftPos, this.topPos,
				SpellTutorialClient::replay));
		this.searchField = this.addRenderableWidget(new EditBox(this.font,
				this.leftPos + PANEL_X, this.topPos + 23, PANEL_W, 16, label("search")));
		this.searchField.setMaxLength(64);
		this.searchField.setHint(label("search"));
		this.searchField.setValue(query);
		this.searchField.setResponder(value -> {
			this.scroll = 0;
			refreshEntries();
		});
		this.levelDownButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> adjustLevel(-1))
				.bounds(this.leftPos + 270, this.topPos + 36, 16, 14).build());
		this.levelUpButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustLevel(1))
				.bounds(this.leftPos + 288, this.topPos + 36, 16, 14).build());
		this.levelDownButton.setTooltip(Tooltip.create(label("level_down")));
		this.levelUpButton.setTooltip(Tooltip.create(label("level_up")));
		this.formationButton = this.addRenderableWidget(Button.builder(label("scribe"), button -> onFormationAction())
				.bounds(this.leftPos + DETAIL_X, this.topPos + 94, DETAIL_W, 20).build());
		this.projectionButton = this.addRenderableWidget(Button.builder(label("project_spell"), button -> projectSelected())
				.bounds(this.leftPos + DETAIL_X, this.topPos + 94, DETAIL_W, 20).build());
		refreshEntries();
		refreshWidgets();
	}

	public void openResearchTab(int selectedTab) {
		this.minecraft.setScreen(this);
		selectTab(selectedTab);
	}

	@Override
	public void removed() {
		if (!this.researchHandoff) super.removed();
	}

	private static Component label(String suffix, Object... args) {
		return Component.translatable("gui.ssc_addon.research." + suffix, args);
	}

	private void selectTab(int nextTab) {
		if (nextTab == ResearchTableTabs.RESEARCH) {
			this.researchHandoff = true;
			this.minecraft.setScreen(new SlottedFormationScreen(this.menu, this.minecraft.player.getInventory(), this));
			return;
		}
		if (this.tab == nextTab) {
			return;
		}
		this.tab = nextTab;
		this.menu.setActivePage(nextTab);
		if (this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, nextTab);
		this.entries = List.of();
		this.selected = -1;
		this.scroll = 0;
		this.draggingThumb = false;
		this.searchField.setValue("");
		this.searchField.setFocused(false);
		refreshEntries();
		refreshWidgets();
	}

	private void adjustLevel(int delta) {
		this.referenceLevel = Math.max(1, Math.min(ScrollData.MAX_SPELL_LEVEL, this.referenceLevel + delta));
		refreshWidgets();
	}

	private Entry selectedEntry() {
		return this.selected >= 0 && this.selected < this.entries.size() ? this.entries.get(this.selected) : null;
	}

	private void refreshEntries() {
		Entry previous = selectedEntry();
		List<Entry> next = new ArrayList<>();
		if (this.minecraft != null && this.minecraft.player != null) {
			FormationKnowledgeComponent knowledge = FormationKnowledgeComponent.get(this.minecraft.player);
			if (this.tab == 2) {
				for (Spell spell : SpellRegistry.all()) {
					if (SlottedSpellRecipes.get(spell.getId().getPath()) != null && knowledge.hasSpell(spell.getId().getPath())) {
						next.add(new Entry(spell.getId().getPath(), null, 0, spell));
					}
				}
			} else if (this.tab == ResearchTableTabs.SCRIBE || this.tab == ResearchTableTabs.LEARN) {
				for (FormationElement element : FormationElement.values()) {
					String[] variants = element == FormationElement.UNIVERSAL ? new String[]{FormationData.VARIANT_REGEN, FormationData.VARIANT_MANA, FormationData.VARIANT_EXP, FormationData.VARIANT_RECOVERY} : new String[]{null};
					for (String variant : variants) for (int level = 1; level <= FormationData.MAX_FORMATION_LEVEL; level++) {
						boolean learned = knowledge.hasLearned(element, variant, level);
						if (this.tab == ResearchTableTabs.SCRIBE ? learned : !learned && knowledge.hasRecorded(element, variant, level))
							next.add(new Entry(element.id, variant, level, null));
					}
				}
			}
		}
		String query = this.searchField == null ? "" : this.searchField.getValue().strip().toLowerCase(Locale.ROOT);
		if (!query.isEmpty()) {
			next.removeIf(entry -> !entry.name().getString().toLowerCase(Locale.ROOT).contains(query)
					&& !entry.id.contains(query) && !elementName(entry.element()).getString().toLowerCase(Locale.ROOT).contains(query));
		}
		this.entries = next;
		this.selected = previous == null ? -1 : this.entries.indexOf(previous);
		this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll));
		if (!this.draggingThumb) {
			syncThumbFromScroll();
		}
	}

	private void refreshWidgets() {
		boolean workshop = this.tab == 2;
		this.searchField.visible = this.tab != ResearchTableTabs.ANALYSIS;
		this.formationButton.visible = this.tab == ResearchTableTabs.SCRIBE || this.tab == ResearchTableTabs.LEARN;
		this.formationButton.active = selectedEntry() != null;
		this.formationButton.setMessage(label(this.tab == ResearchTableTabs.LEARN ? "learn" : "scribe"));
		this.levelDownButton.visible = workshop;
		this.levelUpButton.visible = workshop;
		this.levelDownButton.active = this.referenceLevel > 1;
		this.levelUpButton.active = this.referenceLevel < ScrollData.MAX_SPELL_LEVEL;
		this.projectionButton.visible = workshop;
		Entry selected = selectedEntry();
		int rank = this.minecraft.player == null ? 0 : FormationKnowledgeComponent.get(this.minecraft.player).research().rank;
		this.projectionButton.active = workshop && selected != null && this.referenceLevel <= rank && this.referenceLevel <= selected.spell.getMaxLevel();
		this.projectionButton.setTooltip(Tooltip.create(label(this.projectionButton.active ? "projection_help" : "projection_rank")));
	}

	private void onFormationAction() {
		refreshEntries();
		Entry entry = selectedEntry();
		if (entry == null || this.tab != ResearchTableTabs.SCRIBE && this.tab != ResearchTableTabs.LEARN) return;
		FriendlyByteBuf buffer = AddonNetwork.buffer();
		buffer.writeUtf(entry.id);
		buffer.writeUtf(entry.variant == null ? "" : entry.variant);
		buffer.writeVarInt(entry.level);
		AddonNetwork.sendToServer(this.tab == ResearchTableTabs.LEARN ? SscAddonNetworking.PACKET_FORMATION_LEARN : SscAddonNetworking.PACKET_FORMATION_SCRIBE, buffer);
	}

	private void projectSelected() {
		Entry entry = selectedEntry();
		if (entry == null || !projectionButton.active) return;
		this.researchHandoff = true;
		var screen = new SlottedFormationScreen(this.menu, this.minecraft.player.getInventory(), this);
		screen.projectUnlocked(entry.id, this.referenceLevel);
		this.minecraft.setScreen(screen);
	}

	private void drawMaterialShortfalls(GuiGraphics context) {
		if (this.tab == ResearchTableTabs.WORKSHOP) return;
		Entry entry = selectedEntry();
		if (entry == null) return;
		if (this.tab == ResearchTableTabs.LEARN) {
			drawSlotShortfall(context, 2, learnDustCost(entry.level), this.menu.getSlot(2).getItem().is(RegCustomItem.UNTREATED_MOONDUST));
			return;
		}
		drawSlotShortfall(context, 0, 1, this.menu.getSlot(0).getItem().getItem() instanceof BlankFormationPaperItem);
		ItemStack ink = this.menu.getSlot(1).getItem();
		drawSlotShortfall(context, 1, entry.level, ink.getItem() instanceof FormationInkItem inkItem
				&& inkItem.getType().element == (entry.element() == FormationElement.UNIVERSAL ? null : entry.element()));
	}

	private void drawSlotShortfall(GuiGraphics context, int index, int required, boolean valid) {
		Slot slot = this.menu.getSlot(index);
		ItemStack stack = slot.getItem();
		drawShortfall(context, slot.x, slot.y, Math.max(0, required - (valid ? stack.getCount() : 0)), stack);
	}

	private void drawShortfall(GuiGraphics context, int left, int top, int missing, ItemStack stack) {
		if (missing <= 0) {
			return;
		}
		String value = Integer.toString(missing);
		int availableWidth = stack.getCount() > 1 ? 17 - this.font.width(Integer.toString(stack.getCount())) : 16;
		float scale = Math.min(1.0f, (float) Math.max(1, availableWidth) / this.font.width(value));
		context.pose().pushPose();
		context.pose().translate(left - 1, top + 16 - this.font.lineHeight * scale, 300);
		context.pose().scale(scale, scale, 1);
		context.fill(0, 0, this.font.width(value), this.font.lineHeight, 0xB0000000);
		context.drawString(this.font, value, 0, 0, 0xFFFF55, false);
		context.pose().popPose();
	}

	private static Component elementName(FormationElement element) {
		return Component.translatable(element == null ? "element.ssc_addon.universal" : element.getNameKey());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.searchField.tick();
		refreshEntries();
		refreshWidgets();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.searchField.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
			if (this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
			if (keyCode != GLFW.GLFW_KEY_TAB) {
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.tab == ResearchTableTabs.ANALYSIS) return super.mouseClicked(mouseX, mouseY, button);
		refreshEntries();
		int localX = (int) mouseX - this.leftPos;
		int localY = (int) mouseY - this.topPos;
		if (button == 0 && maxScroll() > 0 && localX >= PANEL_X + TRACK_X && localX < PANEL_X + TRACK_X + THUMB_W
				&& localY >= PANEL_Y + 4 && localY < PANEL_Y + 68) {
			this.draggingThumb = true;
			this.searchField.setFocused(false);
			updateThumbDrag(mouseY);
			return true;
		}
		if (localX >= PANEL_X && localX < PANEL_X + PANEL_W && localY >= PANEL_Y && localY < PANEL_Y + PANEL_H) {
			if (button == 0) {
				this.selected = hoveredEntryIndex(mouseX, mouseY);
				this.searchField.setFocused(false);
				refreshWidgets();
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.draggingThumb && button == 0) {
			updateThumbDrag(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.draggingThumb && button == 0) {
			this.draggingThumb = false;
			syncThumbFromScroll();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (mouseX >= this.leftPos + PANEL_X && mouseX < this.leftPos + PANEL_X + PANEL_W
				&& mouseY >= this.topPos + PANEL_Y && mouseY < this.topPos + PANEL_Y + PANEL_H) {
			this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.signum(amount)));
			syncThumbFromScroll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	private int maxScroll() {
		return Math.max(0, (this.entries.size() + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS);
	}

	private void updateThumbDrag(double mouseY) {
		this.thumbY = Math.max(THUMB_Y_MIN, Math.min(THUMB_Y_MAX, (int) mouseY - this.topPos - PANEL_Y - THUMB_H / 2));
		float progress = (float) (this.thumbY - THUMB_Y_MIN) / (THUMB_Y_MAX - THUMB_Y_MIN);
		this.scroll = Math.round(progress * maxScroll());
	}

	private void syncThumbFromScroll() {
		this.thumbY = THUMB_Y_MIN + (maxScroll() == 0 ? 0 : Math.round((float) this.scroll / maxScroll() * (THUMB_Y_MAX - THUMB_Y_MIN)));
	}

	private int hoveredEntryIndex(double mouseX, double mouseY) {
		int localX = (int) mouseX - this.leftPos - PANEL_X - 4;
		int localY = (int) mouseY - this.topPos - PANEL_Y - 4;
		if (localX < 0 || localY < 0 || localX >= COLUMNS * CELL || localY >= VISIBLE_ROWS * CELL
				|| localX % CELL >= 20 || localY % CELL >= 20) {
			return -1;
		}
		int index = (this.scroll + localY / CELL) * COLUMNS + localX / CELL;
		return index < this.entries.size() ? index : -1;
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
		return mouseX < left || mouseX >= left + this.imageWidth
				|| mouseY < top - TAB_HEIGHT || mouseY >= top + this.imageHeight;
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		context.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 312, 212);
		if (this.tab == ResearchTableTabs.ANALYSIS) context.blit(BACKGROUND, this.leftPos + 144, this.topPos + 20, 138, 20, 4, 96, 312, 212);
		for (Slot slot : this.menu.slots) {
			if (!slot.isActive()) continue;
			context.blit(SLOT_CELL, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 0, 0, 18, 18, 18, 18);
		}
		if (this.tab == ResearchTableTabs.ANALYSIS) {
			ItemStack input = this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_ANALYSIS).getItem();
			Component heading = SpellResearchTableBlockEntity.acceptsAnalysisInput(input) ? input.getHoverName() : Component.translatable("research.ssc_addon.slotted.analysis_hint");
			drawClipped(context, heading, this.leftPos + 10, this.topPos + 29, 290, 0x333333);
			drawClipped(context, Component.translatable("research.ssc_addon.slotted.analysis_cost"), this.leftPos + 10, this.topPos + 45, 290, 0x555555);
			int ticks = this.menu.getAnalysisTicks();
			drawClipped(context, Component.translatable("research.ssc_addon.analysis.input"), this.leftPos + 24, this.topPos + 60, 108, 0x555555);
			drawClipped(context, label("paper_slot"), this.leftPos + 138, this.topPos + 60, 70, 0x555555);
			drawClipped(context, label("output_slot"), this.leftPos + 254, this.topPos + 60, 50, 0x555555);
			context.drawString(this.font, "+", this.leftPos + 97, this.topPos + 76, 0x666666, false);
			context.drawString(this.font, ">", this.leftPos + 271, this.topPos + 76, 0x666666, false);
			context.blit(ANALYSIS_PROGRESS, this.leftPos + 186, this.topPos + 74, 0, 0, 78, 12, 78, 24);
			int filled = Math.min(78, ticks * 78 / SpellResearchTableBlockEntity.ANALYSIS_TICKS);
			if (filled > 0) context.blit(ANALYSIS_PROGRESS, this.leftPos + 186, this.topPos + 74, 0, 12, filled, 12, 78, 24);
			drawClipped(context, Component.translatable("research.ssc_addon.slotted.analysis_time", ticks / 20, SpellResearchTableBlockEntity.ANALYSIS_TICKS / 20), this.leftPos + 184, this.topPos + 91, 116, 0x333333);
			var status = SpellResearchTableBlockEntity.analysisStatus(this.menu.getInventory(), this.menu.getAnalysisTicks());
			drawClipped(context, Component.translatable("research.ssc_addon.slotted.analysis_status." + status.name().toLowerCase(Locale.ROOT)), this.leftPos + 10, this.topPos + 104, 292,
					status == SpellResearchTableBlockEntity.AnalysisStatus.READY ? 0x36545A : 0x885533);
			return;
		}
		Item[] placeholders = {SscAddon.BLANK_FORMATION_PAPER, SscAddon.FORMATION_INK_NORMAL,
				RegCustomItem.UNTREATED_MOONDUST, SscAddon.MAGIC_SCROLL, RegCustomItem.MOONDUST_CRYSTAL_SHARD};
		for (int index = 0; index < placeholders.length; index++) {
			Slot slot = this.menu.getSlot(index);
			if (slot.isActive() && !slot.hasItem() && index != 3) {
				context.renderItem(new ItemStack(placeholders[index]), this.leftPos + slot.x, this.topPos + slot.y);
				context.pose().pushPose();
				context.pose().translate(0, 0, 200);
				context.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0xAA8B8B8B);
				context.pose().popPose();
			}
		}
		drawEntries(context, mouseX, mouseY);
		drawDetails(context);
	}

	private void drawEntries(GuiGraphics context, int mouseX, int mouseY) {
		int panelX = this.leftPos + PANEL_X;
		int panelY = this.topPos + PANEL_Y;
		context.blit(LIST_PANEL, panelX, panelY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
		if (this.entries.isEmpty()) {
			Component message = !this.searchField.getValue().isBlank() ? label("search_empty")
					: label(this.tab == ResearchTableTabs.SCRIBE ? "scribe_empty" : this.tab == ResearchTableTabs.LEARN ? "learn_empty" : "workshop_empty");
			List<FormattedCharSequence> lines = this.font.split(message, 104);
			int lineY = panelY + (PANEL_H - Math.min(lines.size(), 4) * 10) / 2;
			for (FormattedCharSequence line : lines.subList(0, Math.min(lines.size(), 4))) {
				context.drawString(this.font, line, panelX + 58 - this.font.width(line) / 2, lineY, 0xE0E0E0, true);
				lineY += 10;
			}
			return;
		}
		int hover = hoveredEntryIndex(mouseX, mouseY);
		for (int cellIndex = 0; cellIndex < COLUMNS * VISIBLE_ROWS; cellIndex++) {
			int index = this.scroll * COLUMNS + cellIndex;
			if (index >= this.entries.size()) {
				break;
			}
			Entry entry = this.entries.get(index);
			int cellX = panelX + 4 + cellIndex % COLUMNS * CELL;
			int cellY = panelY + 4 + cellIndex / COLUMNS * CELL;
			context.blit(FRAME, cellX, cellY, 0, 0, 20, 20, 20, 20);
			ResourceLocation icon = entry.spell == null ? null : entry.spell.getIconTexture();
			if (icon != null) {
				context.blit(icon, cellX + 2, cellY + 2, 16, 16, 0, 0, 32, 32, 32, 32);
			} else {
				context.renderItem(entry.stack(1), cellX + 2, cellY + 2);
			}
			context.pose().pushPose();
			context.pose().translate(0, 0, 200);
			SpellRarity rarity = entry.spell == null ? FormationData.getRarity(entry.level)
					: entry.spell.getRarity(this.referenceLevel);
			Integer rgb = rarity.color.getColor();
			int rarityColor = rgb == null ? 0xFFFFFF : rgb;
			border(context, cellX + 1, cellY + 1, 18, 18, 0xFF000000 | rarityColor);
			if (entry.spell == null) {
				context.drawString(this.font, Integer.toString(entry.level), cellX + 13, cellY + 11,
						rarityColor, true);
			}
			if (index == this.selected || index == hover) {
				int border = index == this.selected ? 0xFFFFD46A : 0xFFFFFFFF;
				border(context, cellX, cellY, 20, 20, border);
			}
			context.pose().popPose();
		}
		if (maxScroll() > 0) {
			context.blit(SCROLL_THUMB, panelX + TRACK_X, panelY + this.thumbY, 0, 0, THUMB_W, THUMB_H, THUMB_W, THUMB_H);
		}
	}

	private void drawDetails(GuiGraphics context) {
		Entry entry = selectedEntry();
		Component heading = entry == null ? label(this.tab == 2 ? "select_spell" : "select_formation") : entry.name();
		drawClipped(context, heading, this.leftPos + DETAIL_X, this.topPos + 24, DETAIL_W, 0x252525);
		if (this.tab == 2) {
			drawClipped(context, label("workshop_level", this.referenceLevel), this.leftPos + DETAIL_X, this.topPos + 39, 114, 0x404040);
		} else if (entry != null) {
			drawClipped(context, label("workshop_level", entry.level), this.leftPos + DETAIL_X, this.topPos + 39, DETAIL_W, 0x404040);
		}
		if (entry != null) {
			Component detail = this.tab == 2 ? elementName(entry.element()) : this.tab == ResearchTableTabs.LEARN
					? label("learn_cost", learnDustCost(entry.level))
					: label("scribe_cost", elementName(entry.element()), entry.level);
			drawClipped(context, detail, this.leftPos + DETAIL_X, this.topPos + 51, DETAIL_W, 0x36545A);
		}
		if (this.tab == ResearchTableTabs.WORKSHOP) {
			var lines = this.font.split(label("projection_help"), DETAIL_W);
			int top = this.topPos + 61;
			for (var line : lines) {
				context.drawString(this.font, line, this.leftPos + DETAIL_X, top, 0x555555, false);
				top += 10;
			}
		} else {
			drawClipped(context, label("materials"), this.leftPos + DETAIL_X, this.topPos + 62, 92, 0x555555);
			drawClipped(context, label("output_slot"), this.leftPos + 250, this.topPos + 62, 54, 0x555555);
			context.drawString(this.font, ">", this.leftPos + 263, this.topPos + 75, 0x666666, false);
		}
	}

	private void drawClipped(GuiGraphics context, Component text, int left, int top, int maxWidth, int color) {
		String value = text.getString();
		if (this.font.width(value) > maxWidth) {
			value = this.font.plainSubstrByWidth(value, maxWidth - this.font.width("...")) + "...";
		}
		context.drawString(this.font, value, left, top, color, false);
	}

	private static void border(GuiGraphics context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}

	@Override
	protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
		drawClipped(context, Component.translatable("research.ssc_addon.slotted.tab." + this.tab), 8, 8, 200, 0x252525);
		if (this.tab != ResearchTableTabs.ANALYSIS) {
			Component count = label("entry_count", this.entries.size());
			context.drawString(this.font, count, this.imageWidth - 8 - this.font.width(count), 8, 0x555555, false);
		}
		context.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
		if (this.tab != ResearchTableTabs.ANALYSIS) drawMaterialShortfalls(context);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		this.renderTooltip(context, mouseX, mouseY);
		if (this.tab == ResearchTableTabs.ANALYSIS) {
			int[] indices = {SpellResearchTableBlockEntity.SLOT_ANALYSIS, SpellResearchTableBlockEntity.SLOT_PAPER, SpellResearchTableBlockEntity.SLOT_OUTPUT};
			String[] labels = {"insert_scroll", "paper_slot", "output_slot"};
			for (int index = 0; index < indices.length; index++) {
				Slot slot = this.menu.getSlot(indices[index]);
				if (!slot.hasItem() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) context.renderTooltip(this.font, label(labels[index]), mouseX, mouseY);
			}
			if (this.isHovering(10, 104, 292, 10, mouseX, mouseY)) {
				var status = SpellResearchTableBlockEntity.analysisStatus(this.menu.getInventory(), this.menu.getAnalysisTicks());
				context.renderTooltip(this.font, Component.translatable("research.ssc_addon.slotted.analysis_status." + status.name().toLowerCase(Locale.ROOT)), mouseX, mouseY);
			}
			if (this.isHovering(10, 45, 290, 10, mouseX, mouseY)) context.renderTooltip(this.font, Component.translatable("research.ssc_addon.slotted.analysis_cost"), mouseX, mouseY);
			return;
		}
		int hover = hoveredEntryIndex(mouseX, mouseY);
		Entry entry = hover >= 0 ? this.entries.get(hover) : selectedEntry();
		boolean overDetails = mouseX >= this.leftPos + DETAIL_X && mouseX < this.leftPos + DETAIL_X + DETAIL_W
				&& mouseY >= this.topPos + 23 && mouseY < this.topPos + 34;
		if (entry != null && (hover >= 0 || overDetails) && this.minecraft != null && this.minecraft.player != null) {
			TooltipFlag tooltipFlag = this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
			List<Component> tooltip = new ArrayList<>(entry.stack(this.referenceLevel).getTooltipLines(this.minecraft.player, tooltipFlag));
			if (this.tab == ResearchTableTabs.SCRIBE) {
				tooltip.add(label("scribe_cost", elementName(entry.element()), entry.level));
			} else if (this.tab == ResearchTableTabs.LEARN) {
				tooltip.add(label("learn_cost", learnDustCost(entry.level)));
			}
			context.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
		}
		String[] slotLabels = {"paper_slot", "ink_slot", "dust_slot", this.tab == 2 ? "insert_scroll" : "output_slot", "catalyst_slot"};
		for (int index = 0; index < slotLabels.length; index++) {
			Slot slot = this.menu.getSlot(index);
			if (slot.isActive() && !slot.hasItem() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
				context.renderTooltip(this.font, label(slotLabels[index]), mouseX, mouseY);
			}
		}
	}

	public static int learnDustCost(int level) {
		return level * 2;
	}

}
