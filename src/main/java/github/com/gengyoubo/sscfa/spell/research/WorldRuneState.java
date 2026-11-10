package github.com.gengyoubo.sscfa.spell.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class WorldRuneState extends SavedData {
    public static final int VERSION = 2;
    private static final String KEY = "ssc_addon_rune_language";
    private UUID worldId;
    private long seed;
    private RuneLanguage language;
    private CompoundTag legacyPatterns = new CompoundTag();
    private CompoundTag schemes = new CompoundTag();
    private WorldRuneState() {
        worldId = UUID.randomUUID(); seed = new java.security.SecureRandom().nextLong(); language = RuneLanguage.generate(seed);
        setDirty();
    }
    private WorldRuneState(CompoundTag n) {
        if ((n.getInt("Version") != 1 && n.getInt("Version") != VERSION) || !n.hasUUID("World")) throw new IllegalStateException("Unsupported or invalid saved rune language");
        worldId = n.getUUID("World"); seed = n.getLong("Seed"); language = new RuneLanguage(n.getIntArray("Meanings"), n.getIntArray("Rules"));
        legacyPatterns = n.getCompound("Patterns").copy();
        schemes = n.getCompound("RuneSchemes").copy();
        if (n.getInt("Version") == 1) setDirty();
    }
    public static WorldRuneState get(MinecraftServer server) {
        var manager=server.overworld().getDataStorage();
        WorldRuneState state=manager.get(WorldRuneState::new,KEY);
        if(state!=null)return state;
        // PersistentStateManager logs decoding errors and returns null. Never replace an existing language.
        var saved=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").resolve(KEY+".dat");
        if(java.nio.file.Files.exists(saved))throw new IllegalStateException("Cannot read saved rune language; refusing to replace "+saved);
        state=new WorldRuneState();manager.set(KEY,state);return state;
    }
    public UUID worldId() { return worldId; }
    public RuneLanguage language() { return language; }
    public UUID registerScheme(CompoundTag definition) {
        UUID id = UUID.randomUUID(); schemes.put(id.toString(), definition.copy()); setDirty(); return id;
    }
    public CompoundTag scheme(UUID id) { return schemes.contains(id.toString()) ? schemes.getCompound(id.toString()).copy() : null; }
    @Override public CompoundTag save(CompoundTag n) {
        n.putInt("Version", VERSION); n.putUUID("World", worldId); n.putLong("Seed", seed); n.putIntArray("Meanings", language.meanings()); n.putIntArray("Rules", language.rules());
        n.put("Patterns", legacyPatterns.copy()); n.put("RuneSchemes", schemes.copy()); return n;
    }
}
