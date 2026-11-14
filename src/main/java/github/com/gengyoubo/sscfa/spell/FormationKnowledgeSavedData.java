package github.com.gengyoubo.sscfa.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 服务端世界独立保存玩家的法阵知识和研究进度。 */
public final class FormationKnowledgeSavedData extends SavedData {
    private static final String KEY = "ssc_addon_formation_knowledge";
    private final Map<UUID, FormationKnowledgeComponent> players = new HashMap<>();

    public static FormationKnowledgeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FormationKnowledgeSavedData::load, FormationKnowledgeSavedData::new, KEY);
    }

    public FormationKnowledgeComponent getOrCreate(UUID player) {
        // ResearchProgress is mutable, so mark the data dirty before handing it out.
        setDirty();
        return players.computeIfAbsent(player, ignored -> new FormationKnowledgeComponent());
    }

    public static FormationKnowledgeSavedData load(CompoundTag tag) {
        var data = new FormationKnowledgeSavedData();
        for (String key : tag.getAllKeys()) {
            if (!tag.contains(key, Tag.TAG_COMPOUND)) continue;
            try {
                var component = new FormationKnowledgeComponent();
                component.readFromNbt(tag.getCompound(key));
                data.players.put(UUID.fromString(key), component);
            } catch (IllegalArgumentException ignored) {
                // Ignore unrelated or malformed player keys.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        players.forEach((id, component) -> {
            var value = new CompoundTag();
            component.writeToNbt(value);
            tag.put(id.toString(), value);
        });
        return tag;
    }
}
