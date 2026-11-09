package github.com.gengyoubo.sscfa.cooldown;

import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 冷却状态只发给本人：状态变化立即发，倒计时中每秒校准一次。
 * 同时负责玩家进出时的冷却结算：登录恢复暂停的时钟，离线结算进行中的施放并暂停剩余冷却。
 */
public final class SkillCooldownSync {
    public static final ResourceLocation ID = new ResourceLocation("my_addon", "skill_cooldowns_v1");
    private static final Map<UUID, Snapshot> SENT = new HashMap<>();

    private record Stamp(String skill, int phase, long end, int total) {}

    private record Snapshot(Object player, Object world, Object connection, List<Stamp> stamps) {}

    private SkillCooldownSync() {}

    /** 在模组构造时调用一次。 */
    public static void init() {
        AddonNetwork.clientbound(ID);
        PlayerSessionCleanup.register((connection, server) -> {
            UUID id = connection.player.getUUID();
            SENT.remove(id);
            SkillCastManager manager = SkillCastManager.instanceOrNull();
            if (manager != null && server.overworld() != null) {
                // on_end 未起算的效果立即结算，蓄力中按失败结算，剩余冷却暂停到下次登录
                manager.onPlayerRemoved(id, server.overworld().getGameTime(), ignored -> connection.player);
            }
        });
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
                SkillCastManager.get(player.serverLevel()).onPlayerJoined(player.getUUID(), player.getServer().overworld().getGameTime());
            }
        });
        bus.addListener((ServerStoppedEvent event) -> {
            SENT.clear();
            SkillCastManager.clearServerInstance();
        });
        // 数据包同步后（登录、/reload）重新建立基线
        bus.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() != null) SENT.remove(event.getPlayer().getUUID());
            else SENT.clear();
        });
        bus.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(event.getServer());
        });
    }

    private static void tick(MinecraftServer server) {
        if (server.overworld() == null) return;
        if (server.getTickCount() % 20 == 0) SkillCooldowns.sweep(server);
        SkillCastManager manager = SkillCastManager.get(server.overworld());
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 已连接的玩家不应保留暂停的时钟，即使还没按过技能键
            if (server.getTickCount() % 20 == 0) recoverOnlineClock(manager, player, now);
            if (!AddonNetwork.canSend(player, ID)) continue;
            var views = manager.snapshot(player.getUUID(), now);
            var stamps = views.stream().map(v -> new Stamp(v.skill(), v.phase(),
                    v.remaining() > 0 ? now + v.remaining() : 0, v.total())).toList();
            var previous = SENT.get(player.getUUID());
            boolean unchanged = previous != null && previous.player == player && previous.world == player.level()
                    && previous.connection == player.connection && stamps.equals(previous.stamps);
            // 空列表没有时钟可校准，但切到空列表要发一次；重连、重生、换维度、重载后总是重新建立基线
            if (unchanged && (stamps.isEmpty() || server.getTickCount() % 20 != 0)) continue;
            SENT.put(player.getUUID(), new Snapshot(player, player.level(), player.connection, stamps));
            var buf = AddonNetwork.buffer();
            SkillCooldownPacket.write(buf, views);
            AddonNetwork.sendToPlayer(player, ID, buf);
        }
    }

    private static void recoverOnlineClock(SkillCastManager manager, ServerPlayer player, long now) {
        if (player.connection != null && player.connection.isAcceptingMessages()
                && player.getServer().getPlayerList().getPlayer(player.getUUID()) == player) {
            manager.onPlayerJoined(player.getUUID(), now);
        }
    }
}
