package github.com.gengyoubo.sscfa.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Per-target, per-caster windows. Saved with the target, never shared between worlds. */
public final class NpcCombatState {
    private final Map<UUID, Long> fallenMarks = new HashMap<>();
    private final Map<UUID, Long> wildCatStuns = new HashMap<>();

    public void markFallen(UUID caster, long now, int ticks) { put(fallenMarks, caster, now, ticks); }
    public void markWildCat(UUID caster, long now, int ticks) { put(wildCatStuns, caster, now, ticks); }
    public void clearFallen() { fallenMarks.clear(); }
    public void clearWildCat() { wildCatStuns.clear(); }
    public boolean hasFallen(UUID caster, long now) {
        prune(fallenMarks, now);
        return fallenMarks.containsKey(caster);
    }
    public Set<UUID> wildCats(long now) {
        prune(wildCatStuns, now);
        return Set.copyOf(wildCatStuns.keySet());
    }
    private static void put(Map<UUID, Long> windows, UUID caster, long now, int ticks) {
        prune(windows, now);
        if (ticks > 0) windows.merge(caster, now + ticks, Math::max);
    }
    private static void prune(Map<UUID, Long> windows, long now) {
        windows.values().removeIf(end -> end <= now);
    }
    public CompoundTag write(long now) {
        prune(fallenMarks, now);
        prune(wildCatStuns, now);
        CompoundTag nbt = new CompoundTag();
        nbt.put("Fallen", writeWindows(fallenMarks));
        nbt.put("WildCat", writeWindows(wildCatStuns));
        return nbt;
    }
    public void read(CompoundTag nbt) {
        readWindows(nbt.getCompound("Fallen"), fallenMarks);
        readWindows(nbt.getCompound("WildCat"), wildCatStuns);
    }
    private static CompoundTag writeWindows(Map<UUID, Long> windows) {
        CompoundTag nbt = new CompoundTag();
        windows.forEach((caster, end) -> nbt.putLong(caster.toString(), end));
        return nbt;
    }
    private static void readWindows(CompoundTag nbt, Map<UUID, Long> windows) {
        windows.clear();
        for (String key : nbt.getAllKeys()) {
            try { windows.put(UUID.fromString(key), nbt.getLong(key)); }
            catch (IllegalArgumentException ignored) { }
        }
    }
}
