package github.com.gengyoubo.sscfa.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 便携加湿器（1/2/3 级）：等级与使用时间存于 NBT {@code Level}/{@code Charge}，供充能/升级配方读写。
 * 佩戴回复湿润度、淋雨回充、三级 +15% 伤害依赖饰品与形态系统，迁移后补回 accessoryTick 与佩戴限制。
 */
public class PortableMoisturizerItem extends Item {
	public static final int MAX_LEVEL = 3;

	public PortableMoisturizerItem(Properties settings) {
		super(settings);
	}

	/** 各级使用时间上限（1 charge = 1 秒）：一级 5400、二级 8100、三级 10800。 */
	public static int getMaxCharge(int level) {
		return switch (level) {
			case 2 -> 8100;
			case 3 -> 10800;
			default -> 5400;
		};
	}

	public static int getLevel(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt == null || !nbt.contains("Level")) return 1;
		return Math.max(1, Math.min(MAX_LEVEL, nbt.getInt("Level")));
	}

	public static void setLevel(ItemStack stack, int level) {
		stack.getOrCreateTag().putInt("Level", Math.max(1, Math.min(MAX_LEVEL, level)));
	}

	public static int getCharge(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		return nbt == null ? 0 : nbt.getInt("Charge");
	}

	public static void setCharge(ItemStack stack, int charge) {
		int max = getMaxCharge(getLevel(stack));
		stack.getOrCreateTag().putInt("Charge", Math.max(0, Math.min(charge, max)));
	}

	/** 配方充满：按当前等级上限充满使用时间。 */
	public static void setFullCharge(ItemStack stack) {
		stack.getOrCreateTag().putInt("Charge", getMaxCharge(getLevel(stack)));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!world.isClientSide) {
			int level = getLevel(stack);
			player.displayClientMessage(Component.translatable("message.ssc_addon.moisturizer.info", level,
					formatTime(getCharge(stack)), formatTime(getMaxCharge(level))), true);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0f * getCharge(stack) / getMaxCharge(getLevel(stack)));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x31C8CC;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		int level = getLevel(stack);
		tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.level", level).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.charge",
				formatTime(getCharge(stack)), formatTime(getMaxCharge(level))).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.usage").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.rain").withStyle(ChatFormatting.DARK_AQUA));
		if (level >= 3) {
			tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.damage").withStyle(ChatFormatting.RED));
		}
		tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.refill").withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.translatable("tooltip.ssc_addon.moisturizer.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}

	private static String formatTime(int seconds) {
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}
}
