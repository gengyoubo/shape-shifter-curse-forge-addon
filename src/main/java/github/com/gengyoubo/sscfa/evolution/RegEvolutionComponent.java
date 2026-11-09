package github.com.gengyoubo.sscfa.evolution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import github.com.gengyoubo.sscfa.network.AddonNetwork;

/** 进化数据组件访问（Forge 版）：SavedData 持久?+ AddonNetwork 同步?*/
public final class RegEvolutionComponent {
    public static final ResourceLocation SYNC = new ResourceLocation("ssc_addon", "evolution_sync");
    private static final String KEY = "ssc_addon_evolution";
    public static final Access EVOLUTION = new Access();

    public static final class Access {
        private final Map<UUID, EvolutionComponent> CACHE = new HashMap<>();

        public EvolutionComponent get(Player player) {
            return CACHE.computeIfAbsent(player.getUUID(), id -> {
                EvolutionComponent component = new EvolutionComponent();
                if (!player.level().isClientSide() && player.getServer() != null) {
                    CompoundTag tag = EvolutionSavedData.get(player.getServer()).get(id);
                    if (tag != null) component.readFromNbt(tag);
                }
                return component;
            });
        }

        public void save(Player player) {
            if (player.level().isClientSide() || player.getServer() == null) return;
            EvolutionComponent component = CACHE.get(player.getUUID());
            if (component == null) return;
            CompoundTag tag = new CompoundTag();
            component.writeToNbt(tag);
            EvolutionSavedData data = EvolutionSavedData.get(player.getServer());
            data.put(player.getUUID(), tag);
            data.setDirty();
        }

        public void sync(ServerPlayer player) {
            save(player);
            if (!AddonNetwork.canSend(player, SYNC)) return;
            EvolutionComponent component = CACHE.get(player.getUUID());
            if (component == null) return;
            CompoundTag tag = new CompoundTag();
            component.writeToNbt(tag);
            var buf = AddonNetwork.buffer();
            buf.writeUUID(player.getUUID());
            buf.writeNbt(tag);
            AddonNetwork.sendToPlayer(player, SYNC, buf);
        }

        public void clear(Player player) {
            CACHE.remove(player.getUUID());
        }

        public void acceptClient(UUID player, CompoundTag tag) {
            EvolutionComponent component = new EvolutionComponent();
            if (tag != null) component.readFromNbt(tag);
            CACHE.put(player, component);
        }
    }

    private RegEvolutionComponent() {}

    /** 玩家 UUID -> 进化 NBT?*/
    public static final class EvolutionSavedData extends SavedData {
        private final Map<UUID, CompoundTag> data = new HashMap<>();

        public static EvolutionSavedData get(MinecraftServer server) {
            DimensionDataStorage storage = server.overworld().getDataStorage();
            return storage.computeIfAbsent(EvolutionSavedData::load, EvolutionSavedData::new, KEY);
        }

        private static EvolutionSavedData load(CompoundTag nbt) {
            EvolutionSavedData out = new EvolutionSavedData();
            for (String key : nbt.getAllKeys()) {
                try {
                    out.data.put(UUID.fromString(key), nbt.getCompound(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return out;
        }

        public CompoundTag get(UUID player) { return data.get(player); }
        public void put(UUID player, CompoundTag tag) { data.put(player, tag); }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            data.forEach((id, tag) -> nbt.put(id.toString(), tag));
            return nbt;
        }
    }
}
