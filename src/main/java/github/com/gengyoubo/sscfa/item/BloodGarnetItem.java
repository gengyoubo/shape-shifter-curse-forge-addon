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
 * 渴血石榴石（吸血蝙蝠专属）?
 * 好处：血渴值累积速度 +50%（普?/ 技?/ 击杀获得?×1.5）?
 * 坏处：脱战衰?+50% 且每秒被动流?1 点血渴值?
 * 数值逻辑?BatDesmodusBloodThirst 中按是否装备本饰品调整?
 *
 * 获取途径?0% 概率出现在废弃矿井战利品箱中?
 */
public class BloodGarnetItem extends AccessoryItem {

	private static final ResourceLocation MINESHAFT_LOOT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");

	public BloodGarnetItem(Properties settings) {
		super(settings);
	}

	/**
	 * 注册渴血石榴石到废弃矿井战利品表?5% 概率）?
	 */
	public static void registerLootTable() {}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isBatDesmodus);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.blood_garnet.tooltip_1").withStyle(ChatFormatting.DARK_RED));
		tooltip.add(Component.translatable("item.ssc_addon.blood_garnet.tooltip_2").withStyle(ChatFormatting.RED));
		tooltip.add(Component.translatable("item.ssc_addon.blood_garnet.tooltip_exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
