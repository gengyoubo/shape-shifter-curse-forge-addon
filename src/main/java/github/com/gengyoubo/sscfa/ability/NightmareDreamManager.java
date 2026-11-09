package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.spell.DomainManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 食梦魔（Nightmare）「入梦」核心状态机 —??服务端权威??
 *
 * <p>机制：食梦魔玩家对目标造成<b>累??10 ??/b>伤害后（诅咒之月共鸣：当夜降?6 点），目标进入「入梦」状态??
 * 入梦期间再次被该食梦魔攻???计数清零重??+ 噬梦吸血??5% 转化回血；目标死亡额外回 4 点）?
 * 入梦中的敌方对食梦魔本人施加的一?debuff（负面状态效果）全部无效——含定身（ssc_addon:stun）??
 * 主??被动高光描边（GlowMarker / 蛛网高亮 / 侵蚀烙印 / 契灵标记等带源施加点都会??
 * {@code NightmareDreamGuard.isBlocked} 閹凤附鍩呴敍澶堚偓?
 * 食梦魔本人看入梦目标??b>粉红色描??/b>（复?WebHighlight 客户端描边通道，仅本人可见）??/p>
 *
 * <p>数据结构：外??key = 食梦魔玩??UUID；内?key = 入梦目标 UUID??
 * 服务端每 tick（{@link #tick}）推进到期清理并广播；客户端只收同步包维护描边?
 * 死??/ 形态丢?/ 断????清理该玩家的全部入梦状态?/p>
 */
public final class NightmareDreamManager {

	// ==== 以下常量为默认值；运行时从 balance 快照读取（scope: abilities.nightmare_dream，数据包可覆盖）====
	/** 触发入梦所需累计伤害（每次入梦后清零重算）。默认；运行时??balance 快照读取??*/
	public static final float DREAM_THRESHOLD = 10.0f;
	/** 诅咒之月共鸣：诅咒之月当夜入梦阈值降?6 点（梦魇之力高涨）。默认；运行时从 balance 快照读取??*/
	public static final float DREAM_THRESHOLD_CURSED_MOON = 6.0f;
	/** 噬梦被动：对已入梦目标造成伤害的吸血比例（按面板伤害计）。默认；运行时??balance 快照读取??*/
	public static final float DREAM_LIFESTEAL_RATIO = 0.15f;
	/** 噬梦被动：入梦目标死亡时额外回复的生命值（2 颗心）。默认；运行时??balance 快照读取??*/
	public static final float DREAM_KILL_HEAL = 4.0f;

	/** 服务端权??balance 快照读取（快照未初始化时回退上方默认常量）??*/
	private static final BalanceReader BAL = new BalanceReader("abilities.nightmare_dream");

	/** 当前生效的入梦阈值：诅咒之月当夜（天??+ 诅咒之月日）降为 6，否?10。仅服务端调用?*/
	public static float currentDreamThreshold(ServerPlayer player) {
		if (net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isInCursedMoon(player.level())) {
			return (float) BAL.d("threshold_cursed_moon", DREAM_THRESHOLD_CURSED_MOON);
		}
		return (float) BAL.d("threshold", DREAM_THRESHOLD);
	}
	/** 入梦持续 tick??0 秒）。默认；运行时??balance 快照读取??*/
	public static final int DREAM_DURATION_TICKS = 400;
	/** 受击触发入梦的固定时长（tick??0 秒，不随受击刷新；恐惧重置时回??20 秒锁定）。默认；运行时??balance 快照读取??*/
	public static final int DREAM_FIXED_TICKS = 200;

	/** 梦境粒子发射间隔（tick??.75 秒一次，"偶尔?的节奏）。未登记 balance，保持常量??*/
	public static final int DREAM_PARTICLE_INTERVAL = 15;

	/** 入梦描边同步间隔（tick?? 秒一次；把真实剩余时长刷给食梦魔客户端，修复描边与实际入梦时长脱节）?*/
	public static final int DREAM_OUTLINE_SYNC_INTERVAL = 20;

	/** 粉红描边颜色（RGB，仅食梦魔本人可见）?*/
	public static final int DREAM_OUTLINE_COLOR = 0xFF6EC7;

	/** 玩??UUID -> (目??UUID -> 入梦到期世界时间)??*/
	private static final Map<UUID, Map<UUID, Long>> DREAMING = new ConcurrentHashMap<>();
	/** 玩??UUID -> (目??UUID -> 累计伤害)??*/
	private static final Map<UUID, Map<UUID, Float>> ACCUM = new ConcurrentHashMap<>();

	private NightmareDreamManager() {
	}

	/** 该玩家是否为食梦魔形态（服务端）?*/
	public static boolean isNightmare(ServerPlayer player) {
		return FormUtils.isForm(player, github.com.gengyoubo.sscfa.util.FormIdentifiers.WILD_CAT_NIGHTMARE);
	}

	/**
	 * 食梦魔对目标造成伤害后的累计入口（??LivingEntityMixin damage RETURN 调用，仅服务端）??
	 * 累计达阈????目标入梦（计?20s）；已入梦再被????刷新时长 + 计数清零重算?
	 */
	public static void onNightmareDealtDamage(ServerPlayer player, LivingEntity target, float amount) {
		if (amount <= 0) return;
		UUID pid = player.getUUID();
		UUID tid = target.getUUID();

		Map<UUID, Long> dreams = DREAMING.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
		Map<UUID, Float> acc0 = ACCUM.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
		long now = player.level().getGameTime();

		// 已入梦：噬梦吸血??5% 转化回血，按面板伤害计）+ 目标死亡额外?2 心；不再刷新时长
		// （用户定稿：受击入梦固??10 秒到期；恐惧仍可重置并锁??20 秒??
		Long until = dreams.get(tid);
		if (until != null && until > now) {
			acc0.remove(tid); // 计数清零重??
			player.heal(amount * (float) BAL.d("lifesteal_ratio", DREAM_LIFESTEAL_RATIO)); // 噬梦：汲取梦境之力回??
			if (!target.isAlive()) {
				player.heal((float) BAL.d("kill_heal", DREAM_KILL_HEAL)); // 梦尽人亡：额外吞噬残?
			}
			SscAddonNetworking.sendWebHighlight(player, target.getId(),
					(int) Math.max(20, until - now), DREAM_OUTLINE_COLOR); // 描边剩余时长同步（不延长?
			return;
		}

		// 未入梦：累计伤害，达阈值触发入梦（恐惧结束后的 20s 免疫窗口内不再触发??
		// 诅咒之月共鸣：当夜阈值降??6 点??
		Map<UUID, Float> acc = acc0;
		// 免疫真空期内伤害不累计（修复：免疫期一过下一击立即重新入梦，真空期形同虚设）
		if (NightmareFearManager.isDreamImmune(tid, now)) {
			acc.remove(tid); // 清掉免疫期前累积的计数，真空期结束后从零重新累计
			return;
		}
		float total = acc.merge(tid, amount, Float::sum);
		if (total >= currentDreamThreshold(player)
				&& !NightmareFearManager.isDreamImmune(tid, now)) {
			acc.remove(tid);
			// 受击触发入梦：固??10 秒（用户定稿；恐惧重置时才回??20 秒锁定??
			int fixedTicks = BAL.i("fixed_ticks", DREAM_FIXED_TICKS);
			dreams.put(tid, now + fixedTicks);
			SscAddonNetworking.sendWebHighlight(player, target.getId(), fixedTicks, DREAM_OUTLINE_COLOR);
			sendVeilToTarget(player, target, fixedTicks);
			// 入梦音效：低沉梦境钟声（全员可闻 null?? 梦魇低语
			net.minecraft.server.level.ServerLevel sw = (net.minecraft.server.level.ServerLevel) player.level();
			sw.playSound(null, target.getX(), target.getY(), target.getZ(),
					net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.6f);
			sw.playSound(null, target.getX(), target.getY(), target.getZ(),
					net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
			// 粒子：紫色梦境迷雾环绕目??
			sw.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
					target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ(), 16, 0.4, 0.4, 0.4, 0.02);
		}
	}

	/**
	 * 拦截判定（仅服务端）：source 实体（debuff 施加者）是否处于「??target 玩家入梦」状态?
	 * 即：施加者已入梦 ??受体是把它打入梦的食梦魔本??????debuff 无效?
	 */
	public static boolean isBlocked(LivingEntity source, LivingEntity target) {
		if (!(target instanceof ServerPlayer player)) return false;
		Map<UUID, Long> dreams = DREAMING.get(player.getUUID());
		if (dreams == null) return false;
		Long until = dreams.get(source.getUUID());
		return until != null && until > player.level().getGameTime();
	}

	/** 食梦魔玩家??tick 推进（由 SscAddonServerEvents 调用）：到期出梦清??+ 梦境粒子??*/
	public static void tick(ServerPlayer player) {
		Map<UUID, Long> dreams = DREAMING.get(player.getUUID());
		if (dreams == null || dreams.isEmpty()) return;
		if (!isNightmare(player)) {
			// 形态丢失：清全?
			clearAllFor(player);
			return;
		}
		net.minecraft.server.level.ServerLevel sw = (net.minecraft.server.level.ServerLevel) player.level();
		long now = sw.getGameTime();
		dreams.values().removeIf(until -> until <= now);
		// 梦境粒子：入梦目标身上偶尔冒粉色尘粒（与描边同色 0xFF6EC7）；目标本人不收????第一人称不可??
		if (now % DREAM_PARTICLE_INTERVAL == 0) {
			for (Map.Entry<UUID, Long> e : dreams.entrySet()) {
				if (e.getValue() <= now) continue;
				if (!(sw.getEntity(e.getKey()) instanceof LivingEntity target) || !target.isAlive()) continue;
				spawnDreamParticles(sw, target);
			}
		}
		// 描边周期同步（每 1s）：把每个入梦目标的「真实剩余时长」刷给食梦魔客户端?
		// 修复：首次入梦描边只发一?200t 包，若恐惧把入梦锁定 20s/戒指延长 20.25s??
		// 客户端描边会先于实际入梦结束而熄灭——周期刷新让描边与真实状态始终一致?
		if (now % DREAM_OUTLINE_SYNC_INTERVAL == 0) {
			for (Map.Entry<UUID, Long> e : dreams.entrySet()) {
				long remain = e.getValue() - now;
				if (remain <= 0) continue;
				if (!(sw.getEntity(e.getKey()) instanceof LivingEntity target) || !target.isAlive()) continue;
				SscAddonNetworking.sendWebHighlight(player, target.getId(), (int) remain, DREAM_OUTLINE_COLOR);
			}
		}
	}

	/** 入梦目标周围的粉色梦境粒子：逐观察者发包（排除目标本??+ 32 格距离剔除）?*/
	private static void spawnDreamParticles(net.minecraft.server.level.ServerLevel world, LivingEntity target) {
		net.minecraft.world.phys.Vec3 pos = target.position();
		// ??0xFF6EC7 ??(1.0, 0.4314, 0.7804)；偶?1~2 粒、缓慢上??
		net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket pkt = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
				new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0f, 0.4314f, 0.7804f), 1.0f),
				false,
				pos.x, pos.y + target.getBbHeight() * 0.6, pos.z,
				0.35f, 0.4f, 0.35f, 0.01f, 1);
		UUID targetUuid = target.getUUID();
		for (net.minecraft.server.level.ServerPlayer viewer : world.players()) {
			if (viewer.getUUID().equals(targetUuid)) continue; // 目标本人不可见（第一人称不冒粒子挡视线）
			if (viewer.distanceToSqr(target) > 32 * 32) continue;
			viewer.connection.send(pkt);
		}
	}

	/** 断??/ 死??/ 形态丢失：清理该玩家全部入梦状态（不动其他食梦魔的状态）?*/
	public static void clearAllFor(ServerPlayer player) {
		Map<UUID, Long> dreams = DREAMING.remove(player.getUUID());
		ACCUM.remove(player.getUUID());
		// 提前出梦：向所有仍在入梦的目标发关系结束包（粉色晕影消?+ 客户端移除入梦关系）；自然到期由客户端自行淡?
		if (dreams != null && player.getServer() != null) {
			for (UUID tid : dreams.keySet()) {
				if (player.getUUID().equals(tid)) continue;
				net.minecraft.server.level.ServerPlayer target =
						player.getServer().getPlayerList().getPlayer(tid);
				if (target != null) {
					SscAddonNetworking.sendDreamVeil(target, 0, player.getUUID());
				}
			}
		}
	}

	// ===== 客户端镜像（仅客户端使用；服务端永远为空??=====
	/** 客户端：「把我（本地玩家）入梦的食梦?UUID -> 到期客户端时间」。供透视拦截查询??*/
	private static final Map<UUID, Long> CLIENT_DREAMED_BY = new ConcurrentHashMap<>();

	/** 客户端收到晕影包时维护镜像（dreamVeilRenderer 调用）?*/
	public static void clientUpdateDreamedBy(UUID nightmareUuid, long endClientTime) {
		if (nightmareUuid == null) return;
		if (endClientTime <= 0) {
			CLIENT_DREAMED_BY.remove(nightmareUuid);
		} else {
			CLIENT_DREAMED_BY.put(nightmareUuid, endClientTime);
		}
	}

	/** 客户端查询：该实体（透视 power 持有者）是否正处于「把我入梦」状态（??EntityGlowPower 拦截）?
	 *  无世界时间上下文时退化为存在性判断?*/
	public static boolean clientIsDreamingMe(UUID holderUuid, long nowClientTime) {
		if (holderUuid == null) return false;
		Long end = CLIENT_DREAMED_BY.get(holderUuid);
		if (end == null) return false;
		return nowClientTime <= 0 || end > nowClientTime;
	}

	/** 客户端查询：本地玩家当前是否被任一食梦魔入梦（nowClientTime<=0 时不校验到期）?*/
	public static boolean clientHasAnyDream(long nowClientTime) {
		if (CLIENT_DREAMED_BY.isEmpty()) return false;
		if (nowClientTime <= 0) return true;
		return CLIENT_DREAMED_BY.values().stream().anyMatch(end -> end > nowClientTime);
	}

	/** 客户端断??换服清理镜像??*/
	public static void clientClear() {
		CLIENT_DREAMED_BY.clear();
	}

	/** 向入梦目标本人发粉色晕影包（仅玩家目标有屏幕；生物目标跳过）。包内携带食梦??UUID??
	 * 供客户端维护「被谁入梦」镜????拦截入梦者自己的 entity_glow 透视对食梦魔的描边?*/
	private static void sendVeilToTarget(ServerPlayer nightmare, LivingEntity target, int durationTicks) {
		if (target instanceof ServerPlayer targetPlayer) {
			SscAddonNetworking.sendDreamVeil(targetPlayer, durationTicks, nightmare.getUUID());
		}
	}

	/** 断线钩子??*/
	public static void onPlayerDisconnect(ServerPlayer player) {
		clearAllFor(player);
	}

	// ===== 「恐惧」技能支撑（?NightmareFearManager 调用，仅服务端） =====
	/** O(1) lookup for effects landing now; another nightmare's ledger does not qualify. */
	public static boolean isDreaming(ServerPlayer nightmare, LivingEntity target) {
		Map<UUID, Long> dreams = DREAMING.get(nightmare.getUUID());
		Long until = dreams == null ? null : dreams.get(target.getUUID());
		return until != null && until > target.level().getGameTime();
	}

	/** 收集某食梦魔当前已入梦的全部存活目标实体（跨实体查找，找不到的跳过）??*/
	public static List<LivingEntity> collectDreamTargets(ServerPlayer nightmare, long now) {
		List<LivingEntity> out = new ArrayList<>();
		Map<UUID, Long> dreams = DREAMING.get(nightmare.getUUID());
		if (dreams == null) return out;
		for (Map.Entry<UUID, Long> e : dreams.entrySet()) {
			if (e.getValue() <= now) continue;
			if (nightmare.getServer() == null) continue;
			net.minecraft.world.entity.Entity ent = null;
			for (net.minecraft.server.level.ServerLevel w : nightmare.getServer().getAllLevels()) {
				net.minecraft.world.entity.Entity found = w.getEntity(e.getKey());
				if (found != null) { ent = found; break; }
			}
			if (ent instanceof LivingEntity le && le.isAlive() && !DomainManager.blocksTargeting(nightmare, le)) out.add(le);
		}
		return out;
	}

	/** 把某目标在该食梦魔名下的入梦到期时间重置??now+400??0s）??*/
	public static void resetDream(UUID nightmareUuid, UUID targetUuid, long now) {
		Map<UUID, Long> dreams = DREAMING.get(nightmareUuid);
		if (dreams != null) dreams.put(targetUuid, now + BAL.i("duration_ticks", DREAM_DURATION_TICKS));
	}

	/** 恐惧期间持续锁定：把该目标的入梦到期时间钉??now+400（??tick 调用）?*/
	public static void lockDream(UUID nightmareUuid, UUID targetUuid, long now) {
		resetDream(nightmareUuid, targetUuid, now);
	}

	/** 强制唤醒目标：从<b>所??/b>食梦魔的入梦表中移除该目标，并熄灭其客户端晕??入梦镜像（恐惧结束用）??*/
	public static void forceWakeAll(UUID targetUuid, long now, net.minecraft.server.level.ServerLevel contextWorld) {
		List<UUID> wokeBy = new ArrayList<>();
		for (Map.Entry<UUID, Map<UUID, Long>> e : DREAMING.entrySet()) {
			if (e.getValue().remove(targetUuid) != null) wokeBy.add(e.getKey());
		}
		ACCUM.values().forEach(acc -> acc.remove(targetUuid));
		// 向目标本人逐梦魔发关系结束包（客户端镜像逐条移除、晕影无关系时熄灭??
		if (!wokeBy.isEmpty() && contextWorld != null) {
			if (contextWorld.getEntity(targetUuid) instanceof ServerPlayer targetPlayer) {
				for (UUID nightmareUuid : wokeBy) {
					SscAddonNetworking.sendDreamVeil(targetPlayer, 0, nightmareUuid);
				}
			}
			// 向每个曾入梦它的食梦魔发描边熄灭包（真??entityId + duration=0）：服务端已强制出梦?
			// 但描边客户端表只认到期时间——不发包食梦魔视角粉边会残留最??20s??
			// 看起来像「入梦没被强制退出」（多梦魔各自熄灭自己那条）??
			net.minecraft.world.entity.Entity targetEntity = contextWorld.getEntity(targetUuid);
			if (targetEntity != null) {
				int targetEntityId = targetEntity.getId();
				for (UUID nightmareUuid : wokeBy) {
					if (contextWorld.getEntity(nightmareUuid) instanceof ServerPlayer nightmarePlayer) {
						SscAddonNetworking.sendWebHighlight(nightmarePlayer, targetEntityId, 0, DREAM_OUTLINE_COLOR);
					}
				}
			}
		}
	}
}
