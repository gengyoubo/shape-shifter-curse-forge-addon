package github.com.gengyoubo.sscfa.balance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * balance 同步层与 Forge 的接线（服务端部分；客户端部分见 {@link BalanceClient}）。
 *
 * <p>服务端：数据包重载的 apply 阶段暂存候选树，整次重载成功后（{@link ServerStartedEvent} 或
 * {@link OnDatapackSyncEvent}）才提交并向在线玩家全量广播；玩家登录时发送，离线清状态，30 秒内未确认则断开。
 * 单人集成服务端与客户端在同一进程，但两端各持独立的状态与快照，靠 {@link #currentSnapshot()} 按逻辑端读取。</p>
 */
public final class BalanceIntegration {
    public static final ResourceLocation PACKET_BALANCE_S2C = new ResourceLocation("my_addon", "balance_sync");
    public static final ResourceLocation PACKET_BALANCE_ACK = new ResourceLocation("my_addon", "balance_ack");
    public static final ResourceLocation PACKET_BALANCE_RESYNC = new ResourceLocation("my_addon", "balance_resync");

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * 和 balance 一起提交、发送的其他同步数据（例如旧法术注册表）。在 balance 提交前发送，所以其 ACK 也排在之后。
     * 全部回调都在服务端主线程（{@link #clearClient()} 在客户端主线程）调用。
     */
    public interface SyncParticipant {
        /** 本次重载失败或被新的重载取代：丢弃暂存的候选。 */
        default void discardPending() {}
        /** 重载成功：发布暂存的候选。 */
        default void commitPending() {}
        /** 服务器停止：回到默认状态。 */
        default void clearServer() {}
        /** 向该玩家发送当前数据（在 balance 序列之前）。 */
        default void sendTo(ServerPlayer player) {}
        /** 客户端断线。 */
        default void clearClient() {}
    }

    private static final List<SyncParticipant> PARTICIPANTS = new CopyOnWriteArrayList<>();

    /** 服务端权威容器。 */
    private static volatile ServerBalanceState server;
    private static volatile BalanceSnapshot publishedServer;
    private static volatile BalanceSnapshot publishedClient;
    private static volatile Thread clientThread;
    private static Map<String, Object> pendingTree;
    private static BalanceSchema activeSchema;
    private static final Map<UUID, Long> syncDeadlines = new HashMap<>();
    private static final Map<UUID, Long> lastResync = new HashMap<>();
    /** 服务端最近一次提交的传输树 JSON（重载后为在线玩家重发）。 */
    private static String serverTreeJson;

    private BalanceIntegration() {}

    /** 当前是否物理客户端；测试环境（没有 FML）按非客户端处理。 */
    public static boolean isPhysicalClient() {
        try {
            return FMLEnvironment.dist == Dist.CLIENT;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void addParticipant(SyncParticipant participant) {
        PARTICIPANTS.add(participant);
    }

    // ==== 服务端 ====

    /** 在模组构造时调用一次（物理客户端也要调用，供集成服务端使用）。 */
    public static synchronized void initServer(BalanceSchema schema) {
        activeSchema = schema;
        resetServer();
        AddonNetwork.clientbound(PACKET_BALANCE_S2C);
        AddonNetwork.registerServerReceiver(PACKET_BALANCE_ACK, BalanceIntegration::onAck);
        AddonNetwork.registerServerReceiver(PACKET_BALANCE_RESYNC, BalanceIntegration::onResync);
        PlayerSessionCleanup.register((connection, srv) -> onPlayerLeft(connection.player));

        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener((AddReloadListenerEvent event) -> event.addListener(new BalanceLoader(new BalanceLoader.Sink() {
            @Override public void onReloadStarted() { discardPending(); }
            @Override public void onLoaded(List<BalanceMerge.Layer> layers) { onLayersLoaded(layers); }
        })));
        bus.addListener((ServerStartedEvent event) -> onServerReload(event.getServer()));
        // 玩家为 null 表示整次 /reload 成功后的全服同步；登录时针对单个玩家的同步由登录事件处理
        bus.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) onServerReload(event.getPlayerList().getServer());
        });
        bus.addListener((ServerStoppedEvent event) -> onServerStopped());
        bus.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) checkSyncDeadlines(event.getServer());
        });
        bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) onPlayerReady(player);
        });
    }

    private static void resetServer() {
        server = new ServerBalanceState(activeSchema);
        server.commitDefaults();
        publishedServer = server.current();
        serverTreeJson = treeJsonOfDefaults();
        pendingTree = null;
    }

    private static synchronized void discardPending() {
        pendingTree = null;
        PARTICIPANTS.forEach(SyncParticipant::discardPending);
    }

    private static synchronized void onServerStopped() {
        resetServer();
        syncDeadlines.clear();
        lastResync.clear();
        PARTICIPANTS.forEach(SyncParticipant::clearServer);
    }

    private static synchronized void checkSyncDeadlines(MinecraftServer srv) {
        if (srv.getTickCount() % 20 != 0 || syncDeadlines.isEmpty()) return;
        long now = System.nanoTime();
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            Long deadline = syncDeadlines.get(player.getUUID());
            if (deadline == null) continue;
            if (isPlayerReady(player)) syncDeadlines.remove(player.getUUID());
            else if (now > deadline) {
                syncDeadlines.remove(player.getUUID());
                player.connection.disconnect(Component.literal("SSCA configuration sync timed out; check client/server mod versions."));
            }
        }
    }

    /** apply 回调：校验并暂存候选树（非法则保留旧快照）；广播在整次重载成功后进行。 */
    private static synchronized void onLayersLoaded(List<BalanceMerge.Layer> layers) {
        if (server == null) return;
        var merged = BalanceMerge.merge(layers);
        BalanceSnapshot.fromTree(activeSchema, merged, "datapack-balance");
        // 提交前先校验传输大小
        BalanceWire.chunkTreeJson(0, GSON.toJson(merged));
        pendingTree = merged;
    }

    /** 整次重载成功（服务端线程）：提交暂存候选并广播给在线玩家。 */
    public static synchronized void onServerReload(MinecraftServer srv) {
        if (server == null) return;
        if (pendingTree != null) {
            server.commitTree(pendingTree, "datapack-balance");
            serverTreeJson = GSON.toJson(pendingTree);
            publishedServer = server.current();
            pendingTree = null;
        }
        PARTICIPANTS.forEach(SyncParticipant::commitPending);
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) sendSequenceTo(player);
    }

    public static synchronized void onPlayerReady(ServerPlayer player) {
        if (server == null) return;
        server.playerJoined(player.getUUID());
        sendSequenceTo(player);
    }

    public static synchronized void onPlayerLeft(ServerPlayer player) {
        if (server != null) server.playerLeft(player.getUUID());
        syncDeadlines.remove(player.getUUID());
        lastResync.remove(player.getUUID());
    }

    private static void sendSequenceTo(ServerPlayer player) {
        if (server == null || serverTreeJson == null) return;
        if (!AddonNetwork.canSend(player, PACKET_BALANCE_S2C)) {
            player.connection.disconnect(Component.literal("This server requires a compatible SSCA balance sync client."));
            return;
        }
        syncDeadlines.putIfAbsent(player.getUUID(), System.nanoTime() + TimeUnit.SECONDS.toNanos(30));
        // 在 balance 提交前排队，所以它们的确认也排在旧法术等数据之后
        PARTICIPANTS.forEach(p -> p.sendTo(player));
        for (BalanceWire.Message m : server.buildSendSequence(player.getUUID(), serverTreeJson)) {
            FriendlyByteBuf buf = AddonNetwork.buffer();
            buf.writeBytes(BalanceWire.encode(m));
            AddonNetwork.sendToPlayer(player, PACKET_BALANCE_S2C, buf);
        }
    }

    /** 客户端确认后标记就绪。 */
    private static synchronized void onAck(ServerPlayer player, FriendlyByteBuf buf) {
        long session = buf.readLong();
        long revision = buf.readLong();
        if (!player.isRemoved() && server != null) server.playerAcked(player.getUUID(), session, revision);
    }

    /** 客户端请求重发（2 秒内最多一次）。 */
    private static synchronized void onResync(ServerPlayer player, FriendlyByteBuf buf) {
        if (server == null || player.isRemoved()) return;
        long now = System.nanoTime();
        Long previous = lastResync.get(player.getUUID());
        if (previous != null && now - previous < TimeUnit.SECONDS.toNanos(2)) return;
        lastResync.put(player.getUUID(), now);
        sendSequenceTo(player);
    }

    // ==== 客户端状态（由 BalanceClient 维护） ====

    static void bindClientThread(Thread thread) {
        clientThread = thread;
    }

    static void publishClient(BalanceSnapshot snapshot) {
        publishedClient = snapshot;
    }

    static List<SyncParticipant> participants() {
        return PARTICIPANTS;
    }

    /** 客户端当前镜像（未就绪为 null）。 */
    public static BalanceSnapshot clientSnapshot() {
        return publishedClient;
    }

    // ==== 读取 ====

    /** 服务端当前权威快照（未初始化为 null，调用方须自行回退）。 */
    public static BalanceSnapshot serverSnapshot() {
        return publishedServer;
    }

    /** 按逻辑端读取（集成服务端在客户端进程里也读服务端快照），不加锁。 */
    public static boolean isClientThread() { return Thread.currentThread() == clientThread; }

    public static BalanceSnapshot currentSnapshot() { return isClientThread() ? publishedClient : publishedServer; }

    public static boolean isPlayerReady(ServerPlayer player) {
        return server == null || server.isPlayerReady(player.getUUID());
    }

    /** 服务端当前配置代数（重载/提交会递增；用来检测「配置变了需要重算」）。 */
    public static synchronized long serverRevision() {
        return server == null ? 0 : server.revision();
    }

    /** 双端便捷读取，快照缺失时回退 def。 */
    public static double balanceDouble(String scope, String param, double def) {
        var s = currentSnapshot();
        return s == null ? def : s.getDouble(scope, param);
    }

    private static String treeJsonOfDefaults() {
        return GSON.toJson(Map.of("schema_version", BalanceSchema.CURRENT_SCHEMA_VERSION));
    }
}
