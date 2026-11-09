package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import github.com.gengyoubo.sscfa.compat.ManaUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Restore persistent resources at the end of SSC's form rebuild, before gameplay ticks. */
public final class LoginResourceRestoreHandler {
    private LoginResourceRestoreHandler() { }

    private record Pending(Map<ResourceLocation, Integer> resources, double mana, String manaType) { }
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final java.util.Set<ResourceLocation> OTHER_PERSISTENT = java.util.Set.of(
            new ResourceLocation("my_addon", "form_mancianima_resistance"),
            new ResourceLocation("my_addon", "form_ocelot_nova_nine_lives"),
            new ResourceLocation("my_addon", "form_snow_fox_sp_switch_state"));

    /** Only actual persistent resource bars participate; timers, charge and toggle states do not. */
    public static boolean isPersistentResource(ResourceLocation id) {
        for (var bar : BarKeys.ALL) if (bar.id.equals(id)) return true;
        return OTHER_PERSISTENT.contains(id);
    }

    public static void recordSnapshot(UUID uuid, CompoundTag playerNbt) {
        PENDING.remove(uuid);
        CompoundTag components = playerNbt.getCompound("cardinal_components");
        if (components.isEmpty()) return;
        Map<ResourceLocation, Integer> resources = new HashMap<>();
        ListTag powers = components.getCompound("apoli:powers").getList("Powers", Tag.TAG_COMPOUND);
        for (int i = 0; i < powers.size(); i++) {
            CompoundTag power = powers.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(power.getString("Type"));
            if (id != null && isPersistentResource(id) && power.contains("Data", Tag.TAG_INT)) {
                resources.put(id, power.getInt("Data"));
            }
        }
        CompoundTag mana = components.getCompound("shape-shifter-curse:mana");
        double savedMana = mana.contains("Mana", Tag.TAG_ANY_NUMERIC) ? mana.getDouble("Mana") : -1;
        if (!resources.isEmpty() || savedMana >= 0) {
            PENDING.put(uuid, new Pending(resources, savedMana,
                    mana.contains("ManaTypeID") ? mana.getString("ManaTypeID") : null));
        }
    }

    public static void restore(ServerPlayer player) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null) return;
        for (var entry : pending.resources.entrySet()) {
            if (github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(player, entry.getKey())) {
                PowerUtils.setResourceValueAndSync(player, entry.getKey(), entry.getValue());
            }
        }
        if (Double.isFinite(pending.mana) && pending.mana >= 0
                && pending.manaType != null
                && pending.manaType.equals(String.valueOf(ManaUtils.getPlayerManaTypeID(player)))) {
            ManaUtils.setPlayerMana(player, pending.mana);
        }
    }

    public static void register() {
        // OriginFormReloadMixin calls restore after _loadForm finishes. No listener-order
        // assumption or delayed writeback may overwrite the player's subsequent actions.
        PlayerSessionCleanup.register((handler, server) -> PENDING.remove(handler.player.getUUID()));
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> PENDING.clear());
    }
}
