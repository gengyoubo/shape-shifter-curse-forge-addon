package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.UpgradeAxolotlPerks;
import github.com.gengyoubo.sscfa.entity.ShellProjectile;
import github.com.gengyoubo.sscfa.evolution.AxolotlTree;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进化美西螈「投掷水矛」主技能（sp_primary）—?服务端状态机?
 *
 * <p>按主技能键?b>不往背包放任何物?/b>，仅同步「蓄力中」状态给客户??
 * 由渲?mixin 纯渲染在手上画出 3D 水矛模型 + 举矛过肩姿势（{@code getArmPose}/第一人称矩阵 mixin?
 * ?{@code CustomModelData=1} 恒定 3D，不依赖 vanilla「正在使用物品」状态、不动玩家背包）?
 * 同时冲量斜后上跃?b>无重力悬?/b>；蓄?1.35 秒后朝准星投出直线水矛?
 * 直击 12 物理 + 2 格半?5 ?AOE，?6% 湿润度（18 air），CD 8 秒?/p>
 *
 * <p>零背包污染：全程不生?替换任何物品，投?/ 取消 / 死亡 / 形态丢?/ 断线 只需恢复重力 + 结束渲染?/p>
 */
public final class WaterSpearLeapManager {

	// 以下均为默认值；运行时从 balance 快照读取（abilities.water_spear_leap?
	private static final int CHARGE_TICKS = 27;    // 蓄力 ~1.35 秒后投矛
	// CD? 秒）由统一冷却服务 + power JSON 配置管理，原 CD_TICKS 常量已删
	private static final int AIR_COST = 18;        // 6% 婀挎鼎搴?
	private static final double LEAP_BACK = 0.80;  // 起跃向后冲量（更斜后?
	private static final double LEAP_UP = 0.62;    // 起跃向上冲量（跳更高?
	private static final double DECAY = 0.80;       // ?tick 速度衰减（缓入缓?+ 收束到悬浮）

	/** balance 快照读取（快照未初始化回退默认常量?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.water_spear_leap");
	/** 统一冷却服务的稳定技?ID（网络包触发型）?*/
	private static final String SKILL_ID = "my_addon:form_upgrade_axolotl_water_spear";

	/** HUD 门槛同源：水矛空气消耗（balance 可调；HUD 展示用）?*/
	public static int airCostForHud() { return BAL.i("air_cost", AIR_COST); }
	public static int airCostForHud(Player player) { return UpgradeAxolotlPerks.spearAirCost(player, airCostForHud()); }

	private static final Map<UUID, LeapState> STATES = new ConcurrentHashMap<>();

	private static final class LeapState {
        long castId = -1;
		int tick = 0;
	}

	private WaterSpearLeapManager() {
	}

	/** 客户端按主技能键（无 payload）。服务端校验、换水矛并起跃悬浮?*/
	public static void onKeyPress(ServerPlayer player) {
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return;
		if (STATES.containsKey(player.getUUID())) return; // 施法中不可重?
		if (!FormUtils.isUpgradeAxolotl(player)) return;
		if (!RegEvolutionComponent.EVOLUTION.get(player).isUnlocked(AxolotlTree.NODE_WATER_SPEAR)) return;
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return; // CD 涓?
		int airCost = airCostForHud(player);
		if (player.getAirSupply() < airCost) return; // 湿润度不?
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return;

		player.setAirSupply(player.getAirSupply() - airCost);

		LeapState s = new LeapState();
        s.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		STATES.put(player.getUUID(), s);

		// 同步「蓄力中」→ 客户端纯渲染：手上渲?3D 水矛模型（不往背包放任何物品）+ 举矛过肩姿势
		SscAddonNetworking.syncSpearChargeState(player, true);

		// 无重?+ 一次性冲量斜后上跃（视线反方?+ 上），随后衰减到空中悬浮
		PlayerGravityLease.acquire(player, SKILL_ID);
		Vec3 look = player.getLookAngle();
		double leapBack = BAL.d("leap_back", LEAP_BACK);
		double leapUp = BAL.d("leap_up", LEAP_UP);
		Vec3 back = new Vec3(-look.x, 0, -look.z);
		if (back.lengthSqr() < 1.0e-4) back = new Vec3(0, 0, -1);
		back = back.normalize();
		player.setDeltaMovement(back.x * leapBack, leapUp, back.z * leapBack);
		player.hurtMarked = true;
		player.fallDistance = 0.0f;

		ServerLevel sw = (ServerLevel) player.level();
		// 起手蓄力音效：海晶核激活（魔法水涌，音量拉大做明显? 高速水?+ 美西螈入水（全员可闻 null?
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2.0f, 1.2f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0f, 1.1f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1.2f, 0.8f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 0.2, player.getZ(),
				30, 0.4, 0.2, 0.4, 0.3);
	}

	/** 每服务端 tick 对每个在线玩家调用?*/
	public static void tick(ServerPlayer player) {
		LeapState s = STATES.get(player.getUUID());
		if (s == null) return;
		if (player.isDeadOrDying() || !FormUtils.isUpgradeAxolotl(player)) {
			cancel(player); // 死亡 / 形态丢??取消并归还物品、恢复重?
			return;
		}
		s.tick++;
		ServerLevel sw = (ServerLevel) player.level();
		int chargeTicks = BAL.i("charge_ticks", CHARGE_TICKS);
		double decay = BAL.d("decay", DECAY);

		if (s.tick < chargeTicks) {
			// 无重力下速度衰减：起跃冲量平滑收束到 0 ?跃起后悬浮在空中（不落）
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * decay, v.y * decay, v.z * decay);
			player.hurtMarked = true;
			player.fallDistance = 0.0f;
			if (s.tick % 4 == 0) {
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.FALLING_WATER, player.getX(), player.getY() + 1.0, player.getZ(),
						6, 0.4, 0.4, 0.4, 0.0);
			}
			// 蓄力音效：每 5 tick 一声上升气泡（音调随蓄力进?0.8?.7，营造能量聚集感?
			if (s.tick % 5 == 0) {
				float progress = (float) s.tick / (float) chargeTicks;
				float pitch = 0.8f + progress * 0.9f;
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.7f, pitch);
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.35f, pitch);
			}
			// 蓄满前瞬间（最?3 tick）：海晶核短鸣提示「即将投出?
			if (s.tick == chargeTicks - 3) {
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, 0.9f, 1.4f);
			}
		} else {
			// 投矛：恢复重力、朝准星发射直线水矛
			throwSpear(player, s);
			finish(player);
		}
	}

	/** 朝准星方向投出直线水矛?*/
	private static void throwSpear(ServerPlayer player, LeapState s) {
		ServerLevel sw = (ServerLevel) player.level();
		Vec3 dir = player.getLookAngle().normalize();
		ShellProjectile spear = new ShellProjectile(github.com.gengyoubo.sscfa.init.AddonEntities.THROWN_WATER_SPEAR.get(), sw); spear.setOwner(player);
		spear.setPos(player.getX() + dir.x * 0.6, player.getEyeY() - 0.1 + dir.y * 0.6, player.getZ() + dir.z * 0.6);
		spear.shoot(dir.x, dir.y, dir.z, 1.5f, 0.0f);
		sw.addFreshEntity(spear);
		// ?swingHand：与 vanilla 水矛（三叉戟）投掷一致——矛从举矛蓄力姿势直接飞出、手臂落回正常，
		// 而非普通攻击挥手（TridentItem.onStoppedUsing 同样不挥手）?
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.2f, 1.1f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 0.9f);
	}

	/** 投矛完成：恢复重力、结束蓄力渲染、进?CD（统一服务 on_release 结算）并清理状态?*/
	private static void finish(ServerPlayer player) {
        LeapState s = STATES.remove(player.getUUID());
        if (s == null) return;
		PlayerGravityLease.release(player, SKILL_ID);
		SscAddonNetworking.syncSpearChargeState(player, false);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, s.castId);
	}

	/** 取消（不?CD、不投矛）：恢复重力、结束蓄力渲染?*/
	public static void cancel(ServerPlayer player) {
		LeapState s = STATES.remove(player.getUUID());
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s == null ? -1 : s.castId);
		if (s != null) {
			PlayerGravityLease.release(player, SKILL_ID);
			SscAddonNetworking.syncSpearChargeState(player, false);
		}
	}

	/** 该玩家（?UUID）是否处于投掷水矛蓄力中（服务端权威，用于蓄力期禁用右键交互）?*/
	public static boolean isCharging(java.util.UUID id) {
		return STATES.containsKey(id);
	}

	/** 断线：恢复重力并清理?*/
	public static void onPlayerDisconnect(ServerPlayer player) {
        cancel(player);
	}
}
