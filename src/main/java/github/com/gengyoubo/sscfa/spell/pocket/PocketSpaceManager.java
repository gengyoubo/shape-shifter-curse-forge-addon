package github.com.gengyoubo.sscfa.spell.pocket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import github.com.gengyoubo.sscfa.block.PocketSpaceBlocks;
import github.com.gengyoubo.sscfa.init.AddonBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PocketSpaceManager {
	public static final ResourceKey<Level> WORLD_KEY = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation("ssc_addon", "pocket_space"));
	private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
	private static final Map<Integer, Generation> GENERATIONS = new HashMap<>();
	private static final Map<UUID, PocketPortalGate> GATES = new HashMap<>();
	/** 房间消除倒计时（槽????剩??tick）：房内有人时先屏幕中央提??5 秒再送回?*/
	private static final Map<Integer, Eviction> EVICTIONS = new HashMap<>();
	/** 已排队清空的槽位（??tick 分批清）??*/
	private static final Map<Integer, Wipe> WIPES = new HashMap<>();
	private static final Map<Integer, UUID> COMPLETED_WIPES = new HashMap<>();
	private static final UUID CHANNEL_SLOW_ID = UUID.fromString("9d17b640-476a-4bdc-a396-8a729fbd5ced");
	private static final int EVICTION_TICKS = 100;
	private static boolean generating;
	private static MinecraftServer activeServer;
	private static boolean recoveredTasks;

	private PocketSpaceManager() {}

	public static boolean isPocket(Level world) {
		return WORLD_KEY.equals(world.dimension());
	}

	public static boolean canEnter(ServerPlayer player) {
		ensureServer(player.getServer());
		return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isVehicle()
				&& !github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(player)
				&& !isPocket(player.level()) && !CHANNELS.containsKey(player.getUUID())
				&& player.getServer().getLevel(WORLD_KEY) != null;
	}

	public static void start(ServerPlayer player, UUID scrollId, int ticks) {
		ensureServer(player.getServer());
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) return;
		PocketSpaceStorage.Room room = PocketSpaceStorage.findRoom(player.getServer(), scrollId);
		if (room == null || room.collapsing) return;
		CHANNELS.put(player.getUUID(), new Channel(player, scrollId, Math.max(1, ticks)));
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(CHANNEL_SLOW_ID);
			speed.addTransientModifier(new AttributeModifier(CHANNEL_SLOW_ID, "Pocket Space Channel",
					PocketChannelRules.SPEED_MULTIPLIER - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		if (!room.initialized()) GENERATIONS.computeIfAbsent(room.layout().index(), ignored -> new Generation(room));
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
				SoundSource.PLAYERS, 0.6f, 1.6f);
	}

	public static boolean prepareRoom(ServerPlayer player, UUID scrollId) {
		ensureServer(player.getServer());
		PocketSpaceStorage.Room room = PocketSpaceStorage.findRoom(player.getServer(), scrollId);
		if (room == null || room.collapsing) return false;
		if (!room.initialized()) GENERATIONS.computeIfAbsent(room.layout().index(), ignored -> new Generation(room));
		return room.initialized() && !WIPES.containsKey(room.layout().index());
	}

	public static void enterNow(ServerPlayer player, UUID scrollId) {
		ServerLevel pocket = player.getServer().getLevel(WORLD_KEY);
		PocketSpaceStorage.Room room = PocketSpaceStorage.findRoom(player.getServer(), scrollId);
		if (pocket != null && room != null && canEnter(player) && prepareRoom(player, scrollId)) {
			enter(player, new Channel(player, scrollId, 0), pocket, room);
		}
	}

	public static void init() {
		MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ensureServer(event.getServer()));
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) tick(event.getServer());
		});
		MinecraftForge.EVENT_BUS.addListener((LivingAttackEvent event) -> {
			if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player) interrupt(player);
		});
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
			CHANNELS.remove(handler.player.getUUID());
			clearSlow(handler.player);
			GATES.remove(handler.player.getUUID());
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) clearSlow(player);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				CHANNELS.remove(player.getUUID());
				clearSlow(player);
				GATES.remove(player.getUUID());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) ->
				event.getServer().getPlayerList().getPlayers().forEach(PocketSpaceManager::clearSlow));
		MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
			if (activeServer == event.getServer()) {
				clearTransientTasks();
				activeServer = null;
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerSleepInBedEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player && isPocket(player.level()))
				event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerSetSpawnEvent event) -> {
			if (isPocket(event.getEntity().level())) event.setCanceled(true);
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (event.getEntity() instanceof ServerPlayer player && isPocket(player.level())
					&& player.level().getBlockState(event.getPos()).getBlock() instanceof BedBlock
					&& !player.isSecondaryUseActive()) {
				message(player, "no_sleep");
				event.setCanceled(true);
			}
		});
	}

	private static void clearTransientTasks() {
		CHANNELS.clear();
		GENERATIONS.clear();
		GATES.clear();
		EVICTIONS.clear();
		WIPES.clear();
		COMPLETED_WIPES.clear();
		generating = false;
		recoveredTasks = false;
	}

	private static void ensureServer(MinecraftServer server) {
		if (activeServer != server) {
			clearTransientTasks();
			activeServer = server;
		}
	}

	private static void recoverTasks(MinecraftServer server) {
		if (recoveredTasks) return;
		PocketSpaceStorage.Registry registry = PocketSpaceStorage.registry(server);
		// Legacy tasks were created from an incomplete "not seen" census. They are
		// not evidence of destruction and must never resume destructive work.
		EVICTIONS.clear();
		WIPES.clear();
		COMPLETED_WIPES.clear();
		boolean changed = false;
		for (PocketSpaceStorage.Room room : registry.rooms()) {
			if (room.collapsing) {
				room.collapsing = false;
				changed = true;
			}
		}
		// Keep unknown legacy wipe slots reserved: their missing layout cannot safely
		// be reconstructed, and reusing the coordinates would destroy remaining blocks.
		if (changed) {
			registry.setDirty();
			PocketSpaceStorage.persistRegistry(server);
		}
		recoveredTasks = true;
	}

	/** Do not release a wiped slot until the pocket dimension's cleared chunks are durable. */
	public static void afterFlushedSave(ServerLevel world) {
		if (!isPocket(world) || activeServer != world.getServer() || COMPLETED_WIPES.isEmpty()) return;
		PocketSpaceStorage.Registry registry = PocketSpaceStorage.registry(world.getServer());
		Map<Integer, UUID> removed = new HashMap<>();
		COMPLETED_WIPES.forEach((slot, scroll) -> {
			if (!registry.bySlot.containsKey(slot) && registry.pendingWipes.remove(slot, scroll)) removed.put(slot, scroll);
		});
		if (removed.isEmpty()) return;
		registry.setDirty();
		if (PocketSpaceStorage.persistRegistry(world.getServer())) {
			removed.forEach((slot, scroll) -> COMPLETED_WIPES.remove(slot, scroll));
		} else {
			registry.pendingWipes.putAll(removed);
			registry.setDirty();
		}
	}

	// 分段守卫（非整体短路）：setTimeOfDay/视??census 必须照常跑，且「有人在
	//  口袋维度」的检测本身就需要玩家循环——整体短路不可行；改为各迭代器段包空表守卫、GATES.remove
	//  ??!GATES.isEmpty()，行为不变，常态（四表全空）只剩每玩家一?isPocket 判断?
	private static void tick(MinecraftServer server) {
		ensureServer(server);
		ServerLevel pocket = server.getLevel(WORLD_KEY);
		if (pocket == null) return;
		recoverTasks(server);
		pocket.setDayTime(6000); // 口袋维度无时间概念：永远正??
		// 口袋维度观察距离压??3：服务器只为玩家加载周围 7?? 区块（足以覆盖任意等级房间盒）??
		// 而非全视??441 个区块——这是“进入卡很久”的主源。周期重设防全局视距变更把它改回去??
		if (pocket.getGameTime() % 100 == 0) {
			((github.com.gengyoubo.sscfa.mixin.ThreadedAnvilChunkStorageAccessor)
					pocket.getChunkSource().chunkMap).sscAddon$setViewDistance(3);
		}
		// Census records positive observations only. Missing observations never reclaim rooms.
		if (server.overworld().getGameTime() % 100 == 0) census(server, pocket);
		if (!EVICTIONS.isEmpty()) tickEvictions(server, pocket);
		int budget = 4096;
		if (!WIPES.isEmpty()) {
			Iterator<Wipe> wipes = WIPES.values().iterator();
			while (wipes.hasNext() && budget > 0) {
				Wipe wipe = wipes.next();
				PocketSpaceStorage.Registry registry = PocketSpaceStorage.registry(server);
				if (!wipe.belongsTo(server, registry)) {
					wipes.remove();
					continue;
				}
				budget -= wipe.advance(pocket, budget);
				if (wipe.done()) {
					wipes.remove();
					COMPLETED_WIPES.put(wipe.slot, wipe.scrollId);
				}
			}
		}
		if (!GENERATIONS.isEmpty()) {
			Iterator<Generation> builds = GENERATIONS.values().iterator();
			while (builds.hasNext() && budget > 0) {
				Generation generation = builds.next();
				budget -= generation.advance(pocket, budget);
				if (generation.done()) {
					generation.room.finishGeneration();
					PocketSpaceStorage.registry(server).setDirty();
					builds.remove();
				}
			}
		}
		if (!CHANNELS.isEmpty()) {
			Iterator<Map.Entry<UUID, Channel>> channels = CHANNELS.entrySet().iterator();
			while (channels.hasNext()) {
			Map.Entry<UUID, Channel> entry = channels.next();
			Channel channel = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			PocketSpaceStorage.Room room = PocketSpaceStorage.findRoom(server, channel.scrollId);
			if (player == null || !player.isAlive() || player.isPassenger() || player.isVehicle()
					|| player.isSpectator()
					|| github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(player)
					|| !player.level().dimension().equals(channel.dimension)
					|| room == null) {
				if (player != null) {
					clearSlow(player);
					message(player, "interrupted");
				}
				channels.remove();
				continue;
			}
			if (!room.initialized()) {
				GENERATIONS.computeIfAbsent(room.layout().index(), ignored -> new Generation(room));
			}
			if (channel.ticks % 5 == 0) {
				// owner 打标：仅本人第一人称避让（引导门雾）
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(player.serverLevel(), player, ParticleTypes.PORTAL, player.getX(), player.getY(0.7),
						player.getZ(), 5, 0.4, 0.5, 0.4, 0.05);
				message(player, "channel", (Math.max(0, channel.ticks) + 19) / 20);
			}
			if (--channel.ticks <= 0 && room.initialized() && !WIPES.containsKey(room.layout().index())) {
				clearSlow(player);
				enter(player, channel, pocket, room);
				channels.remove();
			}
			}
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isPocket(player.level())) {
				if (!GATES.isEmpty()) GATES.remove(player.getUUID());
				continue;
			}
			if (!player.isAlive() || player.isSpectator()) continue;
			PocketSpaceStorage.Visit visit = PocketSpaceStorage.visit(player);
			if (!visit.active()) continue;
			PocketSpaceStorage.Room room = PocketSpaceStorage.findRoom(server, visit.roomId());
			if (room == null) {
				// 房间已消除而玩家仍在维度内（如消除时离线）：直接送回原位??
				leave(player, visit);
				continue;
			}
			if (EVICTIONS.containsKey(room.layout().index())) continue; // 倒计时中，??tickEvictions 处??
			if (!room.initialized()) {
				GENERATIONS.computeIfAbsent(room.layout().index(), ignored -> new Generation(room));
				continue;
			}
			PocketSpaceLayout layout = room.layout();
			PocketPortalGate gate = GATES.computeIfAbsent(player.getUUID(), ignored -> {
				repairPortal(pocket, layout);
				return new PocketPortalGate();
			});
			BlockPos feet = player.blockPosition();
			if (!layout.isInterior(feet.getX(), feet.getY(), feet.getZ())) {
				teleportToPortal(player, pocket, layout);
				GATES.put(player.getUUID(), new PocketPortalGate());
				continue;
			}
			boolean insidePortal = player.getX() >= layout.centerX() - 1 && player.getX() < layout.centerX() + 1
					&& player.getZ() >= layout.minZ() - 3 && player.getZ() < layout.minZ() - 1;
			boolean onPortal = insidePortal && Math.abs(player.getY()
					- (PocketSpaceLayout.FLOOR_Y + 1 + PocketSpaceLayout.PORTAL_TOP_HEIGHT)) < 0.08
					&& player.onGround();
            if (github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(player)) {
                gate.tick(insidePortal, false); // Voluntary exit waits; eviction/safety-return paths above remain available.
                if (onPortal && player.tickCount % 20 == 0)
                    github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player);
                continue;
            }
			if (gate.tick(insidePortal, onPortal)) {
				leave(player, visit);
				GATES.put(player.getUUID(), new PocketPortalGate());
			} else if (gate.counting() && player.tickCount % 20 == 0) {
				message(player, "exit_countdown", gate.remainingSeconds());
			}
			// 出口倒计时期间：传送台法阵冒紫色上升粒子流（仿施法演出，客户端本地渲染??
			if (gate.counting() && player.tickCount % 5 == 0) {
				spawnExitFx(pocket, layout);
			}
		}
	}

	/**
	 * 刷新已观察到卷轴的心跳。箱子、离线玩家和未加载实体无法被此扫描普查；
	 * 未被观察到不等于卷轴消失，也不会触发房间驱逐或清空??
	 */
	private static void census(MinecraftServer server, ServerLevel pocket) {
		PocketSpaceStorage.Registry registry = PocketSpaceStorage.registry(server);
		if (registry.rooms().isEmpty()) return;
		long now = server.overworld().getGameTime();
		java.util.Set<UUID> seen = new java.util.HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) collectScroll(player.getInventory().getItem(i), seen);
			ItemStack book = github.com.gengyoubo.sscfa.spell.SpellCastManager.getEquippedBook(player);
			if (book != null) {
				for (int slot = 0; slot < github.com.gengyoubo.sscfa.spell.SpellbookData.getSlotCount(book); slot++) {
					collectScroll(github.com.gengyoubo.sscfa.spell.SpellbookData.getScroll(book, slot), seen);
				}
			}
		}
		for (ServerLevel world : server.getAllLevels()) {
			for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
				if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) collectScroll(item.getItem(), seen);
			}
		}
		for (PocketSpaceStorage.Room room : new java.util.ArrayList<>(registry.rooms())) {
			if (seen.contains(room.scrollId)) {
				room.touch(now);
				registry.setDirty();
			}
		}
	}

	private static void collectScroll(ItemStack stack, java.util.Set<UUID> seen) {
		if (stack.isEmpty()) return;
		net.minecraft.nbt.CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.hasUUID(PocketSpaceStorage.SCROLL_ID)) seen.add(nbt.getUUID(PocketSpaceStorage.SCROLL_ID));
		// 卷轴装在别的容器物品里（如魔法书在背包而非佩戴）：递归??Items 列??
		if (nbt != null && nbt.contains(github.com.gengyoubo.sscfa.spell.SpellbookData.NBT_ITEMS, 9)) {
			net.minecraft.nbt.ListTag list = nbt.getList(github.com.gengyoubo.sscfa.spell.SpellbookData.NBT_ITEMS, 10);
			for (int i = 0; i < list.size(); i++) collectScroll(ItemStack.of(list.getCompound(i)), seen);
		}
	}

	/** 开始消除：房内无人立即清空；有人则屏幕中央提??5 秒后送回再清空?*/
	private static void beginEviction(MinecraftServer server, ServerLevel pocket, PocketSpaceStorage.Room room) {
		java.util.List<ServerPlayer> inside = playersInRoom(server, room);
		if (inside.isEmpty()) {
			wipe(server, pocket, room);
			return;
		}
		room.collapsing = true;
		PocketSpaceStorage.registry(server).setDirty();
		if (!PocketSpaceStorage.persistRegistry(server)) {
			room.collapsing = false;
			PocketSpaceStorage.registry(server).setDirty();
			return;
		}
		EVICTIONS.put(room.layout().index(), new Eviction(room.scrollId, EVICTION_TICKS));
		for (ServerPlayer player : inside) {
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 90, 10));
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
					Component.translatable("message.ssc_addon.pocket_space.collapse_title")));
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
					Component.translatable("message.ssc_addon.pocket_space.collapse_subtitle", EVICTION_TICKS / 20)));
		}
	}

	private static void tickEvictions(MinecraftServer server, ServerLevel pocket) {
		Iterator<Map.Entry<Integer, Eviction>> it = EVICTIONS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Eviction> entry = it.next();
			PocketSpaceStorage.Room room = PocketSpaceStorage.registry(server).bySlot.get(entry.getKey());
			if (room == null || !room.scrollId.equals(entry.getValue().scrollId)) { it.remove(); continue; }
			int remaining = entry.getValue().ticks - 1;
			if (remaining > 0) {
				entry.setValue(new Eviction(room.scrollId, remaining));
				continue;
			}
			for (ServerPlayer player : playersInRoom(server, room)) {
				leave(player, PocketSpaceStorage.visit(player));
			}
			wipe(server, pocket, room);
			if (PocketSpaceStorage.findRoom(server, room.scrollId) == room) {
				entry.setValue(new Eviction(room.scrollId, 20));
			} else {
				it.remove();
			}
		}
	}

	private static java.util.List<ServerPlayer> playersInRoom(MinecraftServer server, PocketSpaceStorage.Room room) {
		java.util.List<ServerPlayer> list = new java.util.ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (isPocket(player.level())
					&& PocketSpaceLayout.indexAt(player.getBlockX(), player.getBlockZ()) == room.layout().index()) {
				list.add(player);
			}
		}
		return list;
	}

	/** 消除房间：先保存注销与预留槽位，再分批清空、移除房内非玩家实体?*/
	private static void wipe(MinecraftServer server, ServerLevel pocket, PocketSpaceStorage.Room room) {
		int slot = room.layout().index();
		if (!PocketSpaceStorage.unbind(server, room)) return;
		GENERATIONS.remove(slot);
		WIPES.put(slot, new Wipe(server, slot, room.scrollId));
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
				PocketSpaceLayout.slotBoundsMinX(slot), PocketSpaceLayout.FLOOR_Y, PocketSpaceLayout.slotBoundsMinZ(slot),
				PocketSpaceLayout.slotBoundsMaxX(slot) + 1, PocketSpaceLayout.slotBoundsMaxY() + 1, PocketSpaceLayout.slotBoundsMaxZ(slot) + 1);
		for (net.minecraft.world.entity.Entity entity : pocket.getEntities(null, box)) {
			if (!(entity instanceof ServerPlayer)) entity.discard();
		}
	}

	/**
	 * 逐玩家区块过滤：优先按访问记录解析所在房间（传送前就已写入，不受玩家坐标临时值影响）??
	 * 允许范围 = 真实房间外包围盒完全覆盖的整区块（再外??1 区块裕量）；查无房间时放行——\t * 宁可多发不可漏发，漏发即永久透明空洞?
	 */
	public static boolean shouldSendChunk(ServerPlayer player, net.minecraft.world.level.ChunkPos chunk) {
		if (!isPocket(player.level())) return true;
		PocketSpaceStorage.Room room = null;
		PocketSpaceStorage.Visit visit = PocketSpaceStorage.visit(player);
		if (visit.active()) room = PocketSpaceStorage.findRoom(player.getServer(), visit.roomId());
		if (room == null) {
			int slot = PocketSpaceLayout.indexAt(player.getBlockX(), player.getBlockZ());
			if (slot >= 0) room = PocketSpaceStorage.registry(player.getServer()).bySlot.get(slot);
		}
		if (room == null) return true;
		PocketSpaceLayout layout = room.layout();
		int minChunkX = Math.floorDiv(layout.boundsMinX() - 16, 16);
		int maxChunkX = (layout.boundsMaxX() + 16) >> 4;
		int minChunkZ = Math.floorDiv(layout.boundsMinZ() - 16, 16);
		int maxChunkZ = (layout.boundsMaxZ() + 16) >> 4;
		return chunk.x >= minChunkX && chunk.x <= maxChunkX && chunk.z >= minChunkZ && chunk.z <= maxChunkZ;
	}

	private static void enter(ServerPlayer player, Channel channel, ServerLevel pocket, PocketSpaceStorage.Room room) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) return;
		PocketSpaceStorage.Visit visit = PocketSpaceStorage.visit(player);
		visit.begin(channel.scrollId, channel.dimension, channel.position, channel.yaw, channel.pitch);
		PocketSpaceStorage.save(player.getServer());
		teleportToPortal(player, pocket, room.layout());
		GATES.put(player.getUUID(), new PocketPortalGate());
		message(player, "arrival");
	}

	private static void repairPortal(ServerLevel pocket, PocketSpaceLayout layout) {
		for (int blockX = layout.centerX() - 1; blockX <= layout.centerX(); blockX++) {
			for (int blockZ = layout.minZ() - 3; blockZ <= layout.minZ() - 2; blockZ++) {
				BlockPos pos = new BlockPos(blockX, PocketSpaceLayout.FLOOR_Y + 1, blockZ);
				BlockState current = pocket.getBlockState(pos);
				if (current.is(AddonBlocks.POCKET_SPACE_PORTAL.get())) {
					BlockState expected = portalState(layout, blockX, blockZ);
					if (current != expected) pocket.setBlock(pos, expected, Block.UPDATE_CLIENTS);
					if (pocket.getBlockEntity(pos) == null) {
						pocket.setBlockEntity(new PocketSpaceBlocks.PortalBlockEntity(pos, pocket.getBlockState(pos)));
					}
				}
			}
		}
	}

	private static void teleportToPortal(ServerPlayer player, ServerLevel pocket, PocketSpaceLayout layout) {
		repairPortal(pocket, layout);
		player.teleportTo(pocket, layout.centerX(), PocketSpaceLayout.FLOOR_Y + 1 + PocketSpaceLayout.PORTAL_TOP_HEIGHT,
				layout.minZ() - 2, 0, 0);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0;
		pocket.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.7f, 1.2f);
	}

	/**
	 * 出口传送台法阵粒子?026-09-19 用户定稿）：站上传送台倒计时期间??
	 * 以法?2?? 区域为界冒紫色上升粒子流——DRAGON_BREATH（龙息紫，服务端发??
	 * 自带向上飘浮初速，观感与施法头顶流一致?? PORTAL 传送门紫点缀??
	 * ??5t 一轮（视觉连续且包量克制），服务端撒天然多人同步?
	 */
	private static void spawnExitFx(ServerLevel pocket, PocketSpaceLayout layout) {
		// 法阵中心??? 台面的几何中心）
		double cx = layout.centerX();
		double cz = layout.minZ() - 2.0;
		double cy = PocketSpaceLayout.FLOOR_Y + 1 + PocketSpaceLayout.PORTAL_TOP_HEIGHT + 0.05;
		// 龙息紫上升流：法阵范围内随机起爆?? 格为界），自带缓??
		pocket.sendParticles(ParticleTypes.DRAGON_BREATH,
				cx, cy, cz, 14, 0.8, 0.08, 0.8, 0.01);
		// 传送门紫：少量高亮点缀（旋升感?
		pocket.sendParticles(ParticleTypes.PORTAL,
				cx, cy + 0.1, cz, 6, 0.7, 0.15, 0.7, 0.3);
	}

	private static void leave(ServerPlayer player, PocketSpaceStorage.Visit visit) {
		ServerLevel destination = player.getServer().getLevel(visit.dimension());
		if (destination == null || isPocket(destination)) { message(player, "return_unavailable"); return; }
		Vec3 position = visit.position();
		player.teleportTo(destination, position.x, position.y, position.z, visit.yaw(), visit.pitch());
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0;
		visit.clear();
		PocketSpaceStorage.save(player.getServer());
		destination.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.7f, 1.0f);
	}

	private static void interrupt(ServerPlayer player) {
		if (CHANNELS.remove(player.getUUID()) != null) {
			clearSlow(player);
			message(player, "interrupted");
		}
	}

	private static void clearSlow(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.removeModifier(CHANNEL_SLOW_ID);
	}

	public static void setCancelRefund(ServerPlayer player, Runnable refund) {
		Channel channel = CHANNELS.get(player.getUUID());
		if (channel != null) channel.cancelRefund = refund;
	}

	public static boolean cancelByKey(ServerPlayer player) {
		Channel channel = CHANNELS.remove(player.getUUID());
		if (channel == null) return false;
		clearSlow(player);
		if (channel.cancelRefund != null) channel.cancelRefund.run();
		message(player, "cancelled");
		return true;
	}

	public static void message(Player player, String key, Object... args) {
		player.displayClientMessage(Component.translatable("message.ssc_addon.pocket_space." + key, args), true);
	}

	public static boolean canChangeBlock(ServerLevel world, BlockPos pos) {
		if (generating || !isPocket(world)) return true;
		PocketSpaceStorage.Room room = PocketSpaceStorage.roomAt(world.getServer(), pos.getX(), pos.getZ());
		if (room == null) return false;
		PocketSpaceLayout layout = room.layout();
		return layout.isInterior(pos.getX(), pos.getY(), pos.getZ())
				&& !layout.isAlcove(pos.getX(), pos.getY(), pos.getZ());
	}

	private static final class Channel {
		final UUID scrollId;
		final ResourceKey<Level> dimension;
		final Vec3 position;
		final float yaw;
		final float pitch;
		int ticks;
		Runnable cancelRefund;

		Channel(ServerPlayer player, UUID scrollId, int ticks) {
			this.scrollId = scrollId;
			this.dimension = player.level().dimension();
			this.position = player.position();
			this.yaw = player.getYRot();
			this.pitch = player.getXRot();
			this.ticks = ticks;
		}
	}

	/** 分批清空一个槽位的全部方块（按 Lv5 最大包围盒），与生成共??generating 开关绕过边界保护?*/
	private static final class Wipe {
		final MinecraftServer server;
		final int slot;
		final UUID scrollId;
		final int minX, minZ, width, depth, height;
		int cursor;

		Wipe(MinecraftServer server, int slot, UUID scrollId) {
			this.server = server;
			this.slot = slot;
			this.scrollId = scrollId;
			this.minX = PocketSpaceLayout.slotBoundsMinX(slot);
			this.minZ = PocketSpaceLayout.slotBoundsMinZ(slot);
			this.width = PocketSpaceLayout.slotBoundsMaxX(slot) - minX + 1;
			this.depth = PocketSpaceLayout.slotBoundsMaxZ(slot) - minZ + 1;
			this.height = PocketSpaceLayout.slotBoundsMaxY() - PocketSpaceLayout.FLOOR_Y + 1;
		}

		boolean belongsTo(MinecraftServer current, PocketSpaceStorage.Registry registry) {
			return server == current && !registry.bySlot.containsKey(slot)
					&& scrollId.equals(registry.pendingWipes.get(slot));
		}

		boolean done() { return cursor >= width * depth * height; }

		int advance(ServerLevel world, int budget) {
			int used = 0;
			generating = true;
			try {
				BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
				while (!done() && used < budget) {
					int blockX = minX + cursor % width;
					int blockZ = minZ + cursor / width % depth;
					int blockY = PocketSpaceLayout.FLOOR_Y + cursor / (width * depth);
					cursor++;
					used++;
					BlockPos pos = new BlockPos(blockX, blockY, blockZ);
					if (world.getBlockState(pos).isAir()) continue;
					net.minecraft.world.Clearable.tryClear(world.getBlockEntity(pos)); // 容器内容不掉落，直接抹除
					world.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE);
				}
			} finally {
				generating = false;
			}
			return used;
		}
	}

	private record Eviction(UUID scrollId, int ticks) {}

	private static final class Generation {
		final PocketSpaceStorage.Room room;
		/** 先清空槽位最大包围盒（覆盖旧世界残留/换级旧块），再生成外壳??*/
		final int cleanMinX, cleanMinZ, cleanW, cleanD, cleanH;
		int cleanCursor;
		int cursor;

		Generation(PocketSpaceStorage.Room room) {
			this.room = room;
			int slot = room.layout().index();
			this.cleanMinX = PocketSpaceLayout.slotBoundsMinX(slot);
			this.cleanMinZ = PocketSpaceLayout.slotBoundsMinZ(slot);
			this.cleanW = PocketSpaceLayout.slotBoundsMaxX(slot) - cleanMinX + 1;
			this.cleanD = PocketSpaceLayout.slotBoundsMaxZ(slot) - cleanMinZ + 1;
			this.cleanH = PocketSpaceLayout.slotBoundsMaxY() - PocketSpaceLayout.FLOOR_Y + 1;
		}

		boolean done() {
			int width = room.layout().size() + 2;
			return cleanCursor >= cleanW * cleanD * cleanH
					&& cursor >= width * width * (width + 4);
		}

		int advance(ServerLevel world, int budget) {
			PocketSpaceLayout layout = room.layout();
			int width = layout.size() + 2;
			int depth = width + 4;
			int used = 0;
			generating = true;
			try {
				BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
				while (cleanCursor < cleanW * cleanD * cleanH && used < budget) {
					int blockX = cleanMinX + cleanCursor % cleanW;
					int blockZ = cleanMinZ + cleanCursor / cleanW % cleanD;
					int blockY = PocketSpaceLayout.FLOOR_Y + cleanCursor / (cleanW * cleanD);
					cleanCursor++;
					used++;
					BlockPos pos = new BlockPos(blockX, blockY, blockZ);
					if (world.getBlockState(pos).isAir()) continue;
					net.minecraft.world.Clearable.tryClear(world.getBlockEntity(pos));
					world.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE);
				}
				while (!done() && used < budget) {
					int blockX = layout.minX() - 1 + cursor % width;
					int blockZ = layout.minZ() - 5 + cursor / width % depth;
					int blockY = PocketSpaceLayout.FLOOR_Y + cursor / (width * depth);
					cursor++;
					used++;
					BlockState state = null;
					if (layout.isPortal(blockX, blockY, blockZ)) state = portalState(layout, blockX, blockZ);
					else if (layout.isShell(blockX, blockY, blockZ)) {
						state = blockY == PocketSpaceLayout.FLOOR_Y ? AddonBlocks.POCKET_SPACE_FLOOR.get().defaultBlockState()
								: AddonBlocks.POCKET_SPACE_BEDROCK_WALL.get().defaultBlockState();
					}
					if (state != null) world.setBlock(new BlockPos(blockX, blockY, blockZ), state, Block.UPDATE_CLIENTS);
				}
			} finally {
				generating = false;
			}
			return used;
		}
	}

	/** 出口方块按它在出口凹槽里的位置取朝向。 */
	private static BlockState portalState(PocketSpaceLayout layout, int blockX, int blockZ) {
		net.minecraft.core.Direction facing = switch (layout.portalRotation(blockX, blockZ)) {
			case 90 -> net.minecraft.core.Direction.EAST;
			case 180 -> net.minecraft.core.Direction.SOUTH;
			case 270 -> net.minecraft.core.Direction.WEST;
			default -> net.minecraft.core.Direction.NORTH;
		};
		return AddonBlocks.POCKET_SPACE_PORTAL.get().defaultBlockState().setValue(PocketSpaceBlocks.PORTAL_FACING, facing);
	}
}
