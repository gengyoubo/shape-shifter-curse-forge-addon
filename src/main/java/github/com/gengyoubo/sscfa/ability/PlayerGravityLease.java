package github.com.gengyoubo.sscfa.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Restores only gravity temporarily owned by an SSCA movement skill. Server thread only. */
public final class PlayerGravityLease {
    private static final Map<UUID, Lease> ACTIVE = new HashMap<>();
    private record Lease(ServerPlayer player, boolean previous, Set<String> owners) {}

    private PlayerGravityLease() {}

    public static void acquire(ServerPlayer player, String owner) {
        Lease lease = ACTIVE.get(player.getUUID());
        if (lease == null || lease.player != player) {
            lease = new Lease(player, player.isNoGravity(), new HashSet<>());
            ACTIVE.put(player.getUUID(), lease);
        }
        lease.owners.add(owner);
        player.setNoGravity(true);
    }

    public static void release(ServerPlayer player, String owner) {
        Lease lease = ACTIVE.get(player.getUUID());
        if (lease == null || lease.player != player || !lease.owners.remove(owner)) return;
        if (!lease.owners.isEmpty()) return;
        player.setNoGravity(lease.previous);
        ACTIVE.remove(player.getUUID());
    }

    /** Autosaving an in-flight player must not persist a temporary movement flag. */
    public static void writePersistentGravity(ServerPlayer player, CompoundTag nbt) {
        Lease lease = ACTIVE.get(player.getUUID());
        if (lease != null && lease.player == player) {
            if (lease.previous) nbt.putBoolean("NoGravity", true);
            else nbt.remove("NoGravity");
        }
    }
}
