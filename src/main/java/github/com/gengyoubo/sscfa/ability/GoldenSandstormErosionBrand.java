package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.GoldenSandstormPerks;
import github.com.gengyoubo.sscfa.item.ErosionSandPrismItem;
import github.com.gengyoubo.sscfa.item.WitheredSandRingItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 金沙岚SP - 侵蚀烙印（被动标记系统???
 * <p>
 * 近战攻击时自动叠加侵蚀烙印，每秒最多??层，每个目标最???层??
 * 烙印持???0秒（每次叠加刷新计时器）??
 * <p>
 * 层数颜色对应（用于entity_glow）??
 * - 1閻?闁?濮掓稑瀚竟?
 * - 2閻?闁?婵鐟ㄦ竟?
 * - 3????红??
 * - 绿色状?????已触发被动爆发??秒内不可再次叠层（绿色标志持???0秒??
 * <p>
 * ??次攻击???层满后继续攻击）触发被动爆发???
 * - 造成目标当前生命??0%的伤害（上???0??
 * - 自身回???0%最大生命??
 * - 清除所有层数，进入绿色状态???秒不可叠层，绿色标志持???0秒??
 * - 绿色状???秒冷却期后可重新叠??
 * <p>
 * 引爆（次要技能）??
 * - 造成目标当前生命??0%的伤害（上???0??
 * - 自身按已损生命值???0%回复一???
 */
public class GoldenSandstormErosionBrand {

	// ==================== 常??====================
	// 带「默认」注释的常量已登?balance（scope：abilities.golden_sandstorm_erosion_brand），运行时从快照读取???
	// 其余（STACK_COOLDOWN* / BURST_DAMAGE_CAP_WITH_RING / PRISM_SPLASH_PERCENT）未登记，保持编译期常量??
	/** 烙印持续时间（tick），每次叠加刷新。默认；运行时从 balance 快照读???*/
	private static final int BRAND_DURATION = 200; // 10缂?
	/** 绿色状态持续时间（tick），仅用于视觉标记。默认；运行时从 balance 快照读???*/
	private static final int GREEN_DURATION = 200; // 10缂?
	/** 爆发后叠层冷却时间（tick），冷却结束后可重新叠层。默认；运行时从 balance 快照读???*/
	private static final int BRAND_STACK_COOLDOWN = 100; // 5缂?
	/** 最大叠加层数。默认；运行时??balance 快照读???*/
	private static final int MAX_STACKS = 3;
	/** 被动爆发生命百分比伤害。默认；运行时从 balance 快照读???*/
	private static final float BURST_HP_PERCENT = 0.20f;
	/** 被动爆发伤害上限。默认；运行时从 balance 快照读???*/
	private static final float BURST_DAMAGE_CAP = 20.0f;
	/** 被动爆发自愈百分比。默认；运行时从 balance 快照读???*/
	private static final float BURST_HEAL_PERCENT = 0.10f;
	/** 叠层冷却（tick），同一目??秒内最多?????*/
	private static final long STACK_COOLDOWN = 20L;
	/** 蚀沙棱晶装备时的叠层冷却（tick）??.3??*/
	private static final long STACK_COOLDOWN_WITH_PRISM = 26L;
	/** 枯沙指环装备时的被动爆发伤害上限??30%??*/
	private static final float BURST_DAMAGE_CAP_WITH_RING = 26.0f;
	/** 蚀沙棱晶扩散伤害范围（格）。默认；运行时??balance 快照读???*/
	private static final double SPREAD_RANGE = 5.0;
	/** 扩散叠标记半径（格）。默认；运行时??balance 快照读???*/
	private static final double SPLASH_BRAND_RANGE = 4.0;
	/** 引爆自愈：回复已损生命值的百分比。默认；运行时??balance 快照读???*/
	private static final float DETONATE_HEAL_LOST_PERCENT = 0.20f;
	/** 蚀沙棱晶：扩散伤害占引爆伤害的比例??0%??*/
	private static final float PRISM_SPLASH_PERCENT = 0.40f;

	// ==================== 自定义伤害类???====================
	/** 被动爆发伤害类型（绕过近战攻击的伤害免疫帧??*/
	private static final ResourceKey<DamageType> CURSED_BURST_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("my_addon", "cursed_burst"));
	/** 引爆伤害类型（绕过被动爆发的伤害免疫帧??*/
	private static final ResourceKey<DamageType> CURSED_EROSION_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("my_addon", "cursed_erosion"));

	// ==================== 缂傚啯鍨圭划鍫曞触鐏炵虎鍔?====================
	/** S2C 包ID：同步烙印颜色数据到客户?*/
	public static final ResourceLocation PACKET_BRAND_SYNC = new ResourceLocation("ssc_addon", "erosion_brand_sync");
	/** 需要同步的玩家标记集??*/
	private static final Set<UUID> DIRTY_PLAYERS = ConcurrentHashMap.newKeySet();

	// ==================== 闁绘鍩栭埀顑挎祰閹风兘鐓?====================
	/** 玩家UUID -> { 目标UUID -> 烙印状???} */
	private static final ConcurrentHashMap<UUID, Map<UUID, BrandState>> ACTIVE_BRANDS = new ConcurrentHashMap<>();

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.golden_sandstorm_erosion_brand");
	private static final BalanceReader CAST_BAL = new BalanceReader("systems.form_casting");

	private GoldenSandstormErosionBrand() {
	}

	// ==================== 饰品检???====================

	/** 检查玩家是否装备了蚀沙棱晶（双重检测：tick追???+ 抽象层查询???*/
	private static boolean hasErosionPrism(ServerPlayer player) {
		// 方??：tick回调追???
		if (ErosionSandPrismItem.isEquippedBy(player)) return true;
		// 方??：走框架无关抽象层（后备方案，自动适??Trinkets/Curios??
		return TrinketUtils.isWearing(player, stack -> stack.getItem() instanceof ErosionSandPrismItem);
	}

	/** 检查玩家是否装备了枯沙指环（双重检测：tick追???+ 抽象层查询???*/
	private static boolean hasWitheredRing(ServerPlayer player) {
		// 方??：tick回调追???
		if (WitheredSandRingItem.isEquippedBy(player)) return true;
		// 方??：走框架无关抽象层（后备方案，自动适??Trinkets/Curios??
		return TrinketUtils.isWearing(player, stack -> stack.getItem() instanceof WitheredSandRingItem);
	}

	/**
	 * 近战攻击时调???- 自动叠加烙??
	 * 该方法???SscAddonActions ??erosion_brand_hit biEntity action 触??
	 */
	public static void onPlayerAttack(ServerPlayer player, LivingEntity target) {
		if (github.com.gengyoubo.sscfa.spell.CourtSpellLink.suppressLegacy()) return;
		applyBrand(player, target, 1);
	}
	public static void onSpellHit(ServerPlayer player, LivingEntity target) {
		if (WhitelistUtils.isProtected(player,target)) return;
		applyBrand(player,target,1);
	}

	/** A sandstorm adds its captured layer bonus in one operation, not two attacks. */
	public static void onSandstormHit(ServerPlayer player, LivingEntity target, int layers) {
		applyBrand(player, target, Math.max(1, layers));
	}

	private static void applyBrand(ServerPlayer player, LivingEntity target, int layers) {
		if (!FormUtils.isGoldenSandstormSP(player) || !target.isAlive()) return;
		if (!(player.level() instanceof ServerLevel serverWorld)) return;
		if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)) return;
		// 近战攻击不检查白名单（允许对白名单目标叠标记???
		// 白名单仅限制扩散和引爆的叠标?

		UUID playerUuid = player.getUUID();
		UUID targetUuid = target.getUUID();
		long currentTick = serverWorld.getGameTime();

		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.computeIfAbsent(playerUuid, k -> new HashMap<>());
		BrandState state = playerBrands.get(targetUuid);
		if (state != null && currentTick >= state.expiryTick) {playerBrands.remove(targetUuid);state=null;}
		long effectiveCooldown = (hasErosionPrism(player) ? STACK_COOLDOWN_WITH_PRISM : STACK_COOLDOWN)
				* CAST_BAL.i("sandstorm_stack_cooldown_multiplier",2);
		// One shared gate includes the hit that bursts a full stack; no rapid fourth-hit bypass.
		if (state != null && !state.greenState && currentTick-state.lastStackTick<effectiveCooldown) {
			state.expiryTick=currentTick+BAL.i("brand_duration",BRAND_DURATION);markDirty(playerUuid);updateTargetMarker(target,state,currentTick,player);return;
		}

		if (state != null && state.greenState) {
			// 叠层冷却期间（前5秒）：不叠层，不触发被动爆??
			if (currentTick < state.stackCooldownExpiryTick) {
				return;
			}
			// 冷却结束??-10秒），取消绿色状态，?层开始正常叠?
			state.greenState = false;
			state.stacks = 0;
		}
		// 标记需要同??
		markDirty(playerUuid);

		if (state != null && state.stacks >= BAL.i("max_stacks", MAX_STACKS)) {
			// 已??层，?次攻击触发被动爆???
			// !! 必须先重置状态再造成伤害，否???target.damage() 会通??Apoli action 递归调???onPlayerAttack
			state.stacks = 0;
			state.greenState = true;
			int greenDuration = BAL.i("green_duration", GREEN_DURATION);
			int stackCooldown = BAL.i("brand_stack_cooldown", BRAND_STACK_COOLDOWN);
			state.greenExpiryTick = currentTick + greenDuration;			state.stackCooldownExpiryTick = currentTick + stackCooldown;			state.expiryTick = currentTick + greenDuration;

			// 同步标记效果到客户端（放在伤害前确保即时生效??
			updateTargetMarker(target, state, currentTick, player);

			// 现在安全地造成伤??
			triggerPassiveBurst(player, target, serverWorld);

			// 蚀沙棱晶：被动爆发也触发扩散（白名单目标不触发扩散??
			if (hasErosionPrism(player) && !WhitelistUtils.isProtected(player, target)) {
				float burstDmg = Math.min(target.getHealth() * damagePercent(player), damageCap(player, hasWitheredRing(player)));
				if (burstDmg < 1.0f) burstDmg = 1.0f;
				float splashDmg = burstDmg * PRISM_SPLASH_PERCENT;
				splashDamageToNearby(player, serverWorld, target.position(), splashDmg, target);
			}

			// 爆发音效和粒???
			serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.38f, 1.2f);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					30, 0.5, 0.5, 0.5, 0.1);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLASH,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					1, 0, 0, 0, 0);
			return;
		}

		// 正常叠???
		if (state == null) {
			state = new BrandState();
			playerBrands.put(targetUuid, state);
		}

		// 检查叠层冷却（蚀沙棱晶??.3秒，默认??秒??
		if (currentTick - state.lastStackTick < effectiveCooldown) {
			// 冷却中，刷新持续时间但不叠??
			state.expiryTick = currentTick + BAL.i("brand_duration", BRAND_DURATION);
			updateTargetMarker(target, state, currentTick, player); // 刷新标记持续时间
			return;
		}

		state.stacks = Math.min(state.stacks + layers, BAL.i("max_stacks", MAX_STACKS));
		state.lastStackTick = currentTick;
		state.expiryTick = currentTick + BAL.i("brand_duration", BRAND_DURATION);

		// 同步标记效果到客户??
		updateTargetMarker(target, state, currentTick, player);

		// 叠加粒子提??
		ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				5 * state.stacks, 0.2, 0.2, 0.2, 0.05);

		// 叠加音???
		serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 0.8f + state.stacks * 0.2f);
	}

	/**
	 * 触发被动爆??- 造??0%当前生命伤害（上??0），自我回??0%最大生??
	 */
	private static void triggerPassiveBurst(ServerPlayer player, LivingEntity target, ServerLevel serverWorld) {
		// 计算伤害：目标当前生命值???0%（枯沙指环：上???6，默认：上??0??
		boolean hasRing = hasWitheredRing(player);
		float cap = damageCap(player, hasRing);
		float rawDamage = target.getHealth() * damagePercent(player);
		float damage = Math.min(rawDamage, cap);
		if (damage < 1.0f) damage = 1.0f;

		// 造成自定义伤害（绕过近战攻击的伤害免疫帧??
		Vec3 oldVelocity = target.getDeltaMovement();
		target.invulnerableTime = 0; // 重置伤害免疫计时??
		boolean damaged = target.hurt(new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_BURST_KEY), player), damage);
		if (damaged) {
			target.setDeltaMovement(oldVelocity); // 不击??
		}

		// 自我回???0%最大生命??
		float healAmount = player.getMaxHealth() * (float) BAL.d("burst_heal_percent", BURST_HEAL_PERCENT);
		player.heal(healAmount);
	}

	/**
	 * 次要技能引???- 引爆所有非绿色标记的目???
	 * @param player 施法?
	 * @return 引爆信息：[引爆目标?? 总层??
	 */
	public static int[] detonateAll(ServerPlayer player) {
		if (!FormUtils.isGoldenSandstormSP(player)) return new int[]{0, 0};
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		if (playerBrands == null || playerBrands.isEmpty()) return new int[]{0, 0};

		if (!(player.level() instanceof ServerLevel serverWorld)) return new int[]{0, 0};

		// 预先检查饰品状态（避免循环内重复检查??
		boolean hasRing = hasWitheredRing(player);
		boolean hasPrism = hasErosionPrism(player);
		float cap = damageCap(player, hasRing);
		int totalTargets = 0;
		int totalStacks = 0;

		// Prism splash can create more entries. Only detonate the accepted target snapshot.
		for (Map.Entry<UUID, BrandState> entry : new ArrayList<>(playerBrands.entrySet())) {
			if (playerBrands.get(entry.getKey()) != entry.getValue()) continue;
			BrandState state = entry.getValue();

			// 绿色状态的目标不参与引???
			if (state.greenState || state.stacks <= 0) {
				continue;
			}

			LivingEntity target = findEntity(serverWorld, entry.getKey());
			if (target == null || !target.isAlive()) {
				playerBrands.remove(entry.getKey(), state);
				continue;
			}
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)) continue;

			// 如果目标??层，先触发被动爆发（不消耗层数）
			if (state.stacks >= BAL.i("max_stacks", MAX_STACKS)) {
				triggerPassiveBurst(player, target, serverWorld);
				// 被动爆发的额外粒??
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLASH,
						target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
						1, 0, 0, 0, 0);
			}

			totalTargets++;
			totalStacks += state.stacks;

			// 引爆伤???= 目标当前生命???0%（上?0/26??
			float rawDetonateDmg = target.getHealth() * damagePercent(player);
			float detonateDamage = Math.min(rawDetonateDmg, cap);
			if (detonateDamage < 1.0f) detonateDamage = 1.0f;

			// 蚀沙棱晶：主目标承??0%??0%扩散给周??
			float mainDamage = hasPrism ? detonateDamage * (1.0f - PRISM_SPLASH_PERCENT) : detonateDamage;
			Vec3 oldVelocity = target.getDeltaMovement();
			target.invulnerableTime = 0; // 重置伤害免疫计时??
			boolean detonateResult = target.hurt(new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_EROSION_KEY), player), mainDamage);
			if (detonateResult) {
				target.setDeltaMovement(oldVelocity);
			}

			// 蚀沙棱晶：40%引爆伤害扩散给周??格内的生??
			if (hasPrism) {
				float splashDamage = detonateDamage * PRISM_SPLASH_PERCENT;
				splashDamageToNearby(player, serverWorld, target.position(), splashDamage, target);
			}

			// 瀵洜鍨庣划鎺戠?
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					20 * state.stacks, 0.5, 0.5, 0.5, 0.1);

			// 进入绿色状??
			long currentTick = serverWorld.getGameTime();
			int greenDuration = BAL.i("green_duration", GREEN_DURATION);
			state.stacks = 0;
			state.greenState = true;
			state.greenExpiryTick = currentTick + greenDuration;
			state.stackCooldownExpiryTick = currentTick + BAL.i("brand_stack_cooldown", BRAND_STACK_COOLDOWN);
			state.expiryTick = currentTick + greenDuration;

			// 同步标记效果到客户??
			updateTargetMarker(target, state, currentTick, player);
		}

		// 自我回复：已损生命值??0%
		if (totalTargets > 0) {
			float lostHealth = player.getMaxHealth() - player.getHealth();
			if (lostHealth > 0) {
				float healAmount = lostHealth * (float) BAL.d("detonate_heal_lost_percent", DETONATE_HEAL_LOST_PERCENT);
				player.heal(healAmount);
			}
			markDirty(player.getUUID());
		}

		return new int[]{totalTargets, totalStacks};
	}

	private static float damagePercent(ServerPlayer player) {
		return GoldenSandstormPerks.damagePercent(player, (float) BAL.d("burst_hp_percent", BURST_HP_PERCENT));
	}

	private static float damageCap(ServerPlayer player, boolean ring) {
		return GoldenSandstormPerks.damageCap(player, ring ? BURST_DAMAGE_CAP_WITH_RING : (float) BAL.d("burst_damage_cap", BURST_DAMAGE_CAP));
	}

	/** Consume this player's valid, loaded, non-green layers without running any damage path. */
	public static int recallAll(ServerPlayer player) {
		if (!FormUtils.isGoldenSandstormSP(player) || github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return 0;
		var brands = ACTIVE_BRANDS.get(player.getUUID());
		if (brands == null || brands.isEmpty()) return 0;
		var world = player.serverLevel();
		long now = world.getGameTime();
		int layers = 0;
		boolean changed = false;
		for (var entry : new ArrayList<>(brands.entrySet())) {
			var state = entry.getValue();
			if (state.greenState || state.stacks <= 0) continue;
			var target = findEntity(world, entry.getKey());
			// An unloaded target or a target in another dimension cannot fund healing.
			if (target == null) continue;
			if (state.expiryTick <= now || !target.isAlive()) {
				if (brands.remove(entry.getKey(), state)) {
					refreshOtherMarkers(target, player.getUUID());
					changed = true;
				}
				continue;
			}
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)) continue;
			if (brands.remove(entry.getKey(), state)) {
				layers += state.stacks;
				refreshOtherMarkers(target, player.getUUID());
				changed = true;
			}
		}
		if (changed) {
			if (brands.isEmpty()) ACTIVE_BRANDS.remove(player.getUUID(), brands);
			markDirty(player.getUUID());
			syncIfDirty(player);
		}
		if (layers > 0) player.heal(GoldenSandstormPerks.recallHealing(player.getMaxHealth(), layers));
		return layers;
	}

	/** Shared status icons must not disappear when another owner's layers are removed. */
	private static void refreshOtherMarkers(LivingEntity target, UUID removedOwner) {
		if (!(target.level() instanceof ServerLevel world)) return;
		BrandState selected = null;
		ServerPlayer owner = null;
		long now = world.getGameTime();
		for (var entry : ACTIVE_BRANDS.entrySet()) {
			if (entry.getKey().equals(removedOwner)) continue;
			var candidate = entry.getValue().get(target.getUUID());
			if (candidate == null || candidate.greenState || candidate.stacks <= 0 || candidate.expiryTick <= now) continue;
			var other = world.getPlayerByUUID(entry.getKey());
			if (!(other instanceof ServerPlayer player) || !FormUtils.isGoldenSandstormSP(player)) continue;
			if (selected == null || candidate.lastStackTick > selected.lastStackTick) { selected = candidate; owner = player; }
		}
		if (selected == null) removeAllBrandMarkers(target);
		else updateTargetMarker(target, selected, now, owner);
	}

	/**
	 * 蚀沙棱晶效??- 对引?爆发目标周??格内的非白名单生物造成扩散伤害?
	 * 并??格半径球内的目标额外?层烙印。若叠到?层则触发被动爆发并重置层数???
	 */
	private static void splashDamageToNearby(ServerPlayer player, ServerLevel serverWorld, Vec3 center, float damage, LivingEntity excludeTarget) {
		UUID playerUuid = player.getUUID();
		UUID excludeUuid = excludeTarget != null ? excludeTarget.getUUID() : null;

		// 确保最低伤害??
		if (damage < 1.0f) damage = 1.0f;

		// 使用较大的范围搜索（取伤害范围和叠标记范围的较大值??
		double spreadRange = BAL.d("spread_range", SPREAD_RANGE);
		double splashBrandRange = BAL.d("splash_brand_range", SPLASH_BRAND_RANGE);
		double maxRange = Math.max(spreadRange, splashBrandRange);
		AABB searchBox = new AABB(
				center.x - maxRange, center.y - maxRange, center.z - maxRange,
				center.x + maxRange, center.y + maxRange, center.z + maxRange
		);

		List<Entity> allEntities = serverWorld.getEntities(player, searchBox);

		DamageSource splashDamageSource = new net.minecraft.world.damagesource.DamageSource(player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_EROSION_KEY), player);
		long currentTick = serverWorld.getGameTime();
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.computeIfAbsent(playerUuid, k -> new HashMap<>());
		int hitCount = 0;

		// 收集需要触发爆发的目标（避免在遍历中修改集合??
		List<LivingEntity> burstTargets = new ArrayList<>();

		for (Entity entity : allEntities) {
			if (!(entity instanceof LivingEntity living)) continue;
			if (!living.isAlive()) continue;
			if (living.getUUID().equals(playerUuid) || living.getUUID().equals(excludeUuid)) continue;
			if (WhitelistUtils.isProtected(player, living)) continue;
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, living)) continue;

			double distSq = living.distanceToSqr(center);

			// 伤害范围检查??格??
			if (distSq <= spreadRange * spreadRange) {
				Vec3 oldVelocity = living.getDeltaMovement();
				living.invulnerableTime = 0;
				boolean damaged = living.hurt(splashDamageSource, damage);
				if (damaged) {
					living.setDeltaMovement(oldVelocity);
					hitCount++;
				}
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
						living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
						8, 0.3, 0.3, 0.3, 0.05);
			}

			// 叠标记范围检查（4格半径???
			if (distSq <= splashBrandRange * splashBrandRange) {
				UUID livingUuid = living.getUUID();
				BrandState state = playerBrands.get(livingUuid);

				// 绿色状态的目标：检查叠层冷却是否结??
				if (state != null && state.greenState) {
					if (currentTick < state.stackCooldownExpiryTick) {
						continue;
					}
					// 冷却结束，取消绿色状态，??开始叠???
					state.greenState = false;
					state.stacks = 0;
				}

				if (state == null) {
					state = new BrandState();
					playerBrands.put(livingUuid, state);
				}

				state.stacks = Math.min(state.stacks + 1, BAL.i("max_stacks", MAX_STACKS));
				state.lastStackTick = currentTick;
				state.expiryTick = currentTick + BAL.i("brand_duration", BRAND_DURATION);

				// ??层触发爆?
				if (state.stacks >= BAL.i("max_stacks", MAX_STACKS)) {
					state.stacks = 0;
					state.greenState = true;
					int greenDuration = BAL.i("green_duration", GREEN_DURATION);
					state.greenExpiryTick = currentTick + greenDuration;
					state.stackCooldownExpiryTick = currentTick + BAL.i("brand_stack_cooldown", BRAND_STACK_COOLDOWN);
					state.expiryTick = currentTick + greenDuration;
					updateTargetMarker(living, state, currentTick, player);
					burstTargets.add(living);
				} else {
					updateTargetMarker(living, state, currentTick, player);
				}
			}
		}

		// 延迟处理??层的爆发（避免遍历中修改状态冲突???
		for (LivingEntity burstTarget : burstTargets) {
			triggerPassiveBurst(player, burstTarget, serverWorld);
			serverWorld.playSound(null, burstTarget.getX(), burstTarget.getY(), burstTarget.getZ(),
					SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.38f, 1.2f);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
					burstTarget.getX(), burstTarget.getY() + burstTarget.getBbHeight() * 0.5, burstTarget.getZ(),
					30, 0.5, 0.5, 0.5, 0.1);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLASH,
					burstTarget.getX(), burstTarget.getY() + burstTarget.getBbHeight() * 0.5, burstTarget.getZ(),
					1, 0, 0, 0, 0);
		}

		markDirty(playerUuid);

		if (hitCount > 0) {
			serverWorld.playSound(null, center.x, center.y, center.z,
					SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.6f, 0.6f);
		}
	}

	/**
	 * 获取指定玩家对指定目标的烙印颜色
	 * @return "yellow"/"orange"/"red"/"green"/null
	 */
	public static String getBrandColor(UUID playerUuid, UUID targetUuid) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(playerUuid);
		if (playerBrands == null) return null;
		return getColorFromState(playerBrands.get(targetUuid));
	}

	/**
	 * 检查指定玩家对指定目标是否有某颜色的烙???
	 * 提供?BiEntity 条件使??
	 */
	public static boolean hasColor(UUID playerUuid, UUID targetUuid, String color) {
		String currentColor = getBrandColor(playerUuid, targetUuid);
		return color.equals(currentColor);
	}

	/**
	 * 每tick更??- 清理过期烙??
	 */
	public static void tick(ServerPlayer player) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		if (playerBrands == null) return;

		// 瑜般垺鈧焦顥呴?
		if (!FormUtils.isGoldenSandstormSP(player)) {
			// 脱离形态，清理所有目标的标记效??
			if (player.level() instanceof ServerLevel serverWorld) {
				for (UUID targetUuid : playerBrands.keySet()) {
					LivingEntity target = findEntity(serverWorld, targetUuid);
					if (target != null) refreshOtherMarkers(target, player.getUUID());
				}
			}
			ACTIVE_BRANDS.remove(player.getUUID());
			DIRTY_PLAYERS.remove(player.getUUID());
			// 发送空同步包到客户端，清除发光缓存（防止切回形态时残留旧数据??
			try {
				FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
				buf.writeInt(0);
				github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
			} catch (Exception ignored) {
				// 发送失败时忽??
			}
			return;
		}

		long currentTick = player.serverLevel().getGameTime();
		ServerLevel serverWorld = player.serverLevel();
		boolean[] hadExpiry = {false};
		playerBrands.entrySet().removeIf(entry -> {
			BrandState state = entry.getValue();
			boolean expired = state.greenState && currentTick >= state.greenExpiryTick; // 绿色状态过期检?
            // 普通烙印过期检???
			if (!state.greenState && currentTick >= state.expiryTick) {
				expired = true;
			}
			if (expired) {
				hadExpiry[0] = true;
				// 移除目标的标记效??
				LivingEntity target = findEntity(serverWorld, entry.getKey());
				if (target != null) refreshOtherMarkers(target, player.getUUID());
			}
			return expired;
		});
		if (hadExpiry[0]) {
			markDirty(player.getUUID());
		}

		// 如果该玩家没有任何活跃烙印了，清除整个map
		if (playerBrands.isEmpty()) {
			ACTIVE_BRANDS.remove(player.getUUID());
		}

		// 同步烙印状态到客户端（仅在父变时发送）
		syncIfDirty(player);
	}

	/**
	 * 玩家断???变形时清理，同时移除目标上的标记效果并发送空同步??
	 */
	public static void clearPlayer(ServerPlayer player) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.remove(player.getUUID());
		if (playerBrands != null && player.level() instanceof ServerLevel serverWorld) {
			for (UUID targetUuid : playerBrands.keySet()) {
				LivingEntity target = findEntity(serverWorld, targetUuid);
				if (target != null) refreshOtherMarkers(target, player.getUUID());
			}
		}
		DIRTY_PLAYERS.remove(player.getUUID());
		// 发送空同步包到客户?
		try {
			FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
			buf.writeInt(0);
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
		} catch (Exception ignored) {
			// 玩家可能已断线，忽略发送失??
		}
	}

	/**
	 * 服务器重??热重载时清理所有状??
	 */
	public static void clearAll() {
		ACTIVE_BRANDS.clear();
		DIRTY_PLAYERS.clear();
	}

	/** 重??换维度后强制把当前烙印状态同步给该玩家（无视 dirty 标记?*/
	public static void resyncToPlayer(ServerPlayer player) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		if (playerBrands == null || playerBrands.isEmpty()) {
			buf.writeInt(0);
		} else {
			List<Map.Entry<UUID, String>> entries = new ArrayList<>();
			for (Map.Entry<UUID, BrandState> entry : playerBrands.entrySet()) {
				String color = getColorFromState(entry.getValue());
				if (color != null) entries.add(Map.entry(entry.getKey(), color));
			}
			buf.writeInt(entries.size());
			for (Map.Entry<UUID, String> entry : entries) {
				buf.writeUUID(entry.getKey());
				buf.writeUtf(entry.getValue());
			}
		}
		try {
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
		} catch (Exception ignored) {}
		DIRTY_PLAYERS.remove(player.getUUID());
	}

	// ==================== 网络同步方??====================

	/**
	 * 标记玩家的烙印数据需要同步到客户?
	 */
	private static void markDirty(UUID playerUuid) {
		DIRTY_PLAYERS.add(playerUuid);
	}

	/**
	 * 如果玩家的烙印数据已变化，同步到其客户端
	 * ??tick 结束时调用，避免每次变化都发???
	 */
	private static void syncIfDirty(ServerPlayer player) {
		if (!DIRTY_PLAYERS.remove(player.getUUID())) return;

		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();

		if (playerBrands == null || playerBrands.isEmpty()) {
			buf.writeInt(0);
		} else {
			// 收集有效颜色条??
			List<Map.Entry<UUID, String>> entries = new ArrayList<>();
			for (Map.Entry<UUID, BrandState> entry : playerBrands.entrySet()) {
				String color = getColorFromState(entry.getValue());
				if (color != null) {
					entries.add(Map.entry(entry.getKey(), color));
				}
			}
			buf.writeInt(entries.size());
			for (Map.Entry<UUID, String> entry : entries) {
				buf.writeUUID(entry.getKey());
				buf.writeUtf(entry.getValue());
			}
		}

		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
	}

	/**
	 * ??BrandState 获取颜色字符???
	 */
	private static String getColorFromState(BrandState state) {
		if (state == null) return null;
		if (state.greenState) return "green";
		return switch (state.stacks) {
			case 1 -> "yellow";
			case 2 -> "orange";
			case 3 -> "red";
			default -> null;
		};
	}

	/**
	 * 同步烙印状态到客户?- 通过状态效果实?
	 * 拆分?个独立效果以支持不同层数显示不同图标???
	 * erosion_brand_marker_1: 1??黄??, erosion_brand_marker_2: 2??橙??, erosion_brand_marker_3: 3??红??
	 * 绿色冷却状态不显示图标?
	 */
	/** 同步标记到目标（owner 作为效???source，供食梦魔「入梦」debuff/高光拦截归因）???*/
	private static void updateTargetMarker(LivingEntity target, BrandState state, long currentTick,
	                                       net.minecraft.server.level.ServerPlayer owner) {
		if (state == null || (state.stacks <= 0 && !state.greenState)) {
			removeAllBrandMarkers(target);
			return;
		}
		int duration;
		if (state.greenState) {
			// 绿色冷却状态：不显示图标，移除所有标?
			removeAllBrandMarkers(target);
			return;
		} else {
			duration = (int) (state.expiryTick - currentTick);
		}
		if (duration <= 0) {
			removeAllBrandMarkers(target);
			return;
		}
		// 先移除所有旧标记，再施加当前层数对应的标??
		removeAllBrandMarkers(target);
		net.minecraft.world.effect.MobEffect markerEffect = switch (state.stacks) {
			case 1 -> github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_1.get();
			case 2 -> github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_2.get();
			case 3 -> github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_3.get();
			default -> null;
		};
		if (markerEffect != null) {
			// ambient=false, showParticles=false, showIcon=true（显示对应层数图标??
			target.addEffect(new MobEffectInstance(
					markerEffect, duration, 0, false, false, true
			), owner);
		}
	}

	/**
	 * 移除目标上的所有烙印标记效??
	 */
	private static void removeAllBrandMarkers(LivingEntity target) {
		target.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_1.get());
		target.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_2.get());
		target.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_3.get());
	}

	/**
	 * 在世界中查找目标实??
	 */
	private static LivingEntity findEntity(ServerLevel world, UUID uuid) {
		var entity = world.getEntity(uuid);
		return entity instanceof LivingEntity living ? living : null;
	}

	// ==================== 内部数据??====================
	private static class BrandState {
		int stacks = 0;
		boolean greenState = false;
		long lastStackTick = -100; // 上次叠层时??
		long expiryTick = 0; // 烙印过期时??
		long greenExpiryTick = 0; // 绿色状态过期时间（视觉标记??0秒??
		long stackCooldownExpiryTick = 0; // 叠层冷却过期时间??秒??
	}
}
