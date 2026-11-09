package github.com.gengyoubo.sscfa.spell.pocket;

import github.com.gengyoubo.sscfa.spell.ScrollData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class PocketSpaceStorage {
	public static final String SCROLL_ID = "PocketSpaceId";
	private static final Logger LOGGER = LoggerFactory.getLogger("ssc_addon/pocket");
	private static final String REGISTRY_KEY = "ssc_addon_pocket_registry";

	private PocketSpaceStorage() {}

	private static DimensionDataStorage states(MinecraftServer server) {
		return server.overworld().getDataStorage();
	}

	public static Registry registry(MinecraftServer server) {
		return states(server).computeIfAbsent(Registry::read, Registry::new, REGISTRY_KEY);
	}

	/** 卷轴 UUID → 房间（未绑定或已消除返回 null）。 */
	public static Room findRoom(MinecraftServer server, UUID scrollId) {
		return scrollId == null ? null : registry(server).byScroll.get(scrollId);
	}

	public static Room roomAt(MinecraftServer server, int blockX, int blockZ) {
		int slot = PocketSpaceLayout.indexAt(blockX, blockZ);
		return slot < 0 ? null : registry(server).bySlot.get(slot);
	}

	/**
	 * 为卷轴绑定房间：已有绑定直接复用；否则占用离 (0,0) 最近的空闲槽位（被消除过的槽位优先复用），
	 * 卷轴 NBT 只写一个 UUID。返回 null 表示无法分配。
	 */
	public static UUID bind(MinecraftServer server, ItemStack scroll) {
		CompoundTag nbt = scroll.getOrCreateTag();
		Registry registry = registry(server);
		if (nbt.hasUUID(SCROLL_ID)) {
			UUID existing = nbt.getUUID(SCROLL_ID);
			Room room = registry.byScroll.get(existing);
			if (room != null) return existing;
			// A legacy cleanup log has no room layout snapshot. Preserve its UUID and
			// reserved terrain rather than assigning a replacement that could erase it.
			if (registry.pendingWipes.containsValue(existing)) return null;
			// 卷轴曾绑定但房间已被消除（如丢弃超时后又被捡回）：按新卷轴重新分配
		}
		int slot = 0;
		while (slot < PocketSpaceLayout.MAX_ROOMS && (registry.bySlot.containsKey(slot)
				|| registry.pendingWipes.containsKey(slot))) slot++;
		if (slot >= PocketSpaceLayout.MAX_ROOMS) return null;
		UUID scrollId = UUID.randomUUID();
		Room room = new Room(scrollId, new PocketSpaceLayout(slot, ScrollData.getLevel(scroll)), server.overworld().getGameTime());
		registry.bySlot.put(slot, room);
		registry.byScroll.put(scrollId, room);
		registry.setDirty();
		states(server).save();
		nbt.putUUID(SCROLL_ID, scrollId);
		LOGGER.info("卷轴绑定口袋空间：slot={} level={} scroll={}", slot, room.layout.level(), scrollId);
		return scrollId;
	}

	/** 注销并持久化清空任务；槽位在方块清空且维度刷盘后才可复用。 */
	public static boolean unbind(MinecraftServer server, Room room) {
		Registry registry = registry(server);
		if (registry.bySlot.get(room.layout.index()) != room || registry.byScroll.get(room.scrollId) != room
				|| registry.pendingWipes.containsKey(room.layout.index())) return false;
		// Reserve the slot and journal the wipe in the same saved transaction as unbinding.
		// Recovery deliberately restarts at zero: a saved cursor can run ahead of saved chunks.
		registry.pendingWipes.put(room.layout.index(), room.scrollId);
		registry.bySlot.remove(room.layout.index());
		registry.byScroll.remove(room.scrollId);
		registry.setDirty();
		if (!persistRegistry(server)) {
			registry.pendingWipes.remove(room.layout.index(), room.scrollId);
			registry.bySlot.put(room.layout.index(), room);
			registry.byScroll.put(room.scrollId, room);
			registry.setDirty();
			return false;
		}
		LOGGER.info("口袋空间已消除：slot={} scroll={}", room.layout.index(), room.scrollId);
		return true;
	}

	/** Report write failures to callers before any destructive task starts. */
	static boolean persistRegistry(MinecraftServer server) {
		try {
			registry(server).writeAtomic(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
					.resolve("data/" + REGISTRY_KEY + ".dat"));
			return true;
		} catch (java.io.IOException error) {
			LOGGER.error("Cannot persist pocket-room recovery; destructive cleanup is postponed", error);
			return false;
		}
	}

	public static Visit visit(ServerPlayer player) {
		return states(player.getServer()).computeIfAbsent(Visit::read, Visit::new,
				"ssc_addon_pocket_visit_" + player.getUUID());
	}

	public static void save(MinecraftServer server) {
		states(server).save();
	}

	/** 全部房间登记（单一存档文件，避免分散小文件丢失导致的槽位重叠）。 */
	public static final class Registry extends SavedData {
		final java.util.Map<Integer, Room> bySlot = new java.util.HashMap<>();
		final java.util.Map<UUID, Room> byScroll = new java.util.HashMap<>();
		final java.util.Map<Integer, UUID> pendingWipes = new java.util.HashMap<>();

		public java.util.Collection<Room> rooms() {
			return java.util.Collections.unmodifiableCollection(bySlot.values());
		}

		private void writeAtomic(java.nio.file.Path path) throws java.io.IOException {
			CompoundTag root = new CompoundTag();
			root.put("data", save(new CompoundTag()));
			root.putInt("DataVersion", net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
			github.com.gengyoubo.sscfa.util.AtomicNbtFile.write(path, root);
			setDirty(false);
		}

		@Override
		public void save(java.io.File file) {
			if (!isDirty()) return;
			try {
				writeAtomic(file.toPath());
			} catch (java.io.IOException error) {
				LOGGER.error("Cannot atomically save pocket-space registry; retaining the previous file", error);
			}
		}

		static Registry read(CompoundTag nbt) {
			Registry registry = new Registry();
			net.minecraft.nbt.ListTag list = nbt.getList("Rooms", net.minecraft.nbt.Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag tag = list.getCompound(i);
				if (!tag.hasUUID("Scroll")) continue;
				try {
					Room room = new Room(tag.getUUID("Scroll"),
							new PocketSpaceLayout(tag.getInt("Slot"), tag.getInt("Level")), tag.getLong("LastSeen"));
					room.built = tag.getBoolean("Built");
					room.collapsing = tag.getBoolean("Collapsing");
					if (registry.bySlot.containsKey(room.layout.index())) continue;
					registry.bySlot.put(room.layout.index(), room);
					registry.byScroll.put(room.scrollId, room);
				} catch (IllegalArgumentException ignored) {
					// 跳过损坏条目，避免存档加载失败
				}
			}
			net.minecraft.nbt.ListTag wipes = nbt.getList("PendingWipes", net.minecraft.nbt.Tag.TAG_COMPOUND);
			for (int i = 0; i < wipes.size(); i++) {
				CompoundTag tag = wipes.getCompound(i);
				int slot = tag.getInt("Slot");
				if (slot >= 0 && slot < PocketSpaceLayout.MAX_ROOMS && tag.hasUUID("Scroll")
						&& !registry.bySlot.containsKey(slot)) registry.pendingWipes.put(slot, tag.getUUID("Scroll"));
			}
			return registry;
		}

		@Override
		public CompoundTag save(CompoundTag nbt) {
			net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
			for (Room room : bySlot.values()) {
				CompoundTag tag = new CompoundTag();
				tag.putUUID("Scroll", room.scrollId);
				tag.putInt("Slot", room.layout.index());
				tag.putInt("Level", room.layout.level());
				tag.putLong("LastSeen", room.lastSeen);
				tag.putBoolean("Built", room.built);
				tag.putBoolean("Collapsing", room.collapsing);
				list.add(tag);
			}
			nbt.put("Rooms", list);
			net.minecraft.nbt.ListTag wipes = new net.minecraft.nbt.ListTag();
			pendingWipes.forEach((slot, scroll) -> {
				CompoundTag tag = new CompoundTag();
				tag.putInt("Slot", slot);
				tag.putUUID("Scroll", scroll);
				wipes.add(tag);
			});
			nbt.put("PendingWipes", wipes);
			return nbt;
		}
	}

	public static final class Room {
		public final UUID scrollId;
		final PocketSpaceLayout layout;
		/** 卷轴最后一次被确认存在的世界时间（主世界 tick）。 */
		long lastSeen;
		boolean built;
		boolean collapsing;

		Room(UUID scrollId, PocketSpaceLayout layout, long lastSeen) {
			this.scrollId = scrollId;
			this.layout = layout;
			this.lastSeen = lastSeen;
		}

		public PocketSpaceLayout layout() { return layout; }
		public boolean initialized() { return built; }
		public long lastSeen() { return lastSeen; }

		public void finishGeneration() { built = true; }
		public void touch(long now) { if (now > lastSeen) lastSeen = now; }
	}

	public static final class Visit extends SavedData {
		private UUID roomId;
		private ResourceKey<Level> dimension;
		private Vec3 position;
		private float yaw;
		private float pitch;

		public UUID roomId() { return roomId; }
		public ResourceKey<Level> dimension() { return dimension; }
		public Vec3 position() { return position; }
		public float yaw() { return yaw; }
		public float pitch() { return pitch; }
		public boolean active() { return roomId != null; }

		public void begin(UUID roomId, ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
			this.roomId = roomId;
			this.dimension = dimension;
			this.position = position;
			this.yaw = yaw;
			this.pitch = pitch;
			setDirty();
		}

		public void clear() {
			roomId = null;
			dimension = null;
			position = null;
			setDirty();
		}

		static Visit read(CompoundTag nbt) {
			Visit visit = new Visit();
			ResourceLocation dimensionId = ResourceLocation.tryParse(nbt.getString("Dimension"));
			if (!nbt.hasUUID("Room") || dimensionId == null) return visit;
			Vec3 position = new Vec3(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
			float yaw = nbt.getFloat("Yaw");
			float pitch = nbt.getFloat("Pitch");
			if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
					|| !Float.isFinite(yaw) || !Float.isFinite(pitch)
					|| Math.abs(position.x) > 30_000_000 || Math.abs(position.z) > 30_000_000
					|| Math.abs(position.y) > 20_000_000) return visit;
			visit.roomId = nbt.getUUID("Room");
			visit.dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
			visit.position = position;
			visit.yaw = yaw;
			visit.pitch = pitch;
			return visit;
		}

		@Override
		public CompoundTag save(CompoundTag nbt) {
			if (!active()) return nbt;
			nbt.putUUID("Room", roomId);
			nbt.putString("Dimension", dimension.location().toString());
			nbt.putDouble("X", position.x);
			nbt.putDouble("Y", position.y);
			nbt.putDouble("Z", position.z);
			nbt.putFloat("Yaw", yaw);
			nbt.putFloat("Pitch", pitch);
			return nbt;
		}
	}
}
