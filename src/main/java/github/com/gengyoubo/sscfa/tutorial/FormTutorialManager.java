package github.com.gengyoubo.sscfa.tutorial;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import github.com.gengyoubo.sscfa.util.FormUtils;

/** The client asks only with its book open; a displayed tutorial or an OS browser handoff records it. */
public final class FormTutorialManager {
    public static final ResourceLocation REQUEST = new ResourceLocation("ssc_addon", "form_tutorial_request");
    public static final ResourceLocation OFFER = new ResourceLocation("ssc_addon", "form_tutorial_offer");
    public static final ResourceLocation ACTION = new ResourceLocation("ssc_addon", "form_tutorial_action");
    private record Pending(ServerPlayer player, UUID nonce, ResourceLocation form) { }
    private record RequestKey(UUID player, boolean manual) { }
    private record Request(ServerPlayer player, ResourceLocation form, int tick) { }
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<RequestKey, Request> REQUESTED = new HashMap<>();
    private static boolean registered;

    private FormTutorialManager() { }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            var server = event.getServer();
            PENDING.values().removeIf(pending -> pending.player.getServer() == server);
            REQUESTED.values().removeIf(request -> request.player.getServer() == server);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            UUID id = player.getUUID();
            var pending = PENDING.get(id);
            if (pending != null && pending.player == player) PENDING.remove(id);
            REQUESTED.values().removeIf(request -> request.player == player);
        });
        AddonNetwork.registerServerReceiver(REQUEST, (player, buf) -> {
            if (buf.readableBytes() < 18 || buf.readableBytes() > 1046) return;
            final UUID requestId;
            final ResourceLocation form;
            final boolean manual;
            try {
                requestId = buf.readUUID();
                form = ResourceLocation.tryParse(buf.readUtf(256));
                manual = buf.readBoolean();
                if (form == null || buf.isReadable()) return;
            } catch (RuntimeException malformed) {
                return;
            }
            offer(player, form, manual, requestId);
        });
        AddonNetwork.registerServerReceiver(ACTION, (player, buf) -> {
            if (buf.readableBytes() != 16) return;
            UUID nonce = buf.readUUID();
            acknowledge(player, nonce);
        });
    }

    public static boolean offer(ServerPlayer player, ResourceLocation requestedForm, boolean manual) {
        return offer(player, requestedForm, manual, UUID.randomUUID());
    }

    private static boolean offer(ServerPlayer player, ResourceLocation requestedForm, boolean manual, UUID requestId) {
        if (!isCurrentSession(player)) return false;
        ResourceLocation form = FormIdMigration.canonical(requestedForm);
        if (!matchesForm(player, form) || !AddonNetwork.canSend(player, OFFER)) return false;
        int now = player.getServer().getTickCount();
        RequestKey key = new RequestKey(player.getUUID(), manual);
        Request last = REQUESTED.get(key);
        if (last != null && last.player == player && last.form.equals(form) && now >= last.tick && now - last.tick < 20) return false;
        REQUESTED.put(key, new Request(player, form, now));
        if (!manual && FormTutorialState.get(player.getServer()).hasSeen(player.getUUID(), form)) return false;
        UUID nonce = UUID.randomUUID();
        PENDING.put(player.getUUID(), new Pending(player, nonce, form));
        var packet = AddonNetwork.buffer();
        packet.writeUUID(requestId);
        packet.writeUUID(nonce);
        packet.writeResourceLocation(form);
        packet.writeBoolean(manual);
        AddonNetwork.sendToPlayer(player, OFFER, packet);
        return true;
    }

    public static boolean acknowledge(ServerPlayer player, UUID nonce) {
        if (!isCurrentSession(player)) return false;
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.player != player || !pending.nonce.equals(nonce)) return false;
        if (!matchesForm(player, pending.form)) {
            PENDING.remove(player.getUUID(), pending);
            return false;
        }
        FormTutorialState.get(player.getServer()).markSeen(player.getUUID(), pending.form);
        PENDING.remove(player.getUUID(), pending);
        return true;
    }

    private static boolean isCurrentSession(ServerPlayer player) {
        return player != null && player.getServer() != null
                && player.getServer().getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static boolean matchesForm(ServerPlayer player, ResourceLocation form) {
        if (form == null || !FormTutorialCatalog.supports(form)) return false;
        ResourceLocation current = FormUtils.currentFormId(player);
        return current != null && form.equals(FormIdMigration.canonical(current));
    }
}
