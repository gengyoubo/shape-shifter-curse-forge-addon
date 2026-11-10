package github.com.gengyoubo.sscfa.client.evolution;

import github.com.gengyoubo.sscfa.evolution.EvolutionNode;
import github.com.gengyoubo.sscfa.evolution.EvolutionRegistry;
import github.com.gengyoubo.sscfa.evolution.EvolutionRoute;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.ArrayList;
import java.util.List;

/** 灵能宝珠「转职选择形态」界面：一页一形态，确认后发转职包。 */
public class JobChangeSelectScreen extends Screen {

    private static final class FormEntry {
        final ResourceLocation formId;
        final Item icon;
        final String nameKey;
        final String descKey;

        FormEntry(ResourceLocation formId, Item icon, String nameKey, String descKey) {
            this.formId = formId;
            this.icon = icon;
            this.nameKey = nameKey;
            this.descKey = descKey;
        }
    }

    private final List<FormEntry> forms = new ArrayList<>();
    private static final List<String> DISPLAY_ORDER = List.of("familiar_fox", "axolotl");
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 210;
    private static final int ICON_SCALE = 3;

    private final Screen parent;
    private int page = 0;
    private Button chooseButton;

    public JobChangeSelectScreen(Screen parent) {
        super(Component.translatable("evolution.my_addon.job_change.select.title"));
        this.parent = parent;
    }

    /** 收到 S2C 打开请求时调用。 */
    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_OPEN_JOB_CHANGE, buf ->
                Minecraft.getInstance().setScreen(new JobChangeSelectScreen(null)));
    }

    private void buildForms() {
        forms.clear();
        List<EvolutionRoute> ordered = new ArrayList<>();
        java.util.Map<String, EvolutionRoute> remain = new java.util.TreeMap<>();
        for (EvolutionRoute route : EvolutionRegistry.INSTANCE.all().values()) {
            if (!route.enabled || route.startForm == null) continue;
            remain.put(route.routeId, route);
        }
        for (String routeId : DISPLAY_ORDER) {
            EvolutionRoute route = remain.remove(routeId);
            if (route != null) ordered.add(route);
        }
        ordered.addAll(remain.values());

        for (EvolutionRoute route : ordered) {
            ResourceLocation formId = route.startForm;
            ResourceLocation resourceId = FormIdMigration.legacy(formId);
            String namespace = resourceId.getNamespace();
            String path = resourceId.getPath();
            Item icon = Items.NETHER_STAR;
            String baseId = route.getBaseNodeId();
            if (baseId != null) {
                EvolutionNode baseNode = route.getNode(baseId);
                if (baseNode != null && baseNode.icon != null) icon = baseNode.icon;
            }
            forms.add(new FormEntry(formId, icon,
                    "origin." + namespace + ".form_" + path + ".name",
                    "evolution." + namespace + ".start.form." + path + ".desc"));
        }
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        buildForms();
        int px = panelX();
        int py = panelY();
        boolean multiPage = forms.size() > 1;

        Button prev = Button.builder(Component.literal("<"), b -> prevPage())
                .bounds(px + 6, py + PANEL_H / 2 - 10, 20, 20).build();
        prev.active = multiPage;
        this.addRenderableWidget(prev);

        Button next = Button.builder(Component.literal(">"), b -> nextPage())
                .bounds(px + PANEL_W - 26, py + PANEL_H / 2 - 10, 20, 20).build();
        next.active = multiPage;
        this.addRenderableWidget(next);

        this.chooseButton = Button.builder(
                Component.translatable("evolution.my_addon.start.select.choose"), b -> chooseCurrentForm())
                .bounds(this.width / 2 - 80, py + PANEL_H - 54, 160, 20).build();
        this.addRenderableWidget(this.chooseButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> this.onClose())
                .bounds(this.width / 2 - 60, py + PANEL_H - 30, 120, 20).build());
    }

    private void prevPage() {
        if (forms.isEmpty()) return;
        page = (page - 1 + forms.size()) % forms.size();
    }

    private void nextPage() {
        if (forms.isEmpty()) return;
        page = (page + 1) % forms.size();
    }

    private void updateChooseButtonState() {
        if (chooseButton != null) chooseButton.active = !isCurrentPageOwnForm();
    }

    private boolean isCurrentPageOwnForm() {
        if (page < 0 || page >= forms.size()) return false;
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        var form = FormManager.current(player);
        ResourceLocation nowFormId = form == null ? null : form.id();
        return forms.get(page).formId.equals(nowFormId);
    }

    private void chooseCurrentForm() {
        if (page < 0 || page >= forms.size() || isCurrentPageOwnForm()) return;
        FormEntry form = forms.get(page);
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeUtf(form.formId.toString());
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_JOB_CHANGE_CONFIRM, buf);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        updateChooseButtonState();
        this.renderBackground(ctx);

        int px = panelX();
        int py = panelY();
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xE0140C08);
        drawBorder(ctx, px, py, PANEL_W, PANEL_H, 0xFFB8893A);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, py + 10, 0xFFE8C66A);

        if (!forms.isEmpty()) {
            Component pageInfo = Component.translatable("evolution.my_addon.start.select.page", page + 1, forms.size());
            ctx.drawCenteredString(this.font, pageInfo, this.width / 2, py + 24, 0xFFAAAAAA);

            FormEntry form = forms.get(page);
            int iconCenterX = this.width / 2;
            int iconTopY = py + 44;
            ctx.pose().pushPose();
            ctx.pose().translate(iconCenterX - 8 * ICON_SCALE, iconTopY, 0);
            ctx.pose().scale(ICON_SCALE, ICON_SCALE, 1);
            ctx.renderItem(new ItemStack(form.icon), 0, 0);
            ctx.pose().popPose();

            Component name = Component.translatable(form.nameKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            ctx.drawCenteredString(this.font, name, this.width / 2, iconTopY + 16 * ICON_SCALE + 8, 0xFFFFFFFF);

            MultiLineLabel desc = MultiLineLabel.create(this.font,
                    Component.translatable(form.descKey).withStyle(ChatFormatting.GRAY), PANEL_W - 40);
            desc.renderCentered(ctx, this.width / 2, iconTopY + 16 * ICON_SCALE + 26);

            if (isCurrentPageOwnForm()) {
                Component current = Component.translatable("evolution.my_addon.job_change.current")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
                ctx.drawCenteredString(this.font, current, this.width / 2, py + PANEL_H - 68, 0xFFFF6666);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
