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
 * 嗜血指环（吸血蝙蝠专属）?
 * 好处：高血渴阶段（50+ / 75+）吸血率额?+15%?
 * 坏处：自身满血时仍触发吸血会反噬——每秒对自己造成 1 点真实伤害?
 * 数值逻辑?SscAddonLivingEntityMixin（吸血加成）与 BatDesmodusBloodThirst（满血反噬）中处理?
 *
 * 获取途径?0% 概率出现在废弃矿井战利品箱中?
 */
public class BloodlustRingItem extends AccessoryItem {

	private static final ResourceLocation MINESHAFT_LOOT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");

	public BloodlustRingItem(Properties settings) {
		super(settings);
	}

	/**
	 * 注册嗜血指环到废弃矿井战利品表（25% 概率）?
	 */
	public static void registerLootTable() {}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isBatDesmodus);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.bloodlust_ring.tooltip_1").withStyle(ChatFormatting.DARK_RED));
		tooltip.add(Component.translatable("item.ssc_addon.bloodlust_ring.tooltip_2").withStyle(ChatFormatting.RED));
		tooltip.add(Component.translatable("item.ssc_addon.bloodlust_ring.tooltip_exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
