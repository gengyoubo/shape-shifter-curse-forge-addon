package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.GoldenSandstormPerks;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 金沙岚SP - 凋零金沙（主动①，蓄力型?
 *
 * 按下技能键开始蓄力（1秒），自身减?0%??
 * 蓄力完成后释?5格AoE金沙风暴??
 * - 对范围内非白名单生物施加致盲效果??秒??
 * - 给范围内目标各叠??层侵蚀烙印
 * - 大量沙尘粒子效??
 *
 * 蓄力期间受到伤害则打断：取消技能，进??秒CD??
 * 正常释放CD: 26秒??20tick??
 */
public class GoldenSandstormWitherSand {

	// ==================== 常??====================
	// 以下常量均为默认值；运行时??balance 快照读取（scope：abilities.golden_sandstorm_wither_sand??
	/** AoE半??*/
	private static final double RADIUS = 15.0;
	/** 致盲持续时间（tick??*/
	private static final int BLIND_DURATION = 60; // 3缁?
	/** 蓄力时间（tick??*/
	private static final int CHARGE_TICKS = 20; // 1缁?
	// 正??打??CD 已由统一冷却服务 SkillCooldowns + power JSON 配置管理，原常量已??
	/** 蓄力减速修正器UUID */
	private static final UUID CHARGE_SLOW_UUID = UUID.fromString("b8c9d0e1-f2a3-4b5c-8d6e-7f8901234567");
	private static final String CHARGE_SLOW_NAME = "Wither Sand Charge Slow";

	// ==================== 状态追?====================
	/** 正在蓄力的玩家：UUID -> 蓄力状?*/
	private static final ConcurrentHashMap<UUID, ChargeState> CHARGING_PLAYERS = new ConcurrentHashMap<>();

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.golden_sandstorm_wither_sand");
	/** 统一冷却服务的稳定技?ID?? power 注册路径）??*/
	private static final String SKILL_ID = "my_addon:form_golden_sandstorm_sp_wither_sand";

	private GoldenSandstormWitherSand() {
	}

	/**
	 * 玩家按下技能??- 开始蓄?
	 */
	public static boolean execute(ServerPlayer player) {
		if (!FormUtils.isGoldenSandstormSP(player)) return false;
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return false;
		// 已经在蓄力中则忽略（CD 门禁已由 power ??begin 统一拦截，??P1-1??
		if (CHARGING_PLAYERS.containsKey(player.getUUID())) return false;

		if (!(player.level() instanceof ServerLevel serverWorld)) return false;

        var spec = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.spec(player, SKILL_ID);
        if (spec == null || !github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
		// 开始蓄?
		ChargeState state = new ChargeState();
		state.startTick = serverWorld.getGameTime();
		state.healthAtStart = player.getHealth();
		CHARGING_PLAYERS.put(player.getUUID(), state);
        state.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		state.brandStacks = GoldenSandstormPerks.sandstormStacks(player);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, SKILL_ID, state.castId,
				GoldenSandstormPerks.normalCooldown(spec.cooldown(), GoldenSandstormPerks.responseRank(player)));

		// 施加减?0%
		applyChargeSlow(player);

		// 蓄力开始音??
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);

		return true;
	}

	/**
	 * 每tick更新蓄力状??
	 * 需要??SscAddon ??tick handler 中调?
	 */
	public static void tick(ServerPlayer player) {
		ChargeState state = CHARGING_PLAYERS.get(player.getUUID());
		if (state == null) return;

		if (!(player.level() instanceof ServerLevel serverWorld)) return;

		// 瑜般垺鈧焦顥呴弻?
		if (!FormUtils.isGoldenSandstormSP(player)) {
			cancelCharge(player, false);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, state.castId);
			return;
		}

		long currentTick = serverWorld.getGameTime();
		long elapsed = currentTick - state.startTick;

		// 检查是否被打断（生命值下?= 受到伤害??
		if (player.getHealth() < state.healthAtStart) {
			// 被打断：??fail_cooldown 结算（统一服务? 打断音效
			cancelCharge(player, true);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(player, SKILL_ID, state.castId);

			serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SAND_FALL, SoundSource.PLAYERS, 1.0f, 0.3f);
			return;
		}

		// 蓄力期间粒子（自身周围旋转沙尘）
		if (elapsed % 4 == 0) {
			double angle = (elapsed * 0.3) % (Math.PI * 2);
			double px = player.getX() + Math.cos(angle) * 1.5;
			double pz = player.getZ() + Math.sin(angle) * 1.5;
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, player,
					new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
					px, player.getY() + 1.0, pz, 3, 0.1, 0.3, 0.1, 0);
		}

		// 蓄力完??
		if (elapsed >= BAL.i("charge_ticks", CHARGE_TICKS)) {
			removeChargeSlow(player);
			CHARGING_PLAYERS.remove(player.getUUID());
			releaseSkill(player, serverWorld, state.castId, state.brandStacks);
		}
	}

	/**
	 * 释放技?- 15格AoE
	 */
	private static void releaseSkill(ServerPlayer player, ServerLevel serverWorld, long castId, int brandStacks) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, castId);
		double radius = BAL.d("radius", RADIUS);

		// 释放音效
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
		serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);

		// 大量粒子效果：金色沙尘风?
		for (int i = 0; i < 100; i++) {
			double angle = Math.random() * Math.PI * 2;
			double dist = Math.random() * radius;
			double px = player.getX() + Math.cos(angle) * dist;
			double pz = player.getZ() + Math.sin(angle) * dist;
			double py = player.getY() + Math.random() * 2.5;
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, player,
					new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
					px, py, pz, 1, 0, 0, 0, 0);
		}
		ParticleUtils.spawnDecorationParticles(serverWorld, player, ParticleTypes.SOUL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, radius * 0.5, 1.0, radius * 0.5, 0.02);

		// 获取范围内生?
		AABB box = player.getBoundingBox().inflate(radius);
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius);

		for (LivingEntity target : targets) {
			// 白名单检??
			if (WhitelistUtils.isProtected(player, target)) continue;

			// 施加致盲效果?秒）；带施法?source 供入梦拦截归?
			target.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.SAND_BLIND.get(), BAL.i("blind_duration", BLIND_DURATION), 0, false, true), player);

			// 叠??层侵蚀烙印
			GoldenSandstormErosionBrand.onSandstormHit(player, target, brandStacks);

			// 命中粒子
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					5, 0.3, 0.3, 0.3, 0.02);
		}
	}

	/**
	 * 取消蓄力
	 * @param interrupted 是否被打断（受到伤害??
	 */
	private static void cancelCharge(ServerPlayer player, boolean interrupted) {
		removeChargeSlow(player);
		CHARGING_PLAYERS.remove(player.getUUID());
	}

	/**
	 * 施加蓄力减??
	 */
	private static void applyChargeSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(CHARGE_SLOW_UUID);
			speedAttr.addTransientModifier(new AttributeModifier(
					CHARGE_SLOW_UUID, CHARGE_SLOW_NAME, -0.50,
					AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	/**
	 * 移除蓄力减??
	 */
	private static void removeChargeSlow(ServerPlayer player) {
		AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			speedAttr.removeModifier(CHARGE_SLOW_UUID);
		}
	}

	/**
	 * 检查玩家是否正在蓄力（可用于阻止其他操作??
	 */
	public static boolean isCharging(UUID playerUuid) {
		return CHARGING_PLAYERS.containsKey(playerUuid);
	}

	/**
	 * 玩家断线/变形时清??
	 */
	public static void clearPlayer(ServerPlayer player) {
        ChargeState state = CHARGING_PLAYERS.remove(player.getUUID());
        if (state != null) {
            removeChargeSlow(player);
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, state.castId);
        }
	}

	/**
	 * 服务器重??热重载时清理所有状??
	 * 传??server 以便逐个移除在线玩家身上的蓄力减速修饰符，避?reload 后玩家被永久减速??
	 * 允??server ??null（静态列表仅清空）??
	 */
	public static void clearAll(net.minecraft.server.MinecraftServer server) {
		// SERVER_STARTING 阶??getPlayerManager() 可能尚未初始化，必须做空值检?
		if (server != null) {
			net.minecraft.server.players.PlayerList pm = server.getPlayerList();
			if (pm != null) {
				for (ServerPlayer player : pm.getPlayers()) {
					if (CHARGING_PLAYERS.containsKey(player.getUUID())) {
                        clearPlayer(player);
					}
				}
			}
		}
		CHARGING_PLAYERS.clear();
	}

	// ==================== 内部数据??====================
	private static class ChargeState {
        long castId = -1;
		int brandStacks = 1;
		long startTick;
		float healthAtStart;
	}
}
