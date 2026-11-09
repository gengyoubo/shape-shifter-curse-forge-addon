package github.com.gengyoubo.sscfa.item;

import net.minecraft.ChatFormatting;
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
 * 便携冰箱：充能存于 NBT {@code Charge}，上限 {@value #MAX_CHARGE}，供暴雪罐充能配方读写。
 * 佩戴自动给雪球发射器装弹、自回充、寒棘狐专属佩戴限制依赖饰品系统，迁移后补回。
 */
public class PortableFridgeItem extends Item {
	public static final int MAX_CHARGE = 64;

	public PortableFridgeItem(Properties settings) {
		super(settings);
	}

	public static int getCharge(ItemStack stack) {
		return stack.getTag() == null ? 0 : stack.getTag().getInt("Charge");
	}

	public static void setCharge(ItemStack stack, int amount) {
		stack.getOrCreateTag().putInt("Charge", Math.max(0, Math.min(amount, MAX_CHARGE)));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		if (!world.isClientSide) {
			user.displayClientMessage(Component.translatable("item.ssc_addon.portable_fridge.charge",
					getCharge(user.getItemInHand(hand)), MAX_CHARGE), true);
		}
		return InteractionResultHolder.success(user.getItemInHand(hand));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0f * getCharge(stack) / MAX_CHARGE);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x00FFFF;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("tooltip.ssc_addon.portable_fridge.desc").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.ssc_addon.portable_fridge.status", getCharge(stack), MAX_CHARGE).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.ssc_addon.portable_fridge.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
