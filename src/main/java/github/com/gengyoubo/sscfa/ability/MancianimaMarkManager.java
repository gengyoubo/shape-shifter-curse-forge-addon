package github.com.gengyoubo.sscfa.ability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.spell.UniversalFormationManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 契灵主要技??- 标记状态机（服务端权威）?
 *
 * 颜色等级：YELLOW??24 格）?ORANGE（??4 格）?RED（已升级）??
 * 距离变化??RED→YELLOW，??expireTick 继承不变?
 *
 * 同一时间每个契灵玩家只能持??1 个标记；上一个目标的副作用立刻清除??
 *
 * 颜色高亮：通??S2C 同步?+ my_addon:mancianima_mark_color 条???+ 三??entity_glow power 实现?
 * 仅契灵本人客户端可见，不污染原版 GLOWING / scoreboard team??
 *
 * 红色标记锁??15 秒：每个被红色标记过的目标??5 秒内不能被同一玩家再次红色标记???
 */
public final class MancianimaMarkManager {

	private MancianimaMarkManager() {}

	public enum MarkColor { YELLOW, ORANGE, RED }

	public static final int MARK_DURATION_TICKS = 300;        // 15s
	public static final int RANGE_ORANGE = 24;
	public static final int RANGE_RED_KEEP = 24;
	public static final int RED_RELOCK_COOLDOWN_TICKS = 300;  // 15s 内不能对同一目标再红???
	/** 阶段升级冷却：黄标→升红、红标→引爆都至少需要等??3s */
	public static final int STAGE_GATE_TICKS = 60;            // 3s

	/** 阶段升级冷却（运行时快照读取，供 MancianimaPrimary 等服务端类共用；HUD 客户端仍读常量） */
	public static int stageGateTicks() {
		return BAL.i("stage_gate_ticks", STAGE_GATE_TICKS);
	}

	/** S2C 包：将本地玩家持有的标记同步到客户端，用?entity_glow */
	public static final ResourceLocation PACKET_MARK_SYNC = new ResourceLocation("ssc_addon", "mancianima_mark_sync");

	public static final class Mark {
		public final UUID targetUuid;
		public MarkColor color;
		public long expireTick;
		/** 当前阶段开??tick：黄?首次标???tick；红???升??tick。用于阶段冷却判定??*/
		public long colorSetTick;
		public Mark(UUID t, MarkColor c, long e, long set) {
			targetUuid = t; color = c; expireTick = e; colorSetTick = set;
		}
	}

	/** marker player UUID -> Mark */
	private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();
	/** ??(markerUuid + targetUuid) 的红标重锁冷却结??tick */
	private static final Map<String, Long> RED_LOCKOUT = new ConcurrentHashMap<>();
	/** 当前正在引导段：marker UUID -> ChannelState */
	public static final Map<UUID, ChannelState> CHANNELING = new ConcurrentHashMap<>();
	/** 待同步玩家集?*/
	private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

	/** 契灵玩家最后一次战??tick（受击或主动伤敌??*/
	private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();
	/** 上次抗伤回复???tick */
	private static final Map<UUID, Long> LAST_REGEN = new ConcurrentHashMap<>();
	/** 上次脱???mana 回复?tick（进化使魔专用） */
	private static final Map<UUID, Long> LAST_MANA_REGEN = new ConcurrentHashMap<>();
	/** 非战斗判定阈值???s */
	public static final int OUT_OF_COMBAT_TICKS = 100;
	/** 抗伤回复间隔??5s */
	public static final int RESIST_REGEN_INTERVAL_TICKS = 300;
	/** 进化使魔脱??mana 回复间隔??s（脱战后每秒???点??*/
	public static final int UPGRADE_FOX_MANA_REGEN_INTERVAL_TICKS = 20;
	/** 进化使魔脱战每次回复???mana ??*/
	public static final double UPGRADE_FOX_MANA_REGEN_AMOUNT = 1.0;
	/** 进化使魔 mana 回复暂停计时器资??id（常量复用，避免??tick 现场构??Identifier??*/
	private static final ResourceLocation UPGRADE_FOX_MANA_REGEN_PAUSE_TIMER =
			new ResourceLocation("my_addon", "form_upgrade_familiar_fox_mana_regen_pause_pause_timer");

	// 阶??5：运行时快照读取（abilities.mancianima_mark；快照未初始化回退默认常量???
	private static final BalanceReader BAL = new BalanceReader("abilities.mancianima_mark");

	/** 标记战斗发生（伤害进出契灵玩家时调用??*/
	public static void markCombat(UUID playerUuid, long now) {
		if (playerUuid != null) LAST_COMBAT.put(playerUuid, now);
	}

	public static final class ChannelState {
		public final UUID targetUuid;
		public final long endTick;
		/** 1=主要技能真伤??2s)??=次要技能瞬移斩???1s) */
		public final int type;
        public long castId = -1;
		public ChannelState(UUID target, long endTick, int type) {
			this.targetUuid = target; this.endTick = endTick; this.type = type;
		}
	}

	// ============== 婵炲鍔岄崬?==============
	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerStopping(server -> {
			MARKS.clear(); RED_LOCKOUT.clear(); CHANNELING.clear(); DIRTY.clear();
			LAST_COMBAT.clear(); LAST_REGEN.clear(); LAST_MANA_REGEN.clear();
		});
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(MancianimaMarkManager::onTick);
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			clearMark(server, id);
			CHANNELING.remove(id);
			DIRTY.remove(id);
			LAST_COMBAT.remove(id);
			LAST_REGEN.remove(id);
			LAST_MANA_REGEN.remove(id);
		});
	}

	// ============== Public API ==============

	public static Mark getMark(UUID markerUuid) { return MARKS.get(markerUuid); }

	public static boolean canRedRelock(UUID markerUuid, UUID targetUuid, long now) {
		Long end = RED_LOCKOUT.get(markerUuid + ":" + targetUuid);
		return end == null || now >= end;
	}

	public static String getColorString(UUID markerUuid, UUID targetUuid) {
		Mark m = MARKS.get(markerUuid);
		if (m == null || !m.targetUuid.equals(targetUuid)) return null;
		return colorString(m.color);
	}

	private static String colorString(MarkColor c) {
		return switch (c) { case YELLOW -> "yellow"; case ORANGE -> "orange"; case RED -> "red"; };
	}

	/** 设置或替换标记（YELLOW 起始）。如果替换会先清理上一个目标的副作用?*/
	public static void setMark(ServerPlayer marker, LivingEntity target, MarkColor color) {
		UUID markerId = marker.getUUID();
		Mark old = MARKS.get(markerId);
		if (old != null && (target == null || !old.targetUuid.equals(target.getUUID()))) {
			clearTargetSideEffects(marker.getServer(), old.targetUuid);
		}
		if (target == null) { MARKS.remove(markerId); DIRTY.add(markerId); return; }
		long now = ((ServerLevel) marker.level()).getGameTime();
		long expire = (old != null && old.targetUuid.equals(target.getUUID())) ? old.expireTick : (now + BAL.i("mark_duration_ticks", MARK_DURATION_TICKS));
		// 全??YELLOW 标????重置?marker+target 的红标重??CD（允许立刻升红）
		boolean isFreshYellow = color == MarkColor.YELLOW
				&& (old == null || !old.targetUuid.equals(target.getUUID()));
		if (isFreshYellow) {
			RED_LOCKOUT.remove(markerId + ":" + target.getUUID());
		}
		// 全新标记（不同目标）??colorSetTick ??now；同目标延????保留旧???colorSetTick
		long setTick = (old != null && old.targetUuid.equals(target.getUUID())) ? old.colorSetTick : now;
		MARKS.put(markerId, new Mark(target.getUUID(), color, expire, setTick));
		DIRTY.add(markerId);
	}

	/** 升级到红色（必须当前??ORANGE、过黄???3s 阶段冷却且未在锁定期内）??*/
	public static boolean upgradeToRed(ServerPlayer marker, LivingEntity target) {
		Mark m = MARKS.get(marker.getUUID());
		if (m == null || !m.targetUuid.equals(target.getUUID())) return false;
		if (m.color != MarkColor.ORANGE) return false;
		long now = ((ServerLevel) marker.level()).getGameTime();
		if (now - m.colorSetTick < BAL.i("stage_gate_ticks", STAGE_GATE_TICKS)) return false;
		if (!canRedRelock(marker.getUUID(), target.getUUID(), now)) return false;
		m.color = MarkColor.RED;
		m.expireTick = now + BAL.i("mark_duration_ticks", MARK_DURATION_TICKS);
		m.colorSetTick = now; // 重置阶段计时，红标→引爆需再等 3s
		RED_LOCKOUT.put(marker.getUUID() + ":" + target.getUUID(), now + BAL.i("red_relock_cooldown_ticks", RED_RELOCK_COOLDOWN_TICKS));
		DIRTY.add(marker.getUUID());
		return true;
	}

	public static void clearMark(MinecraftServer server, UUID markerUuid) {
		Mark m = MARKS.remove(markerUuid);
		if (m != null) clearTargetSideEffects(server, m.targetUuid);
		DIRTY.add(markerUuid);
	}

	public static boolean isRedMarkedBy(UUID markerUuid, UUID targetUuid) {
		Mark m = MARKS.get(markerUuid);
		return m != null && m.color == MarkColor.RED && m.targetUuid.equals(targetUuid);
	}

	public static UUID getMarkerOf(UUID targetUuid) {
		for (Map.Entry<UUID, Mark> e : MARKS.entrySet()) {
			if (e.getValue().targetUuid.equals(targetUuid)) return e.getKey();
		}
		return null;
	}

	// ============== Tick ==============
	private static void onTick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		RED_LOCKOUT.entrySet().removeIf(e -> e.getValue() <= now);

		Iterator<Map.Entry<UUID, Mark>> it = MARKS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Mark> e = it.next();
			UUID markerId = e.getKey();
			Mark m = e.getValue();
			ServerPlayer marker = server.getPlayerList().getPlayer(markerId);
			if (marker == null) { removeAndCleanup(server, it, m, markerId); continue; }
			net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(marker);
			if (form == null || !FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(form.id())) {
				removeAndCleanup(server, it, m, markerId); continue;
			}
			if (now >= m.expireTick) { removeAndCleanup(server, it, m, markerId); continue; }
			Entity tgt = findEntity(server, m.targetUuid);
			if (!(tgt instanceof LivingEntity living) || !living.isAlive()) { removeAndCleanup(server, it, m, markerId); continue; }

			double dist = marker.distanceTo(living);
			MarkColor newColor = m.color;
			int rangeOrange = BAL.i("range_orange", RANGE_ORANGE);
			if (m.color == MarkColor.YELLOW) {
				if (dist <= rangeOrange) newColor = MarkColor.ORANGE;
			} else if (m.color == MarkColor.ORANGE) {
				if (dist > rangeOrange) newColor = MarkColor.YELLOW;
			} else { // RED
				if (dist > BAL.i("range_red_keep", RANGE_RED_KEEP)) newColor = MarkColor.YELLOW;
			}
			if (newColor != m.color) {
				m.color = newColor;
				DIRTY.add(markerId);
				if (newColor == MarkColor.YELLOW && living instanceof ServerPlayer sp) {
					playSoundToPlayer(sp, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
				}
			}
			// 持续辅助效果：???SLOWNESS（颜色高亮??entity_glow 提供）；带标记?source 供入梦拦截归???
			if (now % 20 == 0) {
				github.com.gengyoubo.sscfa.spell.DomainManager.runAttachedEffect(marker, living,
						() -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false, false), marker));
			}
		}

		// 引导?tick
		Iterator<Map.Entry<UUID, ChannelState>> cit = CHANNELING.entrySet().iterator();
		while (cit.hasNext()) {
			Map.Entry<UUID, ChannelState> e = cit.next();
			UUID markerId = e.getKey();
			ChannelState cs = e.getValue();
			ServerPlayer marker = server.getPlayerList().getPlayer(markerId);
			if (marker == null) { cit.remove(); continue; }
			Mark m = MARKS.get(markerId);
			boolean valid = m != null && m.color == MarkColor.RED && m.targetUuid.equals(cs.targetUuid);
			Entity tgt = findEntity(server, cs.targetUuid);
			boolean targetAlive = tgt instanceof LivingEntity le && le.isAlive();
			if (!valid || !targetAlive || github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(marker, tgt)) {
				cit.remove();
				marker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ssc_addon.mancianima.channel_fail"), true);
				// 引导中???= 联动失败：次技能引导（type=2）进???3.5s 失??CD；主技能引导（type=1）维持原行为不???CD
				if (cs.type == 2) {
					github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(marker, MancianimaTeleport.SKILL_ID, cs.castId);
				}
				continue;
			}
			marker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false, false));
			// 蓄力期间第三人称特效：marker 周??+ target 周??+ 中间路???
			ServerLevel sw = (ServerLevel) marker.level();
			LivingEntity le = (LivingEntity) tgt;
			double mx = marker.getX(), my = marker.getY() + marker.getEyeHeight() * 0.6, mz = marker.getZ();
			double tx = le.getX(), ty = le.getY() + le.getBbHeight() * 0.5, tz = le.getZ();
			// marker 周围旋转粒子环（紫色魅力??
			for (int i = 0; i < 4; i++) {
				double angle = (now * 0.3 + i * Math.PI / 2.0);
				double rx = mx + Math.cos(angle) * 0.8;
				double rz = mz + Math.sin(angle) * 0.8;
				sw.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, rx, my, rz, 1, 0, 0, 0, 0);
			}
			// target 周围旋转粒子环（红色危险??
			for (int i = 0; i < 4; i++) {
				double angle = (-now * 0.3 + i * Math.PI / 2.0);
				double rx = tx + Math.cos(angle) * 1.0;
				double rz = tz + Math.sin(angle) * 1.0;
				sw.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, rx, ty, rz, 1, 0, 0, 0, 0);
			}
			// marker ??target 路径上???4 tick 撒一颗灵魂火??
			if (now % 4 == 0) {
				int steps = 6;
				for (int s = 1; s < steps; s++) {
					double t = s / (double) steps;
					double px = mx + (tx - mx) * t;
					double py = my + (ty - my) * t;
					double pz = mz + (tz - mz) * t;
					sw.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
				}
			}
			if (now >= cs.endTick) {
				cit.remove();
				if (cs.type == 1) {
					MancianimaPrimary.executeChannelComplete(marker, (LivingEntity) tgt);
				} else if (cs.type == 2) {
					MancianimaTeleport.executeRedMarkChannelComplete(marker, (LivingEntity) tgt, cs.castId);
				}
			}
		}

		// 同步脏数??
		if (!DIRTY.isEmpty()) {
			Set<UUID> snapshot = new HashSet<>(DIRTY);
			DIRTY.clear();
			for (UUID id : snapshot) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null) sendSyncPacket(p);
			}
		}

		// 契灵抗伤回??/ 进化使魔脱??mana 回复：两形态互斥，合并为一次全玩家遍历（??tick 每玩家少一???getCurrentForm??
		for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(sp);
			if (form == null) continue;
			net.minecraft.resources.ResourceLocation formId = form.id();
			if (FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(formId)) {
				// 抗伤回复???5s 内未受到任何伤害后，??15s ??1 抗伤（主动攻击敌人不打断计时??
				UUID id = sp.getUUID();
				long lastCombat = LAST_COMBAT.getOrDefault(id, 0L);
				long resistRegenInterval = BAL.i("resist_regen_interval_ticks", RESIST_REGEN_INTERVAL_TICKS);
				if (now - lastCombat < resistRegenInterval) continue;
				long lastRegen = LAST_REGEN.getOrDefault(id, 0L);
				if (now - lastRegen < resistRegenInterval) continue;
				int cur = PowerUtils.getResourceValue(sp, FormIdentifiers.MANCIANIMA_RESISTANCE);
				int max = PowerUtils.getResourceMax(sp, FormIdentifiers.MANCIANIMA_RESISTANCE);
				if (max <= 0) max = 2;
				if (cur >= max) { LAST_REGEN.put(id, now); continue; }
				PowerUtils.changeResourceValueAndSync(sp, FormIdentifiers.MANCIANIMA_RESISTANCE, 1);
				LAST_REGEN.put(id, now);
			} else if (FormIdentifiers.UPGRADE_FAMILIAR_FOX.equals(formId)) {
				// 进化使魔脱??mana 回复：脱??5s 后??1s ??1 ??mana（需已解?mana_system 节点?
				// 时间门前置：脱战稳态下先被 20t 时间门拦下，免付「进化组件??+
				// isUnlocked + 资源扫描」；两道门均为纯读取，调序不改变行为（对照契灵分支顺序）??
				UUID id = sp.getUUID();
				long lastCombat = LAST_COMBAT.getOrDefault(id, 0L);
				if (now - lastCombat < BAL.i("out_of_combat_ticks", OUT_OF_COMBAT_TICKS)) continue;
				long lastRegen = LAST_MANA_REGEN.getOrDefault(id, 0L);
				if (now - lastRegen < BAL.i("upgrade_fox_mana_regen_interval_ticks", UPGRADE_FOX_MANA_REGEN_INTERVAL_TICKS)) continue;
				if (UniversalFormationManager.isCharging(sp)) continue;
				// 仅在已解?mana_system 节点时生效（mana 条显示门控一致??
				if (!github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent.EVOLUTION
						.get(sp).isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_MANA)) continue;
				// 消??mana ??5s 内暂停自动回复（regen_pause_timer 资??> 0 表示在暂停窗口）
				// 性能：常量复用（原每 tick 每进化使魔现???new Identifier，构造含正则校验??
				int pauseTimer = PowerUtils.getResourceValue(sp, UPGRADE_FOX_MANA_REGEN_PAUSE_TIMER);
				if (pauseTimer > 0) continue;
				double curMana = github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMana(sp);
				double maxMana = github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMaxMana(sp);
				if (maxMana <= 0 || curMana >= maxMana) { LAST_MANA_REGEN.put(id, now); continue; }
				// 灵视节点：mana 自动回复速??+10%
				double regenAmount = BAL.d("upgrade_fox_mana_regen_amount", UPGRADE_FOX_MANA_REGEN_AMOUNT);
				github.com.gengyoubo.sscfa.evolution.EvolutionComponent svComp =
							github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent.EVOLUTION.get(sp);
				if (svComp.isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_SPIRIT_VISION)) {
					regenAmount *= 1.1;
				}
				// 火环节点：mana 自动回复速??+20%
				if (svComp.isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_FIRE_RING)) {
					regenAmount *= 1.2;
				}
				github.com.gengyoubo.sscfa.compat.ManaUtils.gainPlayerMana(sp, regenAmount);
				LAST_MANA_REGEN.put(id, now);
			}
		}
	}

	private static void removeAndCleanup(MinecraftServer server, Iterator<Map.Entry<UUID, Mark>> it, Mark m, UUID markerId) {
		it.remove();
		clearTargetSideEffects(server, m.targetUuid);
		DIRTY.add(markerId);
	}

	private static void clearTargetSideEffects(MinecraftServer server, UUID targetUuid) {
		if (server == null) return;
		Entity ent = findEntity(server, targetUuid);
		if (ent instanceof LivingEntity le) {
			le.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		}
	}

	// ============== Helpers ==============
	private static Entity findEntity(MinecraftServer server, UUID uuid) {
		for (ServerLevel w : server.getAllLevels()) {
			Entity e = w.getEntity(uuid);
			if (e != null) return e;
		}
		return null;
	}

	private static void sendSyncPacket(ServerPlayer player) {
		Mark m = MARKS.get(player.getUUID());
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		if (m == null) {
			buf.writeInt(0);
		} else {
			buf.writeInt(1);
			buf.writeUUID(m.targetUuid);
			buf.writeUtf(colorString(m.color));
		}
		buf.writeInt(m == null ? 0 : (int) Math.max(0,
				BAL.i("stage_gate_ticks", STAGE_GATE_TICKS) - (player.level().getGameTime() - m.colorSetTick)));
		try {
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_MARK_SYNC, buf);
		} catch (Exception ignored) {}
	}

	/** 重??换维度后强制把当?mark 状态同步给该玩家，保证客户???HUD/标记正???*/
	public static void resyncToPlayer(ServerPlayer player) {
		sendSyncPacket(player);
	}

	public static void playSoundToPlayer(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		player.connection.send(new ClientboundSoundPacket(
				entry, SoundSource.PLAYERS,
				player.getX(), player.getY(), player.getZ(),
				volume, pitch, player.getRandom().nextLong()));
	}

	public static void broadcastSoundAtEntity(ServerLevel world, Entity entity, SoundEvent sound, float volume, float pitch) {
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
}
