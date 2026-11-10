package github.com.gengyoubo.sscfa.tutorial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 法术教程的世界级播放策略与已观看记录。 */
public final class SpellTutorialState extends SavedData {
    private static final String KEY = "ssc_addon_spell_tutorial";
    private boolean policyInitialized;
    private boolean autoEnabled;
    private final Set<UUID> seen = new HashSet<>();

    private SpellTutorialState() {
    }

    private SpellTutorialState(CompoundTag tag) {
        policyInitialized = tag.getBoolean("PolicyInitialized");
        autoEnabled = tag.getBoolean("AutoEnabled");
        ListTag list = tag.getList("Seen", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                seen.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static SpellTutorialState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(SpellTutorialState::new, SpellTutorialState::new, KEY);
    }

    public boolean policyInitialized() {
        return policyInitialized;
    }

    public boolean autoEnabled() {
        return autoEnabled;
    }

    public void initializePolicy(boolean enabled) {
        policyInitialized = true;
        autoEnabled = enabled;
        setDirty();
    }

    public boolean hasSeen(UUID player) {
        return seen.contains(player);
    }

    public void markSeen(UUID player) {
        if (seen.add(player)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("PolicyInitialized", policyInitialized);
        tag.putBoolean("AutoEnabled", autoEnabled);
        ListTag list = new ListTag();
        for (UUID id : seen) list.add(StringTag.valueOf(id.toString()));
        tag.put("Seen", list);
        return tag;
    }
}
