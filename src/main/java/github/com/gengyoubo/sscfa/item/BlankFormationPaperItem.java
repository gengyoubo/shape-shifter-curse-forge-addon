package github.com.gengyoubo.sscfa.item;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 空白法阵纸（jackcooper）。月尘纯晶×4 中间夹一张纸合成；放入法术研究台，
 * 配合对应系油墨抄写已学习的增强法阵。
 */
public class BlankFormationPaperItem extends Item {

	public BlankFormationPaperItem(Properties settings) {
		super(settings);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.blank_formation_paper.tip").withStyle(ChatFormatting.DARK_GRAY));
	}
}
