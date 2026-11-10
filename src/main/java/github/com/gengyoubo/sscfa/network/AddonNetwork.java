package github.com.gengyoubo.sscfa.network;

import github.com.gengyoubo.sscfa.SscAddonForge;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 附属的网络层：一个 Forge {@link SimpleChannel}，里面传「通道 ID + 原始字节」，保留 Fabric 版按 {@link ResourceLocation}
 * 分通道、用 {@link FriendlyByteBuf} 读写的写法，移植时只需把 {@code ServerPlayNetworking.send(...)} 换成 {@link #sendToPlayer} 等。
 *
 * <ul>
 *   <li>服务端接收器（客户端 → 服务端）用 {@link #registerServerReceiver} 注册，在公共代码里注册。</li>
 *   <li>客户端接收器（服务端 → 客户端）用 {@link #registerClientReceiver} 注册，只能在客户端代码里注册；
 *       对应通道要先在公共代码里用 {@link #clientbound} 声明，{@link #canSend} 才会返回 true。</li>
 *   <li>两种接收器都在主线程（服务端主线程 / 客户端渲染线程）执行，数据已复制，可以安全地延后读取。</li>
 * </ul>
 */
public final class AddonNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("ssc_addon/network");
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SscAddonForge.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    @FunctionalInterface
    public interface ServerReceiver {
        void receive(ServerPlayer player, FriendlyByteBuf buf);
    }

    @FunctionalInterface
    public interface ClientReceiver {
        void receive(FriendlyByteBuf buf);
    }

    private static final Map<ResourceLocation, ServerReceiver> SERVER_RECEIVERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ClientReceiver> CLIENT_RECEIVERS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> CLIENTBOUND = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    private AddonNetwork() {}

    /** 在模组构造时调用一次。 */
    public static void init() {
        // 两个方向必须用不同的消息类：SimpleChannel 编码时按「消息类」查编号，同一个类注册两次，
        // 后注册的会覆盖前者，客户端发往服务端的包就会带上 PLAY_TO_CLIENT 的编号，
        // 服务端校验方向失败后直接踢人（Illegal packet received, terminating connection）。
        CHANNEL.messageBuilder(Serverbound.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, out) -> message.payload().encode(out))
                .decoder(in -> new Serverbound(Payload.decode(in)))
                .consumerMainThread((message, context) -> handleServerbound(message.payload(), context)).add();
        CHANNEL.messageBuilder(Clientbound.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, out) -> message.payload().encode(out))
                .decoder(in -> new Clientbound(Payload.decode(in)))
                .consumerMainThread((message, context) -> handleClientbound(message.payload(), context)).add();
    }

    // ===== 注册 =====

    public static void registerServerReceiver(ResourceLocation id, ServerReceiver receiver) {
        if (SERVER_RECEIVERS.putIfAbsent(id, receiver) != null) throw new IllegalStateException("Duplicate server receiver " + id);
    }

    /** 声明一个服务端 → 客户端通道：本版本的客户端代码会注册它的接收器。 */
    public static void clientbound(ResourceLocation id) {
        CLIENTBOUND.add(id);
    }

    /** 只在客户端调用。 */
    public static void registerClientReceiver(ResourceLocation id, ClientReceiver receiver) {
        if (CLIENT_RECEIVERS.putIfAbsent(id, receiver) != null) throw new IllegalStateException("Duplicate client receiver " + id);
        CLIENTBOUND.add(id);
    }

    // ===== 发送 =====

    public static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    /** 该玩家的客户端能否接收此通道（装了本模组且此通道已声明）。 */
    public static boolean canSend(ServerPlayer player, ResourceLocation id) {
        return CLIENTBOUND.contains(id) && player.connection != null && CHANNEL.isRemotePresent(player.connection.connection);
    }

    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Clientbound(new Payload(id, buf)));
    }

    /** 发给正在追踪该玩家的所有客户端和该玩家自己（旁人也要看到的表现状态用）。 */
    public static void sendToTrackingAndSelf(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new Clientbound(new Payload(id, buf)));
    }

    /** 只在客户端调用。 */
    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        CHANNEL.sendToServer(new Serverbound(new Payload(id, buf)));
    }

    // ===== 分发 =====

    private static void handleServerbound(Payload payload, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) return;
        ServerReceiver receiver = SERVER_RECEIVERS.get(payload.id());
        if (receiver == null) {
            if (WARNED.add(payload.id())) LOGGER.warn("No server receiver for {} (from {})", payload.id(), player.getGameProfile().getName());
            return;
        }
        receiver.receive(player, payload.data());
    }

    private static void handleClientbound(Payload payload, Supplier<NetworkEvent.Context> context) {
        ClientReceiver receiver = CLIENT_RECEIVERS.get(payload.id());
        if (receiver == null) {
            if (WARNED.add(payload.id())) LOGGER.warn("No client receiver for {}", payload.id());
            return;
        }
        receiver.receive(payload.data());
    }

    /** 客户端 → 服务端的消息类型。 */
    record Serverbound(Payload payload) {}

    /** 服务端 → 客户端的消息类型。 */
    record Clientbound(Payload payload) {}

    /** 通道 ID + 数据；解码时把字节复制出来，接收器可以在主线程安全读取。 */
    record Payload(ResourceLocation id, FriendlyByteBuf data) {
        void encode(FriendlyByteBuf out) {
            out.writeResourceLocation(id);
            out.writeVarInt(data.readableBytes());
            out.writeBytes(data, data.readerIndex(), data.readableBytes());
        }

        static Payload decode(FriendlyByteBuf in) {
            ResourceLocation id = in.readResourceLocation();
            int length = in.readVarInt();
            byte[] bytes = new byte[length];
            in.readBytes(bytes);
            return new Payload(id, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
        }
    }
}
