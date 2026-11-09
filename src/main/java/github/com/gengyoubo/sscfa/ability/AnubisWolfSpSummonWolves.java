package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.compat.MinionTracker;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import net.onixary.shapeShifterCurseForge.power.AnubisMinionService;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.AnubisWolfSpPerks;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP阿努比斯之狼次要技??- 冥狼裁庭
 * 召唤AnubisWolfMinionEntity（与受??攻击时召唤的胡狼相同实体）协助战斗???
 * 嚎叫蓄???.5秒后，交错出???只冥狼（Death Domain激活时???只，且增强属性）????
 * 冥狼持续30秒后消散??
 * 冥狼自带凋零攻击和治愈主人逻辑（level 3：攻击???，生????0，治????HP/hit）??
 * 死亡领域联动时进一步增强属性并给予速度???
 * CD30秒，若已达最大召唤数??只）则惩罚????秒??
 */
public class AnubisWolfSpSummonWolves {

	/**
	 * 嚎叫蓄力时间（tick??
	 */
	private static final int HOWL_TICKS = 30; // 1.5缂?

	// ==================== 常??====================
	// 以下常量均为默认值；运行时??balance 快照读取（scope：abilities.anubis_summon_wolves??
	/**
	 * 嚎叫减速比例（保???0%速度???
	 */
	private static final double HOWL_SLOW_FACTOR = 0.5;
	/**
	 * 召唤间隔（tick），交错出??
	 */
	private static final int SUMMON_INTERVAL = 5;
	/**
	 * 冥狼存活时间（tick??
	 */
	private static final int WOLF_DURATION = 600; // 30缂?
	// CD（正????惩罚）已由统一冷却服??SkillCooldowns + power JSON 配置管理，原常量已??
	/**
	 * 最大同时存在冥狼???
	 */
	private static final int MAX_WOLVES = 6;
	/**
	 * 基础召唤数量
	 */
	private static final int BASE_SUMMON_COUNT = 2;
	/**
	 * 领域增强召唤数????
	 */
	private static final int DOMAIN_SUMMON_COUNT = 4;
	/**
	 * 冥狼等级（与SSC原版level 3一致??
	 */
	private static final int MINION_LEVEL = 3;
	/**
	 * 领域增强额外生命??
	 */
	private static final double DOMAIN_BONUS_HEALTH = 10.0;
	/**
	 * 领域增强额外攻击??
	 */
	private static final double DOMAIN_BONUS_ATTACK = 2.0;
	/**
	 * 嚎叫减速修饰符UUID
	 */
	private static final UUID HOWL_SLOW_UUID = UUID.fromString("c9d5e6f7-a8b9-4c0d-1e2f-3a4b5c6d7e8f");
	/**
	 * 领域增强攻击力修饰符UUID
	 */
	private static final UUID DOMAIN_ATTACK_UUID = UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a");
	/**
	 * 领域增强生命值修饰符UUID
	 */
	private static final UUID DOMAIN_HEALTH_UUID = UUID.fromString("e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b");
	/**
	 * 饰品攻击力削减修饰符UUID
	 */
	private static final UUID TRINKET_DAMAGE_UUID = UUID.fromString("f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c");
	/**
	 * 饰品攻击力削减比例??25%??
	 */
	private static final double TRINKET_DAMAGE_REDUCTION = -0.25;
	/**
	 * 婵°倖顨嗗姗€骞栬閹即鍩€椤掑嫭鐓傞煫鍥ㄦ尭椤も偓闂佸憡鍨电换瀣箞閵婏富娈伴柟娈垮櫘閸庡イUID
	 */
	private static final UUID TRINKET_HEALTH_UUID = UUID.fromString("a3b4c5d6-e7f8-4a9b-0c1d-2e3f4a5b6c7d");
	/**
	 * 饰品血量削减比例???35%??
	 */
	private static final double TRINKET_HEALTH_REDUCTION = -0.35;
	// ==================== 闁绘鍩栭埀顑挎祰閹风兘?====================
	private static final ConcurrentHashMap<UUID, SummonData> ACTIVE_SUMMONS = new ConcurrentHashMap<>();
	/** 统一冷却服务的稳定技?ID?? power 注册路径）??*/
	public static final String SKILL_ID = "my_addon:form_anubis_wolf_sp_summon_wolves";

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.anubis_summon_wolves");

	private AnubisWolfSpSummonWolves() {
	}

	/**
	 * 玩家按下次要技能键触??
	 */
	public static boolean execute(ServerPlayer player) {
		// 重复释放检查????门禁已????power ??begin 统一拦截，??P1-1??
		if (ACTIVE_SUMMONS.containsKey(player.getUUID())) {
			return false;
		}

		// 检查饰品加??
		boolean hasCrystal = hasTrinketEquipped(player);
		int maxWolves = maximumWolves(player);

		// 通过IPlayerEntityMinion系统检查当前冥狼数??
		int aliveCount = getMinionCount(player);
		if (aliveCount >= maxWolves) {
			// 已达上限：??fail_cooldown 结??+ 失败音???
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.penalizeAttempt(player, SKILL_ID);
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8f, 0.5f);
			return false;
		}

		// 检查是否在死亡领域???
		boolean domainActive = AnubisWolfSpDeathDomain.hasActiveDomain(player.getUUID());

		// 计算可召唤数量（饰品增???只常规召唤???
		int baseSummon = hasCrystal ? BAL.i("base_summon_count", BASE_SUMMON_COUNT) + 1 : BAL.i("base_summon_count", BASE_SUMMON_COUNT);
		int domainSummon = hasCrystal ? BAL.i("domain_summon_count", DOMAIN_SUMMON_COUNT) + 1 : BAL.i("domain_summon_count", DOMAIN_SUMMON_COUNT);
		int targetCount = domainActive ? domainSummon : baseSummon;
		int canSummon = Math.min(targetCount, maxWolves - aliveCount);

		// 创建召唤数据并进入嚎叫阶????
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
		SummonData data = new SummonData(canSummon, domainActive);
        data.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		ACTIVE_SUMMONS.put(player.getUUID(), data);

		// 播放狼嚎叫??
		ServerLevel world = player.serverLevel();
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5f, 0.6f);

		// 应用减???
		applyHowlSlow(player);

		return true;
	}

	// ==================== 公开接???====================

	/**
	 * 每tick更??
	 */
	public static void tick(ServerPlayer player) {
		// 周期门控前置（先取余再查形态）：非阿努比斯狼玩家??tick 只花一次取余???
		// 避??%20 判断之前先??CCA 形态查???
		if (player.serverLevel().getGameTime() % 20 == 0 && FormUtils.isAnubisWolfSP(player)) {
			tickTrinketModifiers(player);
		}

		UUID uuid = player.getUUID();
		SummonData data = ACTIVE_SUMMONS.get(uuid);
		if (data == null) {
			if (player.tickCount % 20 == 0) removeHowlSlow(player);
			return;
		}
		if (!player.isAlive() || !FormUtils.isAnubisWolfSP(player)) {
			ACTIVE_SUMMONS.remove(uuid);
			removeHowlSlow(player);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
			return;
		}
		if (data.phase != Phase.ACTIVE && !data.enhancedDomain
				&& (github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != data.castId)) {
			interruptControl(player);
			return;
		}

		data.ticksElapsed++;

		switch (data.phase) {
			case HOWLING -> tickHowling(player, data);
			case SUMMONING -> tickSummoning(player, data);
			case ACTIVE -> tickActive(player, data);
		}
	}

	/**
	 * 玩家断???变形时清??
	 */
	public static void clearPlayer(UUID uuid) {
		ACTIVE_SUMMONS.remove(uuid);
	}

	/**
	 * 清理所有召唤状态（服务器重启时调用??
	 */
	public static void clearAll() {
		ACTIVE_SUMMONS.clear();
	}

	/** Stop the pending action while keeping wolves which have already become autonomous. */
	public static void interruptControl(ServerPlayer player) {
		SummonData data = ACTIVE_SUMMONS.get(player.getUUID());
		removeHowlSlow(player);
		if (data == null || data.phase == Phase.ACTIVE || data.enhancedDomain) return;
		if (data.summonedWolfUuids.isEmpty()) ACTIVE_SUMMONS.remove(player.getUUID());
		data.wolvesToSummon = data.wolvesSummoned;
		data.phase = Phase.ACTIVE;
		data.ticksElapsed = 0;
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
	}

	public static void clearAll(net.minecraft.server.MinecraftServer server) {
		if (server.getPlayerList() != null)
			for (ServerPlayer player : server.getPlayerList().getPlayers()) removeHowlSlow(player);
		clearAll();
	}

	/** Stop pending summons before killing owned wolves, so recall cannot spawn replacements. */
	public static void recallAll(ServerPlayer player) {
		SummonData data = ACTIVE_SUMMONS.remove(player.getUUID());
		removeHowlSlow(player);
		if (data != null && data.phase != Phase.ACTIVE && data.castId >= 0) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
		}
		player.removeEffect(MobEffects.WITHER);
		var minions = MinionTracker.get(player, MinionTracker.ANUBIS_WOLF);
		if (minions == null) return;
		for (UUID uuid : List.copyOf(minions)) {
			for (ServerLevel world : player.getServer().getAllLevels()) {
				if (world.getEntity(uuid) instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()
						&& player.getUUID().equals(wolf.getOwnerUUID())) {
					// Keep SSC's onDeath/death-heal path; discard would lose the recall heal.
					wolf.kill();
				}
			}
		}
		MinionTracker.clear(player, MinionTracker.ANUBIS_WOLF);
	}

	public static int maximumWolves(ServerPlayer player) {
		return AnubisWolfSpPerks.capacity(player,
				BAL.i("max_wolves", MAX_WOLVES) + (hasTrinketEquipped(player) ? 2 : 0));
	}

	/**
	 * 增强死亡领域自动召唤冥狼（跳过嚎叫阶段，直接生成??
	 * 实际召唤数????= min(requestCount, MAX_WOLVES - 已有数量)
	 */
	public static void autoSummonForEnhancedDomain(ServerPlayer player, int requestCount) {
		removeHowlSlow(player);
		// 增强领域：先消散现有冥狼和召唤流程，确保能召?????
		SummonData oldData = ACTIVE_SUMMONS.remove(player.getUUID());
		if (oldData != null) {
			dissipateWolves(player, oldData, player.serverLevel());
		}
		// 消散IPlayerEntityMinion系统中残留的冥???
		dissipateAllMinions(player);

		// 创建一个领域增强的SummonData，直接进入SUMMONING阶??
		SummonData data = new SummonData(Math.min(requestCount, maximumWolves(player)), true);
		data.enhancedDomain = true;
		data.phase = Phase.SUMMONING;
		data.ticksElapsed = 0;

		ACTIVE_SUMMONS.put(player.getUUID(), data);
	}

	private static void tickHowling(ServerPlayer player, SummonData data) {
		ServerLevel world = player.serverLevel();

		// 嚎叫期间粒子效????- 灵魂粒子围??
		if (data.ticksElapsed % 3 == 0) {
			double angle = (data.ticksElapsed * 12.0) * Math.PI / 180.0;
			double radius = 1.5;
			double px = player.getX() + Math.cos(angle) * radius;
			double pz = player.getZ() + Math.sin(angle) * radius;
			ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.SOUL,
					px, player.getY() + 0.5, pz, 2, 0.1, 0.3, 0.1, 0.02);
		}

		if (data.ticksElapsed >= BAL.i("howl_ticks", HOWL_TICKS)) {
			// 蓄力完成，移除减????
			removeHowlSlow(player);

			// 进入召唤阶段
			data.phase = Phase.SUMMONING;
			data.ticksElapsed = 0;

			// 播放召唤音???
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.5f);
		}
	}

	private static void tickSummoning(ServerPlayer player, SummonData data) {
		// 濠殿噯绲界划绫橫MON_INTERVAL tick闂佸憡鐟崜娆撳极濠婂嫮鈻旈柍褜鍓熷畷?
		if (data.ticksElapsed % BAL.i("summon_interval", SUMMON_INTERVAL) == 0 && data.wolvesSummoned < data.wolvesToSummon) {
			spawnMinionWolf(player, data);
			data.wolvesSummoned++;
		}

		// 全部召唤完毕则转入ACTIVE阶??
		if (data.wolvesSummoned >= data.wolvesToSummon) {
			data.phase = Phase.ACTIVE;
			data.ticksElapsed = 0;

			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, data.castId);
		}
	}

	// ==================== 阶段处???====================

	private static void tickActive(ServerPlayer player, SummonData data) {
		ServerLevel world = player.serverLevel();
		int wolfDuration = BAL.i("wolf_duration", WOLF_DURATION);

		// ??0tick检查一次冥狼的灵魂沙免???
		if (data.ticksElapsed % 20 == 0) {
			refreshWolfBuffs(world, data);
		}

		// 冥狼持续时间到???
		if (data.ticksElapsed >= wolfDuration) {
			// 消散所有本批次的???
			dissipateWolves(player, data, world);
			ACTIVE_SUMMONS.remove(player.getUUID());
		}
	}

	private static void spawnMinionWolf(ServerPlayer player, SummonData data) {
		ServerLevel world = player.serverLevel();
		// Passive summons and accessory changes can fill the slots during the howl.
		if (getMinionCount(player) >= maximumWolves(player)) return;

		BlockPos spawnPos = summonPosition(world, player, player);
		AnubisWolfMinionEntity wolf = spawnSpellWolf(player, spawnPos, data.domainActive, data.enhancedDomain);
		if (wolf != null) data.summonedWolfUuids.add(wolf.getUUID());
	}

	/** Shared construction keeps native minion registration, level, domain and accessories identical. */
	public static AnubisWolfMinionEntity spawnSpellWolf(ServerPlayer player, BlockPos spawnPos, boolean domainActive, boolean enhancedDomain) {
		ServerLevel world = player.serverLevel();
		if (getMinionCount(player) >= maximumWolves(player)) return null;

		// 使用MinionRegister.SpawnMinion生成（自动调用InitMinion注册到IPlayerEntityMinion系统????
		AnubisWolfMinionEntity wolf = MinionTracker.spawn(world, spawnPos, player);

		if (wolf == null) return null;

		int wolfDuration = BAL.i("wolf_duration", WOLF_DURATION);

		// 设置等级（level 3: HP=20, Attack=4, 治愈主???HP/hit??
		wolf.setMinionLevel(BAL.i("minion_level", MINION_LEVEL));
		AnubisMinionService.summon(player, wolf, new com.google.gson.JsonObject());

		// 死亡领域联动增????
		if (domainActive) {
			// 额外生命??
			AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
			if (healthAttr != null) {
				healthAttr.addPermanentModifier(new AttributeModifier(
						DOMAIN_HEALTH_UUID, "domain_bonus_health",
						BAL.d("domain_bonus_health", DOMAIN_BONUS_HEALTH), AttributeModifier.Operation.ADDITION));
				wolf.setHealth((float) healthAttr.getValue());
			}
			// 额外攻击??
			AttributeInstance attackAttr = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attackAttr != null) {
				attackAttr.addPermanentModifier(new AttributeModifier(
						DOMAIN_ATTACK_UUID, "domain_bonus_attack",
						BAL.d("domain_bonus_attack", DOMAIN_BONUS_ATTACK), AttributeModifier.Operation.ADDITION));
			}
			// 速度I效??
			wolf.addEffect(new MobEffectInstance(
					MobEffects.MOVEMENT_SPEED, wolfDuration, 0, false, false, true));
		}

		// 饰品效果：降低冥狼攻击???5%、血?5%
		if (hasTrinketEquipped(player)) {
			AttributeInstance atkAttr = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
			if (atkAttr != null && atkAttr.getModifier(TRINKET_DAMAGE_UUID) == null) {
				atkAttr.addPermanentModifier(new AttributeModifier(
						TRINKET_DAMAGE_UUID, "trinket_damage_reduction",
						TRINKET_DAMAGE_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
			}
			AttributeInstance hpAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
			if (hpAttr != null && hpAttr.getModifier(TRINKET_HEALTH_UUID) == null) {
				hpAttr.addPermanentModifier(new AttributeModifier(
						TRINKET_HEALTH_UUID, "trinket_health_reduction",
						TRINKET_HEALTH_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
				// 同步当前血量到新上??
				wolf.setHealth(Math.min(wolf.getHealth(), (float) hpAttr.getValue()));
			}
		}

		// 生成粒子效果（增强领域召唤的狼不显示灵魂冒出粒子???
		if (!enhancedDomain) {
			ParticleUtils.spawnParticles(world, ParticleTypes.SOUL_FIRE_FLAME,
					wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
			ParticleUtils.spawnParticles(world, ParticleTypes.SOUL,
					wolf.getX(), wolf.getY() + 0.3, wolf.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		}

		// 播放出现音??
		world.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
				SoundEvents.WOLF_GROWL, SoundSource.NEUTRAL, 1.0f, 0.7f);
		return wolf;
	}
	public static int spellWolfDuration() { return BAL.i("wolf_duration", WOLF_DURATION); }

	private static void refreshWolfBuffs(ServerLevel world, SummonData data) {
		for (UUID wolfUuid : data.summonedWolfUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (entity instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()) {
				// 灵魂沙免疫：在灵魂沙上时给予速度III抵消减???
				BlockPos below = wolf.blockPosition().below();
				if (world.getBlockState(below).is(net.minecraft.world.level.block.Blocks.SOUL_SAND)
						|| world.getBlockState(wolf.blockPosition()).is(net.minecraft.world.level.block.Blocks.SOUL_SAND)) {
					wolf.addEffect(new MobEffectInstance(
							MobEffects.MOVEMENT_SPEED, 25, 2, false, false, false));
				}
			}
		}
	}

	// ==================== 狼召唤与管??====================

	private static void dissipateWolves(ServerPlayer player, SummonData data, ServerLevel world) {
		for (UUID wolfUuid : data.summonedWolfUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (entity instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()) {
				// 濠电偞鍨甸悧濠囧汲鎼达絽鍨濋柟鐑樺灩閹?
				ParticleUtils.spawnParticles(world, ParticleTypes.SOUL,
						wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 12, 0.4, 0.6, 0.4, 0.03);
				ParticleUtils.spawnParticles(world, ParticleTypes.SMOKE,
						wolf.getX(), wolf.getY() + 0.3, wolf.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
				// 播放消散音??
				world.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
						SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 0.8f, 0.8f);
				// 从IPlayerEntityMinion系统移除并销???
				MinionTracker.remove(player, MinionTracker.ANUBIS_WOLF, wolfUuid);
				wolf.discard();
			}
		}
	}

	/**
	 * 消散玩家所有存活的冥狼（通过IPlayerEntityMinion系统????
	 */
	private static void dissipateAllMinions(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		// 获取所有冥狼UUID副本后逐个消??
		List<UUID> minionUuids = new ArrayList<>(MinionTracker.get(player, MinionTracker.ANUBIS_WOLF));
		for (UUID wolfUuid : minionUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (entity instanceof AnubisWolfMinionEntity wolf && wolf.isAlive()) {
				ParticleUtils.spawnParticles(world, ParticleTypes.SOUL,
						wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 12, 0.4, 0.6, 0.4, 0.03);
				wolf.discard();
			}
			MinionTracker.remove(player, MinionTracker.ANUBIS_WOLF, wolfUuid);
		}
	}

	private static int getMinionCount(ServerPlayer player) {
		return MinionTracker.count(player, MinionTracker.ANUBIS_WOLF);
	}

	/**
	 * 检查玩家是否装备了阿努比斯权杖上的水晶（框架无关：自动适??Trinkets/Curios??
	 */
	public static boolean hasTrinketEquipped(ServerPlayer player) {
		return TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.ANUBIS_CRYSTAL.get());
	}

	/** Soul sand lowers the anchor into its block; do not choose air below a thin floor. */
	public static BlockPos summonPosition(ServerLevel world, ServerPlayer owner, Entity anchor) {
		BlockPos candidate = MinionTracker.nearbyEmptySpace(world, owner.getRandom(), anchor.blockPosition(), 3, 1, 1, 4);
		if (candidate != null && (world.getFluidState(candidate).is(net.minecraft.tags.FluidTags.WATER)
				|| !world.getBlockState(candidate.below()).getCollisionShape(world, candidate.below()).isEmpty())) {
			return candidate;
		}
		return BlockPos.containing(anchor.getX(), Math.ceil(anchor.getY()), anchor.getZ());
	}

	// ==================== 杈呭姪鏂规??====================

	private static void applyHowlSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null && speedAttr.getModifier(HOWL_SLOW_UUID) == null) {
			speedAttr.addTransientModifier(new AttributeModifier(
					HOWL_SLOW_UUID, "howl_slow",
					BAL.d("howl_slow_factor", HOWL_SLOW_FACTOR) - 1.0, // -0.5 = 保???0%
					AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	private static void removeHowlSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(HOWL_SLOW_UUID);
		}
	}

	/**
	 * 周期性更新所有冥狼的饰品属性修饰???
	 * 遍历玩家所有冥狼，根据饰品装备状态添???移??25%攻击力??35%血量修饰符
	 * 主要覆盖受??攻击时由JSON power召唤的冥狼（它们不经过spawnMinionWolf??
	 */
	private static void tickTrinketModifiers(ServerPlayer player) {
		ServerLevel world = player.serverLevel();
		boolean hasCrystal = hasTrinketEquipped(player);

		List<UUID> wolfUuids = new ArrayList<>(MinionTracker.get(player, MinionTracker.ANUBIS_WOLF));
		for (UUID wolfUuid : wolfUuids) {
			Entity entity = world.getEntity(wolfUuid);
			if (!(entity instanceof AnubisWolfMinionEntity wolf) || !wolf.isAlive()) continue;

			// 攻击力修饰???
			AttributeInstance attackAttr = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attackAttr != null) {
				AttributeModifier dmgMod = attackAttr.getModifier(TRINKET_DAMAGE_UUID);
				if (hasCrystal && dmgMod == null) {
					attackAttr.addPermanentModifier(new AttributeModifier(
							TRINKET_DAMAGE_UUID, "trinket_damage_reduction",
							TRINKET_DAMAGE_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
				} else if (!hasCrystal && dmgMod != null) {
					attackAttr.removeModifier(TRINKET_DAMAGE_UUID);
				}
			}

			// 閻炴稈鍋撻梺鎻掔箣閹便劍顨滈幍顕呭剨
			AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
			if (healthAttr != null) {
				AttributeModifier hpMod = healthAttr.getModifier(TRINKET_HEALTH_UUID);
				if (hasCrystal && hpMod == null) {
					healthAttr.addPermanentModifier(new AttributeModifier(
							TRINKET_HEALTH_UUID, "trinket_health_reduction",
							TRINKET_HEALTH_REDUCTION, AttributeModifier.Operation.MULTIPLY_TOTAL));
					wolf.setHealth(Math.min(wolf.getHealth(), (float) healthAttr.getValue()));
				} else if (!hasCrystal && hpMod != null) {
					healthAttr.removeModifier(TRINKET_HEALTH_UUID);
				}
			}
		}
	}

	// ==================== 阶段枚???====================
	private enum Phase {
		HOWLING,     // 嚎叫蓄力中??.5秒??
		SUMMONING,   // 婵炲瓨鍤庨崐婵嬪极婵犲洤鐭楁い鎺嗗亾闁轰焦绮嶇粙?
		ACTIVE,      // 冥狼活动中???0秒??
	}

	// ==================== 数据?====================
	private static class SummonData {
        long castId = -1;
		Phase phase;
		int ticksElapsed;
		int wolvesToSummon;     // 本次需要召唤的总???
		int wolvesSummoned;     // 已召唤的数??
		boolean domainActive;   // 召唤时是否有激活的死亡领域
		boolean enhancedDomain; // 是否为增强死亡领域召唤（不显示灵魂冒出粒子??
		List<UUID> summonedWolfUuids = new ArrayList<>(); // 当前批次召唤的狼UUID

		SummonData(int wolvesToSummon, boolean domainActive) {
			this.phase = Phase.HOWLING;
			this.ticksElapsed = 0;
			this.wolvesToSummon = wolvesToSummon;
			this.wolvesSummoned = 0;
			this.domainActive = domainActive;
			this.enhancedDomain = false;
		}
	}
}
