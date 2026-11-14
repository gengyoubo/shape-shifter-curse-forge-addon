package github.com.gengyoubo.sscfa.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 锚定随世界和玩家 UUID 保存，死亡后不丢失。 */
public final class InstinctAnchorSavedData extends SavedData {
    private static final String KEY = "ssc_addon_instinct_anchors";
    private final Set<UUID> locked = new HashSet<>();

    public static InstinctAnchorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(InstinctAnchorSavedData::load, InstinctAnchorSavedData::new, KEY);
    }

    public boolean isLocked(UUID player) { return locked.contains(player); }
    public void setLocked(UUID player, boolean value) {
        if (value ? locked.add(player) : locked.remove(player)) setDirty();
    }

    public static InstinctAnchorSavedData load(CompoundTag tag) {
        var result = new InstinctAnchorSavedData();
        ListTag entries = tag.getList("Locked", Tag.TAG_STRING);
        for (int i = 0; i < entries.size(); i++) {
            try { result.locked.add(UUID.fromString(entries.getString(i))); }
            catch (IllegalArgumentException ignored) { }
        }
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        locked.forEach(id -> entries.add(StringTag.valueOf(id.toString())));
        tag.put("Locked", entries);
        return tag;
    }
}
