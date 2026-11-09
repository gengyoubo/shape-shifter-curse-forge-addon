package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 「击杀赋能」的释放侧（SP使??/ 红堕落使魔，服务端）?
 *
 * <p>赋能激活期间由原生主动技能入口在服务端选择?次技能：
 * <ul>
 *   <li>主技?= 赋能火环：全周期免蓝，与正常火环互斥（状态机??{@link #tryCastRing}）??/li>
 *   <li>次技?= SP 吐息?Red 火球：免蓝、免普通冷却释放一次?/li>
 * </ul>
 * 释放即消耗一次赋能机会，火环持续时间独立计算??
 */
public final class KillEmpowerCast {

	private KillEmpowerCast() {}

	// 赋能吐息参数：默认??power JSON 常规吐息一致?? ??/ 6 ??×非玩家倍??/ 灼??5s）；运行时??balance 快照读取（abilities.kill_empower_cast??
	private static final float BREATH_DISTANCE = 8.0f;
	private static final float BREATH_DAMAGE = 6.0f;
	private static final float BREATH_NON_PLAYER_MULT_SP = 3.0f;   // 默认；SP使??吐息对怪倍率（与正常?JSON ?? 对齐?
	private static final float BREATH_NON_PLAYER_MULT_RED = 1.5f;  // 默认；红堕??吐息对怪倍率（同????.5??
	private static final int BURN_DURATION = 100;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.kill_empower_cast");

	/** 赋能火环伤害跳（KillEmpowerManager ??16t 调用一次；与正常环 effects_loop 同节奏同半径）?*/
	public static void tickEmpowerRing(ServerPlayer player) {
		ServerLevel world = (ServerLevel) player.level();
		boolean redForm = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
		// 蓝火护符修正（每跳实时读取，??摘即时生效）：伤??+4、范?-40%
		boolean amulet = TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.BLUE_FIRE_AMULET.get());
		final float base;
		if (redForm) base = amulet ? 9.0f : 5.0f; else base = amulet ? 8.0f : 4.0f;	// Red 环基??5??；SP 环基??4??（护?+4??
		final double ringRadius = amulet ? 3.6 : 6.0;
		AABB box = player.getBoundingBox().inflate(ringRadius);
		ResourceKey<DamageType> genericKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "generic"));
		world.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator()).forEach(e -> {
			if (WhitelistUtils.isProtected(player, e)) return;
			if (player.distanceToSqr(e) > ringRadius * ringRadius) return;
			float dmg = e instanceof net.minecraft.world.entity.player.Player ? base : base * 2.0f;	// 对非玩家（NPC/怪物??
			e.invulnerableTime = 0;	// 命中前清：不被普攻无敌帧?
			if (e.hurt(new net.minecraft.world.damagesource.DamageSource(e.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(genericKey), player, player), dmg)) {
				e.invulnerableTime = 0;	// 命中后清：不留无敌帧反吞普??
				e.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.FOX_FIRE_BURN.get(), redForm ? 80 : 60, 0), player);
				KillEmpowerManager.trackBurn(player, e, redForm ? 80 : 60, false);
				e.setRemainingFireTicks(redForm ? 80 : 60);
			}
		});
	}

	/** 判断正常火环（非赋能环）是否正在释放：正常环开?toggle_state=1；赋能环不??toggle。客户端可读?*/
	public static boolean isNormalRingActive(net.minecraft.world.entity.player.Player player) {
		boolean red = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
		// 4 ??Identifier 为静态常??
		ResourceLocation toggleId = red ? TOGGLE_RED : TOGGLE_SP;
		ResourceLocation amuletToggleId = red ? AMULET_TOGGLE_RED : AMULET_TOGGLE_SP;
		return PowerUtils.getClientResourceValue(player, toggleId) == 1
				|| PowerUtils.getClientResourceValue(player, amuletToggleId) == 1;
	}

	/** 火??toggle 资??id 常量（红/SP ??普??护符，避??HUD 每??new Identifier）??*/
	private static final ResourceLocation TOGGLE_SP = new ResourceLocation("my_addon", "form_familiar_fox_sp_blue_fire_ring_toggle_state");
	private static final ResourceLocation TOGGLE_RED = new ResourceLocation("my_addon", "form_familiar_fox_red_blue_fire_ring_toggle_state");
	private static final ResourceLocation AMULET_TOGGLE_SP = new ResourceLocation("my_addon", "form_familiar_fox_sp_blue_fire_ring_amulet_toggle_state");
	private static final ResourceLocation AMULET_TOGGLE_RED = new ResourceLocation("my_addon", "form_familiar_fox_red_blue_fire_ring_amulet_toggle_state");

	/** 客户端按主技能键（赋能态）。返?true 表示已处理??*/
	public static boolean tryCastRing(ServerPlayer player) {
		if (!KillEmpowerManager.isEmpowerForm(player)) return false;
		KillEmpowerState state = KillEmpowerManager.readState(player);
		if (!state.hasReady() && !state.hasRing()) return false;

		ServerLevel world = (ServerLevel) player.level();
		boolean redForm = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);

		if (state.hasRing()) {
			player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			KillEmpowerManager.writeState(player, state.stopRing());
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			return true;
		}

		// STATE_READY：正常环（含护符环）开????先关环进对??CD（赋能保留，遮罩仍隐藏）；否则直接开赋能??
		boolean normalRingOn = player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
		if (normalRingOn) {
			player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			String ringPrefix = (redForm ? "form_familiar_fox_red" : "form_familiar_fox_sp") + "_blue_fire_ring";
			boolean amuletRingOn = PowerUtils.getResourceValue(player, new ResourceLocation("my_addon", ringPrefix + "_amulet_toggle_state")) == 1;
			String ringPower = ringPrefix + (amuletRingOn ? "_amulet" : "");
			PowerUtils.setResourceValueAndSync(player, new ResourceLocation("my_addon", ringPower + "_toggle_state"), 0);
			// 关哪套环就按该火??JSON ??cooldown 起??
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, "my_addon:" + ringPower + "_key_activation");
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			return true; // 本次按键只关环；赋能仍在（下次按键开赋能环??
		}

		// 蓝火护符修正：持续时??-15%（释放瞬间读取；SP 232??97、Red 280??38??
		boolean amulet = TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.BLUE_FIRE_AMULET.get());
		int fullRingTicks = redForm ? KillEmpowerManager.ringTicksRed() : KillEmpowerManager.ringTicksSp();
		if (amulet) fullRingTicks = Math.round(fullRingTicks * 0.85f);
		player.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get(), fullRingTicks, 0, true, false, true));
		MobEffectInstance ringEffect = player.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
		if (ringEffect == null || ringEffect.getDuration() <= 0) return true;
		PowerUtils.setResourceValueAndSync(player, FormIdentifiers.EMPOWER_RING_DURATION, ringEffect.getDuration());
		KillEmpowerManager.writeState(player, state.startRing(ringEffect.getDuration()));
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.0f);
		return true;
	}

	/** 客户端按次技能键（赋能态）。返?true 表示已处理（赋能吐息已释放）??*/
	public static boolean tryCastBreath(ServerPlayer player) {
		if (!KillEmpowerManager.isEmpowerForm(player)) return false;
		KillEmpowerState state = KillEmpowerManager.readState(player);
		if (!state.hasReady()) return false;

		ServerLevel world = (ServerLevel) player.level();
		boolean redForm = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
		// balance 运行时参数（同方法多次使用，先取局部变量）
		float nonPlayerMult = redForm
				? (float) BAL.d("breath_mult_red", BREATH_NON_PLAYER_MULT_RED)
				: (float) BAL.d("breath_mult_sp", BREATH_NON_PLAYER_MULT_SP);
		double breathDist = BAL.d("breath_distance", BREATH_DISTANCE);
		double breathDistSq = breathDist * breathDist;
		float breathDamage = (float) BAL.d("breath_damage", BREATH_DAMAGE);
		int burnDuration = BAL.i("burn_duration", BURN_DURATION);

		// 消耗赋能（标记赋能击????本次吐息的击杀不触发新赋能?
		KillEmpowerManager.writeState(player, state.consume());

		if (redForm) {
			github.com.gengyoubo.sscfa.action.SscAddonActions.launchFoxFireball(player, true);
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 1.0f, 1.0f);
			return true;
		}

		// 免费释放：不?mana、不检?CD/mana 条件；??CD 若在走不受影响（不重置）
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		ResourceKey<DamageType> magicKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "magic"));
		AABB box = player.getBoundingBox().inflate(breathDist).expandTowards(look.scale(breathDist));
		world.getEntitiesOfClass(LivingEntity.class, box, t -> t != player && t.isAlive() && !t.isSpectator()).forEach(t -> {
			if (WhitelistUtils.isProtected(player, t)) return;
			Vec3 toT = t.position().add(0, t.getBbHeight() / 2.0, 0).subtract(eye).normalize();
			if (look.dot(toT) > 0.8 && player.distanceToSqr(t) < breathDistSq) {
				float dmg = t instanceof net.minecraft.world.entity.player.Player ? breathDamage : breathDamage * nonPlayerMult;
				t.invulnerableTime = 0;	// 命中前清：不被普攻无敌帧?
				if (t.hurt(new net.minecraft.world.damagesource.DamageSource(t.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(magicKey), player, player), dmg)) {
					t.invulnerableTime = 0;	// 命中后清：不留无敌帧反吞普??
				t.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.FOX_FIRE_BURN.get(), burnDuration, 0), player);
				KillEmpowerManager.trackBurn(player, t, burnDuration, false);
					if (t instanceof github.com.gengyoubo.sscfa.util.SscIgnitedEntityAccessor acc) {
						acc.sscAddon$setIgniterUuid(player.getUUID());
					}
				}
			}
		});
		// 表现复刻 power JSON：前方多层魂火粒??+ 火焰充能/狐狸叫音??
		for (int d = 1; d <= 5; d++) {
			double spread = 0.2 * d;
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(world, player, ParticleTypes.SOUL_FIRE_FLAME,
					player.getX() + look.x * d, player.getY() + 1.2, player.getZ() + look.z * d,
					4 + d * 3, spread, 0.4 + spread * 0.5, spread, 0.04);
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 1.0f, 1.0f);
		return true;
	}
}
