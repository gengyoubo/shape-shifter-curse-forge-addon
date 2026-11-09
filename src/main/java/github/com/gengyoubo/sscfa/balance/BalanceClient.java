package github.com.gengyoubo.sscfa.balance;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * balance 同步的客户端部分：只在物理客户端加载。在客户端主线程消费服务端下发的分片，提交后回 ACK；
 * 非法数据或包过大直接断开（客户端不能带着错误的数值继续玩）。
 */
public final class BalanceClient {
    private static ClientBalanceState client;
    private static final List<Runnable> COMMIT_LISTENERS = new CopyOnWriteArrayList<>();

    private BalanceClient() {}

    /** 客户端镜像每次提交新版本后调用（客户端主线程），例如让依赖 balance 的缓存失效。 */
    public static void onCommitted(Runnable listener) {
        COMMIT_LISTENERS.add(listener);
    }

    /** 在客户端初始化（主线程）时调用一次。 */
    public static synchronized void init(BalanceSchema schema) {
        BalanceIntegration.bindClientThread(Thread.currentThread());
        client = new ClientBalanceState(schema, null);
        AddonNetwork.registerClientReceiver(BalanceIntegration.PACKET_BALANCE_S2C, BalanceClient::receive);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> onDisconnect());
    }

    private static synchronized void receive(FriendlyByteBuf buf) {
        if (client == null) return;
        if (buf.readableBytes() > BalanceWire.MAX_CHUNK_BYTES + 1024) {
            disconnect("SSCA: balance packet too large");
            return;
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        try {
            BalanceWire.Message msg = BalanceWire.decode(bytes);
            ClientBalanceState.Outcome outcome = client.offer(msg);
            if (outcome instanceof ClientBalanceState.Committed committed) {
                BalanceIntegration.publishClient(committed.committed());
                sendAck(committed.sessionId(), committed.revision());
                COMMIT_LISTENERS.forEach(Runnable::run);
            } else if (outcome instanceof ClientBalanceState.Rejected rejected) {
                disconnect("SSCA balance: " + rejected.reason());
            }
        } catch (RuntimeException invalid) {
            disconnect("SSCA balance sync failed: " + invalid.getMessage());
        }
    }

    private static void sendAck(long session, long revision) {
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeLong(session);
        buf.writeLong(revision);
        AddonNetwork.sendToServer(BalanceIntegration.PACKET_BALANCE_ACK, buf);
    }

    private static void disconnect(String reason) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.getConnection().disconnect(Component.literal(reason));
    }

    public static synchronized boolean ready() {
        return client != null && client.isReady();
    }

    private static synchronized void onDisconnect() {
        if (client != null) client.exitConnection();
        BalanceIntegration.publishClient(null);
        BalanceIntegration.participants().forEach(BalanceIntegration.SyncParticipant::clearClient);
    }
}
