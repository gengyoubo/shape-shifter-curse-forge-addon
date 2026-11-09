package github.com.gengyoubo.sscfa.tutorial;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Viewing records belong to a player in this save, independent of the form history. */
public final class FormTutorialState extends SavedData {
    private static final String SAVE_ID = "ssca_form_tutorials";
    private final Map<UUID, Set<ResourceLocation>> viewed = new HashMap<>();

    public static FormTutorialState get(MinecraftServer server) {
        var manager = server.overworld().getDataStorage();
        FormTutorialState state = manager.get(FormTutorialState::read, SAVE_ID);
        if (state != null) return state;
        var path = server.getWorldPath(LevelResource.ROOT).resolve("data/" + SAVE_ID + ".dat");
        if (Files.exists(path)) throw new IllegalStateException("Unreadable form tutorial data: " + path);
        state = new FormTutorialState();
        manager.set(SAVE_ID, state);
        return state;
    }

    public boolean hasSeen(UUID player, ResourceLocation form) {
        var forms = viewed.get(player);
        return forms != null && forms.contains(FormIdMigration.canonical(form));
    }

    public boolean markSeen(UUID player, ResourceLocation form) {
        if (player == null || form == null) return false;
        boolean added = viewed.computeIfAbsent(player, ignored -> new HashSet<>())
                .add(FormIdMigration.canonical(form));
        if (added) setDirty();
        return added;
    }

    static FormTutorialState read(CompoundTag tag) {
        if (tag.getInt("Version") != 1 || !tag.contains("Players", Tag.TAG_COMPOUND)) {
            throw new IllegalStateException("Invalid form tutorial save format");
        }
        FormTutorialState state = new FormTutorialState();
        CompoundTag players = tag.getCompound("Players");
        boolean migrated = false;
        for (String playerId : players.getAllKeys()) {
            UUID player = UUID.fromString(playerId);
            if (!players.contains(playerId, Tag.TAG_LIST)) {
                throw new IllegalStateException("Invalid form tutorial player record: " + playerId);
            }
            ListTag list = players.getList(playerId, Tag.TAG_STRING);
            if (list.size() != ((ListTag) players.get(playerId)).size()) {
                throw new IllegalStateException("Invalid form tutorial form list: " + playerId);
            }
            Set<ResourceLocation> forms = new HashSet<>();
            for (Tag entry : list) {
                ResourceLocation original = ResourceLocation.tryParse(entry.getAsString());
                if (original == null) throw new IllegalStateException("Invalid form tutorial form ID");
                ResourceLocation canonical = FormIdMigration.canonical(original);
                migrated |= !original.equals(canonical);
                forms.add(canonical);
            }
            if (!forms.isEmpty()) state.viewed.put(player, forms);
        }
        if (migrated) state.setDirty();
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", 1);
        CompoundTag players = new CompoundTag();
        viewed.forEach((player, forms) -> {
            ListTag list = new ListTag();
            forms.stream().map(ResourceLocation::toString).sorted()
                    .forEach(form -> list.add(StringTag.valueOf(form)));
            players.put(player.toString(), list);
        });
        tag.put("Players", players);
        return tag;
    }
}
