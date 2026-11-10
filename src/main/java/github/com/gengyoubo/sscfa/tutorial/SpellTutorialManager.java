package github.com.gengyoubo.sscfa.tutorial;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.research.RuneRole;
import github.com.gengyoubo.sscfa.spell.research.WorldRuneState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 新世界按玩家播放一次法术教程；只确认真实显示后才记录观看。 */
public final class SpellTutorialManager {
    public static final ResourceLocation OFFER = new ResourceLocation("ssc_addon", "spell_tutorial_offer");
    public static final ResourceLocation ACTION = new ResourceLocation("ssc_addon", "spell_tutorial_action");

    private record Pending(ServerPlayer player, UUID nonce) {}

    private record Delivery(ServerPlayer player, int until) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> REQUESTED = new HashMap<>();
    private static final Map<MinecraftServer, Boolean> FRESH = new HashMap<>();
    private static final Map<UUID, Delivery> JOIN_QUEUE = new HashMap<>();
    private static boolean registered;

    private SpellTutorialManager() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) ->
                FRESH.put(event.getServer(), !event.getServer().getWorldData().overworldData().isInitialized()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> initializePolicy(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            FRESH.remove(event.getServer());
            PENDING.clear();
            REQUESTED.clear();
            JOIN_QUEUE.clear();
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                JOIN_QUEUE.put(player.getUUID(), new Delivery(player, player.getServer().getTickCount() + 200));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(event.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            PENDING.remove(player.getUUID());
            JOIN_QUEUE.remove(player.getUUID());
            REQUESTED.remove(player.getUUID());
        });
        AddonNetwork.registerServerReceiver(ACTION, (player, buf) -> {
            if (buf.readableBytes() < 1) return;
            int action = buf.readUnsignedByte();
            if (action == 0 && !buf.isReadable()) offer(player, false);
            else if (action == 1 && buf.readableBytes() == 16) acknowledge(player, buf.readUUID());
        });
    }

    private static void initializePolicy(MinecraftServer server) {
        SpellTutorialState state = SpellTutorialState.get(server);
        if (state.policyInitialized()) return;
        state.initializePolicy(FRESH.getOrDefault(server, false));
    }

    private static void tick(MinecraftServer server) {
        if (JOIN_QUEUE.isEmpty()) return;
        for (var entry : java.util.List.copyOf(JOIN_QUEUE.entrySet())) {
            Delivery delivery = entry.getValue();
            ServerPlayer player = delivery.player();
            if (player.getServer() != server) continue;
            SpellTutorialState state = SpellTutorialState.get(server);
            if (server.getPlayerList().getPlayer(entry.getKey()) != player
                    || server.getTickCount() > delivery.until()
                    || !state.autoEnabled() || state.hasSeen(entry.getKey())) {
                JOIN_QUEUE.remove(entry.getKey(), delivery);
                continue;
            }
            if (AddonNetwork.canSend(player, OFFER)) {
                offer(player, true);
                JOIN_QUEUE.remove(entry.getKey(), delivery);
            }
        }
    }

    public static boolean offer(ServerPlayer player, boolean automatic) {
        MinecraftServer server = player.getServer();
        if (server == null || server.getPlayerList().getPlayer(player.getUUID()) != player
                || !AddonNetwork.canSend(player, OFFER)) return false;
        initializePolicy(server);
        SpellTutorialState state = SpellTutorialState.get(server);
        if (automatic && (!state.autoEnabled() || state.hasSeen(player.getUUID()))) return false;
        int now = server.getTickCount();
        Integer last = REQUESTED.get(player.getUUID());
        if (!automatic && last != null && now >= last && now - last < 20) return false;
        REQUESTED.put(player.getUUID(), now);
        UUID nonce = UUID.randomUUID();
        PENDING.put(player.getUUID(), new Pending(player, nonce));
        WorldRuneState language = WorldRuneState.get(server);
        FriendlyByteBuf packet = AddonNetwork.buffer();
        packet.writeUUID(nonce);
        packet.writeUUID(language.worldId());
        packet.writeBoolean(automatic);
        for (RuneRole role : new RuneRole[]{RuneRole.SOURCE, RuneRole.GAIN, RuneRole.STABLE}) {
            packet.writeByte(language.language().glyph(role));
        }
        AddonNetwork.sendToPlayer(player, OFFER, packet);
        return true;
    }

    public static boolean acknowledge(ServerPlayer player, UUID nonce) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.player() != player || !pending.nonce().equals(nonce)
                || player.getServer().getPlayerList().getPlayer(player.getUUID()) != player) return false;
        SpellTutorialState.get(player.getServer()).markSeen(player.getUUID());
        PENDING.remove(player.getUUID());
        return true;
    }
}
