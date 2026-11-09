package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PhantomBellItem extends AccessoryItem {

	public static final int MAX_COOLDOWN = 1200;

	public PhantomBellItem(Properties settings) {
		super(settings);
	}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isFamiliarFoxForm);
	}

	/**
	 * 获取当前冷却剩余ticks (仅客户端有????
	 * 通??{@link FabricLoader} 环境守??+ {@link LifesavingCatTailClient} 隔离类，
	 * 保证专用服务端不会触??{@code MinecraftClient} 的类链接??
	 */
	private int getCooldownRemaining() {
		if (net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) return 0;
		return LifesavingCatTailClient.getCooldownRemaining(this, MAX_COOLDOWN);
	}

	/**
	 * 判断是否在冷却中 (仅客户端有????
	 */
	private boolean isOnCooldown() {
		if (net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) return false;
		return LifesavingCatTailClient.isOnCooldown(this);
	}

	/**
	 * 根据CD进度获取颜??(红??>橙??>绿??
	 * progress: 0.0 = 刚开始冷?? 1.0 = 冷却完??
	 */
	private int getColorForProgress(float progress) {
		if (progress < 0.33f) {
			// 红色区域 (0% - 33%)
			return 0xFF5555; // 红??
		} else if (progress < 0.66f) {
			// 橙色区域 (33% - 66%)
			return 0xFFAA00; // 濮楁瑨澹?
		} else {
			// 绿色区域 (66% - 100%)
			return 0x55FF55; // 缂佽儻澹?
		}
	}

	/**
	 * 根据CD进度获取Formatting颜??
	 */
	private ChatFormatting getFormattingForProgress(float progress) {
		if (progress < 0.33f) {
			return ChatFormatting.RED;
		} else if (progress < 0.66f) {
			return ChatFormatting.GOLD;
		} else {
			return ChatFormatting.GREEN;
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		// 只有在冷却中才显示进度??
		return isOnCooldown();
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		// 返??-13的值，表示进度条的长度
		// 冷却剩余越少，进度条越??
		int remaining = getCooldownRemaining();
		float progress = 1.0f - ((float) remaining / MAX_COOLDOWN);
		return Math.round(13.0f * progress);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		int remaining = getCooldownRemaining();
		float progress = 1.0f - ((float) remaining / MAX_COOLDOWN);
		return getColorForProgress(progress);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		// 移除?装备在项链栏生??的提?
		tooltip.add(Component.translatable("item.ssc_addon.phantom_bell.desc.1").withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.translatable("item.ssc_addon.phantom_bell.desc.2").withStyle(ChatFormatting.BLUE));

		// 显示CD状??
		if (isOnCooldown()) {
			int remainingTicks = getCooldownRemaining();
			int remainingSeconds = remainingTicks / 20;
			float progress = 1.0f - ((float) remainingTicks / MAX_COOLDOWN);
			ChatFormatting color = getFormattingForProgress(progress);
			tooltip.add(Component.translatable("item.ssc_addon.phantom_bell.cooldown", remainingSeconds).withStyle(color));
		} else {
			tooltip.add(Component.translatable("item.ssc_addon.phantom_bell.ready").withStyle(ChatFormatting.GREEN));
		}

		tooltip.add(Component.translatable("item.ssc_addon.phantom_bell.tooltip.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
