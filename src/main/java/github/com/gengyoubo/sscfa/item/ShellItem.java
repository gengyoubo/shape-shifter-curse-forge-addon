package github.com.gengyoubo.sscfa.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 迁移占位物品：保留注册 ID、堆叠/耐火/稀有度等属性，以及语言文件里已有的 {@code <物品键>.tooltip} 说明。
 * 原物品的使用、饰品、充能等行为依赖能力/饰品/网络等未迁移系统，迁移后替换为原类。
 */
public class ShellItem extends Item {
	private final boolean foil;

	public ShellItem(Properties properties) {
		this(properties, false);
	}

	public ShellItem(Properties properties, boolean foil) {
		super(properties);
		this.foil = foil;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return foil || super.isFoil(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		String key = getDescriptionId() + ".tooltip";
		if (Language.getInstance().has(key)) {
			tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
		}
	}
}
