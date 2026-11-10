package github.com.gengyoubo.sscfa.spell.research;

import github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class FormationResearchNetworking {
    public static final ResourceLocation ACTION = new ResourceLocation("ssc_addon", "formation_research_action"), STATE = new ResourceLocation("ssc_addon", "formation_research_state");
    private static final Map<UUID, Long> LAST = new HashMap<>();
    public static void register() {
        github.com.gengyoubo.sscfa.network.AddonNetwork.registerServerReceiver(ACTION, (player, buf) -> {
            if (buf.readableBytes() > 32768) return;
            final int syncId; final CompoundTag request;
            try { syncId = buf.readVarInt(); request = buf.readNbt(); if (request == null || buf.isReadable()) return; }
            catch (RuntimeException malformed) { return; }
            long now = System.nanoTime(); long prior = LAST.getOrDefault(player.getUUID(), 0L);
            int action = request.getInt("Action");
            if (action < SlottedResearchManager.OPEN || action > SlottedResearchManager.INSPECT) return;
            boolean limited = action == SlottedResearchManager.TEST || action == SlottedResearchManager.COMPLETE;
            if (limited && now - prior < 150_000_000L) {
                if (SlottedResearchManager.context(player, syncId) != null) send(player, syncId, request, SlottedResearchManager.text("wait"));
                return;
            }
            if (limited) LAST.put(player.getUUID(), now);
            SlottedResearchManager.handle(player, syncId, request);
        });
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> LAST.remove(handler.player.getUUID()));
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> LAST.clear());
    }
    private static void send(ServerPlayer p, int syncId, CompoundTag request, Component message) {
        if (!github.com.gengyoubo.sscfa.network.AddonNetwork.canSend(p, STATE)) return;
        var world = WorldRuneState.get(p.getServer()); var progress = FormationKnowledgeComponent.get(p).research(); progress.bind(world.worldId());
        CompoundTag n = new CompoundTag(); n.putUUID("World", world.worldId());
        n.putBoolean("Slotted",true);n.putInt("Action",request.getInt("Action"));n.putInt("Level",request.getInt("Level"));n.putInt("Revision",request.getInt("Revision"));
        if(request.hasUUID("Request"))n.putUUID("Request",request.getUUID("Request"));
        n.put("Progress",progress.write());n.putUUID("Operation",progress.completionToken);
        var buf=github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();buf.writeVarInt(syncId);buf.writeNbt(n);buf.writeComponent(message);buf.writeBoolean(false);buf.writeVarInt(-1);buf.writeVarInt(-1);buf.writeVarInt(0);github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(p,STATE,buf);
    }
}
