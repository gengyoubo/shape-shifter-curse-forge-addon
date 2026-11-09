package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.spell.DomainManager;

/**
 * 契??- 主要技能：三段标记??
 *
 * 段位?
 * <ol>
 *   <li>无标?/ 有??橙标记但准星不在该目???标记新目标（黄）??
 *       消??15 mana；准星无生????5 mana fizzle；触??5s CD（仅首次）??/li>
 *   <li>橙标的同一目标在准星????升级为红色（恐惧 + 凋零叫，1s 触发间隔，不?CD）??/li>
 *   <li>已有红标，无论准星在???启??2s 引导，引导成功对红标目标造??
 *       20% 剩余生命真伤（上?20?? 视觉爆炸；CD +15s??/li>
 * </ol>
 *
 * 白名单：默认机制（{@link WhitelistUtils#isProtected}），白名单成?被驯服宠物不能被标记?
 * 同时禁止标记劫掠阵营？（设定文件?无法对劫掠阵营生物使?——属基础被动，本期暂不做拦截。）
 */
public final class MancianimaPrimary {

	private MancianimaPrimary() {}

	private static float channelDamage(ServerPlayer caster, LivingEntity target, float remainingHealth) {
		float cap = NpcCombatEnhancements.primaryCap(caster, target, (float) BAL.d("damage_cap", DAMAGE_CAP));
		return (float) Math.max(BAL.d("damage_min", DAMAGE_MIN), Math.min(cap, remainingHealth * BAL.d("damage_percent", DAMAGE_PERCENT)));
	}

	public static final int MARK_MANA_COST = 15;
	public static final int FIZZLE_MANA_COST = 5;
	public static final String SKILL_ID = "my_addon:form_familiar_fox_mancianima_contract_mark";
	public static final int RED_TRIGGER_INTERVAL = 20;       // 1s 触发间隔（防连点升红?
	public static final double MARK_RANGE = 32.0;
	public static final double RED_LOCK_RANGE = 24.0;
	public static final int CHANNEL_DAMAGE_TICKS = 40;       // 2s 引??
	public static final double DAMAGE_PERCENT = 0.20;
	public static final float DAMAGE_MIN = 2.0f;
	public static final float DAMAGE_CAP = 37.0f;
	public static final int MANA_REGEN_PAUSE_TICKS = 100;    // 5s
	private static final net.minecraft.resources.ResourceLocation MANA_REGEN_PAUSE_RES =
			new net.minecraft.resources.ResourceLocation("my_addon", "form_familiar_fox_sp_mana_regen_regen_pause_timer");
	// 阶??5：运行时快照读取（abilities.mancianima_primary；快照未初始化回退默认常量?
	private static final BalanceReader BAL = new BalanceReader("abilities.mancianima_primary");
	/** 客户端按键调用的入口?*/
	public static void execute(ServerPlayer player) {
		if (!isMancianima(player)) return;
		ServerLevel world = (ServerLevel) player.level();
		long now = world.getGameTime();

		// 引导中？再次按键 = 取消（设定未明说，默认行为：不响应；这里保持不响应??
		if (MancianimaMarkManager.CHANNELING.containsKey(player.getUUID())) return;

		MancianimaMarkManager.Mark mark = MancianimaMarkManager.getMark(player.getUUID());

		// ??3：已有红????引??2s 真伤
		if (mark != null && mark.color == MancianimaMarkManager.MarkColor.RED) {
			LivingEntity tgt = findLivingByUuid(world, mark.targetUuid);
			if (tgt == null || !tgt.isAlive() || DomainManager.blocksTargeting(player, tgt)) return;
			// 阶段冷却：红标刚升级后需??3s 才能引爆（??MarkManager 侧同 scope 参数以保持同一值）
			int stageGate = MancianimaMarkManager.stageGateTicks();
			if (now - mark.colorSetTick < stageGate) {
				int secLeft = (int) Math.max(1L, (long) Math.ceil((stageGate - (now - mark.colorSetTick)) / 20.0));
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.stage_locked_seconds", secLeft), true);
				playMarkFailSound(player);
				return;
			}
			// 不消?mana 立即开始引导；CD 在引导成功后??15s
			MancianimaMarkManager.CHANNELING.put(player.getUUID(),
					new MancianimaMarkManager.ChannelState(mark.targetUuid, now + BAL.i("channel_damage_ticks", CHANNEL_DAMAGE_TICKS), 1));
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.channeling"), true);
			// 全场低响蓄力嗡鸣（让第三方有所感知?
			MancianimaMarkManager.broadcastSoundAtEntity(world, player, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.5f);
			// marker 私聊：明显??启??反馈（仅自己能听见??
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.4f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
			return;
		}

		// ??1/2：raycast 找目?
		LivingEntity target = raycastLiving(player, BAL.d("mark_range", MARK_RANGE));

		// ??2：橙标的同一目标
		if (mark != null && mark.color == MancianimaMarkManager.MarkColor.ORANGE
				&& target != null && target.getUUID().equals(mark.targetUuid)) {
			// 阶段冷却：首次标记后需?3s 才能升红（??MarkManager 侧同 scope 参数以保持同一值）
			int stageGate = MancianimaMarkManager.stageGateTicks();
			if (now - mark.colorSetTick < stageGate) {
				int secLeft = (int) Math.max(1L, (long) Math.ceil((stageGate - (now - mark.colorSetTick)) / 20.0));
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.stage_locked_seconds", secLeft), true);
				playMarkFailSound(player);
				return;
			}
			// 1s 触发间隔通??SP_PRIMARY_CD 是??0 来检测：但此段不应??CD ??用单独资源不太划算??
			// 简化：若上次红升时间未到（??RED_LOCKOUT），?upgradeToRed 内部判定??
			boolean ok = MancianimaMarkManager.upgradeToRed(player, target);
			if (!ok) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.red_locked"), true);
				return;
			}
			// 伤害与音??
			// 红升成功 - marker 多层叠加反馈音（个人听见，清晰提示第二次命中??
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.7f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.8f, 0.9f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.4f);
			// 目标处也广播一下，让受击者及周围玩家能听见二段升?
			MancianimaMarkManager.broadcastSoundAtEntity(world, target, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.4f);
			if (target instanceof ServerPlayer sp) {
				MancianimaMarkManager.playSoundToPlayer(sp, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.0f);
				MancianimaMarkManager.playSoundToPlayer(sp, SoundEvents.WITHER_AMBIENT, 0.7f, 1.0f);
			} else {
				MancianimaMarkManager.broadcastSoundAtEntity(world, target, SoundEvents.WITHER_AMBIENT, 0.7f, 1.0f);
			}
			// ??2 不消?CD、不消?mana
			return;
		}

		// ??1：标记新目标（或当前没有任何标记??
		// 检??CD（如果当前??CD 中，第一段直接拒绝??
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) {
			playMarkFailSound(player);
			return;
		}

		if (target == null) {
			// 无目?fizzle
			int fizzleCost = BAL.i("fizzle_mana_cost", FIZZLE_MANA_COST);
			github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
			if (mana == null || mana.getMana() < fizzleCost) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.no_mana"), true);
				playMarkFailSound(player);
				return;
			}
			mana.setMana(mana.getMana() - fizzleCost);
			pauseManaRegen(player);
			playMarkFailSound(player);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);
			return;
		}

		// 白名?/ 不能标记自??
		if (target == player) { playMarkFailSound(player); return; }
		if (WhitelistUtils.isProtected(player, target)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.target_protected"), true);
			playMarkFailSound(player);
			return;
		}

		github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
		int markCost = BAL.i("mark_mana_cost", MARK_MANA_COST);
		if (mana == null || mana.getMana() < markCost) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.no_mana"), true);
			playMarkFailSound(player);
			return;
		}
		mana.setMana(mana.getMana() - markCost);
		pauseManaRegen(player);

		MancianimaMarkManager.setMark(player, target, MancianimaMarkManager.MarkColor.YELLOW);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);

		// 标记成功 - ??marker 自己能听到的反馈音（多层叠加，增强可辨识度??
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7f, 1.8f);
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.6f, 1.4f);

		if (target instanceof ServerPlayer sp) {
			MancianimaMarkManager.playSoundToPlayer(sp, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 2.0f);
		} else {
			MancianimaMarkManager.broadcastSoundAtEntity(world, target, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 2.0f);
		}
	}

	/** ??MancianimaMarkManager 引??tick 末尾调用：执行真伤与爆炸特效??*/
	public static void executeChannelComplete(ServerPlayer marker, LivingEntity target) {
		if (target == null || !target.isAlive() || WhitelistUtils.isProtected(marker, target) || DomainManager.blocksTargeting(marker, target)) return;
		ServerLevel world = (ServerLevel) marker.level();
		// 真伤计??
		float remainingHealth = target.getHealth();
		float dmg = channelDamage(marker, target, remainingHealth);
		// 伤害归属：使?indirectMagic ??marker 为攻击者（生物会什7并??tag bypasses_armor不被护甲减免?
		DamageSource src = world.damageSources().indirectMagic(marker, marker);
		target.hurt(src, dmg);

		// AOE：??target 为中?3 格半径内的其他生物受?50% 伤害（跳??marker 自??+ 主目?+ 白名单）
		double aoeRadius = 3.0;
		AABB aoeBox = new AABB(
				target.getX() - aoeRadius, target.getY() - aoeRadius, target.getZ() - aoeRadius,
				target.getX() + aoeRadius, target.getY() + aoeRadius, target.getZ() + aoeRadius);
		for (Entity e : world.getEntities((net.minecraft.world.entity.Entity) null, aoeBox, EntitySelector.NO_SPECTATORS)) {
			if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
			if (le == target || le == marker) continue;
			if (e.distanceToSqr(target) > aoeRadius * aoeRadius) continue;
			if (WhitelistUtils.isProtected(marker, le)) continue;
			// Per-recipient cap: an NPC primary target never increases the player splash.
			le.hurt(src, channelDamage(marker, le, remainingHealth) * 0.5f);
		}

		// 爆炸视觉（不造成实际伤害??
		double cx = target.getX(), cy = target.getY() + target.getBbHeight() / 2.0, cz = target.getZ();
		// 主爆炸（大型?
		world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 2, 0.3, 0.3, 0.3, 0);
		// 灵魂烈焰球（紫黑契灵主题??
		world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 80, 1.5, 1.5, 1.5, 0.15);
		world.sendParticles(ParticleTypes.SCULK_SOUL, cx, cy, cz, 30, 1.2, 1.2, 1.2, 0.1);
		world.sendParticles(ParticleTypes.DRAGON_BREATH, cx, cy, cz, 40, 1.5, 0.8, 1.5, 0.05);
		// AOE 边缘指示?? 格半径周围环形粒子??
		double aoeR = 3.0;
		for (int i = 0; i < 32; i++) {
			double a = i * Math.PI * 2.0 / 32.0;
			double px = cx + Math.cos(a) * aoeR;
			double pz = cz + Math.sin(a) * aoeR;
			world.sendParticles(ParticleTypes.LARGE_SMOKE, px, cy - 0.5, pz, 1, 0, 0.1, 0, 0);
			world.sendParticles(ParticleTypes.FLAME, px, cy, pz, 1, 0, 0.05, 0, 0);
		}
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.8f, 0.8f);
		// 龙振翅音效：?marker 为中心广播，渲染“斩杀”气?
		world.playSound(null, marker.getX(), marker.getY(), marker.getZ(),
				SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2f, 0.8f);
		// 閹存劕濮?CD +15s
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.force(marker, SKILL_ID,
				github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.remaining(marker, SKILL_ID)
						+ github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.extra(marker, SKILL_ID, "channel_success"));
		// 斩杀完成：补?mana 能??
		github.com.gengyoubo.sscfa.compat.ManaUtils.setPlayerMana(marker, github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMaxMana(marker));
		// 红标使命达????清除（已造成伤害，进入下一轮??
		MancianimaMarkManager.clearMark(world.getServer(), marker.getUUID());
	}

	// ============== Helpers ==============

	private static boolean isMancianima(Player player) {
		net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm((ServerPlayer) player);
		return form != null && FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(form.id());
	}

	private static void pauseManaRegen(ServerPlayer player) {
		PowerUtils.setResourceValueAndSync(player, MANA_REGEN_PAUSE_RES, BAL.i("mana_regen_pause_ticks", MANA_REGEN_PAUSE_TICKS));
	}

	/** 标记失败提示音（??marker 自己听到，统一格式??*/
	private static void playMarkFailSound(ServerPlayer player) {
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.0f);
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.6f);
	}

	/** 准星锥形索敌：在前??5度范围内寻找最近的可标记目标??*/
	private static LivingEntity raycastLiving(ServerPlayer player, double maxRange) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		ServerLevel world = (ServerLevel) player.level();

		// 15??锥形半角的余弦阈值（cos(7.5????
		double cosHalfAngle = Math.cos(Math.toRadians(7.5));

		// 锥形远端点（用于构建搜索包围盒）
		Vec3 end = eye.add(look.scale(maxRange));
		double r = Math.max(0.5, maxRange * Math.tan(Math.toRadians(7.5)));
		AABB searchBox = new AABB(eye, end).inflate(r);

		double bestDist = Double.MAX_VALUE;
		LivingEntity best = null;

		for (Entity e : world.getEntities(player, searchBox, EntitySelector.NO_SPECTATORS)) {
			if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
			if (DomainManager.blocksTargeting(player, le)) continue;

			// 取实体中??
			Vec3 entityCenter = le.position().add(0, le.getBbHeight() / 2.0, 0);
			Vec3 toEntity = entityCenter.subtract(eye);
			double dist = toEntity.length();
			if (dist > maxRange) continue;

			// 15度锥形范围判定：方向向量与视线夹???7.5??
			Vec3 toEntityNorm = toEntity.normalize();
			double dot = look.dot(toEntityNorm);
			if (dot < cosHalfAngle) continue;

			// 视野遮挡检查：从眼睛到实体中点做射线，看是否被方块挡在实体?
			BlockHitResult blockHit = world.clip(new ClipContext(
					eye, entityCenter,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					player));
			if (blockHit.getType() != HitResult.Type.MISS) {
				double blockDistSq = eye.distanceToSqr(blockHit.getLocation());
				// 方块命中点比实体???被方块遮??
				if (blockDistSq + 0.5 < dist * dist) continue;
			}

			// 跳过白名??
			if (WhitelistUtils.isProtected(player, le)) continue;

			if (dist < bestDist) {
				bestDist = dist;
				best = le;
			}
		}
		return best;
	}

	private static LivingEntity findLivingByUuid(ServerLevel world, java.util.UUID uuid) {
		Entity e = world.getEntity(uuid);
		if (e instanceof LivingEntity le) return le;
		// 遍历所有维??
		if (world.getServer() != null) {
			for (ServerLevel w : world.getServer().getAllLevels()) {
				Entity en = w.getEntity(uuid);
				if (en instanceof LivingEntity le2) return le2;
			}
		}
		return null;
	}
}
