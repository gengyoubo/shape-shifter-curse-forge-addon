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
	/** 閹村潡妫垮☉鍫ユ珟閸婃帟顓搁弮璁圭礄濡叉垝缍?閳?閸撯晙缍?tick閿涘绱伴幋鍨敶閺堝姹夐弮璺哄帥鐏炲繐绠锋稉顓炪亷閹绘劗銇?5 缁夋帒鍟€闁礁娲栭妴?*/
	private static final Map<Integer, Eviction> EVICTIONS = new HashMap<>();
	/** 瀹稿弶甯撻梼鐔哥缁岃櫣娈戝Σ鎴掔秴閿涘牓鈧?tick 閸掑棙澹掑〒鍜冪礆閵?*/
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

	// 閸掑棙顔岀€瑰牆宕奸敍鍫ユ姜閺佺繝缍嬮惌顓＄熅閿涘绱皊etTimeOfDay/鐟欏棜绐?census 韫囧懘銆忛悡褍鐖剁捄鎴礉娑撴柣鈧本婀佹禍鍝勬躬
	//  閸欙綀顣扮紒鏉戝閵嗗秶娈戝Λ鈧ù瀣拱闊偄姘ㄩ棁鈧憰浣哄负鐎硅泛鎯婇悳顖椻偓鏂衡偓鏃€鏆ｆ担鎾剁叚鐠侯垯绗夐崣顖濐攽閿涙稒鏁兼稉鍝勬倗鏉╊厺鍞崳銊︻唽閸栧懐鈹栫悰銊ョ暓閸楊偁鈧笩ATES.remove
	//  閸?!GATES.isEmpty()閿涘矁顢戞稉杞扮瑝閸欐﹫绱濈敮鍛娾偓渚婄礄閸ユ稖銆冮崗銊р敄閿涘褰ч崜鈺傜槨閻溾晛顔嶆稉鈧▎?isPocket 閸掋倖鏌囬妴?
	private static void tick(MinecraftServer server) {
		ensureServer(server);
		ServerLevel pocket = server.getLevel(WORLD_KEY);
		if (pocket == null) return;
		recoverTasks(server);
		pocket.setDayTime(6000); // 閸欙綀顣扮紒鏉戝閺冪姵妞傞梻瀛橆洤韫囩绱板姝岀箼濮濓絽宕?
		// 閸欙綀顣扮紒鏉戝鐟欏倸鐧傜捄婵堫瀲閸樺鍩?3閿涙碍婀囬崝鈥虫珤閸欘亙璐熼悳鈺侇啀閸旂姾娴囬崨銊ユ纯 7鑴? 閸栧搫娼￠敍鍫ｅ喕娴犮儴顩惄鏍︽崲閹板繒鐡戠痪褎鍩ч梻瀵告磪閿涘绱?
		// 閼板矂娼崗銊潒鐠?441 娑擃亜灏崸妞烩偓鏂衡偓鏃囩箹閺勵垪鈧粏绻橀崗銉ュ幢瀵板牅绠欓垾婵堟畱娑撶粯绨妴鍌氭噯閺堢喖鍣哥拋楣冩Щ閸忋劌鐪憴鍡氱獩閸欐ɑ娲块幎濠傜暊閺€鐟版礀閸樻眹鈧?
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
				// owner 閹垫挻鐖ｉ敍姘矌閺堫兛姹夌粭顑跨娴滆櫣袨闁儻顔€閿涘牆绱╃€靛ジ妫梿鎾呯礆
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
				// 閹村潡妫垮鍙夌Х闂勩倛鈧瞼甯虹€规湹绮涢崷銊ф樊鎼达箑鍞撮敍鍫濐洤濞戝牓娅庨弮鍓侇瀲缁惧尅绱氶敍姘辨纯閹恒儵鈧礁娲栭崢鐔剁秴缂?
				leave(player, visit);
				continue;
			}
			if (EVICTIONS.containsKey(room.layout().index())) continue; // 閸婃帟顓搁弮鏈佃厬閿涘瞼鏁?tickEvictions 婢跺嫮鎮?
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
			// 閸戝搫褰涢崐鎺曨吀閺冭埖婀￠梻杈剧窗娴肩娀鈧礁褰村▔鏇㈡█閸愭帞浼犻懝韫瑐閸楀洨鐭戠€涙劖绁﹂敍鍫滆雹閺傝姤纭跺鏂垮毉閿涘苯顓归幋椋庮伂閺堫剙婀村〒鍙夌厠閿?
			if (gate.counting() && player.tickCount % 5 == 0) {
				spawnExitFx(pocket, layout);
			}
		}
	}

	/**
	 * 閸掗攱鏌婂鑼额潎鐎电喎鍩岄崡鐤叡閻ㄥ嫬绺剧捄鐐解偓鍌滎唸鐎涙劑鈧胶顬囩痪璺ㄥ负鐎硅泛鎷伴張顏勫鏉炶棄鐤勬担鎾存￥濞夋洝顫﹀銈嗗閹诲繑娅橀弻銉幢
	 * 閺堫亣顫︾憴鍌氱檪閸掗绗夌粵澶夌艾閸楃柉閰卞☉鍫濄亼閿涘奔绡冩稉宥勭窗鐟欙箑褰傞幋鍧楁？妞归亶鈧劖鍨ㄥ〒鍛敄閵?
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
		// 閸楃柉閰辩憗鍛躬閸掝偆娈戠€圭懓娅掗悧鈺佹惂闁插矉绱欐俊鍌炵摕濞夋洑鍔熼崷銊ㄥ剹閸栧懓鈧矂娼担鈺傚煋閿涘绱伴柅鎺戠秺閹?Items 閸掓銆?
		if (nbt != null && nbt.contains(github.com.gengyoubo.sscfa.spell.SpellbookData.NBT_ITEMS, 9)) {
			net.minecraft.nbt.ListTag list = nbt.getList(github.com.gengyoubo.sscfa.spell.SpellbookData.NBT_ITEMS, 10);
			for (int i = 0; i < list.size(); i++) collectScroll(ItemStack.of(list.getCompound(i)), seen);
		}
	}

	/** 瀵偓婵绉烽梽銈忕窗閹村灝鍞撮弮鐘辨眽缁斿宓嗗〒鍛敄閿涙稒婀佹禍鍝勫灟鐏炲繐绠锋稉顓炪亷閹绘劗銇?5 缁夋帒鎮楅柅浣告礀閸愬秵绔荤粚鎭掆偓?*/
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

	/** 濞戝牓娅庨幋鍧楁？閿涙艾鍘涙穱婵嗙摠濞夈劑鏀㈡稉搴暕閻ｆ瑦蝎娴ｅ稄绱濋崘宥呭瀻閹佃绔荤粚鎭掆偓浣盒╅梽銈嗗煣閸愬懘娼悳鈺侇啀鐎圭偘缍嬮妴?*/
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
	 * 闁劗甯虹€硅泛灏崸妤勭箖濠娿倧绱版导妯哄帥閹稿顔栭梻顔款唶瑜版洝袙閺嬫劖澧嶉崷銊﹀煣闂傝揪绱欐导鐘烩偓浣稿鐏忓崬鍑￠崘娆忓弳閿涘奔绗夐崣妤冨负鐎硅泛娼楅弽鍥﹀閺冭泛鈧厧濂栭崫宥忕礆閿?
	 * 閸忎浇顔忛懠鍐ㄦ纯 = 閻喎鐤勯幋鍧楁？婢舵牕瀵橀崶瀵告磪鐎瑰苯鍙忕憰鍡欐磰閻ㄥ嫭鏆ｉ崠鍝勬健閿涘牆鍟€婢舵牗澧?1 閸栧搫娼＄憗鏇㈠櫤閿涘绱遍弻銉︽￥閹村潡妫块弮鑸垫杹鐞涘备鈧柡鈧摶t * 鐎逛礁褰叉径姘絺娑撳秴褰插蹇撳絺閿涘本绱￠崣鎴濆祮濮橀晲绠欓柅蹇旀缁岀儤绀婇妴?
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
				if (current.is(PocketSpaceBlocks.PORTAL)) {
					BlockState expected = PocketSpaceBlocks.portalState(layout, blockX, blockZ);
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
	 * 閸戝搫褰涙导鐘烩偓浣稿酱濞夋洟妯€缁帒鐡欓敍?026-09-19 閻劍鍩涚€规氨顭堥敍澶涚窗缁旀瑤绗傛导鐘烩偓浣稿酱閸婃帟顓搁弮鑸垫埂闂傝揪绱?
	 * 娴犮儲纭堕梼?2鑴? 閸栧搫鐓欐稉铏规櫕閸愭帞浼犻懝韫瑐閸楀洨鐭戠€涙劖绁﹂垾鏂衡偓鎿燫AGON_BREATH閿涘牓绶抽幁顖滀紶閿涘本婀囬崝锛勵伂閸欐垵鐨?
	 * 閼奉亜鐢崥鎴滅瑐妞嬫ɑ璇為崚婵嬧偓鐕傜礉鐟欏倹鍔呮稉搴㈡煢濞夋洖銇旀い鑸电ウ娑撯偓閼疯揪绱? PORTAL 娴肩娀鈧線妫槐顐ゅ仯缂傗偓閿?
	 * 濮?5t 娑撯偓鏉烆噯绱欑憴鍡氼潕鏉╃偟鐢绘稉鏂垮瘶闁插繐鍘犻崚璁圭礆閿涘本婀囬崝锛勵伂閹炬帒銇夐悞璺侯樋娴滃搫鎮撳銉ｂ偓?
	 */
	private static void spawnExitFx(ServerLevel pocket, PocketSpaceLayout layout) {
		// 濞夋洟妯€娑擃厼绺鹃敍?鑴? 閸欎即娼伴惃鍕殤娴ｆ洑鑵戣箛鍐跨礆
		double cx = layout.centerX();
		double cz = layout.minZ() - 2.0;
		double cy = PocketSpaceLayout.FLOOR_Y + 1 + PocketSpaceLayout.PORTAL_TOP_HEIGHT + 0.05;
		// 姒瑦浼呯槐顐＄瑐閸楀洦绁﹂敍姘《闂冧絻瀵栭崶鏉戝敶闂呭繑婧€鐠ч鍨庨敍鍩? 閺嶉棿璐熼悾宀嬬礆閿涘矁鍤滅敮锔剧处閸?
		pocket.sendParticles(ParticleTypes.DRAGON_BREATH,
				cx, cy, cz, 14, 0.8, 0.08, 0.8, 0.01);
		// 娴肩娀鈧線妫槐顐窗鐏忔垿鍣烘妯瑰瘨閻愬湱绱戦敍鍫熸閸楀洦鍔呴敍?
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

	/** 閸掑棙澹掑〒鍛敄娑撯偓娑擃亝蝎娴ｅ秶娈戦崗銊╁劥閺傜懓娼￠敍鍫熷瘻 Lv5 閺堚偓婢堆冨瘶閸ュ娲呴敍澶涚礉娑撳海鏁撻幋鎰彙閻?generating 瀵偓閸忓磭绮潻鍥珶閻ｅ奔绻氶幎銈冣偓?*/
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
					net.minecraft.world.Clearable.tryClear(world.getBlockEntity(pos)); // 鐎圭懓娅掗崘鍛啇娑撳秵甯€閽€鏂ょ礉閻╁瓨甯撮幎褰掓珟
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
		/** 閸忓牊绔荤粚鐑樞担宥嗘付婢堆冨瘶閸ュ娲呴敍鍫ｎ洬閻╂牗妫稉鏍櫕濞堝鏆€/閹广垻楠囬弮褍娼￠敍澶涚礉閸愬秶鏁撻幋鎰樆婢圭偨鈧?*/
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
					if (layout.isPortal(blockX, blockY, blockZ)) state = PocketSpaceBlocks.portalState(layout, blockX, blockZ);
					else if (layout.isShell(blockX, blockY, blockZ)) {
						state = blockY == PocketSpaceLayout.FLOOR_Y ? PocketSpaceBlocks.FLOOR.defaultBlockState()
								: PocketSpaceBlocks.BEDROCK_WALL.defaultBlockState();
					}
					if (state != null) world.setBlock(new BlockPos(blockX, blockY, blockZ), state, Block.UPDATE_CLIENTS);
				}
			} finally {
				generating = false;
			}
			return used;
		}
	}
}
