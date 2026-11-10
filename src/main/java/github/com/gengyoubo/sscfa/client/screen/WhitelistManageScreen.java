package github.com.gengyoubo.sscfa.client.screen;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 白名单管理 GUI：所有玩家 / 已加入玩家 / 生物 三个 tab，可切换白名单模式。 */
public class WhitelistManageScreen extends Screen {

    public record MobEntry(UUID uuid, String typeId) {}

    private enum Tab { ALL_PLAYERS, PLAYER_WL, MOB }

    private enum SortMode {
        DISTANCE("screen.ssc_addon.whitelist.mode.distance"),
        ALPHABETICAL("screen.ssc_addon.whitelist.mode.alphabetical"),
        ADDED_FIRST("screen.ssc_addon.whitelist.mode.added");
        final String key;
        SortMode(String key) { this.key = key; }
    }

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 240;
    private static final int ENTRY_HEIGHT = 22;
    private static final int LIST_VISIBLE_ROWS = 6;

    private Set<UUID> whitelist;
    private boolean customMode;
    private List<MobEntry> mobs;

    private Tab currentTab = Tab.ALL_PLAYERS;
    private EditBox searchField;
    private EditBox uuidField;
    private Button sortButton;
    private Button modeButton;
    private Button addUuidButton;
    private Button allPlayersTabButton;
    private Button playerWlTabButton;
    private Button mobTabButton;
    private SortMode sortMode = SortMode.DISTANCE;
    private int scrollOffset = 0;

    private List<PlayerInfo> filteredCache = Collections.emptyList();
    private String lastSearchText = "";
    private SortMode lastSortMode = null;
    private int lastWhitelistHash = 0;

    private Map<UUID, Integer> mobIndexInSpecies = new HashMap<>();

    private int listX;
    private int listY;
    private int listW;

    public WhitelistManageScreen(Set<UUID> whitelist, boolean customMode, List<MobEntry> mobs) {
        super(Component.translatable("screen.ssc_addon.whitelist.title"));
        this.whitelist = whitelist != null ? new HashSet<>(whitelist) : new HashSet<>();
        this.customMode = customMode;
        this.mobs = mobs != null ? new ArrayList<>(mobs) : new ArrayList<>();
        recomputeMobIndices();
    }

    /** 收到 S2C 同步时打开或刷新界面。 */
    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_WHITELIST_GUI_SYNC, buf -> {
            boolean custom = buf.readBoolean();
            int playerCount = buf.readInt();
            Set<UUID> players = new HashSet<>();
            for (int i = 0; i < playerCount; i++) players.add(buf.readUUID());
            int mobCount = buf.readInt();
            List<MobEntry> mobs = new ArrayList<>();
            for (int i = 0; i < mobCount; i++) {
                UUID uuid = buf.readUUID();
                String typeId = buf.readUtf();
                mobs.add(new MobEntry(uuid, typeId.isEmpty() ? null : typeId));
            }
            Minecraft client = Minecraft.getInstance();
            if (client.screen instanceof WhitelistManageScreen screen) {
                screen.updateState(players, custom, mobs);
            } else {
                client.setScreen(new WhitelistManageScreen(players, custom, mobs));
            }
        });
    }

    public void updateState(Set<UUID> newSet, boolean customMode) {
        updateState(newSet, customMode, this.mobs);
    }

    public void updateState(Set<UUID> newSet, boolean customMode, List<MobEntry> mobs) {
        this.whitelist = new HashSet<>(newSet);
        this.customMode = customMode;
        this.mobs = mobs != null ? new ArrayList<>(mobs) : new ArrayList<>();
        if (this.modeButton != null) this.modeButton.setMessage(getWhitelistModeButtonText());
        recomputeMobIndices();
        invalidateFilterCache();
        int max = Math.max(0, getCurrentListSize() - LIST_VISIBLE_ROWS);
        if (this.scrollOffset > max) this.scrollOffset = max;
    }

    private void recomputeMobIndices() {
        this.mobIndexInSpecies = new HashMap<>();
        LinkedHashMap<String, List<MobEntry>> grouped = new LinkedHashMap<>();
        for (MobEntry entry : this.mobs) {
            String key = entry.typeId() != null ? entry.typeId() : "";
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
        }
        for (List<MobEntry> group : grouped.values()) {
            group.sort(Comparator.comparing(entry -> entry.uuid().toString()));
            int index = 1;
            for (MobEntry entry : group) this.mobIndexInSpecies.put(entry.uuid(), index++);
        }
    }

    private int getCurrentListSize() {
        return switch (this.currentTab) {
            case ALL_PLAYERS -> getFilteredEntries().size();
            case PLAYER_WL -> getPlayerWhitelistEntries().size();
            case MOB -> this.mobs.size();
        };
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        this.allPlayersTabButton = Button.builder(
                        Component.translatable("screen.ssc_addon.whitelist.tab.all_players"),
                        button -> switchTab(Tab.ALL_PLAYERS))
                .bounds(left + 10, top + 22, 88, 18).build();
        this.addRenderableWidget(this.allPlayersTabButton);

        this.playerWlTabButton = Button.builder(
                        Component.translatable("screen.ssc_addon.whitelist.tab.player_wl"),
                        button -> switchTab(Tab.PLAYER_WL))
                .bounds(left + 102, top + 22, 88, 18).build();
        this.addRenderableWidget(this.playerWlTabButton);

        this.mobTabButton = Button.builder(
                        Component.translatable("screen.ssc_addon.whitelist.tab.mob"),
                        button -> switchTab(Tab.MOB))
                .bounds(left + 194, top + 22, 88, 18).build();
        this.addRenderableWidget(this.mobTabButton);

        this.searchField = new EditBox(this.font, left + 10, top + 46, PANEL_WIDTH - 20, 18,
                Component.translatable("screen.ssc_addon.whitelist.search.placeholder"));
        this.searchField.setMaxLength(64);
        this.searchField.setResponder(text -> invalidateFilterCache());
        this.addRenderableWidget(this.searchField);

        this.sortButton = Button.builder(getSortButtonText(), button -> {
                    this.sortMode = SortMode.values()[(this.sortMode.ordinal() + 1) % SortMode.values().length];
                    button.setMessage(getSortButtonText());
                    invalidateFilterCache();
                })
                .bounds(left + 10, top + 70, 120, 20).build();
        this.addRenderableWidget(this.sortButton);

        this.modeButton = Button.builder(getWhitelistModeButtonText(), button -> {
                    this.customMode = !this.customMode;
                    button.setMessage(getWhitelistModeButtonText());
                    sendModeToggle(this.customMode);
                })
                .bounds(left + 134, top + 70, 156, 20).build();
        this.addRenderableWidget(this.modeButton);

        int uuidY = top + PANEL_HEIGHT - 50;
        this.uuidField = new EditBox(this.font, left + 10, uuidY, PANEL_WIDTH - 90, 18,
                Component.translatable("screen.ssc_addon.whitelist.uuid_input.placeholder"));
        this.uuidField.setMaxLength(36);
        this.addRenderableWidget(this.uuidField);

        this.addUuidButton = Button.builder(
                        Component.translatable("screen.ssc_addon.whitelist.uuid_input.add"),
                        button -> tryAddByUuidInput())
                .bounds(left + PANEL_WIDTH - 75, uuidY - 1, 65, 20).build();
        this.addRenderableWidget(this.addUuidButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(left + PANEL_WIDTH / 2 - 50, top + PANEL_HEIGHT - 25, 100, 20).build());

        this.listX = left + 10;
        this.listY = top + 94;
        this.listW = PANEL_WIDTH - 20;

        updateTabUiVisibility();
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        this.scrollOffset = 0;
        updateTabUiVisibility();
    }

    private void updateTabUiVisibility() {
        boolean isAllPlayers = this.currentTab == Tab.ALL_PLAYERS;
        this.searchField.visible = isAllPlayers;
        this.sortButton.visible = isAllPlayers;
        this.uuidField.visible = isAllPlayers;
        this.addUuidButton.visible = isAllPlayers;
        this.allPlayersTabButton.active = this.currentTab != Tab.ALL_PLAYERS;
        this.playerWlTabButton.active = this.currentTab != Tab.PLAYER_WL;
        this.mobTabButton.active = this.currentTab != Tab.MOB;
    }

    private Component getSortButtonText() {
        return Component.translatable("screen.ssc_addon.whitelist.mode.label",
                Component.translatable(this.sortMode.key));
    }

    private Component getWhitelistModeButtonText() {
        String key = this.customMode
                ? "screen.ssc_addon.whitelist.wlmode.custom"
                : "screen.ssc_addon.whitelist.wlmode.default";
        return Component.translatable("screen.ssc_addon.whitelist.wlmode.label", Component.translatable(key));
    }

    private void sendModeToggle(boolean custom) {
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeByte(custom ? 1 : 0);
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_WHITELIST_GUI_MODE, buf);
    }

    private void invalidateFilterCache() {
        this.lastSortMode = null;
    }

    private List<PlayerInfo> getFilteredEntries() {
        if (this.minecraft == null || this.minecraft.getConnection() == null) return Collections.emptyList();
        String search = this.searchField != null ? this.searchField.getValue().trim().toLowerCase(Locale.ROOT) : "";
        int whitelistHash = this.whitelist.hashCode();
        if (search.equals(this.lastSearchText) && this.sortMode == this.lastSortMode
                && whitelistHash == this.lastWhitelistHash) {
            return this.filteredCache;
        }
        this.lastSearchText = search;
        this.lastSortMode = this.sortMode;
        this.lastWhitelistHash = whitelistHash;

        Collection<PlayerInfo> all = this.minecraft.getConnection().getOnlinePlayers();
        List<PlayerInfo> list = new ArrayList<>();
        for (PlayerInfo entry : all) {
            GameProfile profile = entry.getProfile();
            if (profile == null) continue;
            if (!search.isEmpty()) {
                String name = profile.getName() == null ? "" : profile.getName().toLowerCase(Locale.ROOT);
                String idStr = profile.getId() == null ? "" : profile.getId().toString().toLowerCase(Locale.ROOT);
                if (!name.contains(search) && !idStr.contains(search)) continue;
            }
            list.add(entry);
        }

        switch (this.sortMode) {
            case DISTANCE -> list.sort(Comparator.comparingDouble(this::distanceTo));
            case ALPHABETICAL -> list.sort((a, b) -> {
                String na = a.getProfile().getName() == null ? "" : a.getProfile().getName();
                String nb = b.getProfile().getName() == null ? "" : b.getProfile().getName();
                return na.compareToIgnoreCase(nb);
            });
            case ADDED_FIRST -> list.sort((a, b) -> {
                boolean aw = this.whitelist.contains(a.getProfile().getId());
                boolean bw = this.whitelist.contains(b.getProfile().getId());
                if (aw == bw) {
                    String na = a.getProfile().getName() == null ? "" : a.getProfile().getName();
                    String nb = b.getProfile().getName() == null ? "" : b.getProfile().getName();
                    return na.compareToIgnoreCase(nb);
                }
                return aw ? -1 : 1;
            });
        }

        this.filteredCache = list;
        int maxOffset = Math.max(0, list.size() - LIST_VISIBLE_ROWS);
        if (this.scrollOffset > maxOffset) this.scrollOffset = maxOffset;
        return list;
    }

    private double distanceTo(PlayerInfo entry) {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return Double.MAX_VALUE;
        }
        UUID uuid = entry.getProfile().getId();
        if (uuid == null) return Double.MAX_VALUE;
        var other = this.minecraft.level.getPlayerByUUID(uuid);
        if (other == null) return Double.MAX_VALUE;
        Vec3 a = this.minecraft.player.position();
        Vec3 b = other.position();
        return a.distanceToSqr(b);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        ctx.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xC0101010);
        drawBorder(ctx, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF606060);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFFFF);

        Component countText = switch (this.currentTab) {
            case ALL_PLAYERS -> Component.translatable("screen.ssc_addon.whitelist.count", this.whitelist.size());
            case PLAYER_WL -> Component.translatable("screen.ssc_addon.whitelist.count.player_wl", getPlayerWhitelistEntries().size());
            case MOB -> Component.translatable("screen.ssc_addon.whitelist.count.mob", this.mobs.size());
        };
        int countWidth = this.font.width(countText);
        ctx.drawString(this.font, countText, left + PANEL_WIDTH - 10 - countWidth, top + PANEL_HEIGHT - 70, 0xFFCCCCCC);

        switch (this.currentTab) {
            case ALL_PLAYERS -> renderPlayerList(ctx, mouseX, mouseY);
            case PLAYER_WL -> renderPlayerWhitelistList(ctx, mouseX, mouseY);
            case MOB -> renderMobList(ctx, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPlayerList(GuiGraphics ctx, int mouseX, int mouseY) {
        List<PlayerInfo> entries = getFilteredEntries();
        int listH = LIST_VISIBLE_ROWS * ENTRY_HEIGHT;
        ctx.fill(this.listX, this.listY, this.listX + this.listW, this.listY + listH, 0x80000000);

        if (entries.isEmpty()) {
            ctx.drawCenteredString(this.font, Component.translatable("screen.ssc_addon.whitelist.empty"),
                    this.listX + this.listW / 2, this.listY + listH / 2 - 4, 0xFF888888);
        } else {
            int end = Math.min(entries.size(), this.scrollOffset + LIST_VISIBLE_ROWS);
            for (int i = this.scrollOffset; i < end; i++) {
                int rowY = this.listY + (i - this.scrollOffset) * ENTRY_HEIGHT;
                renderPlayerEntry(ctx, entries.get(i), rowY, mouseX, mouseY);
            }
        }
        renderScrollbar(ctx, entries.size(), listH);
    }

    private List<UUID> getPlayerWhitelistEntries() {
        List<UUID> list = new ArrayList<>(this.whitelist);
        list.sort(Comparator.comparing(UUID::toString));
        return list;
    }

    private void renderPlayerWhitelistList(GuiGraphics ctx, int mouseX, int mouseY) {
        List<UUID> entries = getPlayerWhitelistEntries();
        int listH = LIST_VISIBLE_ROWS * ENTRY_HEIGHT;
        ctx.fill(this.listX, this.listY, this.listX + this.listW, this.listY + listH, 0x80000000);

        if (entries.isEmpty()) {
            ctx.drawCenteredString(this.font, Component.translatable("screen.ssc_addon.whitelist.empty.player_wl"),
                    this.listX + this.listW / 2, this.listY + listH / 2 - 4, 0xFF888888);
        } else {
            int end = Math.min(entries.size(), this.scrollOffset + LIST_VISIBLE_ROWS);
            for (int i = this.scrollOffset; i < end; i++) {
                int rowY = this.listY + (i - this.scrollOffset) * ENTRY_HEIGHT;
                renderPlayerWhitelistEntry(ctx, entries.get(i), rowY, mouseX, mouseY);
            }
        }
        renderScrollbar(ctx, entries.size(), listH);
    }

    private void renderPlayerWhitelistEntry(GuiGraphics ctx, UUID uuid, int rowY, int mouseX, int mouseY) {
        ctx.fill(this.listX, rowY, this.listX + this.listW - 6, rowY + ENTRY_HEIGHT, 0x4030FF30);

        boolean hover = mouseX >= this.listX && mouseX < this.listX + this.listW - 6
                && mouseY >= rowY && mouseY < rowY + ENTRY_HEIGHT;
        if (hover) ctx.fill(this.listX, rowY, this.listX + this.listW - 6, rowY + ENTRY_HEIGHT, 0x30FFFFFF);

        PlayerInfo online = this.minecraft != null && this.minecraft.getConnection() != null
                ? this.minecraft.getConnection().getPlayerInfo(uuid) : null;
        ResourceLocation skin = online != null ? online.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(uuid);
        if (skin != null) {
            ctx.blit(skin, this.listX + 3, rowY + 3, 16, 16, 8, 8, 8, 8, 64, 64);
            ctx.blit(skin, this.listX + 3, rowY + 3, 16, 16, 40, 8, 8, 8, 64, 64);
        }

        ctx.drawString(this.font, Component.literal("✔"), this.listX + 24, rowY + 7, 0xFF55FF55);

        String name = (online != null && online.getProfile() != null && online.getProfile().getName() != null)
                ? online.getProfile().getName()
                : Component.translatable("screen.ssc_addon.whitelist.entry.offline_name").getString();
        ctx.drawString(this.font, name, this.listX + 36, rowY + 4, 0xFF55FF55);

        String sub = uuid.toString().substring(0, 8);
        ctx.drawString(this.font, Component.literal(sub).withStyle(ChatFormatting.DARK_GRAY),
                this.listX + 36, rowY + 13, 0xFF606060);

        int buttonX = this.listX + this.listW - 30;
        int buttonY = rowY + 3;
        boolean buttonHover = mouseX >= buttonX && mouseX < buttonX + 22 && mouseY >= buttonY && mouseY < buttonY + 16;
        ctx.fill(buttonX, buttonY, buttonX + 22, buttonY + 16, buttonHover ? 0xFFAA3030 : 0xFF802020);
        drawBorder(ctx, buttonX, buttonY, 22, 16, 0xFF000000);
        ctx.drawCenteredString(this.font, "-", buttonX + 11, buttonY + 4, 0xFFFFFFFF);
    }

    private void renderMobList(GuiGraphics ctx, int mouseX, int mouseY) {
        int listH = LIST_VISIBLE_ROWS * ENTRY_HEIGHT;
        ctx.fill(this.listX, this.listY, this.listX + this.listW, this.listY + listH, 0x80000000);

        if (this.mobs.isEmpty()) {
            ctx.drawCenteredString(this.font, Component.translatable("screen.ssc_addon.whitelist.empty.mob"),
                    this.listX + this.listW / 2, this.listY + listH / 2 - 4, 0xFF888888);
        } else {
            int end = Math.min(this.mobs.size(), this.scrollOffset + LIST_VISIBLE_ROWS);
            for (int i = this.scrollOffset; i < end; i++) {
                int rowY = this.listY + (i - this.scrollOffset) * ENTRY_HEIGHT;
                renderMobEntry(ctx, this.mobs.get(i), rowY, mouseX, mouseY);
            }
        }
        renderScrollbar(ctx, this.mobs.size(), listH);
    }

    private void renderScrollbar(GuiGraphics ctx, int total, int listH) {
        if (total > LIST_VISIBLE_ROWS) {
            int barX = this.listX + this.listW - 4;
            int barH = Math.max(8, (LIST_VISIBLE_ROWS * listH) / total);
            int barY = this.listY + (int) ((float) this.scrollOffset / (total - LIST_VISIBLE_ROWS) * (listH - barH));
            ctx.fill(barX, this.listY, barX + 4, this.listY + listH, 0x40FFFFFF);
            ctx.fill(barX, barY, barX + 4, barY + barH, 0xFFAAAAAA);
        }
    }

    private void renderPlayerEntry(GuiGraphics ctx, PlayerInfo entry, int rowY, int mouseX, int mouseY) {
        GameProfile profile = entry.getProfile();
        UUID uuid = profile.getId();
        boolean isSelf = this.minecraft != null && this.minecraft.player != null && uuid != null
                && uuid.equals(this.minecraft.player.getUUID());
        boolean inList = uuid != null && this.whitelist.contains(uuid);

        int rowBg = isSelf ? 0x40808080 : (inList ? 0x4030FF30 : 0x40FF3030);
        ctx.fill(this.listX, rowY, this.listX + this.listW - 6, rowY + ENTRY_HEIGHT, rowBg);

        boolean hover = mouseX >= this.listX && mouseX < this.listX + this.listW - 6
                && mouseY >= rowY && mouseY < rowY + ENTRY_HEIGHT;
        if (hover) ctx.fill(this.listX, rowY, this.listX + this.listW - 6, rowY + ENTRY_HEIGHT, 0x30FFFFFF);

        ResourceLocation skin = entry.getSkinLocation();
        if (skin != null) {
            ctx.blit(skin, this.listX + 3, rowY + 3, 16, 16, 8, 8, 8, 8, 64, 64);
            ctx.blit(skin, this.listX + 3, rowY + 3, 16, 16, 40, 8, 8, 8, 64, 64);
        }

        if (!isSelf) {
            String mark = inList ? "✔" : "✘";
            ctx.drawString(this.font, Component.literal(mark), this.listX + 24, rowY + 7, 0xFFFFFFFF);
        }

        String name = profile.getName() != null ? profile.getName() : "?";
        int nameColor = isSelf ? 0xFF808080 : (inList ? 0xFF55FF55 : 0xFFFFAAAA);
        ctx.drawString(this.font, name, this.listX + 36, rowY + 4, nameColor);

        String sub;
        if (isSelf) {
            sub = i18nStr("screen.ssc_addon.whitelist.entry.self");
        } else {
            double distance = distanceTo(entry);
            sub = distance == Double.MAX_VALUE
                    ? i18nStr("screen.ssc_addon.whitelist.entry.offline")
                    : i18nStr("screen.ssc_addon.whitelist.entry.distance",
                            String.format(Locale.ROOT, "%.1f", Math.sqrt(distance)));
        }
        ctx.drawString(this.font, Component.literal(sub).withStyle(ChatFormatting.GRAY),
                this.listX + 36, rowY + 13, 0xFFAAAAAA);

        if (!isSelf && uuid != null) {
            int buttonX = this.listX + this.listW - 30;
            int buttonY = rowY + 3;
            boolean buttonHover = mouseX >= buttonX && mouseX < buttonX + 22 && mouseY >= buttonY && mouseY < buttonY + 16;
            int bg = buttonHover
                    ? (inList ? 0xFFAA3030 : 0xFF30AA30)
                    : (inList ? 0xFF802020 : 0xFF208020);
            ctx.fill(buttonX, buttonY, buttonX + 22, buttonY + 16, bg);
            drawBorder(ctx, buttonX, buttonY, 22, 16, 0xFF000000);
            String label = inList ? "-" : "+";
            ctx.drawCenteredString(this.font, label, buttonX + 11, buttonY + 4, 0xFFFFFFFF);
        }
    }

    private void renderMobEntry(GuiGraphics ctx, MobEntry entry, int rowY, int mouseX, int mouseY) {
        ctx.fill(this.listX, rowY, this.listX + this.listW - 6, rowY + ENTRY_HEIGHT, 0x4030FF30);

        boolean hover = mouseX >= this.listX && mouseX < this.listX + this.listW - 6
                && mouseY >= rowY && mouseY < rowY + ENTRY_HEIGHT;
        if (hover) ctx.fill(this.listX, rowY, this.listX + this.listW - 6, rowY + ENTRY_HEIGHT, 0x30FFFFFF);

        ItemStack egg = tryGetSpawnEgg(entry.typeId());
        if (!egg.isEmpty()) {
            ctx.renderItem(egg, this.listX + 3, rowY + 3);
        } else {
            ctx.fill(this.listX + 3, rowY + 3, this.listX + 19, rowY + 19, 0xFF303030);
            ctx.drawCenteredString(this.font, "?", this.listX + 11, rowY + 7, 0xFFAAAAAA);
        }

        ctx.drawString(this.font, Component.literal("✔"), this.listX + 24, rowY + 7, 0xFF55FF55);

        Component speciesName = resolveSpeciesName(entry);
        Integer index = this.mobIndexInSpecies.get(entry.uuid());
        String label = speciesName.getString() + (index != null ? " #" + index : "");
        ctx.drawString(this.font, label, this.listX + 36, rowY + 4, 0xFF55FF55);

        String shortUuid = entry.uuid().toString().substring(0, 8);
        ctx.drawString(this.font, Component.literal(shortUuid).withStyle(ChatFormatting.DARK_GRAY),
                this.listX + 36, rowY + 13, 0xFF606060);

        int buttonX = this.listX + this.listW - 30;
        int buttonY = rowY + 3;
        boolean buttonHover = mouseX >= buttonX && mouseX < buttonX + 22 && mouseY >= buttonY && mouseY < buttonY + 16;
        ctx.fill(buttonX, buttonY, buttonX + 22, buttonY + 16, buttonHover ? 0xFFAA3030 : 0xFF802020);
        drawBorder(ctx, buttonX, buttonY, 22, 16, 0xFF000000);
        ctx.drawCenteredString(this.font, "-", buttonX + 11, buttonY + 4, 0xFFFFFFFF);
    }

    private ItemStack tryGetSpawnEgg(String typeId) {
        if (typeId == null || typeId.isEmpty()) return ItemStack.EMPTY;
        try {
            ResourceLocation id = new ResourceLocation(typeId);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type == null) return ItemStack.EMPTY;
            SpawnEggItem egg = SpawnEggItem.byId(type);
            if (egg == null) return ItemStack.EMPTY;
            return new ItemStack(egg);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private Component resolveSpeciesName(MobEntry entry) {
        if (entry.typeId() != null) {
            try {
                ResourceLocation id = new ResourceLocation(entry.typeId());
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                if (type != null) return type.getDescription();
            } catch (Exception ignored) {
            }
        }
        return Component.translatable("screen.ssc_addon.whitelist.entry.unknown_species");
    }

    private String i18nStr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listH = LIST_VISIBLE_ROWS * ENTRY_HEIGHT;
        if (mouseX >= this.listX && mouseX < this.listX + this.listW
                && mouseY >= this.listY && mouseY < this.listY + listH) {
            int row = (int) ((mouseY - this.listY) / ENTRY_HEIGHT) + this.scrollOffset;
            int buttonX = this.listX + this.listW - 30;
            int buttonY = this.listY + (row - this.scrollOffset) * ENTRY_HEIGHT + 3;
            boolean hitButton = mouseX >= buttonX && mouseX < buttonX + 22
                    && mouseY >= buttonY && mouseY < buttonY + 16;
            if (this.currentTab == Tab.ALL_PLAYERS) {
                List<PlayerInfo> entries = getFilteredEntries();
                if (row >= 0 && row < entries.size()) {
                    UUID uuid = entries.get(row).getProfile().getId();
                    if (uuid != null && this.minecraft != null && this.minecraft.player != null
                            && !uuid.equals(this.minecraft.player.getUUID()) && hitButton) {
                        if (this.whitelist.contains(uuid)) sendRemove(uuid);
                        else sendAdd(uuid);
                        return true;
                    }
                }
            } else if (this.currentTab == Tab.PLAYER_WL) {
                List<UUID> entries = getPlayerWhitelistEntries();
                if (row >= 0 && row < entries.size() && hitButton) {
                    sendRemove(entries.get(row));
                    return true;
                }
            } else {
                if (row >= 0 && row < this.mobs.size() && hitButton) {
                    sendMobRemove(this.mobs.get(row).uuid());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int size = getCurrentListSize();
        int maxOffset = Math.max(0, size - LIST_VISIBLE_ROWS);
        this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.signum(amount), 0, maxOffset);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused()) {
            return this.searchField.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.uuidField != null && this.uuidField.isFocused()) {
            return this.uuidField.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused()) return this.searchField.charTyped(chr, modifiers);
        if (this.uuidField != null && this.uuidField.isFocused()) return this.uuidField.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    private void tryAddByUuidInput() {
        String input = this.uuidField.getValue().trim();
        if (input.isEmpty()) return;
        UUID uuid = parseUuidLoose(input);
        if (uuid == null) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.translatable("screen.ssc_addon.whitelist.uuid_input.invalid", input)
                                .withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        sendAdd(uuid);
        this.uuidField.setValue("");
    }

    private static UUID parseUuidLoose(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            String hex = value.replace("-", "");
            if (hex.length() == 32) {
                try {
                    String dashed = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-"
                            + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
                    return UUID.fromString(dashed);
                } catch (IllegalArgumentException ignored2) {
                    return null;
                }
            }
            return null;
        }
    }

    private void sendAdd(UUID uuid) {
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeUUID(uuid);
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_WHITELIST_GUI_ADD, buf);
    }

    private void sendRemove(UUID uuid) {
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeUUID(uuid);
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_WHITELIST_GUI_REMOVE, buf);
    }

    private void sendMobRemove(UUID uuid) {
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeUUID(uuid);
        AddonNetwork.sendToServer(SscAddonNetworking.PACKET_WHITELIST_GUI_MOB_REMOVE, buf);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
