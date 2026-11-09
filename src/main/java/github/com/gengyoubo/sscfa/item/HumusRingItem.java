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
 * 腐殖之戒（寄生果蝠专属）?
 * 好处：敌方削弱果效果时长 +50%?
 * 坏处：友方增益果效果时长 -30%（更偏攻?/ 控制向）?
 * 数值逻辑?ParasiticFruitSeedPower 的果实时长计算中按是否装备本饰品调整?
 *
 * 获取途径?0% 概率出现在废弃矿井战利品箱中?
 */
public class HumusRingItem extends AccessoryItem {

	private static final ResourceLocation MINESHAFT_LOOT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");

	public HumusRingItem(Properties settings) {
		super(settings);
	}

	/**
	 * 注册腐殖之戒到废弃矿井战利品表（25% 概率）?
	 */
	public static void registerLootTable() {}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isBatParasiticFruit);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.humus_ring.tooltip_1").withStyle(ChatFormatting.DARK_PURPLE));
		tooltip.add(Component.translatable("item.ssc_addon.humus_ring.tooltip_2").withStyle(ChatFormatting.GREEN));
		tooltip.add(Component.translatable("item.ssc_addon.humus_ring.tooltip_exclusive").withStyle(ChatFormatting.GOLD));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
