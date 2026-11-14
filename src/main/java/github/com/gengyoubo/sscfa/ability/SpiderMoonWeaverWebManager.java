package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.ShellProjectile;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.SpiderMoonWeaverPerks;
import github.com.gengyoubo.sscfa.init.AddonBlocks;
import github.com.gengyoubo.sscfa.state.RegSpiderMoonWeaverStateComponent;
import github.com.gengyoubo.sscfa.state.SpiderMoonWeaverStateComponent;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月织蛛「织网术」主技?- 服务端状态机??
 *
 * <p>单主技能键（sp_primary），客户端边沿检测发包驱动：
 * <ul>
 *   <li><b>潜行双击主??/b> ??{@link #toggleMode} 切??搭??/ 攻???模式（客户端手势判定，蓄力中禁止切换）??/li>
 *   <li><b>按住主键蓄????松??/b> ??{@link #start} / {@link #release} 发动当前模式??/li>
 *   <ul>
 *     <li>搭路模式：不潜?????发射附???{@link BridgeWebBullet} 蛛丝弹（命中方块建蛛丝梯，梯块可替代减速网膜???
 *         命中实体施缠绕）???
 *         潜????脚下平???{@link AddonWebBridgeAction} 蛛丝桥（同样可替代网膜）??/li>
 *     <li>攻击模式 ????{@link WebMembraneBullet}，命中后按档在半??3/5/6 格内贴面铺减速蛛网??/li>
 *   </ul>
 * </ul>
 * 蓄力?3 档??20t? ??tier1、??0t ??tier2、??0t ??tier3（??3 秒封顶）；边蓄力边??mana??
 * mana 不足自动释放???
 *
 * <p>模式存于独立 CCA 组??{@link SpiderMoonWeaverStateComponent}??=搭??/ 1=攻击，不???origin、跨会???跨形?死亡重生保留）；蓄力档位存??
 * 本类服务??map（瞬态）。CD ??power form_spider_moon_weaver_web ??JSON 字段配置??
 */
public final class SpiderMoonWeaverWebManager {

	private static final int MODE_BRIDGE = SpiderMoonWeaverStateComponent.MODE_BRIDGE;
	private static final int MODE_ATTACK = SpiderMoonWeaverStateComponent.MODE_ATTACK;

	// 以下已登记常量均为默认值；运行时???balance 快照读取（abilities.moon_weaver_web??
	private static final int MAX_TICKS = 60;        // 满档蓄???3 ??
	private static final int TIER1_TICKS = 20;      // 闁? 缂佸甯楁慨?tier1
	private static final int TIER2_TICKS = 40;      // 闁? 缂佸甯熺换?tier2
	// 释放???CD ??power JSON 配置（见类注释），??CD_TICKS_PER_TIER 常量已???
	private static final double START_MANA = 6.0;   // 起手??6 mana（沿用原版蜘蛛???
	private static final double MANA_PER_TICK = 0.25;
	private static final double PERK_CHARGE_TIME_MULTIPLIER = 0.7;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_web");
	/** 统一冷却服务的稳定技?ID（网络包触发型）??*/
	private static final String SKILL_ID = "my_addon:form_spider_moon_weaver_web";

	/** HUD 门槛同源：织网起手法力（balance 可调；HUD 展示用）??*/
	public static double startManaForHud() { return BAL.d("start_mana", START_MANA); }

	/** UUID -> {已蓄?tick 数}。服务端权威，多人一致???*/
	private static final Map<UUID, ChargeState> CHARGING = new ConcurrentHashMap<>();
	/** 平铺搭桥蓄力中的玩家（双击长??/ 潜行长按触发），release 时走脚下平铺而非蛛丝弹???*/
	private static final class ChargeState {
		int ticks;
		double paidMana;
		final long castId;
		final int cooldownTicks;
		final int mode, tier1Ticks, tier2Ticks, maxTicks;
		final double tier1Mana, tier2Mana, totalMana;

		ChargeState(ServerPlayer player) {
			castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
			cooldownTicks = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cooldown(player, SKILL_ID);
			mode = getMode(player);
			int originalMax = BAL.i("max_ticks", MAX_TICKS);
			int originalTier1 = Math.min(originalMax, BAL.i("tier1_ticks", TIER1_TICKS));
			int originalTier2 = Math.max(originalTier1, Math.min(originalMax, BAL.i("tier2_ticks", TIER2_TICKS)));
			double scale = SpiderMoonWeaverPerks.acceleratesCharge(player, mode)
					? BAL.d("perk_charge_time_multiplier", PERK_CHARGE_TIME_MULTIPLIER) : 1.0;
			tier1Ticks = Math.max(1, (int) Math.ceil(originalTier1 * scale));
			tier2Ticks = Math.max(1, (int) Math.ceil(originalTier2 * scale));
			maxTicks = Math.max(1, (int) Math.ceil(originalMax * scale));
			double rate = BAL.d("mana_per_tick", MANA_PER_TICK);
			tier1Mana = originalTier1 * rate;
			tier2Mana = originalTier2 * rate;
			totalMana = originalMax * rate;
		}

		/** 按本次蓄力进度结算累计费用，缩短时间不降低各档总耗法??*/
		double manaDue(int progress) {
			if (progress >= maxTicks) return totalMana;
			if (progress <= tier1Ticks) return tier1Mana * progress / tier1Ticks;
			if (progress <= tier2Ticks) return tier1Mana + (tier2Mana - tier1Mana) * (progress - tier1Ticks) / (tier2Ticks - tier1Ticks);
			return tier2Mana + (totalMana - tier2Mana) * (progress - tier2Ticks) / (maxTicks - tier2Ticks);
		}
	}
	private static final java.util.Set<UUID> FLAT_CHARGING = ConcurrentHashMap.newKeySet();

	private SpiderMoonWeaverWebManager() {}

	private static boolean isSpiderMoonWeaver(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER);
	}

	private static github.com.gengyoubo.sscfa.compat.ManaComponent mana(ServerPlayer player) {
		return github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
	}

	/** 读取玩家当前模式??=搭??/ 1=攻击）??*/
	private static int getMode(ServerPlayer player) {
		return RegSpiderMoonWeaverStateComponent.SPIDER_MOON_WEAVER_STATE.get(player).getMode();
	}

	/** 潜行双击主键：切??搭??/ 攻???模式（客户端手势判定，蓄力中禁止切换）??*/
	public static void toggleMode(ServerPlayer player) {
		if (!isSpiderMoonWeaver(player)) return;
		if (CHARGING.containsKey(player.getUUID())) return;
		int next = (getMode(player) == MODE_ATTACK) ? MODE_BRIDGE : MODE_ATTACK;
		RegSpiderMoonWeaverStateComponent.SPIDER_MOON_WEAVER_STATE.get(player).setMode(next);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8f, next == MODE_ATTACK ? 1.4f : 0.9f);
		player.displayClientMessage(Component.translatable(next == MODE_ATTACK
				? "message.my_addon.spider_moon_weaver.mode.attack"
				: "message.my_addon.spider_moon_weaver.mode.bridge"), true);
	}

	/** 主键按住开始蓄力（服务端重校??form / CD / mana）??*/
	public static void start(ServerPlayer player) {
		if (CHARGING.containsKey(player.getUUID())) return;
		if (!isSpiderMoonWeaver(player)) return;
		// 统一服务门??+ 建档（on_release：松键按档起算）
		if (!beginCharge(player)) return;
		CHARGING.put(player.getUUID(), new ChargeState(player));
		FLAT_CHARGING.remove(player.getUUID()); // 普通蓄?????蛛丝?
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.7f, 1.2f);
	}

	/** 主键按住开始「平铺搭桥」蓄力（双击长按 / 潜行长按触发；服务端重校??form / CD / mana）??*/
	public static void startFlat(ServerPlayer player) {
		if (CHARGING.containsKey(player.getUUID())) return;
		if (!isSpiderMoonWeaver(player)) return;
		if (!beginCharge(player)) return;
		CHARGING.put(player.getUUID(), new ChargeState(player));
		FLAT_CHARGING.add(player.getUUID()); // 平铺蓄?????脚下平???
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.7f, 1.0f);
	}

	/** 登记施放；power JSON cooldown = 满档? 档）CD，松手时按档位比例折算?*/
	private static boolean beginCharge(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return false;
		if (mana(player).getMana() < BAL.d("start_mana", START_MANA)) return false;
		return github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID);
	}

	/** 每服务???tick 对每个在线玩家调用（挂??SscAddonServerEvents 世??tick 循环）???*/
	public static void tick(ServerPlayer player) {
		ChargeState s = CHARGING.get(player.getUUID());
		if (s == null) return;
		if (player.isDeadOrDying() || !isSpiderMoonWeaver(player)) {
			cancel(player); // 死??/ 形态丢?????取消，不结??
			return;
		}
		if (s.ticks < s.maxTicks) {
			double due = s.manaDue(s.ticks + 1);
			double manaPerTick = Math.max(0.0, due - s.paidMana);
			github.com.gengyoubo.sscfa.compat.ManaComponent m = mana(player);
			if (m.getMana() + 1.0E-9 < manaPerTick) {
				release(player); // mana 耗????自动释放当前???
				return;
			}
			m.consumeMana(manaPerTick);
			s.paidMana = due;
			s.ticks++;
			ServerLevel sw = (ServerLevel) player.level();
			float chime = tierChimePitch(s);
			if (chime > 0f) {
				// 跨档瞬间：靠齐原??SSC 蓄力完成音效（note_block hat+snare 升调? cloud 粒??
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8f, chime);
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.8f, chime);
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
						10, 0.5, 0.5, 0.5, 0.0);
			} else if (s.ticks % 4 == 0) {
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
						4, 0.4, 0.5, 0.4, 0.0);
			}
		}
		// 达到满档后保??tier3、不再???mana，直到松键释???
	}

	/** 使用本次施放保存的阈值报音，默???0/40/60t、对应Perk??4/28/42t??*/
	private static float tierChimePitch(ChargeState state) {
		if (state.ticks == state.tier1Ticks) return 1.0f;
		if (state.ticks == state.tier2Ticks) return 1.19f;
		if (state.ticks == state.maxTicks) return 1.414f;
		return 0f;
	}

	/** 松开主???/ 自动释放：按当前??+ 模式 + 平铺标志发动（档???CD = tier×cd_per_tier，统一服务结算）???*/
	public static void release(ServerPlayer player) {
		ChargeState s = CHARGING.remove(player.getUUID());
		boolean flat = FLAT_CHARGING.remove(player.getUUID());
		if (s == null) return;
		if (!player.isAlive() || !isSpiderMoonWeaver(player)
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != s.castId
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.spec(player, SKILL_ID) == null) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
			return;
		}
		int ticks = s.ticks;
		int tier = ticks >= s.maxTicks ? 3 : (ticks >= s.tier2Ticks ? 2 : 1);
		completeCharge(player, s, tier);
		if (s.mode == MODE_ATTACK) {
			fireAttack(player, tier);
		} else if (flat) {
			fireBridgeFlat(player, tier); // 双击长按 / 潜行长?????脚下平???
		} else {
			fireBridgeShoot(player, tier); // 单击长按 ??发射蛛丝??
		}
	}

	private static void completeCharge(ServerPlayer player, ChargeState state, int tier) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, SKILL_ID, state.castId,
				(int) Math.ceil((double) state.cooldownTicks * tier / 3.0));
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, state.castId);
	}

	/** 取消蓄力（不结算、不?CD：统一服务失败???0 清记录）??*/
	public static void cancel(ServerPlayer player) {
		ChargeState s = CHARGING.remove(player.getUUID());
		FLAT_CHARGING.remove(player.getUUID());
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s == null ? -1 : s.castId);
	}

	/** 玩家掉线清理，防僵??UUID 残留??*/
	public static void onDisconnect(ServerPlayer player) {
        cancel(player);
	}

	// 攻击模式：发射蛛丝弹（命中铺减速网??
	private static void fireAttack(ServerPlayer player, int tier) {
		github.com.gengyoubo.sscfa.entity.WebMembraneBullet bullet = new github.com.gengyoubo.sscfa.entity.WebMembraneBullet(player, tier);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.6f, 1.0f);
		player.level().addFreshEntity(bullet);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.7f);
	}

	// 搭路模式-平铺：在玩家前方脚同高水平面铺蛛丝桥（平地也能在前方铺出）；桥块可替代减速网??
	private static void fireBridgeFlat(ServerPlayer player, int tier) {
		int length = tier >= 3 ? 18 : (tier >= 2 ? 14 : 10);
		BlockPos pos = player.blockPosition(); // 脚同高水平面（空气层），从前方第一格开始能铺??
		Direction dir = player.getDirection();
		AddonWebBridgeAction.BuildWebBridge(player.level(), pos, dir,
				new AddonWebBridgeAction.WebBridgeConfig(length, 0), AddonBlocks.TEMP_WEB_BRIDGE.get());
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 0.6f, 0.8f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(),
				40, 1.0, 0.5, 1.0, 0.0);
	}

	// 搭路模式-发射：发射附???BridgeWebBullet 蛛丝弹（命中方块走移植??BuildWebLadder 建蛛丝梯——梯块可替代网膜；命中实体施缠绕???
	private static void fireBridgeShoot(ServerPlayer player, int tier) {
		github.com.gengyoubo.sscfa.entity.BridgeWebBullet bullet = new github.com.gengyoubo.sscfa.entity.BridgeWebBullet(player, tier);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.0f, 1.0f); // 原??speed=2, divergence=1
		player.level().addFreshEntity(bullet);
	}
}
