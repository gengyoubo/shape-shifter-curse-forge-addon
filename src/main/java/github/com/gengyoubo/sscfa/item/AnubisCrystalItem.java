package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 阿努比斯权杖上的水晶 - SP阿努比斯之狼专属饰品
 * 效果：增加冥狼召唤数量和上限
 * 获取途径：沙漠神殿战利品箱，15%概率
 */
public class AnubisCrystalItem extends AccessoryItem {
	public AnubisCrystalItem(Properties settings) {
		super(settings);
	}

	/**
	 * 注册到沙漠神殿战利品表（15%概率?个）
	 */
	public static void registerLootTable() {}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isAnubisWolfSP);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.anubis_crystal.tooltip_1").withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.add(Component.translatable("item.ssc_addon.anubis_crystal.tooltip_2").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.anubis_crystal.tooltip_3").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.anubis_crystal.tooltip_4").withStyle(ChatFormatting.RED));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
