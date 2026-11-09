package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 濂戠伒琚姩琛屼负缁熶竴澶勭悊鍣紙鏈嶅姟绔級銆?
 * 鑱岃矗锛?
 *  - mana 鑷劧鎭㈠ + 鍛ㄥ洿鍔帬鐢熺墿 bonus锛堟瘡 3s +min(count,5)锛?
 *  - 25% 姒傜巼鏉戝簞琚嚮浜嬩欢瑙﹀彂涓庡畬鎴愬鍔?
 *  - 鏉戞皯/鍟嗕汉閬鐏靛嚮鏉€鐨勭墿鍝佹帀钀?
 *  - 鏉戞皯/鍟嗕汉閫冭窇鍒ゅ畾锛堢櫧鍚嶅崟鍐呯殑濂戠伒涓嶄細鍚撹窇锛?
 *
 * 娉細鎶椾激鎭㈠ / 鎴樻枟鐘舵€?鐢?MancianimaMarkManager 澶勭悊锛?
 *     鑽按鍏嶇柅 鐢?form_familiar_fox_mancianima_no_buff_effect.json 澶勭悊銆?
 */
public final class MancianimaPassive {
	private MancianimaPassive() {}

	private static final Map<UUID, AssaultData> ASSAULTS = new ConcurrentHashMap<>();
	private static final long ASSAULT_ROLL_COOLDOWN = 24000L; // 1 MC 澶╁唴鍙兘琚嚮 1 娆★紙璺ㄩ噸鍚寔涔呭寲锛屼粎鍦ㄢ€滄垚鍔熷畬鎴愨€濆悗璁板叆锛?
	private static final double ASSAULT_ABORT_DISTANCE_SQ = 96.0 * 96.0; // 瓒呰繃璧风偣 96 鏍煎垯涓
	private static final int ASSAULT_PREPARE_TICKS = 200; // 璇绘潯 10s
	private static final int RAIDER_PILLAGER_COUNT = 4;
	private static final int RAIDER_VINDICATOR_COUNT = 2;
	private static final int RAIDER_LINGER_MIN_TICKS = 1200; // 琚嚮缁撴潫鍚庡湪鏉戝簞閫楃暀鏈€灏?1 鍒嗛挓
	private static final int RAIDER_LINGER_MAX_TICKS = 6000; // 鏈€澶?5 鍒嗛挓
	private static final int RAIDER_MARCH_DESPAWN_DISTANCE_SQ = 128 * 128; // 琛屽啗闃舵瓒呰繃 128 鏍兼棤鐜╁鍒欐竻鐞?
	private static final int RAIDER_MARCH_DISTANCE = 200; // 琛屽啗鐩爣锛氳繙绂绘潙搴?200 鏍肩殑浣嶇疆

	/** 鍔帬鍐涢槦浼嶏細璺熻釜琚嚮 spawn 鍑虹殑 raider锛岃礋璐?linger 鈫?march 鈫?鑴遍€傚悗娓呯悊銆?*/
	private enum RaiderPhase { ACTIVE, LINGER, MARCH }
	private static class RaiderGroup {
		final Set<UUID> raiderUuids = ConcurrentHashMap.newKeySet();
		final net.minecraft.resources.ResourceLocation dimensionId;
		final BlockPos villageCenter;
		RaiderPhase phase = RaiderPhase.ACTIVE;
		long phaseEndTime = Long.MAX_VALUE;
		RaiderGroup(net.minecraft.resources.ResourceLocation dim, BlockPos center) {
			this.dimensionId = dim;
			this.villageCenter = center;
		}
	}
	/** key = 鐜╁ UUID锛堝悓涓€鐜╁鍚屾椂鍙兘鎷ユ湁涓€缁勫姭鎺犲啗锛夈€?*/
	private static final Map<UUID, RaiderGroup> RAIDER_GROUPS = new ConcurrentHashMap<>();

	// 鈥滆鍚涗复鈥濊繕鍦ㄧ敓鏁堢殑鏉戞皯/鍟嗕汉 UUID + 涓婃鍒锋柊 tick
	private static final Map<UUID, Long> FLEE_AFFECTED = new ConcurrentHashMap<>();
	private static final UUID MANCIANIMA_FEAR_SLOW_UUID = UUID.fromString("7f3e2c4a-3b16-4ad2-9d4d-2bf1d8a5d111");
	private static final double FLEE_RADIUS = 12.0;
	private static final int FLEE_LINGER_TICKS = 60; // 绂诲紑鍚?3s 鎵嶆竻闄?debuff

	private static final Random RNG = new Random();

	public static void tick(ServerPlayer player) {
		// 蹇€熻矾寰勶細琚嚮璇绘潯姣?tick 鎺ㄨ繘锛坆ossbar 鐧惧垎姣斿埛鏂伴渶瑕侊級锛屽厛鐢ㄥ粔浠?map 鏌ヨ鎷︽埅娲昏穬浼氳瘽锛?
		// 鏃犳椿璺冭鍑绘椂鍏朵綑閫昏緫鍏ㄤ负鍛ㄦ湡鎬э紙%10/%60锛夛紝闄嶉鍒?%10 鍐嶅仛 CCA 褰㈡€佹煡璇紝
		// 闈炲鐏电帺瀹舵瘡 tick 鍙姳涓€娆?isEmpty/鍙栦綑锛堝師姣?tick 閮借繃涓€娆″舰鎬佹煡璇級
		if (!ASSAULTS.isEmpty() && ASSAULTS.containsKey(player.getUUID())) {
			tickAssaultPrepare(player);
		}
		if (player.tickCount % 10 != 0) return;
		if (!FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) return;
		// 椹遍€冮€昏緫锛氭瘡 10t 妫€涓€娆★紝浣庡紑閿€
		tickFlee(player);
		// 琚嚮璇绘潯鎺ㄨ繘锛堟瘡 tick锛屾椿璺冧細璇濆凡鍦ㄤ笂鏂瑰揩閫熻矾寰勫鐞嗭級
		// 琚嚮瓒呭嚭鑼冨洿妫€鏌ワ紙姣?20t锛?
		if (player.tickCount % 20 == 0) checkAssaultRange(player);
		if (player.tickCount % 60 != 0) return;
		// 1. mana 鍔帬 bonus锛堟瘡 3s锛?
		int raiderCount = countNearbyRaiders(player, 10.0);
		if (raiderCount > 0) {
			github.com.gengyoubo.sscfa.compat.ManaUtils.gainPlayerMana(player, Math.min(raiderCount, 5));
		}
		// 2. 杩囨湡鍗歌浇椹遍€冧慨楗扮
		cleanupExpiredFlee(player.level().getGameTime());
	}

	/** 鏁烽紟琚嚮瑙﹀彂鍏ュ彛锛氭暦閽熷悗璋冪敤銆傝繑鍥炴槸鍚︽垚鍔熻Е鍙戙€?*/
	public static boolean tryTriggerAssaultByBell(ServerPlayer player) {
		if (!FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) return false;
		UUID pid = player.getUUID();
		if (ASSAULTS.containsKey(pid)) return false; // 姝ｅ湪杩涜涓?
		long now = player.level().getGameTime();
		net.minecraft.server.MinecraftServer server = player.getServer();
		MancianimaAssaultState state = server == null ? null : MancianimaAssaultState.get(server);
		Long last = state == null ? null : state.lastRoll.get(pid);
		if (last != null && now - last < ASSAULT_ROLL_COOLDOWN) {
			player.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.cooldown"), true);
			return false;
		}
		AABB markBox = player.getBoundingBox().inflate(64.0);
		Set<UUID> villagerTargets = new HashSet<>();
		List<AbstractVillager> merchants = player.level().getEntitiesOfClass(AbstractVillager.class, markBox,
				e -> e.isAlive() && !e.isRemoved());
		for (AbstractVillager m : merchants) villagerTargets.add(m.getUUID());
		if (villagerTargets.isEmpty()) {
			player.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.no_villagers"), true);
			return false;
		}
		// 浠呰嚜瀹氫箟琚嚮閫昏緫锛氫笉鏂藉姞 BAD_OMEN 涔熶笉鍙潵鍘熺増 raid锛岄伩鍏嶄笌鍘熺増琚嚮 bossbar 鍐茬獊
		// 璁╅檮杩戦搧鍌€鍎′富鍔ㄨ拷鍑诲鐏?
		for (IronGolem g : player.level().getEntitiesOfClass(IronGolem.class, markBox,
				e -> e.isAlive() && !e.isRemoved())) {
			g.setTarget(player);
			g.setLastHurtByMob(player);
		}
		// 鍒涘缓璇绘潯 bossbar锛堥粍鑹诧紝10s鍚庡垏鎹负琚嚮 bossbar锛?
		ServerBossEvent prepareBar = new ServerBossEvent(
				Component.translatable("ssc_addon.mancianima.assault.preparing"),
				BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		prepareBar.setProgress(1.0f);
		prepareBar.addPlayer(player);
		AssaultData data = new AssaultData(villagerTargets, prepareBar,
				player.level().dimension().location(), player.position(), now);
		data.prepareTicks = ASSAULT_PREPARE_TICKS;
		ASSAULTS.put(pid, data);
		// 娉細鍐峰嵈浠呭湪 onAssaultTargetDeath 琚嚮鎴愬姛瀹屾垚鍚庢墠璁板叆锛岃蛋杩?涓嬬嚎/閲嶅惎涓嶆壄鍐峰嵈
		player.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.preparing_msg", villagerTargets.size()), false);
		if (player.level() instanceof ServerLevel sw) {
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.RAID_HORN.value(), player.getSoundSource(), 1.0f, 0.9f);
		}
		return true;
	}

	private static void tickFlee(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(FLEE_RADIUS);
		long now = player.level().getGameTime();
		List<AbstractVillager> merchants = player.level().getEntitiesOfClass(AbstractVillager.class, box,
				e -> e.isAlive() && !e.isRemoved()
						&& (e instanceof Villager || e instanceof WanderingTrader));
		for (AbstractVillager m : merchants) {
			if (WhitelistUtils.isProtected(player, m)) continue;
			// 绾€垫ⅵ鍚戦噺閫€閬?
			Vec3 away = m.position().subtract(player.position());
			if (away.lengthSqr() < 1.0e-4) {
				away = new Vec3(RNG.nextDouble() - 0.5, 0, RNG.nextDouble() - 0.5);
			}
			away = away.normalize().scale(8.0);
			Vec3 target = m.position().add(away.x, 0, away.z);
			if (m instanceof PathfinderMob) {
				m.getNavigation().moveTo(target.x, m.getY(), target.z, 1.0);
			}
			// -25% 绉婚€燂紙涔樻硶淇グ锛屽箓绛夎　锛?
			AttributeInstance attr = m.getAttribute(Attributes.MOVEMENT_SPEED);
			if (attr != null && attr.getModifier(MANCIANIMA_FEAR_SLOW_UUID) == null) {
				attr.addPermanentModifier(new AttributeModifier(MANCIANIMA_FEAR_SLOW_UUID,
						"Mancianima fear slow", -0.25,
						AttributeModifier.Operation.MULTIPLY_TOTAL));
			}
			FLEE_AFFECTED.put(m.getUUID(), now);
		}
	}

	/** 瓒呰繃 linger 鏈埛鏂?鈫?绉婚櫎淇グ銆?*/
	private static void cleanupExpiredFlee(long now) {
		Iterator<Map.Entry<UUID, Long>> it = FLEE_AFFECTED.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Long> e = it.next();
			if (now - e.getValue() <= FLEE_LINGER_TICKS) continue;
			it.remove();
		}
		// 娉ㄦ剰锛歛ttr modifier 鐨勫疄闄呯Щ闄ら渶瑕佸湪 entity 瀛樺湪鏃舵墠鑳藉仛锛岀敱 onMerchantTickRemove 澶勭悊涓嶅彲琛屻€?
		// 鏀逛负锛氫笅涓€杞?tickFlee 娌℃湁閲嶆柊鍔犲叆鏃讹紝渚濋潬涓嬮潰鐨?maybeRemoveFleeModifier 鍙嶅悜鎵弿銆?
	}

	/** 澶栭儴璋冪敤锛堟瘡椹遍€?tick 鍚庯級锛氬涓嶅啀琚拷韪殑瀹炰綋绉婚櫎 modifier銆傝繖閲岀敱 SscAddon 姣忕鎵竴娆″叏涓栫晫涓嶇幇瀹烇紝
	 * 	鏀逛负鍦?tickFlee 涓湪 +modifier 鍚屾椂璁板綍 last seen锛屾墜鍔ㄥ湪 LivingEntityTickEvent 涓鏌ャ€?
	 * 	涓洪伩鍏嶉澶?mixin锛岃繖閲屾彁渚涗竴涓?static helper锛岀敱姝ｅ湪 tick 鐨勫晢浜鸿嚜宸辫皟鐢ㄣ€傘€傘€?
	 * 	绠€鍖栵細杩欓噷涓嶄富鍔ㄥ嵏杞斤紱鏀圭敤 SscAddon 鏈嶅姟鍣?tick 涓皟鐢ㄤ笅闈㈢殑鏂规硶銆?*/
	public static void serverGlobalFleeCleanup(net.minecraft.server.MinecraftServer server) {
		long now = server.overworld().getGameTime();
		if (FLEE_AFFECTED.isEmpty()) return;
		for (ServerLevel world : server.getAllLevels()) {
			for (UUID id : new ArrayList<>(FLEE_AFFECTED.keySet())) {
				if (now - FLEE_AFFECTED.getOrDefault(id, 0L) <= FLEE_LINGER_TICKS) continue;
				net.minecraft.world.entity.Entity ent = world.getEntity(id);
				if (ent instanceof LivingEntity le) {
					AttributeInstance attr = le.getAttribute(Attributes.MOVEMENT_SPEED);
					if (attr != null && attr.getModifier(MANCIANIMA_FEAR_SLOW_UUID) != null) {
						attr.removeModifier(MANCIANIMA_FEAR_SLOW_UUID);
					}
					FLEE_AFFECTED.remove(id);
				}
			}
		}
	}

	private static int countNearbyRaiders(Player player, double radius) {
		AABB box = player.getBoundingBox().inflate(radius);
		return player.level().getEntitiesOfClass(Raider.class, box,
				e -> e.isAlive() && !e.isRemoved()).size();
	}

	private static class AssaultData {
		final Set<UUID> targets;
		final int initialCount;
		ServerBossEvent bossBar; // 璇绘潯闃舵 = prepareBar锛泂pawn 鍚庡垏涓?mainBar
		final net.minecraft.resources.ResourceLocation dimensionId;
		final Vec3 origin;
		int prepareTicks; // > 0 琛ㄧず璇绘潯涓紱鍒?0 鏃?spawn raider 骞跺垏鎹?bossbar
		boolean raidersSpawned = false;
		// 鈥斺€?绂诲紑/鏆傚仠鏈哄埗 鈥斺€?
		boolean paused = false; // true锛氱帺瀹剁寮€鏉戝簞锛宐ossbar 鐏拌壊鏄剧ず
		long expireTime;         // 瑙﹀彂鍚?24000 ticks锛堜竴鏃ワ級鍚庤嚜鍔ㄥけ璐?
		AssaultData(Set<UUID> targets, ServerBossEvent bossBar,
				net.minecraft.resources.ResourceLocation dimensionId, Vec3 origin, long startTime) {
			this.targets = targets;
			this.initialCount = targets.size();
			this.bossBar = bossBar;
			this.dimensionId = dimensionId;
			this.origin = origin;
			this.expireTime = startTime + 24000L;
		}
	}

	/** 姣?tick 鎺ㄨ繘璇绘潯锛涘埌 0 鍒?spawn 鍔帬鍐涘苟鍒囨崲 涓?涓?bossbar銆?*/
	private static void tickAssaultPrepare(ServerPlayer player) {
		AssaultData data = ASSAULTS.get(player.getUUID());
		if (data == null || data.raidersSpawned) return;
		data.prepareTicks--;
		if (data.prepareTicks > 0) {
			data.bossBar.setProgress(Math.max(0f, (float) data.prepareTicks / ASSAULT_PREPARE_TICKS));
			return;
		}
		// 璇绘潯瀹屾垚锛氱Щ闄?prepareBar锛屽垱寤?main bossbar锛宻pawn raider
		data.bossBar.removeAllPlayers();
		data.bossBar.setVisible(false);
		ServerBossEvent mainBar = new ServerBossEvent(
				Component.translatable("ssc_addon.mancianima.assault.bossbar", data.targets.size()),
				BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
		mainBar.setProgress(1.0f);
		mainBar.addPlayer(player);
		data.bossBar = mainBar;
		data.raidersSpawned = true;
		spawnRaiders(player);
		player.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.start", data.targets.size()), false);
		if (player.level() instanceof ServerLevel sw) {
			sw.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.RAID_HORN.value(), player.getSoundSource(), 1.5f, 0.85f);
		}
	}

	/** 浠ョ帺瀹堕潰鍚戜负鏂瑰悜锛屽湪闄勮繎鏉戝簞闆嗕細鐐瑰鍦堢敓鎴愬姭鎺犲啗锛涢殢鏈洪樀瀹癸紝甯︿竴闈笉绁ヤ箣鏃椼€?*/
	private static void spawnRaiders(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) return;
		BlockPos villageCenter = findVillageCenter(world, player);
		BlockPos spawnPos = findRaiderSpawnPos(world, player, villageCenter);
		RaiderGroup group = new RaiderGroup(world.dimension().location(), villageCenter);

		// 闅忔満闃靛锛? Pillager + 2 Vindicator 涓哄熀纭€锛?0% 灏?1 鍙?Vindicator 鏇挎崲涓?Evoker锛?0% +1 Witch锛?0% +1 Ravager
		int pillagers = RAIDER_PILLAGER_COUNT;
		int vindicators = RAIDER_VINDICATOR_COUNT;
		int evokers = 0;
		int witches = 0;
		int ravagers = 0;
		if (RNG.nextFloat() < 0.30f && vindicators > 0) { vindicators--; evokers++; }
		if (RNG.nextFloat() < 0.30f) witches++;
		if (RNG.nextFloat() < 0.10f) ravagers++;

		boolean leaderAssigned = false;
		for (int i = 0; i < pillagers; i++) {
			Pillager p = EntityType.PILLAGER.create(world);
			if (p == null) continue;
			BlockPos pp = spawnPos.offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
			p.moveTo(pp.getX() + 0.5, pp.getY(), pp.getZ() + 0.5, 0f, 0f);
			p.finalizeSpawn(world, world.getCurrentDifficultyAt(pp), MobSpawnType.EVENT, null, null);
			p.setPersistenceRequired();
			configureRaiderPatrol(p, villageCenter);
			if (!leaderAssigned) {
				// 棣栦綅 Pillager 甯︿笂涓嶇ゥ涔嬫棗浣滀负闃熼暱
				p.setPatrolLeader(true);
				p.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
				leaderAssigned = true;
			}
			world.addFreshEntity(p);
			group.raiderUuids.add(p.getUUID());
		}
		for (int i = 0; i < vindicators; i++) {
			Vindicator v = EntityType.VINDICATOR.create(world);
			if (v == null) continue;
			BlockPos vp = spawnPos.offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
			v.moveTo(vp.getX() + 0.5, vp.getY(), vp.getZ() + 0.5, 0f, 0f);
			v.finalizeSpawn(world, world.getCurrentDifficultyAt(vp), MobSpawnType.EVENT, null, null);
			v.setPersistenceRequired();
			configureRaiderPatrol(v, villageCenter);
			world.addFreshEntity(v);
			group.raiderUuids.add(v.getUUID());
		}
		for (int i = 0; i < evokers; i++) {
			Evoker e = EntityType.EVOKER.create(world);
			if (e == null) continue;
			BlockPos ep = spawnPos.offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
			e.moveTo(ep.getX() + 0.5, ep.getY(), ep.getZ() + 0.5, 0f, 0f);
			e.finalizeSpawn(world, world.getCurrentDifficultyAt(ep), MobSpawnType.EVENT, null, null);
			e.setPersistenceRequired();
			configureRaiderHome(e, villageCenter, 32);
			world.addFreshEntity(e);
			group.raiderUuids.add(e.getUUID());
		}
		for (int i = 0; i < witches; i++) {
			Witch w = EntityType.WITCH.create(world);
			if (w == null) continue;
			BlockPos wp = spawnPos.offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
			w.moveTo(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5, 0f, 0f);
			w.finalizeSpawn(world, world.getCurrentDifficultyAt(wp), MobSpawnType.EVENT, null, null);
			w.setPersistenceRequired();
			configureRaiderHome(w, villageCenter, 32);
			world.addFreshEntity(w);
			group.raiderUuids.add(w.getUUID());
		}
		for (int i = 0; i < ravagers; i++) {
			Ravager r = EntityType.RAVAGER.create(world);
			if (r == null) continue;
			BlockPos rp = spawnPos.offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
			r.moveTo(rp.getX() + 0.5, rp.getY(), rp.getZ() + 0.5, 0f, 0f);
			r.finalizeSpawn(world, world.getCurrentDifficultyAt(rp), MobSpawnType.EVENT, null, null);
			r.setPersistenceRequired();
			configureRaiderHome(r, villageCenter, 32);
			world.addFreshEntity(r);
			group.raiderUuids.add(r.getUUID());
		}

		RAIDER_GROUPS.put(player.getUUID(), group);

		// 鎻愮ず闄勮繎閾佸個鍎¤拷鍑荤帺瀹?
		AABB markBox = player.getBoundingBox().inflate(64.0);
		for (IronGolem g : world.getEntitiesOfClass(IronGolem.class, markBox,
				e -> e.isAlive() && !e.isRemoved())) {
			g.setTarget(player);
			g.setLastHurtByMob(player);
		}
	}

	/** 璁?raider 鍦ㄦ暣涓潙搴勮寖鍥村唴娲诲姩锛汸atrolEntity 璁?patrolTarget 骞舵縺娲诲贰閫?AI锛?
	 *  璁╁姭鎺犺€呯湡姝ｆ湞鏉戝簞涓績绉诲姩鑰岄潪鍙湪鍑虹敓鐐瑰緲寰娿€?*/
	private static void configureRaiderPatrol(net.minecraft.world.entity.monster.PatrollingMonster r, BlockPos center) {
		// 鎶婃椿鍔ㄨ寖鍥存斁澶у埌 64 鏍硷紝瑕嗙洊鏁翠釜鏉戝簞锛汸atrolEntity 鍐呴儴 home 璺濈鍒ゅ畾涔熸嵁姝?
		r.restrictTo(center, 64);
		r.setPatrolTarget(center);
		// 婵€娲诲贰閫?AI锛堥粯璁?patrolling=false锛孭atrolGoal 涓嶄細瑙﹀彂涓诲姩绉诲姩锛?
		((github.com.gengyoubo.sscfa.mixin.entity.PatrolEntityAccessor) r)
				.invokeSetPatrolling(true);
	}

	/** 闈?PatrolEntity 鐨?raider锛圵itch / Evoker / Ravager锛夊彧闇€璁?home銆?*/
	private static void configureRaiderHome(Mob m, BlockPos center, int range) {
		m.restrictTo(center, range);
	}

	private static BlockPos findVillageCenter(ServerLevel world, ServerPlayer player) {
		return world.getPoiManager()
				.findClosest(
						entry -> entry.is(PoiTypes.MEETING),
						player.blockPosition(), 64,
						PoiManager.Occupancy.ANY)
				.orElse(player.blockPosition());
	}

	/** 浠モ€滅帺瀹堕潰鍚戔€濅负鏂瑰悜鍦ㄦ潙搴勪腑蹇冨墠鏂?32 鏍间綅缃綔涓哄叆鍦虹偣锛涜繖鏍峰姭鎺犲啗浼氫粠鐜╁绉诲姩鏂瑰悜杩涙敾銆?*/
	private static BlockPos findRaiderSpawnPos(ServerLevel world, ServerPlayer player, BlockPos villageCenter) {
		Vec3 facing = player.getViewVector(1.0f);
		double flatLen = Math.hypot(facing.x, facing.z);
		Vec3 dir;
		if (flatLen > 1e-3) {
			dir = new Vec3(facing.x / flatLen, 0, facing.z / flatLen);
		} else {
			double ang = RNG.nextDouble() * Math.PI * 2;
			dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
		}
		dir = dir.scale(32);
		BlockPos approxTarget = villageCenter.offset((int) dir.x, 0, (int) dir.z);
		return findGroundAt(world, approxTarget);
	}

	private static BlockPos findGroundAt(ServerLevel world, BlockPos pos) {
		int x = pos.getX(), z = pos.getZ();
		int startY = Math.min(world.getMaxBuildHeight() - 2, pos.getY() + 16);
		for (int y = startY; y > world.getMinBuildHeight() + 1; y--) {
			BlockPos p = new BlockPos(x, y, z);
			BlockPos below = p.below();
			net.minecraft.world.level.block.state.BlockState belowState = world.getBlockState(below);
			net.minecraft.world.level.block.state.BlockState atState = world.getBlockState(p);
			net.minecraft.world.level.block.state.BlockState upState = world.getBlockState(p.above());
			// 绔欑珛浣嶄笌涓婃柟蹇呴』涓虹┖姘旓紙閬垮厤鏂瑰潡鍐呴儴锛夛紝涓旈兘涓嶈兘鏄祦浣?
			if (!atState.isAir() || !upState.isAir()) continue;
			if (!atState.getFluidState().isEmpty() || !upState.getFluidState().isEmpty()) continue;
			// 鑴氫笅蹇呴』鏄浐浣撲笖涓嶆槸娴佷綋锛堟帓闄ゆ按闈?宀╂祮闈級
			if (!belowState.isRedstoneConductor(world, below)) continue;
			if (!belowState.getFluidState().isEmpty()) continue;
			return p;
		}
		// 鍏滃簳锛氬師濮嬩綅缃笂鏂?+ 1 鑷冲皯涓嶅湪鍦颁笅
		return pos.above();
	}

	/**
	 * 鐜╁璧板嚭琚嚮璧风偣杩囪繙鎴栬法缁村害锛氭殏鍋滆鍑汇€?
	 * - 鏆傚仠鏃?bossbar 鍒囩伆鑹?(WHITE) 鏄剧ず"绂诲紑鍖哄煙"
	 * - 鐜╁鍥炴潵 鈫?鑷姩鎭㈠ (鎭㈠ RED 棰滆壊, 鏄剧ず鍘?bossbar 鏂囨湰)
	 * - 褰撳ぉ鍐?(24000 ticks) 鏈洖鏉?鈫?鏄剧ず"琚嚮澶辫触" 5 绉掑悗娓呯悊锛屽姭鎺犲啗 LINGER
	 * 娉ㄦ剰锛氭殏鍋滄湡闂村姭鎺犲啗浠嶅湪鏀诲嚮鏉戞皯锛岃繘搴︿粛鍦ㄥ墠杩涳紝鐜╁鍥炴潵鏃朵細鐪嬪埌褰撳墠瀹為檯鍓╀綑鏉戞皯鏁般€?
	 */
	private static void checkAssaultRange(ServerPlayer player) {
		AssaultData data = ASSAULTS.get(player.getUUID());
		if (data == null) return;
		long now = player.level().getGameTime();
		boolean differentDim = !player.level().dimension().location().equals(data.dimensionId);
		boolean tooFar = player.position().distanceToSqr(data.origin) > ASSAULT_ABORT_DISTANCE_SQ;
		boolean outOfRange = differentDim || tooFar;

		if (outOfRange) {
			// 妫€鏌ヨ秴鏃讹紙涓€鏃ュ唴鏈洖 鈫?琚嚮澶辫触锛?
			if (now > data.expireTime) {
				data.bossBar.setColor(BossEvent.BossBarColor.WHITE);
				data.bossBar.setName(Component.translatable("ssc_addon.mancianima.assault.failed"));
				data.bossBar.setProgress(0f);
				data.bossBar.removeAllPlayers();
				data.bossBar.setVisible(false);
				ASSAULTS.remove(player.getUUID());
				player.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.failed_msg"), false);
				// 鍔帬鍐涚户缁?LINGER 鈫?MARCH 鎾ょ
				startLingerForGroup(player);
				return;
			}
			// 杩涘叆鏆傚仠鐘舵€?
			if (!data.paused) {
				data.paused = true;
				data.bossBar.setColor(BossEvent.BossBarColor.WHITE);
				data.bossBar.setName(Component.translatable("ssc_addon.mancianima.assault.paused"));
			}
		} else {
			// 鐜╁鍥炲埌鑼冨洿 鈫?鎭㈠
			if (data.paused) {
				data.paused = false;
				if (data.raidersSpawned) {
					data.bossBar.setColor(BossEvent.BossBarColor.RED);
					int remaining = data.targets.size();
					data.bossBar.setName(Component.translatable("ssc_addon.mancianima.assault.bossbar", remaining));
					data.bossBar.setProgress(data.initialCount > 0 ? (float) remaining / data.initialCount : 0f);
				} else {
					data.bossBar.setColor(BossEvent.BossBarColor.YELLOW);
					data.bossBar.setName(Component.translatable("ssc_addon.mancianima.assault.preparing"));
				}
				player.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.resumed"), false);
			}
		}
	}

	/** mob 姝讳骸鏃惰皟鐢細鑻ュ睘浜庢煇鐜╁鐨勮鍑荤洰鏍?鈫?绉婚櫎骞舵鏌ユ槸鍚﹀畬鎴愩€?*/
	public static void onAssaultTargetDeath(LivingEntity dead) {
		if (dead.level().isClientSide()) return;
		UUID deadId = dead.getUUID();
		Iterator<Map.Entry<UUID, AssaultData>> it = ASSAULTS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, AssaultData> e = it.next();
			AssaultData data = e.getValue();
			if (!data.targets.remove(deadId)) continue;
			// 璇绘潯闃舵涓嶉€掑噺 bossbar锛堜絾浠嶄粠 targets 涓Щ闄わ紝閬垮厤閲嶅璁℃暟锛?
			if (!data.raidersSpawned) continue;
			// 鏇存柊 bossbar
			int remaining = data.targets.size();
			data.bossBar.setName(Component.translatable("ssc_addon.mancianima.assault.bossbar", remaining));
			data.bossBar.setProgress(data.initialCount > 0 ? (float) remaining / data.initialCount : 0f);
			if (remaining == 0) {
				ServerPlayer p = dead.level().getServer() == null ? null
						: dead.level().getServer().getPlayerList().getPlayer(e.getKey());
				// 璺ㄧ淮搴﹂槻寰★細鑻ョ帺瀹朵笉鍦?dead 鎵€鍦ㄧ殑涓栫晫锛堟瘮濡傚凡浼犻€佽蛋锛夛紝涓嶅湪璇ヤ笘鐣岀粰濂栧姳/鎾煶锛?
				// 浣嗕粛瑕佹竻闄よ鍑荤姸鎬侊紝閬垮厤鏁版嵁娈嬬暀
				if (p != null && p.level() == dead.level()) {
					// 琚嚮鎴愬姛瀹屾垚锛氬湪姝よ褰曞綋澶╁喎鍗达紙鎸佷箙鍖栵級
					net.minecraft.server.MinecraftServer srv = p.getServer();
					if (srv != null) {
						MancianimaAssaultState state = MancianimaAssaultState.get(srv);
						state.lastRoll.put(p.getUUID(), p.level().getGameTime());
						state.setDirty();
					}
					// 濂栧姳浠ュ疄浣撴帀钀界墿褰㈠紡鍑虹幇鍦ㄧ帺瀹?2 鏍煎崐寰勫唴锛堜笉鐩存帴杩涜儗鍖咃級
					dropAssaultRewards(p);
					p.displayClientMessage(Component.translatable("ssc_addon.mancianima.assault.complete"), false);
					if (p.level() instanceof ServerLevel sw) {
						sw.playSound(null, p.getX(), p.getY(), p.getZ(),
								SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, p.getSoundSource(), 1.0f, 1.0f);
					}
				}
				data.bossBar.removeAllPlayers();
				data.bossBar.setVisible(false);
				it.remove();
				// 琚嚮鎴愬姛鍚庡姭鎺犲啗杩涘叆 LINGER銆俴ey 涓虹帺瀹?UUID銆?
				if (e.getKey() != null) startLingerForGroupByPlayerId(e.getKey(), dead.level() instanceof ServerLevel sw0 ? sw0 : null);
			}
		}
	}

	/** 鍦ㄧ帺瀹跺懆鍥?2 鏍煎崐寰勫唴浠ュ疄浣撻」鎺夎惤濂栧姳銆?*/
	private static void dropAssaultRewards(ServerPlayer p) {
		dropAroundPlayer(p, new ItemStack(Items.EMERALD_BLOCK, 2));
		dropAroundPlayer(p, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
	}

	private static void dropAroundPlayer(ServerPlayer p, ItemStack stack) {
		double ang = RNG.nextDouble() * Math.PI * 2;
		double r = RNG.nextDouble() * 2.0;
		double x = p.getX() + Math.cos(ang) * r;
		double z = p.getZ() + Math.sin(ang) * r;
		double y = p.getY() + 0.5;
		net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(p.level(), x, y, z, stack);
		ie.setDeltaMovement((RNG.nextDouble() - 0.5) * 0.1, 0.2, (RNG.nextDouble() - 0.5) * 0.1);
		ie.setPickUpDelay(10);
		p.level().addFreshEntity(ie);
	}

	/** 灏嗙帺瀹跺悕涓嬬殑 RaiderGroup 杞负 LINGER 闃舵銆?*/
	private static void startLingerForGroup(ServerPlayer player) {
		startLingerForGroupByPlayerId(player.getUUID(),
				player.level() instanceof ServerLevel sw ? sw : null);
	}

	private static void startLingerForGroupByPlayerId(UUID playerId, ServerLevel worldHint) {
		RaiderGroup group = RAIDER_GROUPS.get(playerId);
		if (group == null || group.phase != RaiderPhase.ACTIVE) return;
		group.phase = RaiderPhase.LINGER;
		long now = worldHint != null ? worldHint.getGameTime() : 0L;
		group.phaseEndTime = now + RAIDER_LINGER_MIN_TICKS
				+ RNG.nextInt(RAIDER_LINGER_MAX_TICKS - RAIDER_LINGER_MIN_TICKS);
	}

	/** 鏈嶅姟鍣ㄤ富绾跨▼瀹氭湡椹卞姩锛堝缓璁瘡绉掍竴娆★級銆傚鐞?RaiderGroup 鐨?LINGER 鈫?MARCH 鈫?鑴遍€傛竻鐞嗐€?*/
	public static void tickRaiderGroups(net.minecraft.server.MinecraftServer server) {
		// 鍏滃簳锛氭鏌ユ墍鏈夎繘琛屼腑琚嚮鏄惁宸茶秴鏃讹紙鐜╁涓嬬嚎鎯呭喌涓?checkAssaultRange 涓嶄細琚皟鐢級
		if (!ASSAULTS.isEmpty()) {
			ServerLevel ow = server.overworld();
			long now = ow != null ? ow.getGameTime() : 0L;
			Iterator<Map.Entry<UUID, AssaultData>> ait = ASSAULTS.entrySet().iterator();
			while (ait.hasNext()) {
				Map.Entry<UUID, AssaultData> e = ait.next();
				AssaultData d = e.getValue();
				if (now > d.expireTime) {
					d.bossBar.removeAllPlayers();
					d.bossBar.setVisible(false);
					ait.remove();
					startLingerForGroupByPlayerId(e.getKey(), ow);
				}
			}
		}
		if (RAIDER_GROUPS.isEmpty()) return;
		Iterator<Map.Entry<UUID, RaiderGroup>> it = RAIDER_GROUPS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, RaiderGroup> entry = it.next();
			RaiderGroup group = entry.getValue();
			ServerLevel world = server.getLevel(net.minecraft.resources.ResourceKey.create(
					net.minecraft.core.registries.Registries.DIMENSION, group.dimensionId));
			if (world == null) { it.remove(); continue; }
			// 鏀堕泦浠嶅瓨娲荤殑 raider 瀹炰綋
			List<Mob> alive = new ArrayList<>();
			Iterator<UUID> uit = group.raiderUuids.iterator();
			while (uit.hasNext()) {
				UUID uid = uit.next();
				net.minecraft.world.entity.Entity ent = world.getEntity(uid);
				if (ent instanceof Mob m && m.isAlive() && !m.isRemoved()) {
					alive.add(m);
				} else if (ent == null || ent.isRemoved()) {
					// 瀹炰綋鍙兘鍦ㄦ湭鍔犺浇鍖哄潡锛涗繚鐣?UUID锛岀瓑鍖哄潡鍐嶆鍔犺浇鏃跺啀鍒?
					if (ent == null) continue;
					uit.remove();
				}
			}
			if (group.raiderUuids.isEmpty()) { it.remove(); continue; }
			long now = world.getGameTime();
			switch (group.phase) {
				case ACTIVE -> { /* 鐢?onAssaultTargetDeath / abort 涓诲姩杞?LINGER */ }
				case LINGER -> {
					if (now >= group.phaseEndTime) {
						// 杩涘叆 MARCH锛氶€変竴涓繙绂绘潙搴勬柟鍚?200 鏍肩殑 patrol 鐩爣
						double ang = RNG.nextDouble() * Math.PI * 2;
						BlockPos target = group.villageCenter.offset(
								(int) (Math.cos(ang) * RAIDER_MARCH_DISTANCE), 0,
								(int) (Math.sin(ang) * RAIDER_MARCH_DISTANCE));
						group.phase = RaiderPhase.MARCH;
						for (Mob m : alive) {
							// 瑙ｉ櫎 home锛岃浠栦滑鑳界寮€鏉戝簞
							m.restrictTo(BlockPos.ZERO, -1);
							if (m instanceof net.minecraft.world.entity.monster.PatrollingMonster pe) {
								pe.setPatrolTarget(target);
							}
						}
					}
				}
				case MARCH -> {
					// 妫€鏌ユ暣缁?raider 鏄惁閮戒笉鍦ㄤ换浣曠帺瀹?128 鏍间互鍐?鈫?鍏ㄥ憳娓呯悊
					boolean anyNearPlayer = false;
					for (Mob m : alive) {
						for (ServerPlayer sp : world.players()) {
							if (sp.distanceToSqr(m) <= RAIDER_MARCH_DESPAWN_DISTANCE_SQ) {
								anyNearPlayer = true;
								break;
							}
						}
						if (anyNearPlayer) break;
					}
					if (!anyNearPlayer) {
						for (Mob m : alive) m.discard();
						it.remove();
					}
				}
			}
		}
	}

	/** 鏉戞皯/鍟嗕汉姝讳骸鏃讹細濂戠伒鍑绘潃 鈫?鎺夎惤閮ㄥ垎鍙敭鍗栫墿鍝?+ 姒傜巼缁垮疂鐭炽€?*/
	public static void onMerchantKilledByMancianima(AbstractVillager merchant, ServerPlayer killer) {
		MerchantOffers offers = merchant.getOffers();
		boolean droppedSomething = false;
		if (offers != null && !offers.isEmpty()) {
			int picks = Math.min(2, offers.size());
			List<MerchantOffer> shuffled = new ArrayList<>(offers);
			Collections.shuffle(shuffled, RNG);
			for (int i = 0; i < picks; i++) {
				ItemStack sell = shuffled.get(i).getResult();
				if (sell == null || sell.isEmpty()) continue;
				ItemStack drop = sell.copy();
				drop.setCount(Math.max(1, sell.getCount()));
				merchant.spawnAtLocation(drop);
				droppedSomething = true;
			}
		}
		// 鏃犱氦鏄撶殑鏉戞皯锛堟棤鑱屼笟/骞煎勾锛夊崰澶氭暟锛歠allback 鎺夌偣 base 鏉愭枡
		if (!droppedSomething) {
			merchant.spawnAtLocation(new ItemStack(Items.WHEAT, 1 + RNG.nextInt(3)));
			merchant.spawnAtLocation(new ItemStack(Items.BREAD, 1 + RNG.nextInt(2)));
		}
		// 30% 棰濆缁垮疂鐭筹紙1-3锛?
		if (RNG.nextDouble() < 0.30) {
			merchant.spawnAtLocation(new ItemStack(Items.EMERALD, 1 + RNG.nextInt(3)));
		}
	}

	public static void clearAll() {
		for (AssaultData data : ASSAULTS.values()) {
			if (data.bossBar != null) {
				data.bossBar.removeAllPlayers();
				data.bossBar.setVisible(false);
			}
		}
		ASSAULTS.clear();
		// LAST_ASSAULT_ROLL 宸茶縼绉诲埌 MancianimaAssaultState锛堟寔涔呭寲锛夛紝姝ゅ鏃犻渶娓呯┖
		FLEE_AFFECTED.clear();
		// raider 缁勫湪 server 閲嶅惎鏃朵涪寮冭窡韪紙瀹炰綋鏈韩宸插甫 Persistent 鎸佷箙鍖栵級锛岄伩鍏嶆偓绌虹姸鎬?
		RAIDER_GROUPS.clear();
	}

	/**
	 * 鍗曚釜鐜╁閫€鏈嶆椂娓呯悊涓庡叾缁戝畾鐨勫唴瀛樼姸鎬侊紝閬垮厤闀挎湡绱Н銆?
	 * - 鎾ら攢 raid bossBar 鐨勭帺瀹惰闃呭苟娓呯┖ ASSAULTS 涓鐜╁鐨勬潯鐩紱
	 * - 绔嬪嵆涓㈠純 RAIDER_GROUPS 涓鐜╁鐨?group 璺熻釜锛坮aider 瀹炰綋鏈韩宸叉寔涔呭寲锛屼笘鐣屼細缁х画淇濈暀锛夈€?
	 * 娉ㄦ剰锛氭湰鏂规硶浠呮竻鍐呭瓨绱㈠紩锛屼笉浼氱Щ闄や笘鐣屼腑宸茬敓鎴愮殑 raider 瀹炰綋銆?
	 */
	public static void onPlayerDisconnect(UUID uuid) {
		AssaultData data = ASSAULTS.remove(uuid);
		if (data != null && data.bossBar != null) {
			data.bossBar.removeAllPlayers();
			data.bossBar.setVisible(false);
		}
		RAIDER_GROUPS.remove(uuid);
	}
}
