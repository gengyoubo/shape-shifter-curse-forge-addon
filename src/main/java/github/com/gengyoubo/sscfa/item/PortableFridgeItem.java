package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PortableFridgeItem extends AccessoryItem {

	public static final int MAX_CHARGE = 64;

	public PortableFridgeItem(Properties settings) {
		super(settings);
	}

	public static int getCharge(ItemStack stack) {
		if (!stack.hasTag()) return 0;
		if (stack.getTag() != null) {
			return stack.getTag().getInt("Charge");
		}
		return 0;
	}

	public static void setCharge(ItemStack stack, int amount) {
		stack.getOrCreateTag().putInt("Charge", Math.max(0, Math.min(amount, MAX_CHARGE)));
	}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isSnowFoxSP);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		if (!world.isClientSide) {
			user.displayClientMessage(Component.translatable("item.ssc_addon.portable_fridge.charge", getCharge(user.getItemInHand(hand)), MAX_CHARGE), true);
		}
		return InteractionResultHolder.success(user.getItemInHand(hand));
	}

	@Override
	public void accessoryTick(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		if (entity.level().isClientSide) return;

		// 1. Logic: Refill Launcher every 0.5s (10 ticks)
		if (entity.tickCount % 10 == 0 && entity instanceof Player player) {
			int currentCharge = getCharge(stack);

			if (currentCharge > 0) {
				// Find Snowball Launcher in inventory
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack invStack = player.getInventory().getItem(i);
					if (invStack.getItem() instanceof SnowballLauncherItem) {
						int currentAmmo = SnowballLauncherItem.getAmmo(invStack);
						if (currentAmmo < SnowballLauncherItem.MAX_AMMO) {
							SnowballLauncherItem.setAmmo(invStack, currentAmmo + 1);
							setCharge(stack, currentCharge - 1);
							// Only refill one at a time per tick cycle
							break;
						}
					}
				}
			}
		}

		// 2. Logic: Self-regenerate 1 charge every 2s (40 ticks)
		if (entity.tickCount % 40 == 0) {
			int currentCharge = getCharge(stack);
			if (currentCharge < MAX_CHARGE) {
				setCharge(stack, currentCharge + 1);
			}
		}
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
		return 0x00FFFF; // Cyan color for ice/snow
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("tooltip.ssc_addon.portable_fridge.desc").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.ssc_addon.portable_fridge.status", getCharge(stack), MAX_CHARGE).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.ssc_addon.portable_fridge.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
