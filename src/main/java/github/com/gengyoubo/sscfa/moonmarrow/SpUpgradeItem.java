package github.com.gengyoubo.sscfa.moonmarrow;

import github.com.gengyoubo.sscfa.util.AdvancementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

/**
 * 月髓十字环：诅咒之月夜晚、处于可进化形态时使用，进化为对应的 SP 形态。
 * 形态映射与剧情/进化挂钩见 {@link MoonMarrowRing}。
 */
public class SpUpgradeItem extends Item {
	public static final ResourceKey<DamageType> CURSED_EROSION = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("my_addon", "cursed_erosion"));
	public static final ResourceKey<DamageType> CURSED_BURST = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("my_addon", "cursed_burst"));
	private static final ResourceLocation ADV_SUCCESS = new ResourceLocation("ssc_addon", "tonight_moon_beautiful");
	private static final ResourceLocation ADV_BOOM = new ResourceLocation("ssc_addon", "boom_boom_boom");
	private static final String RED_EXPIRE_TAG = "ssc_addon_red_expire:";
	private static final int RED_DURATION_TICKS = 12000;

	public SpUpgradeItem(Properties properties) {
		super(properties);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide && entity instanceof ServerPlayer player) {
			use(stack, level, player);
		}
		return stack;
	}

	private void use(ItemStack stack, Level level, ServerPlayer player) {
		// 剧情「月痕之力」「潮汐之灵」的免费变回由剧情系统注册拦截器（不消耗）
		if (MoonMarrowRing.runInterceptors(player)) return;
		// 正在变形演出中：不重复触发，也不消耗
		if (MoonMarrowForms.isTransforming(player)) return;

		ResourceLocation currentId = MoonMarrowForms.currentFormId(player);
		ResourceLocation targetId = MoonMarrowRing.targetOf(currentId);
		// SSCA 进化路线起点形态：必须先解锁全部分支才能继续进化
		if (targetId != null && !MoonMarrowRing.passesEvolutionGate(player)) {
			fail(player, level, "message.ssc_addon.evolution.fail.branches_locked");
			return;
		}

		boolean cursedMoon = MoonMarrowForms.isCursedMoonNight(level);
		boolean validForm = targetId != null;
		boolean alreadySp = MoonMarrowRing.isUpgradeTarget(currentId);

		if (cursedMoon && alreadySp) {
			// 致命共鸣：已是 SP 又在诅咒之月下使用
			fail(player, level, "message.ssc_addon.evolution.fail.already_sp");
			level.explode(player, player.getX(), player.getY(), player.getZ(), 6.0f, Level.ExplosionInteraction.BLOCK);
			player.hurt(damage(level, CURSED_BURST), 100000.0f);
			consume(stack, player);
			AdvancementUtils.grant(player, ADV_BOOM);
		} else if (validForm && cursedMoon) {
			evolve(stack, level, player, currentId, targetId);
		} else {
			consume(stack, player);
			if (!cursedMoon) {
				// 无诅咒之月：不造成伤害
				fail(player, level, validForm || alreadySp
						? "message.ssc_addon.evolution.fail.no_moon"
						: "message.ssc_addon.evolution.fail.both");
			} else {
				// 诅咒之月下但形态不对
				fail(player, level, "message.ssc_addon.evolution.fail.bad_form");
				player.hurt(damage(level, CURSED_EROSION), 10.0f);
				player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 20, 1));
				player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 20, 1));
				player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 20, 1));
			}
		}
	}

	private void evolve(ItemStack stack, Level level, ServerPlayer player, ResourceLocation currentId, ResourceLocation targetId) {
		// 动画开始前确定唯一最终目标
		ResourceLocation finalTarget = targetId;
		boolean temporaryRed = false;
		if (MoonMarrowRing.FAMILIAR_FOX_SP.equals(targetId)
				&& MoonMarrowRing.FAMILIAR_FOX_3.equals(currentId)
				&& MoonMarrowForms.isRegistered(MoonMarrowRing.FAMILIAR_FOX_RED)
				&& level.random.nextFloat() < MoonMarrowRing.redVariantChance()) {
			finalTarget = MoonMarrowRing.FAMILIAR_FOX_RED;
			temporaryRed = true;
		}

		// 目标 SP 形态尚未迁移/注册：提示且不消耗
		if (!MoonMarrowForms.isRegistered(finalTarget)) {
			fail(player, level, "message.ssc_addon.evolution.fail.form_unavailable");
			return;
		}
		if (!MoonMarrowForms.startTransform(player, finalTarget)) return;

		if (temporaryRed) {
			for (String tag : new HashSet<>(player.getTags())) {
				if (tag.startsWith(RED_EXPIRE_TAG)) player.removeTag(tag);
			}
			player.addTag(RED_EXPIRE_TAG + (level.getGameTime() + RED_DURATION_TICKS));
			player.displayClientMessage(Component.translatable("message.ssc_addon.red_transformation_special").withStyle(ChatFormatting.GREEN), false);
		}

		player.displayClientMessage(Component.translatable("message.ssc_addon.evolution.success").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC), false);
		playSound(level, player, SoundEvents.PLAYER_LEVELUP);
		consume(stack, player);
		AdvancementUtils.grant(player, ADV_SUCCESS);
		ResourceLocation formAdvancement = MoonMarrowRing.formAdvancement(targetId);
		if (formAdvancement != null) AdvancementUtils.grant(player, formAdvancement);
	}

	private static void fail(ServerPlayer player, Level level, String messageKey) {
		player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), false);
		playSound(level, player, SoundEvents.FIRE_EXTINGUISH);
	}

	private static void playSound(Level level, Player player, SoundEvent sound) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private static void consume(ItemStack stack, Player player) {
		if (!player.getAbilities().instabuild) stack.shrink(1);
	}

	private static DamageSource damage(Level level, ResourceKey<DamageType> type) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.translatable("item.ssc_addon.sp_upgrade_thing.tooltip"));
	}
}
