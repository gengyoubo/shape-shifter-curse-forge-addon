package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.evolution.AxolotlTree;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进化美西螈「水流冲刺」（water_spurt 节点）—?服务端可靠实现，水陆两套触发彻底隔离?
 *
 * <p><b>水下冲刺</b>（同 SSC {@code form_axolotl_2_water_spurt}）：在水中按<b>疾跑?/b>（sprint 上升沿）?
 * 沿视线前冲，<b>不消耗湿润度</b>（与 SSC 一致），独立冷?5 秒?/p>
 * <p><b>陆地冲刺</b>：在陆地上疾跑时?b>潜行?/b>（sneak 上升沿且当前/上一 tick 处于疾跑）→
 * 同样的前冲位移，<b>消?{@link #LAND_MOISTURE_COST} 点湿润度(air)</b>，独立冷?5 秒?/p>
 * <p>?{@code isTouchingWater()} 区分?陆，两分支互斥（水里只走疾跑冲刺、不响应潜行；陆地只走潜行冲刺?
 * 不响应疾跑），且各自独立冷却，互不干扰。已解锁 water_spurt 节点才生效?/p>
 */
public final class AxolotlWaterSpurtHandler {

	/** 陆地冲刺的服务端扣费和客户端资源门槛共用同一数值?*/
	public static int landAirCostForHud(net.minecraft.world.entity.player.Player player) {
		return github.com.gengyoubo.sscfa.compat.UpgradeAxolotlPerks.landDashAirCost(player, BAL.i("land_moisture_cost", LAND_MOISTURE_COST));
	}

	// 以下均为默认值；运行时从 balance 快照读取（abilities.axolotl_water_spurt?
	private static final int CD_TICKS = 100;   // 5 绉?
	private static final ResourceLocation WATER_HUD = new ResourceLocation("my_addon", "form_upgrade_axolotl_dash_hud_water");
	private static final ResourceLocation LAND_HUD = new ResourceLocation("my_addon", "form_upgrade_axolotl_dash_hud_land");
	private static final double BURST = 1.6;    // 前冲力度（水陆一致）
	/** 陆地冲刺消耗的湿润度（air 值；参照 WaterSpearLeapManager ?18=6%，冲刺更轻，?12?%）。水下冲刺同 SSC 免费?*/
	private static final int LAND_MOISTURE_COST = 12;

	/** balance 快照读取（快照未初始化回退默认常量?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.axolotl_water_spurt");

	/** 客户端上报的「真正疾跑键」按住状态（区分双击 W / 游泳自动疾跑，避免误触发冲刺）?*/
	private static final Map<UUID, Boolean> CLIENT_SPRINT = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> WAS_CLIENT_SPRINT = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> WAS_SNEAKING = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> WATER_CD = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> LAND_CD = new ConcurrentHashMap<>();

	/** 客户端发「疾跑键按住状态」包时调用（仅进化美西蟠本机会发；区分真正疾跑键与双?W/游泳自动疾跑）?*/
	public static void setClientSprintHeld(ServerPlayer player, boolean held) {
		CLIENT_SPRINT.put(player.getUUID(), held);
	}

	private AxolotlWaterSpurtHandler() {
	}

	/** 每服务端 tick 对每个在线玩家调用?*/
	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		int wcd = WATER_CD.getOrDefault(id, 0);
		if (wcd > 0) WATER_CD.put(id, wcd - 1);
		int lcd = LAND_CD.getOrDefault(id, 0);
		if (lcd > 0) LAND_CD.put(id, lcd - 1);

		if (!FormUtils.isUpgradeAxolotl(player)) {
			CLIENT_SPRINT.remove(id);
			WAS_CLIENT_SPRINT.remove(id);
			WAS_SNEAKING.remove(id);
			return;
		}

		// 用客户端上报的「真正疾跑键」状态，而非服务?isSprinting()（后者会被双?W / 游泳自动置真 ?误冲?
		boolean sprintKey = CLIENT_SPRINT.getOrDefault(id, false);
		// HUD 资源仅在 CD 活跃期同步（空闲稳?wcd==lcd==0 零资源读写）：CD 归零后补发一?0?
		// 之后不再触碰资源；触发冲刺时在分支内直接?HUD（值必变，无需比对）?
		// 归零补发若因极端时序被应用层丢弃（换维度/传送瞬间实体未追踪），空闲期每 100t 低频对账
		// 一次自愈（服务端本地读一次比对，值不符才重发——正常态零额外包，异常态最多残?5s）?
		boolean waterCooling = wcd > 0;
		boolean landCooling = lcd > 0;
		boolean idleAudit = !waterCooling && !landCooling && wcd != 1 && lcd != 1 && player.tickCount % 100 == 0;
		if (waterCooling || wcd == 1 || (idleAudit
				&& PowerUtils.getResourceValue(player, WATER_HUD) != 0)) {
			// ?tick 恰好 -1 ?CD 倒计时：递减期走批量预测通道，归零帧/对账走立即同步（2026-09-24?
			if (waterCooling) PowerUtils.countDownAndSync(player, WATER_HUD, Math.max(0, wcd - 1));
			else PowerUtils.setResourceValueAndSync(player, WATER_HUD, Math.max(0, wcd - 1));
		}
		if (landCooling || lcd == 1 || (idleAudit
				&& PowerUtils.getResourceValue(player, LAND_HUD) != 0)) {
			if (landCooling) PowerUtils.countDownAndSync(player, LAND_HUD, Math.max(0, lcd - 1));
			else PowerUtils.setResourceValueAndSync(player, LAND_HUD, Math.max(0, lcd - 1));
		}
		boolean sneaking = player.isShiftKeyDown();
		boolean wasSprintKey = WAS_CLIENT_SPRINT.getOrDefault(id, false);
		boolean wasSneaking = WAS_SNEAKING.getOrDefault(id, false);
		WAS_CLIENT_SPRINT.put(id, sprintKey);
		WAS_SNEAKING.put(id, sneaking);

		// 未解?water_spurt 节点：只更新状态、不触发任何冲刺
		if (!RegEvolutionComponent.EVOLUTION.get(player).isUnlocked(AxolotlTree.NODE_WATER_SPURT)) {
			return;
		}

		boolean inWater = player.isInWater();
		int cdTicks = BAL.i("cd_ticks", CD_TICKS);

		if (inWater) {
			// ===== 水下冲刺：水?+ 按下真正疾跑键（上升沿）?前冲，免费（双击 W/游泳自动疾跑不触发）=====
			if (sprintKey && !wasSprintKey && WATER_CD.getOrDefault(id, 0) <= 0) {
				doDash(player);
				WATER_CD.put(id, cdTicks);
				PowerUtils.setResourceValueAndSync(player, WATER_HUD, cdTicks);
			}
			// 水里只响应疾跑键分支，隔离陆地潜行逻辑
			return;
		}

		// ===== 陆地冲刺：陆?+ 按住真正疾跑键时按潜行（sneak 上升沿）?前冲，消耗湿润度 =====
		if (sneaking && !wasSneaking && sprintKey && LAND_CD.getOrDefault(id, 0) <= 0) {
			int landCost = landAirCostForHud(player);
			if (player.getAirSupply() < landCost) {
				return; // 湿润度不足，不触?
			}
			player.setAirSupply(player.getAirSupply() - landCost);
			doDash(player);
			LAND_CD.put(id, cdTicks);
			PowerUtils.setResourceValueAndSync(player, LAND_HUD, cdTicks);
		}
	}

	/** 沿视线前冲一次（水陆一致的位移与视听反馈）?*/
	private static void doDash(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		double burst = BAL.d("burst", BURST);
		player.push(look.x * burst, look.y * burst, look.z * burst);
		player.hurtMarked = true;

		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.2f, 1.0f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1.3f, 0.7f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 0.6, player.getZ(),
				60, 0.6, 0.4, 0.6, 0.6);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE, player.getX(), player.getY() + 0.5, player.getZ(),
				40, 0.5, 0.4, 0.5, 0.5);
		github.com.gengyoubo.sscfa.util.ParticleUtils.spawnWaterBurst(
				sw, player, player.getX(), player.getY() + 0.6, player.getZ(), 1.2);
	}

	public static void onPlayerDisconnect(UUID id) {
		CLIENT_SPRINT.remove(id);
		WAS_CLIENT_SPRINT.remove(id);
		WAS_SNEAKING.remove(id);
		WATER_CD.remove(id);
		LAND_CD.remove(id);
	}
}
