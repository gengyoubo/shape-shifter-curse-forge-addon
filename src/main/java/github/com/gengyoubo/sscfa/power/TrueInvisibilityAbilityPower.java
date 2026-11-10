package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.cooldown.SkillCastManager;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月影野猫主技能 {@code my_addon:true_invisibility}：真隐身。
 *
 * <p>按下后蓄力 1 秒（{@code pre_invisibility} 效果，到期由 {@code PreInvisibilityEffect} 转为真隐身），
 * 隐身持续 {@code duration} tick（装备隐身斗篷 +2 秒）。隐身中使用物品或挥手会破隐（碎玻璃声）；
 * 再按一次主动破隐（猫嘶声），获得 5 秒必定暴击与速度 II。冷却字段同统一技能（默认隐身结束才起算，
 * 装备斗篷用 {@code extra_cooldowns.cloak}）。被净化会立即中断。</p>
 *
 * <p>每个玩家的状态按 UUID 存在本类里，效果、法术、震慑冲刺通过静态入口访问；形态切换、离线时由
 * {@link #onRemoved} 清理。</p>
 */
public final class TrueInvisibilityAbilityPower implements SscPowerType {
	public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "true_invisibility");

	private static final int DEFAULT_DURATION = 100;

	/** 一个玩家的隐身状态。 */
	private static final class State {
		String skillId;
		int effectDuration = DEFAULT_DURATION;
		long activeCastId = -1;
		long coveredSpellCastId = -1;
		int gracePeriodTicks;
		int lastAmplifier;
		boolean wasInvisible;
		boolean wasUsingItem;
		boolean wasHandSwinging;
	}

	/** 当前拥有本能力的玩家。 */
	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	private static State state(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		State state = STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
		state.skillId = powerId.toString();
		state.effectDuration = FormPowerRuntime.intValue(data, "duration", DEFAULT_DURATION);
		return state;
	}

	/** 该实体当前是否拥有真隐身能力（即是否是野猫）。 */
	public static boolean holds(Entity entity) {
		return STATES.containsKey(entity.getUUID());
	}

	private static boolean hasInvisibilityCloak(LivingEntity entity) {
		return TrinketUtils.isWearing(entity, AddonItems.INVISIBILITY_CLOAK.get());
	}

	private static boolean isInvisible(LivingEntity entity) {
		return entity.hasEffect(AddonEffects.TRUE_INVISIBILITY.get());
	}

	private static boolean isPrecasting(LivingEntity entity) {
		return entity.hasEffect(AddonEffects.PRE_INVISIBILITY.get());
	}

	/** 真隐身持续时间（装备隐身斗篷 +2 秒）；实体没有本能力时返回 {@code fallback}。 */
	public static int getEffectDuration(LivingEntity entity, int fallback) {
		State state = STATES.get(entity.getUUID());
		if (state == null) return fallback;
		return hasInvisibilityCloak(entity) ? state.effectDuration + 40 : state.effectDuration;
	}

	// ===== 按键 =====

	@Override
	public void onKeyPressed(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		if (!BalanceIntegration.isPlayerReady(player)) return;
		if (!FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) return;
		State state = state(player, powerId, data);
		if (isInvisible(player)) {
			// 已隐身：再按主动破隐（猫嘶声）
			breakInvisibility(player, state, true);
		} else if (!isPrecasting(player) && SkillCooldowns.ready(player, state.skillId)
				&& SkillCooldowns.begin(player, state.skillId)) {
			state.activeCastId = SkillCooldowns.currentCastId(player, state.skillId);
			// 冻结本次斗篷档位；后续破隐不能重启 on_cast/on_release 的计时。
			SkillCooldowns.retune(player, state.skillId, hasInvisibilityCloak(player)
					? SkillCooldowns.extra(player, state.skillId, "cloak") : SkillCooldowns.cooldown(player, state.skillId));
			// 蓄力阶段
			if (!player.addEffect(new MobEffectInstance(AddonEffects.PRE_INVISIBILITY.get(), 20, 0, false, false, true))) {
				interruptCast(player, state);
			}
		}
	}

	// ===== tick =====

	@Override
	public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		State state = state(player, powerId, data);
		if (player.isDeadOrDying()) {
			interruptCast(player, state);
			return;
		}

		if (player.hasEffect(AddonEffects.PURIFIED.get())) {
			if (isPrecasting(player)) {
				player.removeEffect(AddonEffects.PRE_INVISIBILITY.get());
				player.removeEffect(MobEffects.INVISIBILITY);
				interruptCast(player, state);
			}
			if (isInvisible(player)) breakInvisibility(player, state, false);
			state.wasInvisible = false;
			return;
		}

		boolean invisible = isInvisible(player);
		boolean precasting = isPrecasting(player);

		// 蓄力被其他来源清除也必须结算，不能遗留门禁。
		if (!invisible && !precasting) applyUniversalCooldown(player, state);
		// 自然结束的音效；冷却结算本身是幂等的。
		if (state.wasInvisible && !invisible && !precasting && state.lastAmplifier == 0) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
		}

		if (!invisible) {
			state.wasUsingItem = false;
			state.wasHandSwinging = false;
			state.wasInvisible = false;
			state.gracePeriodTicks = 5;
			state.lastAmplifier = 0;
			return;
		}

		MobEffectInstance currentEffect = player.getEffect(AddonEffects.TRUE_INVISIBILITY.get());
		if (currentEffect != null) state.lastAmplifier = currentEffect.getAmplifier();
		boolean coveringSpell = state.coveredSpellCastId >= 0 && state.coveredSpellCastId == state.activeCastId
				&& state.lastAmplifier == 0;
		if (coveringSpell && currentEffect != null) {
			// 法术准备可能超过原本的隐身时长：临近到期才续，走原版效果同步；被净化移除后不会回填。
			if (currentEffect.getDuration() < 8) {
				player.addEffect(new MobEffectInstance(AddonEffects.TRUE_INVISIBILITY.get(), 40, 0, false, false, true));
			}
			MobEffectInstance vanilla = player.getEffect(MobEffects.INVISIBILITY);
			if (vanilla != null && vanilla.getDuration() < 8) {
				player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
			}
		}

		// 刚隐身的宽限期：只记录动作状态，避免宽限结束立刻破隐
		if (state.gracePeriodTicks > 0) {
			state.gracePeriodTicks--;
			state.wasUsingItem = player.isUsingItem();
			state.wasHandSwinging = player.swinging;
			state.wasInvisible = true;
			return;
		}

		if (player.getRandom().nextFloat() < 0.07f) {
			ParticleUtils.spawnParticles(player.serverLevel(), ParticleTypes.SQUID_INK,
					player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
					1, 0.3, 0.5, 0.3, 0.05);
		}

		// 开始使用物品或开始挥手会破隐
		boolean usingItem = player.isUsingItem();
		boolean handSwinging = player.swinging;
		boolean shouldBreak = (usingItem && !state.wasUsingItem) || (handSwinging && !state.wasHandSwinging);
		state.wasUsingItem = usingItem;
		state.wasHandSwinging = handSwinging;

		if (shouldBreak && !coveringSpell) {
			breakInvisibility(player, state, false);
			return;
		}
		state.wasInvisible = true;
	}

	// ===== 移除 =====

	/** 形态切换、离线：解除蓄力与隐身，正在进行的施放按冷却结算。 */
	@Override
	public void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		State state = STATES.remove(player.getUUID());
		if (state == null || state.activeCastId < 0) return;
		player.removeEffect(AddonEffects.PRE_INVISIBILITY.get());
		player.removeEffect(AddonEffects.TRUE_INVISIBILITY.get());
		player.removeEffect(MobEffects.INVISIBILITY);
		applyUniversalCooldown(player, state);
	}

	// ===== 外部入口 =====

	/** 蓄力效果成功转为真实隐身时调用（{@code PreInvisibilityEffect}）。 */
	public static void onInvisibilityReleased(LivingEntity entity) {
		State state = STATES.get(entity.getUUID());
		if (state == null || !(entity instanceof ServerPlayer player) || state.activeCastId < 0) return;
		SkillCastManager.get(player.serverLevel()).released(state.activeCastId, SkillCastManager.now(player));
		state.wasInvisible = true;
		state.lastAmplifier = 0;
		state.gracePeriodTicks = 5;
	}

	/** 被完全控制时中断蓄力与隐身。 */
	public static void interruptControl(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		if (state == null) return;
		if (isPrecasting(player)) {
			player.removeEffect(AddonEffects.PRE_INVISIBILITY.get());
			interruptCast(player, state);
		}
		breakInvisibility(player, state, false);
	}

	/** 隐身结束：只结算本次施放，重复破隐不能重置已起算的冷却。震慑冲刺破隐时也走这里。 */
	public static void applyUniversalCooldown(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		if (state != null) applyUniversalCooldown(player, state);
	}

	/** 一次被接受的法术施放可以保住这一次主技能隐身；返回被保住的施放 ID，不适用时返回 -1。 */
	public static long beginSpellCover(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		MobEffectInstance effect = player.getEffect(AddonEffects.TRUE_INVISIBILITY.get());
		if (state == null || state.activeCastId < 0 || effect == null || effect.getAmplifier() != 0
				|| !FormUtils.isForm(player, FormIdentifiers.WILD_CAT_SP)) return -1;
		state.coveredSpellCastId = state.activeCastId;
		return state.activeCastId;
	}

	public static void endSpellCover(ServerPlayer player, long castId) {
		State state = STATES.get(player.getUUID());
		if (state == null || castId < 0 || state.coveredSpellCastId != castId) return;
		state.coveredSpellCastId = -1;
		if (state.activeCastId == castId) breakInvisibility(player, state, false);
	}

	// ===== 实现 =====

	private static void interruptCast(ServerPlayer player, State state) {
		if (state.activeCastId < 0) return;
		SkillCastManager.get(player.serverLevel()).interrupt(state.activeCastId, SkillCastManager.now(player));
		state.activeCastId = -1;
	}

	private static void applyUniversalCooldown(ServerPlayer player, State state) {
		if (state.activeCastId < 0) return;
		state.coveredSpellCastId = -1;
		interruptCast(player, state);
	}

	/**
	 * 破隐并播放对应音效。
	 *
	 * @param byKey true = 再按技能键主动破隐（猫嘶声，获得增益）；false = 被动作打破（碎玻璃声）
	 */
	private static void breakInvisibility(ServerPlayer player, State state, boolean byKey) {
		state.coveredSpellCastId = -1;
		MobEffectInstance currentEffect = player.getEffect(AddonEffects.TRUE_INVISIBILITY.get());
		if (currentEffect == null) return;
		int currentAmp = currentEffect.getAmplifier();

		player.removeEffect(AddonEffects.TRUE_INVISIBILITY.get());
		player.removeEffect(MobEffects.INVISIBILITY);
		state.wasInvisible = false;

		ServerLevel world = player.serverLevel();
		if (byKey) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.CAT_HISS, SoundSource.PLAYERS, 1.0f, 1.0f);
			// 必定暴击与速度 II，持续 5 秒
			player.addEffect(new MobEffectInstance(AddonEffects.GUARANTEED_CRIT.get(), 100, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, true));
		} else {
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
		}

		// 只有主技能的隐身（等级 0）破隐才结算冷却；其它来源的短隐身（等级 1）不动冷却
		if (currentAmp == 0) applyUniversalCooldown(player, state);
	}
}
