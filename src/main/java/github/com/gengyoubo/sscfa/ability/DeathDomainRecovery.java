package github.com.gengyoubo.sscfa.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Write-ahead terrain log. Each expansion ring is durable before its first block changes. */
public final class DeathDomainRecovery {
    private static final Logger LOGGER = LoggerFactory.getLogger("ssc_addon/death_domain_recovery");
    private static final Map<MinecraftServer, State> SERVERS = new IdentityHashMap<>();
    private static boolean initialized;

    public record Change(BlockState original, BlockState expected) {}

    private DeathDomainRecovery() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStarted(server -> {
            State state = state(server);
            for (ServerLevel world : server.getAllLevels()) recoverLoaded(world, state);
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onChunkLoad((world, chunk) -> recoverChunk(world, chunk.getPos()));
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(SERVERS::remove);
    }

    /** False means no terrain in this ring may be changed. */
    public static boolean record(ServerLevel world, UUID domain, Map<BlockPos, Change> changes) {
        if (changes.isEmpty()) return true;
        State state = state(world.getServer());
        if (state.failed) return false;
        UUID id = UUID.randomUUID();
        Log log = new Log(id, domain, world.dimension().location(), new LinkedHashMap<>(changes));
        try {
            write(state.directory, log);
            log.active = true;
            state.logs.put(id, log);
            return true;
        } catch (IOException error) {
            state.failed = true;
            LOGGER.error("Cannot persist death-domain rollback; terrain conversion is paused", error);
            return false;
        }
    }

    /** Keep the durable log until the restored chunks have completed a flushing save. */
    public static void resolved(ServerLevel world, UUID domain, Set<BlockPos> positions) {
        for (Log log : state(world.getServer()).logs.values()) {
            if (!log.domain.equals(domain) || !log.dimension.equals(world.dimension().location())) continue;
            for (BlockPos pos : positions) {
                Change change = log.changes.get(pos);
                if (change != null && !world.getBlockState(pos).equals(change.expected)) log.resolved.add(pos);
            }
            compactResolved(log);
        }
    }

    public static void finished(ServerLevel world, UUID domain) {
        State state = state(world.getServer());
        for (Log log : state.logs.values()) {
            if (log.domain.equals(domain) && log.dimension.equals(world.dimension().location())) log.active = false;
        }
        recoverLoaded(world, state);
    }

    /** Called only after ServerWorld.save has flushed its chunk writes. */
    public static void afterFlushedSave(ServerLevel world) {
        State state = SERVERS.get(world.getServer());
        if (state == null) return;
        var iterator = state.logs.values().iterator();
        while (iterator.hasNext()) {
            Log log = iterator.next();
            if (log.active || !log.dimension.equals(world.dimension().location())
                    || !log.complete) continue;
            try {
                Files.deleteIfExists(state.directory.resolve(log.id + ".nbt"));
                iterator.remove();
            } catch (IOException error) {
                LOGGER.warn("Keeping death-domain recovery log after failed deletion: {}", log.id, error);
            }
        }
    }

    private static State state(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, ignored -> load(server.getWorldPath(LevelResource.ROOT)
                .resolve("data/ssc_addon_death_domain_recovery")));
    }

    private static State load(Path directory) {
        State state = new State(directory);
        if (!Files.exists(directory)) return state;
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".nbt")).toList()) {
                try {
                    Log log = read(NbtIo.readCompressed(path.toFile()));
                    if (!path.getFileName().toString().equals(log.id + ".nbt")) throw new IOException("Mismatched journal filename");
                    state.logs.put(log.id, log);
                } catch (Exception error) {
                    state.failed = true;
                    LOGGER.error("Cannot read terrain recovery log {}; original file is retained", path, error);
                }
            }
        } catch (IOException error) {
            state.failed = true;
            LOGGER.error("Cannot read death-domain recovery directory {}", directory, error);
        }
        return state;
    }

    private static void recoverLoaded(ServerLevel world, State state) {
        Set<ChunkPos> chunks = new HashSet<>();
        for (Log log : state.logs.values()) {
            if (!log.active && log.dimension.equals(world.dimension().location())) {
                for (BlockPos pos : log.changes.keySet()) {
                    if (!log.resolved.contains(pos)) chunks.add(new ChunkPos(pos));
                }
            }
        }
        for (ChunkPos chunk : chunks) {
            if (world.hasChunk(chunk.x, chunk.z)) recoverChunk(world, chunk);
        }
    }

    private static void recoverChunk(ServerLevel world, ChunkPos chunk) {
        State state = state(world.getServer());
        if (!world.hasChunk(chunk.x, chunk.z)) return;
        for (Log log : state.logs.values()) {
            if (log.active || !log.dimension.equals(world.dimension().location())) continue;
            for (var entry : log.changes.entrySet()) {
                BlockPos pos = entry.getKey();
                if (pos.getX() >> 4 != chunk.x || pos.getZ() >> 4 != chunk.z || log.resolved.contains(pos)) continue;
                Change change = entry.getValue();
                // A different current block belongs to later gameplay and must be preserved.
                if (world.getBlockState(pos).equals(change.expected)) {
                    world.setBlock(pos, change.original, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
                if (!world.getBlockState(pos).equals(change.expected)) log.resolved.add(pos);
            }
            compactResolved(log);
        }
    }

    private static void compactResolved(Log log) {
        if (log.resolved.size() == log.changes.size()) {
            log.complete = true;
            log.active = false;
            // The disk log remains authoritative until a flush. Release block snapshots now
            // so a long-running server does not retain every historical cast's terrain.
            log.changes.clear();
            log.resolved.clear();
        }
    }

    static void write(Path directory, Log log) throws IOException {
        github.com.gengyoubo.sscfa.util.AtomicNbtFile.write(directory.resolve(log.id + ".nbt"), encode(log));
    }

    static CompoundTag encode(Log log) {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 1);
        root.putUUID("Id", log.id);
        root.putUUID("Domain", log.domain);
        root.putString("Dimension", log.dimension.toString());
        ListTag blocks = new ListTag();
        log.changes.forEach((pos, change) -> {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos", pos.asLong());
            tag.put("Original", NbtUtils.writeBlockState(change.original));
            tag.put("Expected", NbtUtils.writeBlockState(change.expected));
            blocks.add(tag);
        });
        root.put("Blocks", blocks);
        return root;
    }

    static Log read(CompoundTag root) throws IOException {
        ResourceLocation dimension = ResourceLocation.tryParse(root.getString("Dimension"));
        if (root.getInt("Version") != 1 || !root.hasUUID("Id") || !root.hasUUID("Domain") || dimension == null)
            throw new IOException("Invalid terrain recovery header");
        Map<BlockPos, Change> changes = new LinkedHashMap<>();
        ListTag blocks = root.getList("Blocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < blocks.size(); index++) {
            CompoundTag tag = blocks.getCompound(index);
            changes.put(BlockPos.of(tag.getLong("Pos")), new Change(blockState(tag.getCompound("Original")),
                    blockState(tag.getCompound("Expected"))));
        }
        return new Log(root.getUUID("Id"), root.getUUID("Domain"), dimension, changes);
    }

    private static BlockState blockState(CompoundTag tag) throws IOException {
        ResourceLocation block = ResourceLocation.tryParse(tag.getString("Name"));
        if (block == null || !BuiltInRegistries.BLOCK.containsKey(block)) throw new IOException("Unknown saved block " + block);
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
    }

    static final class Log {
        final UUID id, domain;
        final ResourceLocation dimension;
        final Map<BlockPos, Change> changes;
        final Set<BlockPos> resolved = new HashSet<>();
        boolean active;
        boolean complete;

        Log(UUID id, UUID domain, ResourceLocation dimension, Map<BlockPos, Change> changes) {
            this.id = id;
            this.domain = domain;
            this.dimension = dimension;
            this.changes = changes;
        }
    }

    private static final class State {
        final Path directory;
        final Map<UUID, Log> logs = new HashMap<>();
        boolean failed;
        State(Path directory) { this.directory = directory; }
    }
}
