package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;

/**
 * 契??- 次要技能：瞬??
 *
 * 两种模式（由客户端配??mancianimaTeleportMode 决定）：
 * - RAYCAST：朝准星方向直接传送，最?8 格，遇到方块停止
 * - PLATFORM：锁定准星方向最近的可站立平台，落点紫色粒子仅自己可见??
 *   按下时显示预览，松开按键时传送；前方无可传送平台则拒绝（不消耗CD/法力?
 *
 * CD: 3.5 秒??0 tick??  法力消? 5
 * 仅自身瞬移，不需要白名单??
 */
public final class MancianimaTeleport {

	public static final int COOLDOWN_TICKS = 70; // 3.5s
	/** 联动攻击（红标闪现斩杀）命中后的独立联??CD??0s 内不能再发动联动，普通闪现仍只走 3.5s 常??CD */
	public static final int RED_LINK_CD_TICKS = 200; // 10s 联动攻击CD
	public static final int RED_FAIL_CD_TICKS = 70; // 3.5s 失败CD（引导中?完成时目标已死）
	public static final int MANA_COST = 5;
	public static final int RED_MARK_MANA_COST = 20;
	public static final double MAX_RANGE = 8.0;
	public static final double RED_MARK_TARGET_RANGE = 32.0;
	public static final int RED_MARK_CHANNEL_TICKS = 20; // 1s
	public static final float RED_MARK_DAMAGE_CAP = 37.0f;
	public static final float RED_MARK_DAMAGE_MIN = 2.0f;
	public static final double RED_MARK_DAMAGE_PERCENT = 0.50;
	/** 传送后冻结自然回蓝??tick 数?? 秒??*/
	public static final int MANA_REGEN_PAUSE_TICKS = 100;
	/** sp_mana_regen 的暂停计时子资源（apoli:multiple 子????power_id + "_" + sub_key??*/
	private static final net.minecraft.resources.ResourceLocation MANA_REGEN_PAUSE_RES =
			new net.minecraft.resources.ResourceLocation("my_addon", "form_familiar_fox_sp_mana_regen_regen_pause_timer");
	// 阶??5：运行时快照读取（abilities.mancianima_teleport；快照未初始化回退默认常量）?
	// MANA_COST=5 ??RED_LINK_CD_TICKS=200 ??client/hud/SkillHudCatalog 引用——HUD 是展示阈值??
	// 那两处保持常量引用不改；服务端消费一律??BAL??
	private static final BalanceReader BAL = new BalanceReader("abilities.mancianima_teleport");
	/** 统一冷却服务的稳定技?ID?? 魂??power 注册路径）??*/
	public static final String SKILL_ID = "my_addon:form_familiar_fox_mancianima_soul_teleport";

	/** 传送最大距离（双端一致）：客户端预览（computePlatformLanding）与服务端执行共用??
	 *  客户端读客户端镜像快照，服务端读权威快照（同 MeteorSpell.maxRange 模式）??*/
	private static double maxRange() {
		if (github.com.gengyoubo.sscfa.balance.BalanceIntegration.isClientThread()) {
			var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.clientSnapshot();
			if (s != null) return s.getDouble("abilities.mancianima_teleport", "max_range");
		}
		return BAL.d("max_range", MAX_RANGE);
	}

	private MancianimaTeleport() {
	}

	/** 模式 0 = RAYCAST?? = PLATFORM。客户端发送，服务端只信任模式（看向矢量自行从玩家状态读取）?*/
	public static boolean execute(ServerPlayer player, byte mode) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) return false;
		if (!isMancianima(player)) return false;
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return false;

		// 红标联动：如果准星在某个被本玩家红标的生物上，启??1s 引??
		net.minecraft.world.entity.LivingEntity redTarget = tryFindRedMarkedInCrosshair(player);
		if (redTarget != null) {
			// 联动攻击独??CD??0s 内不能再发动联动（准星指红标时提示并不闪现；不指红标不受此限，普通闪现照常）
			int linkCd = PowerUtils.getResourceValue(player, FormIdentifiers.MANCIANIMA_LINK_CD);
			if (linkCd > 0) {
				int sec = (int) Math.ceil(linkCd / 20.0);
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.link_locked", sec), true);
				return false;
			}
			if (github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMana(player) < BAL.i("red_mark_mana_cost", RED_MARK_MANA_COST)) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_mana"), true);
				return false;
			}
			if (MancianimaMarkManager.CHANNELING.containsKey(player.getUUID())) return false;
            if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
			long now = ((ServerLevel) player.level()).getGameTime();
			MancianimaMarkManager.CHANNELING.put(player.getUUID(),
					new MancianimaMarkManager.ChannelState(redTarget.getUUID(), now + BAL.i("red_mark_channel_ticks", RED_MARK_CHANNEL_TICKS), 2));
            MancianimaMarkManager.CHANNELING.get(player.getUUID()).castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.channeling"), true);
			return true;
		}

		int manaCost = BAL.i("mana_cost", MANA_COST);
		if (github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMana(player) < manaCost) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_mana"), true);
			return false;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		ServerLevel world = (ServerLevel) player.level();

		Vec3 targetFeet;
		if (mode == 1) {
			targetFeet = computePlatformLanding(world, eye, look, player);
			if (targetFeet == null) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_platform"), true);
				return false; // 无平???拒绝传送、不消耗CD/法??
			}
		} else {
			targetFeet = computeRaycastLanding(world, eye, look, player);
			if (targetFeet == null) return false;
		}

		// 传送落点判定：?落点本身能容纳玩家；?平台模式要求落点在玩家视野内（眼睛→落点腰部无方块遮挡）??
		// 用「视野内」替代旧的整条路径碰撞采样——后者会?传上比自己高的平??误判为穿墙（脚→台顶直线必经平台实体）而拒绝??
		// 视野判定既能挡隔栅??隔墙传送（落点腰部被遮挡），又允许正常登??跳跃落点??6）??
		if (!isSafeLanding(world, player, targetFeet)
				|| (mode == 1 && !isLandingVisible(world, player, targetFeet))) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_platform"), true);
			return false;
		}

		// 出发点粒??+ 音效（对周围所有玩家可?可听?
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.PORTAL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.3, 0.8, 0.3, 0.6);
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.REVERSE_PORTAL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				20, 0.3, 0.5, 0.3, 0.05);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);

		// 执行传送（保留视角方向??
		player.teleportToWithTicket(targetFeet.x, targetFeet.y, targetFeet.z);

		// 落点粒??+ 音效（对周围所有玩家可?可听?
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.PORTAL,
				targetFeet.x, targetFeet.y + 1.0, targetFeet.z,
				40, 0.3, 0.8, 0.3, 0.6);
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.REVERSE_PORTAL,
				targetFeet.x, targetFeet.y + 1.0, targetFeet.z,
				20, 0.3, 0.5, 0.3, 0.05);
		world.playSound(null, targetFeet.x, targetFeet.y, targetFeet.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

		// 扣除法力 + 设置CD
		github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
		if (mana != null) {
			mana.setMana(Math.max(0.0, mana.getMana() - manaCost));
		}
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);
		// 传送后冻结自然回蓝 3 秒（不影响主动回蓝技?消耗??
		PowerUtils.setResourceValueAndSync(player, MANA_REGEN_PAUSE_RES, BAL.i("mana_regen_pause_ticks", MANA_REGEN_PAUSE_TICKS));
		return true;
	}

	private static boolean isMancianima(Player player) {
		net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(player);
		return form != null && FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(form.id());
	}

	/** 准星上是否有被本玩家红标的目标？返回该目标，否??null??*/
	private static net.minecraft.world.entity.LivingEntity tryFindRedMarkedInCrosshair(ServerPlayer player) {
		MancianimaMarkManager.Mark m = MancianimaMarkManager.getMark(player.getUUID());
		if (m == null || m.color != MancianimaMarkManager.MarkColor.RED) return null;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = eye.add(look.scale(BAL.d("red_mark_target_range", RED_MARK_TARGET_RANGE)));
		ServerLevel world = (ServerLevel) player.level();
		net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(eye, end).inflate(2.0);
		double bestDist = Double.MAX_VALUE;
		net.minecraft.world.entity.LivingEntity best = null;
		for (net.minecraft.world.entity.Entity e : world.getEntities(player, searchBox,
				net.minecraft.world.entity.EntitySelector.NO_SPECTATORS)) {
			if (!(e instanceof net.minecraft.world.entity.LivingEntity le) || !le.isAlive()) continue;
			if (!le.getUUID().equals(m.targetUuid)) continue;
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, le)) continue;
			net.minecraft.world.phys.AABB box = e.getBoundingBox().inflate(1.0); // "大致对??放宽
			java.util.Optional<Vec3> hit = box.clip(eye, end);
			if (hit.isEmpty()) continue;
			double d = eye.distanceToSqr(hit.get());
			if (d < bestDist) { bestDist = d; best = le; }
		}
		return best;
	}

	/** MancianimaMarkManager 引??tick 末尾调用：执行红标瞬移斩杀??*/
	public static void executeRedMarkChannelComplete(ServerPlayer marker, net.minecraft.world.entity.LivingEntity target, long castId) {
        if (github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(marker, SKILL_ID) != castId || castId < 0) return;
        if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(marker)) {
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(marker, SKILL_ID, castId);
            return;
        }
		if (target == null || !target.isAlive()
				|| WhitelistUtils.isProtected(marker, target)
				|| github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(marker, target)) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(marker, SKILL_ID, castId);
			return;
		}
		// 閹?mana
		github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(marker);
		if (mana != null) mana.setMana(Math.max(0.0, mana.getMana() - BAL.i("red_mark_mana_cost", RED_MARK_MANA_COST)));
		ServerLevel world = (ServerLevel) marker.level();
		// 计算落点：目标身??格地?
		Vec3 targetPos = target.position();
		Vec3 targetLook = target.getLookAngle().normalize();
		Vec3 behind = targetPos.subtract(targetLook.x, 0, targetLook.z).add(0, 0, 0); // 1 格后?
		Vec3 landing = adjustBehindTarget(world, target, behind);
		// 出发粒子
		ParticleUtils.spawnParticles(world, ParticleTypes.PORTAL,
				marker.getX(), marker.getY() + 1.0, marker.getZ(),
				40, 0.3, 0.8, 0.3, 0.6);
		// 传??
		marker.teleportToWithTicket(landing.x, landing.y, landing.z);
		// 让契灵看向目?
		double dx = target.getX() - landing.x;
		double dz = target.getZ() - landing.z;
		float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		marker.setYRot(yaw);
		marker.setXRot(0);
		marker.connection.teleport(landing.x, landing.y, landing.z, yaw, 0);
		// 钀界偣绮掑?
		ParticleUtils.spawnParticles(world, ParticleTypes.PORTAL,
				landing.x, landing.y + 1.0, landing.z,
				40, 0.3, 0.8, 0.3, 0.6);
		// 50% 缺失血伤害：原上限默??37，NPC 上限翻倍；沿用原有 playerAttack 伤害源?
		float missing = target.getMaxHealth() - target.getHealth();
		float cap = NpcCombatEnhancements.scale(marker, target, (float) BAL.d("red_mark_damage_cap", RED_MARK_DAMAGE_CAP), "mancianima_link_cap_mul", 2.0);
		float dmg = (float) Math.max(BAL.d("red_mark_damage_min", RED_MARK_DAMAGE_MIN), Math.min(cap, missing * BAL.d("red_mark_damage_percent", RED_MARK_DAMAGE_PERCENT)));
		boolean wasAlive = target.isAlive();
		target.hurt(world.damageSources().playerAttack(marker), dmg);
		// 广播暴击音??
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
		// 设??CD + 暂停回蓝：普通闪现只?3.5s 常??CD；联动攻击进独立??10s 联??CD（期间可普通闪现、不可再联动?
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(marker, SKILL_ID, castId);
		PowerUtils.setResourceValueAndSync(marker, FormIdentifiers.MANCIANIMA_LINK_CD,
				github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.extra(marker, SKILL_ID, "link"));
		PowerUtils.setResourceValueAndSync(marker, MANA_REGEN_PAUSE_RES, BAL.i("mana_regen_pause_ticks", MANA_REGEN_PAUSE_TICKS));
		// 击杀奖励：刷新两??CD + 抗伤补??
		if (wasAlive && !target.isAlive()) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.reset(marker, MancianimaPrimary.SKILL_ID);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.reset(marker, SKILL_ID);
			// 击杀奖励同样清空联??CD，允许立刻再发动下一次联动攻?
			PowerUtils.setResourceValueAndSync(marker, FormIdentifiers.MANCIANIMA_LINK_CD, 0);
			int max = PowerUtils.getResourceMax(marker, FormIdentifiers.MANCIANIMA_RESISTANCE);
			if (max <= 0) max = 2;
			PowerUtils.setResourceValueAndSync(marker, FormIdentifiers.MANCIANIMA_RESISTANCE, max);
			// 斩杀回满魔力，与主技能保持一?
			github.com.gengyoubo.sscfa.compat.ManaUtils.setPlayerMana(marker, github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMaxMana(marker));
			marker.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.kill_bonus"), true);
		}
		// 红标使命达????清??
		MancianimaMarkManager.clearMark(world.getServer(), marker.getUUID());
	}

	/** 计算红标瞬移落点：优先目标后?格地面；后??2格悬崖→旁侧；旁侧无地→后方1格悬空?*/
	private static Vec3 adjustBehindTarget(ServerLevel world, net.minecraft.world.entity.LivingEntity target, Vec3 desired) {
		// 落点 Y：??desired Y 向下扫最??3 格寻找地??
		double baseY = target.getY();
		BlockPos basePos = BlockPos.containing(desired.x, baseY, desired.z);
		// 检查后?格地面是否存在（脚下方块实心?
		BlockPos floorBelow = basePos.below();
		boolean hasGround = !world.getBlockState(floorBelow).getCollisionShape(world, floorBelow).isEmpty();
		if (hasGround) return new Vec3(desired.x, baseY, desired.z);
		// 没地????检查目标旁??
		Vec3 targetLook = target.getLookAngle().normalize();
		Vec3 sideRight = new Vec3(-targetLook.z, 0, targetLook.x).scale(1.0);
		Vec3 sidePos = target.position().add(sideRight);
		BlockPos sideBelow = BlockPos.containing(sidePos.x, baseY, sidePos.z).below();
		if (!world.getBlockState(sideBelow).getCollisionShape(world, sideBelow).isEmpty()) {
			return new Vec3(sidePos.x, baseY, sidePos.z);
		}
		// 都没???仍传后方1格（空中??
		return new Vec3(desired.x, baseY, desired.z);
	}

	/**
	 * RAYCAST 模式：沿视线最??8 格，碰到方块时回退一点防卡墙??
	 * 共享算法（客户端可调用同一份逻辑做预览）?
	 */
	public static Vec3 computeRaycastLanding(Level world, Vec3 eye, Vec3 look, Player player) {
		Vec3 end = eye.add(look.scale(maxRange()));
		BlockHitResult hit = world.clip(new ClipContext(
				eye, end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));
		Vec3 landingEye;
		if (hit.getType() == HitResult.Type.MISS) {
			landingEye = end;
		} else {
			// 从碰撞点回退 0.4 格，避免脚部嵌入墙??
			landingEye = hit.getLocation().add(look.scale(-0.4));
		}
		// 转换为脚部坐标（玩家视高??1.62??
		double feetY = landingEye.y - (player.getEyeY() - player.getY());
		// 防??feetY 嵌入地面：若脚下方块为实心则向上抬一?
		BlockPos feetPos = BlockPos.containing(landingEye.x, feetY, landingEye.z);
		if (!world.getBlockState(feetPos).getCollisionShape(world, feetPos).isEmpty()) {
			feetY = feetPos.getY() + 1.0;
		}
		return new Vec3(landingEye.x, feetY, landingEye.z);
	}

	/**
	 * PLATFORM 模式：人性化策略??
	 * <ul>
	 *   <li>使??b>精确射线 XZ</b>作为落点（不卡格中心）；落点 Y 取方块碰撞箱顶面（楼?台阶适配）??/li>
	 *   <li>射线先做一次精?raycast：命???落到命中点（顶面命中=直接站；??底命?该列向下扫）?/li>
	 *   <li>射线未命????沿射?march，所有列内有效平台中?b>3D 距离离射线终点最??/b>的一个，
	 *       使得仰角越平→落点越远；仰角向上→优先选高处壁架而非脚下地面??/li>
	 *   <li>每列扫描窗??[refY-3, refY+1]，绝不会从射线很低处的列取到地面，从而避免低瞄高时落到脚下??/li>
	 * </ul>
	 * 站立条件只要求：方块顶面非??+ 上方有约 1.8 格空气；不限制平台尺寸??
	 */
	public static Vec3 computePlatformLanding(Level world, Vec3 eye, Vec3 look, Player player) {
		Vec3 end = eye.add(look.scale(maxRange()));
		BlockHitResult hit = world.clip(new ClipContext(
				eye, end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));

		if (hit.getType() == HitResult.Type.BLOCK) {
			Vec3 hitPos = hit.getLocation();
			// Case 1：命中方块顶???直接以命中??XZ 落脚（瞄哪站哪??
			if (hit.getDirection() == Direction.UP) {
				BlockPos solid = hit.getBlockPos();
				Double topY = collisionTopY(world, solid);
				if (topY != null && hasHeadroom(world, hitPos.x, topY, hitPos.z)
						&& isSafeLanding(world, player, new Vec3(hitPos.x, topY, hitPos.z))) {
					Vec3 landing = new Vec3(hitPos.x, topY, hitPos.z);
					if (withinRange(eye, landing)) return landing;
				}
			}
			// Case 2：命中侧??底????在命中点前方的空气列里找最贴近命中 Y 的平台（用命中点 XZ??
			BlockPos airCol = hit.getBlockPos().relative(hit.getDirection());
			Vec3 landing = findBestInColumn(world, player, hitPos.x, hitPos.z, airCol.getX(), airCol.getZ(), hitPos.y, eye, hitPos);
			if (landing != null) return landing;
		}

		// Case 3：射线无阻????沿射?march，取?射线终点"3D 距离最近的平台（远偏置?
		Vec3 best = null;
		double bestSqToEnd = Double.MAX_VALUE;
		double step = 0.5;
		for (double d = 1.0; d <= maxRange(); d += step) {
			Vec3 sample = eye.add(look.scale(d));
			Vec3 landing = findBestInColumn(world, player, sample.x, sample.z,
					Mth.floor(sample.x), Mth.floor(sample.z), sample.y, eye, end);
			if (landing == null) continue;
			double sq = landing.distanceToSqr(end);
			if (sq < bestSqToEnd) {
				bestSqToEnd = sq;
				best = landing;
			}
		}
		return best;
	}

	/**
	 * 在指定方块??(bx,bz) 内寻找最佳落脚平台??
	 * 落点 XZ 使??exactX/exactZ（保留小数精度）；Y 使用方块碰撞箱实际顶面高度??
	 * 候选窗口为 [refY-3, refY+1]，避免从远处低高度射线样本误取脚下地面?
	 * 在窗口内全部候选中取??anchor（射线终??命中点）3D 距离最近的一个??
	 */
	private static Vec3 findBestInColumn(Level world, Player player, double exactX, double exactZ,
	                                       int bx, int bz, double refY, Vec3 eye, Vec3 anchor) {
		int topY = Mth.floor(refY) + 1;
		int bottomY = Mth.floor(refY) - 3;
		Vec3 best = null;
		double bestSq = Double.MAX_VALUE;
		for (int y = topY; y >= bottomY; y--) {
			BlockPos pos = new BlockPos(bx, y, bz);
			Double topYExact = collisionTopY(world, pos);
			if (topYExact == null) continue;
			if (!hasHeadroom(world, exactX, topYExact, exactZ)) continue;
			Vec3 candidate = new Vec3(exactX, topYExact, exactZ);
			if (!isSafeLanding(world, player, candidate)) continue;
			if (!withinRange(eye, candidate)) continue;
			double sq = candidate.distanceToSqr(anchor);
			if (sq < bestSq) {
				bestSq = sq;
				best = candidate;
			}
		}
		return best;
	}

	/** 返回方块碰撞箱顶面世??Y；空碰撞箱（空气/植物等）返??null。支持楼??台阶?*/
	private static Double collisionTopY(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getCollisionShape(world, pos);
		if (shape.isEmpty()) return null;
		double maxY = shape.max(Direction.Axis.Y);
		if (maxY <= 0) return null;
		return pos.getY() + maxY;
	}

	/**
	 * 检??(x,z) 处脚?feetY 上方?1.8 格空气（玩家身高）??
	 * ??floor(feetY + 0.5) 作为起点跳过平台本身（包括台?楼梯顶面）??
	 */
	private static boolean hasHeadroom(Level world, double x, double feetY, double z) {
		int bx = Mth.floor(x);
		int bz = Mth.floor(z);
		int startY = Mth.floor(feetY + 0.5);
		for (int dy = 0; dy < 2; dy++) {
			BlockPos p = new BlockPos(bx, startY + dy, bz);
			VoxelShape shape = world.getBlockState(p).getCollisionShape(world, p);
			if (!shape.isEmpty()) return false;
		}
		return true;
	}

	/** 落点距离硬限制（眼睛到落点直线距离）??*/
	private static boolean withinRange(Vec3 eye, Vec3 landing) {
		double maxRange = maxRange();
		return eye.distanceToSqr(landing) <= (maxRange + 0.5) * (maxRange + 0.5);
	}

	/** 检查落点处玩家完整碰撞箱是否为空??*/
	private static boolean isSafeLanding(Level world, Player player, Vec3 feet) {
		AABB box = playerBoxAt(player, feet.x, feet.y, feet.z);
		return world.noCollision(player, box);
	}

	/** 落点必须在玩家视野内：眼睛→落点腰部的视线不被方块（含栅??墙）遮挡。用于杜绝平台模式隔障传送（#6）??*/
	private static boolean isLandingVisible(Level world, Player player, Vec3 targetFeet) {
		Vec3 eye = player.getEyePosition();
		// 取落点身体中心高度作为可见性参考点（约 0.5~0.9 格高），避免只检查脚底被台阶误判
		Vec3 targetCenter = targetFeet.add(0, Math.min(0.9, Math.max(0.5, player.getBbHeight() / 2.0)), 0);
		BlockHitResult hit = world.clip(new ClipContext(
				eye, targetCenter,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));
		if (hit.getType() != HitResult.Type.BLOCK) return true;
		// 命中方块点比落点更近（留 0.25 容差）→ 视线被遮????落点不可???拒绝传?
		return hit.getLocation().distanceToSqr(eye) + 0.25 >= targetCenter.distanceToSqr(eye);
	}

	private static AABB playerBoxAt(Player player, double x, double y, double z) {
		double halfWidth = Math.max(0.3, player.getBbWidth() / 2.0);
		double height = Math.max(1.8, player.getBbHeight());
		return new AABB(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth).deflate(1.0E-7);
	}
}
