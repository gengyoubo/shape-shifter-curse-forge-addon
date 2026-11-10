package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.CountdownPrediction;
import github.com.gengyoubo.sscfa.network.CountdownSync;
import github.com.gengyoubo.sscfa.network.CountdownValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 客户端倒计时预测：按服务端时间锚在本地推算每 tick 递减的资源值。 */
public final class CountdownClient {
    private record Prediction(LivingEntity owner, CountdownValue clock) {}

    private static final Map<CountdownSync.Key, Prediction> VALUES = new HashMap<>();
    private static final Map<CountdownSync.Key, CountdownSync.State> PENDING = new HashMap<>();
    private static ClientLevel world;
    private static long received;

    private CountdownClient() {
    }

    public static void register() {
        CountdownPrediction.setProvider(CountdownClient::predicted);
        AddonNetwork.registerClientReceiver(CountdownSync.STATE, buf -> {
            ResourceLocation dimension = buf.readResourceLocation();
            long time = buf.readLong();
            int count = Math.max(0, Math.min(4096, buf.readVarInt()));
            ArrayList<CountdownSync.State> incoming = new ArrayList<>(count);
            for (int i = 0; i < count; i++) incoming.add(CountdownSync.State.read(buf, time));

            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            if (level == null || !level.dimension().location().equals(dimension)) return;
            if (world != level) clear();
            world = level;
            received = Math.max(time, level.getGameTime());
            VALUES.clear();
            PENDING.clear();
            for (CountdownSync.State state : incoming) PENDING.put(state.key(), state);
            bindPending();
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick();
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> clear());
    }

    private static void onClientTick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != world) {
            clear();
            return;
        }
        if (world == null) return;
        bindPending();
        VALUES.values().removeIf(prediction -> prediction.owner().isRemoved()
                || prediction.owner().level() != world);
        if (world.getGameTime() - received > 60) {
            VALUES.clear();
            PENDING.clear();
        }
    }

    private static void bindPending() {
        if (world == null) return;
        var iterator = PENDING.values().iterator();
        while (iterator.hasNext()) {
            CountdownSync.State state = iterator.next();
            var entity = world.getEntity(state.entityId());
            if (!(entity instanceof LivingEntity living) || !living.getUUID().equals(state.key().owner())) {
                continue;
            }
            VALUES.put(state.key(), new Prediction(living, state.clock()));
            iterator.remove();
        }
    }

    public static Integer predicted(Player player, ResourceLocation resourceId) {
        if (player == null || resourceId == null) return null;
        if (VALUES.isEmpty() || world == null || player.level() != world) return null;
        Prediction prediction = VALUES.get(new CountdownSync.Key(player.getUUID(), resourceId));
        if (prediction == null || prediction.owner() != player) return null;
        return prediction.clock().at(world.getGameTime());
    }

    private static void clear() {
        VALUES.clear();
        PENDING.clear();
        world = null;
    }
}
