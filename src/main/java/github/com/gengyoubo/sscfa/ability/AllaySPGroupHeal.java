package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SP悦灵群体治疗 - 白名单治疗系?
 * <p>
 * 当JSON蓄力完成后设?heal_execute 资源??
 * 此类的tick检测到后执行白名单过滤治疗，然后重置为0?
 * <p>
 * 白名单存储在SP悦灵玩家?command tags 中，
 * 格式: "ssc_allay_wl:<目标玩家UUID>"
 */
public class AllaySPGroupHeal {

	public static final String WHITELIST_TAG_PREFIX = "ssc_allay_wl:";
	private static final ResourceLocation HEAL_EXECUTE_ID = FormIdentifiers.ALLAY_GROUP_HEAL_EXECUTE;
	private static final ResourceLocation CHARGING_STATE_ID = new ResourceLocation("my_addon", "form_allay_sp_group_heal_charging_state");
	private static final ResourceLocation CHARGE_TIMER_ID = new ResourceLocation("my_addon", "form_allay_sp_group_heal_charge_timer");
	private static final String SKILL_ID = "my_addon:form_allay_sp_group_heal_key_activation";
	private static final ResourceLocation SOLO_DAMAGE_TIMER_ID = FormIdentifiers.ALLAY_GROUP_HEAL_SOLO_DAMAGE_TIMER;
	// 群疗参数：默认与注释一致；运行时从 balance 快照读取（abilities.allay_sp_group_heal?
	private static final double HEAL_RADIUS = 20.0;
	private static final int RESISTANCE_TICKS = 200;
	private static final int SOLO_BLESSING_TICKS = 400;
	// 群体治疗：每个目标回血 = 最大生命?5%，额外给 最大生命?0% 的黄心，持续30?
	private static final float HEAL_PERCENT = 0.75f;
	private static final float ABSORPTION_PERCENT = 0.5f;
	private static final int HEAL_ABSORPTION_TICKS = 600;

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.allay_sp_group_heal");

	private AllaySPGroupHeal() {
		throw new UnsupportedOperationException("This class cannot be instantiated.");
	}

	/**
	 * 每tick检查是否需要执行治?
	 */
	public static void tick(ServerPlayer player) {
		// 廉价形态门控前置：?SP 悦灵玩家直接跳过，避免对全员?tick ?Apoli 资源查询
		// （getResourceValue 每次都要遍历玩家全部 power 列表，非持有?100% 浪费?
		if (!github.com.gengyoubo.sscfa.util.FormUtils.isForm(player, FormIdentifiers.ALLAY_SP)) {
			return;
		}
		if (SkillBlocker.isSkillBlocked(player, "allay", "group_heal")) {
			return;
		}
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			onPurified(player);
			return;
		}
		int healExecute = getResourceValue(player, HEAL_EXECUTE_ID);
		if (healExecute != 1) return;

		// 立即重置触发器（不同步，避免重置飘浮计时器）
		setResourceValueNoSync(player, HEAL_EXECUTE_ID, 0);

		// 执行白名单过滤治?
		executeWhitelistHeal(player);
	}

	/** 净化只中断群体治疗；不改变其它悦灵群体净化的蓄力资源?*/
	public static void onPurified(ServerPlayer player) {
		long castId = stopCharge(player);
		if (castId >= 0) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.interruptedFull(player, SKILL_ID, castId);
	}

	public static void interruptControl(ServerPlayer player) {
		long castId = stopCharge(player);
		if (castId >= 0) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, castId);
	}

	private static long stopCharge(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.util.FormUtils.isForm(player, FormIdentifiers.ALLAY_SP)) return -1;
		if (getResourceValue(player, CHARGING_STATE_ID) <= 0 && getResourceValue(player, HEAL_EXECUTE_ID) <= 0) return -1;
		long castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		PowerUtils.setResourceValueAndSync(player, CHARGING_STATE_ID, 0);
		PowerUtils.setResourceValueAndSync(player, CHARGE_TIMER_ID, 0);
		PowerUtils.setResourceValueAndSync(player, HEAL_EXECUTE_ID, 0);
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
		return castId;
	}

	/**
	 * 执行白名单过滤的群体治疗
	 * 白名单为空时治疗所有活体（原版行为）；有白名单时只治疗白名单内的玩家及其召唤物
	 */
	private static void executeWhitelistHeal(ServerPlayer allayPlayer) {
		ServerLevel world = (ServerLevel) allayPlayer.level();
		boolean soloHeal = !hasOtherHealablePlayer(allayPlayer, world);
		// balance 运行时参数（同方法多次使用，先取局部变量）
		double healRadius = BAL.d("heal_radius", HEAL_RADIUS);
		double healRadiusSq = healRadius * healRadius;
		int resistanceTicks = BAL.i("resistance_ticks", RESISTANCE_TICKS);

		// 治疗自身
		if (soloHeal) {
			applySoloBlessing(allayPlayer);
		} else {
			healTarget(allayPlayer);
			allayPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceTicks, 0, false, true, true));
		}
		spawnHealParticles(world, allayPlayer);
		// 只有SP悦灵自己能听?
		allayPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);

		// 获取范围内所有活?
		AABB box = allayPlayer.getBoundingBox().inflate(healRadius);
		List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
					e -> e != allayPlayer && e.isAlive() && e.distanceToSqr(allayPlayer) <= healRadiusSq);

		for (LivingEntity entity : entities) {
			// 统一?WhitelistUtils.isBuffTarget，同时遵从服务端 whitelistEnabled 总开?
			if (WhitelistUtils.isBuffTarget(allayPlayer, entity)) {
				healTarget(entity);
				entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceTicks, 0, false, true, true));
				spawnHealParticles(world, entity);
				// 播放声音：治疗者听见私有声音，其他人听见空间声?
				allayPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
				world.playSound(allayPlayer, entity.getX(), entity.getY(), entity.getZ(),
						SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
			}
		}
	}

	private static boolean hasOtherHealablePlayer(ServerPlayer allayPlayer, ServerLevel world) {
		double healRadius = BAL.d("heal_radius", HEAL_RADIUS);
		double healRadiusSq = healRadius * healRadius;
		AABB box = allayPlayer.getBoundingBox().inflate(healRadius);
		List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, box,
				player -> player != allayPlayer
						&& player.isAlive()
						&& !player.isSpectator()
						&& player.distanceToSqr(allayPlayer) <= healRadiusSq
						&& WhitelistUtils.isBuffTarget(allayPlayer, player));
		return !players.isEmpty();
	}

	private static void applySoloBlessing(ServerPlayer allayPlayer) {
		int soloBlessingTicks = BAL.i("solo_blessing_ticks", SOLO_BLESSING_TICKS);
		healTarget(allayPlayer);
		allayPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, soloBlessingTicks, 1, false, true, true));
		allayPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, soloBlessingTicks, 1, false, true, true));
		setResourceValue(allayPlayer, SOLO_DAMAGE_TIMER_ID, soloBlessingTicks);
	}

	/** 治疗单个目标：回血=最大生命?5%，并给予 最大生命?0% 的黄心（持续30秒）?*/
	private static void healTarget(LivingEntity entity) {
		float maxHp = entity.getMaxHealth();
		entity.heal(maxHp * (float) BAL.d("heal_percent", HEAL_PERCENT));
        float absorption = maxHp * (float) BAL.d("absorption_percent", ABSORPTION_PERCENT);
        if (absorption > 0) {
            // Vanilla absorption grants 4 HP per amplifier level; round to its available granularity.
            int amplifier = Math.max(0, Math.round(absorption / 4.0f) - 1);
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    BAL.i("heal_absorption_ticks", HEAL_ABSORPTION_TICKS), amplifier, false, true, true));
        }
	}

	/**
	 * 将目标实体的所有者（如果是宠物）或实体本身（如果是普通生?玩家）加入白名单
	 */
	public static void addToWhitelist(ServerPlayer allayPlayer, LivingEntity target) {
		if (target instanceof net.minecraft.world.entity.player.Player) {
			// 玩家：只?player 前缀
			allayPlayer.getTags().add(WHITELIST_TAG_PREFIX + target.getUUID().toString());
		} else {
			// 任何非玩家生物（含宠物）：双?player 前缀（兼?isInWhitelist 判定? mob 前缀（GUI 用，带物种信息）
			// 注意：白名单玩家的所有宠物已通过"主人 UUID ?player 白名?路径默认受保护，
			// 这里写宠物自?UUID 仅在该宠物被显式标记时生效，不影响主人的其它宠物?
			String uuidStr = target.getUUID().toString();
			allayPlayer.getTags().add(WHITELIST_TAG_PREFIX + uuidStr);
			net.minecraft.resources.ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
			String mobTag = github.com.gengyoubo.sscfa.util.WhitelistUtils.WHITELIST_MOB_TAG_PREFIX
					+ uuidStr + ":" + typeId.getNamespace() + ":" + typeId.getPath();
			allayPlayer.getTags().add(mobTag);
		}
	}

	/**
	 * 将目标实体的所有者（如果是宠物）或实体本身（如果是普通生?玩家）从白名单移?
	 */
	public static void removeFromWhitelist(ServerPlayer allayPlayer, LivingEntity target) {
		// 始终?target 自身 UUID 移除（与 addToWhitelist 对称?
		allayPlayer.getTags().remove(WHITELIST_TAG_PREFIX + target.getUUID().toString());
		// 同时清除生物白名单上?mob 前缀 tag（包含剩余的 :ns:path 变体?
		github.com.gengyoubo.sscfa.util.WhitelistUtils.removeMobFromWhitelist(allayPlayer, target.getUUID());
	}

	/**
	 * 检查UUID是否在白名单?
	 */
	public static boolean isInWhitelist(Set<String> allayTags, UUID targetUuid) {
		return allayTags.contains(WHITELIST_TAG_PREFIX + targetUuid.toString());
	}

	/**
	 * 便捷方法：检查一个实体是否在玩家的白名单中（供唱片机等外部系统使用）
	 * 白名单为空时返回true（允许所有玩家和友好生物?
	 */
	public static boolean isInWhitelist(ServerPlayer allayPlayer, LivingEntity entity) {
		// 统一?WhitelistUtils.isBuffTarget，遵?whitelistEnabled 总开?
		return WhitelistUtils.isBuffTarget(allayPlayer, entity);
	}

	/**
	 * 在实体位置生成爱心粒?
	 */
	private static void spawnHealParticles(ServerLevel world, LivingEntity entity) {
		github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(world, ParticleTypes.HEART,
				entity.getX(), entity.getY() + entity.getBbHeight() + 0.5, entity.getZ(),
				5, 0.3, 0.3, 0.3, 0.01);
	}

	// ===== 白名单管理方法（?GUI / 网络层调用） =====
	// 旧的 SP-only 玩家重载（add/remove/clear）已?/ssc_addon whitelist 指令一并删除?

	/**
	 * 从白名单按UUID移除
	 */
	public static boolean removeFromWhitelistByUuid(ServerPlayer allayPlayer, UUID targetUuid) {
		String tag = WHITELIST_TAG_PREFIX + targetUuid.toString();
		return allayPlayer.getTags().remove(tag);
	}

	/**
	 * 获取白名单中的所有UUID
	 */
	public static java.util.List<UUID> getWhitelistUuids(ServerPlayer allayPlayer) {
		java.util.List<UUID> uuids = new java.util.ArrayList<>();
		for (String tag : allayPlayer.getTags()) {
			if (tag.startsWith(WHITELIST_TAG_PREFIX)) {
				try {
					uuids.add(UUID.fromString(tag.substring(WHITELIST_TAG_PREFIX.length())));
				} catch (IllegalArgumentException e) {
					// 无效UUID，跳?
				}
			}
		}
		return uuids;
	}

		// ===== Apoli资源读写工具 =====

	private static int getResourceValue(ServerPlayer player, ResourceLocation resourceId) {
		return PowerUtils.getResourceValue(player, resourceId);
	}

	private static void setResourceValue(ServerPlayer player, ResourceLocation resourceId, int value) {
		PowerUtils.setResourceValueAndSync(player, resourceId, value);
	}

		/**
	 * 设置资源值但不同步到客户端，避免重置飘浮等power的内部计时器
	 * 仅用于不需要客户端感知的内部触发器（如 heal_execute?
	 */
	private static void setResourceValueNoSync(ServerPlayer player, ResourceLocation resourceId, int value) {
		PowerUtils.setResourceValue(player, resourceId, value);
	}
}
