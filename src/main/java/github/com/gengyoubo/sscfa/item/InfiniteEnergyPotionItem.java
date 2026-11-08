package github.com.gengyoubo.sscfa.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.registry.ModPotions;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 无限压缩能量药水：使用后进入「空瓶」自充能状态，充能完成后自动恢复满瓶可再次使用。
 * 充能基于世界游戏时间戳（NBT 记录充满时刻），任意位置自动充能、无需 tick，跨维度一致。
 * 效果与压缩能量药水（feed_potion）完全一致：饮用施加 FEED_EFFECT；喷溅/滞留复用原版投掷药水行为。
 * 特意不继承 PotionItem（避免被“药水可堆叠”类模组对所有 PotionItem 放开堆叠而变得可叠加）；
 * 酿造由 BrewingStandInfinitePotionMixin 实现（放行酿造台槽位 + 直接拦截 hasRecipe/craft 驱动产出，
 * 不依赖 ITEM_RECIPES 列表注册）：饮用+火药→喷溅，喷溅+龙息→滞留。
 */
public class InfiniteEnergyPotionItem extends Item {

	/** 三种形态及其充能时长。 */
	public enum Type {
		DRINK(200),     // 饮用：恢复 10 秒
		SPLASH(300),    // 喷溅：恢复 15 秒
		LINGERING(400); // 滞留：恢复 20 秒

		public final int rechargeTicks;

		Type(int ticks) {
			this.rechargeTicks = ticks;
		}
	}

	/** 充满时刻的世界游戏时间（game time）；存在且大于当前时间 = 空瓶充能中。 */
	private static final String NBT_FULL_AT = "FullAtTime";
	/** 饮用读条时长，与原版药水一致（32 tick）。 */
	private static final int DRINK_TIME = 32;

	private final Type type;

	public InfiniteEnergyPotionItem(Properties settings, Type type) {
		super(settings);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	/** 是否处于空瓶充能中（基于世界游戏时间戳，任意位置自动充能，无需 tick）。 */
	public static boolean isRecharging(ItemStack stack, @Nullable Level world) {
		if (world == null) {
			return false;
		}
		CompoundTag nbt = stack.getTag();
		if (nbt == null || !nbt.contains(NBT_FULL_AT)) {
			return false;
		}
		return world.getGameTime() < nbt.getLong(NBT_FULL_AT);
	}

	/** 仅凭 NBT 判断是否标记为空瓶（无世界对象时用于命名/物品栏显示；充满后由 inventoryTick 清除标记）。 */
	public static boolean isEmptyByNbt(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		return nbt != null && nbt.contains(NBT_FULL_AT);
	}

	/** 返回充能结束的世界游戏时间（无标记返回 0）。供药水袋把剩余充能时间同步到冷却遮罩使用。 */
	public static long getRechargeEndTime(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		return (nbt != null && nbt.contains(NBT_FULL_AT)) ? nbt.getLong(NBT_FULL_AT) : 0L;
	}

	/** 标记一次使用：写入充满时刻 = 当前世界时间 + 充能时长（进入空瓶状态）。供本类与药水袋调用。 */
	public void markUsed(ItemStack stack, Level world) {
		stack.getOrCreateTag().putLong(NBT_FULL_AT, world.getGameTime() + type.rechargeTicks);
	}

	/** 生成原版投掷药水弹射物并携带压缩能量药水(feed_potion)，复用原版喷溅/滞留范围效果。仅服务端调用。 */
	public void spawnThrownPotion(Level world, Player user) {
		ItemStack thrown = PotionUtils.setPotion(
				new ItemStack(type == Type.LINGERING ? Items.LINGERING_POTION : Items.SPLASH_POTION),
				ModPotions.FEED.get());
		ThrownPotion entity = new ThrownPotion(world, user);
		entity.setItem(thrown);
		entity.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
		world.addFreshEntity(entity);
	}

	/** 投掷音效（喷溅/滞留通用）。 */
	public static void playThrowSound(Level world, Player user) {
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
				SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		// 空瓶充能中：不可使用
		if (isRecharging(stack, world)) {
			return InteractionResultHolder.pass(stack);
		}
		if (type == Type.DRINK) {
			// 开始饮用读条
			user.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		// 投掷型（喷溅/滞留）
		if (!world.isClientSide) {
			spawnThrownPotion(world, user);
		}
		// 双端均标记空瓶（world.getTime 双端同步），客户端即时显示空瓶材质
		markUsed(stack, world);
		playThrowSound(world, user);
		return InteractionResultHolder.success(stack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (!world.isClientSide && user instanceof Player player) {
			// 施加与压缩能量药水一致的效果（瞬时：+8 饱食 +0.6 饱和，使魔额外 +25 魔力）
			ModEffects.FEED.get().applyInstantenousEffect(player, player, player, 0, 1.0);
		}
		// 双端均标记空瓶，确保客户端立即显示空瓶材质与名称
		markUsed(stack, world);
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return type == Type.DRINK ? DRINK_TIME : 0;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return type == Type.DRINK ? UseAnim.DRINK : UseAnim.NONE;
	}

	/**
	 * 若已充满则清除空瓶标记（NBT 内只剩它则整体清空），使名称/材质恢复满瓶。
	 * 供本类 inventoryTick 与药水袋显示清理共用（药水袋内存储物不走 inventoryTick，需主动清理）。
	 *
	 * @return 是否发生了清除
	 */
	public static boolean clearRechargeMarkIfDone(ItemStack stack, Level world) {
		if (world == null) {
			return false;
		}
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains(NBT_FULL_AT) && world.getGameTime() >= nbt.getLong(NBT_FULL_AT)) {
			nbt.remove(NBT_FULL_AT);
			if (nbt.isEmpty()) {
				stack.setTag(null);
			}
			return true;
		}
		return false;
	}

	/** 充满后清除空瓶标记，使物品栏内名称/材质恢复满瓶（任意位置自动充能由 isRecharging 时间戳保证）。 */
	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
		if (!world.isClientSide) {
			clearRechargeMarkIfDone(stack, world);
		}
		super.inventoryTick(stack, world, entity, slot, selected);
	}

	/** 附魔光效。 */
	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	/** 满/空显示不同名称（自定义命名由 ItemStack#getName 在调用本方法前拦截）。 */
	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.getDescriptionId() + (isEmptyByNbt(stack) ? ".empty" : ""));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.infinite_energy_potion.tooltip").withStyle(ChatFormatting.GRAY));
		if (isRecharging(stack, world)) {
			CompoundTag nbt = stack.getTag();
			long remain = nbt == null ? 0 : nbt.getLong(NBT_FULL_AT) - world.getGameTime();
			int sec = (int) Math.ceil(Math.max(0, remain) / 20.0);
			tooltip.add(Component.translatable("item.ssc_addon.infinite_energy_potion.tooltip.recharging", sec)
					.withStyle(ChatFormatting.AQUA));
		} else {
			tooltip.add(Component.translatable("item.ssc_addon.infinite_energy_potion.tooltip.ready")
					.withStyle(ChatFormatting.GREEN));
		}
		// 不调用 super.appendTooltip：PotionItem 会按空药水追加“无效果”提示
	}
}
