package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * 玩家离线清理：每个连接只执行一次，在服务端主线程、玩家数据保存之前调用。
 *
 * <p>Forge 的 {@link PlayerEvent.PlayerLoggedOutEvent} 在 {@code PlayerList.remove} 开头、保存玩家数据之前触发；
 * 关服时 {@link ServerStoppingEvent} 早于保存全部玩家，这里先对所有在线玩家执行一次（之后的登出事件会被去重）。</p>
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class PlayerSessionCleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger("ssc_addon/session_cleanup");
    private static final List<BiConsumer<ServerGamePacketListenerImpl, MinecraftServer>> CALLBACKS = new CopyOnWriteArrayList<>();
    private static final Set<ServerGamePacketListenerImpl> CLEANED = Collections.newSetFromMap(new WeakHashMap<>());

    private PlayerSessionCleanup() {}

    /** 注册离线清理回调（按注册顺序执行，单个回调抛异常不影响其余回调）。 */
    public static void register(BiConsumer<ServerGamePacketListenerImpl, MinecraftServer> callback) {
        CALLBACKS.add(callback);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.connection != null) {
            disconnect(player.connection, player.server);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (player.connection != null) disconnect(player.connection, server);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CLEANED.clear();
    }

    public static void disconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        if (!server.isSameThread()) {
            server.execute(() -> disconnect(handler, server));
            return;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(handler.player.getUUID());
        if (online != null && online.connection != handler) return;
        if (!CLEANED.add(handler)) return;
        for (var callback : CALLBACKS) {
            try {
                callback.accept(handler, server);
            } catch (RuntimeException exception) {
                LOGGER.error("Player cleanup failed for {} in {}; continuing remaining cleanup",
                        handler.player.getUUID(), callback.getClass().getName(), exception);
            }
        }
    }
}
