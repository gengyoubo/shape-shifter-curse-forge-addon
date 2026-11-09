package github.com.gengyoubo.sscfa.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.sscfa.client.SpellcastClient;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import github.com.gengyoubo.sscfa.spell.*;
import github.com.gengyoubo.sscfa.spell.research.RuneScheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Three equal vanilla iron slots. The independent stability bar owns all cost/risk information. */
public class SpellbookHudRenderer {
    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.RenderGuiEvent.Post event) ->
                        renderBeforeChat(event.getGuiGraphics(), event.getPartialTick()));
    }

    private static int[] anchorPosition(int type, int offsetX, int offsetY, int screenW, int screenH) {
        int col = (Math.max(1, Math.min(9, type)) - 1) % 3;
        int row = (Math.max(1, Math.min(9, type)) - 1) / 3;
        int x = col == 0 ? 0 : col == 1 ? screenW / 2 : screenW;
        int y = row == 0 ? 0 : row == 1 ? screenH / 2 : screenH;
        return new int[]{x + offsetX, y + offsetY};
    }

    /** Includes text that extends past the frame, so every zoom level remains draggable and visible. */
    public static int[] hudAnchor() {
        var mc=Minecraft.getInstance();var cfg=SSCAddonConfig.client();
        int[] p=anchorPosition(cfg.spellbookHudPosType,cfg.spellbookHudPosOffsetX,cfg.spellbookHudPosOffsetY,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        return new int[]{clampX(p[0],mc.getWindow().getGuiScaledWidth()),clampY(p[1],mc.getWindow().getGuiScaledHeight())};
    }
    public static HudScaleGeometry.Box scaleBounds() {
        var mc=Minecraft.getInstance();int[] a=hudAnchor();
        int left=a[0]+ORIGIN_X,right=left+WIDTH;
        var book=mc.player==null?ItemStack.EMPTY:SpellcastClient.getEquippedBook();
        if(mc.level!=null && book!=null && !book.isEmpty() && SpellbookData.getSlotCount(book)>0){
            int selected=Math.max(0,Math.min(SpellcastClient.getSelectedSlot(),SpellbookData.getSlotCount(book)-1));
            var snap=snapshotOf(mc,book,selected);int nameW=mc.font.width(snap.label());
            if(nameW>0){int x=Math.max(3,Math.min(a[0]+CENTER_X-nameW/2,mc.getWindow().getGuiScaledWidth()-nameW-3));left=Math.min(left,x-3);right=Math.max(right,x+nameW+3);}
        }
        return new HudScaleGeometry.Box(left,a[1]+0,right-left,HEIGHT);
    }
    public static final int DEFAULT_X = 26;
    public static final int DEFAULT_Y = -60;
    public static final int SLOT_SIZE = 24;
    public static final int ICON_SIZE = 16;
    public static final int SLOT_STEP = 26;
    private static final int SOURCE_WIDTH = 76;
    private static final int CENTER_X = SOURCE_WIDTH / 2;
    public static final float SLOT_SCALE = 1.2f;
    public static final int ORIGIN_X = (int) Math.floor(CENTER_X * (1 - SLOT_SCALE));
    public static final int WIDTH = (int) Math.ceil(SOURCE_WIDTH * SLOT_SCALE);
    private static final int NAME_Y = Math.round(SLOT_SIZE * SLOT_SCALE) + 4;
    public static final int HEIGHT = NAME_Y + 12;
    // Double the previously requested 2x2 display size; the PNG core remains exactly 3x3.
    private static final float MARK_SCALE = 4f / (3f * SLOT_SCALE);
    private static final int OUTLINE = 0xFF101416;
    private static final ResourceLocation SLOT = texture("slot");
    private static final ResourceLocation SELECTED = texture("slot-selected");
    private static final ResourceLocation MARKS = texture("rarity-marks");
    private static final ResourceLocation NAME_PLATE = texture("name-plate");
    private static final SpellbookHudRenderer BEFORE_CHAT = new SpellbookHudRenderer();

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("ssca_neomagic", "textures/gui/spell_hud/" + name + ".png");
    }

    /** Called once by InGameHud, immediately before vanilla chat. */
    public static void renderBeforeChat(GuiGraphics context, float tickDelta) {
        BEFORE_CHAT.onHudRender(context, tickDelta);
    }

    private record SlotView(ItemStack scroll, Spell spell, int rarity, boolean modified,
                            float cooldownFraction, String cooldownText) {}
    private record TickSnapshot(SlotView previous, SlotView selected, SlotView next, Component label) {}
    private static final SlotView EMPTY = new SlotView(ItemStack.EMPTY, null, 0, false, 0, "");
    private static Object snapWorld;
    private static ItemStack snapBook;
    private static long snapTick = Long.MIN_VALUE;
    private static int snapSlot = -1;
    private static TickSnapshot snapshot;

    /** Cache scroll decoding, rune/formation queries and formatted labels once per game tick. */
    private static TickSnapshot snapshotOf(Minecraft mc, ItemStack book, int selected) {
        long tick = mc.level.getGameTime();
        if (snapWorld == mc.level && snapBook == book && snapSlot == selected && snapTick == tick
                && snapshot != null) return snapshot;
        int previous = SpellbookData.nextFilledSlot(book, selected, -1);
        int next = SpellbookData.nextFilledSlot(book, selected, 1);
        if (previous < 0 || next < 0) previous = next = selected;
        SlotView current = slotView(mc, book, SpellbookData.getScroll(book, selected));
        var label = current.spell() == null ? Component.empty() : SpellCastFeedback.spellName(current.spell(), current.scroll()).copy();
        if (current.spell() != null) {
            int castLevel = ScrollData.getCastLevel(current.scroll());
            int scrollLevel = ScrollData.getLevel(current.scroll());
            if (castLevel < scrollLevel) label.append(Component.literal(" Lv" + castLevel + "/" + scrollLevel).withStyle(ChatFormatting.AQUA));
        }
        snapshot = new TickSnapshot(slotView(mc, book, SpellbookData.getScroll(book, previous)), current,
                slotView(mc, book, SpellbookData.getScroll(book, next)), label);
        snapWorld = mc.level;
        snapBook = book;
        snapSlot = selected;
        snapTick = tick;
        return snapshot;
    }

    private static SlotView slotView(Minecraft mc, ItemStack book, ItemStack scroll) {
        if (scroll.isEmpty()) return EMPTY;
        Spell spell = ScrollData.getSpell(scroll);
        if (spell == null) return new SlotView(scroll, null, 0, false, 0, "");
        int rarity = spell.getRarity(ScrollData.getLevel(scroll)).ordinal();
        long remaining = Math.max(0, SharedSpellCooldowns.getEffectiveCooldownEnd(mc.player, scroll) - mc.level.getGameTime());
        float fraction = 0;
        if (remaining > 0) {
            int level = ScrollData.getCastLevel(scroll);
            float levelCd = spell.getBaseCooldownTicks() * spell.getCooldownMultiplier(level);
            int total = Math.max(Math.round(levelCd * (2 - ScrollData.getDurabilityRatio(scroll))
                            * FormationData.sumCooldownMultiplier(book, spell.getElement())),
                    Math.max(spell.getCooldownFloorTicks(), Math.round(levelCd * SpellNumbers.RELATIVE_CD_FLOOR)));
            total = RuneScheme.modifiers(mc.player, scroll, level).cooldown(total);
            int authoritative = SharedSpellCooldowns.getClientCooldownTotal(spell, ScrollData.getCooldownEnd(scroll),
                    scroll.getTag() == null ? 0 : scroll.getTag().getInt("RuneCdTotal"));
            if (authoritative > 0) total = authoritative;
            fraction = total > 0 ? Math.min(1, remaining / (float) total) : 1;
        }
        // Long cooldowns use whole seconds so the value fits inside a 24px slot.
        String time = remaining == 0 ? "" : remaining < 2000 ? SpellCastFeedback.seconds(remaining)
                : Long.toString((remaining + 19) / 20);
        return new SlotView(scroll, spell, rarity, RuneScheme.isModified(scroll), fraction, time);
    }

    /** The editor and renderer share dimensions and edge clamping. */
    public static int clampX(int x, int screenWidth) {
        return Math.max(-ORIGIN_X, Math.min(x, screenWidth - WIDTH - ORIGIN_X));
    }

    public static int clampY(int y, int screenHeight) {
        return Math.max(0, Math.min(y, screenHeight - HEIGHT));
    }

    public void onHudRender(GuiGraphics context, float tickDelta) {
        github.com.gengyoubo.sscfa.client.hud.HudScaling.render(context, 3, () -> renderUnscaled(context, tickDelta));
    }

    private void renderUnscaled(GuiGraphics context, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui
                || !SSCAddonConfig.client().showSpellbookHud) return;
        ItemStack book = SpellcastClient.getEquippedBook();
        if (book == null || book.isEmpty() || SpellbookData.getSlotCount(book) <= 0) return;
        var cfg = SSCAddonConfig.client();
        int[] anchor = anchorPosition(cfg.spellbookHudPosType, cfg.spellbookHudPosOffsetX, cfg.spellbookHudPosOffsetY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        int selected = Math.max(0, Math.min(SpellcastClient.getSelectedSlot(), SpellbookData.getSlotCount(book) - 1));
        draw(context, clampX(anchor[0], mc.getWindow().getGuiScaledWidth()),
                clampY(anchor[1], mc.getWindow().getGuiScaledHeight()), snapshotOf(mc, book, selected));
    }

    /** Actual textures in the position editor, also usable without a book or player. */
    public static void drawPreview(GuiGraphics context, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack book = mc.player == null ? ItemStack.EMPTY : SpellcastClient.getEquippedBook();
        if (mc.level != null && book != null && !book.isEmpty() && SpellbookData.getSlotCount(book) > 0) {
            int selected = Math.max(0, Math.min(SpellcastClient.getSelectedSlot(), SpellbookData.getSlotCount(book) - 1));
            draw(context, x, y, snapshotOf(mc, book, selected));
        } else draw(context, x, y, new TickSnapshot(EMPTY, EMPTY, EMPTY, Component.empty()));
    }

    private static void draw(GuiGraphics context, int x, int y, TickSnapshot data) {
        context.flush();
        float[] color = RenderSystem.getShaderColor().clone();
        SpellbookHudLayer.push(context.pose());
        try {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.pose().pushPose();
            try {
                // Grow around the existing center; all three frames share the same baseline.
                context.pose().translate(x + CENTER_X, y, 0);
                context.pose().scale(SLOT_SCALE, SLOT_SCALE, 1);
                context.pose().translate(-CENTER_X, 0, 0);
                drawSlot(context, data.previous(), 0, 0, false);
                drawSlot(context, data.next(), 2 * SLOT_STEP, 0, false);
                drawSlot(context, data.selected(), SLOT_STEP, 0, true);
            } finally {
                context.pose().popPose();
            }
            // The name follows the taller row, retaining its original font and PNG backplate.
            if (!data.label().getString().isEmpty()) drawLabel(context, data.label(), x + CENTER_X, y + NAME_Y);
        } finally {
            try {
                context.flush();
            } finally {
                context.pose().popPose();
                RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }
        }
    }

    private static void drawSlot(GuiGraphics context, SlotView data, int x, int y, boolean selected) {
        context.blit(selected ? SELECTED : SLOT, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (data.scroll().isEmpty()) return;
        int iconX = x + 4, iconY = y + 4;
        ResourceLocation icon = data.spell() == null ? null : data.spell().getIconTexture();
        if (icon == null) context.renderItem(data.scroll(), iconX, iconY);
        else context.blit(icon, iconX, iconY, ICON_SIZE, ICON_SIZE, 0, 0, 32, 32, 32, 32);
        context.pose().pushPose();
        context.pose().translate(0, 0, 260);
        try {
            if (data.cooldownFraction() > 0) {
                int maskHeight = (int) Math.ceil(ICON_SIZE * data.cooldownFraction());
                context.fill(iconX, iconY + ICON_SIZE - maskHeight, iconX + ICON_SIZE, iconY + ICON_SIZE, 0xAA000000);
                var mc = Minecraft.getInstance();
                int textWidth = mc.font.width(data.cooldownText());
                // 冷却数字缩为原来??75%，长数字继续按图标内宽收敛，保留描边余量??
                float textScale = Math.min(0.75f, (ICON_SIZE - 2f) / Math.max(1, textWidth));
                context.pose().pushPose();
                try {
                    context.pose().translate(x + (SLOT_SIZE - textWidth * textScale) / 2f,
                            y + (SLOT_SIZE - mc.font.lineHeight * textScale) / 2f, 0);
                    context.pose().scale(textScale, textScale, 1);
                    drawOutlined(context, Component.literal(data.cooldownText()), 0, 0, 0xFFF3F7F1);
                } finally {
                    context.pose().popPose();
                }
            }
            if (data.spell() != null) {
                // Keep the requested +1 right / +2 down GUI-pixel offsets independent of row scaling.
                // Compensate for SLOT_SCALE so the core finishes at 4x4, independent of row size.
                context.pose().pushPose();
                try {
                    context.pose().translate(x + 21 - 5 * MARK_SCALE + 1 / SLOT_SCALE,
                            y + 20 - 5 * MARK_SCALE + 2 / SLOT_SCALE, 0);
                    context.pose().scale(MARK_SCALE, MARK_SCALE, 1);
                    context.blit(MARKS, 0, 0, 5 * Math.max(0, Math.min(5, data.rarity())),
                            data.modified() ? 5 : 0, 5, 5, 30, 10);
                } finally {
                    context.pose().popPose();
                }
            }
        } finally {
            context.pose().popPose();
        }
    }

    private static void drawLabel(GuiGraphics context, Component label, int centerX, int y) {
        var mc = Minecraft.getInstance();
        int width = mc.font.width(label);
        int x = Math.max(3, Math.min(centerX - width / 2, mc.getWindow().getGuiScaledWidth() - width - 3));
        int plateX = x - 3, plateY = y - 2, plateWidth = width + 6, plateHeight = mc.font.lineHeight + 4;
        // Nine-slice the PNG. Only solid pixels stretch, keeping the stepped corners sharp.
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            int dx = col == 0 ? 0 : col == 1 ? 1 : plateWidth - 1;
            int dy = row == 0 ? 0 : row == 1 ? 1 : plateHeight - 1;
            int w = col == 1 ? plateWidth - 2 : 1, h = row == 1 ? plateHeight - 2 : 1;
            context.blit(NAME_PLATE, plateX + dx, plateY + dy, w, h, col, row, 1, 1, 3, 3);
        }
        drawOutlined(context, label, x, y, 0xFFF0F6F2);
    }

    private static void drawOutlined(GuiGraphics context, Component text, int x, int y, int color) {
        var font = Minecraft.getInstance().font;
        // The outline is plain so formatted text retains a dark contour, including the blue level tag.
        String plain = text.getString();
        // 同一段描边和字面一起提交，仍保持四个描边在前、字面在后的顺序?
        context.drawManaged(() -> {
            context.drawString(font, plain, x - 1, y, OUTLINE, false);
            context.drawString(font, plain, x + 1, y, OUTLINE, false);
            context.drawString(font, plain, x, y - 1, OUTLINE, false);
            context.drawString(font, plain, x, y + 1, OUTLINE, false);
            context.drawString(font, text, x, y, color, false);
        });
    }
}
