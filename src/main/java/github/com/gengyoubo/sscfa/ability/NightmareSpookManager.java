package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.GhostCatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 食梦魔（Nightmare）次要技能「惊吓」（Spook）—??服务端权威??
 *
 * <p>按键（sp_secondary，CD 8s）对所有已入梦目标生效?/p>
 * <ul>
 *   <li><b>幽灵苦力?/b>：目标身后空气位生成真???{@link Creeper}（NoAI/无??对他??
 *       隐身），仅目标客户端可见（mixin 局部显形）；引信音效与到期爆炸?粒全?
 *       <b>S2C 直发目???/b>（他人零感知）；3 秒后无实伤爆除???/li>
 *   <li><b>幽灵野???/b>：目标攻击苦力怕后，从苦力怕消散处生成真??
 *       {@link GhostCatEntity}（野猫形??geo 模型，NoAI/无??对他人隐身，仅目标可见），???
 *       <b>猫哈?/b>（定向音效），朝目标奔跑（服务端手动驱动位移），距目??4 ??b>起跳扑脸</b>??
 *       贴身挥爪??2 点魔法伤害），随后烟雾消??+ 头顶仅目标可见的烟花?/li>
 * </ul>
 *
 * <p>定向声画原则：所有声?粒子只???{@code PlaySoundS2CPacket}/{@code ParticleS2CPacket}
 * 直发给目标本人（服务端权??+ 只有目标听得?看得?+ 带空间方位），绝?world.playSound
 * 广播（那会把幽灵泄漏给周围所有人）??/p>
 */
public final class NightmareSpookManager {

	// ==== 以下常量为默认值；运行时从 balance 快照读取（scope: abilities.nightmare_spook，数据包可覆盖）====
	/** 技??CD（tick?? 秒）。默认；运行时??balance 快照读取??*/
	public static final int SPOOK_COOLDOWN_TICKS = 160;
	/** 幽灵野猫攻击伤害（魔法）。默认；运行时??balance 快照读取??*/
	public static final float CLONE_DAMAGE = 12.0f;
	/** 幽灵苦力怕存活（tick?? 秒）。默认；运行时??balance 快照读取??*/
	public static final int CREEPER_LIFE_TICKS = 60;
	/** 幽灵野猫存活上限（tick，兜底；正??~1.5-2.5 秒完成扑咬）。默认；运行时??balance 快照读取??*/
	public static final int CAT_LIFE_TICKS = 90;
	/** 野猫奔跑步速（?tick）。默认；运行时从 balance 快照读取??*/
	public static final double CAT_RUN_SPEED = 0.32;
	/** 野猫距目标多远起跳（格）。默认；运行时从 balance 快照读取??*/
	public static final double CAT_LEAP_DISTANCE = 4.0;

	/** 服务端权??balance 快照读取（快照未初始化时回退上方默认常量）??*/
	private static final BalanceReader BAL = new BalanceReader("abilities.nightmare_spook");

	/** 活跃幽灵苦力怕：实??UUID -> 目标玩家 UUID??*/
	private record CreeperState(UUID target, UUID caster) {}
	private static final Map<UUID, CreeperState> CREEPERS = new ConcurrentHashMap<>();
	/** 活跃幽灵野猫：实??UUID -> CatState??*/
	private static final Map<UUID, CatState> CATS = new ConcurrentHashMap<>();

	/** 妤犵偟鏅导鎺楁煂鎼达絽鐨搁弶鈺傚姌椤㈡垿骞€娴ｇ鍋?*/
	private static final class CatState {
		final ServerPlayer target;
		final UUID casterUuid;
		final long bornTick;
		/** 阶段?=追???1=跳跃?2=已挥?待消???*/
		int phase;
		/** 跳跃起点（插值抛物线用）??*/
		double jumpFromX, jumpFromY, jumpFromZ;
		/** 跳跃已进??tick??*/
		int jumpTicks;

		CatState(ServerPlayer target, UUID casterUuid, long bornTick) {
			this.target = target;
			this.casterUuid = casterUuid;
			this.bornTick = bornTick;
		}
	}

	private NightmareSpookManager() {
	}

	/** 次技能入口（power action 调用，仅服务端）。无入梦目标返回 false（不?CD）??*/
	public static boolean execute(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) return false;
		// CD 门禁已由 power ??begin 统一拦截（??P1-1??

		long now = world.getGameTime();
		List<LivingEntity> targets = NightmareDreamManager.collectDreamTargets(player, now);
		if (targets.isEmpty()) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}

		boolean any = false;
		for (LivingEntity target : targets) {
			if (target instanceof ServerPlayer sp) {
				// 玩家目标：幽灵苦力怕幻象全套（引?????回????幽灵野猫扑脸???
				if (spawnGhostCreeper(world, player, sp)) any = true;
			} else {
				// 非玩家目标（用户定稿）：无幻象，直接被幽灵野猫撕咬??2 点魔法伤害）???
				// 入梦时间重置???20 秒满额，不强制退出（无恐惧联动的强制苏醒）??
				if (directMobBite(world, player, target, now)) any = true;
			}
		}
		if (!any) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}
		return true;
	}

	/** 非玩家目标：无形的幽灵野猫直接撕咬（定向声效 + 伤??+ 入梦重???20s）??*/
	private static boolean directMobBite(ServerLevel world, ServerPlayer caster, LivingEntity target, long now) {
		if (!target.isAlive() || target.level() != world) return false;
		// 白名单保护：白名单内生物不受惊吓伤??
		if (github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(caster, target)) return false;
		// 幽灵野猫贴身一击???2 点魔法伤害（???completeCatAttack 的伤害源，可归因食梦魔??
		target.hurt(caster.damageSources().indirectMagic(caster, caster), (float) BAL.d("clone_damage", CLONE_DAMAGE));
		// 入梦时间重置???20 秒满额（不强制退出；与恐惧的重置同额?
		NightmareDreamManager.resetDream(caster.getUUID(), target.getUUID(), now);
		// 声效与粒子（生物无客户端屏幕：全员可闻的低沉猫啦 + 目标位置烟雾???
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.CAT_HISS, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.85f);
		world.sendParticles(ParticleTypes.CLOUD,
				target.getX(), target.getY(0.5), target.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
		return true;
	}

	// ==================== 定向声画（仅目标本人?====================

	/** 定向音效：仅目标听到??C 直发，带空间方位衰减）???*/
	private static void playSoundAt(ServerPlayer target, SoundEvent sound, double x, double y, double z,
	                                float volume, float pitch) {
		Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		target.connection.send(new ClientboundSoundPacket(
				entry, SoundSource.HOSTILE, x, y, z, volume, pitch, target.getRandom().nextLong()));
	}

	/** 定向粒子：仅目标看到??C 直发）???*/
	private static void spawnParticlesAt(ServerPlayer target, ParticleOptions particle,
	                                     double x, double y, double z, int count, double ox, double oy, double oz) {
		target.connection.send(new ClientboundLevelParticlesPacket(particle, true, x, y, z,
				(float) ox, (float) oy, (float) oz, 0.0f, count));
	}

	// ==================== 妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€?====================

	/** 在目标身??180??随机方??2~3 格的空气位置生成幽灵苦力怕（真实体，仅目标可见）?*/
	private static boolean spawnGhostCreeper(ServerLevel world, ServerPlayer caster, LivingEntity target) {
		if (!(target instanceof ServerPlayer sp)) return false;
		Vec3 pos = findAirPosBehind(world, target, 2.0, 3.0, 16, 0.9, 1.5);
		if (pos == null) return false;

		Creeper creeper = EntityType.CREEPER.create(world);
		creeper.moveTo(pos.x, pos.y, pos.z, target.getYHeadRot() + 180.0f, 0.0f);
		// 幽灵化数据：???AI（站桩）、无敌、对他人隐身、无重力、持久化
		creeper.setNoAi(true);
		creeper.setInvulnerable(true);
		creeper.setInvisible(true);
		creeper.setNoGravity(true);
		creeper.setPersistenceRequired();
		creeper.addTag("ssca_ghost_creeper");
        CREEPERS.put(creeper.getUUID(), new CreeperState(sp.getUUID(),caster.getUUID()));
        if (!world.addFreshEntity(creeper)) { CREEPERS.remove(creeper.getUUID()); return false; }
		// 幽灵标记包（仅目标）：客户端对该实体局部取??invisible
		SscAddonNetworking.sendSpookGhost(sp, creeper.getUUID(), BAL.i("creeper_life_ticks", CREEPER_LIFE_TICKS));
		// 引信嘶嘶声（定向，仅目标听到，从幽灵方向传来???
		playSoundAt(sp, SoundEvents.CREEPER_PRIMED, pos.x, pos.y, pos.z, 1.0f, 1.0f);
		return true;
	}

	/** 目标攻击幽灵苦力怕（服务???AttackEntityCallback）：击杀苦力?+ 召出幽灵野猫???*/
	public static void onGhostCreeperAttacked(ServerPlayer attacker, Creeper creeper) {
		CreeperState ghostState = CREEPERS.get(creeper.getUUID());
		if (ghostState == null) return;
		if (!attacker.getUUID().equals(ghostState.target())) return; // 只有目标本人的攻击有???
		CREEPERS.remove(creeper.getUUID());
		ServerLevel world = (ServerLevel) attacker.level();
		// 击杀音???烟雾（定向，仅目标??
		playSoundAt(attacker, SoundEvents.CREEPER_DEATH, creeper.getX(), creeper.getY(), creeper.getZ(), 0.8f, 1.0f);
		spawnParticlesAt(attacker, ParticleTypes.CLOUD, creeper.getX(), creeper.getY(0.5), creeper.getZ(), 8, 0.3, 0.3, 0.3);
		Vec3 creeperPos = creeper.position();
		creeper.discard();
		spawnGhostCat(world, attacker, creeperPos,ghostState.caster());
	}

	// ==================== 妤犵偟鏅导鎺楁煂鎼达絽鐨?====================

	/** 从苦力怕消散处生成幽灵野猫（野猫形态模型真实体，仅目标可见）??奔????跳脸攻击??*/
	private static void spawnGhostCat(ServerLevel world, ServerPlayer target, Vec3 nearPos,UUID casterId) {
		// 生成点：优先苦力怕原位置附近（野猫「从苦力怕消散处扑出」），失败退目标身???
		Vec3 pos = findAirPosNear(world, nearPos, 1.5, 8, 0.9, 1.0);
		if (pos == null) pos = findAirPosBehind(world, target, 2.0, 3.0, 12, 0.9, 1.0);
		if (pos == null) return;

		GhostCatEntity cat = github.com.gengyoubo.sscfa.init.AddonEntities.GHOST_CAT.get().create(world);
		cat.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
		// 幽灵化：NoAI + 无??+ 隐身（仅目标客户端显形）；保留重力（要贴地???跳??
		cat.setNoAi(true);
		cat.setInvulnerable(true);
		cat.setInvisible(true);
		cat.setPersistenceRequired();
		cat.addTag("ssca_ghost_cat");
        long now = world.getGameTime();
        CATS.put(cat.getUUID(), new CatState(target, casterId, now));
        if (!world.addFreshEntity(cat)) { CATS.remove(cat.getUUID()); return; }
		// 显形标记包（仅目标??
		SscAddonNetworking.sendSpookGhost(target, cat.getUUID(), BAL.i("cat_life_ticks", CAT_LIFE_TICKS));
		// 猫哈气音效（定向，仅目标听到?
		playSoundAt(target, SoundEvents.CAT_HISS, pos.x, pos.y, pos.z, 1.0f, 1.0f);
		// 出场小烟雾（定向粒子，仅目标??
		spawnParticlesAt(target, ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 6, 0.25, 0.25, 0.25);
	}

	/** ??tick 推进幽灵野猫：追????起????贴身挥爪??2 魔法伤）??*/
	private static void tickGhostCat(ServerLevel world, CatState st, GhostCatEntity cat) {
		ServerPlayer target = st.target;
		long now = world.getGameTime();
		long age = now - st.bornTick;
		// 兜底：超时没咬??/ 目标已?????烟雾消???
		if (age >= BAL.i("cat_life_ticks", CAT_LIFE_TICKS) || !target.isAlive() || target.level() != world) {
			spawnParticlesAt(target, ParticleTypes.CLOUD, cat.getX(), cat.getY(0.5), cat.getZ(), 8, 0.3, 0.3, 0.3);
			cat.discard();
			CATS.remove(cat.getUUID());
			return;
		}
		Vec3 catPos = cat.position();
		Vec3 eyePos = new Vec3(target.getX(), target.getY(0.5), target.getZ());
		Vec3 toEye = eyePos.subtract(catPos);
		double dist = toEye.length();
		// 面向目???
		float yaw = (float) Math.toDegrees(Math.atan2(toEye.x, toEye.z)) - 90.0f;
		cat.setYRot(yaw);
		cat.yHeadRot = yaw;
		cat.yBodyRot = yaw;

		if (st.phase == 0) {
			// 追跑：朝目标移动（NoAI 下手动驱动；limbAnimator 由位置差驱动腿部动画???
			Vec3 dir = toEye.normalize();
			Vec3 next = catPos.add(dir.scale(BAL.d("cat_run_speed", CAT_RUN_SPEED)));
			cat.setPos(next.x, next.y, next.z);
			// 距??4 ????起跳扑脸
			if (dist <= BAL.d("cat_leap_distance", CAT_LEAP_DISTANCE)) {
				st.phase = 1;
				st.jumpFromX = catPos.x;
				st.jumpFromY = catPos.y;
				st.jumpFromZ = catPos.z;
				st.jumpTicks = 0;
				// 扑击前低吼（猫叫降调，定向??
				playSoundAt(target, SoundEvents.CAT_AMBIENT, catPos.x, catPos.y, catPos.z, 0.9f, 0.75f);
			}
		} else if (st.phase == 1) {
			// 跳跃：插值抛物线扑向目标脸部???t / 0.4s??
			st.jumpTicks++;
			float t = Math.min(1.0f, st.jumpTicks / 8.0f);
			double nx = st.jumpFromX + (target.getX() - st.jumpFromX) * t;
			double ny = st.jumpFromY + (target.getY(0.5) - 1.4 - st.jumpFromY) * t
					+ Math.sin(t * Math.PI) * 0.9; // 鐎殿啫鍥モ偓?+0.9 闁?
			double nz = st.jumpFromZ + (target.getZ() - st.jumpFromZ) * t;
			// 防卡墙：跳跃路径前方有方????直接结算贴身攻击
			BlockPos front = BlockPos.containing(nx, ny, nz);
			if (!world.getBlockState(front).isAir()) {
				completeCatAttack(world, st, cat, target);
				return;
			}
			cat.setPos(nx, ny, nz);
			if (t >= 1.0f) {
				completeCatAttack(world, st, cat, target);
			}
		}
	}

	/** 野猫贴身挥爪???2 魔法?+ 命中音???粒子，随后消散???*/
	private static void completeCatAttack(ServerLevel world, CatState st, GhostCatEntity cat, ServerPlayer target) {
		if (st.phase == 2) return;
		st.phase = 2;
		ServerPlayer caster = world.getServer().getPlayerList().getPlayer(st.casterUuid);
		if (caster != null && target.isAlive() && target.level() == world
                &&!github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(caster,target)) {
			target.hurt(caster.damageSources().indirectMagic(caster, caster), (float) BAL.d("clone_damage", CLONE_DAMAGE));
			// 命中音效：哈???+ 挥击（定向，仅目标??
			playSoundAt(target, SoundEvents.CAT_HISS, target.getX(), target.getY(), target.getZ(), 1.2f, 0.9f);
			playSoundAt(target, SoundEvents.PLAYER_ATTACK_SWEEP, target.getX(), target.getY(), target.getZ(), 0.7f, 1.2f);
		}
		// 消散：烟??+ 目标头顶烟花（定向粒子，仅目标??
		spawnParticlesAt(target, ParticleTypes.CLOUD, cat.getX(), cat.getY(0.5), cat.getZ(), 10, 0.35, 0.35, 0.35);
		spawnParticlesAt(target, ParticleTypes.FIREWORK, cat.getX(), cat.getY(1.8), cat.getZ(), 3, 0.25, 0.1, 0.25);
		cat.discard();
		CATS.remove(cat.getUUID());
	}

	// ==================== 生成点采??====================

	/** 目标身???180??扇区找空气落点（碰撞盒无方??+ 向???? 格贴地）??*/
	private static Vec3 findAirPosBehind(ServerLevel world, LivingEntity target, double minDist, double maxDist,
	                                      int tries, double width, double height) {
		float behindYaw = target.getYHeadRot() + 180.0f;
		for (int i = 0; i < tries; i++) {
			float yaw = behindYaw + (world.random.nextFloat() - 0.5f) * 180.0f;
			double dist = minDist + world.random.nextDouble() * (maxDist - minDist);
			double cx = target.getX() - Math.sin(Math.toRadians(yaw)) * dist;
			double cz = target.getZ() + Math.cos(Math.toRadians(yaw)) * dist;
			Vec3 pos = tryGroundedAir(world, cx, target.getY(), cz, width, height);
			if (pos != null) return pos;
		}
		return null;
	}

	/** 某点周围找空气落点（环形随机采样）???*/
	private static Vec3 findAirPosNear(ServerLevel world, Vec3 center, double radius, int tries,
	                                    double width, double height) {
		for (int i = 0; i < tries; i++) {
			float ang = world.random.nextFloat() * (float) (Math.PI * 2);
			double r = 0.8 + world.random.nextDouble() * radius;
			double cx = center.x + Math.cos(ang) * r;
			double cz = center.z + Math.sin(ang) * r;
			Vec3 pos = tryGroundedAir(world, cx, center.y, cz, width, height);
			if (pos != null) return pos;
		}
		return null;
	}

	/** 校??(cx, cy, cz)：碰撞盒空间无方??+ 向???? 格贴地，返回最终落点??*/
	private static Vec3 tryGroundedAir(ServerLevel world, double cx, double cy, double cz,
	                                    double width, double height) {
		AABB box = new AABB(cx - width / 2, cy, cz - width / 2, cx + width / 2, cy + height, cz + width / 2);
		if (!world.noCollision(null, box)) return null;
		// 向下最???2 格找贴地（避免悬空??
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(Math.floor(cx), (int) Math.floor(cy), Math.floor(cz));
		int drop = 0;
		while (drop < 2 && world.getBlockState(mpos.below(drop + 1)).isAir()) {
			drop++;
		}
		if (drop >= 2) return null; // 悬空超???2 ????放弃该采样???
		return new Vec3(cx, cy - drop, cz);
	}

	// ==================== 查??/ 生命周???====================

	/** 服务端查询：被攻击实体是否幽灵苦力怕（tag 快路径）??*/
	public static boolean isGhostCreeperEntity(Entity e) {
		return e instanceof Creeper && e.getTags().contains("ssca_ghost_creeper");
	}

	/** ??tick 推进（由 SscAddonServerEvents 调用）：苦力怕引??到期、野猫追??扑咬?*/
	public static void tick(ServerPlayer player) {
		if (CREEPERS.isEmpty() && CATS.isEmpty()) return;
		if (!(player.level() instanceof ServerLevel world)) return;

		// 幽灵苦力怕：只由目标本人?tick 推进（去重??
		int creeperLife = BAL.i("creeper_life_ticks", CREEPER_LIFE_TICKS);
		Iterator<Map.Entry<UUID, CreeperState>> cit = CREEPERS.entrySet().iterator();
		while (cit.hasNext()) {
			Map.Entry<UUID, CreeperState> e = cit.next();
			if (!e.getValue().target().equals(player.getUUID())) continue;
			Entity creeper = world.getEntity(e.getKey());
			if (!(creeper instanceof Creeper gc)) {
				cit.remove();
				continue;
			}
			if (gc.tickCount >= creeperLife) {
				// 到期：无实伤爆除（???粒直发目标，不广播??
				cit.remove();
				playSoundAt(player, SoundEvents.GENERIC_EXPLODE, gc.getX(), gc.getY(), gc.getZ(), 1.0f, 1.0f);
				spawnParticlesAt(player, ParticleTypes.EXPLOSION_EMITTER, gc.getX(), gc.getY(0.5), gc.getZ(), 1, 0, 0, 0);
				gc.discard();
				continue;
			}
			// 最??1.2 秒：点燃引信（视觉闪白）
			if (gc.tickCount >= creeperLife - 24) {
				gc.setSwellDir(1);
			}
		}

		// 幽灵野猫：只由目标本人???tick 推??
		Iterator<Map.Entry<UUID, CatState>> it2 = CATS.entrySet().iterator();
		while (it2.hasNext()) {
			Map.Entry<UUID, CatState> e = it2.next();
			CatState st = e.getValue();
			if (!st.target.getUUID().equals(player.getUUID())) continue;
			Entity cat = world.getEntity(e.getKey());
			if (!(cat instanceof GhostCatEntity oc)) {
				it2.remove();
				continue;
			}
			tickGhostCat(world, st, oc);
		}
	}

	/** 目标断???出梦清理：清其名下幽灵登记??*/
    public static void clearFor(UUID targetUuid, MinecraftServer server) {
        java.util.Set<UUID> ghosts = new java.util.HashSet<>();
        CREEPERS.forEach((id, state) -> { if (targetUuid.equals(state.target())) ghosts.add(id); });
        CATS.forEach((id, state) -> { if (targetUuid.equals(state.target.getUUID())) ghosts.add(id); });
        for (UUID id : ghosts) {
            for (ServerLevel world : server.getAllLevels()) {
                Entity ghost = world.getEntity(id);
                if (ghost != null) ghost.discard();
            }
            CREEPERS.remove(id);
            CATS.remove(id);
        }
    }

    public static boolean ownsGhost(UUID id) {
        return CREEPERS.containsKey(id) || CATS.containsKey(id);
    }

	/** 玩家掉线：清状态（实体???tick 查不到时自然移除；clearAll 兜底?tag）??*/
	public static void onDisconnect(ServerPlayer player) {
        clearFor(player.getUUID(), player.getServer());
	}

	/** 服务器启??数据包重载：清空全部状态并击杀残留的幽灵实体??*/
	public static void clearAll(MinecraftServer server) {
		for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
			for (Entity e : world.getAllEntities()) {
				if (e instanceof Creeper && e.getTags().contains("ssca_ghost_creeper")) {
					e.discard();
				}
				if (e instanceof GhostCatEntity && e.getTags().contains("ssca_ghost_cat")) {
					e.discard();
				}
			}
		}
		CREEPERS.clear();
		CATS.clear();
	}

	/** 服务端事件注册（??SscAddonServerEvents 调用）：目标攻击幽灵苦力怕的判定?*/
	public static void registerEvents() {
		github.com.gengyoubo.sscfa.util.SscEvents.attackEntity((player, world, hand, entity, hitResult) -> {
			// AttackEntityCallback 注??PlayerEntity.attack，服务端触发（ServerPlayerEntityMixin 已核实??
			if (world.isClientSide || !(player instanceof ServerPlayer sp)) return net.minecraft.world.InteractionResult.PASS;
			if (isGhostCreeperEntity(entity)) {
				onGhostCreeperAttacked(sp, (Creeper) entity);
				return net.minecraft.world.InteractionResult.FAIL;
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
	}
}
