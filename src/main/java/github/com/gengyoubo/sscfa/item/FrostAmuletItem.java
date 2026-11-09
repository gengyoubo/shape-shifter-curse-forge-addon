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

public class FrostAmuletItem extends AccessoryItem {
	public FrostAmuletItem(Properties settings) {
		super(settings);
	}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isSnowFoxSP);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.frost_amulet.tooltip.2").withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.translatable("item.ssc_addon.frost_amulet.tooltip.exclusive").withStyle(ChatFormatting.AQUA));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
