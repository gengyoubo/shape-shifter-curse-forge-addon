package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.init.AddonItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.onixary.shapeShifterCurseForge.effect.TransformativeStatusEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一瓶药水分装为三支试管：原药水 NBT 原样保留，饮用时按 1/3 缩放（持续类时长、瞬时类强度）。
 * 未迁移部分：power 提升堆叠上限（ModifyPotionStackPower）、变形药水目标形态、月尘缓解数值（NeoRuntime）、
 * 喂食类效果的剂量 mixin；这些依赖 Apoli/剧情/法术系统，迁移后补回。
 */
public final class PotionTestTubes {
	public static final int PORTIONS = 3;
	public static final int DEFAULT_STACK_LIMIT = 3;
	private static final String BOTTLE_TICKS = "TestTubeBottleUseTicks";
	/** 变形药水的目标形态 NBT：一次性语义，不可分装。 */
	private static final String TARGET_FORM = "targetForm";
	/** 原版月尘瓶的饮用时长。 */
	private static final int MOON_DUST_BOTTLE_TICKS = 32;

	private PotionTestTubes() {}

	public static boolean isDivisible(MobEffect effect) {
		if (effect instanceof TransformativeStatusEffect) return false;
		return !effect.isInstantenous() || effect == MobEffects.HEAL || effect == MobEffects.HARM;
	}

	public static boolean isDivisible(ItemStack stack) {
		if (stack.isEmpty() || stack.getTag() != null && stack.getTag().contains(TARGET_FORM)) return false;
		return PotionUtils.getMobEffects(stack).stream().allMatch(effect -> isDivisible(effect.getEffect()));
	}

	public static boolean isBottle(ItemStack stack) {
		if (stack.is(AddonItems.MOON_DUST_BOTTLE.get())) return true;
		if (!(stack.getItem() instanceof PotionItem) || stack.getItem() instanceof SplashPotionItem
				|| stack.getItem() instanceof LingeringPotionItem || stack.is(AddonItems.TEST_TUBE_POTION.get())) return false;
		var potion = PotionUtils.getPotion(stack);
		return potion != AddonEffects.MOON_DUST_TUBE.get() && potion != AddonEffects.UNIVERSAL_ENERGY_POTION.get()
				&& isDivisible(stack);
	}

	public static ItemStack divide(ItemStack bottle) {
		if (!isBottle(bottle)) return ItemStack.EMPTY;
		boolean moon = bottle.is(AddonItems.MOON_DUST_BOTTLE.get());
		ItemStack out = new ItemStack(moon ? AddonItems.MOON_DUST_VIAL.get() : AddonItems.TEST_TUBE_POTION.get(), PORTIONS);
		if (bottle.hasTag()) out.setTag(bottle.getTag().copy());
		out.getOrCreateTag().putInt(BOTTLE_TICKS, moon ? MOON_DUST_BOTTLE_TICKS : bottle.getUseDuration());
		if (moon) {
			out.getOrCreateTag().remove("Potion");
			// 旧月尘瓶没有普通药水载荷，不能叠加两份缓解。
			out.getOrCreateTag().remove("CustomPotionEffects");
			if (!out.getTag().contains("CustomPotionColor")) out.getTag().putInt("CustomPotionColor", AddonEffects.MOON_DUST_COLOR);
		}
		return out;
	}

	public static int drinkTicks(int bottleTicks) {
		return Math.max(1, (int) Math.ceil(Math.max(1, bottleTicks) * .4));
	}

	/** 持续类效果时长按份数向上取整缩放（含隐藏的较弱效果链）；瞬时类原样返回，由饮用时按剂量缩放。 */
	public static List<MobEffectInstance> doseEffects(ItemStack stack) {
		List<MobEffectInstance> result = new ArrayList<>();
		for (var effect : PotionUtils.getMobEffects(stack)) {
			if (effect.getEffect().isInstantenous()) {
				result.add(new MobEffectInstance(effect));
				continue;
			}
			CompoundTag nbt = effect.save(new CompoundTag());
			for (CompoundTag part = nbt; ; part = part.getCompound("HiddenEffect")) {
				int duration = part.getInt("Duration");
				if (duration > 0) part.putInt("Duration", Math.max(1, (int) (((long) duration + PORTIONS - 1) / PORTIONS)));
				if (!part.contains("HiddenEffect", 10)) break;
			}
			var scaled = MobEffectInstance.load(nbt);
			if (scaled != null) result.add(scaled);
		}
		return result;
	}

	public static final class FilledTube extends PotionItem {
		public FilledTube(Item.Properties properties) {
			super(properties);
		}

		@Override
		public ItemStack getDefaultInstance() {
			var result = divide(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING));
			result.setCount(1);
			return result;
		}

		@Override
		public int getUseDuration(ItemStack stack) {
			int ticks = stack.hasTag() && stack.getTag().contains(BOTTLE_TICKS) ? stack.getTag().getInt(BOTTLE_TICKS) : 32;
			return drinkTicks(ticks);
		}

		@Override
		public Component getName(ItemStack stack) {
			var potion = PotionUtils.getPotion(stack);
			if (potion == AddonEffects.MOON_DUST_TUBE.get()) return Component.translatable("item.ssc_addon.moon_dust_vial");
			if (potion == Potions.WATER) return Component.translatable("item.ssc_addon.test_tube_water");
			if (potion == Potions.EMPTY) return Component.translatable("item.ssc_addon.test_tube_potion_empty");
			ItemStack bottle = new ItemStack(Items.POTION);
			if (stack.hasTag()) bottle.setTag(stack.getTag().copy());
			return Component.translatable("item.ssc_addon.test_tube_potion", bottle.getHoverName());
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			// 原版药水瓶对泥土使用会变成泥巴并返还玻璃瓶；试管只能喝。
			return InteractionResult.PASS;
		}

		@Override
		public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
			if (stack.isEmpty() || !stack.is(this)) return stack;
			Player player = user instanceof Player p ? p : null;
			if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
			if (!world.isClientSide) {
				for (var effect : doseEffects(stack)) {
					if (effect.getEffect().isInstantenous()) {
						effect.getEffect().applyInstantenousEffect(player, player, user, effect.getAmplifier(), 1.0 / PORTIONS);
					} else {
						user.addEffect(effect);
					}
				}
			}
			if (player != null) player.awardStat(Stats.ITEM_USED.get(this));
			user.gameEvent(GameEvent.DRINK);
			if (player == null || !player.getAbilities().instabuild) {
				stack.shrink(1);
				if (stack.isEmpty()) return new ItemStack(AddonItems.TEST_TUBE.get());
				if (player != null) player.getInventory().placeItemBackInInventory(new ItemStack(AddonItems.TEST_TUBE.get()));
			}
			return stack;
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> lines, TooltipFlag context) {
			ItemStack shown = new ItemStack(Items.POTION);
			PotionUtils.setCustomEffects(shown, doseEffects(stack));
			PotionUtils.addPotionTooltip(shown, lines, 1f);
			lines.add(Component.translatable("tooltip.ssc_addon.test_tube_dose").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("tooltip.ssc_addon.test_tube_stack").withStyle(ChatFormatting.GRAY));
			if (PotionUtils.getPotion(stack) == Potions.WATER && PotionUtils.getMobEffects(stack).isEmpty()) {
				lines.add(Component.translatable("tooltip.ssc_addon.test_tube_water").withStyle(ChatFormatting.GRAY));
			}
		}
	}
}
