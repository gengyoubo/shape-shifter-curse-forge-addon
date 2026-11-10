package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.entity.FrostThornEntity;
import github.com.gengyoubo.sscfa.init.AddonParticles;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 寒棘狐主技能蓄力粒子客户端自算：服务端只发蓄力开始/结束事件，
 * 各客户端按同步的冰锥槽位在即将生成的冰锥位置复现汇聚冰晶（每 4t 18 颗）。
 */
public final class FrostSpikeChargeClientState {

    private static final Map<UUID, Boolean> CHARGING = new ConcurrentHashMap<>();

    private FrostSpikeChargeClientState() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_FROST_SPIKE_CHARGE_STATE, buf -> {
            UUID playerId = buf.readUUID();
            setCharging(playerId, buf.readBoolean());
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) onClientTick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> CHARGING.clear());
    }

    public static void setCharging(UUID playerId, boolean charging) {
        if (charging) {
            CHARGING.put(playerId, Boolean.TRUE);
        } else {
            CHARGING.remove(playerId);
        }
    }

    public static void clearAll() {
        CHARGING.clear();
    }

    private static void onClientTick(Minecraft client) {
        ClientLevel world = client.level;
        if (world == null || CHARGING.isEmpty()) return;
        if (world.getGameTime() % 4 != 0) return;
        for (UUID id : CHARGING.keySet()) {
            Player player = world.getPlayerByUUID(id);
            if (player == null) continue;
            if (!FormUtils.isForm(player, FormIdentifiers.SNOW_FOX_FROSTSPINE)) continue;
            AABB scan = player.getBoundingBox().inflate(4.0);
            int nextIndex = firstEmptySlot(world, scan, id);
            if (nextIndex < 0) nextIndex = oldestSlot(world, scan, id);
            if (nextIndex < 0) continue;
            Vec3 center = FrostThornEntity.hoverTarget(player, nextIndex);
            for (int i = 0; i < 18; i++) {
                double u = world.random.nextDouble() * 2 - 1;
                double theta = world.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(1 - u * u);
                double dx = r * Math.cos(theta);
                double dy = u;
                double dz = r * Math.sin(theta);
                double speed = 1.0 / 20.0;
                world.addParticle(AddonParticles.INWARD_ICE.get(),
                        center.x + dx, center.y + dy, center.z + dz,
                        -dx * speed, -dy * speed, -dz * speed);
            }
        }
    }

    private static int firstEmptySlot(ClientLevel world, AABB scan, UUID ownerId) {
        boolean[] used = new boolean[5];
        for (FrostThornEntity thorn : world.getEntitiesOfClass(FrostThornEntity.class,
                scan, e -> e.isHover() && ownerId.equals(e.getOwnerUuid().orElse(null)))) {
            int slot = thorn.getSlot();
            if (slot >= 0 && slot < 5) used[slot] = true;
        }
        for (int i = 0; i < 5; i++) {
            if (!used[i]) return i;
        }
        return -1;
    }

    private static int oldestSlot(ClientLevel world, AABB scan, UUID ownerId) {
        int index = -1;
        int max = -1;
        for (FrostThornEntity thorn : world.getEntitiesOfClass(FrostThornEntity.class,
                scan, e -> e.isHover() && ownerId.equals(e.getOwnerUuid().orElse(null)))) {
            if (thorn.tickCount > max) {
                max = thorn.tickCount;
                index = thorn.getSlot();
            }
        }
        return index;
    }
}
