package github.com.gengyoubo.sscfa.network;

import github.com.gengyoubo.sscfa.spell.FormCastingStyle;
import github.com.gengyoubo.sscfa.spell.SpellCastManager;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.spell.SpellbookStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Sends deadline changes immediately and a sparse recovery snapshot; never predicts or changes mana. */
public final class SpellbookStatusSync {
    public static final ResourceLocation STATE = new ResourceLocation("ssc_addon", "spellbook_status_v1");
    private static final int HEARTBEAT_TICKS = 100;
    private record Audience(ServerPlayer player, ServerLevel world, SpellbookStatus status, int sent) {}
    private static final Map<UUID, Audience> AUDIENCES = new HashMap<>();

    public record Snapshot(ResourceLocation dimension, int entityId, long time, SpellbookStatus status) {
        public void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(dimension);
            buf.writeVarInt(entityId);
            buf.writeLong(time);
            buf.writeBoolean(status != null);
            if (status != null) {
                buf.writeLong(status.naturalRegenAt());
                buf.writeLong(status.swapReadyAt());
                buf.writeLong(status.castReadyAt());
                buf.writeBoolean(status.casting());
                buf.writeVarInt(status.naturalRegenPerSecond());
            }
        }
        public static Snapshot read(FriendlyByteBuf buf) {
            ResourceLocation dimension = buf.readResourceLocation();
            int entityId = buf.readVarInt();
            long time = buf.readLong();
            SpellbookStatus status = buf.readBoolean()
                    ? new SpellbookStatus(buf.readLong(), buf.readLong(), buf.readLong(),
                    buf.readBoolean(), buf.readVarInt()) : null;
            return new Snapshot(dimension, entityId, time, status);
        }
    }

    private SpellbookStatusSync() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 20 == 0)
                event.getServer().getPlayerList().getPlayers().forEach(SpellbookStatusSync::sync);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) sync(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) sync(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) sync(player);
        });
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((connection, server) -> AUDIENCES.remove(connection.player.getUUID()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> AUDIENCES.clear());
    }

    public static void sync(ServerPlayer player) {
        if (player == null || !AddonNetwork.canSend(player, STATE)) return;
        var book = SpellCastManager.getEquippedBook(player);
        SpellbookStatus status = book == null || book.isEmpty() ? null
                : new SpellbookStatus(FormCastingStyle.getNaturalRegenAt(player),
                FormCastingStyle.getSwapReadyAt(player), SpellChannelManager.getNextCastAt(player),
                SpellChannelManager.isCasting(player), FormCastingStyle.naturalRegenPerSecond(book));
        Audience previous = AUDIENCES.get(player.getUUID());
        if (status == null && previous == null) return;
        int tick = player.getServer().getTickCount();
        if (previous != null && previous.player == player && previous.world == player.level()
                && Objects.equals(previous.status, status) && tick - previous.sent < HEARTBEAT_TICKS) return;
        Snapshot snapshot = new Snapshot(player.level().dimension().location(), player.getId(),
                player.level().getGameTime(), status);
        FriendlyByteBuf buf = AddonNetwork.buffer();
        snapshot.write(buf);
        AddonNetwork.sendToPlayer(player, STATE, buf);
        if (status == null) AUDIENCES.remove(player.getUUID());
        else AUDIENCES.put(player.getUUID(), new Audience(player, player.serverLevel(), status, tick));
    }
}
